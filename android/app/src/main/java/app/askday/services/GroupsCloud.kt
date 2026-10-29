package app.askday.services

import android.net.Uri
import app.askday.data.Answer
import app.askday.data.Comment
import app.askday.data.DB
import app.askday.data.DropSlot
import app.askday.data.GroupRec
import app.askday.data.Profile
import app.askday.data.QuestionRec
import app.askday.data.Reaction
import app.askday.data.Store
import app.askday.data.qkey
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File

/**
 * B3: real groups. Listens to your groups on the server and mirrors them into the phone's data,
 * so every existing screen works unchanged. Your actions go to the server; the server decides
 * drops, unlocks and streaks.
 *
 * groups/{gid}                                    (members only; written by Cloud Functions)
 * groups/{gid}/questions/{qid}                    (members only; written by Cloud Functions)
 * groups/{gid}/questions/{qid}/answers/{uid}      (yours, and everyone's once you answered and it unlocked)
 * groups/{gid}/questions/{qid}/comments/{cid}     (after unlock)
 */
object GroupsCloud {
    private val fs get() = FirebaseFirestore.getInstance()
    private val fn get() = FirebaseFunctions.getInstance("asia-south1")

    private var uid: String? = null
    private var groupsReg: ListenerRegistration? = null
    private val questionRegs = mutableMapOf<String, ListenerRegistration>()
    /** "gid/qid" → (mode, listeners); mode is "own" before unlock and "all" after. */
    private val answerRegs = mutableMapOf<String, Pair<String, List<ListenerRegistration>>>()
    private val fetchedPeople = mutableSetOf<String>()

    private const val WATCH_QUESTIONS = 7L

    // ------------------------------------------------------------ lifecycle

    fun start(me: String) {
        if (!Services.firebaseReady || uid == me) return
        stop()
        uid = me
        groupsReg = fs.collection("groups").whereArrayContains("memberIds", me).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener Services.recordError(err)
            snap ?: return@addSnapshotListener
            val ids = snap.documents.map { it.id }.toSet()
            Store.update { d ->
                // Groups you've left (or that were deleted) disappear from the phone too.
                d.groups.values.filter { it.cloud && it.id !in ids }.map { it.id }.forEach { gone ->
                    d.groups.remove(gone); d.questions.remove(gone)
                }
                for (doc in snap.documents) mirrorGroup(d, doc)
                true
            }
            for (doc in snap.documents) {
                watchQuestions(doc.id)
                (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>()?.forEach(::fetchPerson)
            }
            (questionRegs.keys - ids).forEach { questionRegs.remove(it)?.remove() }
        }
    }

    fun stop() {
        groupsReg?.remove(); groupsReg = null
        questionRegs.values.forEach { it.remove() }; questionRegs.clear()
        answerRegs.values.forEach { (_, regs) -> regs.forEach { it.remove() } }; answerRegs.clear()
        fetchedPeople.clear()
        uid = null
    }

    // ------------------------------------------------------------- mirroring

    private fun mirrorGroup(d: DB, doc: DocumentSnapshot) {
        val old = d.groups[doc.id]
        d.groups[doc.id] = GroupRec(
            id = doc.id,
            name = doc.getString("name") ?: "Group",
            memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(),
            inviteCode = doc.getString("inviteCode") ?: "",
            dropSlot = runCatching { DropSlot.valueOf(doc.getString("slot") ?: "evening") }.getOrDefault(DropSlot.evening),
            seed = (doc.getLong("seed") ?: 1L).toInt(),
            day = (doc.getLong("day") ?: 0L).toInt(),
            streak = (doc.getLong("streak") ?: 0L).toInt(),
            bestStreak = (doc.getLong("bestStreak") ?: 0L).toInt(),
            currentId = doc.getString("currentId"),
            nextDropAt = doc.getLong("nextDropAt"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            lastActivityAt = doc.getLong("lastActivityAt") ?: 0L,
            nudged = old?.nudged ?: mutableMapOf(),
            freezes = (doc.getLong("freezes") ?: 0L).toInt(),
            nextQuestion = doc.getString("nextQuestion"),
            nextQuestionBy = doc.getString("nextQuestionBy"),
            cloud = true,
        )
        // Your best streak across groups shows on your profile.
        d.profile?.let { p -> p.stats.bestStreak = maxOf(p.stats.bestStreak, (doc.getLong("bestStreak") ?: 0L).toInt()) }
    }

    private fun fetchPerson(id: String) {
        if (id == uid || !fetchedPeople.add(id)) return
        fs.collection("users").document(id).get().addOnSuccessListener { u ->
            if (!u.exists()) return@addOnSuccessListener
            Store.update { d ->
                val p = d.people[id]
                if (p == null) d.people[id] = Profile(id, u.getString("name") ?: "Askday user", photo = u.getString("photoUrl"), username = u.getString("username"), createdAt = u.getLong("createdAt") ?: 0L)
                else { p.name = u.getString("name") ?: p.name; p.photo = u.getString("photoUrl") ?: p.photo; p.username = u.getString("username") }
                true
            }
        }
    }

    private fun watchQuestions(gid: String) {
        if (questionRegs.containsKey(gid)) return
        questionRegs[gid] = fs.collection("groups").document(gid).collection("questions")
            .orderBy("day", Query.Direction.DESCENDING).limit(WATCH_QUESTIONS)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener Services.recordError(err)
                snap ?: return@addSnapshotListener
                Store.update { d ->
                    val map = d.questions.getOrPut(gid) { mutableMapOf() }
                    for (q in snap.documents) {
                        val old = map[q.id]
                        map[q.id] = QuestionRec(
                            id = q.id,
                            day = (q.getLong("day") ?: 0L).toInt(),
                            text = q.getString("text") ?: "",
                            qIndex = (q.getLong("qIndex") ?: -1L).toInt(),
                            dropAt = q.getLong("dropAt") ?: 0L,
                            unlockAt = q.getLong("unlockAt") ?: 0L,
                            unlocked = q.getBoolean("unlocked") == true,
                            allAnswered = q.getBoolean("allAnswered") == true,
                            answeredBy = (q.get("answeredBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(),
                            topAnswerId = old?.topAnswerId,
                            topCount = old?.topCount ?: 0,
                            frozen = q.getBoolean("frozen") == true,
                            askedBy = q.getString("askedBy"),
                        )
                    }
                    true
                }
                for (q in snap.documents) watchAnswers(gid, q.id, q.getBoolean("unlocked") == true, (q.get("answeredBy") as? List<*>)?.contains(uid) == true)
            }
    }

    /** Before you can see everyone's answers, only your own is loaded; after, all of them and the replies. */
    private fun watchAnswers(gid: String, qid: String, unlocked: Boolean, iAnswered: Boolean) {
        val me = uid ?: return
        val key = "$gid/$qid"
        val mode = if (unlocked && iAnswered) "all" else "own"
        if (answerRegs[key]?.first == mode) return
        answerRegs.remove(key)?.second?.forEach { it.remove() }
        val qref = fs.collection("groups").document(gid).collection("questions").document(qid)
        val regs = mutableListOf<ListenerRegistration>()
        if (mode == "own") {
            regs += qref.collection("answers").document(me).addSnapshotListener { doc, _ ->
                val a = doc?.takeIf { it.exists() }?.let(::toAnswer) ?: return@addSnapshotListener
                Store.update { d ->
                    val list = d.answers.getOrPut(qkey(gid, qid)) { mutableListOf() }
                    list.removeAll { it.uid == me }
                    list.add(a)
                    true
                }
            }
        } else {
            regs += qref.collection("answers").addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener Services.recordError(err)
                snap ?: return@addSnapshotListener
                val answers = snap.documents.map(::toAnswer).sortedBy { it.createdAt }
                Store.update { d ->
                    val replies = d.comments[qkey(gid, qid)].orEmpty()
                    answers.forEach { a -> a.replyCount = replies.count { it.answerId == a.id } }
                    d.answers[qkey(gid, qid)] = answers.toMutableList()
                    d.questions[gid]?.get(qid)?.let { q ->
                        val top = answers.maxByOrNull { it.reactions.size }
                        if (top != null && top.reactions.size >= 2) { q.topAnswerId = top.id; q.topCount = top.reactions.size }
                    }
                    true
                }
            }
            regs += qref.collection("comments").orderBy("createdAt").addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                val list = snap.documents.map { c ->
                    Comment(c.id, c.getString("uid") ?: "", c.getString("text") ?: "", c.getString("replyTo"), c.getLong("createdAt") ?: 0L)
                }
                Store.update { d ->
                    d.comments[qkey(gid, qid)] = list.toMutableList()
                    d.answers[qkey(gid, qid)]?.forEach { a -> a.replyCount = list.count { it.answerId == a.id } }
                    true
                }
            }
        }
        answerRegs[key] = mode to regs
    }

    private fun toAnswer(doc: DocumentSnapshot): Answer {
        val reactions = (doc.get("reactions") as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
            val r = runCatching { Reaction.valueOf(v as String) }.getOrNull()
            if (k is String && r != null) k to r else null
        }.toMap().toMutableMap()
        return Answer(
            id = doc.id, uid = doc.id, text = doc.getString("text") ?: "", createdAt = doc.getLong("createdAt") ?: 0L,
            reactions = reactions, photo = doc.getString("photoUrl"), voice = doc.getString("voiceUrl"), voiceMs = doc.getLong("voiceMs") ?: 0L,
        )
    }

    // ---------------------------------------------------------------- actions

    private fun friendly(e: Exception): String =
        (e as? FirebaseFunctionsException)?.message?.takeIf { it.isNotBlank() && e.code != FirebaseFunctionsException.Code.INTERNAL }
            ?: "Something went wrong. Check your connection and try again."

    /** Creates a real group on the server. Returns its id and invite code. */
    fun createGroup(name: String, slot: DropSlot, onDone: (gid: String?, code: String?, error: String?) -> Unit) {
        fn.getHttpsCallable("createGroup").call(mapOf("name" to name, "slot" to slot.name))
            .addOnSuccessListener { r ->
                val m = r.getData() as? Map<*, *>
                Services.log("group_created", "kind" to "real", "drop_slot" to slot.name)
                onDone(m?.get("gid") as? String, m?.get("code") as? String, null)
            }
            .addOnFailureListener { onDone(null, null, friendly(it)) }
    }

    fun joinGroup(code: String, onDone: (gid: String?, error: String?) -> Unit) {
        fn.getHttpsCallable("joinGroup").call(mapOf("code" to code.trim().uppercase()))
            .addOnSuccessListener { r -> Services.log("group_joined"); onDone((r.getData() as? Map<*, *>)?.get("gid") as? String, null) }
            .addOnFailureListener { onDone(null, friendly(it)) }
    }

    fun leaveGroup(gid: String) {
        fn.getHttpsCallable("leaveGroup").call(mapOf("gid" to gid)).addOnFailureListener { Services.recordError(it) }
    }

    /** Uploads any photo or voice note, then saves your answer. The server counts it and unlocks when ready. */
    fun submitAnswer(gid: String, qid: String, text: String, photo: String?, voice: String?, voiceMs: Long, onDone: (String?) -> Unit) {
        val me = uid ?: return onDone("Sign in first.")
        val clean = text.trim().replace(Regex("\\s+"), " ").take(280)
        if (clean.isEmpty() && photo == null && voice == null) return onDone("Write an answer, or add a photo or voice note.")
        val folder = FirebaseStorage.getInstance().reference.child("answers/$gid/$qid")
        fun upload(path: String?, name: String, type: String, next: (String?) -> Unit) {
            if (path == null) return next(null)
            val ref = folder.child(name)
            ref.putFile(Uri.fromFile(File(path)), StorageMetadata.Builder().setContentType(type).build())
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { next(it.toString()) }
                .addOnFailureListener { Services.recordError(it); next(null) }
        }
        upload(photo, "$me.jpg", "image/jpeg") { photoUrl ->
            upload(voice, "$me.m4a", "audio/mp4") { voiceUrl ->
                val data = mutableMapOf<String, Any>("text" to clean, "createdAt" to System.currentTimeMillis(), "reactions" to emptyMap<String, Any>(), "voiceMs" to voiceMs)
                photoUrl?.let { data["photoUrl"] = it }
                voiceUrl?.let { data["voiceUrl"] = it }
                fs.collection("groups").document(gid).collection("questions").document(qid).collection("answers").document(me).set(data)
                    .addOnSuccessListener {
                        Services.log("answer_submitted", "kind" to "real")
                        // Show your answer right away, before the server confirms.
                        Store.update { d ->
                            val q = d.questions[gid]?.get(qid)
                            if (q != null && me !in q.answeredBy) q.answeredBy.add(me)
                            true
                        }
                        onDone(null)
                    }
                    .addOnFailureListener { onDone("Couldn’t save your answer. Check your connection.") }
            }
        }
    }

    fun react(gid: String, qid: String, answerUid: String, kind: Reaction, currently: Reaction?) {
        val me = uid ?: return
        fs.collection("groups").document(gid).collection("questions").document(qid).collection("answers").document(answerUid)
            .update("reactions.$me", if (currently == kind) FieldValue.delete() else kind.name)
            .addOnFailureListener { Services.recordError(it) }
    }

    fun comment(gid: String, qid: String, text: String, replyTo: String?, onDone: (String?) -> Unit) {
        val me = uid ?: return onDone("Sign in first.")
        val t = text.trim().take(280)
        if (t.isEmpty()) return onDone("Write something first.")
        val data = mutableMapOf<String, Any>("uid" to me, "text" to t, "createdAt" to System.currentTimeMillis())
        replyTo?.let { data["replyTo"] = it }
        fs.collection("groups").document(gid).collection("questions").document(qid).collection("comments").add(data)
            .addOnSuccessListener { onDone(null) }
            .addOnFailureListener { onDone("Couldn’t post that. Try again.") }
    }

    fun nudge(gid: String, to: String, onDone: (String?) -> Unit) {
        fn.getHttpsCallable("nudge").call(mapOf("gid" to gid, "to" to to))
            .addOnSuccessListener { onDone(null) }
            .addOnFailureListener { onDone(friendly(it)) }
    }

    fun setNextQuestion(gid: String, text: String, onDone: (String?) -> Unit) {
        fn.getHttpsCallable("setNextQuestion").call(mapOf("gid" to gid, "text" to text))
            .addOnSuccessListener { onDone(null) }
            .addOnFailureListener { onDone(friendly(it)) }
    }

    /** The link friends tap to join. */
    fun inviteLink(code: String) = "https://ask-day.web.app/j/$code"
}
