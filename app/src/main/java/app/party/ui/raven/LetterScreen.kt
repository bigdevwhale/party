package app.party.ui.raven

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.container
import app.party.data.Letter
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.WaxSeal
import app.party.ui.art.vellum
import app.party.ui.components.Field
import app.party.ui.components.Leaf
import app.party.ui.components.PrimaryButton
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.TopBar
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType

/**
 * A letter for the other hero, sealed with the party's arms. The raven keeps it until the next quest,
 * when it's opened before the secrets are dealt. Letters read on past quests can be read again.
 */
@Composable
fun LetterScreen(hall: Hall, go: Go) {
    val c = ctx()
    val s = hall.settings
    var from by rememberSaveable { mutableIntStateOf(-1) }
    var text by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.letters_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.letter_intro), style = UType.Italic)
            Spacer(Modifier.height(16.dp))
            when {
                sent -> {
                    WaxSeal(s.crest, 0, Modifier.size(120.dp).align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.letter_sent), style = UType.Title, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(16.dp))
                    SoftButton(stringResource(R.string.letter_another), { sent = false; from = -1; text = "" }, Modifier.fillMaxWidth())
                }
                from < 0 -> {
                    Text(stringResource(R.string.letter_who), style = UType.Title)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(T.hero(c, s, 0), { from = 0 }, Modifier.weight(1f))
                        PrimaryButton(T.hero(c, s, 1), { from = 1 }, Modifier.weight(1f), color = U.c.lapis)
                    }
                    Text(stringResource(R.string.letter_privacy), style = UType.Small, modifier = Modifier.padding(top = 10.dp))
                }
                else -> {
                    Text(stringResource(R.string.letter_to, T.hero(c, s, 1 - from)), style = UType.Title)
                    Spacer(Modifier.height(10.dp))
                    Field(text, { text = it }, stringResource(R.string.letter_field), singleLine = false, minLines = 6, max = 2000)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(stringResource(R.string.letter_seal), {
                        val l = Letter(from = from, text = text.trim(), createdAt = System.currentTimeMillis())
                        c.container.write { db.dao().insert(l) }
                        sent = true
                    }, Modifier.fillMaxWidth(), color = U.c.wax, enabled = text.isNotBlank())
                }
            }

            val waiting = hall.party.waitingLetters
            if (waiting.isNotEmpty()) {
                SectionTitle(stringResource(R.string.letters_with_raven))
                waiting.forEach { l ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        WaxSeal(s.crest, 0, Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.letter_waiting_line, T.hero(c, s, l.from), T.hero(c, s, 1 - l.from), T.shortDate(c, l.createdAt)), style = UType.Body)
                    }
                }
            }
            val read = hall.party.letters.filter { it.openedAt > 0 }.reversed()
            if (read.isNotEmpty()) {
                SectionTitle(stringResource(R.string.letters_read))
                read.forEach { l ->
                    var open by rememberSaveable(l.id) { mutableStateOf(false) }
                    Leaf(Modifier.padding(bottom = 8.dp), onClick = { open = !open }) {
                        Text(stringResource(R.string.letter_waiting_line, T.hero(c, s, l.from), T.hero(c, s, 1 - l.from), T.shortDate(c, l.createdAt)), style = UType.Small)
                        if (open) {
                            Spacer(Modifier.height(6.dp))
                            Text(l.text, style = UType.Prose)
                        } else {
                            Text(stringResource(R.string.letter_tap), style = UType.Small.copy(color = U.c.lapis), modifier = Modifier.clip(U.RSm).clickable { open = true })
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
