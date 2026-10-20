package app.party.ui.quest

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.party.R
import app.party.container
import app.party.data.Done
import app.party.data.Quest
import app.party.data.Stage
import app.party.domain.Deed
import app.party.domain.GameMaster
import app.party.domain.Party
import app.party.domain.Xp
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.Achievement
import app.party.ui.art.Realm
import app.party.ui.art.realm
import app.party.ui.art.vellum
import app.party.ui.components.Field
import app.party.ui.components.Fleuron
import app.party.ui.components.Illuminated
import app.party.ui.components.Leaf
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.TopBar
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType
import java.io.File

/** The way home: guessing the secrets, the chronicle lines, a trophy and a place on the map. */
@Composable
fun Homecoming(q: Quest, hall: Hall, go: Go) {
    when (q.stage) {
        Stage.GUESS_A -> Guess(q, hall, guesser = 0)
        Stage.GUESS_B -> Guess(q, hall, guesser = 1)
        Stage.LINES -> Lines(q, hall, go)
        Stage.TROPHY -> Trophy(q, hall, go)
        else -> Place(q, hall, go)
    }
}

/** One hero guesses which secret the other was carrying; then the other owns up. */
@Composable
private fun Guess(q: Quest, hall: Hall, guesser: Int) {
    val c = ctx()
    val s = hall.settings
    val owner = 1 - guesser
    val real = if (owner == 0) q.secretA else q.secretB
    val mine = if (guesser == 0) q.secretA else q.secretB
    val choices = remember(q.id, guesser) { GameMaster.choices(hall.lore, real, mine, q.id * 31 + guesser) }
    var picked by rememberSaveable(guesser) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().vellum().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp)) {
            Rubric(stringResource(R.string.home_eyebrow))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.guess_title, T.hero(c, s, guesser), T.hero(c, s, owner)), style = UType.Display)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.guess_sub), style = UType.Italic)
            Spacer(Modifier.height(18.dp))
            choices.forEach { id ->
                val isReal = id == real
                val border = when {
                    picked.isEmpty() -> U.c.rule
                    isReal -> U.c.verdigris
                    id == picked -> U.c.wax
                    else -> U.c.rule
                }
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(U.RMd).background(U.c.page).border(if (picked.isNotEmpty() && (isReal || id == picked)) 2.dp else 1.dp, border, U.RMd)
                        .clickable(enabled = picked.isEmpty()) { picked = id }.padding(16.dp),
                ) {
                    // Each option is read as its owner would have read it.
                    Text(T.secret(c, s, owner, hall.lore[id]), style = UType.Prose)
                    if (picked.isNotEmpty() && isReal) Text(stringResource(R.string.guess_real), style = UType.Rubric.copy(color = U.c.verdigris), modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (picked.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(if (picked == real) R.string.guess_hit else R.string.guess_miss),
                    style = UType.Title.copy(color = if (picked == real) U.c.verdigris else U.c.wax),
                )
                Fleuron()
                Text(stringResource(R.string.guess_owned, T.hero(c, s, owner)), style = UType.Body)
                Spacer(Modifier.height(12.dp))
                val hit = if (picked == real) 1 else 0
                fun answer(done: Int) {
                    val next = if (guesser == 0) Stage.GUESS_B else Stage.LINES
                    saveQuest(c, if (guesser == 0) q.copy(guessedA = hit, doneB = done, stage = next) else q.copy(guessedB = hit, doneA = done, stage = next))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton(stringResource(R.string.owned_yes), { answer(Done.YES) }, Modifier.weight(1f), color = U.c.verdigris)
                    SoftButton(stringResource(R.string.owned_partly), { answer(Done.PARTLY) }, Modifier.weight(1f), color = U.c.goldDark)
                    SoftButton(stringResource(R.string.owned_no), { answer(Done.NO) }, Modifier.weight(1f), color = U.c.wax)
                }
            }
        }
    }
}

/** Each hero writes a line for the chronicle. */
@Composable
private fun Lines(q: Quest, hall: Hall, go: Go) {
    val c = ctx()
    val s = hall.settings
    val text = T.quest(c, hall.lore, q)
    var a by rememberSaveable { mutableStateOf(q.lineA) }
    var b by rememberSaveable { mutableStateOf(q.lineB) }
    val n = hall.party.finished.size + 1
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.lines_bar), { saveQuest(c, q.copy(stage = Stage.JOURNEY)) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
            Rubric(stringResource(R.string.lines_eyebrow))
            Spacer(Modifier.height(8.dp))
            Text(T.chapter(c, n, text.inWhich), style = UType.Display)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.lines_sub), style = UType.Italic)
            Spacer(Modifier.height(18.dp))
            Field(a, { a = it }, stringResource(R.string.lines_of, T.hero(c, s, 0)), singleLine = false, minLines = 3, max = 300)
            Spacer(Modifier.height(12.dp))
            Field(b, { b = it }, stringResource(R.string.lines_of, T.hero(c, s, 1)), singleLine = false, minLines = 3, max = 300)
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            PrimaryButton(stringResource(R.string.next), { saveQuest(c, q.copy(lineA = a.trim(), lineB = b.trim(), stage = Stage.TROPHY)) }, Modifier.fillMaxWidth())
        }
    }
}

/** Loads a trophy photo, scaled down. */
fun trophyBitmap(c: android.content.Context, name: String, max: Int = 900): ImageBitmap? {
    if (name.isBlank()) return null
    val f = File(File(c.filesDir, "trophies"), name)
    if (!f.exists()) return null
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, o)
    var sample = 1
    while (o.outWidth / (sample * 2) >= max && o.outHeight / (sample * 2) >= max) sample *= 2
    return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

/** A trophy from the road: a thing kept, named, maybe photographed, with lore of its own. */
@Composable
private fun Trophy(q: Quest, hall: Hall, go: Go) {
    val c = ctx()
    var name by rememberSaveable { mutableStateOf(q.trophyName) }
    var seed by rememberSaveable { mutableLongStateOf(q.id * 977 + q.startedAt) }
    var photo by rememberSaveable { mutableStateOf(q.trophyPhoto) }
    var pending by rememberSaveable { mutableStateOf("") }
    var shown by remember { mutableIntStateOf(0) }
    val bitmap = remember(photo, shown) { trophyBitmap(c, photo) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { photo = pending; shown++ } else File(File(c.filesDir, "trophies"), pending).delete()
    }
    fun shoot() {
        val dir = File(c.filesDir, "trophies").apply { mkdirs() }
        val file = "q${q.id}_${System.currentTimeMillis()}.jpg"
        pending = file
        val uri = FileProvider.getUriForFile(c, c.packageName + ".files", File(dir, file))
        runCatching { camera.launch(uri) }
    }
    val lore = T.trophyLore(c, seed)
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.trophy_bar), { saveQuest(c, q.copy(stage = Stage.LINES)) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
            Rubric(stringResource(R.string.trophy_eyebrow))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.trophy_title), style = UType.Display)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.trophy_sub), style = UType.Italic)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(U.RMd).background(U.c.vellumDeep).border(1.dp, U.c.rule, U.RMd).clickable { shoot() },
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PhotoCamera, null, tint = U.c.inkSoft, modifier = Modifier.size(36.dp))
                    Text(stringResource(R.string.trophy_photo), style = UType.Muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Field(name, { name = it }, stringResource(R.string.trophy_name), max = 60)
            Spacer(Modifier.height(12.dp))
            Leaf {
                Rubric(stringResource(R.string.trophy_lore_title), color = U.c.goldDark)
                Spacer(Modifier.height(6.dp))
                Text(lore, style = UType.Italic)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.clip(U.RSm).clickable { seed++ }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Casino, null, Modifier.size(18.dp), tint = U.c.lapis)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.trophy_reroll), style = UType.Muted.copy(color = U.c.lapis))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            PrimaryButton(stringResource(R.string.next), {
                val keep = name.isNotBlank() || photo.isNotBlank()
                saveQuest(c, q.copy(trophyName = name.trim(), trophyPhoto = photo, trophyLore = if (keep) lore else "", stage = Stage.PLACE))
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            SoftButton(stringResource(R.string.trophy_none), { saveQuest(c, q.copy(trophyName = "", trophyPhoto = "", trophyLore = "", stage = Stage.PLACE)) }, Modifier.fillMaxWidth(), color = U.c.inkSoft)
        }
    }
}

/** A place on the party's map, then the reckoning. */
@Composable
private fun Place(q: Quest, hall: Hall, go: Go) {
    val c = ctx()
    val realmMap = remember(hall.settings.seed) { Realm(hall.settings.seed) }
    var x by rememberSaveable { mutableStateOf(q.placeX) }
    var y by rememberSaveable { mutableStateOf(q.placeY) }
    var seed by rememberSaveable { mutableLongStateOf(q.startedAt) }
    var name by rememberSaveable { mutableStateOf(q.placeName.ifBlank { T.placeName(c, q.startedAt) }) }
    val pins = remember(hall.party.places) { hall.party.places.map { app.party.ui.art.Pin(it.placeX, it.placeY, it.placeName, it.rankOf == app.party.data.Rank.GOLD) } }

    fun finish(placed: Boolean) {
        val events = hall.party.events(q.id)
        val ended = q.copy(
            placeX = if (placed) x else -1f, placeY = if (placed) y else -1f, placeName = if (placed) name.trim() else "",
            endedAt = System.currentTimeMillis(), chapter = hall.party.finished.size + 1,
        )
        saveQuest(c, ended.copy(xp = Xp.reckon(ended, events).sumOf { it.xp }))
    }

    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.place_bar), { saveQuest(c, q.copy(stage = Stage.TROPHY)) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(stringResource(R.string.place_sub), style = UType.Italic)
            Spacer(Modifier.height(12.dp))
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(1f / Realm.ASPECT).clip(U.RSm).pointerInput(Unit) {
                    detectTapGestures { o -> x = o.x / size.width; y = o.y / size.width }
                },
            ) { realm(realmMap, pins, if (x >= 0f) Offset(x, y) else null) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(name, { name = it }, stringResource(R.string.place_name), Modifier.weight(1f), max = 50)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.clip(U.RSm).clickable { seed++; name = T.placeName(c, seed) }.padding(10.dp)) {
                    Icon(Icons.Rounded.Casino, stringResource(R.string.trophy_reroll), tint = U.c.lapis)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            PrimaryButton(stringResource(R.string.place_finish), { finish(true) }, Modifier.fillMaxWidth(), enabled = x >= 0f)
            Spacer(Modifier.height(6.dp))
            SoftButton(stringResource(R.string.place_skip), { finish(false) }, Modifier.fillMaxWidth(), color = U.c.inkSoft)
        }
    }
}

/** The reckoning: XP line by line, a new level, new deeds, a school's new rank, a campaign's epilogue. */
@Composable
fun Rewards(q: Quest, hall: Hall, go: Go) {
    val c = ctx()
    val text = T.quest(c, hall.lore, q)
    val party = hall.party
    val before = remember(party) {
        Party(party.quests.map { if (it.id == q.id) it.copy(endedAt = 0) else it }, party.events, party.dragons, party.strikes, party.letters, party.errands)
    }
    val campaignsBefore = remember(before) { GameMaster.campaigns(hall.lore, before).count { it.complete } }
    val newDeeds = remember(party) { hall.deeds.filter { !it.earned(before, campaignsBefore) } }
    val lines = remember(q) { Xp.reckon(q, party.events(q.id)) }
    val levelUp = party.level > before.level
    val school = q.schoolOf
    val rankUp = party.rank(school) > before.rank(school)

    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding()) {
        TopBar(stringResource(R.string.rewards_bar), go::home)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Rubric(stringResource(R.string.rewards_eyebrow))
            Spacer(Modifier.height(8.dp))
            Text(T.chapter(c, q.chapter, text.inWhich), style = UType.Display, textAlign = TextAlign.Center)
            if (text.epilogue.isNotBlank()) {
                Fleuron()
                Illuminated(text.epilogue, colour = U.c.lapis)
            }
            Fleuron()
            lines.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(T.reason(c, l.what) + if (l.count > 1 && l.what != Xp.Reason.SEAL) " ×${l.count}" else "", style = UType.Body, modifier = Modifier.weight(1f))
                    Text("+${l.xp}", style = UType.Strong.copy(color = U.c.goldDark))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.rewards_total), style = UType.Title, modifier = Modifier.weight(1f))
                Text("+${q.xp}", style = UType.Title.copy(color = U.c.vermilion))
            }
            if (levelUp) {
                Spacer(Modifier.height(20.dp))
                Leaf(border = U.c.gold) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Rubric(stringResource(R.string.rewards_level), color = U.c.goldDark)
                        Spacer(Modifier.height(6.dp))
                        Text(c.getString(R.string.level_line, party.level, T.level(c, party.level)), style = UType.Title, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Achievement(hall.settings.crest, hall.honours, Modifier.width(140.dp))
                        if (hall.honours != app.party.domain.Honours.of(before.level, campaignsBefore, before.slain.size)) {
                            Text(stringResource(R.string.rewards_arms_grew), style = UType.Italic, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (rankUp) {
                Spacer(Modifier.height(12.dp))
                Leaf {
                    Rubric(stringResource(R.string.rewards_rank))
                    Spacer(Modifier.height(6.dp))
                    Text(T.schoolRank(c, school, party.rank(school)), style = UType.Title)
                }
            }
            if (newDeeds.isNotEmpty()) {
                SectionTitle(stringResource(R.string.rewards_deeds))
                newDeeds.forEach { d ->
                    Leaf(Modifier.padding(bottom = 8.dp), border = U.c.gold) {
                        Text(T.deed(c, d), style = UType.Title)
                        Text(T.deedText(c, d), style = UType.Italic)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            PrimaryButton(stringResource(R.string.rewards_done), go::home, Modifier.fillMaxWidth())
        }
    }
}
