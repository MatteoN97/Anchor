package ch.bfh.anchor.ui.toolkit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import ch.bfh.anchor.ui.icons.AnchorIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.viewmodel.AnchorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolkitScreen(viewModel: AnchorViewModel) {
    val tools by viewModel.tools.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var toolToEdit by remember { mutableStateOf<CopingTool?>(null) }  // ← add this

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.toolkit_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(AnchorIcons.Add, contentDescription = stringResource(Res.string.toolkit_add_tool))
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(tools, key = { it.id }) { tool ->
                ListItem(
                    headlineContent = {
                        Text(tool.name, style = MaterialTheme.typography.titleMedium)
                    },
                    supportingContent = {
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
                    trailingContent = {
                        Row {
                            IconButton(onClick = { toolToEdit = tool }) {  // ← edit button
                                Icon(
                                    AnchorIcons.Edit,
                                    contentDescription = stringResource(Res.string.common_edit),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTool(tool) }) {
                                Icon(
                                    AnchorIcons.Delete,
                                    contentDescription = stringResource(Res.string.common_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    // Add sheet
    if (showAddSheet) {
        AddToolSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, category, description ->
                viewModel.addTool(name, category, description)
                showAddSheet = false
            }
        )
    }

    // Edit sheet
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