package app.party.ui.tavern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.party.R
import app.party.container
import app.party.data.Entry
import app.party.data.Errand
import app.party.data.Rank
import app.party.domain.CampaignState
import app.party.domain.GameMaster
import app.party.domain.Xp
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.Achievement
import app.party.ui.art.dragon
import app.party.ui.art.dragonColour
import app.party.ui.art.vellum
import app.party.ui.components.GoldBar
import app.party.ui.components.Leaf
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.Stamp
import app.party.ui.ctx
import app.party.ui.theme.Sans
import app.party.ui.theme.U
import app.party.ui.theme.UType

/** The tavern: the party's arms and level, the road it's on, the raven, the campaign and the board. */
@Composable
fun TavernTab(hall: Hall, go: Go) {
    val c = ctx()
    val party = hall.party
    val s = hall.settings
    Column(Modifier.fillMaxSize().vellum().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        // The party's banner.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Achievement(s.crest, hall.honours, Modifier.width(84.dp).clickable { go.arms() })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).clickable { go.arms() }) {
                Text(c.getString(R.string.party_names, T.hero(c, s, 0), T.hero(c, s, 1)), style = UType.Title, maxLines = 2)
                Spacer(Modifier.height(2.dp))
                Text(c.getString(R.string.level_line, party.level, T.level(c, party.level)), style = UType.Muted)
                Spacer(Modifier.height(8.dp))
                GoldBar(Xp.progress(party.xp))
                Spacer(Modifier.height(3.dp))
                val next = if (party.level >= Xp.MAX_LEVEL) c.getString(R.string.xp_max, party.xp) else c.getString(R.string.xp_line, party.xp, Xp.threshold(party.level + 1))
                Text(next, style = UType.Small)
            }
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.inkSoft) }
        }
        if (s.motto.isNotBlank()) {
            Text(T.quote(c, s.motto), style = UType.Motto, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }

        party.ongoing?.let { q ->
            val text = T.quest(c, hall.lore, q)
            Spacer(Modifier.height(18.dp))
            Leaf(onClick = { go.quest(q.id) }, border = U.c.vermilion) {
                Rubric(stringResource(R.string.on_the_road))
                Spacer(Modifier.height(6.dp))
                Text(text.title, style = UType.Title)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.on_the_road_since, T.shortDate(c, q.startedAt)), style = UType.Small)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.on_the_road_back), { go.quest(q.id) }, Modifier.fillMaxWidth())
            }
        }

        Raven(hall)

        val campaign = hall.campaigns.firstOrNull { !it.complete && !it.locked }
        if (campaign != null) {
            SectionTitle(stringResource(R.string.campaign_title))
            CampaignNotice(hall, campaign, go)
        }

        Row(verticalAlignment = Alignment.Bottom) {
            SectionTitle(stringResource(R.string.board_title), Modifier.weight(1f))
            val left = GameMaster.REROLLS - hall.rerolls
            if (left > 0 && party.ongoing == null) {
                Row(
                    Modifier.padding(bottom = 6.dp).clip(U.RSm).clickable {
                        c.container.write { settings.update { it.copy(rerollWeek = hall.week, rerolls = hall.rerolls + 1) } }
                    }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp), tint = U.c.inkSoft)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.board_tear, left), fontFamily = Sans, fontSize = 13.5.sp, color = U.c.inkSoft)
                }
            }
        }
        Text(stringResource(R.string.board_sub), style = UType.Small, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        hall.board.forEachIndexed { i, e -> Notice(hall, e, if (i % 2 == 0) -1.2f else 1f) { go.notice(e.id) } }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(stringResource(R.string.book_open), { go.book() }, Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.MenuBook)
            SoftButton(stringResource(R.string.custom_open), { go.notice("custom") }, Modifier.weight(1f), icon = Icons.Rounded.Edit, color = U.c.verdigris)
        }

        SectionTitle(stringResource(R.string.dragons_title))
        Leaf(onClick = { go.dragons() }) {
            if (party.living.isEmpty()) {
                Text(stringResource(R.string.dragons_none), style = UType.Body)
                Text(stringResource(R.string.dragons_none_sub), style = UType.Small, modifier = Modifier.padding(top = 4.dp))
            } else {
                party.living.take(3).forEach { d ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(42.dp)) { dragon(dragonColour(d.seed), false) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            val hits = party.strikes(d.id).size
                            GoldBar(1f - hits.toFloat() / d.hp, color = U.c.vermilion, height = 6.dp)
                            Text(stringResource(R.string.dragon_hp, d.hp - hits, d.hp), style = UType.Small)
                        }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.letters_title))
        Leaf(onClick = { go.letter() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MailOutline, null, tint = U.c.wax)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.letters_write), style = UType.Strong)
                    val waiting = party.waitingLetters.size
                    Text(
                        if (waiting > 0) T.plural(c, R.plurals.letters_waiting, waiting) else stringResource(R.string.letters_sub),
                        style = UType.Small,
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** The raven's errand for today. */
@Composable
private fun Raven(hall: Hall) {
    val c = ctx()
    val e = GameMaster.raven(hall.lore, hall.settings.seed, hall.today) ?: return
    val done = hall.today in hall.party.errandDays
    val streak = hall.party.streak(hall.today)
    SectionTitle(stringResource(R.string.raven_title))
    Leaf(border = U.c.night.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.Top) {
            Text("✉", fontSize = 22.sp, color = U.c.night, modifier = Modifier.padding(end = 10.dp, top = 2.dp))
            Column(Modifier.weight(1f)) {
                Text(e.text(T.lang(c)), style = UType.Prose)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (done) {
                        Stamp(stringResource(R.string.raven_done), color = U.c.verdigris)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.raven_undo), fontFamily = Sans, fontSize = 13.sp, color = U.c.faint,
                            modifier = Modifier.clickable { c.container.write { db.dao().deleteErrand(hall.today) } }.padding(4.dp),
                        )
                    } else {
                        SoftButton(stringResource(R.string.raven_do, Xp.ERRAND), {
                            c.container.write { db.dao().insert(Errand(hall.today, e.id, System.currentTimeMillis())) }
                        }, color = U.c.night)
                    }
                    Spacer(Modifier.weight(1f))
                    if (streak > 0) Text(T.plural(c, R.plurals.raven_streak, streak), style = UType.Small)
                }
            }
        }
    }
}

@Composable
private fun CampaignNotice(hall: Hall, st: CampaignState, go: Go) {
    val c = ctx()
    val lang = T.lang(c)
    val next = st.next ?: return
    val ready = hall.now >= st.nextAt
    Leaf(onClick = if (ready && hall.party.ongoing == null) ({ go.notice(next.id) }) else null, border = U.c.gold) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Rubric(st.header.part(lang, 0), Modifier.weight(1f), color = U.c.goldDark)
            Text(stringResource(R.string.campaign_progress, st.done, st.chapters.size), style = UType.Small)
        }
        Spacer(Modifier.height(6.dp))
        Text(c.getString(R.string.campaign_chapter, GameMaster.roman(next.order), next.part(lang, 0)), style = UType.Title)
        Spacer(Modifier.height(8.dp))
        if (ready) {
            Text(if (st.done == 0) st.header.part(lang, 1) else next.part(lang, 2), style = UType.Italic, maxLines = 4, overflow = TextOverflow.Ellipsis)
        } else {
            Text(stringResource(R.string.campaign_wait, T.weekday(c, st.nextAt)), style = UType.Italic)
        }
    }
}

/** A notice pinned to the board, a little askew. */
@Composable
private fun Notice(hall: Hall, e: Entry, tilt: Float, onClick: () -> Unit) {
    val c = ctx()
    val lang = T.lang(c)
    val rank = e.rankOf ?: Rank.COPPER
    val times = hall.party.doneTimes[e.id] ?: 0
    Box(Modifier.fillMaxWidth().padding(vertical = 7.dp).rotate(tilt)) {
        Column(
            Modifier.fillMaxWidth().clip(U.RSm).background(U.c.page).border(1.dp, U.c.rule, U.RSm).clickable(onClick = onClick).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Coin(rank)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Rubric(c.getString(R.string.notice_meta, T.rank(c, rank), e.schoolOf?.let { T.school(c, it) } ?: ""), color = U.c.inkSoft)
                    Text(e.part(lang, 0), style = UType.Title, maxLines = 2)
                }
                if (times > 0) Stamp(stringResource(R.string.done_stamp))
            }
            Spacer(Modifier.height(8.dp))
            Text(e.part(lang, 2), style = UType.Italic, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        // The pin.
        Canvas(Modifier.size(14.dp).align(Alignment.TopCenter).padding(top = 0.dp)) {
            drawCircle(Color(0xFF7A1E1E), size.minDimension / 2, center = Offset(size.width / 2, size.height / 2))
            drawCircle(Color.White.copy(alpha = 0.4f), size.minDimension / 6, center = Offset(size.width * 0.38f, size.height * 0.38f))
        }
    }
}

/** A coin for the quest's rank: copper, silver or gold. */
@Composable
fun Coin(rank: Rank, size: androidx.compose.ui.unit.Dp = 34.dp) {
    val fill = when (rank) {
        Rank.COPPER -> Color(0xFFB8733F)
        Rank.SILVER -> Color(0xFFC9CDD2)
        Rank.GOLD -> Color(0xFFE0B443)
    }
    val ink = U.c.ink
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(fill, r)
        drawCircle(Color.White.copy(alpha = 0.3f), r * 0.75f, center = Offset(r * 0.85f, r * 0.85f))
        drawCircle(ink.copy(alpha = 0.6f), r * 0.78f, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.07f))
        drawCircle(ink, r, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.09f))
        drawPath(app.party.ui.art.Charges.star(r, r, r * 0.45f, r * 0.2f, 5), ink.copy(alpha = 0.55f))
    }
}
