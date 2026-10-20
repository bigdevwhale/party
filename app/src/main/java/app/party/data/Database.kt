package app.party.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** How a hero's secret objective went, as owned up at the end. */
object Done {
    const val UNTOLD = -1
    const val NO = 0
    const val PARTLY = 1
    const val YES = 2
}

/**
 * One quest the party went on: a date. [source] is the lore id of the quest or chapter, or empty for one
 * written by hand ([customTitle], [customTasks] one per line). Hero 0 is A, hero 1 is B.
 *
 * [guessedA] is whether A guessed B's secret (-1 not yet, 0 missed, 1 right), and the other way round.
 * The chronicle number [chapter] and [xp] are set when the party comes home ([endedAt] > 0).
 */
@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val customTitle: String = "",
    val customTasks: String = "",
    /** [School] key. */
    val school: String,
    /** [Rank] code. */
    val rank: String,
    val startedAt: Long,
    val endedAt: Long = 0,
    /** Bit i: task i is done. */
    val tasksDone: Int = 0,
    val secretA: String = "",
    val secretB: String = "",
    val doneA: Int = Done.UNTOLD,
    val doneB: Int = Done.UNTOLD,
    val guessedA: Int = -1,
    val guessedB: Int = -1,
    /** Minutes the phones lay face down under the spell of silence, and how often it was broken. */
    val sealMinutes: Int = 0,
    val cracks: Int = 0,
    val lineA: String = "",
    val lineB: String = "",
    val trophyName: String = "",
    val trophyLore: String = "",
    /** File name in filesDir/trophies. */
    val trophyPhoto: String = "",
    /** Where on the party's map it happened, 0..1; negative when not placed. */
    val placeX: Float = -1f,
    val placeY: Float = -1f,
    val placeName: String = "",
    val chapter: Int = 0,
    val xp: Int = 0,
    /** Where in the quest the party is: see [Stage]. */
    val stage: Int = 0,
) {
    val done: Boolean get() = endedAt > 0
    val custom: Boolean get() = source.isEmpty()
    val schoolOf: School get() = School.of(school) ?: School.ROAD
    val rankOf: Rank get() = rank.singleOrNull()?.let(Rank::of) ?: Rank.COPPER
    val placed: Boolean get() = placeX >= 0f && placeY >= 0f
    fun taskDone(i: Int): Boolean = tasksDone and (1 shl i) != 0
    val tasksCount: Int get() = Integer.bitCount(tasksDone)
    val campaign: String get() = if (source.startsWith("c") && '.' in source && source[1].isDigit()) source.substringBefore('.') else ""
}

/** The steps of a quest, in order; a quest is picked up again at its stage. */
object Stage {
    const val OATH = 0
    const val LETTERS = 1
    const val SECRET_A = 2
    const val SECRET_B = 3
    const val JOURNEY = 4
    const val GUESS_A = 5
    const val GUESS_B = 6
    const val LINES = 7
    const val TROPHY = 8
    const val PLACE = 9
}

/** What can happen on the road, logged for the chronicle's margins. */
enum class EventKind { ENCOUNTER, CAMPFIRE, ROLL, SEAL, CRACK, LETTER }

@Entity(tableName = "events", indices = [Index("questId")])
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: Long,
    val at: Long,
    /** [EventKind] name. */
    val kind: String,
    /** A lore id: the encounter, the question, the fate. */
    val ref: String = "",
    /** The number rolled, the minutes under the seal. */
    val value: Int = 0,
    /** Who rolled: 0 or 1, -1 for both. */
    val who: Int = -1,
) {
    val kindOf: EventKind? get() = runCatching { EventKind.valueOf(kind) }.getOrNull()
}

/**
 * The party's dragon: a shared thing put off for too long. It has [hp] hit points; every step taken is
 * a strike, and when the strikes reach [hp] it's slain ([slainAt]).
 */
@Entity(tableName = "dragons")
data class Dragon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val deed: String,
    val hp: Int,
    /** Colours and shape. */
    val seed: Int,
    val createdAt: Long,
    val slainAt: Long = 0,
) {
    val slain: Boolean get() = slainAt > 0
}

@Entity(tableName = "strikes", indices = [Index("dragonId")])
data class Strike(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dragonId: Long,
    val at: Long,
    val note: String = "",
)

/** A sealed letter from one hero to the other; the raven brings it at the start of the next quest. */
@Entity(tableName = "letters")
data class Letter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 0 or 1: who wrote it. */
    val from: Int,
    val text: String,
    val createdAt: Long,
    val openedAt: Long = 0,
    val questId: Long = 0,
)

/** The raven's errand on a day, kept once done. */
@Entity(tableName = "errands")
data class Errand(
    /** Epoch day. */
    @PrimaryKey val day: Long,
    val ravenId: String,
    val doneAt: Long,
)

@Dao
abstract class PartyDao {
    @Query("SELECT * FROM quests ORDER BY startedAt DESC") abstract fun observeQuests(): Flow<List<Quest>>
    @Query("SELECT * FROM events ORDER BY at ASC") abstract fun observeEvents(): Flow<List<Event>>
    @Query("SELECT * FROM dragons ORDER BY createdAt ASC") abstract fun observeDragons(): Flow<List<Dragon>>
    @Query("SELECT * FROM strikes ORDER BY at ASC") abstract fun observeStrikes(): Flow<List<Strike>>
    @Query("SELECT * FROM letters ORDER BY createdAt ASC") abstract fun observeLetters(): Flow<List<Letter>>
    @Query("SELECT * FROM errands ORDER BY day ASC") abstract fun observeErrands(): Flow<List<Errand>>

    @Query("SELECT * FROM quests ORDER BY startedAt DESC") abstract suspend fun quests(): List<Quest>
    @Query("SELECT * FROM events ORDER BY at ASC") abstract suspend fun events(): List<Event>
    @Query("SELECT * FROM dragons ORDER BY createdAt ASC") abstract suspend fun dragons(): List<Dragon>
    @Query("SELECT * FROM strikes ORDER BY at ASC") abstract suspend fun strikes(): List<Strike>
    @Query("SELECT * FROM letters ORDER BY createdAt ASC") abstract suspend fun letters(): List<Letter>
    @Query("SELECT * FROM errands ORDER BY day ASC") abstract suspend fun errands(): List<Errand>
    @Query("SELECT * FROM quests WHERE id = :id") abstract suspend fun quest(id: Long): Quest?
    @Query("SELECT * FROM events WHERE questId = :id ORDER BY at ASC") abstract suspend fun eventsOf(id: Long): List<Event>

    @Insert abstract suspend fun insert(q: Quest): Long
    @Update abstract suspend fun update(q: Quest)
    @Insert abstract suspend fun insert(e: Event): Long
    @Insert abstract suspend fun insert(d: Dragon): Long
    @Update abstract suspend fun update(d: Dragon)
    @Insert abstract suspend fun insert(s: Strike): Long
    @Delete abstract suspend fun delete(s: Strike)
    @Insert abstract suspend fun insert(l: Letter): Long
    @Update abstract suspend fun update(l: Letter)
    @Delete abstract suspend fun delete(l: Letter)
    @Insert abstract suspend fun insert(e: Errand)
    @Query("DELETE FROM errands WHERE day = :day") abstract suspend fun deleteErrand(day: Long)

    @Query("DELETE FROM quests WHERE id = :id") protected abstract suspend fun deleteQuestRow(id: Long)
    @Query("DELETE FROM events WHERE questId = :id") protected abstract suspend fun deleteEventsOf(id: Long)
    @Query("DELETE FROM dragons WHERE id = :id") protected abstract suspend fun deleteDragonRow(id: Long)
    @Query("DELETE FROM strikes WHERE dragonId = :id") protected abstract suspend fun deleteStrikesOf(id: Long)

    /** A quest and everything that happened on it. */
    @Transaction
    open suspend fun deleteQuest(id: Long) {
        deleteEventsOf(id); deleteQuestRow(id)
    }

    @Transaction
    open suspend fun deleteDragon(id: Long) {
        deleteStrikesOf(id); deleteDragonRow(id)
    }

    @Query("DELETE FROM quests") protected abstract suspend fun clearQuests()
    @Query("DELETE FROM events") protected abstract suspend fun clearEvents()
    @Query("DELETE FROM dragons") protected abstract suspend fun clearDragons()
    @Query("DELETE FROM strikes") protected abstract suspend fun clearStrikes()
    @Query("DELETE FROM letters") protected abstract suspend fun clearLetters()
    @Query("DELETE FROM errands") protected abstract suspend fun clearErrands()
    @Insert protected abstract suspend fun insertQuests(l: List<Quest>)
    @Insert protected abstract suspend fun insertEvents(l: List<Event>)
    @Insert protected abstract suspend fun insertDragons(l: List<Dragon>)
    @Insert protected abstract suspend fun insertStrikes(l: List<Strike>)
    @Insert protected abstract suspend fun insertLetters(l: List<Letter>)
    @Insert protected abstract suspend fun insertErrands(l: List<Errand>)

    @Transaction
    open suspend fun replaceAll(all: Everything) {
        clearQuests(); clearEvents(); clearDragons(); clearStrikes(); clearLetters(); clearErrands()
        insertQuests(all.quests); insertEvents(all.events); insertDragons(all.dragons)
        insertStrikes(all.strikes); insertLetters(all.letters); insertErrands(all.errands)
    }

    suspend fun everything() = Everything(quests(), events(), dragons(), strikes(), letters(), errands())
}

/** The party's whole history, for backups. */
class Everything(
    val quests: List<Quest>,
    val events: List<Event>,
    val dragons: List<Dragon>,
    val strikes: List<Strike>,
    val letters: List<Letter>,
    val errands: List<Errand>,
)

@Database(entities = [Quest::class, Event::class, Dragon::class, Strike::class, Letter::class, Errand::class], version = 1, exportSchema = true)
abstract class PartyDatabase : RoomDatabase() {
    abstract fun dao(): PartyDao

    companion object {
        fun create(context: Context): PartyDatabase =
            Room.databaseBuilder(context.applicationContext, PartyDatabase::class.java, "party.db").build()
    }
}
