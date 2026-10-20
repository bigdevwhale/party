package app.party.data

import android.content.res.AssetManager

/** What a line of lore is. See docs/LORE.md. */
enum class Kind {
    /** A quest notice on the tavern board: a real date. */
    Q,

    /** A secret objective, dealt face down to one hero. */
    S,

    /** A wandering encounter on the road. */
    E,

    /** A question by the campfire. */
    T,

    /** The raven's errand for the day. */
    R,

    /** A fate of the d20: a natural 20 or a natural 1. */
    F,

    /** A campaign's header. */
    K,

    /** A campaign chapter. */
    C,
}

/** The eight schools a quest belongs to; the party grows a rank in each. */
enum class School(val key: String) {
    ROAD("road"),
    HEARTH("hearth"),
    CRAFT("craft"),
    ARTS("arts"),
    WILD("wild"),
    LORE("lore"),
    DARING("daring"),
    TENDER("tender");

    companion object {
        fun of(key: String): School? = entries.firstOrNull { it.key == key }
    }
}

/** How big a quest is: copper an hour or two, silver an evening, gold a day to remember. */
enum class Rank(val code: Char, val xp: Int) {
    COPPER('C', 120),
    SILVER('S', 220),
    GOLD('G', 400);

    companion object {
        fun of(c: Char): Rank? = entries.firstOrNull { it.code == c }
    }
}

/**
 * One line of the lore files. [parts] are the `|`-separated pieces of the text in each language, so a
 * quest is `title, in which…, story, tasks…`. [school] holds a campaign key for [Kind.K] and [Kind.C],
 * [order] a chapter number.
 */
data class Entry(
    val id: String,
    val kind: Kind,
    val school: String,
    val rank: String,
    val flags: Set<String>,
    val en: List<String>,
    val ru: List<String>,
) {
    fun parts(lang: String): List<String> = if (lang == "ru") ru else en
    fun text(lang: String): String = parts(lang).joinToString(" ")
    fun part(lang: String, i: Int): String = parts(lang).getOrElse(i) { "" }

    val schoolOf: School? get() = School.of(school)
    val rankOf: Rank? get() = rank.singleOrNull()?.let(Rank::of)
    val order: Int get() = rank.toIntOrNull() ?: 0
    val boon: Boolean get() = "+" in flags
}

/**
 * A quest as the party plays it, from the lore or written by hand: what it's called, how it goes into
 * the chronicle, the story and the tasks.
 */
data class QuestText(val title: String, val inWhich: String, val story: String, val tasks: List<String>, val epilogue: String = "")

/** Everything the game master can say, in both languages. */
class Lore(val entries: List<Entry>) {
    private val byId = entries.associateBy { it.id }
    operator fun get(id: String): Entry? = byId[id]

    fun of(kind: Kind): List<Entry> = entries.filter { it.kind == kind }

    val quests: List<Entry> by lazy { of(Kind.Q) }
    val secrets: List<Entry> by lazy { of(Kind.S) }
    val encounters: List<Entry> by lazy { of(Kind.E) }
    val campfire: List<Entry> by lazy { of(Kind.T) }
    val ravens: List<Entry> by lazy { of(Kind.R) }
    val fates: List<Entry> by lazy { of(Kind.F) }
    val campaigns: List<Entry> by lazy { of(Kind.K).sortedBy { it.school } }

    fun chapters(campaign: String): List<Entry> = entries.filter { it.kind == Kind.C && it.school == campaign }.sortedBy { it.order }
    fun quests(school: School): List<Entry> = quests.filter { it.school == school.key }.sortedBy { it.id }

    /** A quest or chapter as text; chapters keep their epilogue. */
    fun questText(e: Entry, lang: String): QuestText {
        val p = e.parts(lang)
        return if (e.kind == Kind.C) {
            QuestText(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.drop(3).dropLast(1), p.last())
        } else {
            QuestText(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.drop(3))
        }
    }

    companion object {
        const val DIR = "lore"

        fun load(assets: AssetManager): Lore {
            val files = assets.list(DIR).orEmpty().filter { it.endsWith(".tsv") }.sorted()
            return Lore(files.flatMap { f -> assets.open("$DIR/$f").use { parse(it.readBytes().decodeToString()) } })
        }

        /** Lines without all seven fields, or of an unknown kind, are skipped. */
        fun parse(text: String): List<Entry> = text.lineSequence().mapNotNull { line ->
            val f = line.trimEnd('\r').split('\t')
            if (f.size != 7) return@mapNotNull null
            val kind = runCatching { Kind.valueOf(f[1].trim()) }.getOrNull() ?: return@mapNotNull null
            Entry(
                id = f[0].trim(), kind = kind, school = f[2].trim(), rank = f[3].trim(),
                flags = f[4].split(',').map { it.trim() }.filter { it.isNotEmpty() && it != "-" }.toSet(),
                en = parts(f[5]),
                ru = parts(f[6]),
            )
        }.toList()

        /** Splits a text at `|`, leaving the bars inside `{…|…}` and `[…|…]` gender marks alone. */
        fun parts(text: String): List<String> {
            val out = mutableListOf<String>()
            val cur = StringBuilder()
            var depth = 0
            text.forEach { ch ->
                when {
                    ch == '{' || ch == '[' -> { depth++; cur.append(ch) }
                    ch == '}' || ch == ']' -> { depth--; cur.append(ch) }
                    ch == '|' && depth == 0 -> { out += cur.toString().trim(); cur.clear() }
                    else -> cur.append(ch)
                }
            }
            out += cur.toString().trim()
            return out
        }

        /**
         * Russian gender marks: `{masc|fem}` follows the companion, `[masc|fem]` the reader.
         * English text has none and passes through.
         */
        fun gender(text: String, readerFemale: Boolean, companionFemale: Boolean): String {
            val companion = Regex("\\{([^{}|]*)\\|([^{}|]*)\\}").replace(text) { if (companionFemale) it.groupValues[2] else it.groupValues[1] }
            return Regex("\\[([^\\[\\]|]*)\\|([^\\[\\]|]*)\\]").replace(companion) { if (readerFemale) it.groupValues[2] else it.groupValues[1] }
        }
    }
}
