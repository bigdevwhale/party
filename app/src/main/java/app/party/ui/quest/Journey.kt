package app.party.ui.quest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.party.R
import app.party.SHOTS
import app.party.container
import app.party.data.Entry
import app.party.data.Event
import app.party.data.EventKind
import app.party.data.Quest
import app.party.data.Stage
import app.party.domain.GameMaster
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.d20
import app.party.ui.art.vellum
import app.party.ui.components.Confirm
import app.party.ui.components.Fleuron
import app.party.ui.components.Leaf
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.Stamp
import app.party.ui.components.TopBar
import app.party.ui.components.DarkBars
import app.party.ui.components.haptics
import app.party.ui.ctx
import app.party.ui.theme.Cormorant
import app.party.ui.theme.Sans
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlinx.coroutines.delay
import kotlin.random.Random

/** What lies open over the road. */
private enum class Over { NONE, DIE, ENCOUNTER, CAMPFIRE, SEAL, PEEK }

/** When each quest was last looked at, so an encounter can wait on the road for the party's return. */
private val lastSeen = mutableMapOf<Long, Long>()

fun logEvent(c: android.content.Context, q: Quest, kind: EventKind, ref: String = "", value: Int = 0, who: Int = -1) =
    c.container.write { db.dao().insert(Event(questId = q.id, at = System.currentTimeMillis(), kind = kind.name, ref = ref, value = value, who = who)) }

/** On the road: the tasks, and the game master's tools. */
@Composable
fun Journey(q: Quest, hall: Hall, go: Go) {
    val c = ctx()
    val lang = T.lang(c)
    val text = T.quest(c, hall.lore, q)
    val events = hall.party.events(q.id)
    var over by rememberSaveable { mutableStateOf(Over.NONE) }
    var wandered by rememberSaveable { mutableStateOf(false) }
    var confirmHome by remember { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val h = haptics(hall.settings.haptics)

    // Back on the road after a while away: something has turned up.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val last = lastSeen[q.id]
        if (last != null && System.currentTimeMillis() - last > GameMaster.WANDER && over == Over.NONE) {
            wandered = true; over = Over.ENCOUNTER
        }
        lastSeen[q.id] = System.currentTimeMillis()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { lastSeen[q.id] = System.currentTimeMillis() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().vellum().navigationBarsPadding()) {
            TopBar(stringResource(R.string.journey_bar), go::back) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null, tint = U.c.inkSoft) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.journey_abandon)) }, onClick = { menu = false; confirmAbandon = true })
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                Text(text.title, style = UType.Display)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.journey_since, T.shortDate(c, q.startedAt)), style = UType.Small)

                SectionTitle(stringResource(R.string.notice_tasks))
                text.tasks.forEachIndexed { i, t ->
                    val done = q.taskDone(i)
                    Row(
                        Modifier.fillMaxWidth().clip(U.RSm).clickable {
                            h.tick()
                            saveQuest(c, q.copy(tasksDone = q.tasksDone xor (1 shl i)))
                        }.padding(vertical = 9.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(26.dp).border(1.5.dp, if (done) U.c.vermilion else U.c.faint, U.RSm), contentAlignment = Alignment.Center) {
                            if (done) Text("✓", color = U.c.vermilion, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(t, style = UType.Body.copy(color = if (done) U.c.inkSoft else U.c.ink), modifier = Modifier.weight(1f))
                    }
                }

                SectionTitle(stringResource(R.string.journey_tools))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tool("⚀", stringResource(R.string.tool_die), stringResource(R.string.tool_die_sub), Modifier.weight(1f)) { over = Over.DIE }
                    Tool("✦", stringResource(R.string.tool_encounter), stringResource(R.string.tool_encounter_sub), Modifier.weight(1f)) { wandered = false; over = Over.ENCOUNTER }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tool("♨", stringResource(R.string.tool_campfire), stringResource(R.string.tool_campfire_sub), Modifier.weight(1f)) { over = Over.CAMPFIRE }
                    Tool("◉", stringResource(R.string.tool_seal), if (q.sealMinutes > 0) T.plural(c, R.plurals.minutes_sealed, q.sealMinutes) else stringResource(R.string.tool_seal_sub), Modifier.weight(1f)) { over = Over.SEAL }
                }
                Spacer(Modifier.height(10.dp))
                SoftButton(stringResource(R.string.tool_peek), { over = Over.PEEK }, Modifier.fillMaxWidth(), color = U.c.night)

                if (events.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.journey_log))
                    events.takeLast(12).reversed().forEach { e -> LogLine(e, hall, lang) }
                }
                Spacer(Modifier.height(20.dp))
            }
            Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                PrimaryButton(stringResource(R.string.journey_home), { confirmHome = true }, Modifier.fillMaxWidth())
            }
        }

        AnimatedVisibility(over != Over.NONE, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(U.c.night)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .statusBarsPadding().navigationBarsPadding(),
            ) {
                DarkBars()
                when (over) {
                    Over.DIE -> DieTable(q, hall) { over = Over.NONE }
                    Over.ENCOUNTER -> Encounter(q, hall, wandered) { over = Over.NONE }
                    Over.CAMPFIRE -> Campfire(q, hall) { over = Over.NONE }
                    Over.SEAL -> SealOfSilence(q, hall) { over = Over.NONE }
                    Over.PEEK -> Peek(q, hall) { over = Over.NONE }
                    Over.NONE -> {}
                }
                if (over != Over.SEAL) IconButton(onClick = { over = Over.NONE }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = U.c.rule)
                }
            }
        }
    }

    if (confirmHome) Confirm(
        stringResource(R.string.home_confirm_title), stringResource(R.string.home_confirm_text), stringResource(R.string.home_confirm_yes),
        { saveQuest(c, q.copy(stage = Stage.GUESS_A)) }, { confirmHome = false },
    )
    if (confirmAbandon) Confirm(
        stringResource(R.string.abandon_title), stringResource(R.string.abandon_text), stringResource(R.string.journey_abandon),
        { c.container.write { db.dao().deleteQuest(q.id) }; go.home() }, { confirmAbandon = false },
    )
}

@Composable
private fun Tool(glyph: String, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(U.RMd).background(U.c.page).border(1.dp, U.c.rule, U.RMd).clickable(onClick = onClick).padding(14.dp),
    ) {
        Text(glyph, fontSize = 26.sp, color = U.c.vermilion)
        Spacer(Modifier.height(4.dp))
        Text(title, style = UType.Strong)
        Text(sub, style = UType.Small, maxLines = 2)
    }
}

@Composable
private fun LogLine(e: Event, hall: Hall, lang: String) {
    val c = ctx()
    val line = when (e.kindOf) {
        EventKind.ROLL -> c.getString(R.string.log_roll, e.value) + (hall.lore[e.ref]?.let { " · " + it.part(lang, 0) } ?: "")
        EventKind.ENCOUNTER -> hall.lore[e.ref]?.part(lang, 0) ?: ""
        EventKind.CAMPFIRE -> hall.lore[e.ref]?.text(lang) ?: ""
        EventKind.SEAL -> T.plural(c, R.plurals.log_seal, e.value)
        EventKind.CRACK -> T.plural(c, R.plurals.log_crack, e.value)
        EventKind.LETTER, null -> ""
    }
    val glyph = when (e.kindOf) {
        EventKind.ROLL -> "⚀"; EventKind.ENCOUNTER -> "✦"; EventKind.CAMPFIRE -> "♨"; EventKind.SEAL -> "◉"; EventKind.CRACK -> "✕"; else -> "·"
    }
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(glyph, color = U.c.vermilion, modifier = Modifier.width(26.dp))
        Text(line, style = UType.Italic, maxLines = 2)
    }
}

/** The d20: tap to roll; a 20 or a 1 brings a fate. */
@Composable
private fun DieTable(q: Quest, hall: Hall, onClose: () -> Unit) {
    val c = ctx()
    val lang = T.lang(c)
    var face by remember { mutableIntStateOf(if (SHOTS) 20 else 0) }
    var rolling by remember { mutableStateOf(false) }
    var fate by remember { mutableStateOf<Entry?>(if (SHOTS) hall.lore.fates.firstOrNull { it.boon } else null) }
    var spin by remember { mutableIntStateOf(0) }
    val h = haptics(hall.settings.haptics)
    LaunchedEffect(spin) {
        if (spin == 0) return@LaunchedEffect
        rolling = true; fate = null
        val rnd = Random(System.nanoTime())
        repeat(14) { i -> face = rnd.nextInt(1, 21); h.tick(); delay(45L + i * 12L) }
        face = rnd.nextInt(1, 21)
        rolling = false
        h.thud()
        val f = when (face) { 20 -> GameMaster.fate(hall.lore, true, rnd); 1 -> GameMaster.fate(hall.lore, false, rnd); else -> null }
        fate = f
        logEvent(c, q, EventKind.ROLL, f?.id ?: "", face)
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Rubric(stringResource(R.string.die_eyebrow), color = U.c.goldLight)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.die_text), style = UType.Italic.copy(color = U.c.rule), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        val gold = U.c.gold; val ink = U.c.ink
        Box(
            Modifier.size(200.dp).rotate(if (rolling) (face * 37f) % 360f else 0f).pointerInput(Unit) { awaitEachGesture { awaitFirstDown(); if (!rolling) spin++ } },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) { d20(if (face == 1) androidx.compose.ui.graphics.Color(0xFF8E1F22) else gold, ink) }
            Text(if (face == 0) "?" else face.toString(), fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = ink, modifier = Modifier.padding(top = 30.dp))
        }
        Spacer(Modifier.height(20.dp))
        val f = fate
        if (f != null && !rolling) {
            Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.page).border(1.5.dp, if (f.boon) U.c.gold else U.c.wax, U.RMd).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Rubric(stringResource(if (f.boon) R.string.die_twenty else R.string.die_one), color = if (f.boon) U.c.goldDark else U.c.wax)
                Spacer(Modifier.height(6.dp))
                Text(f.part(lang, 0), style = UType.Title, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(f.part(lang, 1), style = UType.Prose, textAlign = TextAlign.Center)
            }
        } else if (face > 0 && !rolling) {
            Text(stringResource(if (face >= 11) R.string.die_high else R.string.die_low), style = UType.Italic.copy(color = U.c.page), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(if (face == 0) R.string.die_roll else R.string.die_again), { if (!rolling) spin++ }, Modifier.fillMaxWidth(), enabled = !rolling)
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.back_to_road), onClose, Modifier.fillMaxWidth(), color = U.c.goldLight)
    }
}

/** Someone, or something, on the road. */
@Composable
private fun Encounter(q: Quest, hall: Hall, wandered: Boolean, onClose: () -> Unit) {
    val c = ctx()
    val lang = T.lang(c)
    val seen = remember { hall.party.events.filter { it.kindOf == EventKind.ENCOUNTER }.map { it.ref } }
    val here = remember { hall.party.events(q.id).filter { it.kindOf == EventKind.ENCOUNTER }.map { it.ref } }
    var e by remember { mutableStateOf(GameMaster.pick(hall.lore.encounters, seen, here, Random(System.nanoTime()))) }
    var rerolled by remember { mutableStateOf(false) }
    val entry = e ?: run { onClose(); return }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Rubric(stringResource(if (wandered) R.string.encounter_wandered else R.string.encounter_eyebrow), color = U.c.goldLight)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.page).border(1.5.dp, U.c.gold, U.RMd).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", fontSize = 30.sp, color = U.c.vermilion)
            Spacer(Modifier.height(6.dp))
            Text(entry.part(lang, 0), style = UType.Display, textAlign = TextAlign.Center)
            Fleuron()
            Text(entry.part(lang, 1), style = UType.Story, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.encounter_done), { logEvent(c, q, EventKind.ENCOUNTER, entry.id); onClose() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        if (!rerolled) SoftButton(stringResource(R.string.encounter_other), {
            rerolled = true
            e = GameMaster.pick(hall.lore.encounters, seen + entry.id, here + entry.id, Random(System.nanoTime()))
        }, Modifier.fillMaxWidth(), color = U.c.goldLight)
    }
}

/** A rest by the fire: questions, one after another, for as long as the fire burns. */
@Composable
private fun Campfire(q: Quest, hall: Hall, onClose: () -> Unit) {
    val c = ctx()
    val lang = T.lang(c)
    val asked = remember { hall.party.events.filter { it.kindOf == EventKind.CAMPFIRE }.map { it.ref }.toMutableSet() }
    var current by remember { mutableStateOf(GameMaster.pick(hall.lore.campfire, asked, emptyList(), Random(System.nanoTime()))) }
    var count by remember { mutableIntStateOf(0) }
    val entry = current ?: run { onClose(); return }
    fun answered() {
        logEvent(c, q, EventKind.CAMPFIRE, entry.id)
        asked += entry.id
        count++
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Fire(Modifier.size(110.dp))
        Spacer(Modifier.height(10.dp))
        Rubric(stringResource(R.string.campfire_eyebrow), color = U.c.goldLight)
        Spacer(Modifier.height(18.dp))
        Text(entry.text(lang), style = UType.Story.copy(color = U.c.page, fontSize = 23.sp, lineHeight = 31.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.campfire_both), style = UType.Small.copy(color = U.c.rule))
        Spacer(Modifier.height(28.dp))
        PrimaryButton(stringResource(R.string.campfire_next), {
            answered()
            current = GameMaster.pick(hall.lore.campfire, asked, emptyList(), Random(System.nanoTime()))
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.campfire_skip), {
            asked += entry.id
            current = GameMaster.pick(hall.lore.campfire, asked, emptyList(), Random(System.nanoTime()))
        }, Modifier.fillMaxWidth(), color = U.c.rule)
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.campfire_rise), { answered(); onClose() }, Modifier.fillMaxWidth(), color = U.c.goldLight)
        if (count > 0) Text(T.plural(c, R.plurals.campfire_count, count), style = UType.Small.copy(color = U.c.rule), modifier = Modifier.padding(top = 10.dp))
    }
}

/** A little fire that flickers. */
@Composable
private fun Fire(modifier: Modifier) {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (!SHOTS) while (true) { delay(110); t++ } }
    val gold = U.c.goldLight; val red = U.c.vermilion; val wood = U.c.leather
    Canvas(modifier) {
        val w = size.width; val hgt = size.height
        drawLine(wood, androidx.compose.ui.geometry.Offset(w * 0.2f, hgt * 0.92f), androidx.compose.ui.geometry.Offset(w * 0.8f, hgt * 0.78f), w * 0.07f, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(wood, androidx.compose.ui.geometry.Offset(w * 0.2f, hgt * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.8f, hgt * 0.92f), w * 0.07f, androidx.compose.ui.graphics.StrokeCap.Round)
        val rnd = Random(t)
        for ((col, scale) in listOf(red to 1f, gold to 0.62f)) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f - w * 0.22f * scale, hgt * 0.82f)
                quadraticTo(w * (0.28f + rnd.nextFloat() * 0.06f), hgt * (0.5f), w * (0.48f + rnd.nextFloat() * 0.06f), hgt * (0.82f - 0.7f * scale))
                quadraticTo(w * (0.7f + rnd.nextFloat() * 0.06f), hgt * 0.5f, w * 0.5f + w * 0.22f * scale, hgt * 0.82f)
                close()
            }
            drawPath(p, col)
        }
    }
}

/** A hero looks at their own secret again, the other turning away. */
@Composable
private fun Peek(q: Quest, hall: Hall, onClose: () -> Unit) {
    val c = ctx()
    var who by remember { mutableIntStateOf(-1) }
    var peeking by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Rubric(stringResource(R.string.peek_eyebrow), color = U.c.goldLight)
        Spacer(Modifier.height(16.dp))
        if (who < 0) {
            Text(stringResource(R.string.peek_who), style = UType.Display.copy(color = U.c.page), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(T.hero(c, hall.settings, 0), { who = 0 }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            PrimaryButton(T.hero(c, hall.settings, 1), { who = 1 }, Modifier.fillMaxWidth(), color = U.c.lapis)
        } else {
            Text(stringResource(R.string.secret_no_peeking, T.hero(c, hall.settings, 1 - who)), style = UType.Italic.copy(color = U.c.rule), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(U.RMd).background(if (peeking) U.c.page else U.c.leather).border(1.5.dp, U.c.gold, U.RMd)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(); peeking = true
                            do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                            peeking = false
                        }
                    }.padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (peeking) T.secret(c, hall.settings, who, hall.lore[if (who == 0) q.secretA else q.secretB]) else stringResource(R.string.secret_hold),
                    style = if (peeking) UType.Story else UType.Muted.copy(color = U.c.rule), textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            SoftButton(stringResource(R.string.back_to_road), onClose, Modifier.fillMaxWidth(), color = U.c.goldLight)
        }
    }
}
