package ch.bfh.anchor.model

enum class Feeling(
    val label: String,
    val emoji: String,
    val matchingCategories: List<String>,
    val skipBreathing: Boolean = false
) {
    ANXIOUS(
        label = "Anxious or overwhelmed",
        emoji = "😰",
        matchingCategories = listOf("Breathing", "Grounding"),
        skipBreathing = false
    ),
    SAD(
        label = "Sad or hopeless",
        emoji = "😢",
        matchingCategories = listOf("Social", "Grounding"),
        skipBreathing = true
    ),
    ANGRY(
        label = "Angry or frustrated",
        emoji = "😡",
        matchingCategories = listOf("Physical"),
        skipBreathing = true
    ),
    NUMB(
        label = "Numb or disconnected",
        emoji = "😶",
        matchingCategories = listOf("Grounding", "Physical"),
        skipBreathing = false
    ),
    EXHAUSTED(
        label = "Exhausted",
        emoji = "😴",
        matchingCategories = listOf("Grounding"),
        skipBreathing = true
    )
}
