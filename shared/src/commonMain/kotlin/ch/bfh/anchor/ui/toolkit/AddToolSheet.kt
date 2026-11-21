package ch.bfh.anchor.ui.toolkit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.model.CopingTool

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToolSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, description: String) -> Unit,
    existingTool: CopingTool? = null  // ← add this
) {
    val defaultCategory = stringResource(Res.string.tool_category_breathing)
    var name by remember { mutableStateOf(existingTool?.name ?: "") }
    var category by remember {
        mutableStateOf(existingTool?.category ?: defaultCategory)
    }
    var description by remember { mutableStateOf(existingTool?.description ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        stringResource(Res.string.tool_category_breathing),
        stringResource(Res.string.tool_category_physical),
        stringResource(Res.string.tool_category_social),
        stringResource(Res.string.tool_category_grounding),
        stringResource(Res.string.tool_category_creative),
        stringResource(Res.string.tool_category_other)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (existingTool != null) {
                    stringResource(Res.string.toolkit_edit_tool)
                } else {
                    stringResource(Res.string.toolkit_new_tool)
                },
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.toolkit_name_label)) },
                placeholder = { Text(stringResource(Res.string.toolkit_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.toolkit_category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(Res.string.toolkit_description_label)) },
                placeholder = { Text(stringResource(Res.string.toolkit_description_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.common_cancel))
                }
                Button(
                    onClick = { onSave(name.trim(), category, description.trim()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.common_save))
                }
            }
        }
    }
}