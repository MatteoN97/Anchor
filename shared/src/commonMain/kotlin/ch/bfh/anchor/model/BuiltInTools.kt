package ch.bfh.anchor.model

object BuiltInTools {
    private val byFeeling: Map<Feeling, List<CopingTool>> = mapOf(
        Feeling.ANXIOUS to listOf(
            CopingTool(name = "4-7-8 Breathing", category = "Breathing",
                description = "Inhale 4s, hold 7s, exhale 8s. Activates the parasympathetic nervous system."),
            CopingTool(name = "5-4-3-2-1 Grounding", category = "Grounding",
                description = "Name 5 things you see, 4 you hear, 3 you can touch, 2 you smell, 1 you taste.")
        ),
        Feeling.SAD to listOf(
            CopingTool(name = "Reach out", category = "Social",
                description = "Text or call someone you trust, even just to say hi."),
            CopingTool(name = "Comfort object", category = "Grounding",
                description = "Hold something soft or familiar. Let yourself feel it.")
        ),
        Feeling.ANGRY to listOf(
            CopingTool(name = "Cold water", category = "Physical",
                description = "Splash cold water on your face or hold ice. It triggers the dive reflex and slows your heart rate."),
            CopingTool(name = "Move your body", category = "Physical",
                description = "Even 60 seconds of jumping jacks or walking can release tension.")
        ),
        Feeling.NUMB to listOf(
            CopingTool(name = "5-4-3-2-1 Grounding", category = "Grounding",
                description = "Name 5 things you see, 4 you hear, 3 you can touch, 2 you smell, 1 you taste."),
            CopingTool(name = "Temperature change", category = "Physical",
                description = "Hold something cold or warm. Focus entirely on the sensation.")
        ),
        Feeling.EXHAUSTED to listOf(
            CopingTool(name = "One thing only", category = "Grounding",
                description = "Pick one tiny thing to do. Just one. Nothing else matters right now."),
            CopingTool(name = "Rest without guilt", category = "Grounding",
                description = "Lie down. You don't have to be productive right now.")
        )
    )

    data class Suggestions(val tools: List<CopingTool>, val builtIn: List<CopingTool>)

    fun suggestionsFor(feeling: Feeling, userTools: List<CopingTool>): Suggestions {
        val matching = userTools.filter { it.category in feeling.matchingCategories }
        val fallbacks = byFeeling[feeling] ?: emptyList()

        return if (matching.size >= 2) {
            Suggestions(tools = matching.shuffled().take(3), builtIn = emptyList())
        } else {
            val needed = maxOf(0, 2 - matching.size)
            val builtInToShow = fallbacks.take(needed)
            Suggestions(tools = matching + builtInToShow, builtIn = builtInToShow)
        }
    }
}
