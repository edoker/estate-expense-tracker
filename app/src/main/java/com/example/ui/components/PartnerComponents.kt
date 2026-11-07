package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.formatIndianCurrency
import com.example.ui.theme.PartnerTheme
import kotlinx.coroutines.launch

/**
 * Primary action button: 56dp tall, 12dp radius, accent fill, uppercase text
 */
@Composable
fun PartnerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = PartnerTheme.colors.accentButtonFill,
    contentColor: Color = PartnerTheme.colors.accentButtonText,
    testTag: String = "primary_button"
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.primaryButtonHeight)
            .clip(RoundedCornerShape(dimens.buttonRadius))
            .background(if (enabled) containerColor else colors.border)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = typography.primaryButton,
            color = if (enabled) contentColor else colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Secondary / Outlined action button: 52-56dp tall, 12dp radius, 1.5dp border
 */
@Composable
fun PartnerOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = PartnerTheme.colors.border,
    textColor: Color = PartnerTheme.colors.text,
    height: Dp = PartnerTheme.dimens.primaryButtonHeight,
    testTag: String = "outlined_button"
) {
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(dimens.buttonRadius))
            .border(dimens.standardBorder, borderColor, RoundedCornerShape(dimens.buttonRadius))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = typography.bodyBold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Danger Outlined Button (e.g. Delete button on Entry details)
 */
@Composable
fun PartnerDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "danger_button"
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    Box(
        modifier = modifier
            .height(dimens.primaryButtonHeight)
            .clip(RoundedCornerShape(dimens.buttonRadius))
            .border(dimens.standardBorder, colors.dangerRed, RoundedCornerShape(dimens.buttonRadius))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = typography.bodyBold,
            color = colors.dangerRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/**
 * Icon button with 48dp minimum touch target and outlined rounded square
 */
@Composable
fun PartnerSquareIconButton(
    iconType: PartnerIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PartnerTheme.colors.accent,
    iconSize: Dp = 20.dp,
    containerSize: Dp = 44.dp,
    borderColor: Color = PartnerTheme.colors.border,
    backgroundColor: Color = PartnerTheme.colors.surface,
    testTag: String = "icon_button"
) {
    val dimens = PartnerTheme.dimens

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(RoundedCornerShape(dimens.iconSquareRadius))
            .background(backgroundColor)
            .border(dimens.standardBorder, borderColor, RoundedCornerShape(dimens.iconSquareRadius))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        PartnerIcon(
            type = iconType,
            tint = tint,
            size = iconSize
        )
    }
}

/**
 * Standard Outlined Card: 14dp radius, flat surface fill, 1.5dp border, no heavy shadow
 */
@Composable
fun PartnerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = PartnerTheme.colors.border,
    backgroundColor: Color = PartnerTheme.colors.surface,
    borderWidth: Dp = PartnerTheme.dimens.standardBorder,
    cornerRadius: Dp = PartnerTheme.dimens.cardRadius,
    testTag: String = "partner_card",
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
        .testTag(testTag)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        content()
    }
}

enum class PartnerChipVariant {
    DEFAULT, // Neutral blue accent when selected (e.g. All, Member)
    GAVE,    // Red border, text, and red-soft fill when selected
    GOT      // Green border, text, and green-soft fill when selected
}

/**
 * 44dp tall filter chip: radius 10dp, 1.5dp border, 13sp/600 font
 * Styling is consistent across All, Gave, and Got variants.
 */
@Composable
fun PartnerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PartnerChipVariant = PartnerChipVariant.DEFAULT,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "partner_chip"
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    // Background color: soft variant tint when selected, surface when unselected
    val bg = if (selected) {
        when (variant) {
            PartnerChipVariant.DEFAULT -> colors.accentSoft
            PartnerChipVariant.GAVE -> colors.gaveSoft
            PartnerChipVariant.GOT -> colors.gotSoft
        }
    } else {
        colors.surface
    }

    // Border color: variant color when selected, neutral border when unselected
    val borderCol = if (selected) {
        when (variant) {
            PartnerChipVariant.DEFAULT -> colors.accent
            PartnerChipVariant.GAVE -> colors.gaveRed
            PartnerChipVariant.GOT -> colors.gotGreen
        }
    } else {
        colors.border
    }

    // Text color: variant color when selected, primary text when unselected
    val textCol = if (selected) {
        when (variant) {
            PartnerChipVariant.DEFAULT -> colors.accent
            PartnerChipVariant.GAVE -> colors.gaveRed
            PartnerChipVariant.GOT -> colors.gotGreen
        }
    } else {
        colors.text
    }

    Box(
        modifier = modifier
            .height(dimens.chipHeight)
            .clip(RoundedCornerShape(dimens.chipRadius))
            .background(bg)
            .border(dimens.standardBorder, borderCol, RoundedCornerShape(dimens.chipRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = typography.chip,
                color = textCol,
                maxLines = 1
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
                trailingIcon()
            }
        }
    }
}

/**
 * Tab bar (Expenses / Audit Trail / Members) with icons above label
 */
@Composable
fun PartnerTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography

    val tabs = listOf(
        Triple(0, "Expenses", PartnerIconType.TAB_EXPENSES),
        Triple(1, "Audit Trail", PartnerIconType.TAB_AUDIT),
        Triple(2, "Members", PartnerIconType.TAB_MEMBERS)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            tabs.forEach { (index, title, iconType) ->
                val isSelected = selectedTab == index
                val tint = if (isSelected) colors.accent else colors.textMuted

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(vertical = 10.dp)
                        .testTag("tab_$index"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PartnerIcon(
                        type = iconType,
                        tint = tint,
                        size = 22.dp
                    )
                    Text(
                        text = title,
                        style = typography.chip,
                        color = tint,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) colors.accent else Color.Transparent)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider)
        )
    }
}

/**
 * Date divider: "— 13 Nov 2026 —"
 */
@Composable
fun PartnerDateDivider(
    dateText: String,
    modifier: Modifier = Modifier
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "—  $dateText  —",
            style = typography.smallLabel,
            color = colors.textMuted
        )
    }
}

/**
 * Amount text with + or - and Indian currency formatting
 */
@Composable
fun PartnerAmountText(
    amount: Double,
    isIncome: Boolean,
    style: TextStyle = PartnerTheme.typography.amountList,
    modifier: Modifier = Modifier
) {
    val colors = PartnerTheme.colors
    val formatted = formatIndianCurrency(amount)
    val prefix = if (isIncome) "+ " else "− "
    val color = if (isIncome) colors.gotGreen else colors.gaveRed

    Text(
        text = "$prefix$formatted",
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * Outlined input field: 12dp radius, 1.5dp border, Tomorrow font, small uppercase label
 */
@Composable
fun PartnerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    customBorderColor: Color? = null,
    customTextColor: Color? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingContent: (@Composable () -> Unit)? = null,
    testTag: String = "input_field"
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = typography.smallLabel,
            color = if (isError) colors.dangerRed else colors.textMuted,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )

        val borderColor = if (isError) colors.dangerRed else (customBorderColor ?: colors.border)
        val textColor = customTextColor ?: colors.text
        val cursorColor = customTextColor ?: colors.accent

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.buttonRadius))
                .background(colors.surface)
                .border(
                    width = dimens.standardBorder,
                    color = borderColor,
                    shape = RoundedCornerShape(dimens.buttonRadius)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = typography.body,
                            color = colors.textMuted.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        textStyle = typography.bodyBold.copy(color = textColor),
                        cursorBrush = SolidColor(cursorColor),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                }
            }
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = typography.notes,
                color = colors.dangerRed,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

/**
 * Bottom Sheet container matching the theme tokens: 24dp top corners, sheet color, border
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    testTag: String = "partner_bottom_sheet",
    content: @Composable () -> Unit
) {
    val colors = PartnerTheme.colors
    val dimens = PartnerTheme.dimens

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = dimens.sheetTopRadius, topEnd = dimens.sheetTopRadius),
        containerColor = colors.sheet,
        scrimColor = colors.modalOverlay,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            )
        },
        modifier = Modifier.testTag(testTag)
    ) {
        content()
    }
}

/**
 * Dangerous Hold Button: Must hold 3 seconds to complete action with animated fill.
 * Releasing early cancels progress.
 */
@Composable
fun DangerousHoldButton(
    actionLabel: String = "HOLD 3 SEC TO REMOVE",
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "dangerous_hold_button"
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens
    val scope = rememberCoroutineScope()

    val progressAnim = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.primaryButtonHeight)
            .clip(RoundedCornerShape(dimens.buttonRadius))
            .background(colors.surface)
            .border(dimens.emphasizedBorder, colors.dangerRed, RoundedCornerShape(dimens.buttonRadius))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        val animJob = scope.launch {
                            progressAnim.snapTo(0f)
                            progressAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                            )
                            if (isHolding) {
                                onHoldComplete()
                            }
                        }
                        try {
                            awaitRelease()
                        } finally {
                            isHolding = false
                            animJob.cancel()
                            scope.launch {
                                progressAnim.snapTo(0f)
                            }
                        }
                    }
                )
            }
            .testTag(testTag)
    ) {
        // Red progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progressAnim.value)
                .background(colors.dangerRed.copy(alpha = 0.45f))
        )

        // Text label centered
        Text(
            text = actionLabel.uppercase(),
            style = typography.primaryButton,
            color = colors.dangerRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * Search Bar: 46dp tall, 12dp radius, search icon, clear button, Tomorrow body font
 */
@Composable
fun PartnerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search amount, paid to, note...",
    modifier: Modifier = Modifier,
    onClear: () -> Unit = { onQueryChange("") },
    testTag: String = "partner_search_bar"
) {
    val colors = PartnerTheme.colors
    val typography = PartnerTheme.typography
    val dimens = PartnerTheme.dimens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.buttonRadius))
            .background(colors.surface)
            .border(
                width = dimens.standardBorder,
                color = if (query.isNotBlank()) colors.accent else colors.border,
                shape = RoundedCornerShape(dimens.buttonRadius)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PartnerIcon(
                type = PartnerIconType.SEARCH,
                tint = if (query.isNotBlank()) colors.accent else colors.textMuted,
                size = 18.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = typography.body,
                        color = colors.textMuted.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = typography.bodyBold.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input")
                )
            }

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background)
                        .clickable(onClick = onClear)
                        .testTag("clear_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    PartnerIcon(
                        type = PartnerIconType.CLOSE,
                        tint = colors.textMuted,
                        size = 12.dp
                    )
                }
            }
        }
    }
}

