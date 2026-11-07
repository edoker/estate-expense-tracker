import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

// Initialize Firebase Admin SDK
const app = initializeApp();

// Database ID used by PartnerLedger
const DEFAULT_DATABASE_ID = "ai-studio-android-partnerl-1d4a93ff-48a6-402e-a9a9-8c80c5c0e6a6";

/**
 * Format currency in Indian Numbering System (e.g. ₹50,000)
 */
function formatIndianCurrency(amount: number): string {
  try {
    const formatted = new Intl.NumberFormat("en-IN", {
      maximumFractionDigits: 2,
      minimumFractionDigits: 0,
    }).format(amount);
    return `₹${formatted}`;
  } catch {
    return `₹${amount}`;
  }
}

/**
 * Format Firestore Timestamp or string date to readable string
 */
function formatDate(timestampOrDate: any): string {
  if (!timestampOrDate) return "";
  try {
    if (typeof timestampOrDate.toDate === "function") {
      const d = timestampOrDate.toDate();
      return d.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
      });
    }
    return String(timestampOrDate);
  } catch {
    return String(timestampOrDate);
  }
}

/**
 * Trigger handler for changes in projects/{projectId}/expenses/{expenseId}
 */
async function handleExpenseChange(event: any) {
  const projectId = event.params.projectId;
  const expenseId = event.params.expenseId;

  if (!projectId || !expenseId) {
    console.log("Missing projectId or expenseId in event params");
    return;
  }

  // Determine the database ID from the event
  const rawDb: string = event.database || "";
  const dbName = rawDb.split("/").pop() || DEFAULT_DATABASE_ID;
  const db = getFirestore(app, dbName);

  const beforeSnap = event.data?.before;
  const afterSnap = event.data?.after;

  const beforeExists = beforeSnap && beforeSnap.exists;
  const afterExists = afterSnap && afterSnap.exists;

  // Case 1: Neither exists (shouldn't happen)
  if (!beforeExists && !afterExists) {
    return;
  }

  const beforeData = beforeExists ? beforeSnap.data() : null;
  const afterData = afterExists ? afterSnap.data() : null;

  // Determine action: "created" | "updated" | "deleted"
  let action: "created" | "updated" | "deleted" = "updated";
  const isSoftDelete =
    beforeExists &&
    afterExists &&
    !beforeData?.isDeleted &&
    afterData?.isDeleted === true;
  const isHardDelete = beforeExists && !afterExists;

  if (!beforeExists && afterExists) {
    action = "created";
  } else if (isSoftDelete || isHardDelete) {
    action = "deleted";
  } else {
    action = "updated";
  }

  // Fetch project document to get project name, members, and details
  const projectRef = db.collection("projects").doc(projectId);
  const projectDoc = await projectRef.get();
  const projectData = projectDoc.exists ? projectDoc.data() : null;
  const projectName = projectData?.name || "SpendSync";
  const membersMap = (projectData?.members || {}) as Record<
    string,
    { name?: string; email?: string; role?: string }
  >;

  // Resolve actor UID
  let actorUid = "";
  if (action === "created") {
    actorUid = afterData?.enteredByUid || afterData?.updatedByUid || "";
  } else if (action === "deleted") {
    actorUid =
      (afterData?.deletedByUid ||
        afterData?.updatedByUid ||
        beforeData?.deletedByUid ||
        beforeData?.updatedByUid ||
        beforeData?.enteredByUid) ||
      "";
  } else {
    actorUid = afterData?.updatedByUid || afterData?.enteredByUid || "";
  }

  // Resolve actor Name
  let actorName = "";
  if (actorUid) {
    const member = membersMap[actorUid];
    if (member?.name && member.name.trim().length > 0) {
      actorName = member.name.trim();
    } else if (member?.email) {
      actorName = member.email;
    } else {
      try {
        const userDoc = await db.collection("users").doc(actorUid).get();
        if (userDoc.exists) {
          const u = userDoc.data();
          actorName = u?.displayName || u?.email || "Partner";
        }
      } catch (err) {
        console.warn(`Could not fetch user info for ${actorUid}:`, err);
      }
    }
  }
  if (!actorName) {
    actorName = "Partner";
  }

  // Compute changes, summary, and snapshot
  let summary = "";
  const changes: Record<string, { from: any; to: any }> = {};
  let snapshotData: Record<string, any> = {};

  const currentData = afterData || beforeData;
  const type = currentData?.type || "GAVE";
  const isIncome = type === "GOT";
  const paymentMode = currentData?.paymentMode || "UPI";

  if (action === "created") {
    const supplier = (afterData?.supplierName || "").trim();
    const amount = Number(afterData?.amount || 0);
    const paidByUid = afterData?.paidByUid || "";
    const payer = membersMap[paidByUid];
    const payerName = payer?.name || payer?.email || "Partner";

    const attachmentsList = Array.isArray(afterData?.attachments)
      ? afterData.attachments
      : [];
    const attSuffix =
      attachmentsList.length > 0
        ? ` with ${attachmentsList.length} attachment(s)`
        : "";

    if (isIncome) {
      summary = `${actorName} recorded ${formatIndianCurrency(amount)} received from ${supplier} (${paymentMode}, received by ${payerName})${attSuffix}`;
    } else {
      summary = `${actorName} added ${formatIndianCurrency(amount)} to ${supplier} (${paymentMode}, paid by ${payerName})${attSuffix}`;
    }

    snapshotData = {
      id: expenseId,
      supplierName: supplier,
      supplierPhone: afterData?.supplierPhone || "",
      amount: amount,
      date: formatDate(afterData?.date),
      paidByUid: paidByUid,
      paidByName: payerName,
      enteredByUid: actorUid,
      enteredByName: actorName,
      type: type,
      paymentMode: paymentMode,
      note: afterData?.note || "",
      attachments: attachmentsList.map((a: any) => a?.name || "file"),
      isDeleted: false,
    };
  } else if (action === "deleted") {
    const data = isSoftDelete ? afterData : beforeData;
    const supplier = (data?.supplierName || "").trim();
    const amount = Number(data?.amount || 0);

    if (isIncome) {
      summary = `${actorName} deleted income entry of ${formatIndianCurrency(amount)} from ${supplier}`;
    } else {
      summary = `${actorName} deleted payment of ${formatIndianCurrency(amount)} to ${supplier}`;
    }
    changes["isDeleted"] = { from: false, to: true };

    const paidByUid = data?.paidByUid || "";
    const payer = membersMap[paidByUid];
    const payerName = payer?.name || payer?.email || "Partner";
    const attachmentsList = Array.isArray(data?.attachments)
      ? data.attachments
      : [];

    snapshotData = {
      id: expenseId,
      supplierName: supplier,
      supplierPhone: data?.supplierPhone || "",
      amount: amount,
      date: formatDate(data?.date),
      paidByUid: paidByUid,
      paidByName: payerName,
      enteredByUid: data?.enteredByUid || "",
      type: type,
      paymentMode: paymentMode,
      note: data?.note || "",
      attachments: attachmentsList.map((a: any) => a?.name || "file"),
      isDeleted: true,
      deletedByUid: actorUid,
    };
  } else {
    // Action: "updated"
    const cleanSupplier = (afterData?.supplierName || "").trim();
    const oldSupplier = (beforeData?.supplierName || "").trim();
    const summaryParts: string[] = [];

    if (oldSupplier !== cleanSupplier) {
      changes["supplierName"] = { from: oldSupplier, to: cleanSupplier };
      summaryParts.push(`party to '${cleanSupplier}'`);
    }

    const oldAmount = Number(beforeData?.amount || 0);
    const newAmount = Number(afterData?.amount || 0);
    if (oldAmount !== newAmount) {
      changes["amount"] = { from: oldAmount, to: newAmount };
      summaryParts.push(
        `amount from ${formatIndianCurrency(oldAmount)} to ${formatIndianCurrency(newAmount)}`
      );
    }

    const oldType = beforeData?.type || "GAVE";
    const newType = afterData?.type || "GAVE";
    if (oldType !== newType) {
      changes["type"] = { from: oldType, to: newType };
      summaryParts.push(`type to ${newType === "GOT" ? "You Got" : "You Gave"}`);
    }

    const oldMode = beforeData?.paymentMode || "UPI";
    const newMode = afterData?.paymentMode || "UPI";
    if (oldMode !== newMode) {
      changes["paymentMode"] = { from: oldMode, to: newMode };
      summaryParts.push(`mode to ${newMode}`);
    }

    const oldDateStr = formatDate(beforeData?.date);
    const newDateStr = formatDate(afterData?.date);
    if (oldDateStr !== newDateStr) {
      changes["date"] = { from: oldDateStr, to: newDateStr };
      summaryParts.push(`date to ${newDateStr}`);
    }

    const oldPaidBy = beforeData?.paidByUid || "";
    const newPaidBy = afterData?.paidByUid || "";
    if (oldPaidBy !== newPaidBy) {
      const oldPayer = membersMap[oldPaidBy]?.name || membersMap[oldPaidBy]?.email || "Partner";
      const newPayer = membersMap[newPaidBy]?.name || membersMap[newPaidBy]?.email || "Partner";
      changes["paidBy"] = { from: oldPayer, to: newPayer };
      summaryParts.push(`payer to ${newPayer}`);
    }

    const oldNote = beforeData?.note || "";
    const newNote = afterData?.note || "";
    if (oldNote !== newNote) {
      changes["note"] = { from: oldNote, to: newNote };
      summaryParts.push("note");
    }

    const oldAtt = Array.isArray(beforeData?.attachments) ? beforeData.attachments : [];
    const newAtt = Array.isArray(afterData?.attachments) ? afterData.attachments : [];
    const oldAttPaths = oldAtt.map((a: any) => a?.storagePath || a?.name).sort().join(",");
    const newAttPaths = newAtt.map((a: any) => a?.storagePath || a?.name).sort().join(",");
    if (oldAttPaths !== newAttPaths) {
      changes["attachments"] = {
        from: oldAtt.map((a: any) => a?.name || "file"),
        to: newAtt.map((a: any) => a?.name || "file"),
      };
      if (newAtt.length > oldAtt.length) {
        summaryParts.push("added attachment");
      } else if (newAtt.length < oldAtt.length) {
        summaryParts.push("removed attachment");
      } else {
        summaryParts.push("attachments");
      }
    }

    if (summaryParts.length > 0) {
      summary = `${actorName} changed ${summaryParts.join(", ")}`;
    } else {
      summary = `${actorName} updated expense for ${cleanSupplier}`;
    }

    const newPayer = membersMap[newPaidBy];
    const newPayerName = newPayer?.name || newPayer?.email || "Partner";
    const enterer = membersMap[afterData?.enteredByUid || ""];
    const entererName = enterer?.name || enterer?.email || "Partner";

    snapshotData = {
      id: expenseId,
      supplierName: cleanSupplier,
      supplierPhone: afterData?.supplierPhone || "",
      amount: newAmount,
      date: newDateStr,
      paidByUid: newPaidBy,
      paidByName: newPayerName,
      enteredByUid: afterData?.enteredByUid || "",
      enteredByName: entererName,
      note: newNote,
      attachments: newAtt.map((a: any) => a?.name || "file"),
      isDeleted: false,
    };
  }

  // 1. Write the audit document using the Firebase Admin SDK (bypasses security rules)
  const auditDocRef = db.collection("projects").doc(projectId).collection("audit").doc();
  const auditData = {
    action: action,
    entityType: "expense",
    entityId: expenseId,
    actorUid: actorUid,
    actorName: actorName,
    timestamp: FieldValue.serverTimestamp(),
    summary: summary,
    changes: changes,
    snapshot: snapshotData,
  };

  try {
    await auditDocRef.set(auditData);
    console.log(`[Audit] Created audit doc ${auditDocRef.id} for ${action} on expense ${expenseId}`);
  } catch (err) {
    console.error(`[Audit] Failed to create audit document:`, err);
  }

  // 2. Send FCM Push Notification to every OTHER project member
  try {
    const memberUids: string[] = projectData?.memberUids || Object.keys(membersMap);
    const recipientUids = memberUids.filter((uid) => uid && uid !== actorUid);

    if (recipientUids.length === 0) {
      console.log("[FCM] No other project members to notify.");
      return;
    }

    // Fetch user documents for recipient tokens
    const userDocRefs = recipientUids.map((uid) => db.collection("users").doc(uid));
    const userDocs = await db.getAll(...userDocRefs);

    const tokenToUidMap = new Map<string, string>();
    for (const uDoc of userDocs) {
      if (!uDoc.exists) continue;
      const tokens = uDoc.data()?.fcmTokens;
      if (Array.isArray(tokens)) {
        for (const t of tokens) {
          if (typeof t === "string" && t.trim().length > 0) {
            tokenToUidMap.set(t.trim(), uDoc.id);
          }
        }
      }
    }

    const allTokens = Array.from(tokenToUidMap.keys());
    if (allTokens.length === 0) {
      console.log("[FCM] No registered FCM tokens found for other members.");
      return;
    }

    console.log(`[FCM] Sending push notification to ${allTokens.length} token(s)...`);
    const messaging = getMessaging(app);

    const response = await messaging.sendEachForMulticast({
      tokens: allTokens,
      notification: {
        title: projectName,
        body: summary,
      },
      data: {
        projectId: projectId,
        expenseId: expenseId,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "expense_updates_channel",
        },
      },
    });

    console.log(
      `[FCM] Sent. Success: ${response.successCount}, Failure: ${response.failureCount}`
    );

    // Clean up invalid/unregistered tokens
    const badTokensByUid = new Map<string, string[]>();
    response.responses.forEach((res, idx) => {
      if (!res.success) {
        const token = allTokens[idx];
        const uid = tokenToUidMap.get(token);
        const errorCode = res.error?.code;
        if (
          uid &&
          (errorCode === "messaging/invalid-registration-token" ||
            errorCode === "messaging/registration-token-not-registered")
        ) {
          if (!badTokensByUid.has(uid)) {
            badTokensByUid.set(uid, []);
          }
          badTokensByUid.get(uid)!.push(token);
        }
      }
    });

    for (const [uid, badTokens] of badTokensByUid.entries()) {
      try {
        await db.collection("users").doc(uid).update({
          fcmTokens: FieldValue.arrayRemove(...badTokens),
        });
        console.log(`[FCM] Removed ${badTokens.length} stale token(s) for user ${uid}`);
      } catch (err) {
        console.error(`[FCM] Failed to remove stale tokens for user ${uid}:`, err);
      }
    }
  } catch (fcmErr) {
    console.error("[FCM] Error processing notifications:", fcmErr);
  }
}

/**
 * 2nd-gen Cloud Function trigger on custom named database
 */
export const onExpenseWritten = onDocumentWritten(
  {
    document: "projects/{projectId}/expenses/{expenseId}",
    database: DEFAULT_DATABASE_ID,
  },
  async (event) => {
    await handleExpenseChange(event);
  }
);
