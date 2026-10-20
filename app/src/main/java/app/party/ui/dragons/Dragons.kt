package app.party.ui.dragons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.container
import app.party.data.Dragon
import app.party.data.Strike
import app.party.domain.Xp
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.dragon
import app.party.ui.art.dragonColour
import app.party.ui.art.vellum
import app.party.ui.components.Confirm
import app.party.ui.components.Field
import app.party.ui.components.GoldBar
import app.party.ui.components.Leaf
import app.party.ui.components.PrimaryButton
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.Stamp
import app.party.ui.components.TopBar
import app.party.ui.components.haptics
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlin.random.Random

/** "Grimfang the Unpaid": a dragon's name, made up. */
private fun dragonName(c: android.content.Context, seed: Int): String {
    val rnd = Random(seed)
    val names = c.resources.getStringArray(R.array.dragon_names)
    val titles = c.resources.getStringArray(R.array.dragon_titles)
    return c.getString(R.string.dragon_name, names[rnd.nextInt(names.size)], titles[rnd.nextInt(titles.size)])
}

/** The party's dragons: things both have put off, to be worn down a strike at a time. */
@Composable
fun DragonsScreen(hall: Hall, go: Go) {
    val c = ctx()
    var seed by rememberSaveable { mutableIntStateOf(Random.nextInt()) }
    var name by rememberSaveable { mutableStateOf(dragonName(c, seed)) }
    var deed by rememberSaveable { mutableStateOf("") }
    var hp by rememberSaveable { mutableIntStateOf(5) }
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.dragons_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(stringResource(R.string.dragons_intro), style = UType.Italic)
            hall.party.living.forEach { d ->
                Spacer(Modifier.height(12.dp))
                DragonRow(d, hall.party.strikes(d.id).size) { go.dragon(d.id) }
            }
            SectionTitle(stringResource(R.string.dragon_new))
            Leaf {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(110.dp)) { dragon(dragonColour(seed), false) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field(name, { name = it }, stringResource(R.string.dragon_name_field), Modifier.weight(1f), max = 50)
                    Box(Modifier.clip(U.RSm).clickable { seed = Random.nextInt(); name = dragonName(c, seed) }.padding(10.dp)) {
                        Icon(Icons.Rounded.Casino, null, tint = U.c.lapis)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Field(deed, { deed = it }, stringResource(R.string.dragon_deed_field), singleLine = false, minLines = 2, max = 200)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.dragon_hp_pick, hp), style = UType.Body)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 5, 8, 12).forEach { n -> SoftButton("$n", { hp = n }, Modifier.weight(1f), color = if (hp == n) U.c.wax else U.c.faint) }
                }
                Text(stringResource(R.string.dragon_hp_hint), style = UType.Small, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.dragon_summon), {
                    val d = Dragon(name = name.trim(), deed = deed.trim(), hp = hp, seed = seed, createdAt = System.currentTimeMillis())
                    c.container.write { db.dao().insert(d) }
                    deed = ""; seed = Random.nextInt(); name = dragonName(c, seed)
                }, Modifier.fillMaxWidth(), color = U.c.wax, enabled = name.isNotBlank() && deed.isNotBlank())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DragonRow(d: Dragon, hits: Int, onClick: () -> Unit) {
    Leaf(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(56.dp)) { dragon(dragonColour(d.seed), d.slain) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = UType.Title)
                Text(d.deed, style = UType.Small, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                GoldBar(1f - hits.toFloat() / d.hp, color = U.c.vermilion, height = 6.dp)
                Text(stringResource(R.string.dragon_hp, (d.hp - hits).coerceAtLeast(0), d.hp), style = UType.Small)
            }
        }
    }
}

/** One dragon: strike it, see the scars, and see it fall. */
@Composable
fun DragonScreen(id: Long, hall: Hall, go: Go) {
    val c = ctx()
    val d = hall.party.dragons.firstOrNull { it.id == id } ?: return
    val strikes = hall.party.strikes(d.id)
    var note by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val h = haptics(hall.settings.haptics)
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(d.name, go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(200.dp)) { dragon(dragonColour(d.seed), d.slain) }
            if (d.slain) Stamp(stringResource(R.string.dragon_slain_stamp), color = U.c.wax, angle = -8f)
            Spacer(Modifier.height(10.dp))
            Text(d.deed, style = UType.Story, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            GoldBar(1f - strikes.size.toFloat() / d.hp, color = U.c.vermilion)
            Text(stringResource(R.string.dragon_hp, (d.hp - strikes.size).coerceAtLeast(0), d.hp), style = UType.Small)
            if (!d.slain) {
                Spacer(Modifier.height(16.dp))
                Field(note, { note = it }, stringResource(R.string.strike_note), max = 120)
                Spacer(Modifier.height(10.dp))
                PrimaryButton(stringResource(R.string.strike, Xp.STRIKE), {
                    h.thud()
                    val now = System.currentTimeMillis()
                    val n = note.trim()
                    note = ""
                    c.container.write {
                        db.dao().insert(Strike(dragonId = d.id, at = now, note = n))
                        if (strikes.size + 1 >= d.hp) db.dao().update(d.copy(slainAt = now))
                    }
                }, Modifier.fillMaxWidth(), color = U.c.wax)
                Text(stringResource(R.string.strike_hint, Xp.SLAIN), style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            } else {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.bestiary_slain, T.date(c, d.slainAt)), style = UType.Italic)
            }
            if (strikes.isNotEmpty()) {
                SectionTitle(stringResource(R.string.strikes_title), Modifier.fillMaxWidth())
                strikes.reversed().forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("⚔", color = U.c.wax, modifier = Modifier.width(26.dp))
                        Text(s.note.ifBlank { stringResource(R.string.strike_blank) }, style = UType.Body, modifier = Modifier.weight(1f))
                        Text(T.shortDate(c, s.at), style = UType.Small)
                    }
                }
                if (!d.slain) Text(
                    stringResource(R.string.strike_undo), style = UType.Small.copy(color = U.c.lapis),
                    modifier = Modifier.align(Alignment.End).clip(U.RSm).clickable { c.container.write { db.dao().delete(strikes.last()) } }.padding(6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.dragon_forget), style = UType.Small.copy(color = U.c.wax),
                modifier = Modifier.clip(U.RSm).clickable { confirm = true }.padding(8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm) Confirm(stringResource(R.string.dragon_forget), stringResource(R.string.dragon_forget_text), stringResource(R.string.delete), {
        c.container.write { db.dao().deleteDragon(d.id) }; go.back()
    }, { confirm = false })
}
