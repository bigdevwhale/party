package app.party.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.party.R
import app.party.container
import app.party.data.Lore
import app.party.data.Settings
import app.party.domain.CampaignState
import app.party.domain.Deed
import app.party.domain.GameMaster
import app.party.domain.Honours
import app.party.domain.Party
import app.party.ui.arms.ArmsScreen
import app.party.ui.book.BookScreen
import app.party.ui.book.NoticeScreen
import app.party.ui.chronicle.ChronicleTab
import app.party.ui.chronicle.PageScreen
import app.party.ui.dragons.DragonScreen
import app.party.ui.dragons.DragonsScreen
import app.party.ui.map.MapTab
import app.party.ui.onboarding.FoundingScreen
import app.party.ui.quest.QuestScreen
import app.party.ui.raven.LetterScreen
import app.party.ui.settings.SettingsScreen
import app.party.ui.tavern.TavernTab
import app.party.ui.theme.Sans
import app.party.ui.theme.U
import app.party.ui.treasury.TreasuryTab

enum class Tab { TAVERN, CHRONICLE, MAP, TREASURY }

/** Everything stored, plus what the game master makes of it. */
@Immutable
class Hall(val party: Party, val settings: Settings, val lore: Lore, val now: Long) {
    val campaigns: List<CampaignState> by lazy { GameMaster.campaigns(lore, party) }
    val campaignsDone: Int by lazy { campaigns.count { it.complete } }
    val honours: Honours by lazy { Honours.of(party.level, campaignsDone, party.slain.size) }
    val deeds: List<Deed> by lazy { Deed.entries.filter { it.earned(party, campaignsDone) } }
    val week: Long get() = GameMaster.week(now)
    val today: Long get() = GameMaster.epochDay(now)
    val rerolls: Int get() = if (settings.rerollWeek == week) settings.rerolls else 0
    val board by lazy { GameMaster.board(lore, party, settings.seed, week, rerolls) }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun notice(id: String) = nav.navigate("notice/$id") { launchSingleTop = true }
    fun quest(id: Long) = nav.navigate("quest/$id") {
        popUpTo("main") { inclusive = false }
        launchSingleTop = true
    }
    fun book() = nav.navigate("book") { launchSingleTop = true }
    fun arms() = nav.navigate("arms") { launchSingleTop = true }
    fun dragons() = nav.navigate("dragons") { launchSingleTop = true }
    fun dragon(id: Long) = nav.navigate("dragon/$id") { launchSingleTop = true }
    fun letter() = nav.navigate("letter") { launchSingleTop = true }
    fun page(id: Long) = nav.navigate("page/$id") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun home() { nav.popBackStack("main", false) }
    fun back() { if (!nav.popBackStack()) home() }
}

@Composable
fun PartyRoot(settings: Settings) {
    val container = ctx().container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.TAVERN) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()

    val quests by dao.observeQuests().collectAsStateWithLifecycle(initialValue = null)
    val events by dao.observeEvents().collectAsStateWithLifecycle(initialValue = null)
    val dragons by dao.observeDragons().collectAsStateWithLifecycle(initialValue = null)
    val strikes by dao.observeStrikes().collectAsStateWithLifecycle(initialValue = null)
    val letters by dao.observeLetters().collectAsStateWithLifecycle(initialValue = null)
    val errands by dao.observeErrands().collectAsStateWithLifecycle(initialValue = null)
    val hall = remember(quests, events, dragons, strikes, letters, errands, settings) {
        Hall(
            Party(quests.orEmpty(), events.orEmpty(), dragons.orEmpty(), strikes.orEmpty(), letters.orEmpty(), errands.orEmpty()),
            settings, container.lore, System.currentTimeMillis(),
        )
    }
    val go = remember(nav) { Go(nav) }

    // Founded elsewhere (a restored backup, the demo seed): leave the founding pages.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }

    Box(Modifier.fillMaxSize().background(U.c.vellum)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.vellum),
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("welcome") {
                FoundingScreen(settings) { s ->
                    container.write { this.settings.update { s.copy(onboarded = true, seed = System.nanoTime(), foundedAt = System.currentTimeMillis()) } }
                    nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                }
            }
            composable("main") { MainTabs(tab, { tab = it }, go, hall) }
            composable("notice/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                NoticeScreen(e.arguments?.getString("id").orEmpty(), hall, go)
            }
            composable("quest/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                QuestScreen(e.arguments?.getLong("id") ?: 0L, hall, go)
            }
            composable("book") { BookScreen(hall, go) }
            composable("arms") { ArmsScreen(hall, go) }
            composable("dragons") { DragonsScreen(hall, go) }
            composable("dragon/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                DragonScreen(e.arguments?.getLong("id") ?: 0L, hall, go)
            }
            composable("letter") { LetterScreen(hall, go) }
            composable("page/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PageScreen(e.arguments?.getLong("id") ?: 0L, hall, go)
            }
            composable("settings") { SettingsScreen(hall, go) }
        }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, hall: Hall) {
    Column(Modifier.fillMaxSize().background(U.c.vellum)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.TAVERN -> TavernTab(hall, go)
                Tab.CHRONICLE -> ChronicleTab(hall, go)
                Tab.MAP -> MapTab(hall, go)
                Tab.TREASURY -> TreasuryTab(hall, go)
            }
        }
        HorizontalDivider(color = U.c.rule)
        Row(
            Modifier.fillMaxWidth().background(U.c.page).navigationBarsPadding().height(66.dp).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.Castle, stringResource(R.string.tab_tavern), tab == Tab.TAVERN) { onTab(Tab.TAVERN) }
            NavItem(Icons.Rounded.AutoStories, stringResource(R.string.tab_chronicle), tab == Tab.CHRONICLE) { onTab(Tab.CHRONICLE) }
            NavItem(Icons.Rounded.Map, stringResource(R.string.tab_map), tab == Tab.MAP) { onTab(Tab.MAP) }
            NavItem(Icons.Rounded.Diamond, stringResource(R.string.tab_treasury), tab == Tab.TREASURY) { onTab(Tab.TREASURY) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.vermilion else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.vermilionSoft else Color.Transparent).padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Sans, fontSize = 13.sp, color = color, maxLines = 1)
    }
}
