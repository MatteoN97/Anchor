package ch.bfh.anchor.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuiltInToolsTest {

    private fun tool(name: String, category: String) =
        CopingTool(name = name, category = category, description = "")

    @Test
    fun fallsBackToTwoBuiltInsWhenTheToolkitIsEmpty() {
        val suggestions = BuiltInTools.suggestionsFor(Feeling.ANXIOUS, emptyList())

        assertEquals(2, suggestions.tools.size)
        assertEquals(suggestions.tools, suggestions.builtIn)
    }

    @Test
    fun topsUpASingleMatchingToolToTwo() {
        val mine = tool("Box breathing", "Breathing")

        val suggestions = BuiltInTools.suggestionsFor(Feeling.ANXIOUS, listOf(mine))

        assertEquals(2, suggestions.tools.size)
        assertEquals(1, suggestions.builtIn.size)
        assertTrue(mine in suggestions.tools)
        assertFalse(mine in suggestions.builtIn)
    }

    @Test
    fun suggestsOnlyTheUsersOwnToolsOnceThereAreTwoMatches() {
        val mine = listOf(
            tool("Box breathing", "Breathing"),
            tool("Cold water", "Breathing"),
            tool("Name five things", "Grounding")
        )

        val suggestions = BuiltInTools.suggestionsFor(Feeling.ANXIOUS, mine)

        assertEquals(3, suggestions.tools.size)
        assertTrue(suggestions.builtIn.isEmpty())
        assertTrue(suggestions.tools.all { it in mine })
    }

    @Test
    fun ignoresToolsFromCategoriesTheFeelingDoesNotMatch() {
        // Angry matches Physical only, so a breathing tool must not be suggested.
        val suggestions = BuiltInTools.suggestionsFor(
            Feeling.ANGRY,
            listOf(tool("Box breathing", "Breathing"))
        )

        assertTrue(suggestions.tools.none { it.category == "Breathing" })
        assertEquals(2, suggestions.tools.size)
    }

    @Test
    fun everyFeelingHasAFallbackAndAMatchingCategory() {
        Feeling.entries.forEach { feeling ->
            val suggestions = BuiltInTools.suggestionsFor(feeling, emptyList())
            assertEquals(2, suggestions.tools.size, "no fallback tools for $feeling")
            assertTrue(feeling.matchingCategories.isNotEmpty(), "no categories for $feeling")
            assertTrue(
                suggestions.tools.all { it.category in feeling.matchingCategories },
                "a fallback for $feeling is in a category the feeling does not match"
            )
        }
    }
}
