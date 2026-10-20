package app.party.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.party.R
import app.party.data.Rank
import app.party.domain.GameMaster
import app.party.ui.Go
import app.party.ui.Hall
import app.party.ui.T
import app.party.ui.art.Pin
import app.party.ui.art.Realm
import app.party.ui.art.realm
import app.party.ui.art.vellum
import app.party.ui.components.SectionTitle
import app.party.ui.ctx
import app.party.ui.theme.U
import app.party.ui.theme.UType
import kotlin.math.hypot

/** The party's realm: an island of its own, with a tower wherever a quest was done. */
@Composable
fun MapTab(hall: Hall, go: Go) {
    val c = ctx()
    val realmMap = remember(hall.settings.seed) { Realm(hall.settings.seed) }
    val places = hall.party.places
    val pins = remember(places) { places.map { Pin(it.placeX, it.placeY, it.placeName, it.rankOf == Rank.GOLD) } }
    Column(Modifier.fillMaxSize().vellum().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.map_title), style = UType.Title)
        Text(T.plural(c, R.plurals.map_places, places.size), style = UType.Small)
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1f / Realm.ASPECT).clip(U.RSm).pointerInput(places) {
                detectTapGestures { o ->
                    val x = o.x / size.width; val y = o.y / size.width
                    places.minByOrNull { hypot(it.placeX - x, it.placeY - y) }?.takeIf { hypot(it.placeX - x, it.placeY - y) < 0.06f }?.let { go.page(it.id) }
                }
            },
        ) { realm(realmMap, pins) }
        if (places.isEmpty()) {
            Text(stringResource(R.string.map_empty), style = UType.Italic, modifier = Modifier.padding(top = 14.dp))
        } else {
            SectionTitle(stringResource(R.string.map_list))
            places.reversed().forEach { q ->
                Row(
                    Modifier.fillMaxWidth().clip(U.RSm).clickable { go.page(q.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(GameMaster.roman(q.chapter), style = UType.Strong.copy(color = U.c.vermilion), modifier = Modifier.width(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.placeName, style = UType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(T.quest(c, hall.lore, q).title + " · " + T.shortDate(c, q.endedAt), style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
