package ch.bfh.anchor.ui.toolkit

import androidx.compose.runtime.*
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.model.ToolCategory
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AdaptivePicker
import ch.bfh.anchor.ui.adaptive.AdaptiveSheet
import ch.bfh.anchor.ui.adaptive.AdaptiveTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddToolSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, description: String) -> Unit,
    existingTool: CopingTool? = null
) {
    var name by remember { mutableStateOf(existingTool?.name ?: "") }
    // Holds the category key; only the picker shows the translated label.
    var category by remember { mutableStateOf(existingTool?.category ?: ToolCategory.Breathing.key) }
    var description by remember { mutableStateOf(existingTool?.description ?: "") }

    val labels = ToolCategory.entries.associate { it.key to stringResource(it.labelRes) }

    AdaptiveSheet(
        title = if (existingTool != null) stringResource(Res.string.toolkit_edit_tool)
        else stringResource(Res.string.toolkit_new_tool),
        cancelLabel = stringResource(Res.string.common_cancel),
        confirmLabel = stringResource(Res.string.common_save),
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(name.trim(), category, description.trim()) }
    ) {
        AdaptiveTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(Res.string.toolkit_name_label),
            placeholder = stringResource(Res.string.toolkit_name_placeholder),
            singleLine = true
        )

        AdaptivePicker(
            label = stringResource(Res.string.toolkit_category_label),
            options = labels.values.toList(),
            selected = labels[category] ?: category,
            onSelect = { label -> category = labels.entries.first { it.value == label }.key }
        )

        AdaptiveTextField(
            value = description,
            onValueChange = { description = it },
            label = stringResource(Res.string.toolkit_description_label),
            placeholder = stringResource(Res.string.toolkit_description_placeholder),
            minLines = 3,
            maxLines = 6
        )
    }
}

/** A tool's category in the user's language; categories the app does not know are shown as stored. */
@Composable
fun categoryLabel(key: String): String =
    ToolCategory.fromKey(key)?.let { stringResource(it.labelRes) } ?: key
