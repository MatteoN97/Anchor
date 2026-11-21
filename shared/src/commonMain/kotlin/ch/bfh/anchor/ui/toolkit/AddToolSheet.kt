package ch.bfh.anchor.ui.toolkit

import androidx.compose.runtime.*
import ch.bfh.anchor.model.CopingTool
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
    val defaultCategory = stringResource(Res.string.tool_category_breathing)
    var name by remember { mutableStateOf(existingTool?.name ?: "") }
    var category by remember { mutableStateOf(existingTool?.category ?: defaultCategory) }
    var description by remember { mutableStateOf(existingTool?.description ?: "") }

    val categories = listOf(
        stringResource(Res.string.tool_category_breathing),
        stringResource(Res.string.tool_category_physical),
        stringResource(Res.string.tool_category_social),
        stringResource(Res.string.tool_category_grounding),
        stringResource(Res.string.tool_category_creative),
        stringResource(Res.string.tool_category_other)
    )

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
            options = categories,
            selected = category,
            onSelect = { category = it }
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
