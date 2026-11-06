package com.taskalarm.app.data

/** A thing found in almost every home that the user photographs to stop the alarm. */
data class HouseObject(
    val key: String,
    /** With the definite article, e.g. "המקרר". */
    val name: String,
    /** Framing tip, e.g. "צלם את דלת המקרר מקרוב". */
    val hint: String,
    val room: String,
)

object Rooms {
    const val KITCHEN = "מטבח"
    const val BATHROOM = "אמבטיה ושירותים"
    const val LIVING = "סלון"
    const val ENTRANCE = "כניסה"
    const val GENERAL = "בכל הבית"
    val order = listOf(KITCHEN, BATHROOM, LIVING, ENTRANCE, GENERAL)
}

/**
 * Photo missions: get up and photograph one or two things in the home.
 * Each object is registered once with a reference photo; in the morning the live camera
 * must match that reference. Pure Kotlin so the library is unit tested.
 */
object PhotoCatalog {
    val objects: List<HouseObject> = listOf(
        HouseObject("fridge", "המקרר", "צלם את דלת המקרר מקרוב", Rooms.KITCHEN),
        HouseObject("fridge_inside", "המדפים בתוך המקרר", "פתח את המקרר וצלם את המדפים", Rooms.KITCHEN),
        HouseObject("kitchen_sink", "כיור המטבח", "צלם את הכיור והברז", Rooms.KITCHEN),
        HouseObject("kettle", "הקומקום", "צלם את הקומקום על השיש", Rooms.KITCHEN),
        HouseObject("coffee", "מכונת הקפה", "צלם את מכונת הקפה מלפנים", Rooms.KITCHEN),
        HouseObject("microwave", "המיקרוגל", "צלם את דלת המיקרוגל", Rooms.KITCHEN),
        HouseObject("oven", "התנור", "צלם את דלת התנור", Rooms.KITCHEN),
        HouseObject("stove", "הכיריים", "צלם את הכיריים מלמעלה", Rooms.KITCHEN),
        HouseObject("trash", "פח האשפה", "צלם את פח האשפה", Rooms.KITCHEN),
        HouseObject("dining_table", "שולחן האוכל", "צלם את שולחן האוכל", Rooms.KITCHEN),
        HouseObject("bath_sink", "כיור האמבטיה", "צלם את הכיור והברז באמבטיה", Rooms.BATHROOM),
        HouseObject("toilet", "האסלה", "צלם את האסלה", Rooms.BATHROOM),
        HouseObject("shower", "המקלחת", "צלם את המקלחת או האמבטיה", Rooms.BATHROOM),
        HouseObject("toothbrush", "מברשת השיניים", "צלם את כוס מברשות השיניים", Rooms.BATHROOM),
        HouseObject("mirror", "המראה באמבטיה", "צלם את המראה", Rooms.BATHROOM),
        HouseObject("towel", "המגבת", "צלם את המגבת התלויה", Rooms.BATHROOM),
        HouseObject("washing_machine", "מכונת הכביסה", "צלם את מכונת הכביסה מלפנים", Rooms.BATHROOM),
        HouseObject("tv", "הטלוויזיה", "צלם את הטלוויזיה", Rooms.LIVING),
        HouseObject("sofa", "הספה", "צלם את הספה", Rooms.LIVING),
        HouseObject("window", "החלון בסלון", "צלם את החלון", Rooms.LIVING),
        HouseObject("plant", "העציץ", "צלם את העציץ", Rooms.LIVING),
        HouseObject("bookshelf", "מדף הספרים", "צלם את המדף", Rooms.LIVING),
        HouseObject("wall_clock", "שעון הקיר", "צלם את השעון", Rooms.LIVING),
        HouseObject("air_conditioner", "המזגן", "צלם את המזגן", Rooms.LIVING),
        HouseObject("front_door", "דלת הכניסה", "צלם את דלת הכניסה מבפנים", Rooms.ENTRANCE),
        HouseObject("shoes", "הנעליים", "צלם את הנעליים ליד הדלת", Rooms.ENTRANCE),
        HouseObject("keys", "מתלה המפתחות", "צלם את המקום שבו המפתחות", Rooms.ENTRANCE),
        HouseObject("light_switch", "מתג התאורה", "צלם את מתג האור מקרוב", Rooms.GENERAL),
        HouseObject("balcony", "המרפסת", "צלם את המרפסת", Rooms.GENERAL),
        HouseObject("laundry", "סל הכביסה", "צלם את סל הכביסה", Rooms.GENERAL),
    )

    private val byKey = objects.associateBy { it.key }

    fun get(key: String): HouseObject? = byKey[key]

    /** One or two objects; two-object missions require walking between them. */
    val missions: List<PhotoMission> = buildList {
        objects.forEach { add(PhotoMission(listOf(it))) }
        for (i in objects.indices) for (j in i + 1 until objects.size) {
            add(PhotoMission(listOf(objects[i], objects[j])))
        }
    }

    private val missionsById = missions.associateBy { it.id }

    fun mission(id: String?): PhotoMission? = id?.let { missionsById[it] }

    /** Missions whose objects have all been photographed (registered). */
    fun eligible(registered: Set<String>): List<PhotoMission> =
        missions.filter { m -> m.objects.all { it.key in registered } }

    fun random(
        registered: Set<String>,
        difficulty: Int = MissionDifficulty.ANY,
        exclude: String? = null,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): PhotoMission? {
        val ok = eligible(registered).filter { it.id != exclude }
        val wanted = ok.filter { difficulty == MissionDifficulty.ANY || it.difficulty == difficulty }
        return wanted.randomOrNull(random) ?: ok.randomOrNull(random)
    }
}

data class PhotoMission(val objects: List<HouseObject>) {
    val id: String = "photo:" + objects.joinToString("+") { it.key }
    val title: String = "צלם את " + objects.joinToString(" ואת ") { it.name }
    /** Easy: one object. Medium: two in the same room. Hard: two in different rooms. */
    val difficulty: Int = when {
        objects.size == 1 -> MissionDifficulty.EASY
        objects.map { it.room }.distinct().size == 1 -> MissionDifficulty.MEDIUM
        else -> MissionDifficulty.HARD
    }
}

object MissionDifficulty {
    const val ANY = 0
    const val EASY = 1
    const val MEDIUM = 2
    const val HARD = 3
    fun name(level: Int) = when (level) {
        EASY -> "קל"
        MEDIUM -> "בינוני"
        HARD -> "קשה"
        else -> "הכול"
    }
    fun explain(level: Int) = when (level) {
        EASY -> "מקום אחד"
        MEDIUM -> "שני מקומות באותו חדר"
        HARD -> "שני מקומות בחדרים שונים"
        else -> "כל המשימות"
    }
}
