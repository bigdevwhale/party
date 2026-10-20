package app.party.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import app.party.domain.Crest
import app.party.domain.Honours
import app.party.domain.Ordinary
import app.party.domain.Tincture
import app.party.ui.theme.BookPalette
import app.party.ui.theme.tincture
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val Ink = BookPalette.ink

/** A heater shield in [r]: straight top, sides that curve to a point. */
fun shieldPath(r: Rect): Path = Path().apply {
    val w = r.width; val h = r.height
    moveTo(r.left, r.top)
    lineTo(r.right, r.top)
    lineTo(r.right, r.top + h * 0.42f)
    cubicTo(r.right, r.top + h * 0.78f, r.left + w * 0.72f, r.top + h * 0.9f, r.left + w / 2, r.bottom)
    cubicTo(r.left + w * 0.28f, r.top + h * 0.9f, r.left, r.top + h * 0.78f, r.left, r.top + h * 0.42f)
    close()
}

/** Just the shield, filling [r]. */
fun DrawScope.shield(crest: Crest, r: Rect, bordure: Boolean = false) {
    val outline = shieldPath(r)
    val w = r.width; val h = r.height
    val (a, b) = crest.hour
    clipPath(outline) {
        // The field, split down the middle where the two chose different hours.
        drawRect(tincture(a.tincture), r.topLeft, r.size)
        if (crest.splitField) drawRect(tincture(b.tincture), Offset(r.center.x, r.top), Size(w / 2, h))

        // The ordinary; two halves of two ordinaries where they chose different roads.
        val metal = tincture(crest.ordinaryMetal)
        if (crest.splitOrdinary) {
            clipRect(r.left, r.top, r.center.x, r.bottom) { ordinary(crest.ordinary.first, r, metal) }
            clipRect(r.center.x, r.top, r.right, r.bottom) { ordinary(crest.ordinary.second, r, metal) }
        } else {
            ordinary(crest.ordinary.first, r, metal)
        }

        // The chief: a band of the symbols across the top.
        val chargeMetal = tincture(crest.chargeMetal)
        val size = w * 0.17f
        val ys = r.top + h * 0.06f
        val symbols = if (crest.twoSymbols) listOf(crest.symbol.first, crest.symbol.second, crest.symbol.first) else List(3) { crest.symbol.first }
        symbols.forEachIndexed { i, s ->
            val x = r.left + w * (0.2f + 0.3f * i) - size / 2
            val field = fieldAt(crest, x + size / 2, r)
            charge(Charges.symbol(s), Rect(Offset(x, ys), Size(size, size)), chargeMetal, field, Ink)
        }

        // The beasts: one in the middle, or two facing each other.
        if (crest.twoBeasts) {
            val bs = w * 0.4f
            val y = r.top + h * 0.36f
            charge(Charges.beast(crest.beast.first), Rect(Offset(r.left + w * 0.07f, y), Size(bs, bs)), chargeMetal, tincture(a.tincture), Ink, flip = true)
            charge(Charges.beast(crest.beast.second), Rect(Offset(r.right - w * 0.07f - bs, y), Size(bs, bs)), chargeMetal, tincture(crest.hour.second.tincture), Ink)
        } else {
            val bs = w * 0.5f
            charge(Charges.beast(crest.beast.first), Rect(Offset(r.center.x - bs / 2, r.top + h * 0.32f), Size(bs, bs)), chargeMetal, fieldAt(crest, r.center.x - bs * 0.2f, r), Ink)
        }

        if (bordure) {
            drawPath(outline, tincture(Tincture.OR), style = Stroke(w * 0.14f))
            // A pricked line along the border.
            val inner = shieldPath(Rect(r.left + w * 0.035f, r.top + w * 0.035f, r.right - w * 0.035f, r.bottom - w * 0.05f))
            drawPath(inner, Ink.copy(alpha = 0.35f), style = Stroke(w * 0.012f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.02f, w * 0.05f))))
        }

        // Light on the paint, as on a gilded page.
        drawRect(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(r.left + w * 0.3f, r.top + h * 0.22f), radius = w * 0.8f),
            r.topLeft, r.size,
        )
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)), r.top + h * 0.5f, r.bottom), r.topLeft, r.size)
    }
    drawPath(outline, Ink, style = Stroke(w * 0.025f, join = StrokeJoin.Round))
}

private fun fieldAt(crest: Crest, x: Float, r: Rect): Color =
    tincture(if (crest.splitField && x > r.center.x) crest.hour.second.tincture else crest.hour.first.tincture)

private fun DrawScope.ordinary(o: Ordinary, r: Rect, metal: Color) {
    val w = r.width; val h = r.height
    val edge = Stroke(w * 0.012f)
    when (o) {
        Ordinary.BEND -> {
            val band = w * 0.2f
            rotate(degrees = (kotlin.math.atan2(h.toDouble(), w.toDouble()) * 180 / PI).toFloat(), pivot = r.center) {
                val len = kotlin.math.hypot(w, h) * 1.2f
                val rr = Rect(Offset(r.center.x - len / 2, r.center.y - band / 2), Size(len, band))
                drawRect(metal, rr.topLeft, rr.size)
                drawLine(Ink, rr.topLeft, Offset(rr.right, rr.top), edge.width)
                drawLine(Ink, Offset(rr.left, rr.bottom), rr.bottomRight, edge.width)
            }
        }
        Ordinary.CHEVRON -> {
            val p = Path().apply {
                moveTo(r.left - w * 0.1f, r.top + h * 0.92f); lineTo(r.center.x, r.top + h * 0.42f); lineTo(r.right + w * 0.1f, r.top + h * 0.92f)
            }
            drawPath(p, Ink, style = Stroke(w * 0.2f + edge.width * 2, join = StrokeJoin.Miter))
            drawPath(p, metal, style = Stroke(w * 0.2f, join = StrokeJoin.Miter))
        }
        Ordinary.WAVES -> {
            for (k in 0..1) {
                val y0 = r.top + h * (0.56f + k * 0.17f)
                val p = Path().apply {
                    val n = 4
                    moveTo(r.left - 10f, y0)
                    for (i in 0 until n) {
                        val x0 = r.left + w * i / n
                        quadraticTo(x0 + w / (n * 4), y0 - h * 0.035f, x0 + w / (n * 2), y0)
                        quadraticTo(x0 + w * 3 / (n * 4), y0 + h * 0.035f, x0 + w / n, y0)
                    }
                }
                drawPath(p, Ink, style = Stroke(h * 0.075f + edge.width * 2, cap = StrokeCap.Butt))
                drawPath(p, metal, style = Stroke(h * 0.075f, cap = StrokeCap.Butt))
            }
        }
        Ordinary.CROSS -> {
            val t = w * 0.17f
            val v = Rect(Offset(r.center.x - t / 2, r.top - 2f), Size(t, h + 4f))
            val hz = Rect(Offset(r.left - 2f, r.top + h * 0.42f - t / 2), Size(w + 4f, t))
            listOf(v, hz).forEach { drawRect(Ink, it.topLeft - Offset(edge.width, edge.width), Size(it.width + edge.width * 2, it.height + edge.width * 2)) }
            listOf(v, hz).forEach { drawRect(metal, it.topLeft, it.size) }
        }
        Ordinary.FESS -> {
            val t = h * 0.2f
            val rr = Rect(Offset(r.left - 2f, r.top + h * 0.5f - t / 2), Size(w + 4f, t))
            drawRect(metal, rr.topLeft, rr.size)
            drawLine(Ink, rr.topLeft, Offset(rr.right, rr.top), edge.width)
            drawLine(Ink, Offset(rr.left, rr.bottom), rr.bottomRight, edge.width)
        }
    }
}

/**
 * The full achievement: the shield and whatever the party has earned around it (helm, mantling,
 * crown, laurel, campaign stars). Fills a box 1 : 1.18.
 */
@Composable
fun Achievement(crest: Crest, honours: Honours, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f / 1.18f)) { achievement(crest, honours, size) }
}

fun DrawScope.achievement(crest: Crest, honours: Honours, sz: Size) {
    val W = sz.width
    val sw = W * 0.6f
    val sh = sw * 1.17f
    val top = sz.height - sh - W * 0.04f
    val r = Rect(Offset((W - sw) / 2, top), Size(sw, sh))
    val gold = tincture(Tincture.OR)
    val colour = tincture(crest.hour.first.tincture)

    if (honours.laurel) laurel(r, W)
    if (honours.mantling) mantling(r, colour, tincture(crest.ordinaryMetal))
    if (honours.helm) helm(Rect(Offset(r.center.x - sw * 0.2f, r.top - sw * 0.36f), Size(sw * 0.4f, sw * 0.42f)))
    if (honours.crown) crown(Rect(Offset(r.center.x - sw * 0.22f, r.top - sw * 0.56f), Size(sw * 0.44f, sw * 0.2f)), gold)
    shield(crest, r, honours.bordure)

    // A star above for every campaign seen through to the end.
    if (honours.campaigns > 0) {
        val n = honours.campaigns.coerceAtMost(3)
        val s = W * 0.07f
        val y = if (honours.crown) r.top - sw * 0.7f else if (honours.helm) r.top - sw * 0.5f else r.top - s * 1.6f
        for (i in 0 until n) {
            val x = W / 2 + (i - (n - 1) / 2f) * s * 1.6f
            val p = Charges.star(x, y, s / 2, s / 4.6f, 5)
            drawPath(p, gold); drawPath(p, Ink, style = Stroke(1.6f))
        }
    }
}

private fun DrawScope.helm(r: Rect) {
    val steel = Color(0xFFB9BCC2)
    val p = Path().apply {
        moveTo(r.left + r.width * 0.12f, r.bottom)
        cubicTo(r.left - r.width * 0.05f, r.top + r.height * 0.3f, r.left + r.width * 0.2f, r.top, r.center.x, r.top)
        cubicTo(r.right - r.width * 0.2f, r.top, r.right + r.width * 0.05f, r.top + r.height * 0.3f, r.right - r.width * 0.12f, r.bottom)
        close()
    }
    drawPath(p, steel)
    drawPath(p, Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.25f)), r.left, r.right))
    drawPath(p, Ink, style = Stroke(r.width * 0.04f, join = StrokeJoin.Round))
    // The grille: bars across the visor.
    for (i in 0..4) {
        val x = r.left + r.width * (0.3f + i * 0.1f)
        drawLine(Ink, Offset(x, r.top + r.height * 0.35f), Offset(x, r.top + r.height * 0.8f), r.width * 0.03f, StrokeCap.Round)
    }
    drawLine(Ink, Offset(r.left + r.width * 0.26f, r.top + r.height * 0.35f), Offset(r.right - r.width * 0.26f, r.top + r.height * 0.35f), r.width * 0.035f)
}

private fun DrawScope.crown(r: Rect, gold: Color) {
    val p = Path().apply {
        moveTo(r.left, r.bottom)
        lineTo(r.left, r.top + r.height * 0.3f)
        for (i in 1..4) {
            lineTo(r.left + r.width * (i - 0.5f) / 4, r.top + r.height * 0.62f)
            lineTo(r.left + r.width * i / 4, if (i < 4) r.top else r.top + r.height * 0.3f)
        }
        lineTo(r.right, r.bottom)
        close()
    }
    drawPath(p, gold)
    drawPath(p, Ink, style = Stroke(r.width * 0.03f, join = StrokeJoin.Round))
    for (i in 1..3) drawCircle(Color(0xFFB0302A), r.height * 0.11f, Offset(r.left + r.width * i / 4, r.top + r.height * 0.08f))
    drawCircle(Color(0xFF2E58A6), r.height * 0.12f, Offset(r.center.x, r.top + r.height * 0.72f))
}

/** Cloth flowing from the helm down both sides of the shield, colour outside and metal lining. */
private fun DrawScope.mantling(r: Rect, colour: Color, lining: Color) {
    for (side in listOf(-1f, 1f)) {
        val x0 = r.center.x + side * r.width * 0.12f
        val p = Path().apply {
            moveTo(x0, r.top - r.width * 0.18f)
            cubicTo(r.center.x + side * r.width * 0.9f, r.top - r.width * 0.1f, r.center.x + side * r.width * 0.4f, r.top + r.height * 0.4f, r.center.x + side * r.width * 0.78f, r.top + r.height * 0.72f)
            cubicTo(r.center.x + side * r.width * 0.62f, r.top + r.height * 0.58f, r.center.x + side * r.width * 0.68f, r.top + r.height * 0.3f, r.center.x + side * r.width * 0.5f, r.top + r.height * 0.1f)
            cubicTo(r.center.x + side * r.width * 0.5f, r.top, x0, r.top, x0, r.top)
            close()
        }
        drawPath(p, lining)
        drawPath(p, colour, style = Stroke(r.width * 0.07f, join = StrokeJoin.Round))
        drawPath(p, Ink, style = Stroke(r.width * 0.012f))
    }
}

/** Two laurel branches rising from under the shield up both its sides. */
private fun DrawScope.laurel(r: Rect, W: Float) {
    val green = Color(0xFF4E7B5E)
    val c = Offset(r.center.x, r.top + r.height * 0.5f)
    val rx = r.width * 0.66f
    val ry = r.height * 0.6f
    val leafLen = r.width * 0.075f
    for (side in listOf(-1f, 1f)) {
        // From just under the point, round the side, to level with the top of the shield.
        val stem = Path()
        val n = 13
        for (i in 0..n) {
            val t = i / n.toFloat()
            val a = PI / 2 + side * t * PI * 0.62
            val p = Offset(c.x + (rx * cos(a)).toFloat(), c.y + (ry * sin(a)).toFloat())
            if (i == 0) stem.moveTo(p.x, p.y) else stem.lineTo(p.x, p.y)
            if (i == 0) continue
            // A pair of leaves at each step, pointing along the branch.
            val tangent = (atan2((ry * cos(a)).toFloat() * side, (-rx * sin(a)).toFloat() * side) * 180 / PI).toFloat()
            for (k in listOf(-1f, 1f)) {
                withTransform({ translate(p.x, p.y); rotate(tangent + 90f + k * 38f, Offset.Zero) }) {
                    val leaf = Path().apply {
                        moveTo(0f, 0f); quadraticTo(leafLen * 0.45f, -leafLen * 0.5f, 0f, -leafLen * 1.3f); quadraticTo(-leafLen * 0.45f, -leafLen * 0.5f, 0f, 0f); close()
                    }
                    drawPath(leaf, green)
                    drawPath(leaf, Ink, style = Stroke(1.2f))
                }
            }
        }
        drawPath(stem, Ink, style = Stroke(r.width * 0.012f, cap = StrokeCap.Round))
    }
}
