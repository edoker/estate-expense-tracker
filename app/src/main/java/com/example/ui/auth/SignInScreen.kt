package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GoogleOfficialLogo
import com.example.ui.theme.PartnerTheme

@Composable
fun SignInScreen(
    errorMessage: String?,
    onSignInClick: () -> Unit,
    onDismissError: () -> Unit
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sign_in_screen"),
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = dimens.screenPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered App Logo Tile (Our existing logo with 30px corner radius, matching mockup size)
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(dimens.loginLogoRadius))
                        .background(colors.accentSoft)
                        .border(
                            width = dimens.emphasizedBorder,
                            color = colors.accent,
                            shape = RoundedCornerShape(dimens.loginLogoRadius)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "SpendSync Logo",
                        modifier = Modifier.size(54.dp),
                        tint = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Name in Tomorrow bold
                Text(
                    text = "SpendSync",
                    style = typography.netBalance.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // One-line tagline
                Text(
                    text = "Shared payment ledger for\ninfrastructure projects",
                    style = typography.body.copy(fontSize = 15.sp),
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(44.dp))

                // Error message if any
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .clip(RoundedCornerShape(dimens.buttonRadius))
                            .background(colors.redSoft)
                            .border(dimens.standardBorder, colors.dangerRed, RoundedCornerShape(dimens.buttonRadius))
                            .clickable { onDismissError() }
                            .padding(14.dp)
                            .testTag("error_card")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = colors.dangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                style = typography.notes,
                                color = colors.dangerRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Official Google Button style per spec:
                // dark theme = fill #131314, 2px border #8E918F, text #E3E3E3
                // light theme = fill #FFFFFF, 2px border #747775, text #1F1F1F
                // radius 14px, 60px tall
                val googleBg = if (colors.isDark) Color(0xFF131314) else Color(0xFFFFFFFF)
                val googleBorder = if (colors.isDark) Color(0xFF8E918F) else Color(0xFF747775)
                val googleText = if (colors.isDark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(googleBg)
                        .border(2.dp, googleBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onSignInClick)
                        .testTag("sign_in_with_google_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleOfficialLogo(size = 24.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Sign in with Google",
                            style = typography.cardName.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = googleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Under it: "Private partner access · No password"
                Text(
                    text = "PRIVATE PARTNER ACCESS · NO PASSWORD",
                    style = typography.smallLabel,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
