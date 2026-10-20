package app.party.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.party.R
import app.party.ui.art.DropCap
import app.party.ui.theme.Sans
import app.party.ui.theme.U
import app.party.ui.theme.UType

/** A leaf of the book: lighter vellum, a double rule round it. */
@Composable
fun Leaf(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(18.dp), border: Color = U.c.rule, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(U.c.page)
            .border(1.dp, border, U.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp)
            .border(0.6.dp, border.copy(alpha = 0.6f), U.RSm)
            .padding(padding),
        content = content,
    )
}

/** A rubric: a small heading in red, as scribes marked sections. */
@Composable
fun Rubric(text: String, modifier: Modifier = Modifier, color: Color = U.c.vermilion) {
    Text(text.uppercase(), style = UType.Rubric.copy(color = color), modifier = modifier)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Rubric(text, modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
}

/** A rule with a fleuron in the middle. */
@Composable
fun Fleuron(modifier: Modifier = Modifier, color: Color = U.c.vermilion) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).height(1.dp)) { drawLine(color.copy(alpha = 0.4f), Offset(0f, 0f), Offset(size.width, 0f), 2f) }
        Text("❦", color = color, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Canvas(Modifier.weight(1f).height(1.dp)) { drawLine(color.copy(alpha = 0.4f), Offset(0f, 0f), Offset(size.width, 0f), 2f) }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = U.c.vermilion, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp).border(1.5.dp, if (enabled) U.c.gold else Color.Transparent, U.RMd),
        shape = U.RMd,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = U.c.page,
            disabledContainerColor = U.c.rule,
            disabledContentColor = U.c.faint,
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Quiet button: ink on a faint wash. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.lapis, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .height(50.dp)
            .clip(U.RMd)
            .background(color.copy(alpha = if (enabled) 0.1f else 0.04f))
            .border(1.dp, color.copy(alpha = if (enabled) 0.35f else 0.12f), U.RMd)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) color else color.copy(alpha = 0.4f)
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontFamily = Sans, fontSize = 15.5.sp, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 8.dp, top = 4.dp).height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = U.c.ink) }
        Text(title, style = UType.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minLines: Int = 1, max: Int = 400) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(max)) },
        label = { Text(label, fontFamily = Sans) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = UType.Body,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
        shape = U.RMd,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = U.c.lapis,
            unfocusedBorderColor = U.c.rule,
            focusedLabelColor = U.c.lapis,
            unfocusedContainerColor = U.c.page,
            focusedContainerColor = U.c.page,
            cursorColor = U.c.vermilion,
        ),
    )
}

@Composable
fun Confirm(title: String, text: String, yes: String, onYes: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.page,
        title = { Text(title, style = UType.Title) },
        text = { Text(text, style = UType.Body) },
        confirmButton = { TextButton(onClick = { onYes(); onDismiss() }) { Text(yes, color = U.c.vermilion, fontFamily = Sans, fontSize = 16.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.inkSoft, fontFamily = Sans, fontSize = 16.sp) } },
    )
}

/** A thin gold bar, filled to [progress]. */
@Composable
fun GoldBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, color: Color = U.c.gold) {
    val track = U.c.rule
    val ink = U.c.ink
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2
        drawLine(track, Offset(r, r), Offset(size.width - r, r), size.height, StrokeCap.Round)
        if (progress > 0f) drawLine(color, Offset(r, r), Offset(r + (size.width - 2 * r) * progress.coerceIn(0f, 1f), r), size.height, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.15f), Offset(r, r), Offset(size.width - r, r), size.height, StrokeCap.Round)
    }
}

/** An ink stamp, tilted: "Done", "Taken". */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, color: Color = U.c.vermilion, angle: Float = -12f) {
    Box(
        modifier.rotate(angle).border(2.dp, color.copy(alpha = 0.8f), U.RSm).padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), fontFamily = Sans, fontSize = 12.sp, letterSpacing = 1.5.sp, color = color.copy(alpha = 0.85f)) }
}

/** A story with an illuminated initial beside its opening, when it opens with a letter. */
@Composable
fun Illuminated(text: String, modifier: Modifier = Modifier, colour: Color = U.c.vermilion, size: Dp = 54.dp, style: androidx.compose.ui.text.TextStyle = UType.Story) {
    val first = text.firstOrNull()
    if (first == null || !first.isLetter()) {
        Text(text, style = style, modifier = modifier, textAlign = TextAlign.Start)
        return
    }
    Row(modifier) {
        DropCap(first, size, colour)
        Spacer(Modifier.width(12.dp))
        Text(text.drop(1), style = style, modifier = Modifier.weight(1f).padding(top = 4.dp))
    }
}

/** Haptic ticks, if the party wants them. */
class Haptics(private val view: View, private val on: Boolean) {
    fun tick() { if (on) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun thud() { if (on) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
}

@Composable
fun haptics(on: Boolean): Haptics = Haptics(LocalView.current, on)

/** Light status and navigation icons while a dark page is shown; the vellum's dark icons come back after. */
@Composable
fun DarkBars() {
    val view = LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val bars = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
        onDispose {
            bars?.isAppearanceLightStatusBars = true
            bars?.isAppearanceLightNavigationBars = true
        }
    }
}
