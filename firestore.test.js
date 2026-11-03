const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const userAlice = { uid: "user_alice", email: "alice@example.com", email_verified: true };
const userBob = { uid: "user_bob", email: "bob@example.com", email_verified: true };
const userCharlie = { uid: "user_charlie", email: "charlie@example.com", email_verified: true };
const unverifiedUser = { uid: "user_unverified", email: "unverified@example.com", email_verified: false };

test("unauthenticated or unverified requests are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc("user_alice").get());
  await assertFails(unauthDb.collection("projects").doc("proj_1").get());

  const unverifiedDb = testEnv.authenticatedContext(unverifiedUser.uid, {
    email: unverifiedUser.email,
    email_verified: false,
  }).firestore();
  await assertFails(unverifiedDb.collection("users").doc(unverifiedUser.uid).get());
});

test("users collection: user can read/write only their own document", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();

  // Alice creates own profile
  await assertSucceeds(
    aliceDb.collection("users").doc(userAlice.uid).set({
      displayName: "Alice",
      email: "alice@example.com",
      createdAt: new Date(),
      lastLoginAt: new Date(),
      fcmTokens: [],
    })
  );

  // Alice reads own profile
  await assertSucceeds(aliceDb.collection("users").doc(userAlice.uid).get());

  // Alice cannot read or write Bob's profile
  await assertFails(aliceDb.collection("users").doc(userBob.uid).get());
  await assertFails(
    aliceDb.collection("users").doc(userBob.uid).set({
      displayName: "Bob Fake",
      email: "bob@example.com",
      createdAt: new Date(),
      lastLoginAt: new Date(),
    })
  );
});

test("projects collection: create enforces adminUid and memberUids == [uid]", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();

  // Valid creation
  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").set({
      name: "Solar Project",
      adminUid: userAlice.uid,
      memberUids: [userAlice.uid],
      members: {
        [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
      },
      createdAt: new Date(),
    })
  );

  // Invalid: adminUid does not match auth.uid
  await assertFails(
    aliceDb.collection("projects").doc("proj_2").set({
      name: "Wind Project",
      adminUid: userBob.uid,
      memberUids: [userAlice.uid],
      members: {
        [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
      },
      createdAt: new Date(),
    })
  );

  // Invalid: memberUids contains other members at creation
  await assertFails(
    aliceDb.collection("projects").doc("proj_3").set({
      name: "Hydro Project",
      adminUid: userAlice.uid,
      memberUids: [userAlice.uid, userBob.uid],
      members: {
        [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
        [userBob.uid]: { name: "Bob", email: "bob@example.com", role: "member" },
      },
      createdAt: new Date(),
    })
  );
});

test("projects collection: read and hard-delete enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();

  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  const bobDb = testEnv.authenticatedContext(userBob.uid, {
    email: userBob.email,
    email_verified: true,
  }).firestore();

  // Member Alice can read
  await assertSucceeds(aliceDb.collection("projects").doc("proj_1").get());

  // Non-member Bob cannot read
  await assertFails(bobDb.collection("projects").doc("proj_1").get());

  // Hard delete is never allowed
  await assertFails(aliceDb.collection("projects").doc("proj_1").delete());
});

test("invites collection: admin invites, non-admin denied, invited user reads and accepts", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(userBob.uid, {
    email: userBob.email,
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(userCharlie.uid, {
    email: userCharlie.email,
    email_verified: true,
  }).firestore();

  // Alice creates project
  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  // Bob (not admin) cannot create invite
  await assertFails(
    bobDb.collection("invites").doc("proj_1_charlie@example.com").set({
      projectId: "proj_1",
      projectName: "Solar Project",
      invitedEmail: "charlie@example.com",
      invitedByUid: userBob.uid,
      invitedByName: "Bob",
      status: "pending",
      createdAt: new Date(),
    })
  );

  // Alice (admin) creates invite for Bob
  await assertSucceeds(
    aliceDb.collection("invites").doc("proj_1_bob@example.com").set({
      projectId: "proj_1",
      projectName: "Solar Project",
      invitedEmail: "bob@example.com",
      invitedByUid: userAlice.uid,
      invitedByName: "Alice",
      status: "pending",
      createdAt: new Date(),
    })
  );

  // Bob reads his invite
  await assertSucceeds(bobDb.collection("invites").doc("proj_1_bob@example.com").get());

  // Charlie (stranger) cannot read Bob's invite
  await assertFails(charlieDb.collection("invites").doc("proj_1_bob@example.com").get());

  // Bob accepts invite and joins project
  await assertSucceeds(
    bobDb.collection("projects").doc("proj_1").update({
      memberUids: [userAlice.uid, userBob.uid],
      "members.user_bob": { name: "Bob", email: "bob@example.com", role: "member" },
    })
  );

  await assertSucceeds(
    bobDb.collection("invites").doc("proj_1_bob@example.com").update({
      status: "accepted",
    })
  );

  // Bob is now a member and can read the project
  await assertSucceeds(bobDb.collection("projects").doc("proj_1").get());
});

test("joining project: uninvited user cannot join; invited user cannot join as admin", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(userCharlie.uid, {
    email: userCharlie.email,
    email_verified: true,
  }).firestore();

  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  // Charlie has no invite, cannot join
  await assertFails(
    charlieDb.collection("projects").doc("proj_1").update({
      memberUids: [userAlice.uid, userCharlie.uid],
      "members.user_charlie": { name: "Charlie", email: "charlie@example.com", role: "member" },
    })
  );

  // Admin invites Charlie
  await aliceDb.collection("invites").doc("proj_1_charlie@example.com").set({
    projectId: "proj_1",
    projectName: "Solar Project",
    invitedEmail: "charlie@example.com",
    invitedByUid: userAlice.uid,
    invitedByName: "Alice",
    status: "pending",
    createdAt: new Date(),
  });

  // Charlie cannot join as 'admin' role
  await assertFails(
    charlieDb.collection("projects").doc("proj_1").update({
      memberUids: [userAlice.uid, userCharlie.uid],
      "members.user_charlie": { name: "Charlie", email: "charlie@example.com", role: "admin" },
    })
  );
});

test("expenses subcollection: CRUD and invariants", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(userCharlie.uid, {
    email: userCharlie.email,
    email_verified: true,
  }).firestore();

  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  const now = new Date();

  // Valid expense creation by member Alice
  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_1").set({
      supplierName: "Apex Cement",
      supplierPhone: "9876543210",
      amount: 45000,
      date: now,
      paidByUid: userAlice.uid,
      enteredByUid: userAlice.uid,
      note: "50 bags cement",
      attachments: [],
      isDeleted: false,
      deletedByUid: null,
      deletedAt: null,
      createdAt: now,
      updatedAt: now,
      updatedByUid: userAlice.uid,
    })
  );

  // Non-member Charlie cannot read or create expense
  await assertFails(
    charlieDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_1").get()
  );
  await assertFails(
    charlieDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_2").set({
      supplierName: "Bricks Inc",
      amount: 10000,
      date: now,
      paidByUid: userAlice.uid,
      enteredByUid: userCharlie.uid,
      updatedByUid: userCharlie.uid,
      isDeleted: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Member cannot create expense with amount <= 0
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_invalid_amount").set({
      supplierName: "Apex Cement",
      amount: 0,
      date: now,
      paidByUid: userAlice.uid,
      enteredByUid: userAlice.uid,
      updatedByUid: userAlice.uid,
      isDeleted: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Member cannot set enteredByUid to someone else
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_invalid_enterer").set({
      supplierName: "Apex Cement",
      amount: 5000,
      date: now,
      paidByUid: userAlice.uid,
      enteredByUid: userBob.uid,
      updatedByUid: userAlice.uid,
      isDeleted: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid expense update
  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_1").update({
      amount: 48000,
      updatedAt: new Date(),
      updatedByUid: userAlice.uid,
    })
  );

  // Invalid update: attempting to change enteredByUid
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_1").update({
      enteredByUid: userBob.uid,
      updatedAt: new Date(),
      updatedByUid: userAlice.uid,
    })
  );

  // Soft delete is allowed via update
  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_1").update({
      isDeleted: true,
      deletedByUid: userAlice.uid,
      deletedAt: new Date(),
      updatedAt: new Date(),
      updatedByUid: userAlice.uid,
    })
  );

  // Hard delete is forbidden
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").collection("expenses").doc("exp_1").delete()
  );
});

test("audit subcollection: append-only, read-only for members, no update or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(userCharlie.uid, {
    email: userCharlie.email,
    email_verified: true,
  }).firestore();

  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  const now = new Date();

  // Alice logs audit entry
  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").collection("audit").doc("audit_1").set({
      action: "created",
      entityType: "expense",
      entityId: "exp_1",
      actorUid: userAlice.uid,
      actorName: "Alice",
      timestamp: now,
      summary: "Alice added ₹45,000 to Apex Cement",
      changes: {},
      snapshot: { amount: 45000 },
    })
  );

  // Alice can read audit trail
  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").collection("audit").doc("audit_1").get()
  );

  // Non-member Charlie cannot read or create audit entry
  await assertFails(
    charlieDb.collection("projects").doc("proj_1").collection("audit").doc("audit_1").get()
  );
  await assertFails(
    charlieDb.collection("projects").doc("proj_1").collection("audit").doc("audit_2").set({
      action: "created",
      entityType: "expense",
      entityId: "exp_1",
      actorUid: userCharlie.uid,
      actorName: "Charlie",
      timestamp: now,
      summary: "Tampered audit entry",
    })
  );

  // Nobody can update audit entries
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").collection("audit").doc("audit_1").update({
      summary: "Modified summary",
    })
  );

  // Nobody can delete audit entries
  await assertFails(
    aliceDb.collection("projects").doc("proj_1").collection("audit").doc("audit_1").delete()
  );
});

test("invites management: cancel by admin, delete by non-admin denied, decline by invitee", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(userBob.uid, {
    email: userBob.email,
    email_verified: true,
  }).firestore();
  const charlieDb = testEnv.authenticatedContext(userCharlie.uid, {
    email: userCharlie.email,
    email_verified: true,
  }).firestore();

  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  // Admin invites Bob
  await aliceDb.collection("invites").doc("proj_1_bob@example.com").set({
    projectId: "proj_1",
    projectName: "Solar Project",
    invitedEmail: "bob@example.com",
    invitedByUid: userAlice.uid,
    invitedByName: "Alice",
    status: "pending",
    createdAt: new Date(),
  });

  // Non-admin Charlie cannot delete the invite
  await assertFails(
    charlieDb.collection("invites").doc("proj_1_bob@example.com").delete()
  );

  // Bob cannot delete the invite (only update status or admin can delete)
  await assertFails(
    bobDb.collection("invites").doc("proj_1_bob@example.com").delete()
  );

  // Bob can decline the invite
  await assertSucceeds(
    bobDb.collection("invites").doc("proj_1_bob@example.com").update({
      status: "declined",
    })
  );

  // Admin can cancel/delete a pending invite
  await aliceDb.collection("invites").doc("proj_1_charlie@example.com").set({
    projectId: "proj_1",
    projectName: "Solar Project",
    invitedEmail: "charlie@example.com",
    invitedByUid: userAlice.uid,
    invitedByName: "Alice",
    status: "pending",
    createdAt: new Date(),
  });

  await assertSucceeds(
    aliceDb.collection("invites").doc("proj_1_charlie@example.com").delete()
  );
});

test("project updates: non-admin member can update name but cannot modify members or adminUid", async () => {
  const aliceDb = testEnv.authenticatedContext(userAlice.uid, {
    email: userAlice.email,
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(userBob.uid, {
    email: userBob.email,
    email_verified: true,
  }).firestore();

  // Alice creates project (only herself in memberUids)
  await aliceDb.collection("projects").doc("proj_1").set({
    name: "Solar Project",
    adminUid: userAlice.uid,
    memberUids: [userAlice.uid],
    members: {
      [userAlice.uid]: { name: "Alice", email: "alice@example.com", role: "admin" },
    },
    createdAt: new Date(),
  });

  // Admin Alice adds Bob as a member
  await aliceDb.collection("projects").doc("proj_1").update({
    memberUids: [userAlice.uid, userBob.uid],
    "members.user_bob": { name: "Bob", email: "bob@example.com", role: "member" },
  });

  // Bob updates project name: allowed because members and memberUids and adminUid are untouched
  await assertSucceeds(
    bobDb.collection("projects").doc("proj_1").update({
      name: "Solar Project Updated by Bob",
    })
  );

  // Bob tries to make himself admin: denied
  await assertFails(
    bobDb.collection("projects").doc("proj_1").update({
      adminUid: userBob.uid,
    })
  );

  // Bob tries to remove Alice from members: denied
  await assertFails(
    bobDb.collection("projects").doc("proj_1").update({
      memberUids: [userBob.uid],
    })
  );
});

