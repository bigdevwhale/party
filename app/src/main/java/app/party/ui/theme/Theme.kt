package app.party.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.party.R
import app.party.domain.Tincture

/**
 * An illuminated manuscript: vellum, iron-gall ink, vermilion rubrics, lapis blue, verdigris and
 * gold leaf. Light all the way through, like a page by a window.
 */
@Immutable
data class Palette(
    val vellum: Color,
    val vellumDeep: Color,
    val page: Color,
    val ink: Color,
    val inkSoft: Color,
    val faint: Color,
    val rule: Color,
    val vermilion: Color,
    val vermilionSoft: Color,
    val lapis: Color,
    val lapisSoft: Color,
    val verdigris: Color,
    val gold: Color,
    val goldLight: Color,
    val goldDark: Color,
    val wax: Color,
    val leather: Color,
    val night: Color,
)

val BookPalette = Palette(
    vellum = Color(0xFFF1E6CC),
    vellumDeep = Color(0xFFE4D3AE),
    page = Color(0xFFF7EEDB),
    ink = Color(0xFF2B2018),
    inkSoft = Color(0xFF5E4B3A),
    faint = Color(0xFF98846A),
    rule = Color(0xFFD5C29C),
    vermilion = Color(0xFFA8322B),
    vermilionSoft = Color(0x22A8322B),
    lapis = Color(0xFF2C4C8C),
    lapisSoft = Color(0x1F2C4C8C),
    verdigris = Color(0xFF4E7B5E),
    gold = Color(0xFFC99A2E),
    goldLight = Color(0xFFF0D27A),
    goldDark = Color(0xFF8A6418),
    wax = Color(0xFF8E1F22),
    leather = Color(0xFF5A3221),
    night = Color(0xFF1B1A2E),
)

val LocalPalette = staticCompositionLocalOf { BookPalette }

/** Heraldic colours as painted in a roll of arms. */
fun tincture(t: Tincture): Color = when (t) {
    Tincture.OR -> Color(0xFFD6A93A)
    Tincture.ARGENT -> Color(0xFFF4F0E6)
    Tincture.GULES -> Color(0xFFB0302A)
    Tincture.AZURE -> Color(0xFF2E58A6)
    Tincture.VERT -> Color(0xFF2F7A4C)
    Tincture.PURPURE -> Color(0xFF6E3A78)
    Tincture.SABLE -> Color(0xFF262323)
}

/** Ruslan Display: the rubrics, like letters cut on an old charter. */
val DisplayFont = FontFamily(Font(R.font.ruslan, FontWeight.Normal))

/** EB Garamond: what the chronicle and the game master say. */
val Book = FontFamily(
    Font(R.font.garamond_regular, FontWeight.Normal),
    Font(R.font.garamond_medium, FontWeight.Medium),
    Font(R.font.garamond_semibold, FontWeight.SemiBold),
    Font(R.font.garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Cormorant for the big initials and the motto. */
val Cormorant = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Alegreya Sans for buttons and labels. */
val Sans = FontFamily(
    Font(R.font.alegreya_sans, FontWeight.Normal),
    Font(R.font.alegreya_sans_medium, FontWeight.Medium),
    Font(R.font.alegreya_sans_bold, FontWeight.Bold),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RLg = RoundedCornerShape(16.dp)
    val RMd = RoundedCornerShape(10.dp)
    val RSm = RoundedCornerShape(6.dp)
}

object UType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = DisplayFont, fontSize = 32.sp, lineHeight = 40.sp, color = U.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = DisplayFont, fontSize = 25.sp, lineHeight = 32.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = DisplayFont, fontSize = 19.sp, lineHeight = 25.sp, color = U.c.ink)
    /** A rubric: a small red heading. */
    val Rubric: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.16.em, color = U.c.vermilion)
    val Story: TextStyle @Composable get() = TextStyle(fontFamily = Book, fontSize = 20.sp, lineHeight = 28.sp, color = U.c.ink)
    val Prose: TextStyle @Composable get() = TextStyle(fontFamily = Book, fontSize = 18.sp, lineHeight = 26.sp, color = U.c.ink)
    val Italic: TextStyle @Composable get() = TextStyle(fontFamily = Book, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 24.sp, color = U.c.inkSoft)
    val Motto: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 19.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Book, fontSize = 17.sp, lineHeight = 24.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.5.sp, lineHeight = 20.sp, color = U.c.inkSoft)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 17.sp, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = U.c.ink)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
}

@Composable
fun PartyTheme(content: @Composable () -> Unit) {
    val p = BookPalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.vermilion,
                onPrimary = p.page,
                primaryContainer = p.vermilionSoft,
                onPrimaryContainer = p.ink,
                secondary = p.lapis,
                background = p.vellum,
                surface = p.page,
                surfaceContainer = p.page,
                surfaceContainerHigh = p.page,
                surfaceContainerHighest = p.vellumDeep,
                surfaceContainerLow = p.page,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.inkSoft,
                outline = p.rule,
                outlineVariant = p.rule,
                error = p.wax,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 16.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
