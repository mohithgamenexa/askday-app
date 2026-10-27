package app.askday.data

import java.util.Calendar
import kotlin.random.Random

// The rules of Askday, run on the phone:
// - each group gets one question a day at its chosen time
// - answers unlock when everyone's in, or 2 hours after the drop
// - the streak grows when everyone answers; a freeze saves it once, otherwise it resets
// Sample friends stand in for real people until the app goes online.
//
// Every function edits the DB it's given. The Store hands it a copy and saves the result.

class AskdayError(message: String) : Exception(message)

object Engine {
    const val SEC = 1000L
    const val MIN = 60 * SEC
    const val UNLOCK_AFTER = 2 * 60 * MIN

    var random: Random = Random.Default

    private fun rand(min: Long, max: Long) = min + (random.nextDouble() * (max - min)).toLong()
    private fun <T> pick(xs: List<T>) = xs[random.nextInt(xs.size)]
    fun newId(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + java.lang.Long.toString(random.nextLong(1L shl 40), 36)
    fun friendId(name: String) = "friend_${name.lowercase()}"
    fun isSample(uid: String) = uid.startsWith("friend_")

    // ----------------------------------------------------------- scheduling

    fun nextSlotAfter(slot: DropSlot, after: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = after
            set(Calendar.HOUR_OF_DAY, slot.hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= after + MIN) c.add(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    // ----------------------------------------------------------------- core

    private fun dropQuestion(d: DB, g: GroupRec, now: Long) {
        val prev = g.currentId?.let { d.questions[g.id]?.get(it) }
        if (prev != null && !prev.unlocked) prev.unlocked = true
        if (prev != null && !prev.allAnswered && g.streak > 0) {
            if (g.freezes > 0) {
                g.freezes -= 1 // A streak freeze saves the day.
                prev.frozen = true
            } else {
                g.streak = 0
            }
        }

        val day = g.day + 1
        val qIndex = questionIndexFor(g.seed, day)
        val q = QuestionRec(
            id = "day-" + day.toString().padStart(5, '0'),
            day = day,
            text = DEMO_QUESTIONS[qIndex].text,
            qIndex = qIndex,
            dropAt = now,
            unlockAt = now + UNLOCK_AFTER,
        )
        d.questions.getOrPut(g.id) { mutableMapOf() }[q.id] = q
        d.answers[qkey(g.id, q.id)] = mutableListOf()
        d.comments[qkey(g.id, q.id)] = mutableListOf()

        // Some friends answer early, the rest wait until you've answered.
        val answerAt = mutableMapOf<String, Long?>()
        for (m in g.memberIds.filter(::isSample)) {
            answerAt[m] = if (random.nextDouble() < 0.55) now + rand(15 * SEC, 2 * MIN) else null
        }
        d.sim[qkey(g.id, q.id)] = Sim(answerAt)

        g.day = day
        g.currentId = q.id
        g.nextDropAt = nextSlotAfter(g.dropSlot, now)
        g.lastActivityAt = now
    }

    private fun friendAnswerText(q: QuestionRec, uid: String, g: GroupRec): String {
        val answers = DEMO_QUESTIONS.getOrNull(q.qIndex)?.answers ?: listOf("Good question. Still thinking.")
        val friends = g.memberIds.filter(::isSample)
        return answers[(friends.indexOf(uid) + q.day).mod(answers.size)]
    }

    private fun award(d: DB, badge: String, now: Long) {
        val p = d.profile ?: return
        if (p.badges.containsKey(badge)) return
        p.badges[badge] = now
        d.celebrations.add(Celebration(newId(), CelebrationKind.badge, badge = badge))
    }

    private fun addAnswer(d: DB, g: GroupRec, q: QuestionRec, uid: String, text: String, now: Long) {
        if (uid in q.answeredBy) return
        val key = qkey(g.id, q.id)
        val reactions = mutableMapOf<String, Reaction>()
        if (isSample(uid)) {
            for (o in g.memberIds) {
                if (isSample(o) && o != uid && o in q.answeredBy && random.nextDouble() < 0.35) reactions[o] = pick(Reaction.entries)
            }
        }
        val wasFirst = q.answeredBy.isEmpty()
        d.answers.getOrPut(key) { mutableListOf() }.add(Answer(uid, uid, text, now, reactions))
        q.answeredBy.add(uid)
        g.lastActivityAt = now
        refreshTop(d, g.id, q)

        val profile = d.profile
        if (uid == d.me && profile != null) {
            profile.stats.answers += 1
            award(d, "first_answer", now)
            if (wasFirst && g.memberIds.size > 1) award(d, "early_bird", now)
            val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            if (hour >= 23 || hour < 4) award(d, "night_owl", now)
        }

        val everyoneIn = g.memberIds.all { it in q.answeredBy }
        if (everyoneIn && !q.allAnswered) {
            q.allAnswered = true
            q.unlocked = true
            if (g.currentId == q.id) {
                g.streak += 1
                g.bestStreak = maxOf(g.bestStreak, g.streak)
                profile?.let { it.stats.bestStreak = maxOf(it.stats.bestStreak, g.streak) }
                STREAK_MILESTONES[g.streak]?.let { badge ->
                    d.celebrations.add(Celebration(newId(), CelebrationKind.streak, streak = g.streak, groupName = g.name))
                    award(d, badge, now)
                }
                if (g.streak % FREEZE_EVERY == 0 && g.freezes < MAX_FREEZES) {
                    g.freezes += 1
                    d.celebrations.add(Celebration(newId(), CelebrationKind.freeze, streak = g.streak, groupName = g.name))
                }
            }
            planReactions(d, g, q, now)
        }
    }

    /** After answers unlock, friends react to your answer and someone replies. */
    private fun planReactions(d: DB, g: GroupRec, q: QuestionRec, now: Long) {
        val me = d.me ?: return
        if (me !in q.answeredBy) return
        val sim = d.sim[qkey(g.id, q.id)] ?: return
        val friends = g.memberIds.filter(::isSample)
        for (f in friends) {
            if (random.nextDouble() < 0.7) {
                sim.events.add(SimEvent(now + rand(4 * SEC, 40 * SEC), SimKind.react, f, target = me, reaction = pick(Reaction.entries)))
            }
        }
        if (friends.isNotEmpty()) {
            sim.events.add(SimEvent(now + rand(8 * SEC, 45 * SEC), SimKind.reply, pick(friends), target = me, text = pick(SAMPLE_REPLIES)))
        }
    }

    private fun refreshTop(d: DB, groupId: String, q: QuestionRec) {
        val top = d.answers[qkey(groupId, q.id)].orEmpty().maxByOrNull { it.reactions.size }
        q.topAnswerId = if (top != null && top.reactions.isNotEmpty()) top.id else null
        q.topCount = top?.reactions?.size ?: 0
    }

    /** Moves time forward: drops, sample friends' answers, reactions and unlocks. Returns true if anything changed. */
    fun tick(d: DB, now: Long): Boolean {
        var dirty = false
        for (g in d.groups.values) {
            val next = g.nextDropAt
            if (next != null && next <= now) {
                dropQuestion(d, g, now)
                dirty = true
            }
            val q = g.currentId?.let { d.questions[g.id]?.get(it) } ?: continue
            val sim = d.sim[qkey(g.id, q.id)]
            if (sim != null) {
                val due = sim.answerAt.entries
                    .filter { (uid, at) -> at != null && at <= now && uid in g.memberIds && uid !in q.answeredBy }
                    .sortedBy { it.value }
                for ((uid, at) in due) {
                    addAnswer(d, g, q, uid, friendAnswerText(q, uid, g), at!!)
                    dirty = true
                }
                val ready = sim.events.filter { it.at <= now }
                if (ready.isNotEmpty()) {
                    sim.events.removeAll { it.at <= now }
                    ready.forEach { applyEvent(d, g, q, it) }
                    dirty = true
                }
            }
            if (!q.unlocked && now >= q.unlockAt) {
                q.unlocked = true
                dirty = true
            }
        }
        return dirty
    }

    private fun applyEvent(d: DB, g: GroupRec, q: QuestionRec, e: SimEvent) {
        val key = qkey(g.id, q.id)
        when (e.kind) {
            SimKind.react -> {
                val a = d.answers[key]?.firstOrNull { it.id == e.target }
                if (a != null && !a.reactions.containsKey(e.uid)) a.reactions[e.uid] = e.reaction ?: Reaction.love
                if (a != null && a.uid == d.me && a.reactions.size >= 3) award(d, "crowd_favorite", e.at)
                refreshTop(d, g.id, q)
            }
            SimKind.reply, SimKind.chat -> {
                val answerId = if (e.kind == SimKind.reply) e.target else null
                d.comments.getOrPut(key) { mutableListOf() }.add(Comment(newId(), e.uid, e.text ?: "", answerId, e.at))
                if (answerId != null) d.answers[key]?.firstOrNull { it.id == answerId }?.let { it.replyCount += 1 }
            }
        }
        g.lastActivityAt = e.at
    }

    // -------------------------------------------------------------- actions

    private fun requireMe(d: DB) = d.me ?: throw AskdayError("Start as a guest first.")

    fun createGuest(d: DB) {
        d.me = "me_" + newId()
    }

    fun saveProfile(d: DB, name: String, photo: String?, now: Long) {
        val me = requireMe(d)
        val p = d.profile
        if (p == null) d.profile = Profile(id = me, name = name, photo = photo, createdAt = now)
        else {
            p.name = name
            p.photo = photo
        }
    }

    fun createGroup(d: DB, name: String, slot: DropSlot, now: Long): String {
        val clean = name.trim()
        if (clean.isEmpty()) throw AskdayError("Give your group a name.")
        val me = requireMe(d)
        val id = newId()
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val names = SAMPLE_FRIENDS.shuffled(random).take(4)
        for (n in names) d.people.getOrPut(friendId(n)) { Profile(friendId(n), n, sample = true) }
        val g = GroupRec(
            id = id,
            name = clean,
            memberIds = (listOf(me) + names.map(::friendId)).toMutableList(),
            inviteCode = (1..6).map { alphabet[random.nextInt(alphabet.length)] }.joinToString(""),
            dropSlot = slot,
            seed = random.nextInt(Int.MAX_VALUE),
            createdAt = now,
            lastActivityAt = now,
        )
        d.groups[id] = g
        dropQuestion(d, g, now) // A new group gets its first question right away.
        return id
    }

    fun setGroupPhoto(d: DB, groupId: String, photo: String) {
        d.groups[groupId]?.photo = photo
    }

    fun leaveGroup(d: DB, groupId: String) {
        d.groups.remove(groupId)
        d.questions.remove(groupId)?.keys?.forEach { qid ->
            val k = qkey(groupId, qid)
            d.answers.remove(k)
            d.comments.remove(k)
            d.sim.remove(k)
        }
        d.seen.remove(groupId)
    }

    fun submitAnswer(d: DB, groupId: String, questionId: String, uid: String, text: String, now: Long) {
        val clean = text.trim().replace(Regex("\\s+"), " ").take(280)
        if (clean.isEmpty()) throw AskdayError("Write an answer first.")
        val g = d.groups[groupId] ?: throw AskdayError("This group isn’t available anymore.")
        val q = d.questions[groupId]?.get(questionId) ?: throw AskdayError("This question isn’t available anymore.")
        if (g.currentId != q.id) throw AskdayError("You can only answer today’s question.")
        addAnswer(d, g, q, uid, clean, now)
        // Friends who were waiting on you answer over the next minute or so.
        val sim = d.sim[qkey(groupId, questionId)] ?: return
        var at = now
        for (m in sim.answerAt.keys.toList()) {
            if (m in q.answeredBy) continue
            at += rand(10 * SEC, 35 * SEC)
            val planned = sim.answerAt[m]
            sim.answerAt[m] = if (planned == null) at else minOf(planned, at)
        }
    }

    /** Sets your reaction on an answer. Tapping the same reaction again removes it. */
    fun toggleReaction(d: DB, groupId: String, questionId: String, answerId: String, uid: String, kind: Reaction): Boolean {
        val q = d.questions[groupId]?.get(questionId) ?: return false
        val a = d.answers[qkey(groupId, questionId)]?.firstOrNull { it.id == answerId } ?: return false
        if (a.reactions[uid] == kind) a.reactions.remove(uid) else a.reactions[uid] = kind
        refreshTop(d, groupId, q)
        return true
    }

    fun postComment(d: DB, groupId: String, questionId: String, uid: String, text: String, answerId: String?, now: Long) {
        val clean = text.trim().take(280)
        if (clean.isEmpty()) return
        val key = qkey(groupId, questionId)
        d.comments.getOrPut(key) { mutableListOf() }.add(Comment(newId(), uid, clean, answerId, now))
        if (answerId != null) d.answers[key]?.firstOrNull { it.id == answerId }?.let { it.replyCount += 1 }
        // Sometimes a friend writes back.
        val friends = d.groups[groupId]?.memberIds?.filter(::isSample).orEmpty()
        val sim = d.sim[key]
        if (sim != null && friends.isNotEmpty() && random.nextDouble() < 0.6) {
            val replier = if (answerId != null && isSample(answerId)) answerId else pick(friends)
            sim.events.add(
                SimEvent(
                    now + rand(5 * SEC, 18 * SEC),
                    if (answerId != null) SimKind.reply else SimKind.chat,
                    replier,
                    target = answerId,
                    text = pick(SAMPLE_REPLIES),
                ),
            )
        }
    }

    fun nudge(d: DB, groupId: String, target: String, now: Long) {
        val g = d.groups[groupId]
        val q = g?.currentId?.let { d.questions[groupId]?.get(it) }
        if (g == null || q == null) throw AskdayError("There’s no question to answer yet.")
        if (target in q.answeredBy) throw AskdayError("They’ve already answered.")
        if (g.nudged[target] == q.id) throw AskdayError("You’ve already nudged them today.")
        g.nudged[target] = q.id
        // A nudge works: they answer within a few seconds.
        d.sim[qkey(groupId, q.id)]?.answerAt?.set(target, now + rand(4 * SEC, 9 * SEC))
    }

    fun markSeen(d: DB, groupId: String, questionId: String, count: Int): Boolean {
        val s = d.seen[groupId]
        if (s != null && s.questionId == questionId && s.count == count) return false
        d.seen[groupId] = Seen(questionId, count)
        return true
    }

    /** Demo shortcut: release the next question now instead of waiting for tomorrow. */
    fun dropNextNow(d: DB, groupId: String, now: Long) {
        d.groups[groupId]?.let { dropQuestion(d, it, now) }
    }

    fun dismissCelebration(d: DB, id: String) {
        d.celebrations.removeAll { it.id == id }
    }

    /** Saves a "Who said it?" result, keeping the best score. */
    fun saveGameScore(d: DB, groupId: String, questionId: String, score: Int, total: Int, now: Long) {
        val key = qkey(groupId, questionId)
        val prev = d.games[key]
        if (prev == null || score > prev.score) d.games[key] = GameScore(score, total)
        if (total >= 3 && score == total) award(d, "mind_reader", now)
    }
}
