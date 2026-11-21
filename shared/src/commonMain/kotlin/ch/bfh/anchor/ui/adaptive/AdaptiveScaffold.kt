package ch.bfh.anchor.ui.adaptive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.bfh.anchor.ui.theme.IosMetrics
import ch.bfh.anchor.ui.theme.LocalIsIos

/** The one primary action of a page: a floating round button at the bottom right on both platforms. */
data class PageAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * A top-level tab page.
 *
 * Android: Scaffold with a TopAppBar and a FAB for [action].
 * iOS: a navigation bar whose small centred title fades in once [listState] has scrolled the
 * large title away. The page's list must start with [largeTitle] for that to work, which renders
 * nothing on Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePageScaffold(
    title: String,
    listState: LazyListState,
    action: PageAction? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    if (!LocalIsIos.current) {
        Scaffold(
            topBar = { TopAppBar(title = { Text(title) }) },
            floatingActionButton = {
                if (action != null) {
                    FloatingActionButton(onClick = action.onClick) {
                        Icon(action.icon, contentDescription = action.label)
                    }
                }
            },
            content = content
        )
        return
    }

    val thresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    val collapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > thresholdPx
        }
    }
    Scaffold(
        topBar = { IosNavigationBar(title = title, showTitle = collapsed) },
        floatingActionButton = {
            if (action != null) {
                IosFloatingButton(icon = action.icon, label = action.label, onClick = action.onClick)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}

@Composable
private fun IosNavigationBar(title: String, showTitle: Boolean) {
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(52.dp)
            .padding(horizontal = IosMetrics.RowHorizontalPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp)
                .alpha(titleAlpha)
        )
    }
}

/** The page's primary action on iOS: a round, tinted button floating above the tab bar. */
@Composable
private fun IosFloatingButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(56.dp)
            .pressDim(source, pressedAlpha = 0.6f)
            .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.22f))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
    }
}

/** The iOS large title, as the first item of a page's list. Renders nothing on Android. */
fun LazyListScope.largeTitle(title: String) {
    item(key = "large-title", contentType = "large-title") {
        if (LocalIsIos.current) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(start = IosMetrics.RowHorizontalPadding, end = IosMetrics.RowHorizontalPadding, bottom = 12.dp)
                    .semantics { heading() }
            )
        }
    }
}

// ── Tab bar ──────────────────────────────────────────────────────────────────

data class TabItem(val label: String, val icon: ImageVector)

/**
 * Android: a Material NavigationBar. iOS: the floating capsule tab bar of iOS 26, with the selected
 * tab tinted on a soft pill and the others in the label colour.
 */
@Composable
fun AdaptiveTabBar(tabs: List<TabItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    if (!LocalIsIos.current) {
        NavigationBar {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
        return
    }

    val colors = MaterialTheme.colorScheme
    // The bar floats just above the home indicator rather than below the full safe area.
    val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = maxOf(bottomInset - 14.dp, 10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(18.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.14f))
                .clip(CircleShape)
                .background(colors.surfaceVariant.copy(alpha = 0.96f))
                .border(IosMetrics.Hairline, colors.outlineVariant.copy(alpha = 0.4f), CircleShape)
                .padding(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val pill by animateFloatAsState(if (selected) 1f else 0f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colors.secondaryContainer.copy(alpha = colors.secondaryContainer.alpha * pill))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        )
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val tint = if (selected) colors.primary else colors.onSurface
                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Sheet ────────────────────────────────────────────────────────────────────

/**
 * A modal form: Cancel on the left, the title in the middle, the confirming action on the right.
 * iOS adds the grey form background, a grabber and the iOS sheet corner radius.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    title: String,
    cancelLabel: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val isIos = LocalIsIos.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isIos) colors.background else BottomSheetDefaults.ContainerColor,
        shape = if (isIos) MaterialTheme.shapes.extraLarge.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
        ) else BottomSheetDefaults.ExpandedShape,
        dragHandle = if (isIos) {
            {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(colors.outline)
                )
            }
        } else {
            { BottomSheetDefaults.DragHandle() }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                AnchorTextButton(onClick = onDismiss) { Text(cancelLabel) }
            }
            Text(
                text = title,
                style = if (isIos) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.4f)
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                AnchorTextButton(onClick = onConfirm, enabled = confirmEnabled, emphasized = true) {
                    Text(confirmLabel)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isIos) 16.dp else 24.dp)
                .padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(if (isIos) 20.dp else 16.dp),
            content = content
        )
    }
}
