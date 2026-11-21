package ch.bfh.anchor.model

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * The categories a coping tool can have. A tool stores [key], never the translated label:
 * the crisis flow matches tools to feelings by category, and a label saved in one language
 * would stop matching the moment the app is used in another.
 *
 * The keys are the English labels the first versions stored, so existing toolkits keep working.
 */
enum class ToolCategory(val key: String, val labelRes: StringResource) {
    Breathing("Breathing", Res.string.tool_category_breathing),
    Physical("Physical", Res.string.tool_category_physical),
    Social("Social", Res.string.tool_category_social),
    Grounding("Grounding", Res.string.tool_category_grounding),
    Creative("Creative", Res.string.tool_category_creative),
    Other("Other", Res.string.tool_category_other);

    companion object {
        fun fromKey(key: String): ToolCategory? = entries.firstOrNull { it.key == key }
    }
}
