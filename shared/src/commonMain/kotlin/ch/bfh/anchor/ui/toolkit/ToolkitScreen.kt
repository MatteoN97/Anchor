package ch.bfh.anchor.ui.toolkit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePageScaffold
import ch.bfh.anchor.ui.adaptive.PageAction
import ch.bfh.anchor.ui.adaptive.RowEditActions
import ch.bfh.anchor.ui.adaptive.SwipeToDelete
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.adaptive.emptyState
import ch.bfh.anchor.ui.adaptive.largeTitle
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToolkitScreen(viewModel: AnchorViewModel) {
    val tools by viewModel.tools.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var toolToEdit by remember { mutableStateOf<CopingTool?>(null) }
    val listState = rememberLazyListState()
    val isIos = LocalIsIos.current

    val title = stringResource(Res.string.toolkit_title)
    val editLabel = stringResource(Res.string.common_edit)
    val deleteLabel = stringResource(Res.string.common_delete)
    val emptyTitle = stringResource(Res.string.toolkit_empty_title)
    val emptySubtitle = stringResource(Res.string.toolkit_empty_subtitle)

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
            // Room below the last row so the floating "+" never covers it.
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            largeTitle(title)
            if (tools.isEmpty()) emptyState(emptyTitle, emptySubtitle)
            else adaptiveSection(items = tools, key = { it.id }) { tool ->
                SwipeToDelete(onDelete = { viewModel.deleteTool(tool) }, deleteLabel = deleteLabel) {
                    AdaptiveListRow(
                        headline = {
                            Text(
                                tool.name,
                                style = if (isIos) MaterialTheme.typography.bodyLarge
                                else MaterialTheme.typography.titleMedium
                            )
                        },
                        supporting = {
                            Column {
                                Text(
                                    tool.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (tool.description.isNotEmpty()) {
                                    Text(
                                        tool.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailing = {
                            RowEditActions(
                                onEdit = { toolToEdit = tool },
                                onDelete = { viewModel.deleteTool(tool) },
                                editLabel = editLabel,
                                deleteLabel = deleteLabel
                            )
                        },
                        // iOS rows open the editor on tap; Android keeps its edit button.
                        onClick = if (isIos) ({ toolToEdit = tool }) else null
                    )
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
}
