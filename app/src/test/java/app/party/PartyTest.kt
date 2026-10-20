package app.party

import androidx.test.core.app.ApplicationProvider
import app.party.data.Backup
import app.party.data.Done
import app.party.data.Dragon
import app.party.data.Errand
import app.party.data.Event
import app.party.data.EventKind
import app.party.data.Everything
import app.party.data.Kind
import app.party.data.Letter
import app.party.data.Lore
import app.party.data.Quest
import app.party.data.Rank
import app.party.data.School
import app.party.data.Settings
import app.party.data.Strike
import app.party.domain.Beast
import app.party.domain.Crest
import app.party.domain.Deed
import app.party.domain.GameMaster
import app.party.domain.Hour
import app.party.domain.Ordinary
import app.party.domain.Party
import app.party.domain.Symbol
import app.party.domain.Xp
import app.party.ui.art.Realm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PartyTest {
    private val app get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val lore: Lore by lazy { Lore.load(app.assets) }

    private fun party(quests: List<Quest> = emptyList(), events: List<Event> = emptyList(), errands: List<Errand> = emptyList()) =
        Party(quests, events, emptyList(), emptyList(), emptyList(), errands, ZoneOffset.UTC)

    @Test
    fun loreIsWellFormed() {
        val raw = app.assets.let { a ->
            a.list(Lore.DIR)!!.filter { it.endsWith(".tsv") }.flatMap { f -> a.open("${Lore.DIR}/$f").use { it.readBytes().decodeToString() }.lines().filter { it.isNotBlank() } }
        }
        // Every line parses: nothing is silently dropped.
        assertEquals(raw.size, lore.entries.size)
        assertEquals("ids are unique", lore.entries.size, lore.entries.map { it.id }.toSet().size)
        lore.entries.forEach { e ->
            assertEquals(e.id, e.en.size, e.ru.size)
            assertTrue(e.id, e.en.all { it.isNotBlank() } && e.ru.all { it.isNotBlank() })
            assertFalse(e.id, e.ru.any { "(а)" in it })
            when (e.kind) {
                Kind.Q -> {
                    assertTrue(e.id, e.schoolOf != null && e.rankOf != null)
                    assertTrue(e.id, e.en.size in 6..7)
                    assertTrue(e.id, e.en[1].startsWith("in which the party") && e.ru[1].startsWith("в которой отряд"))
                }
                Kind.C -> assertEquals(e.id, 7, e.en.size)
                Kind.E, Kind.F, Kind.K -> assertEquals(e.id, 2, e.en.size)
                Kind.S, Kind.T, Kind.R -> assertEquals(e.id, 1, e.en.size)
            }
        }
        School.entries.forEach { s -> Rank.entries.forEach { r -> assertTrue("$s $r", lore.quests(s).count { it.rankOf == r } >= 3) } }
        assertEquals(3, lore.campaigns.size)
        lore.campaigns.forEach { assertEquals(it.id, 7, lore.chapters(it.school).size) }
        assertTrue(lore.secrets.size >= 100)
        assertTrue(lore.encounters.size >= 60)
        assertTrue(lore.campfire.size >= 80)
        assertTrue(lore.ravens.size >= 60)
        assertTrue(lore.fates.count { it.boon } >= 20 && lore.fates.count { !it.boon } >= 20)
    }

    @Test
    fun genderMarksResolveForEveryone() {
        lore.secrets.forEach { e ->
            for (reader in listOf(false, true)) for (companion in listOf(false, true)) {
                val t = Lore.gender(e.text("ru"), reader, companion)
                assertFalse(e.id, t.contains('{') || t.contains('}') || t.contains('[') || t.contains(']') || t.contains('|'))
            }
        }
        assertEquals("Рассмеши свою спутницу.", Lore.gender("Рассмеши {своего спутника|свою спутницу}.", readerFemale = false, companionFemale = true))
        assertEquals("Ты сама.", Lore.gender("Ты [сам|сама].", readerFemale = true, companionFemale = false))
    }

    @Test
    fun resourceArraysMatchTheirEnums() {
        val r = app.resources
        assertEquals(Deed.entries.size, r.getStringArray(R.array.deed_names).size)
        assertEquals(Deed.entries.size, r.getStringArray(R.array.deed_texts).size)
        assertEquals(Xp.MAX_LEVEL, r.getStringArray(R.array.level_titles).size)
        assertEquals(Xp.Reason.entries.size, r.getStringArray(R.array.xp_reasons).size)
        assertEquals(School.entries.size, r.getStringArray(R.array.schools).size)
        assertEquals(School.entries.size, r.getStringArray(R.array.schools_of).size)
        assertEquals(Hour.entries.size, r.getStringArray(R.array.hours).size)
        assertEquals(Ordinary.entries.size, r.getStringArray(R.array.ordinaries).size)
        assertEquals(Beast.entries.size, r.getStringArray(R.array.beasts).size)
        assertEquals(Symbol.entries.size, r.getStringArray(R.array.symbols).size)
        assertEquals(Rank.entries.size, r.getStringArray(R.array.ranks).size)
    }

    @Test
    fun levelsClimb() {
        assertEquals(1, Xp.level(0))
        assertEquals(2, Xp.level(200))
        assertEquals(2, Xp.level(599))
        assertEquals(3, Xp.level(600))
        assertEquals(Xp.MAX_LEVEL, Xp.level(1_000_000))
        assertEquals(1f, Xp.progress(1_000_000))
    }

    @Test
    fun aQuestIsReckoned() {
        val q = Quest(
            id = 1, source = "c1.1", school = "road", rank = "S", startedAt = 0, endedAt = 1, tasksDone = 0b111,
            doneA = Done.YES, doneB = Done.PARTLY, guessedA = 1, guessedB = 0, sealMinutes = 40,
        )
        val events = listOf(Event(questId = 1, at = 0, kind = EventKind.ENCOUNTER.name), Event(questId = 1, at = 0, kind = EventKind.CAMPFIRE.name))
        val lines = Xp.reckon(q, events)
        val total = lines.sumOf { it.xp }
        assertEquals(220 + 90 + 60 + 30 + 40 + 50 + 15 + 10 + 100, total)
    }

    @Test
    fun crestRoundTrips() {
        val c = Crest(Hour.DUSK to Hour.NOON, Ordinary.WAVES to Ordinary.CROSS, Beast.OWL to Beast.OWL, Symbol.KEY to Symbol.ROSE, true to false)
        assertEquals(c, Crest.decode(c.encode()))
        assertEquals(1, c.agreed)
        assertTrue(c.splitField && c.splitOrdinary && !c.twoBeasts && c.twoSymbols)
        assertEquals(null, Crest.decode("nonsense"))
    }

    @Test
    fun theBoardIsSteadyForAWeekAndVaried() {
        val p = party()
        val a = GameMaster.board(lore, p, 42, 100, 0)
        assertEquals(a, GameMaster.board(lore, p, 42, 100, 0))
        assertEquals(3, a.size)
        assertEquals(Rank.entries.toList(), a.map { it.rankOf })
        assertEquals(3, a.map { it.school }.toSet().size)
        assertNotEquals(a, GameMaster.board(lore, p, 42, 100, 1))
        // A quest already done doesn't come back while fresh ones remain.
        val done = a.map { Quest(source = it.id, school = it.school, rank = it.rank, startedAt = 0, endedAt = 1) }
        val next = GameMaster.board(lore, party(done), 42, 100, 0)
        assertTrue(next.none { n -> done.any { it.source == n.id } })
    }

    @Test
    fun campaignsOpenInTurn() {
        val fresh = GameMaster.campaigns(lore, party())
        assertFalse(fresh[0].locked)
        assertTrue(fresh[1].locked && fresh[2].locked)
        val c1 = lore.chapters("c1").mapIndexed { i, e -> Quest(source = e.id, school = "road", rank = "S", startedAt = i * 10L, endedAt = i * 10L + 1) }
        val after = GameMaster.campaigns(lore, party(c1))
        assertTrue(after[0].complete)
        assertFalse(after[1].locked)
        assertTrue(after[2].locked)
        // The next chapter waits a few days after the last one.
        val one = GameMaster.campaigns(lore, party(c1.take(1)))[0]
        assertEquals(c1[0].endedAt + GameMaster.CHAPTER_GAP, one.nextAt)
        assertEquals("c1.2", one.next?.id)
    }

    @Test
    fun secretsAndChoices() {
        val (a, b) = GameMaster.deal(lore, party(), Random(1))
        assertNotEquals(a, b)
        val choices = GameMaster.choices(lore, a, b, 7)
        assertEquals(3, choices.size)
        assertTrue(a in choices && b !in choices)
        assertEquals(choices, GameMaster.choices(lore, a, b, 7))
    }

    @Test
    fun ravensComeRoundInTurn() {
        val n = lore.ravens.size
        val round = (0 until n).map { GameMaster.raven(lore, 5, it.toLong())!!.id }
        assertEquals(n, round.toSet().size)
        val p = party(errands = listOf(10L, 11L, 12L, 14L).map { Errand(it, "r", 0) })
        assertEquals(1, p.streak(14))
        assertEquals(3, p.streak(13))
        assertEquals(3, p.bestStreak)
    }

    @Test
    fun deedsAreEarned() {
        val q = Quest(source = "q.road.01", school = "road", rank = "G", startedAt = 0, endedAt = 1, guessedA = 1, guessedB = 1, sealMinutes = 61)
        val p = party(listOf(q), listOf(Event(questId = 0, at = 0, kind = EventKind.ROLL.name, value = 20)))
        val earned = Deed.entries.filter { it.earned(p, 0) }.toSet()
        assertTrue(Deed.FIRST_QUEST in earned && Deed.FIRST_GOLD in earned && Deed.MIND_READERS in earned && Deed.SEAL_HOUR in earned && Deed.NATURAL_TWENTY in earned)
        assertFalse(Deed.FIVE_QUESTS in earned)
    }

    @Test
    fun theRealmIsTheParty() {
        val a = Realm(1); val b = Realm(1); val c = Realm(2)
        assertEquals(a.coast(), b.coast())
        assertNotEquals(a.coast(), c.coast())
        assertTrue(a.land(a.centre.x, a.centre.y))
        assertFalse(a.land(0.01f, 0.01f))
    }

    @Test
    fun backupRoundTrips() {
        val all = Everything(
            quests = listOf(Quest(id = 3, source = "q.arts.01", school = "arts", rank = "C", startedAt = 5, endedAt = 9, lineA = "a\tb", placeX = 0.4f, placeY = 0.6f, xp = 300, chapter = 1)),
            events = listOf(Event(id = 1, questId = 3, at = 6, kind = EventKind.ROLL.name, value = 20)),
            dragons = listOf(Dragon(id = 2, name = "Grimfang", deed = "taxes", hp = 5, seed = 1, createdAt = 1)),
            strikes = listOf(Strike(id = 4, dragonId = 2, at = 3, note = "receipts")),
            letters = listOf(Letter(id = 5, from = 1, text = "hi", createdAt = 2)),
            errands = listOf(Errand(100, "r.001", 7)),
        )
        val s = Settings(nameA = "Anna", nameB = "Mark", motto = "Onward", seed = 77)
        val (back, apply) = Backup.decode(Backup.encode(all, s))
        assertEquals(all.quests, back.quests)
        assertEquals(all.events, back.events)
        assertEquals(all.dragons, back.dragons)
        assertEquals(all.strikes, back.strikes)
        assertEquals(all.letters, back.letters)
        assertEquals(all.errands, back.errands)
        val restored = apply(Settings())
        assertEquals("Anna", restored.nameA)
        assertEquals(77L, restored.seed)
        assertTrue(restored.onboarded)
    }
}
