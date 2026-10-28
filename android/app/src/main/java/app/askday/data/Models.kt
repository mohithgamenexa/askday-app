package app.askday.data

import kotlinx.serialization.Serializable

// Everything Askday knows, saved on the phone as one JSON file.
// Classes are mutable so the engine can edit a copy and save it in one go.

@Serializable
enum class ThemeChoice { light, dark, system }

@Serializable
enum class Reaction { love, haha, fire, wow }

@Serializable
enum class DropSlot(val hour: Int, val label: String, val time: String) {
    morning(9, "Morning", "9:00 AM"),
    evening(19, "Evening", "7:00 PM"),
    night(22, "Night", "10:00 PM"),
}

@Serializable
data class NotifPrefs(
    var daily: Boolean = true,
    var unlocked: Boolean = true,
    var nudges: Boolean = true,
    var reminders: Boolean = true,
)

@Serializable
data class Stats(var answers: Int = 0, var bestStreak: Int = 0)

@Serializable
data class Profile(
    val id: String,
    var name: String,
    var photo: String? = null,
    /** Verified phone number, for finding friends later. Never shown to other people. */
    var phone: String? = null,
    var notif: NotifPrefs = NotifPrefs(),
    var stats: Stats = Stats(),
    /** badge id → when it was earned */
    var badges: MutableMap<String, Long> = mutableMapOf(),
    var createdAt: Long = 0,
    var sample: Boolean = false,
)

@Serializable
data class GroupRec(
    val id: String,
    var name: String,
    var photo: String? = null,
    var memberIds: MutableList<String>,
    var inviteCode: String,
    var dropSlot: DropSlot,
    var seed: Int,
    var day: Int = 0,
    var streak: Int = 0,
    var bestStreak: Int = 0,
    var currentId: String? = null,
    var nextDropAt: Long? = null,
    var createdAt: Long,
    var lastActivityAt: Long,
    /** friend id → question id they were last nudged on */
    var nudged: MutableMap<String, String> = mutableMapOf(),
    /** Streak freezes saved up (max 2). One is used automatically when someone misses a day. */
    var freezes: Int = 0,
)

@Serializable
data class QuestionRec(
    val id: String,
    val day: Int,
    val text: String,
    val qIndex: Int,
    val dropAt: Long,
    val unlockAt: Long,
    var unlocked: Boolean = false,
    var allAnswered: Boolean = false,
    var answeredBy: MutableList<String> = mutableListOf(),
    var topAnswerId: String? = null,
    var topCount: Int = 0,
    /** True when a streak freeze saved the streak on this day. */
    var frozen: Boolean = false,
)

@Serializable
data class Answer(
    val id: String,
    val uid: String,
    val text: String,
    val createdAt: Long,
    var reactions: MutableMap<String, Reaction> = mutableMapOf(),
    var replyCount: Int = 0,
)

@Serializable
data class Comment(val id: String, val uid: String, val text: String, val answerId: String?, val createdAt: Long)

@Serializable
enum class SimKind { react, reply, chat }

@Serializable
data class SimEvent(
    val at: Long,
    val kind: SimKind,
    val uid: String,
    val target: String? = null,
    val text: String? = null,
    val reaction: Reaction? = null,
)

/** What the sample friends will do, and when. */
@Serializable
data class Sim(
    var answerAt: MutableMap<String, Long?> = mutableMapOf(),
    var events: MutableList<SimEvent> = mutableListOf(),
)

@Serializable
enum class CelebrationKind { badge, streak, freeze }

@Serializable
data class Celebration(
    val id: String,
    val kind: CelebrationKind,
    val badge: String? = null,
    val streak: Int? = null,
    val groupName: String? = null,
)

@Serializable
data class GameScore(val score: Int, val total: Int)

@Serializable
data class Seen(val questionId: String, val count: Int)

@Serializable
data class DB(
    var me: String? = null,
    var profile: Profile? = null,
    var people: MutableMap<String, Profile> = mutableMapOf(),
    var groups: MutableMap<String, GroupRec> = mutableMapOf(),
    /** group id → question id → question */
    var questions: MutableMap<String, MutableMap<String, QuestionRec>> = mutableMapOf(),
    /** "group/question" → answers */
    var answers: MutableMap<String, MutableList<Answer>> = mutableMapOf(),
    var comments: MutableMap<String, MutableList<Comment>> = mutableMapOf(),
    var seen: MutableMap<String, Seen> = mutableMapOf(),
    var sim: MutableMap<String, Sim> = mutableMapOf(),
    var theme: ThemeChoice = ThemeChoice.light,
    var celebrations: MutableList<Celebration> = mutableListOf(),
    var games: MutableMap<String, GameScore> = mutableMapOf(),
)

fun qkey(groupId: String, questionId: String) = "$groupId/$questionId"
