package app.party.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import app.party.domain.Beast
import app.party.domain.Symbol
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The charges of the party's arms, drawn in a 100-unit box like a herald's pattern book. Beasts face
 * dexter (the viewer's left), as beasts on a shield do. A charge is a [body] in the charge's metal, with
 * [strokes] drawn in the same metal and [marks] (eyes, beaks) in the field's colour.
 */
class Charge(val body: Path, val strokes: List<Pair<Path, Float>> = emptyList(), val marks: Path? = null)

private fun path(block: Path.() -> Unit) = Path().apply(block)

private fun Path.mirrored(): Path {
    val m = Path()
    m.addPath(this)
    m.transform(androidx.compose.ui.graphics.Matrix().apply { scale(-1f, 1f); translate(-100f, 0f) })
    return m
}

private fun Path.circle(x: Float, y: Float, r: Float) = addOval(Rect(Offset(x, y), r))

private fun union(vararg p: Path): Path = p.reduce { a, b -> Path().apply { op(a, b, PathOperation.Union) } }

object Charges {
    fun beast(b: Beast): Charge = when (b) {
        Beast.OWL -> Charge(
            body = union(
                path {
                    moveTo(28f, 8f); lineTo(40f, 22f); quadraticTo(50f, 18f, 60f, 22f); lineTo(72f, 8f)
                    lineTo(73f, 30f); quadraticTo(82f, 46f, 77f, 66f); quadraticTo(71f, 87f, 50f, 91f)
                    quadraticTo(29f, 87f, 23f, 66f); quadraticTo(18f, 46f, 27f, 30f); close()
                },
                path { moveTo(38f, 89f); lineTo(36f, 97f); lineTo(44f, 93f); close(); moveTo(62f, 89f); lineTo(64f, 97f); lineTo(56f, 93f); close() },
            ),
            marks = path {
                circle(40f, 37f, 8.5f); circle(60f, 37f, 8.5f)
                moveTo(46f, 47f); lineTo(54f, 47f); lineTo(50f, 56f); close()
                // Breast feathers.
                for (row in 0..2) for (i in 0..(2 - row % 2)) {
                    val x = 38f + i * 12f + (row % 2) * 6f
                    val y = 64f + row * 8f
                    moveTo(x - 4f, y); quadraticTo(x, y + 5f, x + 4f, y); quadraticTo(x, y + 2f, x - 4f, y)
                }
            },
        )
        Beast.FOX -> Charge(
            // A fox's mask, as heralds draw it: the face alone, full on.
            body = path {
                moveTo(16f, 10f); lineTo(37f, 30f); quadraticTo(50f, 25f, 63f, 30f); lineTo(84f, 10f)
                lineTo(81f, 44f); quadraticTo(92f, 52f, 78f, 61f); lineTo(57f, 85f); quadraticTo(50f, 92f, 43f, 85f)
                lineTo(22f, 61f); quadraticTo(8f, 52f, 19f, 44f); close()
            },
            marks = path {
                moveTo(23f, 19f); lineTo(34f, 33f); lineTo(25f, 37f); close()
                moveTo(77f, 19f); lineTo(66f, 33f); lineTo(75f, 37f); close()
                moveTo(31f, 50f); lineTo(44f, 52f); lineTo(35f, 57f); close()
                moveTo(69f, 50f); lineTo(56f, 52f); lineTo(65f, 57f); close()
                circle(50f, 80f, 4.5f)
            },
        )
        Beast.CAT -> Charge(
            body = path {
                moveTo(22f, 18f); lineTo(23f, 5f); lineTo(32f, 12f); quadraticTo(38f, 10f, 44f, 12f); lineTo(52f, 5f)
                lineTo(52f, 19f); quadraticTo(56f, 28f, 51f, 35f); quadraticTo(68f, 42f, 71f, 66f)
                quadraticTo(73f, 80f, 68f, 88f); lineTo(78f, 88f); quadraticTo(88f, 86f, 87f, 74f)
                quadraticTo(86f, 64f, 92f, 62f); quadraticTo(98f, 70f, 95f, 81f); quadraticTo(90f, 95f, 74f, 95f)
                lineTo(28f, 95f); quadraticTo(23f, 92f, 29f, 88f); lineTo(36f, 88f); quadraticTo(33f, 70f, 38f, 50f)
                quadraticTo(28f, 43f, 23f, 35f); quadraticTo(18f, 27f, 22f, 18f); close()
            },
            marks = path {
                moveTo(26f, 23f); lineTo(34f, 22f); lineTo(29f, 27f); close()
                moveTo(48f, 60f); quadraticTo(52f, 74f, 46f, 86f); lineTo(44f, 86f); quadraticTo(49f, 74f, 46f, 60f); close()
            },
        )
        Beast.STAG -> Charge(
            // A stag's head caboshed: full on, no neck, all antler.
            body = union(
                path {
                    moveTo(39f, 42f); quadraticTo(50f, 37f, 61f, 42f); lineTo(63f, 62f); quadraticTo(59f, 82f, 50f, 92f)
                    quadraticTo(41f, 82f, 37f, 62f); close()
                },
                path { moveTo(39f, 46f); lineTo(20f, 40f); quadraticTo(26f, 50f, 38f, 53f); close() },
                path { moveTo(61f, 46f); lineTo(80f, 40f); quadraticTo(74f, 50f, 62f, 53f); close() },
            ),
            strokes = run {
                val left = path {
                    moveTo(43f, 41f); quadraticTo(31f, 30f, 27f, 6f)
                    moveTo(35f, 29f); lineTo(20f, 24f)
                    moveTo(30f, 18f); lineTo(16f, 12f)
                    moveTo(38f, 35f); lineTo(34f, 18f)
                }
                listOf(left to 5f, left.mirrored() to 5f)
            },
            marks = path {
                moveTo(42f, 57f); lineTo(47f, 59f); lineTo(43f, 62f); close()
                moveTo(58f, 57f); lineTo(53f, 59f); lineTo(57f, 62f); close()
                circle(50f, 86f, 3.5f)
            },
        )
        Beast.BEAR -> Charge(
            body = path {
                moveTo(8f, 50f); quadraticTo(9f, 40f, 20f, 37f); lineTo(23f, 29f); lineTo(29f, 33f)
                quadraticTo(40f, 28f, 56f, 30f); quadraticTo(80f, 28f, 88f, 44f); quadraticTo(94f, 57f, 89f, 70f)
                lineTo(89f, 88f); lineTo(78f, 88f); lineTo(77f, 73f); quadraticTo(69f, 75f, 62f, 73f)
                lineTo(62f, 88f); lineTo(51f, 88f); lineTo(49f, 70f); quadraticTo(40f, 70f, 34f, 66f)
                lineTo(34f, 88f); lineTo(23f, 88f); lineTo(23f, 62f); quadraticTo(13f, 60f, 8f, 50f); close()
            },
            marks = path {
                circle(20f, 44f, 2.6f)
                moveTo(56f, 44f); quadraticTo(64f, 52f, 60f, 62f); lineTo(58f, 62f); quadraticTo(61f, 52f, 54f, 45f); close()
            },
        )
        Beast.RAVEN -> Charge(
            body = path {
                moveTo(6f, 41f); lineTo(23f, 34f); quadraticTo(33f, 23f, 46f, 27f); quadraticTo(61f, 33f, 72f, 43f)
                lineTo(96f, 51f); lineTo(75f, 55f); quadraticTo(66f, 65f, 53f, 66f); lineTo(58f, 84f); lineTo(53f, 84f)
                lineTo(47f, 67f); lineTo(43f, 84f); lineTo(38f, 84f); lineTo(41f, 64f); quadraticTo(28f, 59f, 26f, 47f)
                lineTo(6f, 43f); close()
            },
            marks = path {
                circle(30f, 35f, 2.8f)
                moveTo(42f, 42f); quadraticTo(58f, 40f, 72f, 50f); quadraticTo(58f, 45f, 44f, 46f); close()
            },
        )
        Beast.HARE -> Charge(
            body = path {
                moveTo(30f, 30f); quadraticTo(20f, 4f, 28f, 1f); quadraticTo(36f, 8f, 37f, 25f)
                quadraticTo(41f, 4f, 48f, 5f); quadraticTo(52f, 16f, 41f, 32f); quadraticTo(53f, 36f, 63f, 50f)
                quadraticTo(81f, 54f, 85f, 70f); quadraticTo(87f, 82f, 77f, 87f); lineTo(89f, 92f); lineTo(24f, 92f)
                quadraticTo(19f, 88f, 29f, 86f); quadraticTo(34f, 78f, 41f, 76f); quadraticTo(30f, 70f, 28f, 58f)
                quadraticTo(17f, 56f, 15f, 46f); quadraticTo(17f, 35f, 30f, 30f); close()
            },
            marks = path { circle(25f, 41f, 2.6f) },
        )
        Beast.WOLF -> Charge(
            // A wolf's head, erased at the neck.
            body = path {
                moveTo(8f, 54f); lineTo(30f, 41f); lineTo(35f, 15f); lineTo(46f, 33f); lineTo(53f, 18f); lineTo(58f, 38f)
                quadraticTo(79f, 44f, 85f, 64f); lineTo(91f, 90f); lineTo(82f, 84f); lineTo(76f, 92f); lineTo(68f, 85f)
                lineTo(60f, 93f); lineTo(53f, 85f); quadraticTo(46f, 72f, 36f, 67f); lineTo(14f, 64f)
                quadraticTo(6f, 60f, 8f, 54f); close()
            },
            marks = path {
                moveTo(33f, 48f); lineTo(43f, 46f); lineTo(37f, 51f); close()
                moveTo(14f, 60f); lineTo(30f, 60f); lineTo(16f, 62f); close()
            },
        )
    }

    fun symbol(s: Symbol): Charge = when (s) {
        Symbol.STAR -> Charge(star(50f, 53f, 46f, 19f, 5))
        Symbol.CRESCENT -> Charge(
            Path().apply {
                op(path { circle(50f, 50f, 40f) }, path { circle(50f, 34f, 33f) }, PathOperation.Difference)
            }
        )
        Symbol.HEART -> Charge(path {
            moveTo(50f, 88f); cubicTo(14f, 62f, 6f, 40f, 18f, 24f); cubicTo(30f, 10f, 46f, 18f, 50f, 30f)
            cubicTo(54f, 18f, 70f, 10f, 82f, 24f); cubicTo(94f, 40f, 86f, 62f, 50f, 88f); close()
        })
        Symbol.KEY -> Charge(
            body = path {
                addRect(Rect(46f, 30f, 54f, 92f))
                addRect(Rect(54f, 74f, 68f, 80f)); addRect(Rect(54f, 84f, 64f, 90f))
            },
            strokes = listOf(path { circle(50f, 21f, 13f) } to 8f),
        )
        Symbol.ROSE -> Charge(
            body = union(*Array(5) { i ->
                val a = -PI / 2 + i * 2 * PI / 5
                path { circle(50f + 22f * cos(a).toFloat(), 52f + 22f * sin(a).toFloat(), 19f) }
            }, path { circle(50f, 52f, 22f) }),
            marks = path { circle(50f, 52f, 9f) },
        )
        Symbol.SUN -> Charge(
            body = union(path { circle(50f, 50f, 24f) }, star(50f, 50f, 46f, 26f, 12)),
            marks = path {
                circle(42f, 46f, 2.5f); circle(58f, 46f, 2.5f)
                moveTo(42f, 58f); quadraticTo(50f, 64f, 58f, 58f); quadraticTo(50f, 61f, 42f, 58f)
            },
        )
        Symbol.LEAF -> Charge(
            body = path {
                moveTo(50f, 6f); cubicTo(80f, 26f, 80f, 66f, 50f, 86f); cubicTo(20f, 66f, 20f, 26f, 50f, 6f); close()
                addRect(Rect(47.5f, 80f, 52.5f, 96f))
            },
            marks = path { moveTo(49f, 16f); lineTo(51f, 16f); lineTo(51.5f, 82f); lineTo(48.5f, 82f); close() },
        )
        Symbol.BELL -> Charge(
            body = path {
                moveTo(50f, 10f); cubicTo(70f, 12f, 72f, 30f, 72f, 50f); quadraticTo(73f, 68f, 86f, 78f); lineTo(14f, 78f)
                quadraticTo(27f, 68f, 28f, 50f); cubicTo(28f, 30f, 30f, 12f, 50f, 10f); close()
                circle(50f, 84f, 7f)
                circle(50f, 7f, 5f)
            },
        )
    }

    fun star(cx: Float, cy: Float, outer: Float, inner: Float, points: Int): Path = Path().apply {
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val a = -PI / 2 + i * PI / points
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** Paints a charge into [box] in [metal], marks in [field], outlined in [ink]. [flip] turns it to face sinister. */
fun DrawScope.charge(c: Charge, box: Rect, metal: Color, field: Color, ink: Color, flip: Boolean = false) {
    val s = box.width / 100f
    translate(box.left, box.top) {
        scale(if (flip) -s else s, s, pivot = Offset.Zero) {
            translate(if (flip) -100f else 0f, 0f) {
                val line = 2.2f
                c.strokes.forEach { (p, w) -> drawPath(p, ink, style = Stroke(w + line * 2, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                drawPath(c.body, ink, style = Stroke(line * 2, join = StrokeJoin.Round))
                c.strokes.forEach { (p, w) -> drawPath(p, metal, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                drawPath(c.body, metal)
                c.marks?.let { drawPath(it, field) }
            }
        }
    }
}
