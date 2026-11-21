package ch.bfh.anchor.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.IosMetrics
import ch.bfh.anchor.ui.theme.LocalIsIos

/**
 * A titled group of rows. iOS: an inset-grouped section (white rounded block on the grey page,
 * uppercase caption above, footnote below, inset hairlines between rows). Android: a header,
 * the rows, and full-width dividers.
 *
 * Rows stay individual lazy items, so long journals still scroll lazily.
 */
fun <T> LazyListScope.adaptiveSection(
    items: List<T>,
    key: ((T) -> Any)? = null,
    header: String? = null,
    footer: String? = null,
    separatorInset: Dp = IosMetrics.SeparatorInset,
    row: @Composable (T) -> Unit
) {
    if (header != null) {
        item(contentType = "section-header") { SectionHeader(header) }
    }
    itemsIndexed(
        items,
        key = key?.let { k -> { _: Int, item: T -> k(item) } },
        contentType = { _, _ -> "section-row" }
    ) { index, item ->
        SectionRow(
            isFirst = index == 0,
            isLast = index == items.lastIndex,
            separatorInset = separatorInset
        ) { row(item) }
    }
    item(contentType = "section-footer") { SectionFooter(footer) }
}

/** A section with fixed, hand-written rows (settings). */
fun LazyListScope.adaptiveSection(
    header: String? = null,
    footer: String? = null,
    separatorInset: Dp = IosMetrics.SeparatorInset,
    rows: List<@Composable () -> Unit>
) = adaptiveSection(
    items = rows,
    header = header,
    footer = footer,
    separatorInset = separatorInset
) { it() }

@Composable
private fun SectionHeader(text: String) {
    if (LocalIsIos.current) {
        SectionCaption(text, modifier = Modifier.padding(horizontal = IosMetrics.RowHorizontalPadding))
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun SectionFooter(text: String?) {
    val isIos = LocalIsIos.current
    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = if (isIos) 32.dp else 16.dp,
                end = if (isIos) 32.dp else 16.dp,
                top = 7.dp
            )
        )
    }
    // Space between sections; iOS separates them generously.
    Spacer(Modifier.height(if (isIos) 28.dp else 8.dp))
}

@Composable
private fun SectionRow(
    isFirst: Boolean,
    isLast: Boolean,
    separatorInset: Dp,
    content: @Composable () -> Unit
) {
    if (!LocalIsIos.current) {
        Column {
            content()
            HorizontalDivider()
        }
        return
    }
    val r = IosMetrics.CardCorner
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(r)
        isFirst -> RoundedCornerShape(topStart = r, topEnd = r)
        isLast -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        else -> RectangleShape
    }
    Column(
        modifier = Modifier
            .padding(horizontal = IosMetrics.RowHorizontalPadding)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        content()
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = separatorInset),
                thickness = IosMetrics.Hairline,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * One list row. iOS: a 44pt-minimum cell with the cell highlight on press and an optional
 * disclosure chevron. Android: a Material ListItem.
 */
@Composable
fun AdaptiveListRow(
    headline: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supporting: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false
) {
    if (!LocalIsIos.current) {
        ListItem(
            headlineContent = headline,
            supportingContent = supporting,
            leadingContent = leading,
            trailingContent = trailing,
            modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = IosMetrics.RowMinHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                start = if (leading != null) 8.dp else IosMetrics.RowHorizontalPadding,
                end = IosMetrics.RowHorizontalPadding,
                top = 11.dp,
                bottom = 11.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) { headline() }
            if (supporting != null) {
                // Content colour too, not just the style, so a Text with its own style stays grey.
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { supporting() }
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(12.dp))
            Chevron()
        }
    }
}

/** A row that is an action, like "Remove" or "Use automatic": tinted text, nothing else. */
@Composable
fun AdaptiveActionRow(text: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.primary) {
    AdaptiveListRow(
        headline = { Text(text, color = color) },
        onClick = onClick
    )
}

/**
 * Wraps an editable list row. iOS: swipe left to delete, as in Mail or Notes, with tapping the
 * row left to the caller. Android: unchanged, the row shows its own edit and delete buttons
 * (see [RowEditActions]).
 */
@Composable
fun SwipeToDelete(onDelete: () -> Unit, deleteLabel: String, content: @Composable () -> Unit) {
    if (!LocalIsIos.current) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = deleteLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) { content() }
    }
}

/** Edit and delete buttons at the end of a row. Android only: iOS rows tap to edit and swipe to delete. */
@Composable
fun RowEditActions(
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
    editLabel: String,
    deleteLabel: String
) {
    if (LocalIsIos.current) return
    Row {
        if (onEdit != null) {
            AnchorIconButton(onClick = onEdit) {
                Icon(AnchorIcons.Edit, contentDescription = editLabel, tint = MaterialTheme.colorScheme.primary)
            }
        }
        AnchorIconButton(onClick = onDelete) {
            Icon(AnchorIcons.Delete, contentDescription = deleteLabel, tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** Centred placeholder for an empty list, as a lazy item. */
fun LazyListScope.emptyState(title: String, subtitle: String) {
    item(contentType = "empty") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
