package app.party

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.party.data.DemoSeed
import app.party.data.Quest
import app.party.data.Stage
import app.party.domain.Beast
import app.party.domain.Crest
import app.party.domain.Honours
import app.party.domain.Hour
import app.party.domain.Ordinary
import app.party.domain.Party
import app.party.domain.Symbol
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.arms.ArmsScreen
import app.party.ui.art.Achievement
import app.party.ui.art.WaxSeal
import app.party.ui.art.d20
import app.party.ui.art.dragon
import app.party.ui.art.dragonColour
import app.party.ui.art.vellum
import app.party.ui.book.BookScreen
import app.party.ui.book.NoticeScreen
import app.party.ui.chronicle.ChronicleTab
import app.party.ui.dragons.DragonsScreen
import app.party.ui.map.MapTab
import app.party.ui.onboarding.FoundingScreen
import app.party.ui.quest.QuestScreen
import app.party.ui.tavern.TavernTab
import app.party.ui.theme.PartyTheme
import app.party.ui.theme.U
import app.party.ui.treasury.TreasuryTab
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/** Every main screen over the demo party, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun hall(extra: (Party) -> List<Quest> = { emptyList() }): Hall = runBlocking {
        val app = ApplicationProvider.getApplicationContext<PartyApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        val dao = c.db.dao()
        var p = Party(dao.quests(), dao.events(), dao.dragons(), dao.strikes(), dao.letters(), dao.errands())
        val more = extra(p)
        if (more.isNotEmpty()) p = Party(more + p.quests, p.events, p.dragons, p.strikes, p.letters, p.errands)
        Hall(p, c.settings.now(), c.lore, System.currentTimeMillis())
    }

    private fun ongoing(stage: Int, lore: String = "q.road.02") = { p: Party ->
        listOf(Quest(id = 999, source = lore, school = "road", rank = "C", startedAt = System.currentTimeMillis() - 3_600_000, tasksDone = 0b101,
            secretA = "s.001", secretB = "s.002", stage = stage))
    }

    private fun shot(name: String, h: Hall, content: @Composable (Hall, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            PartyTheme { androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(U.c.vellum)) { content(h, Go(nav)) } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun founding() = hall().let { shot("00-founding", it) { h, _ -> FoundingScreen(h.settings) {} } }
    @Test fun tavern() = hall().let { shot("01-tavern", it) { h, go -> TavernTab(h, go) } }
    @Test fun notice() = hall().let { h -> shot("02-notice", h) { hh, go -> NoticeScreen(hh.board[1].id, hh, go) } }
    @Test fun oath() = hall(ongoing(Stage.OATH)).let { shot("03-oath", it) { h, go -> QuestScreen(999, h, go) } }
    @Test fun secret() = hall(ongoing(Stage.SECRET_A)).let { shot("04-secret", it) { h, go -> QuestScreen(999, h, go) } }
    @Test fun journey() = hall(ongoing(Stage.JOURNEY)).let { shot("05-journey", it) { h, go -> QuestScreen(999, h, go) } }
    @Test fun guess() = hall(ongoing(Stage.GUESS_A)).let { shot("06-guess", it) { h, go -> QuestScreen(999, h, go) } }
    @Test fun place() = hall(ongoing(Stage.PLACE)).let { shot("07-place", it) { h, go -> QuestScreen(999, h, go) } }
    @Test fun rewards() = hall().let { h -> shot("08-rewards", h) { hh, go -> QuestScreen(hh.party.finished.last().id, hh, go) } }
    @Test fun chronicle() = hall().let { shot("09-chronicle", it) { h, go -> ChronicleTab(h, go) } }
    @Test fun map() = hall().let { shot("10-map", it) { h, go -> MapTab(h, go) } }
    @Test fun treasury() = hall().let { shot("11-treasury", it) { h, go -> TreasuryTab(h, go) } }
    @Test fun arms() = hall().let { shot("12-arms", it) { h, go -> ArmsScreen(h, go) } }
    @Test fun dragons() = hall().let { shot("13-dragons", it) { h, go -> DragonsScreen(h, go) } }
    @Test fun book() = hall().let { shot("14-book", it) { h, go -> BookScreen(h, go) } }

    /** A sheet of random arms at every stage of honours, plus the seal, the die and a dragon. */
    @Test fun arts() = hall().let { h ->
        shot("99-arts", h) { _, _ ->
            val rnd = Random(3)
            fun crest() = Crest(
                Hour.entries.random(rnd) to Hour.entries.random(rnd), Ordinary.entries.random(rnd) to Ordinary.entries.random(rnd),
                Beast.entries.random(rnd) to Beast.entries.random(rnd), Symbol.entries.random(rnd) to Symbol.entries.random(rnd),
                rnd.nextBoolean() to rnd.nextBoolean(),
            )
            Column(Modifier.fillMaxSize().vellum()) {
                listOf(1, 5, 8, 12, 16, 20).chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { lvl -> Achievement(crest(), Honours.of(lvl, lvl / 7, 0), Modifier.width(137.dp)) }
                    }
                }
                Row {
                    Beast.entries.forEach { b ->
                        Achievement(Crest(TWO_HOURS, Ordinary.FESS to Ordinary.FESS, b to b, Symbol.STAR to Symbol.STAR, true to true), Honours(), Modifier.width(51.dp))
                    }
                }
                Row {
                    Symbol.entries.forEach { s ->
                        Achievement(Crest(TWO_HOURS, Ordinary.BEND to Ordinary.CROSS, Beast.OWL to Beast.WOLF, s to s, false to false), Honours(), Modifier.width(51.dp))
                    }
                }
                Row {
                    WaxSeal(h.settings.crest, 0, Modifier.size(100.dp))
                    WaxSeal(h.settings.crest, 2, Modifier.size(100.dp))
                    val gold = U.c.gold; val ink = U.c.ink
                    Canvas(Modifier.size(100.dp)) { d20(gold, ink) }
                    Canvas(Modifier.size(100.dp)) { dragon(dragonColour(0), false) }
                }
            }
        }
    }
}

private val TWO_HOURS = Hour.NOON to Hour.GREEN

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
