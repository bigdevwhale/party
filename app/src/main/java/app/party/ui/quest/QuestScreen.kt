package app.party.ui.quest

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.SHOTS
import app.party.container
import app.party.data.Quest
import app.party.data.Stage
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.WaxSeal
import app.party.ui.art.vellum
import app.party.ui.components.Fleuron
import app.party.ui.components.Illuminated
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SoftButton
import app.party.ui.components.TopBar
import app.party.ui.components.DarkBars
import app.party.ui.components.haptics
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlinx.coroutines.launch

/** Saves a change to the quest in the background. */
fun saveQuest(c: android.content.Context, q: Quest) = c.container.write { db.dao().update(q) }

/** A quest from the oath to the homecoming, picked up wherever the party left it. */
@Composable
fun QuestScreen(id: Long, hall: Hall, go: Go) {
    val q = hall.party.quests.firstOrNull { it.id == id }
    if (q == null) {
        // Abandoned or not loaded yet.
        Box(Modifier.fillMaxSize().vellum())
        return
    }
    if (q.done) {
        Rewards(q, hall, go)
        return
    }
    val c = ctx()
    // Skip the letters step when the raven carries nothing.
    LaunchedEffect(q.stage, hall.party.waitingLetters.size) {
        if (q.stage == Stage.LETTERS && hall.party.waitingLetters.isEmpty()) saveQuest(c, q.copy(stage = Stage.SECRET_A))
    }
    when (q.stage) {
        Stage.OATH -> Oath(q, hall, go)
        Stage.LETTERS -> Letters(q, hall)
        Stage.SECRET_A, Stage.SECRET_B -> SecretDeal(q, hall, if (q.stage == Stage.SECRET_A) 0 else 1)
        Stage.JOURNEY -> Journey(q, hall, go)
        else -> Homecoming(q, hall, go)
    }
}

/** The oath: both heroes press the seal at once, and the quest begins. */
@Composable
private fun Oath(q: Quest, hall: Hall, go: Go) {
    val c = ctx()
    val text = T.quest(c, hall.lore, q)
    val hold = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val h = haptics(hall.settings.haptics)
    var fingers by remember { mutableStateOf(0) }

    fun sworn() {
        h.thud()
        saveQuest(c, q.copy(stage = Stage.LETTERS, startedAt = System.currentTimeMillis()))
    }

    // Two thumbs on the seal fill the gold ring; letting go drains it.
    val both = fingers >= 2
    LaunchedEffect(both) {
        if (both) {
            hold.animateTo(1f, tween(((1f - hold.value) * 1600).toInt()))
            sworn()
        } else {
            hold.animateTo(0f, tween(300))
        }
    }

    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding()) {
        TopBar(stringResource(R.string.oath_bar), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Rubric(stringResource(R.string.oath_eyebrow))
            Spacer(Modifier.height(8.dp))
            Text(text.title, style = UType.Display, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (text.story.isNotBlank()) Illuminated(text.story)
            Fleuron()
            Text(stringResource(R.string.oath_words, T.hero(c, hall.settings, 0), T.hero(c, hall.settings, 1)), style = UType.Italic, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.size(190.dp).pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            fingers = event.changes.count { it.pressed }
                        } while (event.changes.any { it.pressed })
                        fingers = 0
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                val ring = U.c.gold
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(ring, -90f, 360f * hold.value, false, style = Stroke(10f))
                }
                WaxSeal(hall.settings.crest, 0, Modifier.size(160.dp), glow = hold.value)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (fingers == 1) R.string.oath_one_more else R.string.oath_hold),
                style = UType.Muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.oath_alone),
                style = UType.Small.copy(color = U.c.lapis),
                modifier = Modifier.clip(U.RSm).padding(8.dp).pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(); sworn() }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The raven brought letters written since the last quest: each hero opens the one for them. */
@Composable
private fun Letters(q: Quest, hall: Hall) {
    val c = ctx()
    val letters = hall.party.waitingLetters
    var open by rememberSaveable { mutableStateOf(false) }
    val letter = letters.firstOrNull() ?: return
    DarkBars()
    val to = 1 - letter.from
    Column(Modifier.fillMaxSize().background(U.c.night).statusBarsPadding().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Rubric(stringResource(R.string.letter_raven), color = U.c.goldLight)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.letter_for, T.hero(c, hall.settings, to)), style = UType.Display.copy(color = U.c.page), textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        if (!open) {
            WaxSeal(hall.settings.crest, 0, Modifier.size(150.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.letter_only, T.hero(c, hall.settings, to)), style = UType.Italic.copy(color = U.c.rule), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(R.string.letter_break), { open = true }, Modifier.fillMaxWidth())
        } else {
            Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.page).border(1.dp, U.c.gold, U.RMd).padding(22.dp)) {
                Text(letter.text, style = UType.Story)
                Spacer(Modifier.height(12.dp))
                Text("— " + T.hero(c, hall.settings, letter.from), style = UType.Italic, modifier = Modifier.align(Alignment.End))
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(if (letters.size > 1) R.string.letter_next else R.string.letter_done), {
                open = false
                c.container.write {
                    db.dao().update(letter.copy(openedAt = System.currentTimeMillis(), questId = q.id))
                    if (letters.size == 1) db.dao().update(q.copy(stage = Stage.SECRET_A))
                }
            }, Modifier.fillMaxWidth())
        }
    }
}

/** One hero takes the phone alone and reads their secret objective. */
@Composable
private fun SecretDeal(q: Quest, hall: Hall, who: Int) {
    val c = ctx()
    val s = hall.settings
    var seen by rememberSaveable(who) { mutableStateOf(false) }
    var peeking by remember { mutableStateOf(SHOTS) }
    val secret = hall.lore[if (who == 0) q.secretA else q.secretB]
    BackHandler { }
    DarkBars()
    Column(
        Modifier.fillMaxSize().background(U.c.night).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Rubric(stringResource(R.string.secret_eyebrow, who + 1), color = U.c.goldLight)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.secret_pass, T.hero(c, s, who)), style = UType.Display.copy(color = U.c.page), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.secret_no_peeking, T.hero(c, s, 1 - who)), style = UType.Italic.copy(color = U.c.rule), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(U.RMd)
                .background(if (peeking) U.c.page else U.c.leather)
                .border(1.5.dp, U.c.gold, U.RMd)
                // Keyed by the hero: each one's hold must reach their own state.
                .pointerInput(who) {
                    awaitEachGesture {
                        awaitFirstDown()
                        peeking = true; seen = true
                        do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                        peeking = SHOTS
                    }
                }
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (peeking) {
                Text(T.secret(c, s, who, secret), style = UType.Story, textAlign = TextAlign.Center)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WaxSeal(s.crest, 0, Modifier.size(84.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.secret_hold), style = UType.Muted.copy(color = U.c.rule), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.secret_rules), style = UType.Small.copy(color = U.c.rule), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.secret_got_it), {
            saveQuest(c, q.copy(stage = if (who == 0) Stage.SECRET_B else Stage.JOURNEY))
        }, Modifier.fillMaxWidth(), enabled = seen)
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.secret_other), {
            // A secret that can't be done today: draw another.
            val other = hall.lore.secrets.filter { it.id != q.secretA && it.id != q.secretB }.randomOrNull() ?: return@SoftButton
            saveQuest(c, if (who == 0) q.copy(secretA = other.id) else q.copy(secretB = other.id))
        }, Modifier.fillMaxWidth(), color = U.c.goldLight, enabled = seen)
    }
}
