package app.party.domain

import app.party.data.Entry
import app.party.data.Kind
import app.party.data.Lore
import app.party.data.Rank
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/** Where a campaign stands for the party. */
data class CampaignState(
    val header: Entry,
    val chapters: List<Entry>,
    val done: Int,
    /** The party can't take it yet: the previous campaign isn't finished and the level is too low. */
    val locked: Boolean,
    val unlockLevel: Int,
    /** The next chapter's notice goes up at this time (the story needs a few days to breathe). */
    val nextAt: Long,
) {
    val key: String get() = header.school
    val complete: Boolean get() = done >= chapters.size
    val next: Entry? get() = chapters.getOrNull(done)
}

/** The game master's choices: what's on the board, which secrets, which encounter. Deterministic per seed. */
object GameMaster {
    const val DAY = 24 * 60 * 60 * 1000L

    /** A new chapter is posted three days after the last one was played. */
    const val CHAPTER_GAP = 3 * DAY

    /** An encounter waits on the road when the phone hasn't been looked at for this long. */
    const val WANDER = 20 * 60 * 1000L

    /** How many times a week the board can be torn down for fresh notices. */
    const val REROLLS = 2

    fun epochDay(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Long = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

    /** Weeks start on Monday: epoch day 4 was one. */
    fun week(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Long = Math.floorDiv(epochDay(ms, zone) - 4, 7L)

    /**
     * This week's three notices, one of each rank and of three different schools. Quests the party
     * hasn't done come first; quests done come back once everything else in their rank has been.
     */
    fun board(lore: Lore, party: Party, seed: Long, week: Long, rerolls: Int): List<Entry> {
        val rnd = Random(seed * 31 + week * 1009 + rerolls * 7919)
        val used = mutableSetOf<String>()
        return Rank.entries.mapNotNull { rank ->
            val pool = lore.quests.filter { it.rankOf == rank && it.school !in used }
            val fresh = pool.filter { (party.doneTimes[it.id] ?: 0) == 0 }
            val pick = (fresh.ifEmpty { pool.filter { (party.doneTimes[it.id] ?: 0) == pool.minOf { p -> party.doneTimes[p.id] ?: 0 } } })
                .shuffled(rnd).firstOrNull()
            pick?.also { used += it.school }
        }
    }

    /** Level at which a campaign opens even if the one before it isn't finished. */
    fun unlockLevel(index: Int): Int = when (index) { 0 -> 1; 1 -> 5; else -> 9 }

    fun campaigns(lore: Lore, party: Party): List<CampaignState> {
        var previousComplete = true
        return lore.campaigns.mapIndexed { i, header ->
            val chapters = lore.chapters(header.school)
            val done = party.chaptersDone(header.school)
            val unlock = unlockLevel(i)
            val locked = !(previousComplete || party.level >= unlock)
            val last = party.lastChapterAt(header.school)
            val state = CampaignState(header, chapters, done, locked, unlock, if (done == 0) 0L else last + CHAPTER_GAP)
            previousComplete = state.complete
            state
        }
    }

    /** Lore ids of secrets dealt in the party's recent quests, kept out of the next deal. */
    private fun recentSecrets(party: Party): Set<String> =
        party.quests.sortedByDescending { it.startedAt }.take(25).flatMap { listOf(it.secretA, it.secretB) }.toSet()

    /** Two different secrets, one for each hero. */
    fun deal(lore: Lore, party: Party, rnd: Random): Pair<String, String> {
        val recent = recentSecrets(party)
        val pool = lore.secrets.filter { it.id !in recent }.ifEmpty { lore.secrets }.shuffled(rnd)
        return (pool.getOrNull(0)?.id ?: "") to (pool.getOrNull(1)?.id ?: "")
    }

    /** The real secret among two decoys, in a fixed shuffled order for the quest. */
    fun choices(lore: Lore, real: String, other: String, seed: Long): List<String> {
        val rnd = Random(seed)
        val decoys = lore.secrets.map { it.id }.filter { it != real && it != other }.shuffled(rnd).take(2)
        return (decoys + real).shuffled(rnd)
    }

    /** Something not yet met on this road, preferring things the party has never met. */
    fun pick(pool: List<Entry>, seen: Collection<String>, here: Collection<String>, rnd: Random): Entry? {
        val notHere = pool.filter { it.id !in here }.ifEmpty { pool }
        return notHere.filter { it.id !in seen }.ifEmpty { notHere }.randomOrNull(rnd)
    }

    fun fate(lore: Lore, boon: Boolean, rnd: Random): Entry? = lore.fates.filter { it.kind == Kind.F && it.boon == boon }.randomOrNull(rnd)

    /** The raven's errand for a day: every errand once, in the party's own order, then round again. */
    fun raven(lore: Lore, seed: Long, day: Long): Entry? {
        val all = lore.ravens
        if (all.isEmpty()) return null
        val round = Math.floorDiv(day, all.size.toLong())
        val order = all.shuffled(Random(seed xor (round * 7_368_787L)))
        return order[Math.floorMod(day, all.size.toLong()).toInt()]
    }

    fun roman(n: Int): String {
        if (n <= 0) return "—"
        val v = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val s = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        var x = n
        return buildString { v.forEachIndexed { i, k -> while (x >= k) { append(s[i]); x -= k } } }
    }
}
