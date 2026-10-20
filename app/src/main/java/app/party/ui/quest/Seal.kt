package app.party.ui.quest

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.SHOTS
import app.party.data.EventKind
import app.party.data.Quest
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.WaxSeal
import app.party.ui.components.PrimaryButton
import app.party.ui.components.Rubric
import app.party.ui.components.SoftButton
import app.party.ui.components.haptics
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlinx.coroutines.delay

private enum class SealState { CHOOSE, WAITING, SEALED, KEPT, BROKEN }

/**
 * The spell of silence: the phone lies face down for as long as the party chose. Turning it over before
 * the time is up cracks the seal, and the chronicle remembers.
 */
@Composable
fun SealOfSilence(q: Quest, hall: Hall, onClose: () -> Unit) {
    val c = ctx()
    var state by remember { mutableStateOf(if (SHOTS) SealState.KEPT else SealState.CHOOSE) }
    var target by remember { mutableIntStateOf(45) }
    var since by remember { mutableLongStateOf(0L) }
    var minutes by remember { mutableIntStateOf(if (SHOTS) 60 else 0) }
    var faceDown by remember { mutableStateOf(false) }
    val h = haptics(hall.settings.haptics)
    val view = LocalView.current

    // Watch which way the phone faces while the spell is being cast or held.
    val watching = state == SealState.WAITING || state == SealState.SEALED
    DisposableEffect(watching) {
        if (!watching) return@DisposableEffect onDispose { }
        view.keepScreenOn = true
        val sm = c.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val z = e.values[2]
                if (z < -7f) faceDown = true else if (z > 2f) faceDown = false
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener); view.keepScreenOn = false }
    }

    LaunchedEffect(faceDown, state) {
        if (state == SealState.WAITING && faceDown) {
            delay(900)
            since = System.currentTimeMillis(); state = SealState.SEALED; h.thud()
        } else if (state == SealState.SEALED && !faceDown) {
            val held = System.currentTimeMillis() - since
            minutes = (held / 60_000L).toInt()
            if (held >= target * 60_000L) {
                state = SealState.KEPT
                logEvent(c, q, EventKind.SEAL, value = minutes)
                saveQuest(c, q.copy(sealMinutes = q.sealMinutes + minutes))
            } else {
                state = SealState.BROKEN
                h.thud()
                logEvent(c, q, EventKind.CRACK, value = minutes)
                saveQuest(c, q.copy(sealMinutes = q.sealMinutes + minutes, cracks = q.cracks + 1))
            }
        }
    }
    // A soft buzz when the time is up, so the party knows the seal can be lifted.
    LaunchedEffect(state) {
        if (state == SealState.SEALED) {
            delay(target * 60_000L)
            if (state == SealState.SEALED) h.thud()
        }
    }

    if (state == SealState.SEALED) {
        // Face down: a dark page, nearly no light.
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.seal_held), style = UType.Italic.copy(color = Color(0xFF3A3530)), textAlign = TextAlign.Center)
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Rubric(stringResource(R.string.seal_eyebrow), color = U.c.goldLight)
        Spacer(Modifier.height(16.dp))
        WaxSeal(hall.settings.crest, if (state == SealState.BROKEN) 1 + q.cracks.coerceAtMost(2) else 0, Modifier.size(170.dp), glow = if (state == SealState.KEPT) 1f else 0f)
        Spacer(Modifier.height(18.dp))
        when (state) {
            SealState.CHOOSE -> {
                Text(stringResource(R.string.seal_text), style = UType.Story.copy(color = U.c.page), textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 45, 60, 90).forEach { m ->
                        SoftButton(c.getString(R.string.seal_minutes, m), { target = m }, Modifier.weight(1f), color = if (target == m) U.c.goldLight else U.c.rule)
                    }
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton(stringResource(R.string.seal_cast), { state = SealState.WAITING }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                SoftButton(stringResource(R.string.back_to_road), onClose, Modifier.fillMaxWidth(), color = U.c.goldLight)
            }
            SealState.WAITING -> {
                Text(stringResource(R.string.seal_lay_down), style = UType.Story.copy(color = U.c.page), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.seal_lay_sub, target), style = UType.Small.copy(color = U.c.rule), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                SoftButton(stringResource(R.string.cancel), { state = SealState.CHOOSE }, Modifier.fillMaxWidth(), color = U.c.rule)
            }
            SealState.KEPT -> {
                Text(stringResource(R.string.seal_kept), style = UType.Display.copy(color = U.c.goldLight), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(T.plural(c, R.plurals.seal_kept_sub, minutes), style = UType.Italic.copy(color = U.c.page), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(stringResource(R.string.back_to_road), onClose, Modifier.fillMaxWidth())
            }
            SealState.BROKEN -> {
                Text(stringResource(R.string.seal_broken), style = UType.Display.copy(color = U.c.page), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(T.plural(c, R.plurals.seal_broken_sub, minutes), style = UType.Italic.copy(color = U.c.rule), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(stringResource(R.string.seal_again), { state = SealState.CHOOSE }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                SoftButton(stringResource(R.string.back_to_road), onClose, Modifier.fillMaxWidth(), color = U.c.goldLight)
            }
            SealState.SEALED -> {}
        }
    }
}
