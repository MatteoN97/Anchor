package ch.bfh.anchor.ui.toolkit

import androidx.compose.ui.graphics.vector.ImageVector
import ch.bfh.anchor.model.ToolCategory
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.icons.AnchorIcons
import org.jetbrains.compose.resources.StringResource

/** One numbered or lettered step of a [GuidedActivity]: "5", "S", "1"… and its instruction. */
data class GuidedStep(val marker: String, val textRes: StringResource)

/**
 * A suggested activity in the Toolkit, shown as an expandable row that can be saved as one of
 * the user's own tools. All text is a resource, so it follows the app language.
 *
 * [key] identifies the activity in the list. [savedToolId] is the id of the tool it becomes when
 * saved: stable across languages, so a saved activity is recognised whatever language it was
 * saved in, and a second tap cannot add it twice. [category] decides which feelings in the
 * crisis flow suggest the saved tool (see `Feeling.matchingCategories`).
 */
data class GuidedActivity(
    val key: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val titleRes: StringResource,
    val summaryRes: StringResource,
    val metaRes: StringResource,
    val introRes: StringResource,
    val steps: List<GuidedStep>,
    val closingRes: StringResource,
    val exampleRes: StringResource? = null
) {
    val savedToolId: String get() = "guided-$key"
}

/** The suggested activities, in the order the Toolkit lists them. */
val guidedActivities: List<GuidedActivity> = listOf(
    GuidedActivity(
        key = "grounding-54321",
        category = ToolCategory.Grounding,
        icon = AnchorIcons.SelfImprovement,
        // The built-in tool of the same technique already has this name in every language.
        titleRes = Res.string.builtin_54321_name,
        summaryRes = Res.string.guided_grounding_summary,
        metaRes = Res.string.guided_grounding_meta,
        introRes = Res.string.guided_grounding_intro,
        steps = listOf(
            GuidedStep("5", Res.string.guided_grounding_5),
            GuidedStep("4", Res.string.guided_grounding_4),
            GuidedStep("3", Res.string.guided_grounding_3),
            GuidedStep("2", Res.string.guided_grounding_2),
            GuidedStep("1", Res.string.guided_grounding_1)
        ),
        closingRes = Res.string.guided_grounding_closing
    ),
    GuidedActivity(
        key = "check-the-thought",
        // Worry: offered when the user feels anxious.
        category = ToolCategory.Grounding,
        icon = AnchorIcons.EditNote,
        titleRes = Res.string.guided_thought_title,
        summaryRes = Res.string.guided_thought_summary,
        metaRes = Res.string.guided_thought_meta,
        introRes = Res.string.guided_thought_intro,
        steps = listOf(
            GuidedStep("1", Res.string.guided_thought_1),
            GuidedStep("2", Res.string.guided_thought_2),
            GuidedStep("3", Res.string.guided_thought_3),
            GuidedStep("4", Res.string.guided_thought_4)
        ),
        closingRes = Res.string.guided_thought_closing,
        exampleRes = Res.string.guided_thought_example
    ),
    GuidedActivity(
        key = "one-small-step",
        // A small physical action: offered when the user feels numb.
        category = ToolCategory.Physical,
        icon = AnchorIcons.Mood,
        titleRes = Res.string.guided_step_title,
        summaryRes = Res.string.guided_step_summary,
        metaRes = Res.string.guided_step_meta,
        introRes = Res.string.guided_step_intro,
        steps = listOf(
            GuidedStep("1", Res.string.guided_step_1),
            GuidedStep("2", Res.string.guided_step_2),
            GuidedStep("3", Res.string.guided_step_3)
        ),
        closingRes = Res.string.guided_step_closing
    ),
    GuidedActivity(
        key = "stop",
        // Anger and strong emotions: the angry feeling routes to physical tools.
        category = ToolCategory.Physical,
        icon = AnchorIcons.Air,
        titleRes = Res.string.guided_stop_title,
        summaryRes = Res.string.guided_stop_summary,
        metaRes = Res.string.guided_stop_meta,
        introRes = Res.string.guided_stop_intro,
        steps = listOf(
            GuidedStep("S", Res.string.guided_stop_s),
            GuidedStep("T", Res.string.guided_stop_t),
            GuidedStep("O", Res.string.guided_stop_o),
            GuidedStep("P", Res.string.guided_stop_p)
        ),
        closingRes = Res.string.guided_stop_closing
    )
)
