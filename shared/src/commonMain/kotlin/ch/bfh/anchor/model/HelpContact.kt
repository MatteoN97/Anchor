package ch.bfh.anchor.model

/** Someone to call when the crisis flow has not been enough: a crisis line or a trusted person. */
data class HelpContact(val name: String, val number: String)

/** A crisis line Anchor knows about for one country, identified by ISO 3166-1 alpha-2 code. */
data class RegionalCrisisLine(
    val countryCode: String,
    val countryName: String,
    val contact: HelpContact
)

/**
 * Free, general-purpose emotional-support or suicide-prevention lines, one per country.
 *
 * Only lines reachable nationwide on a short or toll-free number are listed. Verify against the
 * operator's website before changing an entry: a wrong number here is reached at the worst moment.
 */
object CrisisLines {
    val all: List<RegionalCrisisLine> = listOf(
        RegionalCrisisLine("CH", "Switzerland", HelpContact("La Main Tendue / Die Dargebotene Hand", "143")),
        RegionalCrisisLine("DE", "Germany", HelpContact("TelefonSeelsorge", "0800 111 0 111")),
        RegionalCrisisLine("AT", "Austria", HelpContact("TelefonSeelsorge", "142")),
        RegionalCrisisLine("FR", "France", HelpContact("Numéro national de prévention du suicide", "3114")),
        RegionalCrisisLine("ES", "Spain", HelpContact("Línea de atención a la conducta suicida", "024")),
        RegionalCrisisLine("NL", "Netherlands", HelpContact("113 Zelfmoordpreventie", "113")),
        RegionalCrisisLine("GB", "United Kingdom", HelpContact("Samaritans", "116 123")),
        RegionalCrisisLine("IE", "Ireland", HelpContact("Samaritans", "116 123")),
        RegionalCrisisLine("US", "United States", HelpContact("988 Suicide & Crisis Lifeline", "988")),
        RegionalCrisisLine("CA", "Canada", HelpContact("9-8-8 Suicide Crisis Helpline", "988")),
        RegionalCrisisLine("AU", "Australia", HelpContact("Lifeline", "13 11 14")),
        RegionalCrisisLine("NZ", "New Zealand", HelpContact("Need to talk?", "1737"))
    )

    /** Used when the region is unknown or not listed. Anchor is developed in Switzerland. */
    val fallback: RegionalCrisisLine = all.first { it.countryCode == "CH" }

    fun forRegion(countryCode: String?): RegionalCrisisLine? =
        countryCode?.uppercase()?.let { code -> all.firstOrNull { it.countryCode == code } }
}

/** The crisis line in effect: the user's own choice, else the one for their region, else [CrisisLines.fallback]. */
fun effectiveCrisisLine(custom: HelpContact?, region: String?): HelpContact =
    custom ?: (CrisisLines.forRegion(region) ?: CrisisLines.fallback).contact
