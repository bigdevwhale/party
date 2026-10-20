package app.party.ui.chronicle

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.data.Done
import app.party.data.EventKind
import app.party.data.Quest
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.Achievement
import app.party.ui.art.WaxSeal
import app.party.ui.art.vellum
import app.party.ui.components.Fleuron
import app.party.ui.components.Illuminated
import app.party.ui.components.Rubric
import app.party.ui.components.TopBar
import app.party.ui.ctx
import app.party.ui.quest.trophyBitmap
import app.party.ui.theme.U
import app.party.ui.theme.UType

/** The chronicle: a book of the party's chapters, the prologue first, leafed through page by page. */
@Composable
fun ChronicleTab(hall: Hall, go: Go) {
    val chapters = hall.party.finished
    // The newest chapter is where the book falls open.
    val pager = rememberPagerState(initialPage = chapters.size) { chapters.size + 1 }
    Column(Modifier.fillMaxSize().vellum().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.chronicle_title), style = UType.Title, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.chronicle_page, pager.currentPage + 1, chapters.size + 1), style = UType.Small)
        }
        HorizontalPager(pager, Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp), pageSpacing = 10.dp) { i ->
            if (i == 0) Prologue(hall) else ChapterPage(chapters[i - 1], hall, go)
        }
    }
}

/** A page of the book, with a ruled frame. */
@Composable
private fun Folio(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().padding(bottom = 12.dp).clip(U.RSm).vellum(U.c.page, seed = 3).border(1.dp, U.c.rule, U.RSm).padding(5.dp).border(0.6.dp, U.c.vermilion.copy(alpha = 0.35f), U.RSm),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp)) { content() }
    }
}

@Composable
private fun Prologue(hall: Hall) {
    val c = ctx()
    val s = hall.settings
    Folio {
        Rubric(stringResource(R.string.prologue_eyebrow), Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.prologue_title), style = UType.Display, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Achievement(s.crest, hall.honours, Modifier.fillMaxWidth(0.6f).align(Alignment.CenterHorizontally))
        if (s.motto.isNotBlank()) Text(T.quote(c, s.motto), style = UType.Motto, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Fleuron()
        val founded = if (s.foundedAt > 0) T.date(c, s.foundedAt) else "—"
        Illuminated(stringResource(R.string.prologue_text, T.hero(c, s, 0), T.hero(c, s, 1), founded))
        Spacer(Modifier.height(12.dp))
        if (hall.party.finished.isEmpty()) Text(stringResource(R.string.prologue_empty), style = UType.Italic)
        else Text(stringResource(R.string.prologue_swipe), style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** One chapter as a page of the chronicle. */
@Composable
fun ChapterPage(q: Quest, hall: Hall, go: Go?) {
    val c = ctx()
    val s = hall.settings
    val lang = T.lang(c)
    val text = T.quest(c, hall.lore, q)
    val events = hall.party.events(q.id)
    val photo = remember(q.trophyPhoto) { trophyBitmap(c, q.trophyPhoto, 700) }
    Folio {
        Text(T.date(c, q.endedAt).uppercase(), style = UType.Small, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Text(T.chapter(c, q.chapter, text.inWhich), style = UType.Title.copy(color = U.c.vermilion), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Text(text.title, style = UType.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        // The heroes' own lines, each with an initial in their colour.
        if (q.lineA.isNotBlank()) {
            Illuminated(q.lineA, colour = U.c.vermilion, size = 46.dp, style = UType.Prose)
            Text("— " + T.hero(c, s, 0), style = UType.Small, modifier = Modifier.align(Alignment.End).padding(top = 2.dp, bottom = 12.dp))
        }
        if (q.lineB.isNotBlank()) {
            Illuminated(q.lineB, colour = U.c.lapis, size = 46.dp, style = UType.Prose)
            Text("— " + T.hero(c, s, 1), style = UType.Small, modifier = Modifier.align(Alignment.End).padding(top = 2.dp, bottom = 12.dp))
        }
        if (q.lineA.isBlank() && q.lineB.isBlank() && text.story.isNotBlank()) Illuminated(text.story, style = UType.Prose)

        Fleuron()
        // Marginalia: what happened on the road.
        Rubric(stringResource(R.string.page_margins))
        Spacer(Modifier.height(6.dp))
        val notes = buildList {
            add(c.getString(R.string.page_tasks, q.tasksCount, text.tasks.size))
            listOf(0 to q.doneA, 1 to q.doneB).forEach { (who, d) ->
                val sec = T.secret(c, s, who, hall.lore[if (who == 0) q.secretA else q.secretB])
                val how = when (d) { Done.YES -> c.getString(R.string.page_secret_yes); Done.PARTLY -> c.getString(R.string.page_secret_partly); else -> c.getString(R.string.page_secret_no) }
                if (sec.isNotBlank()) add(c.getString(R.string.page_secret, T.hero(c, s, who), sec, how))
            }
            val guessed = listOf(q.guessedA, q.guessedB).count { it == 1 }
            add(c.resources.getStringArray(R.array.page_guessed)[guessed])
            events.filter { it.kindOf == EventKind.ENCOUNTER }.forEach { e -> hall.lore[e.ref]?.let { add(c.getString(R.string.page_met, it.part(lang, 0))) } }
            events.filter { it.kindOf == EventKind.ROLL && (it.value == 20 || it.value == 1) }.forEach { e ->
                add(c.getString(R.string.page_rolled, e.value, hall.lore[e.ref]?.part(lang, 0) ?: ""))
            }
            val fire = events.count { it.kindOf == EventKind.CAMPFIRE }
            if (fire > 0) add(T.plural(c, R.plurals.page_campfire, fire))
        }
        notes.forEach { Text("· $it", style = UType.Italic, modifier = Modifier.padding(vertical = 2.dp)) }

        if (q.sealMinutes > 0 || q.cracks > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaxSeal(s.crest, q.cracks.coerceAtMost(3), Modifier.size(54.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (q.cracks == 0) T.plural(c, R.plurals.page_seal_kept, q.sealMinutes) else c.getString(R.string.page_seal_cracked, q.sealMinutes, q.cracks),
                    style = UType.Italic, modifier = Modifier.weight(1f),
                )
            }
        }
        if (q.trophyName.isNotBlank() || photo != null) {
            Spacer(Modifier.height(14.dp))
            Rubric(stringResource(R.string.page_trophy), color = U.c.goldDark)
            Spacer(Modifier.height(6.dp))
            if (photo != null) Image(photo, null, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(U.RSm).border(1.dp, U.c.rule, U.RSm), contentScale = ContentScale.Crop)
            if (q.trophyName.isNotBlank()) Text(q.trophyName, style = UType.Strong, modifier = Modifier.padding(top = 6.dp))
            if (q.trophyLore.isNotBlank()) Text(q.trophyLore, style = UType.Italic)
        }
        if (q.placeName.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.page_place, q.placeName), style = UType.Small)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("+${q.xp} XP", style = UType.Small.copy(color = U.c.goldDark), modifier = if (go != null) Modifier.clickable { go.quest(q.id) } else Modifier)
        }
    }
}

/** A single chapter, opened from the map. */
@Composable
fun PageScreen(id: Long, hall: Hall, go: Go) {
    val q = hall.party.finished.firstOrNull { it.id == id } ?: return
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding()) {
        TopBar(stringResource(R.string.chronicle_title), go::back)
        Box(Modifier.weight(1f).padding(horizontal = 14.dp)) { ChapterPage(q, hall, go) }
    }
}
