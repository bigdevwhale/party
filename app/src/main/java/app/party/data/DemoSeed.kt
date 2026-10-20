package app.party.data

import app.party.AppContainer
import app.party.domain.Beast
import app.party.domain.Crest
import app.party.domain.GameMaster
import app.party.domain.Hour
import app.party.domain.Ordinary
import app.party.domain.Symbol
import app.party.domain.Xp
import app.party.ui.art.Realm
import java.util.Locale
import kotlin.random.Random

/** A made-up year of a party (`-Pdemo`), so screenshots have a chronicle, a map and a treasury to show. */
object DemoSeed {
    private const val DAY = GameMaster.DAY

    suspend fun seedIfEmpty(app: AppContainer) {
        val dao = app.db.dao()
        if (dao.quests().isNotEmpty()) return
        val ru = Locale.getDefault().language == "ru"
        val now = System.currentTimeMillis()
        val seed = 20_261_020L
        app.settings.update {
            it.copy(
                onboarded = true,
                nameA = if (ru) "Аня" else "Anna", nameB = if (ru) "Марк" else "Mark", femaleA = true, femaleB = false,
                crest = Crest(Hour.DUSK to Hour.NIGHT, Ordinary.WAVES to Ordinary.WAVES, Beast.FOX to Beast.OWL, Symbol.STAR to Symbol.STAR, true to true),
                motto = if (ru) "Сначала поесть, потом решать" else "First we eat, then we decide",
                seed = seed, foundedAt = now - 300 * DAY,
            )
        }
        val lore = app.lore
        val rnd = Random(7)
        val realm = Realm(seed)
        fun land(): Pair<Float, Float> {
            repeat(500) {
                val x = 0.15f + rnd.nextFloat() * 0.7f
                val y = 0.25f + rnd.nextFloat() * 0.8f
                if (realm.land(x, y)) return x to y
            }
            return 0.5f to 0.66f
        }
        val linesEn = listOf(
            "We got lost twice and it was the best part.", "The rain started exactly when we sat down.", "I didn't know you could whistle like that.",
            "The soup was terrible, the evening wasn't.", "Never again on a hill at 5 am. Next month?", "You laughed so hard the owl left.",
        )
        val linesRu = listOf(
            "Дважды заблудились — и это было лучшее.", "Дождь начался ровно тогда, когда мы сели.", "Не знала, что ты так умеешь свистеть.",
            "Суп был ужасен, вечер — нет.", "Больше никогда на холм в пять утра. В следующем месяце?", "Ты смеялся так, что сова улетела.",
        )
        val trophies = if (ru) listOf("Билет на последний трамвай", "Камешек с реки", "Пуговица из лавки старьёвщика", "Засушенный клён", "Бумажный фонарик")
        else listOf("Ticket for the last tram", "A river pebble", "A button from the junk shop", "A pressed maple leaf", "A paper lantern")
        val places = if (ru) listOf("Таверна Сонной Цапли", "Мост Двух Зонтов", "Холм Раннего Солнца", "Лавка Потерянных Пуговиц", "Сад Тихих Скамеек", "Башня Ночного Чая")
        else listOf("Inn of the Sleepy Heron", "Bridge of Two Umbrellas", "Hill of the Early Sun", "Shop of Lost Buttons", "Garden of Quiet Benches", "Tower of Night Tea")

        val picks = listOf("q.hearth.01", "q.road.02", "c1.1", "q.wild.05", "q.arts.03", "c1.2", "q.lore.02", "q.tender.01", "c1.3", "q.craft.06", "q.daring.04", "q.road.09", "c1.4", "q.wild.10")
            .filter { lore[it] != null }
        picks.forEachIndexed { i, id ->
            val e = lore[id]!!
            val start = now - (280 - i * 19) * DAY + 18 * 60 * 60 * 1000L
            val tasks = lore.questText(e, "en").tasks.size
            val (px, py) = land()
            val secrets = lore.secrets.shuffled(rnd).take(2)
            var q = Quest(
                source = id,
                school = (e.schoolOf ?: School.LORE).key,
                rank = (e.rankOf ?: Rank.SILVER).code.toString(),
                startedAt = start,
                endedAt = start + (2 + rnd.nextInt(4)) * 60 * 60 * 1000L,
                tasksDone = (1 shl (tasks - rnd.nextInt(0, 2))) - 1,
                secretA = secrets[0].id, secretB = secrets[1].id,
                doneA = listOf(Done.YES, Done.YES, Done.PARTLY, Done.NO)[rnd.nextInt(4)],
                doneB = listOf(Done.YES, Done.PARTLY, Done.YES)[rnd.nextInt(3)],
                guessedA = rnd.nextInt(2), guessedB = if (i % 3 == 0) 1 else rnd.nextInt(2),
                sealMinutes = if (i % 2 == 0) 45 + rnd.nextInt(30) else 0,
                cracks = if (i == 5) 1 else 0,
                lineA = (if (ru) linesRu else linesEn)[i % 6],
                lineB = (if (ru) linesRu else linesEn)[(i + 3) % 6],
                trophyName = if (i % 3 == 1) trophies[(i / 3) % trophies.size] else "",
                placeX = if (i % 2 == 0 || i > 9) px else -1f, placeY = if (i % 2 == 0 || i > 9) py else -1f,
                chapter = i + 1,
                stage = Stage.PLACE,
            )
            if (q.placeX >= 0) q = q.copy(placeName = places[i % places.size])
            if (q.trophyName.isNotEmpty()) q = q.copy(trophyLore = if (ru) "+2 к нежности. Проклятие: теряется в карманах." else "+2 to Tenderness. Curse: hides in pockets.")
            val qid = dao.insert(q)
            val evs = buildList {
                repeat(1 + rnd.nextInt(3)) { k -> add(Event(questId = qid, at = start + (k + 1) * 30 * 60 * 1000L, kind = EventKind.ENCOUNTER.name, ref = lore.encounters[rnd.nextInt(lore.encounters.size)].id)) }
                repeat(2 + rnd.nextInt(4)) { k -> add(Event(questId = qid, at = start + (k + 2) * 25 * 60 * 1000L, kind = EventKind.CAMPFIRE.name, ref = lore.campfire[rnd.nextInt(lore.campfire.size)].id)) }
                if (i == 4) add(Event(questId = qid, at = start + 70 * 60 * 1000L, kind = EventKind.ROLL.name, ref = lore.fates.first { it.boon }.id, value = 20))
                if (i == 8) add(Event(questId = qid, at = start + 70 * 60 * 1000L, kind = EventKind.ROLL.name, ref = lore.fates.first { !it.boon }.id, value = 1))
                add(Event(questId = qid, at = start + 80 * 60 * 1000L, kind = EventKind.ROLL.name, value = 3 + rnd.nextInt(15)))
            }
            evs.forEach { dao.insert(it) }
            val saved = q.copy(id = qid)
            dao.update(saved.copy(xp = Xp.reckon(saved, evs).sumOf { it.xp }))
        }

        val slain = dao.insert(Dragon(name = if (ru) "Пылевик Неразобранный" else "Dustmaw the Unsorted", deed = if (ru) "Разобрать кладовку" else "Clear out the box room", hp = 5, seed = 2, createdAt = now - 120 * DAY, slainAt = now - 60 * DAY))
        repeat(5) { dao.insert(Strike(dragonId = slain, at = now - (110 - it * 12) * DAY)) }
        val living = dao.insert(Dragon(name = if (ru) "Бумагожор Неоплаченный" else "Grimfang the Unpaid", deed = if (ru) "Разобраться с налоговыми бумагами" else "Sort out the tax papers", hp = 8, seed = 0, createdAt = now - 20 * DAY))
        repeat(3) { dao.insert(Strike(dragonId = living, at = now - (15 - it * 4) * DAY, note = if (it == 0) (if (ru) "Нашли все чеки" else "Found all the receipts") else "")) }
        dao.insert(Letter(from = 1, text = if (ru) "Спасибо за холм в пять утра." else "Thank you for the hill at 5 am.", createdAt = now - 3 * DAY))

        val today = GameMaster.epochDay(now)
        for (d in 1..9) {
            val day = today - d
            GameMaster.raven(lore, seed, day)?.let { dao.insert(Errand(day, it.id, now - d * DAY)) }
        }
    }
}
