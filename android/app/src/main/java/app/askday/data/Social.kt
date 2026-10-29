package app.askday.data

import java.util.Calendar

// Phase A: the global Askday moment, member Asks and onboarding, run on the phone.
// Sample friends stand in for real people until the online version connects.

/** Questions sample friends ask their groups. */
class SampleAsk(val type: AskType, val question: String, val options: List<String> = emptyList())

val SAMPLE_ASKS = listOf(
    SampleAsk(AskType.poll, "Best thing in the canteen right now?", listOf("Vada pav", "Cold coffee", "Maggi, obviously", "Nothing. It’s a crime scene.")),
    SampleAsk(AskType.thisOrThat, "Mountains or beaches?", listOf("Mountains", "Beaches")),
    SampleAsk(AskType.slider, "Rate your week so far, 1 to 10"),
    SampleAsk(AskType.member, "Who’s most likely to be late tonight?"),
    SampleAsk(AskType.poll, "Pick tonight’s movie genre", listOf("Comedy", "Horror", "Romance", "Action")),
    SampleAsk(AskType.thisOrThat, "Tea or coffee?", listOf("Tea", "Coffee")),
    SampleAsk(AskType.slider, "How hungry are you right now?"),
    SampleAsk(AskType.member, "Who gives the best advice?"),
    SampleAsk(AskType.thisOrThat, "Call or text?", listOf("Call", "Text")),
    SampleAsk(AskType.poll, "Weekend plan?", listOf("Sleep", "Party", "Trip", "Netflix")),
    SampleAsk(AskType.member, "Who would survive a zombie apocalypse?"),
    SampleAsk(AskType.slider, "How ready are you for Monday?"),
    SampleAsk(AskType.thisOrThat, "Early bird or night owl?", listOf("Early bird", "Night owl")),
    SampleAsk(AskType.poll, "Best festival food?", listOf("Jalebi", "Modak", "Biryani", "Pani puri")),
    SampleAsk(AskType.member, "Who here is the funniest?"),
    SampleAsk(AskType.slider, "Rate today’s weather"),
)

/** What sample friends write back to text Asks. */
val SAMPLE_TEXT_RESPONSES = listOf(
    "Honestly? Same.",
    "Okay this is a great question.",
    "Asking the real questions today.",
    "Can I answer this in person?",
    "No comment. Okay, one comment: yes.",
    "Ha, I knew someone would ask this.",
)

object Social {
    private const val SEC = 1000L
    private const val MIN = 60 * SEC
    private const val HOUR = 60 * MIN
    const val ASK_LIFETIME = 24 * HOUR
    /** Answer within 5 minutes of the drop to be "on time". Later answers are marked late. */
    const val ON_TIME_WINDOW = 5 * MIN
    /** How far back you can still answer. */
    const val CATCH_UP_DAYS = 7
    /** Free Asks per person per day (Askday+ would raise this). */
    const val FREE_ASKS_PER_DAY = 3

    private fun rand(min: Long, max: Long) = min + (Engine.random.nextDouble() * (max - min)).toLong()

    fun dayKey(at: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = at }
        return "%04d%02d%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** Every day the moment drops at a different time between 10 AM and 9 PM, the same for everyone. */
    fun momentDropFor(at: Long): Long {
        val key = dayKey(at)
        val r = Mulberry32(key.toInt() * 7919)
        val c = Calendar.getInstance().apply {
            timeInMillis = at
            set(Calendar.HOUR_OF_DAY, 10 + (r.next() * 11).toInt())
            set(Calendar.MINUTE, (r.next() * 60).toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun momentIndexFor(key: String) = questionIndexFor(key.toInt() * 31 + 17, 1)

    /** Your friends for the moment: every sample friend you have, or all of them if you have none yet. */
    private fun friendsOf(d: DB): List<String> {
        val ids = d.people.keys.filter(Engine::isSample)
        if (ids.isNotEmpty()) return ids
        for (n in SAMPLE_FRIENDS) d.people.getOrPut(Engine.friendId(n)) { Profile(Engine.friendId(n), n, sample = true) }
        return SAMPLE_FRIENDS.map(Engine::friendId)
    }

    private fun dropMoment(d: DB, now: Long, dropAt: Long) {
        val key = dayKey(dropAt)
        val qIndex = momentIndexFor(key)
        val m = Moment(key, DEMO_QUESTIONS[qIndex].text, qIndex, dropAt, dropAt + ON_TIME_WINDOW)
        // Five friends answer today: some on time, some late.
        val friends = friendsOf(d).shuffled(Engine.random).take(5)
        // About half answer within the 5-minute window, the rest later (late).
        for (f in friends) m.plan[f] = maxOf(now, dropAt) + if (Engine.random.nextBoolean()) rand(20 * SEC, 4 * MIN) else rand(6 * MIN, 4 * HOUR)
        d.moment?.let { old ->
            d.pastMoments.add(0, old)
            while (d.pastMoments.size > CATCH_UP_DAYS) d.pastMoments.removeAt(d.pastMoments.lastIndex)
        }
        d.moment = m
        d.momentQuestions[key] = m.text
    }

    private fun momentFriendText(m: Moment, uid: String): String {
        val answers = DEMO_QUESTIONS[m.qIndex].answers
        val order = m.plan.keys.sorted()
        return answers[order.indexOf(uid).coerceAtLeast(0) % answers.size]
    }

    /** Moves the moment and the Asks forward. Returns true if anything changed. */
    fun tick(d: DB, now: Long): Boolean {
        var dirty = false
        // People who finished onboarding before starter groups existed get them once.
        if (d.onboarded && !d.starterDone && d.me != null && d.profile != null) { createStarterGroups(d, now); dirty = true }
        val todayKey = dayKey(now)
        val dropAt = momentDropFor(now)
        val m = d.moment
        when {
            // The very first moment drops right away, so new people start with a question.
            m == null -> { dropMoment(d, now, minOf(now, dropAt)); dirty = true }
            m.dayKey != todayKey && now >= dropAt -> { dropMoment(d, now, dropAt); dirty = true }
        }
        d.moment?.let { mo ->
            val due = mo.plan.entries.filter { (uid, at) -> at != null && at <= now && mo.answers.none { it.uid == uid } }
            for ((uid, at) in due.sortedBy { it.value }) {
                mo.answers.add(MomentAnswer(uid, momentFriendText(mo, uid), at!!, at > mo.onTimeUntil))
                dirty = true
            }
        }
        for (g in d.groups.values) {
            if (d.askDays[g.id] != todayKey) {
                postFriendAsks(d, g, now)
                d.askDays[g.id] = todayKey
                dirty = true
            }
            for (a in d.asks[g.id].orEmpty()) {
                val due = a.plan.filter { (uid, at) -> at <= now && !a.responses.containsKey(uid) }
                for ((uid, at) in due) {
                    a.responses[uid] = friendResponse(a, g, uid, at)
                    dirty = true
                }
                if (due.isNotEmpty() && a.uid == d.me && fullyAnsweredAsks(d) >= 10) Engine.award(d, "question_master", now)
            }
            if (d.asks[g.id]?.removeAll { now > it.expiresAt + ASK_LIFETIME } == true) dirty = true
        }
        if (Waves.tickDuos(d, now)) dirty = true
        if (Waves.applyBirthdays(d, now)) dirty = true
        return dirty
    }

    // ------------------------------------------------------------- moment

    fun momentState(d: DB, now: Long): Moment? = d.moment

    /** Today's moment, or one of the last 7 days by its day key. */
    fun momentFor(d: DB, dayKey: String?): Moment? =
        if (dayKey == null || dayKey == d.moment?.dayKey) d.moment else d.pastMoments.firstOrNull { it.dayKey == dayKey }

    fun answerMoment(d: DB, text: String, now: Long, photo: String? = null, voice: String? = null, voiceMs: Long = 0, dayKey: String? = null) {
        val me = d.me ?: throw AskdayError("Sign in first.")
        val m = momentFor(d, dayKey) ?: throw AskdayError("That question isn’t available anymore.")
        val clean = text.trim().replace(Regex("\\s+"), " ").take(280)
        if (clean.isEmpty() && photo == null && voice == null) throw AskdayError("Write an answer, or add a photo or voice note.")
        if (m.answers.any { it.uid == me }) throw AskdayError("You’ve already answered today.")
        val mine = MomentAnswer(me, clean, now, now > m.onTimeUntil, photo = photo, voice = voice, voiceMs = voiceMs)
        // Friends who've already answered react to yours.
        for (f in m.answers.map { it.uid }) {
            if (Engine.random.nextDouble() < 0.5) mine.reactions[f] = Reaction.entries[Engine.random.nextInt(Reaction.entries.size)]
        }
        m.answers.add(mine)
        d.momentLog[m.dayKey] = clean.ifEmpty { if (photo != null) "A photo" else "A voice note" }
        d.profile?.let { it.stats.answers += 1 }
    }

    fun reactMoment(d: DB, answerUid: String, kind: Reaction, dayKey: String? = null): Boolean {
        val me = d.me ?: return false
        val a = momentFor(d, dayKey)?.answers?.firstOrNull { it.uid == answerUid } ?: return false
        if (a.reactions[me] == kind) a.reactions.remove(me) else a.reactions[me] = kind
        return true
    }

    // --------------------------------------------------------------- Asks

    private fun postFriendAsks(d: DB, g: GroupRec, now: Long) {
        val friends = g.memberIds.filter(Engine::isSample)
        if (friends.isEmpty()) return
        val list = d.asks.getOrPut(g.id) { mutableListOf() }
        val picks = SAMPLE_ASKS.shuffled(Engine.random).take(3)
        picks.forEachIndexed { i, s ->
            val author = friends[i % friends.size]
            val created = now - rand(10 * MIN, 3 * HOUR)
            val ask = Ask(Engine.newId(), g.id, author, s.type, s.question, s.options, created, created + ASK_LIFETIME)
            for (f in friends) if (f != author && Engine.random.nextDouble() < 0.7) ask.plan[f] = now + rand(-2 * HOUR, 2 * HOUR)
            list.add(ask)
        }
    }

    private fun friendResponse(a: Ask, g: GroupRec, uid: String, at: Long): AskResponse = when (a.type) {
        AskType.poll, AskType.thisOrThat -> {
            val n = a.options.size.coerceAtLeast(1)
            AskResponse(at, option = Engine.random.nextInt(n), guess = Engine.random.nextInt(n))
        }
        AskType.slider -> AskResponse(at, value = 3 + Engine.random.nextInt(8))
        AskType.member -> AskResponse(at, member = g.memberIds.filter { it != uid }.let { it[Engine.random.nextInt(it.size)] })
        AskType.text -> AskResponse(at, text = SAMPLE_TEXT_RESPONSES[Engine.random.nextInt(SAMPLE_TEXT_RESPONSES.size)])
    }

    fun asksLeftToday(d: DB, groupId: String, now: Long): Int {
        val me = d.me ?: return 0
        val today = dayKey(now)
        val used = d.asks[groupId].orEmpty().count { it.uid == me && dayKey(it.createdAt) == today }
        return (FREE_ASKS_PER_DAY - used).coerceAtLeast(0)
    }

    fun createAsk(d: DB, groupId: String, type: AskType, question: String, options: List<String>, now: Long): String {
        val me = d.me ?: throw AskdayError("Sign in first.")
        val g = d.groups[groupId] ?: throw AskdayError("This group isn’t available anymore.")
        val q = question.trim()
        if (q.isEmpty()) throw AskdayError("Write your question first.")
        val opts = options.map { it.trim() }.filter { it.isNotEmpty() }
        if (type == AskType.poll && opts.size < 2) throw AskdayError("Add at least two options.")
        if (type == AskType.thisOrThat && opts.size != 2) throw AskdayError("Add both choices.")
        if (asksLeftToday(d, groupId, now) == 0) throw AskdayError("You’ve used today’s $FREE_ASKS_PER_DAY Asks. More tomorrow.")
        val ask = Ask(Engine.newId(), groupId, me, type, q.take(140), if (type == AskType.poll || type == AskType.thisOrThat) opts.take(4) else emptyList(), now, now + ASK_LIFETIME)
        // Everyone else answers your Ask over the next few minutes.
        for (f in g.memberIds.filter { it != me }) ask.plan[f] = now + rand(15 * SEC, 4 * MIN)
        d.asks.getOrPut(groupId) { mutableListOf() }.add(ask)
        return ask.id
    }

    fun respondAsk(d: DB, groupId: String, askId: String, response: AskResponse) {
        val me = d.me ?: throw AskdayError("Sign in first.")
        val a = d.asks[groupId]?.firstOrNull { it.id == askId } ?: throw AskdayError("This Ask has expired.")
        if (a.uid == me) throw AskdayError("You can’t answer your own Ask.")
        a.responses[me] = response
    }

    fun skipAsk(d: DB, groupId: String, askId: String) {
        val me = d.me ?: return
        d.asks[groupId]?.firstOrNull { it.id == askId }?.let { if (me !in it.skippedBy) it.skippedBy.add(me) }
    }

    /** Asks you haven't answered or skipped yet, newest first. */
    fun waitingAsks(d: DB, groupId: String, now: Long): List<Ask> {
        val me = d.me ?: return emptyList()
        return d.asks[groupId].orEmpty()
            .filter { it.uid != me && now < it.expiresAt && !it.responses.containsKey(me) && me !in it.skippedBy }
            .sortedByDescending { it.createdAt }
    }

    fun waitingAsksTotal(d: DB, now: Long) = d.groups.keys.sumOf { waitingAsks(d, it, now).size }

    /** The group's most-answered live Ask today. */
    fun askOfTheDay(d: DB, groupId: String, now: Long): Ask? =
        d.asks[groupId].orEmpty().filter { now < it.expiresAt && it.responses.size >= 2 }.maxByOrNull { it.responses.size }

    /** How many of your Asks everyone in the group answered. */
    fun fullyAnsweredAsks(d: DB): Int {
        val me = d.me ?: return 0
        return d.asks.entries.sumOf { (gid, list) ->
            val size = d.groups[gid]?.memberIds?.size ?: return@sumOf 0
            list.count { it.uid == me && it.responses.size >= size - 1 }
        }
    }

    class Leader(val uid: String, val value: Int)
    class Board(val consistent: List<Leader>, val funniest: List<Leader>, val earliest: List<Leader>)

    /** The group's last 30 days: most answers, most "haha" reactions received, and most often first to answer. */
    fun leaderboard(d: DB, groupId: String, now: Long): Board {
        val g = d.groups[groupId] ?: return Board(emptyList(), emptyList(), emptyList())
        val since = now - 30 * 86_400_000L
        val qs = d.questions[groupId]?.values.orEmpty().filter { it.dropAt >= since }
        val answers = HashMap<String, Int>()
        val haha = HashMap<String, Int>()
        val first = HashMap<String, Int>()
        for (q in qs) {
            q.answeredBy.firstOrNull()?.let { first[it] = (first[it] ?: 0) + 1 }
            for (a in d.answers[qkey(groupId, q.id)].orEmpty()) {
                answers[a.uid] = (answers[a.uid] ?: 0) + 1
                haha[a.uid] = (haha[a.uid] ?: 0) + a.reactions.values.count { it == Reaction.haha }
            }
        }
        fun top(m: Map<String, Int>) = g.memberIds.map { Leader(it, m[it] ?: 0) }.filter { it.value > 0 }.sortedByDescending { it.value }.take(5)
        return Board(top(answers), top(haha), top(first))
    }

    // --------------------------------------------------------- onboarding

    /** Saves the answer written before signing up. */
    fun setPendingAnswer(d: DB, text: String) {
        d.pendingAnswer = text.trim().take(280).ifEmpty { null }
    }

    fun saveDetails(d: DB, username: String, birthday: String) {
        val u = username.trim().removePrefix("@").lowercase()
        if (!Regex("^[a-z0-9._]{3,20}$").matches(u)) throw AskdayError("Usernames are 3–20 letters, numbers, dots or underscores.")
        val parts = birthday.split("/").map { it.trim().toIntOrNull() }
        if (parts.size != 3 || parts.any { it == null }) throw AskdayError("Enter your birthday as DD / MM / YYYY.")
        val (day, month, year) = parts.map { it!! }
        if (day !in 1..31 || month !in 1..12 || year !in 1900..2100) throw AskdayError("That birthday doesn’t look right.")
        val now = Calendar.getInstance()
        var age = now.get(Calendar.YEAR) - year
        if (now.get(Calendar.MONTH) + 1 < month || (now.get(Calendar.MONTH) + 1 == month && now.get(Calendar.DAY_OF_MONTH) < day)) age--
        if (age < 13) throw AskdayError("You need to be 13 or older to use Askday.")
        val p = d.profile ?: throw AskdayError("Set your name first.")
        p.username = u
        p.birthday = "%02d/%02d/%04d".format(day, month, year)
    }

    /** Finishes onboarding, joins the starter groups, and posts the answer written before signing up. */
    fun finishOnboarding(d: DB, packs: List<String>, now: Long) {
        d.profile?.packs = packs.toMutableList()
        d.onboarded = true
        createStarterGroups(d, now)
        val pending = d.pendingAnswer
        d.pendingAnswer = null
        if (pending != null && d.moment != null && d.moment!!.answers.none { it.uid == d.me }) {
            runCatching { answerMoment(d, pending, now) }
        }
    }

    // ------------------------------------------------ starter groups

    /** Two groups with sample members, so a new person has something to answer right away. */
    fun createStarterGroups(d: DB, now: Long) {
        if (d.starterDone || d.groups.values.any { it.starter } || d.me == null) return
        d.starterDone = true
        val lounge = Engine.createGroup(d, "Askday Lounge", DropSlot.evening, now, friends = 12)
        val late = Engine.createGroup(d, "Late Night Talks", DropSlot.night, now, friends = 4)
        d.groups[lounge]?.starter = true
        d.groups[late]?.starter = true
    }

    // ------------------------------------------------ quick questions

    class QuickAnswer(val name: String, val place: String, val text: String)

    /** Five global questions a day, open all day, different from the Askday moment. */
    fun quickQuestions(d: DB, now: Long): List<Int> {
        val key = dayKey(now)
        val moment = d.moment?.qIndex
        val r = Mulberry32(key.toInt() * 104_729)
        val all = DEMO_QUESTIONS.indices.filter { it != moment }.toMutableList()
        for (i in all.size - 1 downTo 1) {
            val j = (r.next() * (i + 1)).toInt()
            val t = all[i]; all[i] = all[j]; all[j] = t
        }
        return all.take(5)
    }

    fun quickKey(now: Long, qIndex: Int) = dayKey(now) + "/" + qIndex

    fun answerQuick(d: DB, qIndex: Int, text: String, now: Long) {
        val clean = text.trim().take(200)
        if (clean.isEmpty()) throw AskdayError("Write an answer first.")
        d.quick[quickKey(now, qIndex)] = clean
    }

    private val WORLD = listOf("Lucía" to "Madrid", "Takumi" to "Osaka", "Nisha" to "Pune", "Ola" to "Accra", "Liam" to "Dublin", "Mei" to "Taipei", "Omar" to "Cairo", "Ana" to "São Paulo")

    /** What people around the world said. Sample answers until the online version connects. */
    fun quickAnswers(qIndex: Int, now: Long): List<QuickAnswer> {
        val answers = DEMO_QUESTIONS[qIndex].answers
        val r = Mulberry32(dayKey(now).toInt() + qIndex * 31)
        val people = WORLD.sortedBy { r.next() }
        return answers.take(4).mapIndexed { i, t -> QuickAnswer(people[i].first, people[i].second, t) }
    }

    // ----------------------------------------------------------- memories

    /** Day keys ("20261028") on which you answered anything: the moment or a group question. */
    fun answeredDays(d: DB): Set<String> {
        val me = d.me ?: return emptySet()
        val days = d.momentLog.keys.toMutableSet()
        for ((_, list) in d.answers) for (a in list) if (a.uid == me) days += dayKey(a.createdAt)
        return days
    }
}
