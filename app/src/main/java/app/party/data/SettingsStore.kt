package app.party.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.party.domain.Crest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    val nameA: String = "",
    val nameB: String = "",
    /** For Russian forms: how each hero is spoken of. */
    val femaleA: Boolean = false,
    val femaleB: Boolean = true,
    val crest: Crest = Crest.DEFAULT,
    val motto: String = "",
    /** Makes the party's map and its weekly board its own. */
    val seed: Long = 0,
    val foundedAt: Long = 0,
    val haptics: Boolean = true,
    /** The week (since the epoch) the board was last torn down, and how many times. */
    val rerollWeek: Long = -1,
    val rerolls: Int = 0,
) {
    fun name(who: Int, fallbackA: String, fallbackB: String): String = when (who) {
        0 -> nameA.ifBlank { fallbackA }
        else -> nameB.ifBlank { fallbackB }
    }

    fun female(who: Int): Boolean = if (who == 0) femaleA else femaleB
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        nameA = this[NAME_A] ?: "",
        nameB = this[NAME_B] ?: "",
        femaleA = this[FEMALE_A] ?: false,
        femaleB = this[FEMALE_B] ?: true,
        crest = this[CREST]?.let(Crest::decode) ?: Crest.DEFAULT,
        motto = this[MOTTO] ?: "",
        seed = this[SEED] ?: 0L,
        foundedAt = this[FOUNDED] ?: 0L,
        haptics = this[HAPTICS] ?: true,
        rerollWeek = this[REROLL_WEEK] ?: -1L,
        rerolls = this[REROLLS] ?: 0,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[NAME_A] = s.nameA
            it[NAME_B] = s.nameB
            it[FEMALE_A] = s.femaleA
            it[FEMALE_B] = s.femaleB
            it[CREST] = s.crest.encode()
            it[MOTTO] = s.motto
            it[SEED] = s.seed
            it[FOUNDED] = s.foundedAt
            it[HAPTICS] = s.haptics
            it[REROLL_WEEK] = s.rerollWeek
            it[REROLLS] = s.rerolls
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME_A = stringPreferencesKey("name_a")
        val NAME_B = stringPreferencesKey("name_b")
        val FEMALE_A = booleanPreferencesKey("female_a")
        val FEMALE_B = booleanPreferencesKey("female_b")
        val CREST = stringPreferencesKey("crest")
        val MOTTO = stringPreferencesKey("motto")
        val SEED = longPreferencesKey("seed")
        val FOUNDED = longPreferencesKey("founded")
        val HAPTICS = booleanPreferencesKey("haptics")
        val REROLL_WEEK = longPreferencesKey("reroll_week")
        val REROLLS = intPreferencesKey("rerolls")
    }
}
