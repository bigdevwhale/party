package app.party.domain

import app.party.data.Done
import app.party.data.Dragon
import app.party.data.Errand
import app.party.data.Event
import app.party.data.EventKind
import app.party.data.Letter
import app.party.data.Quest
import app.party.data.Rank
import app.party.data.School
import app.party.data.Strike
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Experience: what each thing is worth, and the levels it adds up to. */
object Xp {
    const val TASK = 30
    const val SECRET_DONE = 40
    const val SECRET_PARTLY = 20
    const val GUESS = 30
    const val SEAL_INTACT = 50
    const val ENCOUNTER = 15
    const val CAMPFIRE = 10
    const val CHAPTER = 100
    const val ERRAND = 20
    const val STRIKE = 15
    const val SLAIN = 150
    const val LETTER = 10
    const val MAX_LEVEL = 20

    /** The XP a quest ends with, line by line, so the reward screen can show the reckoning. */
    data class Line(val what: Reason, val xp: Int, val count: Int = 1)

    enum class Reason { RANK, TASKS, SECRETS, GUESSES, SEAL, SEAL_INTACT, ENCOUNTERS, CAMPFIRE, CHAPTER }

    fun reckon(q: Quest, events: List<Event>): List<Line> {
        val lines = mutableListOf(Line(Reason.RANK, q.rankOf.xp))
        if (q.tasksCount > 0) lines += Line(Reason.TASKS, q.tasksCount * TASK, q.tasksCount)
        val secrets = listOf(q.doneA, q.doneB).sumOf { if (it == Done.YES) SECRET_DONE else if (it == Done.PARTLY) SECRET_PARTLY else 0 }
        if (secrets > 0) lines += Line(Reason.SECRETS, secrets)
        val guesses = listOf(q.guessedA, q.guessedB).count { it == 1 }
        if (guesses > 0) lines += Line(Reason.GUESSES, guesses * GUESS, guesses)
        if (q.sealMinutes > 0) lines += Line(Reason.SEAL, q.sealMinutes.coerceAtMost(90), q.sealMinutes)
        if (q.sealMinutes >= 30 && q.cracks == 0) lines += Line(Reason.SEAL_INTACT, SEAL_INTACT)
        val enc = events.count { it.kindOf == EventKind.ENCOUNTER }.coerceAtMost(5)
        if (enc > 0) lines += Line(Reason.ENCOUNTERS, enc * ENCOUNTER, enc)
        val fire = events.count { it.kindOf == EventKind.CAMPFIRE }.coerceAtMost(6)
        if (fire > 0) lines += Line(Reason.CAMPFIRE, fire * CAMPFIRE, fire)
        if (q.campaign.isNotEmpty()) lines += Line(Reason.CHAPTER, CHAPTER)
        return lines
    }

    /** XP needed to reach [level]: 0, 200, 600, 1200, 2000… — about a year of weekly quests to level 20. */
    fun threshold(level: Int): Int = 100 * level * (level - 1)

    fun level(xp: Int): Int = (1..MAX_LEVEL).last { threshold(it) <= xp }

    /** How far into the current level, 0..1. */
    fun progress(xp: Int): Float {
        val l = level(xp)
        if (l >= MAX_LEVEL) return 1f
        return (xp - threshold(l)).toFloat() / (threshold(l + 1) - threshold(l))
    }
}

/** A school's rank, I to V, by how many of its quests the party has done. */
object Ranks {
    val steps = intArrayOf(1, 2, 4, 7, 11)
    fun of(done: Int): Int = steps.count { done >= it }
    fun next(done: Int): Int? = steps.firstOrNull { done < it }
}

/** Everything the party did, and what it adds up to. Pure: built from the stored rows. */
class Party(
    val quests: List<Quest>,
    val events: List<Event>,
    val dragons: List<Dragon>,
    val strikes: List<Strike>,
    val letters: List<Letter>,
    val errands: List<Errand>,
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    val finished: List<Quest> by lazy { quests.filter { it.done }.sortedBy { it.endedAt } }
    val ongoing: Quest? by lazy { quests.firstOrNull { !it.done } }

    private val eventsBy: Map<Long, List<Event>> by lazy { events.groupBy { it.questId } }
    fun events(quest: Long): List<Event> = eventsBy[quest].orEmpty()

    private val strikesBy: Map<Long, List<Strike>> by lazy { strikes.groupBy { it.dragonId } }
    fun strikes(dragon: Long): List<Strike> = strikesBy[dragon].orEmpty()

    val living: List<Dragon> by lazy { dragons.filter { !it.slain } }
    val slain: List<Dragon> by lazy { dragons.filter { it.slain }.sortedBy { it.slainAt } }

    val xp: Int by lazy {
        finished.sumOf { it.xp } + errands.size * Xp.ERRAND + strikes.size * Xp.STRIKE + slain.size * Xp.SLAIN + letters.size * Xp.LETTER
    }
    val level: Int by lazy { Xp.level(xp) }

    /** Quests done per school; hand-written quests count for the school they were filed under. */
    val bySchool: Map<School, Int> by lazy { School.entries.associateWith { s -> finished.count { it.schoolOf == s } } }
    fun rank(s: School): Int = Ranks.of(bySchool[s] ?: 0)

    /** Each lore quest and how many times it was done. */
    val doneTimes: Map<String, Int> by lazy { finished.filter { !it.custom }.groupingBy { it.source }.eachCount() }

    /** Campaign key → chapters done, in order. */
    fun chaptersDone(campaign: String): Int = finished.count { it.campaign == campaign }

    fun campaignsCompleted(sizes: Map<String, Int>): Int = sizes.count { (k, n) -> n > 0 && chaptersDone(k) >= n }

    /** When the last chapter of a campaign was finished. */
    fun lastChapterAt(campaign: String): Long = finished.filter { it.campaign == campaign }.maxOfOrNull { it.endedAt } ?: 0L

    // Ravens.
    val errandDays: Set<Long> by lazy { errands.map { it.day }.toSet() }

    /** Days in a row with the raven's errand done, ending today or yesterday. */
    fun streak(today: Long): Int {
        var d = if (today in errandDays) today else today - 1
        var n = 0
        while (d in errandDays) { n++; d-- }
        return n
    }

    val bestStreak: Int by lazy {
        var best = 0; var run = 0; var prev = Long.MIN_VALUE
        errandDays.sorted().forEach { d -> run = if (d == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
        best
    }

    val waitingLetters: List<Letter> by lazy { letters.filter { it.openedAt == 0L } }

    // Counters for deeds and the treasury.
    val rolls: List<Event> by lazy { events.filter { it.kindOf == EventKind.ROLL } }
    val twenties: Int by lazy { rolls.count { it.value == 20 } }
    val ones: Int by lazy { rolls.count { it.value == 1 } }
    val encounters: Int by lazy { events.count { it.kindOf == EventKind.ENCOUNTER } }
    val campfires: Int by lazy { events.count { it.kindOf == EventKind.CAMPFIRE } }
    val guessed: Int by lazy { finished.sumOf { q -> listOf(q.guessedA, q.guessedB).count { it == 1 } } }
    val secretsKept: Int by lazy { finished.sumOf { q -> listOf(q.doneA, q.doneB).count { it == Done.YES } } }
    val sealMinutes: Int by lazy { finished.sumOf { it.sealMinutes } }
    val intactSeals: Int by lazy { finished.count { it.sealMinutes >= 30 && it.cracks == 0 } }
    val cracks: Int by lazy { finished.sumOf { it.cracks } }
    val trophies: List<Quest> by lazy { finished.filter { it.trophyName.isNotBlank() || it.trophyPhoto.isNotBlank() } }
    val places: List<Quest> by lazy { finished.filter { it.placed } }

    fun day(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /** Seasons (0 winter … 3 autumn) a quest was done in. */
    val seasons: Set<Int> by lazy { finished.map { (day(it.endedAt).monthValue % 12) / 3 }.toSet() }
}

/**
 * The party's deeds: honours earned once, shown in the treasury. Each knows how to tell from the
 * party whether it's earned; [campaigns] is how many campaigns were completed.
 */
enum class Deed(val earned: (Party, Int) -> Boolean) {
    FIRST_QUEST({ p, _ -> p.finished.isNotEmpty() }),
    FIVE_QUESTS({ p, _ -> p.finished.size >= 5 }),
    TEN_QUESTS({ p, _ -> p.finished.size >= 10 }),
    TWENTY_FIVE({ p, _ -> p.finished.size >= 25 }),
    FIFTY({ p, _ -> p.finished.size >= 50 }),
    FIRST_GOLD({ p, _ -> p.finished.any { it.rankOf == Rank.GOLD } }),
    ALL_SCHOOLS({ p, _ -> p.bySchool.values.all { it > 0 } }),
    MASTER({ p, _ -> School.entries.any { p.rank(it) >= 5 } }),
    SEAL_HOUR({ p, _ -> p.finished.any { it.sealMinutes >= 60 && it.cracks == 0 } }),
    TEN_SEALS({ p, _ -> p.intactSeals >= 10 }),
    IMPATIENT({ p, _ -> p.cracks >= 3 }),
    MIND_READERS({ p, _ -> p.finished.any { it.guessedA == 1 && it.guessedB == 1 } }),
    TEN_GUESSES({ p, _ -> p.guessed >= 10 }),
    SPIES({ p, _ -> p.secretsKept >= 10 }),
    NATURAL_TWENTY({ p, _ -> p.twenties > 0 }),
    NATURAL_ONE({ p, _ -> p.ones > 0 }),
    WANDERERS({ p, _ -> p.encounters >= 10 }),
    STORYTELLERS({ p, _ -> p.campfires >= 50 }),
    DRAGON_SLAYERS({ p, _ -> p.slain.isNotEmpty() }),
    THREE_DRAGONS({ p, _ -> p.slain.size >= 3 }),
    FIRST_CAMPAIGN({ _, c -> c >= 1 }),
    ALL_CAMPAIGNS({ _, c -> c >= 3 }),
    RAVEN_WEEK({ p, _ -> p.bestStreak >= 7 }),
    RAVEN_MONTH({ p, _ -> p.errands.size >= 30 }),
    LETTER({ p, _ -> p.letters.isNotEmpty() }),
    CARTOGRAPHERS({ p, _ -> p.places.size >= 10 }),
    COLLECTORS({ p, _ -> p.trophies.size >= 10 }),
    FOUR_SEASONS({ p, _ -> p.seasons.size == 4 }),
    MIDNIGHT({ p, _ -> p.finished.any { p.day(it.endedAt) != p.day(it.startedAt) } }),
    LEVEL_TEN({ p, _ -> p.level >= 10 }),
    LEGEND({ p, _ -> p.level >= Xp.MAX_LEVEL }),
}
