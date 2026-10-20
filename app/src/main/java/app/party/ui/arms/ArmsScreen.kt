package app.party.ui.arms

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.container
import app.party.domain.Xp
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.Achievement
import app.party.ui.art.vellum
import app.party.ui.components.Field
import app.party.ui.components.Fleuron
import app.party.ui.components.GoldBar
import app.party.ui.components.Leaf
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.TopBar
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType

/** The party's arms in full: the blazon, the honours earned and to come, the road of levels. */
@Composable
fun ArmsScreen(hall: Hall, go: Go) {
    val c = ctx()
    val s = hall.settings
    val crest = s.crest
    val p = hall.party
    var motto by rememberSaveable { mutableStateOf(s.motto) }
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.arms_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Achievement(crest, hall.honours, Modifier.fillMaxWidth(0.8f))
            if (s.motto.isNotBlank()) Text(T.quote(c, s.motto), style = UType.Motto, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(10.dp))
            Text(c.getString(R.string.party_names, T.hero(c, s, 0), T.hero(c, s, 1)), style = UType.Title, textAlign = TextAlign.Center)
            Text(c.getString(R.string.level_line, p.level, T.level(c, p.level)), style = UType.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            GoldBar(Xp.progress(p.xp))
            Spacer(Modifier.height(4.dp))
            Text(if (p.level >= Xp.MAX_LEVEL) c.getString(R.string.xp_max, p.xp) else c.getString(R.string.xp_line, p.xp, Xp.threshold(p.level + 1)), style = UType.Small)

            Fleuron()
            // The blazon, in words.
            SectionTitle(stringResource(R.string.blazon_title), Modifier.fillMaxWidth())
            Leaf {
                fun both(a: String, b: String) = if (a == b) a else c.getString(R.string.blazon_both, a, b)
                Line(stringResource(R.string.blazon_field), both(T.hour(c, crest.hour.first), T.hour(c, crest.hour.second)))
                Line(stringResource(R.string.blazon_ordinary), both(T.ordinary(c, crest.ordinary.first), T.ordinary(c, crest.ordinary.second)))
                Line(stringResource(R.string.blazon_beast), both(T.beast(c, crest.beast.first), T.beast(c, crest.beast.second)))
                Line(stringResource(R.string.blazon_symbol), both(T.symbol(c, crest.symbol.first), T.symbol(c, crest.symbol.second)))
                Line(stringResource(R.string.blazon_metal), both(T.metal(c, crest.gold.first), T.metal(c, crest.gold.second)))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.arms_agreed, crest.agreed), style = UType.Italic)
            }

            SectionTitle(stringResource(R.string.honours_title), Modifier.fillMaxWidth())
            Leaf {
                val h = hall.honours
                listOf(
                    Triple(5, R.string.honour_bordure, h.bordure),
                    Triple(8, R.string.honour_helm, h.helm),
                    Triple(12, R.string.honour_mantling, h.mantling),
                    Triple(16, R.string.honour_crown, h.crown),
                    Triple(20, R.string.honour_laurel, h.laurel),
                ).forEach { (lvl, name, got) ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(if (got) "✓" else "·", style = UType.Strong.copy(color = if (got) U.c.verdigris else U.c.faint), modifier = Modifier.width(24.dp))
                        Text(stringResource(name), style = UType.Body.copy(color = if (got) U.c.ink else U.c.inkSoft), modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.honour_level, lvl), style = UType.Small)
                    }
                }
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text(if (h.campaigns > 0) "✓" else "·", style = UType.Strong.copy(color = if (h.campaigns > 0) U.c.verdigris else U.c.faint), modifier = Modifier.width(24.dp))
                    Text(stringResource(R.string.honour_stars, h.campaigns), style = UType.Body, modifier = Modifier.weight(1f))
                }
            }

            SectionTitle(stringResource(R.string.levels_title), Modifier.fillMaxWidth())
            Leaf {
                (1..Xp.MAX_LEVEL).forEach { l ->
                    val reached = p.level >= l
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("$l", style = UType.Strong.copy(color = if (reached) U.c.vermilion else U.c.faint), modifier = Modifier.width(32.dp))
                        Text(T.level(c, l), style = UType.Body.copy(color = if (reached) U.c.ink else U.c.faint), modifier = Modifier.weight(1f))
                        Text("${Xp.threshold(l)}", style = UType.Small)
                    }
                }
            }

            SectionTitle(stringResource(R.string.motto_title), Modifier.fillMaxWidth())
            Field(motto, { motto = it }, stringResource(R.string.found_motto_field), max = 60)
            Spacer(Modifier.height(8.dp))
            SoftButton(stringResource(R.string.save), { c.container.write { settings.update { it.copy(motto = motto.trim()) } } }, Modifier.fillMaxWidth(), enabled = motto.trim() != s.motto)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = UType.Muted, modifier = Modifier.width(110.dp))
        Text(value, style = UType.Body, modifier = Modifier.weight(1f))
    }
}
