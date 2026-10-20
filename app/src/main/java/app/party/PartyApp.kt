package app.party

import android.app.Application
import android.content.Context
import app.party.data.DemoSeed
import app.party.data.Lore
import app.party.data.PartyDatabase
import app.party.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = PartyDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every line of lore in both languages, read from assets once. */
    val lore: Lore by lazy { Lore.load(context.assets) }

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class PartyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { DemoSeed.seedIfEmpty(container) }
    }
}

val Context.container: AppContainer get() = (applicationContext as PartyApp).container

/** The screenshot build (`-Pdemo -Pshots`): dice and seals hold still so every capture looks the same. */
val SHOTS: Boolean get() = BuildConfig.APPLICATION_ID.endsWith(".shots")
