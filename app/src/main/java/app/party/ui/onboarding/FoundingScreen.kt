package app.party.ui.onboarding

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.data.Settings
import app.party.domain.Beast
import app.party.domain.Crest
import app.party.domain.Honours
import app.party.domain.Hour
import app.party.domain.Ordinary
import app.party.domain.Symbol
import app.party.ui.T
import app.party.ui.art.Achievement
import app.party.ui.art.vellum
import app.party.ui.components.Field
import app.party.ui.components.Fleuron
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SoftButton
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType

/** The five questions of the arms, each asked of both heroes in turn. */
private enum class Ask { HOUR, ORDINARY, BEAST, SYMBOL, METAL }

/**
 * Founding the party: who the two heroes are, then the coat of arms answered question by question (each
 * hero alone, passing the phone), then the motto written together.
 */
@Composable
fun FoundingScreen(initial: Settings, onDone: (Settings) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var nameA by rememberSaveable { mutableStateOf(initial.nameA) }
    var nameB by rememberSaveable { mutableStateOf(initial.nameB) }
    var femaleA by rememberSaveable { mutableStateOf(initial.femaleA) }
    var femaleB by rememberSaveable { mutableStateOf(initial.femaleB) }
    // Answers as enum ordinals, A's then B's per question; -1 unanswered.
    var answers by rememberSaveable { mutableStateOf(List(10) { -1 }) }
    var motto by rememberSaveable { mutableStateOf("") }

    val c = ctx()
    val settings = Settings(nameA = nameA.trim(), nameB = nameB.trim(), femaleA = femaleA, femaleB = femaleB)
    fun crest(): Crest {
        fun a(i: Int) = answers[i].coerceAtLeast(0)
        return Crest(
            Hour.entries[a(0)] to Hour.entries[a(1)],
            Ordinary.entries[a(2)] to Ordinary.entries[a(3)],
            Beast.entries[a(4)] to Beast.entries[a(5)],
            Symbol.entries[a(6)] to Symbol.entries[a(7)],
            (a(8) == 0) to (a(9) == 0),
        )
    }

    // Steps: 0 welcome, 1 heroes, 2..11 the ten answers, 12 the arms revealed, 13 motto.
    Column(Modifier.fillMaxSize().vellum().statusBarsPadding().navigationBarsPadding().imePadding()) {
        AnimatedContent(step, label = "founding", modifier = Modifier.weight(1f)) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
                when {
                    s == 0 -> Welcome()
                    s == 1 -> Heroes(nameA, { nameA = it }, nameB, { nameB = it }, femaleA, { femaleA = it }, femaleB, { femaleB = it })
                    s in 2..11 -> {
                        val q = (s - 2) / 2
                        val who = (s - 2) % 2
                        Question(Ask.entries[q], who, T.hero(c, settings, who), answers[s - 2]) { v ->
                            answers = answers.toMutableList().also { it[s - 2] = v }
                        }
                    }
                    s == 12 -> Reveal(crest())
                    else -> Motto(crest(), motto) { motto = it }
                }
            }
        }
        val canGo = when {
            step == 1 -> nameA.isNotBlank() && nameB.isNotBlank()
            step in 2..11 -> answers[step - 2] >= 0
            else -> true
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) SoftButton(stringResource(R.string.back), { step-- }, Modifier.weight(0.4f))
            val label = when (step) {
                0 -> stringResource(R.string.found_begin)
                13 -> stringResource(R.string.found_seal)
                else -> stringResource(R.string.next)
            }
            PrimaryButton(label, {
                if (step < 13) step++ else onDone(settings.copy(crest = crest(), motto = motto.trim()))
            }, Modifier.weight(1f), enabled = canGo)
        }
    }
}

@Composable
private fun Welcome() {
    Spacer(Modifier.height(24.dp))
    Rubric(stringResource(R.string.found_eyebrow))
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.found_title), style = UType.Hero)
    Spacer(Modifier.height(18.dp))
    Text(stringResource(R.string.found_text), style = UType.Story)
    Fleuron()
    Text(stringResource(R.string.found_how), style = UType.Italic)
}

@Composable
private fun Heroes(
    a: String, onA: (String) -> Unit, b: String, onB: (String) -> Unit,
    fa: Boolean, onFa: (Boolean) -> Unit, fb: Boolean, onFb: (Boolean) -> Unit,
) {
    Rubric(stringResource(R.string.found_heroes_eyebrow))
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.found_heroes), style = UType.Display)
    Spacer(Modifier.height(20.dp))
    Field(a, onA, stringResource(R.string.found_name_a), max = 24)
    Spacer(Modifier.height(8.dp))
    Pronoun(fa, onFa)
    Spacer(Modifier.height(20.dp))
    Field(b, onB, stringResource(R.string.found_name_b), max = 24)
    Spacer(Modifier.height(8.dp))
    Pronoun(fb, onFb)
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.found_pronoun_why), style = UType.Small)
}

@Composable
private fun Pronoun(female: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(R.string.found_he), !female) { onChange(false) }
        Chip(stringResource(R.string.found_she), female) { onChange(true) }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(U.RMd)
            .background(if (selected) U.c.vermilionSoft else U.c.page)
            .border(1.dp, if (selected) U.c.vermilion else U.c.rule, U.RMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { Text(text, style = UType.Body.copy(color = if (selected) U.c.vermilion else U.c.ink)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Question(ask: Ask, who: Int, name: String, value: Int, onPick: (Int) -> Unit) {
    val c = ctx()
    Rubric(stringResource(R.string.found_question_n, ask.ordinal + 1, Ask.entries.size))
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.found_for, name), style = UType.Italic.copy(color = if (who == 0) U.c.vermilion else U.c.lapis))
    Spacer(Modifier.height(6.dp))
    val title = when (ask) {
        Ask.HOUR -> R.string.ask_hour
        Ask.ORDINARY -> R.string.ask_ordinary
        Ask.BEAST -> R.string.ask_beast
        Ask.SYMBOL -> R.string.ask_symbol
        Ask.METAL -> R.string.ask_metal
    }
    Text(stringResource(title), style = UType.Display)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(if (who == 0) R.string.found_alone_a else R.string.found_alone_b), style = UType.Small)
    Spacer(Modifier.height(18.dp))
    val options: List<String> = when (ask) {
        Ask.HOUR -> Hour.entries.map { T.hour(c, it) }
        Ask.ORDINARY -> Ordinary.entries.map { T.ordinary(c, it) }
        Ask.BEAST -> Beast.entries.map { T.beast(c, it) }
        Ask.SYMBOL -> Symbol.entries.map { T.symbol(c, it) }
        Ask.METAL -> listOf(T.metal(c, true), T.metal(c, false))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o -> Chip(o, value == i) { onPick(i) } }
    }
}

@Composable
private fun Reveal(crest: Crest) {
    Rubric(stringResource(R.string.found_arms_eyebrow))
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.found_arms), style = UType.Display)
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Achievement(crest, Honours(), Modifier.fillMaxWidth(0.78f)) }
    Spacer(Modifier.height(14.dp))
    val c = ctx()
    val notes = buildList {
        add(if (crest.splitField) c.getString(R.string.arms_split_field) else c.getString(R.string.arms_one_field))
        if (crest.splitOrdinary) add(c.getString(R.string.arms_split_ordinary))
        add(if (crest.twoBeasts) c.getString(R.string.arms_two_beasts) else c.getString(R.string.arms_one_beast))
        if (crest.twoSymbols) add(c.getString(R.string.arms_two_symbols))
        if (crest.gold.first != crest.gold.second) add(c.getString(R.string.arms_two_metals))
    }
    notes.forEach { Text("· $it", style = UType.Body, modifier = Modifier.padding(vertical = 3.dp)) }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.arms_agreed, crest.agreed), style = UType.Italic)
    Text(stringResource(R.string.arms_grows), style = UType.Small, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun Motto(crest: Crest, motto: String, onChange: (String) -> Unit) {
    Rubric(stringResource(R.string.found_motto_eyebrow))
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.found_motto), style = UType.Display)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.found_motto_text), style = UType.Italic)
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Achievement(crest, Honours(), Modifier.width(110.dp))
        Spacer(Modifier.width(16.dp))
        Text(if (motto.isBlank()) stringResource(R.string.motto_placeholder) else T.quote(ctx(), motto), style = UType.Motto, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Field(motto, onChange, stringResource(R.string.found_motto_field), max = 60)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.found_motto_hint), style = UType.Small)
    Spacer(Modifier.size(10.dp))
}
