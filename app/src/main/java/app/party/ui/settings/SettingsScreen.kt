package app.party.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.party.BuildConfig
import app.party.R
import app.party.container
import app.party.data.Backup
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.Language
import app.party.ui.art.vellum
import app.party.ui.components.Confirm
import app.party.ui.components.Field
import app.party.ui.components.Leaf
import app.party.ui.components.SectionTitle
import app.party.ui.components.SoftButton
import app.party.ui.components.TopBar
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(hall: Hall, go: Go) {
    val c = ctx()
    val s = hall.settings
    val app = c.container
    val scope = rememberCoroutineScope()
    var nameA by rememberSaveable { mutableStateOf(s.nameA) }
    var nameB by rememberSaveable { mutableStateOf(s.nameB) }
    var restore by remember { mutableStateOf<String?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.write(c, uri, Backup.encode(app.db.dao().everything(), app.settings.now())) }.isSuccess }
            Toast.makeText(c, if (ok) R.string.backup_saved else R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            restore = withContext(Dispatchers.IO) { runCatching { Backup.read(c, uri) }.getOrNull() }
            if (restore == null) Toast.makeText(c, R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().vellum().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionTitle(stringResource(R.string.settings_heroes))
            Field(nameA, { nameA = it }, stringResource(R.string.found_name_a), max = 24)
            Spacer(Modifier.height(8.dp))
            Field(nameB, { nameB = it }, stringResource(R.string.found_name_b), max = 24)
            Spacer(Modifier.height(8.dp))
            SoftButton(stringResource(R.string.save), {
                app.write { settings.update { it.copy(nameA = nameA.trim(), nameB = nameB.trim()) } }
            }, Modifier.fillMaxWidth(), enabled = nameA.isNotBlank() && nameB.isNotBlank() && (nameA.trim() != s.nameA || nameB.trim() != s.nameB))
            Spacer(Modifier.height(10.dp))
            Toggle(stringResource(R.string.settings_female, nameA.ifBlank { stringResource(R.string.hero_a) }), s.femaleA) { v -> app.write { settings.update { it.copy(femaleA = v) } } }
            Toggle(stringResource(R.string.settings_female, nameB.ifBlank { stringResource(R.string.hero_b) }), s.femaleB) { v -> app.write { settings.update { it.copy(femaleB = v) } } }

            SectionTitle(stringResource(R.string.settings_feel))
            Toggle(stringResource(R.string.settings_haptics), s.haptics) { v -> app.write { settings.update { it.copy(haptics = v) } } }

            SectionTitle(stringResource(R.string.settings_language))
            val current = Language.current()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Language.SYSTEM to R.string.lang_system, Language.EN to R.string.lang_en, Language.RU to R.string.lang_ru).forEach { (tag, label) ->
                    SoftButton(stringResource(label), { Language.set(tag) }, Modifier.weight(1f), color = if (current == tag) U.c.vermilion else U.c.faint)
                }
            }

            SectionTitle(stringResource(R.string.settings_backup))
            Leaf {
                Text(stringResource(R.string.backup_text), style = UType.Muted)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton(stringResource(R.string.backup_export), { export.launch("party-of-two.json") }, Modifier.weight(1f))
                    SoftButton(stringResource(R.string.backup_import), { import.launch(arrayOf("application/json", "*/*")) }, Modifier.weight(1f), color = U.c.wax)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.settings_about, BuildConfig.VERSION_NAME), style = UType.Small)
            Spacer(Modifier.height(24.dp))
        }
    }

    restore?.let { text ->
        Confirm(stringResource(R.string.backup_import), stringResource(R.string.backup_replace), stringResource(R.string.backup_import), {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val (all, apply) = Backup.decode(text)
                        app.db.dao().replaceAll(all)
                        app.settings.update(apply)
                    }.isSuccess
                }
                Toast.makeText(c, if (ok) R.string.backup_restored else R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }, { restore = null })
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = U.c.vermilion, checkedThumbColor = U.c.page))
    }
}
