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
    var username: String? = null,
    /** Day/month/year, used only for the 13+ age check. Never shown. */
    var birthday: String? = null,
    /** Question packs picked in onboarding ("Funny", "Deep talk", ...). */
    var packs: MutableList<String> = mutableListOf(),
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
    /** Tomorrow's question, written by today's question master or promoted from the Ask of the day. */
    var nextQuestion: String? = null,
    var nextQuestionBy: String? = null,
    /** Joined automatically after onboarding, with sample members, so the app is never empty. */
    var starter: Boolean = false,
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
    /** Who wrote this question, when a member did (question master or promoted Ask). */
    val askedBy: String? = null,
)

@Serializable
data class Answer(
    val id: String,
    val uid: String,
    val text: String,
    val createdAt: Long,
    var reactions: MutableMap<String, Reaction> = mutableMapOf(),
    var replyCount: Int = 0,
    /** Optional photo or voice note saved on the phone. */
    val photo: String? = null,
    val voice: String? = null,
    val voiceMs: Long = 0,
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

/** One friend's (or your) answer to the global Askday moment. */
@Serializable
data class MomentAnswer(
    val uid: String,
    val text: String,
    val at: Long,
    val late: Boolean,
    var reactions: MutableMap<String, Reaction> = mutableMapOf(),
    val photo: String? = null,
    val voice: String? = null,
    val voiceMs: Long = 0,
)

/** The global Askday moment: one question for everyone, once a day, at a different time. */
@Serializable
data class Moment(
    val dayKey: String,
    val text: String,
    val qIndex: Int,
    val dropAt: Long,
    /** Answers before this count as "on time"; later ones are marked late. */
    val onTimeUntil: Long,
    var answers: MutableList<MomentAnswer> = mutableListOf(),
    /** friend → when they'll answer (null = not today) */
    var plan: MutableMap<String, Long?> = mutableMapOf(),
)

@Serializable
enum class AskType(val label: String) { text("Text"), poll("Poll"), thisOrThat("This or that"), slider("1–10 slider"), member("Pick a member") }

@Serializable
data class AskResponse(
    val at: Long,
    val text: String? = null,
    val option: Int? = null,
    val value: Int? = null,
    val member: String? = null,
    /** Guess the group: which option you thought most people would pick. */
    val guess: Int? = null,
)

/** Photo of the day: the back camera, plus an optional front-camera shot. */
@Serializable
data class DailyPhoto(val prompt: String, val back: String, val front: String? = null, val at: Long)

/** A question a group member asks the rest of the group. Lasts 24 hours. */
@Serializable
data class Ask(
    val id: String,
    val groupId: String,
    val uid: String,
    val type: AskType,
    val question: String,
    val options: List<String> = emptyList(),
    val createdAt: Long,
    val expiresAt: Long,
    var responses: MutableMap<String, AskResponse> = mutableMapOf(),
    var skippedBy: MutableList<String> = mutableListOf(),
    /** friend → when they'll respond */
    var plan: MutableMap<String, Long> = mutableMapOf(),
    /** Became a group's daily question. */
    var promoted: Boolean = false,
)

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
    /** Finished onboarding (find friends, notifications and vibe). */
    var onboarded: Boolean = false,
    /** The answer written before signing up, posted once the account exists. */
    var pendingAnswer: String? = null,
    var moment: Moment? = null,
    /** Every global moment you answered, by day ("20261028" → your answer), for Memories. */
    var momentLog: MutableMap<String, String> = mutableMapOf(),
    var momentQuestions: MutableMap<String, String> = mutableMapOf(),
    /** group id → its Asks */
    var asks: MutableMap<String, MutableList<Ask>> = mutableMapOf(),
    /** group id → the day its sample friends last posted Asks */
    var askDays: MutableMap<String, String> = mutableMapOf(),
    /** Quick questions you answered: "day/question index" → your answer */
    var quick: MutableMap<String, String> = mutableMapOf(),
    /** Starter groups were added (once only, even if you leave them later). */
    var starterDone: Boolean = false,
    /** The last 7 days of Askday moments, so you can still answer them (late). */
    var pastMoments: MutableList<Moment> = mutableListOf(),
    // ---- wave 1: daily rituals and games (all keyed by day "20261028")
    var photos: MutableMap<String, DailyPhoto> = mutableMapOf(),
    var moods: MutableMap<String, String> = mutableMapOf(),
    var challenges: MutableMap<String, Long> = mutableMapOf(),
    var battles: MutableMap<String, Int> = mutableMapOf(),
    var doodles: MutableMap<String, String> = mutableMapOf(),
    /** group id → best group-trivia score */
    var trivia: MutableMap<String, Int> = mutableMapOf(),
)

fun qkey(groupId: String, questionId: String) = "$groupId/$questionId"
