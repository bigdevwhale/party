package app.party.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.party.ui.theme.BookPalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The party's realm: an island drawn once from the party's seed, as on an old portolan. Coordinates are
 * 0..1 across the width and 0..[ASPECT] down.
 */
class Realm(seed: Long) {
    private val rnd = Random(seed)

    /** Land: the main island as a radius per angle, plus a few islets. */
    private val harmonics = List(6) { k -> Triple(k + 2, 0.018f + rnd.nextFloat() * 0.05f / (1 + k * 0.3f), rnd.nextFloat() * 2 * PI.toFloat()) }
    val centre = Offset(0.5f, 0.66f)
    private val base = 0.36f

    fun radius(a: Float): Float = base + harmonics.sumOf { (k, amp, ph) -> (amp * sin(k * a + ph)).toDouble() }.toFloat()

    /** Is the point on the main island? */
    fun land(x: Float, y: Float): Boolean {
        val dx = x - centre.x; val dy = (y - centre.y) / STRETCH
        return hypot(dx, dy) < radius(atan2(dy, dx)) * 0.97f
    }

    val islets: List<Triple<Offset, Float, Long>> = List(3) {
        val a = rnd.nextFloat() * 2 * PI.toFloat()
        val d = radius(a) + 0.07f + rnd.nextFloat() * 0.04f
        Triple(Offset(centre.x + d * cos(a), centre.y + d * sin(a) * STRETCH), 0.018f + rnd.nextFloat() * 0.018f, rnd.nextLong())
    }.filter { (o, r, _) -> o.x in r..(1 - r) && o.y in (r + 0.05f)..(ASPECT - r - 0.05f) }

    private fun inside(): Offset {
        repeat(200) {
            val x = 0.15f + rnd.nextFloat() * 0.7f
            val y = 0.2f + rnd.nextFloat() * (ASPECT - 0.4f)
            if (land(x, y) && hypot(x - centre.x, y - centre.y) < 0.32f) return Offset(x, y)
        }
        return centre
    }

    /** A range of mountains along an arc. */
    val mountains: List<Offset> = run {
        val start = inside()
        val dir = rnd.nextFloat() * 2 * PI.toFloat()
        List(9) { i ->
            val t = i * 0.045f
            Offset(start.x + t * cos(dir) + (rnd.nextFloat() - 0.5f) * 0.03f, start.y + t * sin(dir) * STRETCH + (rnd.nextFloat() - 0.5f) * 0.03f)
        }.filter { land(it.x, it.y) }
    }

    /** Woods: clumps of little trees. */
    val trees: List<Offset> = List(4) { inside() }.flatMap { c ->
        List(11) { Offset(c.x + (rnd.nextFloat() - 0.5f) * 0.12f, c.y + (rnd.nextFloat() - 0.5f) * 0.1f) }
    }.filter { land(it.x, it.y) && mountains.none { m -> hypot(m.x - it.x, m.y - it.y) < 0.04f } }

    /** A river from the mountains to the sea. */
    val river: List<Offset> = run {
        val from = mountains.getOrNull(mountains.size / 2) ?: centre
        val a = atan2(from.y - centre.y, from.x - centre.x) + (rnd.nextFloat() - 0.5f)
        val pts = mutableListOf(from)
        var p = from
        var heading = a
        while (land(p.x, p.y) && pts.size < 40) {
            heading += (rnd.nextFloat() - 0.5f) * 0.7f
            p = Offset(p.x + 0.025f * cos(heading), p.y + 0.025f * sin(heading))
            pts += p
        }
        pts
    }

    /** The coastline, sampled. */
    fun coast(n: Int = 160): List<Offset> = List(n) { i ->
        val a = i * 2 * PI.toFloat() / n
        val r = radius(a)
        Offset(centre.x + r * cos(a), centre.y + r * sin(a) * STRETCH)
    }

    companion object {
        /** The map is taller than wide. */
        const val ASPECT = 1.3f

        /** The island is drawn a little taller than round. */
        const val STRETCH = 1.12f
    }
}

/** A place on the map: where a quest happened. */
data class Pin(val x: Float, val y: Float, val label: String, val gold: Boolean)

fun DrawScope.realm(realm: Realm, pins: List<Pin>, pending: Offset? = null) {
    val w = size.width
    fun at(o: Offset) = Offset(o.x * w, o.y * w)
    val ink = BookPalette.ink
    // The sea, with lines of swell following the coast.
    drawRect(Color(0xFFE6DCC0))
    val coast = realm.coast()
    fun ring(scale: Float): Path = Path().apply {
        coast.forEachIndexed { i, o ->
            val p = at(Offset(realm.centre.x + (o.x - realm.centre.x) * scale, realm.centre.y + (o.y - realm.centre.y) * scale))
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    for (k in 1..4) drawPath(ring(1f + k * 0.045f), BookPalette.lapis.copy(alpha = 0.16f - k * 0.03f), style = Stroke(w * 0.003f))
    realm.islets.forEach { (o, r, _) ->
        for (k in 1..2) drawCircle(BookPalette.lapis.copy(alpha = 0.12f), (r + k * 0.012f) * w, at(o), style = Stroke(w * 0.003f))
    }
    // Land.
    val land = ring(1f)
    drawPath(land, Color(0xFFF3EAD2))
    drawPath(land, ink, style = Stroke(w * 0.005f, join = StrokeJoin.Round))
    realm.islets.forEach { (o, r, _) ->
        drawCircle(Color(0xFFF3EAD2), r * w, at(o)); drawCircle(ink, r * w, at(o), style = Stroke(w * 0.004f))
    }
    // River.
    if (realm.river.size > 1) {
        val p = Path().apply { realm.river.forEachIndexed { i, o -> val q = at(o); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
        drawPath(p, BookPalette.lapis.copy(alpha = 0.7f), style = Stroke(w * 0.006f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    // Trees: little lollipops with a shadow side.
    realm.trees.forEach { o ->
        val p = at(o)
        val r = w * 0.011f
        drawLine(ink, p, p + Offset(0f, r * 1.6f), w * 0.0035f)
        drawCircle(BookPalette.verdigris.copy(alpha = 0.75f), r, p)
        drawCircle(ink, r, p, style = Stroke(w * 0.003f))
    }
    // Mountains: hatched peaks, light from the left.
    realm.mountains.sortedBy { it.y }.forEach { o ->
        val p = at(o)
        val h = w * 0.045f; val b = w * 0.035f
        val peak = Path().apply { moveTo(p.x - b, p.y + h * 0.4f); lineTo(p.x, p.y - h * 0.6f); lineTo(p.x + b, p.y + h * 0.4f); close() }
        drawPath(peak, Color(0xFFF3EAD2))
        val shade = Path().apply { moveTo(p.x, p.y - h * 0.6f); lineTo(p.x + b, p.y + h * 0.4f); lineTo(p.x + b * 0.15f, p.y + h * 0.4f); close() }
        drawPath(shade, BookPalette.leather.copy(alpha = 0.35f))
        drawPath(peak, ink, style = Stroke(w * 0.004f, join = StrokeJoin.Round))
    }
    // The compass rose.
    val rc = Offset(w * 0.86f, w * 0.14f)
    val rr = w * 0.07f
    for (i in 0 until 8) {
        val a = -PI / 2 + i * PI / 4
        val len = if (i % 2 == 0) rr else rr * 0.55f
        val tip = rc + Offset((len * cos(a)).toFloat(), (len * sin(a)).toFloat())
        val side = PI / 2
        val l = rc + Offset((rr * 0.13f * cos(a + side)).toFloat(), (rr * 0.13f * sin(a + side)).toFloat())
        val r = rc + Offset((rr * 0.13f * cos(a - side)).toFloat(), (rr * 0.13f * sin(a - side)).toFloat())
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(rc.x, rc.y); close() }, if (i == 0) BookPalette.vermilion else ink)
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(r.x, r.y); lineTo(rc.x, rc.y); close() }, Color(0xFFF3EAD2))
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(rc.x, rc.y); lineTo(r.x, r.y); close() }, ink, style = Stroke(w * 0.002f))
    }
    drawCircle(ink, rr * 0.75f, rc, style = Stroke(w * 0.002f))
    // The party's road between its places, in the order they were found.
    if (pins.size > 1) {
        val road = Path().apply { pins.forEachIndexed { i, p -> val q = at(Offset(p.x, p.y)); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
        drawPath(road, BookPalette.vermilion.copy(alpha = 0.8f), style = Stroke(w * 0.004f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.012f, w * 0.01f))))
    }
    pins.forEach { pin -> marker(at(Offset(pin.x, pin.y)), w, if (pin.gold) BookPalette.gold else BookPalette.vermilion) }
    pending?.let { marker(at(it), w, BookPalette.lapis) }
    // A border, double ruled.
    drawRect(ink, Offset(w * 0.012f, w * 0.012f), Size(w * 0.976f, size.height - w * 0.024f), style = Stroke(w * 0.004f))
    drawRect(ink, Offset(w * 0.024f, w * 0.024f), Size(w * 0.952f, size.height - w * 0.048f), style = Stroke(w * 0.0015f))
}

/** A little tower with a pennant. */
private fun DrawScope.marker(p: Offset, w: Float, flag: Color) {
    val s = w * 0.03f
    val ink = BookPalette.ink
    drawRect(Color(0xFFF3EAD2), Offset(p.x - s * 0.4f, p.y - s), Size(s * 0.8f, s))
    drawRect(ink, Offset(p.x - s * 0.4f, p.y - s), Size(s * 0.8f, s), style = Stroke(w * 0.003f))
    // Crenels.
    for (i in 0..2) drawRect(ink, Offset(p.x - s * 0.4f + i * s * 0.3f, p.y - s - s * 0.18f), Size(s * 0.2f, s * 0.18f))
    drawLine(ink, Offset(p.x, p.y - s * 1.18f), Offset(p.x, p.y - s * 1.9f), w * 0.003f)
    drawPath(Path().apply { moveTo(p.x, p.y - s * 1.9f); lineTo(p.x + s * 0.75f, p.y - s * 1.7f); lineTo(p.x, p.y - s * 1.5f); close() }, flag)
    drawCircle(ink.copy(alpha = 0.25f), s * 0.5f, Offset(p.x, p.y + s * 0.08f), style = Stroke(w * 0.002f))
}
