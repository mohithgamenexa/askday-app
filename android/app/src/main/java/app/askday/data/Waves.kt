package app.askday.data

import java.util.Calendar

// Waves 2–4: friendships and memories, progress and expression, smart features and keepsakes.
// Plain Kotlin, so the rules can be tested without a phone.

val COMPLIMENT_QUESTIONS = listOf(
    "Who has the best laugh?", "Who gives the best advice?", "Who would you call in a crisis?", "Who always makes you smile?",
    "Who has the best taste in music?", "Who is the most loyal friend?", "Who tells the best stories?", "Who is secretly the kindest?",
    "Who would win a dance-off?", "Who brings the best snacks?", "Who is the most creative?", "Who is the best listener?",
)

class Season(val name: String, val pack: String, val blurb: String)

/** A theme for every month. */
val SEASONS = listOf(
    Season("New Beginnings", "Deep talk", "Fresh starts and big hopes."), Season("Love & Friends", "Spicy", "The people who matter."),
    Season("Spring Fever", "Funny", "Silly questions for sunny days."), Season("Exam Season", "Campus", "Survive it together."),
    Season("Summer Stories", "Throwback", "Holidays, past and present."), Season("Monsoon Moods", "Deep talk", "Rainy-day thoughts."),
    Season("Food Month", "Funny", "Everything tastes better shared."), Season("Throwback August", "Throwback", "Remember when?"),
    Season("Hot Takes", "Hot takes", "Pick a side."), Season("Festive Season", "Family", "Lights, sweets and family."),
    Season("Gratitude", "Deep talk", "Thankful for each other."), Season("Year in Review", "Throwback", "The year, together."),
)

object Waves {
    private const val DAY = 86_400_000L
    private fun rand(min: Long, max: Long) = min + (Engine.random.nextDouble() * (max - min)).toLong()

    // ================================================================ wave 2

    // ----------------------------------------------------- compliment polls

    /** Today's compliment poll in a group (created on first look). Sample friends vote right away. */
    fun complimentPoll(d: DB, groupId: String, now: Long): ComplimentPoll? {
        val g = d.groups[groupId] ?: return null
        val day = Social.dayKey(now)
        val list = d.compliments.getOrPut(groupId) { mutableListOf() }
        list.firstOrNull { it.day == day }?.let { return it }
        val r = Mulberry32(day.toInt() + groupId.hashCode())
        val poll = ComplimentPoll(Engine.newId(), day, COMPLIMENT_QUESTIONS[(r.next() * COMPLIMENT_QUESTIONS.size).toInt()])
        val friends = g.memberIds.filter(Engine::isSample)
        for (f in friends) {
            val choices = g.memberIds.filter { it != f }
            // Friends pick you a little more often, so the feature feels alive.
            poll.votes[f] = if (Engine.random.nextDouble() < 0.35 && d.me != null) d.me!! else choices[Engine.random.nextInt(choices.size)]
        }
        list.add(poll)
        list.removeAll { it.day < Social.dayKey(now - 30 * DAY) }
        return poll
    }

    fun voteCompliment(d: DB, groupId: String, pollId: String, target: String) {
        val me = d.me ?: throw AskdayError("Sign in first.")
        if (target == me) throw AskdayError("Pick someone else.")
        val p = d.compliments[groupId]?.firstOrNull { it.id == pollId } ?: throw AskdayError("This poll has ended.")
        if (p.votes.containsKey(me)) throw AskdayError("You’ve already voted today.")
        p.votes[me] = target
    }

    /** People who picked you (kept anonymous; one hint at a time). */
    fun pickedMe(d: DB, poll: ComplimentPoll): List<String> = poll.votes.filter { it.value == d.me }.keys.toList()

    /** Reveals the first letter of one person who picked you. */
    fun revealHint(d: DB, groupId: String, pollId: String): String? {
        val p = d.compliments[groupId]?.firstOrNull { it.id == pollId } ?: return null
        val next = pickedMe(d, p).firstOrNull { it !in p.hintsSeen } ?: return null
        p.hintsSeen.add(next)
        return next
    }

    // ----------------------------------------------------------------- duos

    fun startDuo(d: DB, friend: String, now: Long): String {
        if (d.duos.any { it.friend == friend }) return d.duos.first { it.friend == friend }.id
        val duo = Duo(Engine.newId(), friend, now)
        d.duos.add(duo)
        return duo.id
    }

    /** Today's duo question (the same one for both of you). */
    fun duoToday(d: DB, duoId: String, now: Long): DuoDay? {
        val duo = d.duos.firstOrNull { it.id == duoId } ?: return null
        val day = Social.dayKey(now)
        return duo.days.getOrPut(day) { DuoDay(questionIndexFor(duo.id.hashCode(), day.toInt() % 365 + 1)) }
    }

    fun answerDuo(d: DB, duoId: String, text: String, now: Long) {
        val duo = d.duos.firstOrNull { it.id == duoId } ?: throw AskdayError("This duo isn’t available.")
        val t = text.trim().take(280)
        if (t.isEmpty()) throw AskdayError("Write an answer first.")
        val today = duoToday(d, duoId, now)!!
        if (today.mine != null) throw AskdayError("You’ve answered today.")
        today.mine = t
        // Your friend answers soon after (a sample answer from the question bank).
        today.theirsAt = now + rand(10_000, 60_000)
        val yesterday = duo.days[Social.dayKey(now - DAY)]
        duo.streak = if (yesterday?.mine != null) duo.streak + 1 else 1
    }

    fun tickDuos(d: DB, now: Long): Boolean {
        var dirty = false
        for (duo in d.duos) for ((_, day) in duo.days) {
            if (day.mine != null && day.theirs == null && day.theirsAt in 1..now) {
                val answers = DEMO_QUESTIONS[day.qIndex].answers
                day.theirs = answers[Math.floorMod(duo.friend.hashCode() + day.qIndex, answers.size)]
                dirty = true
            }
        }
        return dirty
    }

    /** How in sync you two are, 0–100, from your shared poll votes and how much you react to each other. */
    fun compatibility(d: DB, friend: String): Int {
        val me = d.me ?: return 0
        val f = friendship(d, friend)
        val base = if (f.sharedPolls == 0) 50 else (f.agreed * 100 / f.sharedPolls)
        return (base * 0.7 + minOf(30, (f.theirReactions + f.myReactions) * 3)).toInt().coerceIn(0, 100).let { if (me.isEmpty()) 0 else it }
    }

    // ------------------------------------------------- friendship insights

    class Friendship(val sharedPolls: Int, val agreed: Int, val theirReactions: Int, val myReactions: Int, val groupsTogether: Int, val notes: Int)

    fun friendship(d: DB, friend: String): Friendship {
        val me = d.me ?: return Friendship(0, 0, 0, 0, 0, 0)
        var shared = 0
        var agreed = 0
        for ((_, list) in d.asks) for (a in list) {
            if (a.type != AskType.poll && a.type != AskType.thisOrThat) continue
            val mine = a.responses[me]?.option ?: continue
            val theirs = a.responses[friend]?.option ?: continue
            shared++
            if (mine == theirs) agreed++
        }
        var theirR = 0
        var myR = 0
        for ((_, list) in d.answers) for (a in list) {
            if (a.uid == me && a.reactions.containsKey(friend)) theirR++
            if (a.uid == friend && a.reactions.containsKey(me)) myR++
        }
        d.moment?.answers?.forEach { a ->
            if (a.uid == me && a.reactions.containsKey(friend)) theirR++
            if (a.uid == friend && a.reactions.containsKey(me)) myR++
        }
        val groups = d.groups.values.count { me in it.memberIds && friend in it.memberIds }
        return Friendship(shared, agreed, theirR, myR, groups, d.notes.count { (it.from == me && it.to == friend) || (it.to == me && it.from == friend) })
    }

    // -------------------------------------------------------- thank-you notes

    fun sendNote(d: DB, to: String, text: String, now: Long) {
        val me = d.me ?: throw AskdayError("Sign in first.")
        val t = text.trim().take(200)
        if (t.isEmpty()) throw AskdayError("Write a note first.")
        d.notes.add(ThanksNote(Engine.newId(), me, to, t, now))
        // Sample friends sometimes write back.
        if (Engine.isSample(to) && Engine.random.nextDouble() < 0.7) {
            d.notes.add(ThanksNote(Engine.newId(), to, me, listOf("Aww, thank you! You’re the best.", "This made my day.", "Right back at you!")[Engine.random.nextInt(3)], now + 30_000))
        }
    }

    // ------------------------------------------------------------- birthdays

    /** Everyone's birthday as "DD/MM": yours from your profile, and a made-up one for each sample friend. */
    fun birthdayOf(d: DB, uid: String): String? {
        if (uid == d.me) return d.profile?.birthday?.take(5)
        return d.birthdays.getOrPut(uid) {
            val r = Mulberry32(uid.hashCode())
            "%02d/%02d".format(1 + (r.next() * 28).toInt(), 1 + (r.next() * 12).toInt())
        }
    }

    fun isBirthday(d: DB, uid: String, now: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        return birthdayOf(d, uid) == "%02d/%02d".format(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1)
    }

    /** Whose birthday it is today in a group, if anyone's. */
    fun birthdayInGroup(d: DB, groupId: String, now: Long): String? =
        d.groups[groupId]?.memberIds?.firstOrNull { isBirthday(d, it, now) }

    /** On a member's birthday, tomorrow's question becomes about them (unless someone already wrote one). */
    fun applyBirthdays(d: DB, now: Long): Boolean {
        var dirty = false
        for (g in d.groups.values) {
            val who = birthdayInGroup(d, g.id, now + DAY) ?: continue
            if (g.nextQuestion == null) {
                g.nextQuestion = "It’s ${if (who == d.me) "your" else (d.people[who]?.name ?: "a friend") + "’s"} birthday! What’s your favourite memory with ${if (who == d.me) "you" else d.people[who]?.name ?: "them"}?"
                g.nextQuestionBy = null
                dirty = true
            }
        }
        return dirty
    }

    // ------------------------------------------------------------ group album

    /** Every photo in a group: photo answers, plan photos and event photos, newest first. */
    fun album(d: DB, groupId: String): List<Pair<String, Long>> {
        val out = mutableListOf<Pair<String, Long>>()
        for (q in d.questions[groupId]?.values.orEmpty()) for (a in d.answers[qkey(groupId, q.id)].orEmpty()) a.photo?.let { out += it to a.createdAt }
        for (p in d.plans[groupId].orEmpty()) for (ph in p.photos) out += ph to p.createdAt
        d.events[groupId]?.let { e -> for (ph in e.photos) out += ph to 0L }
        return out.sortedByDescending { it.second }
    }

    // ----------------------------------------------------------- time capsule

    fun createCapsule(d: DB, text: String, voice: String?, days: Int, now: Long) {
        val t = text.trim().take(500)
        if (t.isEmpty() && voice == null) throw AskdayError("Write a message or record a voice note.")
        d.capsules.add(Capsule(Engine.newId(), t, voice, now, now + days * DAY))
    }

    // ---------------------------------------------------------- year in review

    class YearStats(val answers: Int, val photos: Int, val bestStreak: Int, val topMood: Mood?, val topAnswer: String?, val asks: Int, val challenges: Int, val friends: Int)

    fun yearStats(d: DB, now: Long): YearStats {
        val me = d.me ?: return YearStats(0, 0, 0, null, null, 0, 0, 0)
        val year = Social.dayKey(now).take(4)
        var answers = d.momentLog.keys.count { it.startsWith(year) }
        var top: Answer? = null
        for ((_, list) in d.answers) for (a in list) if (a.uid == me && Social.dayKey(a.createdAt).startsWith(year)) {
            answers++
            if (top == null || a.reactions.size > top.reactions.size) top = a
        }
        val moods = d.moods.filterKeys { it.startsWith(year) }.values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        return YearStats(
            answers,
            d.photos.keys.count { it.startsWith(year) },
            d.profile?.stats?.bestStreak ?: 0,
            moods?.let { runCatching { Mood.valueOf(it) }.getOrNull() },
            top?.text?.takeIf { it.isNotBlank() },
            d.asks.values.sumOf { l -> l.count { it.uid == me } },
            d.challenges.keys.count { it.startsWith(year) },
            d.people.keys.count(Engine::isSample),
        )
    }

    // ================================================================ wave 3

    fun season(now: Long): Season = SEASONS[Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.MONTH)]

    /** Points for everything you do; 100 per level. */
    fun xp(d: DB): Int {
        val me = d.me ?: return 0
        var xp = (d.profile?.stats?.answers ?: 0) * 10
        for ((_, list) in d.answers) for (a in list) if (a.uid != me && a.reactions.containsKey(me)) xp += 2
        xp += d.asks.values.sumOf { l -> l.count { it.uid == me } } * 15
        xp += d.challenges.size * 10 + d.photos.size * 10 + d.moods.size * 2 + d.doodles.size * 10
        xp += (d.profile?.badges?.size ?: 0) * 25
        xp += d.duos.sumOf { duo -> duo.days.values.count { it.mine != null } } * 10
        xp += d.notes.count { it.from == me } * 5
        xp += d.plans.values.sumOf { l -> l.count { it.by == me } } * 10
        xp += d.capsules.size * 5
        xp += d.quick.size * 5 + d.battles.size * 2
        xp += (d.profile?.stats?.bestStreak ?: 0) * 5
        return xp
    }

    fun level(xp: Int) = 1 + xp / 100
    fun levelProgress(xp: Int) = (xp % 100) / 100f

    /** Group goal: answers together this month, against a target based on group size. */
    fun groupGoal(d: DB, groupId: String, now: Long): Pair<Int, Int> {
        val g = d.groups[groupId] ?: return 0 to 1
        val month = Social.dayKey(now).take(6)
        var done = 0
        for (q in d.questions[groupId]?.values.orEmpty()) if (Social.dayKey(q.dropAt).startsWith(month)) done += q.answeredBy.size
        return done to g.memberIds.size * 20
    }

    // ------------------------------------------------------------------ plans

    fun createPlan(d: DB, groupId: String, title: String, options: List<String>, now: Long): String {
        val me = d.me ?: throw AskdayError("Sign in first.")
        val g = d.groups[groupId] ?: throw AskdayError("This group isn’t available.")
        val t = title.trim()
        val opts = options.map { it.trim() }.filter { it.isNotEmpty() }
        if (t.isEmpty()) throw AskdayError("What’s the plan?")
        if (opts.isEmpty()) throw AskdayError("Add at least one time or day.")
        val plan = Plan(Engine.newId(), groupId, me, t.take(80), opts.take(4), now)
        plan.votes[me] = 0
        for (f in g.memberIds.filter(Engine::isSample)) if (Engine.random.nextDouble() < 0.75) plan.votes[f] = Engine.random.nextInt(plan.options.size)
        d.plans.getOrPut(groupId) { mutableListOf() }.add(plan)
        return plan.id
    }

    fun votePlan(d: DB, groupId: String, planId: String, option: Int) {
        val me = d.me ?: return
        d.plans[groupId]?.firstOrNull { it.id == planId }?.votes?.set(me, option)
    }

    fun addPlanPhoto(d: DB, groupId: String, planId: String, path: String) {
        d.plans[groupId]?.firstOrNull { it.id == planId }?.photos?.add(path)
    }

    // ------------------------------------------------------------------ events

    fun startEvent(d: DB, groupId: String, name: String, now: Long) {
        val n = name.trim()
        if (n.isEmpty()) throw AskdayError("Name the event first.")
        d.events[groupId] = GroupEvent(n.take(40), Social.dayKey(now))
    }

    fun activeEvent(d: DB, groupId: String, now: Long): GroupEvent? = d.events[groupId]?.takeIf { it.day == Social.dayKey(now) }

    // -------------------------------------------------------- custom reactions

    fun addCustomReaction(d: DB, groupId: String, label: String) {
        val l = label.trim().take(16)
        if (l.isEmpty()) throw AskdayError("Name your reaction.")
        val list = d.customReactions.getOrPut(groupId) { mutableListOf() }
        if (list.size >= 3) throw AskdayError("A group can have 3 of its own reactions.")
        if (list.none { it.equals(l, true) }) list.add(l)
    }

    fun toggleCustomVote(d: DB, key: String, label: String) {
        val me = d.me ?: return
        val m = d.customVotes.getOrPut(key) { mutableMapOf() }
        if (m[me] == label) m.remove(me) else m[me] = label
    }

    fun setVoiceReaction(d: DB, key: String, path: String) {
        val me = d.me ?: return
        d.voiceReactions.getOrPut(key) { mutableMapOf() }[me] = path
    }

    // ================================================================ wave 4

    class Insights(val answers: Int, val topMood: Mood?, val moodDays: Int, val topWord: String?, val topWordCount: Int, val topReactor: String?, val challenges: Int, val photos: Int)

    private val STOP = setOf(
        "the", "a", "an", "and", "or", "but", "to", "of", "in", "on", "at", "for", "with", "my", "me", "i", "is", "it", "was", "be", "you",
        "your", "that", "this", "so", "just", "not", "no", "yes", "are", "am", "we", "our", "they", "he", "she", "his", "her", "its", "im",
        "i’m", "it’s", "do", "did", "have", "had", "from", "by", "as", "all", "one", "like", "very", "really",
    )

    /** A summary of your last 7 days, worked out on the phone. */
    fun weeklyInsights(d: DB, now: Long): Insights {
        val me = d.me ?: return Insights(0, null, 0, null, 0, null, 0, 0)
        val since = now - 7 * DAY
        val days = (0..6).map { Social.dayKey(now - it * DAY) }.toSet()
        val texts = mutableListOf<String>()
        val reactors = HashMap<String, Int>()
        for ((_, list) in d.answers) for (a in list) if (a.uid == me && a.createdAt >= since) {
            texts += a.text
            for (r in a.reactions.keys) reactors[r] = (reactors[r] ?: 0) + 1
        }
        for ((k, v) in d.momentLog) if (k in days) texts += v
        val words = texts.flatMap { it.lowercase().split(Regex("[^\\p{L}’']+")) }.filter { it.length > 2 && it !in STOP }
        val top = words.groupingBy { it }.eachCount().maxByOrNull { it.value }
        val moods = d.moods.filterKeys { it in days }.values
        val topMood = moods.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
        return Insights(texts.size, topMood, moods.size, top?.key, top?.value ?: 0, reactors.maxByOrNull { it.value }?.key, d.challenges.keys.count { it in days }, d.photos.keys.count { it in days })
    }

    /** Packs ordered by how much your groups react to their questions: smarter question picking. */
    fun packsByEngagement(d: DB, groupId: String): List<String> {
        val score = HashMap<String, Int>()
        for (q in d.questions[groupId]?.values.orEmpty()) {
            if (q.qIndex < 0) continue
            val n = d.answers[qkey(groupId, q.id)].orEmpty().sumOf { it.reactions.size + it.replyCount }
            for (p in QUESTION_PACKS.getOrNull(q.qIndex).orEmpty()) score[p] = (score[p] ?: 0) + n
        }
        return score.entries.sortedByDescending { it.value }.map { it.key }
    }

    /** Suggestions while writing an Ask. */
    fun askHelper(question: String, type: AskType, options: List<String>): List<String> {
        val q = question.trim()
        val out = mutableListOf<String>()
        if (q.isEmpty()) return listOf("Try: “What’s the best thing that happened this week?”", "Try: “Who’s most likely to become famous?”", "Try a poll: “Pick tonight’s plan”")
        if (!q.endsWith("?")) out += "End it with a question mark so it reads like a question."
        if (q.length > 100) out += "Shorter Asks get more answers. Try trimming it."
        val lower = q.lowercase()
        if (type == AskType.text && (lower.startsWith("who") || lower.contains("most likely"))) out += "This would work well as a “Pick a member” Ask."
        if (type == AskType.text && (lower.contains(" or ") )) out += "Sounds like a choice: try “This or that”."
        if (type == AskType.text && (lower.startsWith("rate") || lower.contains("1 to 10") || lower.contains("how much"))) out += "Make it a 1–10 slider."
        if (type == AskType.poll && options.count { it.isNotBlank() } in 2..3 && options.none { it.contains("neither", true) || it.contains("none", true) }) out += "Add a “Neither” option for people who can’t decide."
        return out
    }
}
