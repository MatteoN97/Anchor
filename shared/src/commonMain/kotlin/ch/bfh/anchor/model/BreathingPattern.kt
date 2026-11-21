package ch.bfh.anchor.model

import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.StringResource

enum class BreathPhase(val labelRes: StringResource) {
    Inhale(Res.string.breath_phase_inhale),
    HoldIn(Res.string.breath_phase_hold),
    Exhale(Res.string.breath_phase_exhale),
    HoldOut(Res.string.breath_phase_hold)
}

data class BreathStep(val phase: BreathPhase, val durationMs: Int)

enum class BreathingPattern(
    val nameRes: StringResource,
    val descriptionRes: StringResource,
    /** Rounds for a standalone practice session, roughly one minute. */
    val practiceCycles: Int,
    val steps: List<BreathStep>
) {
    /** Longer exhale than inhale. The gentlest option and the default. */
    Calm(
        Res.string.breathing_pattern_calm_name,
        Res.string.breathing_pattern_calm_description,
        6,
        listOf(
            BreathStep(BreathPhase.Inhale, 4000),
            BreathStep(BreathPhase.Exhale, 6000)
        )
    ),
    Box(
        Res.string.breathing_pattern_box_name,
        Res.string.breathing_pattern_box_description,
        4,
        listOf(
            BreathStep(BreathPhase.Inhale, 4000),
            BreathStep(BreathPhase.HoldIn, 4000),
            BreathStep(BreathPhase.Exhale, 4000),
            BreathStep(BreathPhase.HoldOut, 4000)
        )
    ),
    FourSevenEight(
        Res.string.breathing_pattern_478_name,
        Res.string.breathing_pattern_478_description,
        4,
        listOf(
            BreathStep(BreathPhase.Inhale, 4000),
            BreathStep(BreathPhase.HoldIn, 7000),
            BreathStep(BreathPhase.Exhale, 8000)
        )
    );
}
