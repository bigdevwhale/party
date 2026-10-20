package app.party.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** The party's whole history in one JSON file. Nothing leaves the phone unless you export it. */
object Backup {
    private const val VERSION = 1

    fun encode(all: Everything, s: Settings): String = JSONObject()
        .put("app", "party")
        .put("version", VERSION)
        .put("party", JSONObject().put("nameA", s.nameA).put("nameB", s.nameB).put("femaleA", s.femaleA).put("femaleB", s.femaleB)
            .put("crest", s.crest.encode()).put("motto", s.motto).put("seed", s.seed).put("foundedAt", s.foundedAt))
        .put("quests", JSONArray(all.quests.map {
            JSONObject().put("id", it.id).put("source", it.source).put("customTitle", it.customTitle).put("customTasks", it.customTasks)
                .put("school", it.school).put("rank", it.rank).put("startedAt", it.startedAt).put("endedAt", it.endedAt)
                .put("tasksDone", it.tasksDone).put("secretA", it.secretA).put("secretB", it.secretB).put("doneA", it.doneA).put("doneB", it.doneB)
                .put("guessedA", it.guessedA).put("guessedB", it.guessedB).put("sealMinutes", it.sealMinutes).put("cracks", it.cracks)
                .put("lineA", it.lineA).put("lineB", it.lineB).put("trophyName", it.trophyName).put("trophyLore", it.trophyLore)
                .put("trophyPhoto", it.trophyPhoto).put("placeX", it.placeX.toDouble()).put("placeY", it.placeY.toDouble())
                .put("placeName", it.placeName).put("chapter", it.chapter).put("xp", it.xp).put("stage", it.stage)
        }))
        .put("events", JSONArray(all.events.map {
            JSONObject().put("id", it.id).put("questId", it.questId).put("at", it.at).put("kind", it.kind).put("ref", it.ref).put("value", it.value).put("who", it.who)
        }))
        .put("dragons", JSONArray(all.dragons.map {
            JSONObject().put("id", it.id).put("name", it.name).put("deed", it.deed).put("hp", it.hp).put("seed", it.seed).put("createdAt", it.createdAt).put("slainAt", it.slainAt)
        }))
        .put("strikes", JSONArray(all.strikes.map { JSONObject().put("id", it.id).put("dragonId", it.dragonId).put("at", it.at).put("note", it.note) }))
        .put("letters", JSONArray(all.letters.map {
            JSONObject().put("id", it.id).put("from", it.from).put("text", it.text).put("createdAt", it.createdAt).put("openedAt", it.openedAt).put("questId", it.questId)
        }))
        .put("errands", JSONArray(all.errands.map { JSONObject().put("day", it.day).put("ravenId", it.ravenId).put("doneAt", it.doneAt) }))
        .toString(2)

    private fun JSONObject.list(key: String) = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()

    /** Throws [IllegalArgumentException] when the text is not a Party of Two backup. */
    fun decode(text: String): Pair<Everything, (Settings) -> Settings> {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "party") { "Not a Party of Two backup" }
        val p = root.optJSONObject("party") ?: JSONObject()
        val settings: (Settings) -> Settings = { s ->
            s.copy(
                onboarded = true, nameA = p.optString("nameA"), nameB = p.optString("nameB"),
                femaleA = p.optBoolean("femaleA"), femaleB = p.optBoolean("femaleB", true),
                crest = app.party.domain.Crest.decode(p.optString("crest")) ?: s.crest, motto = p.optString("motto"),
                seed = p.optLong("seed", s.seed), foundedAt = p.optLong("foundedAt", s.foundedAt),
            )
        }
        val all = Everything(
            quests = root.list("quests").map {
                Quest(
                    id = it.getLong("id"), source = it.optString("source"), customTitle = it.optString("customTitle"), customTasks = it.optString("customTasks"),
                    school = it.optString("school", School.ROAD.key), rank = it.optString("rank", "C"), startedAt = it.optLong("startedAt"),
                    endedAt = it.optLong("endedAt"), tasksDone = it.optInt("tasksDone"), secretA = it.optString("secretA"), secretB = it.optString("secretB"),
                    doneA = it.optInt("doneA", Done.UNTOLD), doneB = it.optInt("doneB", Done.UNTOLD), guessedA = it.optInt("guessedA", -1),
                    guessedB = it.optInt("guessedB", -1), sealMinutes = it.optInt("sealMinutes"), cracks = it.optInt("cracks"),
                    lineA = it.optString("lineA"), lineB = it.optString("lineB"), trophyName = it.optString("trophyName"),
                    trophyLore = it.optString("trophyLore"), trophyPhoto = it.optString("trophyPhoto"),
                    placeX = it.optDouble("placeX", -1.0).toFloat(), placeY = it.optDouble("placeY", -1.0).toFloat(),
                    placeName = it.optString("placeName"), chapter = it.optInt("chapter"), xp = it.optInt("xp"), stage = it.optInt("stage"),
                )
            },
            events = root.list("events").map {
                Event(id = it.getLong("id"), questId = it.getLong("questId"), at = it.optLong("at"), kind = it.optString("kind"), ref = it.optString("ref"), value = it.optInt("value"), who = it.optInt("who", -1))
            },
            dragons = root.list("dragons").map {
                Dragon(id = it.getLong("id"), name = it.optString("name"), deed = it.optString("deed"), hp = it.optInt("hp", 5), seed = it.optInt("seed"), createdAt = it.optLong("createdAt"), slainAt = it.optLong("slainAt"))
            },
            strikes = root.list("strikes").map { Strike(id = it.getLong("id"), dragonId = it.getLong("dragonId"), at = it.optLong("at"), note = it.optString("note")) },
            letters = root.list("letters").map {
                Letter(id = it.getLong("id"), from = it.optInt("from"), text = it.optString("text"), createdAt = it.optLong("createdAt"), openedAt = it.optLong("openedAt"), questId = it.optLong("questId"))
            },
            errands = root.list("errands").map { Errand(day = it.getLong("day"), ravenId = it.optString("ravenId"), doneAt = it.optLong("doneAt")) },
        )
        return all to settings
    }

    fun write(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("Can't write there")
    }

    fun read(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Can't read that")
}
