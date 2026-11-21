package ch.bfh.anchor.ui.toolkit

import androidx.compose.runtime.*
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.ui.adaptive.AdaptiveFilterChip
import ch.bfh.anchor.ui.adaptive.AdaptiveSheet
import ch.bfh.anchor.ui.adaptive.AdaptiveTextField
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
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

        // Six short options: chips show them all at once, a dropdown hid them behind a tap.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.toolkit_category_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { option ->
                    AdaptiveFilterChip(
                        selected = option == category,
                        onClick = { category = option },
                        label = option
                    )
                }
            }
        }

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
