package ch.bfh.anchor.ui.toolkit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePageScaffold
import ch.bfh.anchor.ui.adaptive.PageAction
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.adaptive.largeTitle
import ch.bfh.anchor.ui.components.calmCardColor
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Shared Android/iOS Toolkit: personal coping tools are always visible first; both optional
 * sections start collapsed. None of the interactions use Android-only or iOS-only APIs.
 */
@Composable
fun ToolkitScreen(viewModel: AnchorViewModel) {
    val tools by viewModel.tools.collectAsState()
    val inactiveTools by viewModel.inactiveTools.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var toolToEdit by remember { mutableStateOf<CopingTool?>(null) }
    var toolToDelete by remember { mutableStateOf<CopingTool?>(null) }
    var showInactive by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var expandedPersonalToolId by remember { mutableStateOf<String?>(null) }
    var pendingSaves by remember { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()

    val title = stringResource(Res.string.toolkit_title)
    val personalTitle = stringResource(Res.string.guided_personal_title)
    val suggestionsTitle = stringResource(Res.string.guided_suggestions_title)
    val inactiveTitle = stringResource(Res.string.toolkit_inactive_title)

    // Resolve titles in the current locale. Keep suggested activities out of this list once
    // they are active personal tools (also when they were renamed after being saved).
    // Pending saves are hidden immediately while Room emits the newly inserted row.
    val guidedTitles = guidedActivities.map { stringResource(it.titleRes).trim() }
    val availableGuidedActivities = guidedActivities.filterIndexed { index, activity ->
        val localizedTitle = guidedTitles[index]
        activity.savedToolId !in pendingSaves && tools.none { tool ->
            tool.id == activity.savedToolId ||
                tool.name.trim().equals(localizedTitle, ignoreCase = true)
        }
    }

    // Remain disabled after a save tap until the new row reaches Room's active Flow.
    LaunchedEffect(tools) {
        val savedIds = tools.mapTo(mutableSetOf()) { it.id }
        pendingSaves = pendingSaves - savedIds
    }

    AdaptivePageScaffold(
        title = title,
        listState = listState,
        action = PageAction(
            icon = AnchorIcons.Add,
            label = stringResource(Res.string.toolkit_add_tool),
            onClick = { showAddSheet = true }
        )
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            largeTitle(title)
            if (tools.isEmpty()) {
                adaptiveSection(header = personalTitle, items = listOf(Unit)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(Res.string.guided_personal_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showAddSheet = true }) {
                            Icon(AnchorIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.toolkit_add_tool))
                        }
                    }
                }
            } else {
                adaptiveSection(header = personalTitle, items = tools, key = { it.id }) { tool ->
                    PersonalToolRow(
                        tool = tool,
                        inactive = false,
                        expanded = expandedPersonalToolId == tool.id,
                        onToggle = {
                            expandedPersonalToolId = if (expandedPersonalToolId == tool.id) null else tool.id
                        },
                        onEdit = { toolToEdit = tool },
                        onMove = {
                            expandedPersonalToolId = null
                            viewModel.setToolActive(tool, false)
                        },
                        onDelete = { toolToDelete = tool }
                    )
                }
            }

            item(key = "toolkit-inactive-toggle") {
                ToolkitSectionToggle(
                    title = inactiveTitle,
                    count = inactiveTools.size,
                    expanded = showInactive,
                    onToggle = {
                        showInactive = !showInactive
                        if (!showInactive) expandedPersonalToolId = null
                    }
                )
            }
            if (showInactive) {
                if (inactiveTools.isEmpty()) {
                    item(key = "toolkit-inactive-empty") {
                        Text(
                            stringResource(Res.string.toolkit_inactive_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    adaptiveSection(items = inactiveTools, key = { it.id }) { tool ->
                        PersonalToolRow(
                            tool = tool,
                            inactive = true,
                            expanded = expandedPersonalToolId == tool.id,
                            onToggle = {
                                expandedPersonalToolId = if (expandedPersonalToolId == tool.id) null else tool.id
                            },
                            onEdit = { toolToEdit = tool },
                            onMove = {
                                expandedPersonalToolId = null
                                viewModel.setToolActive(tool, true)
                            },
                            onDelete = { toolToDelete = tool }
                        )
                    }
                }
            }

            item(key = "toolkit-suggestions-toggle") {
                ToolkitSectionToggle(
                    title = suggestionsTitle,
                    count = availableGuidedActivities.size,
                    expanded = showSuggestions,
                    onToggle = {
                        showSuggestions = !showSuggestions
                        if (!showSuggestions) expandedKey = null
                    }
                )
            }
            if (showSuggestions) {
                if (availableGuidedActivities.isEmpty()) {
                    item(key = "toolkit-suggestions-empty") {
                        Text(
                            stringResource(Res.string.guided_all_saved),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    item(key = "toolkit-suggestions-intro") {
                        Text(
                            stringResource(Res.string.guided_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                        )
                    }
                    adaptiveSection(items = availableGuidedActivities, key = { it.key }) { activity ->
                        val titleText = stringResource(activity.titleRes).trim()
                        val archived = inactiveTools.firstOrNull {
                            it.id == activity.savedToolId ||
                                it.name.trim().equals(titleText, ignoreCase = true)
                        }
                        GuidedActivityRow(
                            activity = activity,
                            expanded = expandedKey == activity.key,
                            alreadySaved = false,
                            archivedTool = archived,
                            savePending = false,
                            onToggle = {
                                expandedKey = if (expandedKey == activity.key) null else activity.key
                            },
                            onRestore = {
                                if (archived != null) {
                                    expandedKey = null
                                    viewModel.setToolActive(archived, true)
                                }
                            },
                            onSave = { name, description ->
                                if (archived == null && activity.savedToolId !in pendingSaves) {
                                    expandedKey = null
                                    pendingSaves = pendingSaves + activity.savedToolId
                                    viewModel.addGuidedTool(
                                        id = activity.savedToolId,
                                        name = name,
                                        category = activity.category.key,
                                        description = description
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddToolSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, category, description ->
                viewModel.addTool(name, category, description)
                showAddSheet = false
            }
        )
    }
    toolToEdit?.let { tool ->
        AddToolSheet(
            existingTool = tool,
            onDismiss = { toolToEdit = null },
            onSave = { name, category, description ->
                viewModel.updateTool(tool.copy(name = name, category = category, description = description))
                toolToEdit = null
            }
        )
    }
    toolToDelete?.let { tool ->
        AlertDialog(
            onDismissRequest = { toolToDelete = null },
            title = { Text(stringResource(Res.string.toolkit_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.toolkit_delete_confirm_message, tool.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTool(tool)
                    if (expandedPersonalToolId == tool.id) expandedPersonalToolId = null
                    toolToDelete = null
                }) {
                    Text(
                        stringResource(Res.string.toolkit_delete_permanent),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { toolToDelete = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

/** Same accessible disclosure pattern on both platforms. */
@Composable
private fun ToolkitSectionToggle(
    title: String,
    count: Int?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val stateLabel = stringResource(
        if (expanded) Res.string.guided_expanded else Res.string.guided_collapsed
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        color = calmCardColor(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = stateLabel }
                .clickable(onClick = onToggle)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (count != null) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                AnchorIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(if (expanded) 90f else 0f)
            )
        }
    }
}

/**
 * A personal tool is a read-only disclosure row on both platforms. Tapping the row only
 * expands/collapses its description; editing must be started with the explicit Edit button.
 * The actions menu contains archive/restore and confirmed permanent deletion only.
 */
@Composable
private fun PersonalToolRow(
    tool: CopingTool,
    inactive: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val isIos = LocalIsIos.current
    val editLabel = stringResource(Res.string.common_edit)
    val optionsLabel = stringResource(Res.string.toolkit_options)
    val moveLabel = stringResource(
        if (inactive) Res.string.toolkit_restore else Res.string.toolkit_archive
    )
    val stateLabel = stringResource(
        if (expanded) Res.string.guided_expanded else Res.string.guided_collapsed
    )
    var menuExpanded by remember(tool.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AdaptiveListRow(
            headline = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tool.name,
                        style = if (isIos) MaterialTheme.typography.bodyLarge
                        else MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        AnchorIcons.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).rotate(if (expanded) 90f else 0f)
                    )
                }
            },
            supporting = {
                Text(
                    categoryLabel(tool.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Keep editing explicit, visually compact, and accessible by name.
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            AnchorIcons.Edit,
                            contentDescription = editLabel,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Text(
                                "⋮",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { contentDescription = optionsLabel }
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(moveLabel) },
                                onClick = { menuExpanded = false; onMove() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(Res.string.toolkit_delete_permanent),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.semantics { stateDescription = stateLabel },
            onClick = onToggle
        )
        if (expanded) {
            val descriptionText = if (tool.description.isNotBlank()) tool.description
            else stringResource(Res.string.toolkit_description_placeholder)
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun GuidedActivityRow(
    activity: GuidedActivity,
    expanded: Boolean,
    alreadySaved: Boolean,
    savePending: Boolean,
    archivedTool: CopingTool?,
    onToggle: () -> Unit,
    onRestore: () -> Unit,
    onSave: (name: String, description: String) -> Unit
) {
    val name = stringResource(activity.titleRes)
    val summary = stringResource(activity.summaryRes)
    val meta = stringResource(activity.metaRes)
    val intro = stringResource(activity.introRes)
    val closing = stringResource(activity.closingRes)
    val example = activity.exampleRes?.let { stringResource(it) }
    val steps = activity.steps.map { it.marker to stringResource(it.textRes) }
    val saved = alreadySaved || savePending
    val archived = archivedTool != null
    val expandedLabel = stringResource(Res.string.guided_expanded)
    val collapsedLabel = stringResource(Res.string.guided_collapsed)

    Column(modifier = Modifier.fillMaxWidth()) {
        AdaptiveListRow(
            headline = {
                Text(name, style = MaterialTheme.typography.titleMedium)
            },
            supporting = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(summary, style = MaterialTheme.typography.bodyMedium)
                }
            },
            leading = {
                Icon(activity.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailing = {
                Icon(
                    AnchorIcons.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 90f else 0f)
                )
            },
            modifier = Modifier.semantics {
                stateDescription = if (expanded) expandedLabel else collapsedLabel
            },
            onClick = onToggle
        )

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                steps.forEach { (marker, instruction) ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    marker,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(top = 5.dp)
                        )
                    }
                }
                if (example != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(
                                stringResource(Res.string.guided_example_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(example, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text(
                    closing,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        if (archived) {
                            onRestore()
                        } else if (!saved) {
                            val description = buildString {
                                append(intro)
                                append("\n\n")
                                steps.forEach { (marker, instruction) ->
                                    append(marker).append(". ").append(instruction).append('\n')
                                }
                                if (example != null) append('\n').append(example).append('\n')
                                append('\n').append(closing)
                            }
                            onSave(name, description)
                        }
                    },
                    enabled = !saved,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        if (saved) AnchorIcons.Check else AnchorIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(
                            when {
                                saved -> Res.string.crisis_suggestions_added
                                archived -> Res.string.toolkit_restore
                                else -> Res.string.crisis_suggestions_add
                            }
                        )
                    )
                }
            }
        }
    }
}
