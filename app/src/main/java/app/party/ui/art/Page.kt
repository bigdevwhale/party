package app.party.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.party.domain.Crest
import app.party.ui.theme.BookPalette
import app.party.ui.theme.DisplayFont
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Vellum: a warm page with flecks in the skin and edges browned by hands. */
fun Modifier.vellum(base: Color = BookPalette.vellum, seed: Int = 7): Modifier = drawWithCache {
    val rnd = Random(seed)
    val flecks = List(((size.width * size.height) / 2600f).toInt().coerceAtMost(1400)) {
        Triple(Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height), 0.6f + rnd.nextFloat() * 1.4f, rnd.nextFloat())
    }
    val edge = Brush.radialGradient(
        listOf(Color.Transparent, BookPalette.goldDark.copy(alpha = 0.16f)),
        center = Offset(size.width / 2, size.height / 2),
        radius = maxOf(size.width, size.height) * 0.75f,
    )
    onDrawBehind {
        drawRect(base)
        flecks.forEach { (o, r, a) -> drawCircle(BookPalette.leather.copy(alpha = 0.035f + a * 0.05f), r, o) }
        drawRect(edge)
    }
}

/**
 * An illuminated initial: the first letter in vermilion on a gold-leaf square, with a curl of vine.
 * [colour] alternates with lapis in the chronicle.
 */
@Composable
fun DropCap(letter: Char, size: Dp = 64.dp, colour: Color = BookPalette.vermilion, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) { initialFrame(colour) }
        Text(
            letter.uppercase(),
            style = TextStyle(fontFamily = DisplayFont, fontSize = (size.value * 0.56f).sp, color = colour),
        )
    }
}

private fun DrawScope.initialFrame(colour: Color) {
    val s = size.minDimension
    val inset = s * 0.06f
    val r = Rect(Offset(inset, inset), Size(s - inset * 2, s - inset * 2))
    drawRect(Brush.linearGradient(listOf(BookPalette.goldLight, BookPalette.gold, BookPalette.goldDark, BookPalette.gold), r.topLeft, r.bottomRight), r.topLeft, r.size)
    drawRect(BookPalette.ink, r.topLeft, r.size, style = Stroke(s * 0.025f))
    val inner = Rect(r.left + s * 0.07f, r.top + s * 0.07f, r.right - s * 0.07f, r.bottom - s * 0.07f)
    drawRect(BookPalette.page, inner.topLeft, inner.size)
    // Vine tendrils in the corners of the inner ground.
    val vine = colour.copy(alpha = 0.32f)
    for ((cx, cy, dir) in listOf(Triple(inner.left, inner.top, 0f), Triple(inner.right, inner.bottom, 180f))) {
        val p = Path()
        for (i in 0..18) {
            val t = i / 18f
            val a = (dir + t * 140f) * PI / 180
            val rad = s * (0.05f + t * 0.17f)
            val x = cx + (rad * cos(a)).toFloat() * (if (dir == 0f) 1f else 1f)
            val y = cy + (rad * sin(a)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, vine, style = Stroke(s * 0.022f, cap = StrokeCap.Round))
        drawCircle(vine, s * 0.03f, Offset(cx + if (dir == 0f) s * 0.14f else -s * 0.14f, cy + if (dir == 0f) s * 0.05f else -s * 0.05f))
    }
    // Gold dots at the outer corners.
    listOf(r.topLeft, Offset(r.right, r.top), Offset(r.left, r.bottom), r.bottomRight).forEach {
        drawCircle(BookPalette.gold, s * 0.045f, it); drawCircle(BookPalette.ink, s * 0.045f, it, style = Stroke(s * 0.015f))
    }
}

/**
 * A wax seal with the party's shield pressed in it. [cracks] 0 is whole; each crack splits it further.
 */
@Composable
fun WaxSeal(crest: Crest, cracks: Int, modifier: Modifier = Modifier, glow: Float = 0f) {
    Canvas(modifier) { seal(crest, cracks, glow) }
}

fun DrawScope.seal(crest: Crest, cracks: Int, glow: Float = 0f) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    val rnd = Random(11)
    if (glow > 0f) drawCircle(Brush.radialGradient(listOf(BookPalette.goldLight.copy(alpha = 0.55f * glow), Color.Transparent), c, s * 0.62f), s * 0.62f, c)
    // The blob of wax: a circle with a wavering edge.
    val blob = Path()
    val n = 40
    for (i in 0..n) {
        val a = i * 2 * PI / n
        val rr = s * (0.43f + (if (i % n == 0) 0f else rnd.nextFloat() * 0.035f))
        val x = c.x + (rr * cos(a)).toFloat(); val y = c.y + (rr * sin(a)).toFloat()
        if (i == 0) blob.moveTo(x, y) else blob.lineTo(x, y)
    }
    blob.close()
    drawPath(blob, Color.Black.copy(alpha = 0.18f), style = Stroke(s * 0.03f))
    drawPath(blob, Brush.radialGradient(listOf(Color(0xFFC0393A), BookPalette.wax, Color(0xFF5C1012)), c + Offset(-s * 0.1f, -s * 0.12f), s * 0.55f))
    // The pressed ring and the shield in it.
    drawCircle(Color.Black.copy(alpha = 0.22f), s * 0.33f, c, style = Stroke(s * 0.025f))
    drawCircle(Color.White.copy(alpha = 0.12f), s * 0.33f, c + Offset(s * 0.008f, s * 0.008f), style = Stroke(s * 0.012f))
    val sw = s * 0.34f
    val r = Rect(Offset(c.x - sw / 2, c.y - sw * 0.6f), Size(sw, sw * 1.17f))
    val p = shieldPath(r)
    drawPath(p, Color.Black.copy(alpha = 0.18f))
    drawPath(p, Color.White.copy(alpha = 0.10f), style = Stroke(s * 0.012f))
    val beast = Charges.beast(crest.beast.first)
    val bs = sw * 0.62f
    translate(r.center.x - bs / 2, r.top + sw * 0.28f) {
        scale(bs / 100f, bs / 100f, Offset.Zero) {
            drawPath(beast.body, Color.Black.copy(alpha = 0.25f))
        }
    }
    // Cracks run from the edge toward the middle.
    val cr = Random(cracks * 31 + 5)
    repeat(cracks.coerceAtMost(4)) { k ->
        val a0 = (k * 1.7 + cr.nextDouble() * 0.6) * PI
        val crack = Path()
        var x = c.x + (s * 0.45f * cos(a0)).toFloat(); var y = c.y + (s * 0.45f * sin(a0)).toFloat()
        crack.moveTo(x, y)
        repeat(5) {
            x += (c.x - x) * 0.28f + (cr.nextFloat() - 0.5f) * s * 0.08f
            y += (c.y - y) * 0.28f + (cr.nextFloat() - 0.5f) * s * 0.08f
            crack.lineTo(x, y)
        }
        drawPath(crack, Color(0xFF2A0606), style = Stroke(s * 0.022f, cap = StrokeCap.Round))
        drawPath(crack, Color.White.copy(alpha = 0.18f), style = Stroke(s * 0.007f, cap = StrokeCap.Round))
    }
}

/** The d20 seen face on: a hexagon outline with the facets inside, the number on the front face. */
fun DrawScope.d20(face: Color, edge: Color) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    val r = s * 0.47f
    val hex = List(6) { i -> val a = -PI / 2 + i * PI / 3; Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat()) }
    val tri = List(3) { i -> val a = PI / 2 + i * 2 * PI / 3 + PI; Offset(c.x + (r * 0.58f * cos(a)).toFloat(), c.y + (r * 0.58f * sin(a)).toFloat() + r * 0.06f) }
    val outline = Path().apply { hex.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
    drawPath(outline, face)
    drawPath(outline, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.3f)), hex[5], hex[2]))
    val front = Path().apply { moveTo(tri[0].x, tri[0].y); lineTo(tri[1].x, tri[1].y); lineTo(tri[2].x, tri[2].y); close() }
    drawPath(front, Color.White.copy(alpha = 0.16f))
    val w = s * 0.018f
    drawPath(outline, edge, style = Stroke(w * 1.4f))
    drawPath(front, edge, style = Stroke(w))
    // Facet lines from the front triangle to the hexagon.
    val links = listOf(0 to listOf(0, 1, 5), 1 to listOf(3, 4, 5), 2 to listOf(1, 2, 3))
    links.forEach { (t, hs) -> hs.forEach { h -> drawLine(edge, tri[t], hex[h], w) } }
}

/** A wyvern rampant, the shape of a dragon the party has to fight; [hue] from its seed. */
fun dragonPath(): Path = Path().apply {
    // Body, neck and head, facing dexter.
    moveTo(30f, 92f); quadraticTo(26f, 78f, 36f, 66f); quadraticTo(30f, 56f, 32f, 44f)
    quadraticTo(26f, 36f, 22f, 26f); lineTo(10f, 24f); lineTo(16f, 20f); lineTo(8f, 14f); lineTo(22f, 14f)
    lineTo(24f, 6f); lineTo(30f, 14f); quadraticTo(40f, 20f, 42f, 34f); quadraticTo(46f, 44f, 50f, 50f)
    // The wing, raised behind.
    lineTo(56f, 30f); lineTo(62f, 8f); lineTo(70f, 22f); lineTo(80f, 10f); lineTo(84f, 28f); lineTo(95f, 24f)
    lineTo(90f, 42f); quadraticTo(76f, 46f, 62f, 56f)
    // Haunch, leg and the tail curling round.
    quadraticTo(70f, 66f, 66f, 78f); lineTo(74f, 90f); lineTo(64f, 90f); lineTo(58f, 82f)
    quadraticTo(62f, 92f, 80f, 94f); quadraticTo(96f, 92f, 92f, 80f); lineTo(97f, 82f); quadraticTo(100f, 98f, 80f, 99f)
    quadraticTo(56f, 100f, 50f, 88f); quadraticTo(44f, 96f, 30f, 92f); close()
}

fun DrawScope.dragon(body: Color, slain: Boolean) {
    val s = size.minDimension
    translate((size.width - s) / 2, (size.height - s) / 2) {
        scale(s / 100f, s / 100f, Offset.Zero) {
            val p = dragonPath()
            drawPath(p, if (slain) body.copy(alpha = 0.35f) else body)
            drawPath(p, BookPalette.ink, style = Stroke(1.8f))
            drawCircle(BookPalette.goldLight, 1.8f, Offset(22f, 18f))
            if (slain) {
                drawLine(BookPalette.ink, Offset(8f, 96f), Offset(96f, 8f), 3f, StrokeCap.Round)
            }
        }
    }
}

/** A dragon's colour from its seed: crimson, green, ash, lapis, bronze or violet. */
fun dragonColour(seed: Int): Color = listOf(
    Color(0xFF9E2B25), Color(0xFF3F6E4A), Color(0xFF55524F), Color(0xFF2C4C8C), Color(0xFF8A6418), Color(0xFF5E3570),
)[Math.floorMod(seed, 6)]
