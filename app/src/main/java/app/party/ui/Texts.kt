package app.party.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.party.R
import app.party.data.Entry
import app.party.data.Lore
import app.party.data.Quest
import app.party.data.QuestText
import app.party.data.Rank
import app.party.data.School
import app.party.data.Settings
import app.party.domain.Beast
import app.party.domain.Deed
import app.party.domain.GameMaster
import app.party.domain.Hour
import app.party.domain.Ordinary
import app.party.domain.Symbol
import app.party.domain.Xp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/** Words, names and dates in the app's language. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    /** "ru" or "en": which side of the lore to read. */
    fun lang(c: Context): String = if (locale(c).language == "ru") "ru" else "en"

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "17 October 2026". */
    fun date(c: Context, ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale(c)))

    fun shortDate(c: Context, ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM", locale(c)))

    /** "Saturday": when the next chapter's notice goes up. */
    fun weekday(c: Context, ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale(c)))

    fun hero(c: Context, s: Settings, who: Int): String = s.name(who, c.getString(R.string.hero_a), c.getString(R.string.hero_b))

    fun school(c: Context, s: School): String = c.resources.getStringArray(R.array.schools)[s.ordinal]

    /** "Masters of the Hearth"; rank 0 is "not yet begun". */
    fun schoolRank(c: Context, s: School, rank: Int): String =
        if (rank == 0) c.getString(R.string.rank_none) else c.getString(R.string.rank_of, c.resources.getStringArray(R.array.rank_titles)[rank - 1], c.resources.getStringArray(R.array.schools_of)[s.ordinal])

    fun rank(c: Context, r: Rank): String = c.resources.getStringArray(R.array.ranks)[r.ordinal]

    fun level(c: Context, level: Int): String = c.resources.getStringArray(R.array.level_titles)[(level - 1).coerceIn(0, Xp.MAX_LEVEL - 1)]

    fun deed(c: Context, d: Deed): String = c.resources.getStringArray(R.array.deed_names)[d.ordinal]
    fun deedText(c: Context, d: Deed): String = c.resources.getStringArray(R.array.deed_texts)[d.ordinal]

    fun hour(c: Context, h: Hour): String = c.resources.getStringArray(R.array.hours)[h.ordinal]
    fun ordinary(c: Context, o: Ordinary): String = c.resources.getStringArray(R.array.ordinaries)[o.ordinal]
    fun beast(c: Context, b: Beast): String = c.resources.getStringArray(R.array.beasts)[b.ordinal]
    fun symbol(c: Context, s: Symbol): String = c.resources.getStringArray(R.array.symbols)[s.ordinal]
    fun metal(c: Context, gold: Boolean): String = c.getString(if (gold) R.string.metal_gold else R.string.metal_silver)

    fun reason(c: Context, r: Xp.Reason): String = c.resources.getStringArray(R.array.xp_reasons)[r.ordinal]

    /** A secret objective as its hero reads it, Russian forms set for both of them. */
    fun secret(c: Context, s: Settings, who: Int, e: Entry?): String {
        if (e == null) return ""
        return Lore.gender(e.text(lang(c)), s.female(who), s.female(1 - who))
    }

    /** A quest's text, from the lore or as the party wrote it. */
    fun quest(c: Context, lore: Lore, q: Quest): QuestText {
        val e = lore[q.source]
        if (q.custom || e == null) {
            val title = q.customTitle.ifBlank { c.getString(R.string.custom_quest) }
            return QuestText(title, c.getString(R.string.custom_in_which, title), "", q.customTasks.lines().map { it.trim() }.filter { it.isNotEmpty() })
        }
        return lore.questText(e, lang(c))
    }

    /** "Chapter VII, in which the party lost its way in the fog". */
    fun chapter(c: Context, n: Int, inWhich: String): String = c.getString(R.string.chapter_heading, GameMaster.roman(n), inWhich)

    /** "+3 to Courage. Curse: …": what a trophy is good for, made up from its seed. */
    fun trophyLore(c: Context, seed: Long): String {
        val rnd = Random(seed)
        val stats = c.resources.getStringArray(R.array.trophy_stats)
        val curses = c.resources.getStringArray(R.array.trophy_curses)
        return c.getString(R.string.trophy_lore, rnd.nextInt(1, 5), stats[rnd.nextInt(stats.size)], curses[rnd.nextInt(curses.size)])
    }

    /** "The Inn of the Sleepy Heron": a name for a place on the map. */
    fun placeName(c: Context, seed: Long): String {
        val rnd = Random(seed)
        val kinds = c.resources.getStringArray(R.array.place_kinds)
        val of = c.resources.getStringArray(R.array.place_of)
        return c.getString(R.string.place_name_fmt, kinds[rnd.nextInt(kinds.size)], of[rnd.nextInt(of.size)])
    }

    fun quote(c: Context, text: String): String = c.getString(R.string.quoted, text)
}

@Composable
fun ctx(): Context = LocalContext.current
