package app.party.ui.treasury

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.data.School
import app.party.domain.Deed
import app.party.domain.GameMaster
import app.party.domain.Ranks
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.Charges
import app.party.ui.art.dragon
import app.party.ui.art.dragonColour
import app.party.ui.art.vellum
import app.party.ui.components.GoldBar
import app.party.ui.components.Leaf
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.ctx
import app.party.ui.quest.trophyBitmap
import app.party.ui.theme.U
import app.party.ui.theme.UType

/** The treasury: the schools' ranks, the deeds, the trophies, the slain dragons and the tally. */
@Composable
fun TreasuryTab(hall: Hall, go: Go) {
    val c = ctx()
    val p = hall.party
    Column(Modifier.fillMaxSize().vellum().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.treasury_title), style = UType.Title)

        SectionTitle(stringResource(R.string.schools_title))
        Leaf {
            School.entries.forEachIndexed { i, s ->
                val done = p.bySchool[s] ?: 0
                val rank = Ranks.of(done)
                val next = Ranks.next(done)
                Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (rank == 0) "·" else GameMaster.roman(rank), style = UType.Strong.copy(color = if (rank == 0) U.c.faint else U.c.vermilion), modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                    Column(Modifier.weight(1f)) {
                        Text(T.school(c, s), style = UType.Strong)
                        Text(T.schoolRank(c, s, rank), style = UType.Small)
                        Spacer(Modifier.height(4.dp))
                        val prev = if (rank == 0) 0 else Ranks.steps[rank - 1]
                        GoldBar(if (next == null) 1f else (done - prev).toFloat() / (next - prev), height = 5.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(if (next == null) "$done" else "$done/$next", style = UType.Small)
                }
                if (i < School.entries.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(U.c.rule.copy(alpha = 0.5f)))
            }
        }

        SectionTitle(stringResource(R.string.deeds_title, hall.deeds.size, Deed.entries.size))
        Deed.entries.chunked(2).forEach { row ->
            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    val earned = d in hall.deeds
                    Column(
                        Modifier.weight(1f).clip(U.RMd).background(if (earned) U.c.page else U.c.vellumDeep.copy(alpha = 0.5f))
                            .border(1.dp, if (earned) U.c.gold else U.c.rule, U.RMd).padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val gold = U.c.gold; val faint = U.c.faint; val ink = U.c.ink
                            Canvas(Modifier.size(18.dp)) {
                                val star = Charges.star(size.width / 2, size.height / 2, size.width / 2, size.width / 4.4f, 5)
                                drawPath(star, if (earned) gold else faint.copy(alpha = 0.3f))
                                drawPath(star, ink.copy(alpha = if (earned) 0.8f else 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(T.deed(c, d), style = UType.Strong.copy(color = if (earned) U.c.ink else U.c.faint), maxLines = 2)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(T.deedText(c, d), style = UType.Small)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionTitle(stringResource(R.string.trophies_title))
        if (p.trophies.isEmpty()) Text(stringResource(R.string.trophies_empty), style = UType.Italic)
        p.trophies.reversed().chunked(2).forEach { row ->
            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { q ->
                    val bmp = remember(q.trophyPhoto) { trophyBitmap(c, q.trophyPhoto, 400) }
                    Column(Modifier.weight(1f).clip(U.RMd).background(U.c.page).border(1.dp, U.c.rule, U.RMd).clickable { go.page(q.id) }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(U.c.vellumDeep), contentAlignment = Alignment.Center) {
                            if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text("❖", style = UType.Hero.copy(color = U.c.goldDark))
                        }
                        Column(Modifier.padding(10.dp)) {
                            Text(q.trophyName.ifBlank { T.quest(c, hall.lore, q).title }, style = UType.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(q.trophyLore, style = UType.Small, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionTitle(stringResource(R.string.bestiary_title))
        if (p.slain.isEmpty()) Text(stringResource(R.string.bestiary_empty), style = UType.Italic)
        p.slain.forEach { d ->
            Row(Modifier.fillMaxWidth().clip(U.RSm).clickable { go.dragon(d.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(46.dp)) { dragon(dragonColour(d.seed), true) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = UType.Strong)
                    Text(stringResource(R.string.bestiary_slain, T.date(c, d.slainAt)), style = UType.Small)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.dragons_open), { go.dragons() }, Modifier.fillMaxWidth(), color = U.c.wax)

        SectionTitle(stringResource(R.string.tally_title))
        Leaf {
            val rows = listOf(
                R.string.tally_quests to p.finished.size,
                R.string.tally_seal to p.sealMinutes,
                R.string.tally_secrets to p.secretsKept,
                R.string.tally_guessed to p.guessed,
                R.string.tally_encounters to p.encounters,
                R.string.tally_campfire to p.campfires,
                R.string.tally_twenties to p.twenties,
                R.string.tally_ravens to p.errands.size,
                R.string.tally_letters to p.letters.size,
            )
            rows.forEach { (label, n) ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(stringResource(label), style = UType.Body, modifier = Modifier.weight(1f))
                    Text("$n", style = UType.Strong)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
