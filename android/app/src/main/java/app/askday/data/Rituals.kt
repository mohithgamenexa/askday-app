package app.askday.data

// Wave 1: daily rituals and games. Everything rotates daily and is the same for everyone on a given day.

val PHOTO_PROMPTS = listOf(
    "Show your view right now", "What’s on your desk?", "Your lunch, no filters", "Something that made you smile",
    "The sky where you are", "Your shoes today", "Something blue", "Where you’re sitting", "Your favourite mug",
    "A tiny detail you love", "What you’re reading or watching", "The last thing you bought",
)

/** Five moods, from great to rough. */
enum class Mood(val label: String, val color: Long) {
    great("Great", 0xFF10B981), good("Good", 0xFF0A8A80), okay("Okay", 0xFFFFB020), low("Low", 0xFF0E7FA8), rough("Rough", 0xFFFF6B5B),
}

val CHALLENGES = listOf(
    "Compliment someone you don’t usually talk to", "Drink 8 glasses of water", "Call a grandparent or an old friend",
    "Take a 15-minute walk without your phone", "Learn one word in a new language", "Write down three things you’re grateful for",
    "Cook or try something new", "Tidy one drawer", "Send a voice note instead of a text", "Go to bed 30 minutes earlier",
    "Stretch for 5 minutes", "Say thank you to someone who helped you this week", "Read 10 pages of a book",
    "Take a photo of something beautiful", "Message a friend you haven’t talked to in a while",
)

/** Today's global this-or-that battle. */
class Battle(val left: String, val right: String)

val BATTLES = listOf(
    Battle("Tea", "Coffee"), Battle("Mountains", "Beaches"), Battle("Call", "Text"), Battle("Early bird", "Night owl"),
    Battle("Books", "Movies"), Battle("Sweet", "Spicy"), Battle("Cats", "Dogs"), Battle("Summer", "Winter"),
    Battle("Pizza", "Biryani"), Battle("City", "Village"), Battle("Plan everything", "Go with the flow"), Battle("Morning shower", "Night shower"),
)

val DOODLE_PROMPTS = listOf(
    "Draw your mood", "Draw your dream house", "Draw your favourite food", "Draw a cat (badly)", "Draw today’s weather",
    "Draw your best friend as an animal", "Draw what you’re doing this weekend", "Draw a monster", "Draw your phone",
)

object Rituals {
    /** The battle's results reveal at 9 PM. */
    const val BATTLE_REVEAL_HOUR = 21

    private fun pickFor(day: String, salt: Int, size: Int): Int {
        val r = Mulberry32(day.toInt() * 7 + salt)
        return (r.next() * size).toInt().coerceIn(0, size - 1)
    }

    fun photoPrompt(day: String) = PHOTO_PROMPTS[pickFor(day, 11, PHOTO_PROMPTS.size)]
    fun challenge(day: String) = CHALLENGES[pickFor(day, 23, CHALLENGES.size)]
    fun battle(day: String) = BATTLES[pickFor(day, 37, BATTLES.size)]
    fun doodlePrompt(day: String) = DOODLE_PROMPTS[pickFor(day, 51, DOODLE_PROMPTS.size)]

    // --------------------------------------------------------------- photo

    fun savePhoto(d: DB, back: String, front: String?, now: Long) {
        val day = Social.dayKey(now)
        d.photos[day] = DailyPhoto(photoPrompt(day), back, front, now)
        d.momentLog.putIfAbsent(day, "Photo: " + photoPrompt(day))
    }

    // ---------------------------------------------------------------- mood

    fun setMood(d: DB, mood: Mood, now: Long) {
        d.moods[Social.dayKey(now)] = mood.name
    }

    /** How your friends feel today. Sample moods until the online version connects. */
    fun groupMoods(d: DB, now: Long): Map<Mood, Int> {
        val day = Social.dayKey(now)
        val friends = d.people.keys.filter(Engine::isSample)
        val out = Mood.entries.associateWith { 0 }.toMutableMap()
        friends.forEachIndexed { i, _ -> val m = Mood.entries[pickFor(day, 100 + i, Mood.entries.size)]; out[m] = out.getValue(m) + 1 }
        d.moods[day]?.let { runCatching { Mood.valueOf(it) }.getOrNull() }?.let { out[it] = out.getValue(it) + 1 }
        return out
    }

    // ----------------------------------------------------------- challenge

    fun completeChallenge(d: DB, now: Long) {
        d.challenges[Social.dayKey(now)] = now
    }

    /** Days in a row you completed the daily challenge, counting today or yesterday. */
    fun challengeStreak(d: DB, now: Long): Int {
        var streak = 0
        var t = now
        if (d.challenges[Social.dayKey(t)] == null) t -= 86_400_000L
        while (d.challenges[Social.dayKey(t)] != null) { streak++; t -= 86_400_000L }
        return streak
    }

    // -------------------------------------------------------------- battle

    fun voteBattle(d: DB, side: Int, now: Long) {
        val day = Social.dayKey(now)
        if (d.battles.containsKey(day)) throw AskdayError("You’ve already picked a side today.")
        d.battles[day] = side.coerceIn(0, 1)
    }

    /** The share who picked the left option, out of 100. Revealed at 9 PM. */
    fun battleLeftPercent(day: String) = 30 + pickFor(day, 71, 41)

    // -------------------------------------------------------------- doodle

    fun saveDoodle(d: DB, path: String, now: Long) {
        d.doodles[Social.dayKey(now)] = path
    }

    // ------------------------------------------------------ guess the group

    /** True if your guess matched the group's top pick. Null if you didn't guess or it's too early to tell. */
    fun guessedRight(a: Ask, uid: String): Boolean? {
        val guess = a.responses[uid]?.guess ?: return null
        if (a.responses.size < 3) return null
        val counts = a.options.indices.map { i -> a.responses.values.count { it.option == i } }
        val top = counts.maxOrNull() ?: return null
        return counts.getOrNull(guess) == top
    }

    /** Points for correct guesses, per member, in a group: who knows the group best. */
    fun guessScores(d: DB, groupId: String): List<Pair<String, Int>> {
        val g = d.groups[groupId] ?: return emptyList()
        val asks = d.asks[groupId].orEmpty().filter { it.type == AskType.poll || it.type == AskType.thisOrThat }
        return g.memberIds.map { m -> m to asks.count { guessedRight(it, m) == true } }.filter { it.second > 0 }.sortedByDescending { it.second }
    }

    // -------------------------------------------------------- group trivia

    class TriviaQuestion(val prompt: String, val options: List<String>, val correct: Int)

    /** Questions about your own group, made from past answers: "What did Priya say to …?" */
    fun trivia(d: DB, groupId: String): List<TriviaQuestion> {
        val g = d.groups[groupId] ?: return emptyList()
        val out = mutableListOf<TriviaQuestion>()
        val qs = d.questions[groupId]?.values.orEmpty().filter { it.unlocked }.sortedByDescending { it.day }
        for (q in qs) {
            val answers = d.answers[qkey(groupId, q.id)].orEmpty().filter { it.text.isNotBlank() && it.uid != d.me }
            if (answers.size < 3) continue
            val target = answers[Math.floorMod(q.day * 7, answers.size)]
            val wrong = answers.filter { it.uid != target.uid }.take(2).map { it.text }
            val options = (wrong + target.text).sortedBy { (it.hashCode() xor q.day).toLong() }
            val name = d.people[target.uid]?.name ?: "A friend"
            out += TriviaQuestion("What did $name say to “${q.text}”", options, options.indexOf(target.text))
            if (out.size == 5) break
        }
        if (out.isEmpty() && g.memberIds.size > 1) return emptyList()
        return out
    }

    fun saveTriviaScore(d: DB, groupId: String, score: Int) {
        if (score > (d.trivia[groupId] ?: 0)) d.trivia[groupId] = score
    }
}
