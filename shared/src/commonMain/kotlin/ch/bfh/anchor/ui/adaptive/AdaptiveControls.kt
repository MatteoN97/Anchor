package ch.bfh.anchor.ui.adaptive

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.ui.theme.IosMetrics
import ch.bfh.anchor.ui.theme.LocalIsIos

// ── Switch ────────────────────────────────────────────────────────────────────

/** iOS: green track, full-size white thumb in both states, no outline. */
@Composable
fun AdaptiveSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    if (!LocalIsIos.current) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
        return
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        // Any thumb content makes Material draw the unchecked thumb at full size, as iOS does.
        thumbContent = {},
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

// ── Checkbox ─────────────────────────────────────────────────────────────────

/** iOS uses a round check, as in Reminders, rather than a square box. */
@Composable
fun AdaptiveCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    if (!LocalIsIos.current) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (checked) Modifier.background(primary, CircleShape)
                    else Modifier.border(1.5.dp, outline, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Checkmark(color = Color.White, size = 12.dp, strokeWidth = 2.dp)
        }
    }
}

/** The SF Symbols "checkmark" glyph, drawn so it needs no icon asset. */
@Composable
fun Checkmark(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp, strokeWidth: Dp = 2.dp) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.08f, this@Canvas.size.height * 0.55f)
            lineTo(this@Canvas.size.width * 0.38f, this@Canvas.size.height * 0.85f)
            lineTo(this@Canvas.size.width * 0.92f, this@Canvas.size.height * 0.15f)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** The thin grey disclosure chevron at the end of a navigating row. */
@Composable
fun Chevron(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outline) {
    Canvas(modifier = modifier.size(width = 8.dp, height = 14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.08f)
            lineTo(size.width * 0.85f, size.height * 0.5f)
            lineTo(size.width * 0.15f, size.height * 0.92f)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** SF Symbols "chevron.up.chevron.down", the affordance on an iOS menu picker. */
@Composable
private fun UpDownChevrons(color: Color) {
    Canvas(modifier = Modifier.size(width = 9.dp, height = 14.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        drawPath(Path().apply {
            moveTo(w * 0.1f, h * 0.36f); lineTo(w * 0.5f, h * 0.08f); lineTo(w * 0.9f, h * 0.36f)
        }, color, style = stroke)
        drawPath(Path().apply {
            moveTo(w * 0.1f, h * 0.64f); lineTo(w * 0.5f, h * 0.92f); lineTo(w * 0.9f, h * 0.64f)
        }, color, style = stroke)
    }
}

// ── Segmented control ─────────────────────────────────────────────────────────

/**
 * iOS: a UISegmentedControl. Android: a Material TabRow. [counts] adds a badge per option when
 * non-null and positive.
 */
@Composable
fun AdaptiveSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int?> = emptyList()
) {
    if (!LocalIsIos.current) {
        PrimaryTabRow(selectedTabIndex = selectedIndex, modifier = modifier) {
            options.forEachIndexed { index, label ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(label)
                            val count = counts.getOrNull(index)
                            if (count != null && count > 0) Badge { Text("$count") }
                        }
                    }
                )
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val thumb by animateColorAsState(
                // White in light mode, a lifted grey in dark mode, as UISegmentedControl draws it.
                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (selected) Modifier.shadow(2.dp, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(thumb)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) }
                    )
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    },
                contentAlignment = Alignment.Center
            ) {
                val count = counts.getOrNull(index)
                Text(
                    text = if (count != null && count > 0) "$label ($count)" else label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Text field ───────────────────────────────────────────────────────────────

/**
 * iOS: a borderless field in a white rounded cell, with a section-style caption above it and the
 * placeholder inside, as in a Form. Android: an OutlinedTextField with a floating label.
 */
@Composable
fun AdaptiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    showLabel: Boolean = true
) {
    if (!LocalIsIos.current) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (showLabel) ({ Text(label) }) else null,
            // Without a label the field would be blank; fall back to it as the hint, as iOS does.
            placeholder = (placeholder ?: label.takeIf { !showLabel })?.let {
                { Text(it, maxLines = if (singleLine) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis) }
            },
            modifier = modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions
        )
        return
    }

    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        if (showLabel) SectionCaption(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = IosMetrics.RowMinHeight)
                        .clip(RoundedCornerShape(IosMetrics.CardCorner))
                        .background(colors.surfaceVariant)
                        .padding(horizontal = IosMetrics.RowHorizontalPadding, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder ?: if (showLabel) "" else label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            }
        )
    }
}

/** The small grey caption iOS puts above a grouped section. */
@Composable
internal fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            start = IosMetrics.RowHorizontalPadding,
            end = IosMetrics.RowHorizontalPadding,
            bottom = 7.dp
        )
    )
}

// ── Chips ────────────────────────────────────────────────────────────────────

/** A toggleable tag. iOS: a capsule that fills with the tint when on. */
@Composable
fun AdaptiveFilterChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    if (!LocalIsIos.current) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            modifier = modifier
        )
        return
    }
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) colors.primary else colors.surfaceVariant)
    val foreground by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface)
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = foreground,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            // toggleable reports the on/off state itself, in the system language.
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

/** A read-only tag on a list row. */
@Composable
fun AdaptiveTag(text: String) {
    if (!LocalIsIos.current) {
        SuggestionChip(
            onClick = {},
            label = { Text(text, style = MaterialTheme.typography.labelSmall) }
        )
        return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

// ── Picker ───────────────────────────────────────────────────────────────────

/**
 * Choose one of [options]. Android: an exposed dropdown field. iOS: a form row showing the value
 * in the tint colour that opens a menu, like a SwiftUI Picker in a Form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePicker(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    if (!LocalIsIos.current) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        return
    }

    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = IosMetrics.RowMinHeight)
                .clip(RoundedCornerShape(IosMetrics.CardCorner))
                .background(colors.surfaceVariant)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = IosMetrics.RowHorizontalPadding, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(selected, style = MaterialTheme.typography.bodyLarge, color = colors.primary)
            Spacer(Modifier.width(6.dp))
            UpDownChevrons(color = colors.primary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = colors.surfaceContainer
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = {
                        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                            if (option == selected) Checkmark(colors.onSurface, size = 13.dp)
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
                if (index < options.lastIndex) {
                    HorizontalDivider(thickness = IosMetrics.Hairline, color = colors.outlineVariant)
                }
            }
        }
    }
}
