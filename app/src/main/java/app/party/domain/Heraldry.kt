package app.party.domain

/**
 * The party's coat of arms. Each hero answered five questions on their own; where the two agreed the
 * shield shows one thing, where they didn't it is split and both are shown (heraldry's own dimidiation).
 */
enum class Tincture(val metal: Boolean) {
    OR(true), ARGENT(true),
    GULES(false), AZURE(false), VERT(false), PURPURE(false), SABLE(false),
}

/** The field's colour: the hour of the day the hero likes the two of them best in. */
enum class Hour(val tincture: Tincture) {
    DAWN(Tincture.GULES),
    NOON(Tincture.AZURE),
    GREEN(Tincture.VERT),
    DUSK(Tincture.PURPURE),
    NIGHT(Tincture.SABLE),
}

/** The band across the shield: the landscape the hero would set out into. */
enum class Ordinary { BEND, CHEVRON, WAVES, CROSS, FESS }

/** The party's beast. */
enum class Beast { OWL, FOX, CAT, STAG, BEAR, RAVEN, HARE, WOLF }

/** The small charges in the chief. */
enum class Symbol { STAR, CRESCENT, HEART, KEY, ROSE, SUN, LEAF, BELL }

/** The two heroes' answers, A first. */
data class Crest(
    val hour: Pair<Hour, Hour>,
    val ordinary: Pair<Ordinary, Ordinary>,
    val beast: Pair<Beast, Beast>,
    val symbol: Pair<Symbol, Symbol>,
    /** Gold (true) or silver for the ordinary and the charges. */
    val gold: Pair<Boolean, Boolean>,
) {
    private fun <T> Pair<T, T>.agree() = first == second

    /** How many of the five questions the two answered alike. */
    val agreed: Int get() = listOf(hour.agree(), ordinary.agree(), beast.agree(), symbol.agree(), gold.agree()).count { it }

    val splitField: Boolean get() = !hour.agree()
    val splitOrdinary: Boolean get() = !ordinary.agree()
    val twoBeasts: Boolean get() = !beast.agree()
    val twoSymbols: Boolean get() = !symbol.agree()

    /** The ordinary in A's metal and the charges in B's, when they chose differently. */
    val ordinaryMetal: Tincture get() = if (gold.first) Tincture.OR else Tincture.ARGENT
    val chargeMetal: Tincture get() = if (gold.second) Tincture.OR else Tincture.ARGENT

    fun encode(): String = listOf(
        hour.first, hour.second, ordinary.first, ordinary.second, beast.first, beast.second,
        symbol.first, symbol.second,
    ).joinToString(",") { it.name } + ",${gold.first},${gold.second}"

    companion object {
        val DEFAULT = Crest(Hour.NOON to Hour.NOON, Ordinary.BEND to Ordinary.BEND, Beast.FOX to Beast.FOX, Symbol.STAR to Symbol.STAR, true to true)

        fun decode(s: String): Crest? = runCatching {
            val f = s.split(',')
            Crest(
                Hour.valueOf(f[0]) to Hour.valueOf(f[1]),
                Ordinary.valueOf(f[2]) to Ordinary.valueOf(f[3]),
                Beast.valueOf(f[4]) to Beast.valueOf(f[5]),
                Symbol.valueOf(f[6]) to Symbol.valueOf(f[7]),
                f[8].toBoolean() to f[9].toBoolean(),
            )
        }.getOrNull()
    }
}

/** What the party has earned that is added around the shield. */
data class Honours(
    /** A border round the shield (level 5). */
    val bordure: Boolean = false,
    /** A helm above it (level 8). */
    val helm: Boolean = false,
    /** Cloth mantling flowing from the helm (level 12). */
    val mantling: Boolean = false,
    /** A crown on the helm (level 16). */
    val crown: Boolean = false,
    /** A laurel wreath round it all (level 20). */
    val laurel: Boolean = false,
    /** A star in a canton for each campaign completed. */
    val campaigns: Int = 0,
    /** Dragons slain, shown as fangs along the base of the motto. */
    val dragons: Int = 0,
) {
    companion object {
        fun of(level: Int, campaigns: Int, dragons: Int) = Honours(
            bordure = level >= 5, helm = level >= 8, mantling = level >= 12, crown = level >= 16, laurel = level >= 20,
            campaigns = campaigns, dragons = dragons,
        )
    }
}
