package ch.bfh.anchor.model

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.StringResource

object BuiltInTools {
    /**
     * A technique Anchor ships with. Its name and description are string resources, so they are
     * resolved where they are shown and appear in the user's language.
     */
    class Definition(
        val id: String,
        val category: ToolCategory,
        val nameRes: StringResource,
        val descriptionRes: StringResource
    ) {
        fun toTool(name: String, description: String) =
            CopingTool(id = id, name = name, category = category.key, description = description)
    }

    private val breathing478 = Definition("builtin-478", ToolCategory.Breathing,
        Res.string.builtin_478_name, Res.string.builtin_478_description)
    private val grounding54321 = Definition("builtin-54321", ToolCategory.Grounding,
        Res.string.builtin_54321_name, Res.string.builtin_54321_description)
    private val reachOut = Definition("builtin-reach-out", ToolCategory.Social,
        Res.string.builtin_reach_out_name, Res.string.builtin_reach_out_description)
    private val comfortObject = Definition("builtin-comfort-object", ToolCategory.Grounding,
        Res.string.builtin_comfort_object_name, Res.string.builtin_comfort_object_description)
    private val coldWater = Definition("builtin-cold-water", ToolCategory.Physical,
        Res.string.builtin_cold_water_name, Res.string.builtin_cold_water_description)
    private val moveYourBody = Definition("builtin-move", ToolCategory.Physical,
        Res.string.builtin_move_name, Res.string.builtin_move_description)
    private val temperatureChange = Definition("builtin-temperature", ToolCategory.Physical,
        Res.string.builtin_temperature_name, Res.string.builtin_temperature_description)
    private val oneThingOnly = Definition("builtin-one-thing", ToolCategory.Grounding,
        Res.string.builtin_one_thing_name, Res.string.builtin_one_thing_description)
    private val restWithoutGuilt = Definition("builtin-rest", ToolCategory.Grounding,
        Res.string.builtin_rest_name, Res.string.builtin_rest_description)

    private val byFeeling: Map<Feeling, List<Definition>> = mapOf(
        Feeling.ANXIOUS to listOf(breathing478, grounding54321),
        Feeling.SAD to listOf(reachOut, comfortObject),
        Feeling.ANGRY to listOf(coldWater, moveYourBody),
        Feeling.NUMB to listOf(grounding54321, temperatureChange),
        Feeling.EXHAUSTED to listOf(oneThingOnly, restWithoutGuilt)
    )

    /** Every built-in technique, once each. */
    val all: List<Definition> = byFeeling.values.flatten().distinctBy { it.id }

    data class Suggestions(val tools: List<CopingTool>, val builtIn: List<CopingTool>)

    /**
     * [resolve] turns a built-in definition into a tool with its text in the current language.
     * The default leaves the text empty, which is enough for anything that only looks at ids
     * and categories.
     */
    fun suggestionsFor(
        feeling: Feeling,
        userTools: List<CopingTool>,
        resolve: (Definition) -> CopingTool = { it.toTool(name = it.id, description = "") }
    ): Suggestions {
        val matching = userTools.filter { it.category in feeling.matchingCategories }
        val fallbacks = byFeeling[feeling].orEmpty().map(resolve)

        return if (matching.size >= 2) {
            Suggestions(tools = matching.shuffled().take(3), builtIn = emptyList())
        } else {
            val needed = maxOf(0, 2 - matching.size)
            val builtInToShow = fallbacks.take(needed)
            Suggestions(tools = matching + builtInToShow, builtIn = builtInToShow)
        }
    }
}
