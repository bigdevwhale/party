package app.party.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.container
import app.party.data.Kind
import app.party.data.Quest
import app.party.data.Rank
import app.party.data.School
import app.party.domain.GameMaster
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.components.Illuminated
import app.party.ui.art.vellum
import app.party.ui.components.Field
import app.party.ui.components.Fleuron
import app.party.ui.components.GoldBar
import app.party.ui.components.Leaf
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SectionTitle
import app.party.ui.components.Stamp
import app.party.ui.components.TopBar
import app.party.ui.ctx
import app.party.ui.tavern.Coin
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** A quest notice read in full, to be taken up; "custom" writes one by hand. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticeScreen(id: String, hall: Hall, go: Go) {
    val c = ctx()
    val lang = T.lang(c)
    val scope = rememberCoroutineScope()
    val entry = hall.lore[id]
    val custom = id == "custom" || entry == null
    var title by rememberSaveable { mutableStateOf("") }
    var tasks by rememberSaveable { mutableStateOf("") }
    var school by rememberSaveable { mutableStateOf(School.ROAD) }
    var rank by rememberSaveable { mutableStateOf(Rank.SILVER) }

    fun take() {
        val app = c.container
        val party = hall.party
        val (a, b) = GameMaster.deal(hall.lore, party, Random(System.nanoTime()))
        val q = Quest(
            source = if (custom) "" else id,
            customTitle = if (custom) title.trim() else "",
            customTasks = if (custom) tasks.trim() else "",
            school = if (custom) school.key else (entry?.schoolOf ?: School.ROAD).key,
            rank = (if (custom) rank else entry?.rankOf ?: Rank.SILVER).code.toString(),
            startedAt = System.currentTimeMillis(),
            secretA = a,
            secretB = b,
        )
        scope.launch {
            val newId = withContext(Dispatchers.IO) { app.db.dao().insert(q) }
            go.quest(newId)
        }
    }

    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(if (custom) stringResource(R.string.custom_open) else stringResource(R.string.notice_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (custom) {
                Text(stringResource(R.string.custom_intro), style = UType.Italic)
                Spacer(Modifier.height(16.dp))
                Field(title, { title = it }, stringResource(R.string.custom_title), max = 60)
                Spacer(Modifier.height(10.dp))
                Field(tasks, { tasks = it }, stringResource(R.string.custom_tasks), singleLine = false, minLines = 4, max = 600)
                SectionTitle(stringResource(R.string.custom_school))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    School.entries.forEach { s -> Pick(T.school(c, s), s == school) { school = s } }
                }
                SectionTitle(stringResource(R.string.custom_rank))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rank.entries.forEach { r -> Pick(T.rank(c, r), r == rank) { rank = r } }
                }
            } else {
                val e = entry!!
                val text = hall.lore.questText(e, lang)
                val times = hall.party.doneTimes[e.id] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    e.rankOf?.let { Coin(it) }
                    Spacer(Modifier.width(10.dp))
                    val meta = if (e.kind == Kind.C) {
                        hall.lore[("k." + e.school)]?.part(lang, 0) ?: ""
                    } else c.getString(R.string.notice_meta, T.rank(c, e.rankOf ?: Rank.COPPER), e.schoolOf?.let { T.school(c, it) } ?: "")
                    Rubric(meta, Modifier.weight(1f), color = U.c.inkSoft)
                    if (times > 0) Stamp(T.plural(c, R.plurals.done_times, times))
                }
                Spacer(Modifier.height(10.dp))
                Text(if (e.kind == Kind.C) c.getString(R.string.campaign_chapter, GameMaster.roman(e.order), text.title) else text.title, style = UType.Display)
                Spacer(Modifier.height(14.dp))
                Illuminated(text.story)
                Fleuron()
                Rubric(stringResource(R.string.notice_tasks))
                Spacer(Modifier.height(8.dp))
                text.tasks.forEachIndexed { i, t ->
                    Row(Modifier.padding(vertical = 5.dp)) {
                        Text("${GameMaster.roman(i + 1)}.", style = UType.Body.copy(color = U.c.vermilion), modifier = Modifier.width(34.dp))
                        Text(t, style = UType.Body, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                val flags = buildList {
                    if ("h" in e.flags) add(c.getString(R.string.flag_home))
                    if ("o" in e.flags) add(c.getString(R.string.flag_out))
                    if ("n" in e.flags) add(c.getString(R.string.flag_evening))
                    if ("m" in e.flags) add(c.getString(R.string.flag_money))
                }
                if (flags.isNotEmpty()) Text(flags.joinToString(" · "), style = UType.Small)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.notice_secret_hint), style = UType.Italic)
            }
            Spacer(Modifier.height(20.dp))
        }
        val ongoing = hall.party.ongoing
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (ongoing != null) {
                PrimaryButton(stringResource(R.string.notice_busy), { go.quest(ongoing.id) }, Modifier.fillMaxWidth(), color = U.c.lapis)
            } else {
                PrimaryButton(stringResource(R.string.notice_take), ::take, Modifier.fillMaxWidth(), enabled = !custom || (title.isNotBlank() && tasks.isNotBlank()))
            }
        }
    }
}

@Composable
private fun Pick(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(U.RMd)
            .background(if (selected) U.c.vermilionSoft else U.c.page)
            .border(1.dp, if (selected) U.c.vermilion else U.c.rule, U.RMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) { Text(text, style = UType.Body.copy(color = if (selected) U.c.vermilion else U.c.ink)) }
}

/** The book of quests: every quest of every school, which ones are done, and the campaigns. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookScreen(hall: Hall, go: Go) {
    val c = ctx()
    val lang = T.lang(c)
    var school by rememberSaveable { mutableStateOf<School?>(null) }
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding()) {
        TopBar(stringResource(R.string.book_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            val all = hall.lore.quests
            val done = all.count { (hall.party.doneTimes[it.id] ?: 0) > 0 }
            Text(stringResource(R.string.book_count, done, all.size), style = UType.Italic)
            Spacer(Modifier.height(8.dp))
            GoldBar(done.toFloat() / all.size.coerceAtLeast(1))
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Pick(stringResource(R.string.book_all), school == null) { school = null }
                School.entries.forEach { s -> Pick(T.school(c, s), s == school) { school = s } }
            }

            if (school == null) {
                SectionTitle(stringResource(R.string.campaigns_title))
                hall.campaigns.forEach { st ->
                    Leaf(Modifier.padding(bottom = 10.dp), border = if (st.complete) U.c.gold else U.c.rule) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(st.header.part(lang, 0), style = UType.Title, modifier = Modifier.weight(1f))
                            if (st.complete) Stamp(stringResource(R.string.campaign_done), color = U.c.goldDark)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(st.header.part(lang, 1), style = UType.Italic, maxLines = if (st.locked) 3 else 8, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        if (st.locked) {
                            Text(stringResource(R.string.campaign_locked, st.unlockLevel), style = UType.Small)
                        } else {
                            st.chapters.forEachIndexed { i, ch ->
                                val played = i < st.done
                                Text(
                                    c.getString(R.string.campaign_chapter, GameMaster.roman(ch.order), if (played || i == st.done) ch.part(lang, 0) else "· · ·"),
                                    style = UType.Body.copy(color = if (played) U.c.ink else U.c.faint),
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            (school?.let { listOf(it) } ?: School.entries).forEach { s ->
                val qs = hall.lore.quests(s)
                SectionTitle("${T.school(c, s)} · ${qs.count { (hall.party.doneTimes[it.id] ?: 0) > 0 }}/${qs.size}")
                Text(T.schoolRank(c, s, hall.party.rank(s)), style = UType.Small, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                qs.forEach { e ->
                    val times = hall.party.doneTimes[e.id] ?: 0
                    Row(
                        Modifier.fillMaxWidth().clip(U.RSm).clickable { go.notice(e.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Coin(e.rankOf ?: Rank.COPPER, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.part(lang, 0), style = UType.Body.copy(color = if (times > 0) U.c.ink else U.c.inkSoft), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (times > 0) Text("✓" + (if (times > 1) " ×$times" else ""), style = UType.Strong.copy(color = U.c.verdigris))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
