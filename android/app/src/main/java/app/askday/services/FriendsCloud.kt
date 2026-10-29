package app.askday.services

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

/** Someone on Askday, as others see them. */
data class CloudUser(val uid: String, val name: String, val username: String?, val photoUrl: String?)

/** Online right now, or when they were last seen (null = they hide it). */
data class Presence(val online: Boolean, val lastSeen: Long?)

/**
 * Phase B2: real friends.
 *
 * friendRequests/{from_to}     pending requests
 * users/{uid}/friends/{fid}    accepted friends (written for both people)
 * Realtime Database status/{uid} → { state: online | offline | hidden, last }
 * Cloud Functions registerPhone / matchContacts for "your contacts on Askday"
 */
object FriendsCloud {
    private val fs get() = FirebaseFirestore.getInstance()
    private val functions get() = FirebaseFunctions.getInstance("asia-south1")

    /** Online status needs a Realtime Database; if the project has none yet, it's quietly off. */
    private val rtdb: FirebaseDatabase? get() = runCatching { FirebaseDatabase.getInstance() }.getOrNull()

    private val _friends = MutableStateFlow<List<CloudUser>>(emptyList())
    val friends: StateFlow<List<CloudUser>> = _friends
    private val _incoming = MutableStateFlow<List<CloudUser>>(emptyList())
    val incoming: StateFlow<List<CloudUser>> = _incoming
    private val _outgoing = MutableStateFlow<Set<String>>(emptySet())
    val outgoing: StateFlow<Set<String>> = _outgoing
    private val _presence = MutableStateFlow<Map<String, Presence>>(emptyMap())
    val presence: StateFlow<Map<String, Presence>> = _presence

    private var uid: String? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private val presenceListeners = mutableMapOf<String, ValueEventListener>()
    private var connectedListener: ValueEventListener? = null
    var showOnline = true
        private set

    // ------------------------------------------------------------ lifecycle

    /** Starts listening for friends and requests once signed in. Safe to call repeatedly. */
    fun start(me: String, showOnline: Boolean) {
        if (!Services.firebaseReady || uid == me) return
        stop()
        uid = me
        this.showOnline = showOnline
        listeners += fs.collection("users").document(me).collection("friends").addSnapshotListener { snap, _ ->
            val ids = snap?.documents?.map { it.id }.orEmpty()
            loadUsers(ids) { list -> _friends.value = list.sortedBy { it.name.lowercase() } }
            watchPresence(ids)
        }
        listeners += fs.collection("friendRequests").whereEqualTo("to", me).addSnapshotListener { snap, _ ->
            loadUsers(snap?.documents?.mapNotNull { it.getString("from") }.orEmpty()) { _incoming.value = it }
        }
        listeners += fs.collection("friendRequests").whereEqualTo("from", me).addSnapshotListener { snap, _ ->
            _outgoing.value = snap?.documents?.mapNotNull { it.getString("to") }.orEmpty().toSet()
        }
        startPresence(me)
        // Make your own number findable by friends who have you in their contacts.
        functions.getHttpsCallable("registerPhone").call().addOnFailureListener { Services.recordError(it) }
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        val db = rtdb
        if (db != null) {
            presenceListeners.forEach { (id, l) -> db.getReference("status/$id").removeEventListener(l) }
            connectedListener?.let { db.getReference(".info/connected").removeEventListener(it) }
            uid?.let { db.getReference("status/$it").setValue(mapOf("state" to "offline", "last" to ServerValue.TIMESTAMP)) }
        }
        presenceListeners.clear()
        connectedListener = null
        uid = null
        _friends.value = emptyList(); _incoming.value = emptyList(); _outgoing.value = emptySet(); _presence.value = emptyMap()
    }

    private fun loadUsers(ids: List<String>, onDone: (List<CloudUser>) -> Unit) {
        if (ids.isEmpty()) return onDone(emptyList())
        val out = mutableListOf<CloudUser>()
        var left = ids.size
        for (id in ids) fs.collection("users").document(id).get().addOnCompleteListener { t ->
            t.result?.takeIf { it.exists() }?.let { d -> out += CloudUser(id, d.getString("name") ?: "", d.getString("username"), d.getString("photoUrl")) }
            if (--left == 0) onDone(out)
        }
    }

    // --------------------------------------------------------------- search

    /** Finds people whose username starts with what you typed. */
    fun search(query: String, onDone: (List<CloudUser>) -> Unit) {
        val q = query.trim().removePrefix("@").lowercase()
        if (!Services.firebaseReady || q.length < 2) return onDone(emptyList())
        fs.collection("users").whereGreaterThanOrEqualTo("username", q).whereLessThanOrEqualTo("username", q + "\uf8ff").limit(15).get()
            .addOnSuccessListener { snap ->
                onDone(snap.documents.filter { it.id != uid }.map { CloudUser(it.id, it.getString("name") ?: "", it.getString("username"), it.getString("photoUrl")) })
            }
            .addOnFailureListener { onDone(emptyList()) }
    }

    // -------------------------------------------------------------- requests

    fun sendRequest(to: String, onDone: (String?) -> Unit = {}) {
        val me = uid ?: return onDone("Sign in first.")
        fs.collection("friendRequests").document("${me}_$to").set(mapOf("from" to me, "to" to to, "at" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { Services.log("friend_request_sent"); onDone(null) }
            .addOnFailureListener { onDone("Couldn’t send the request. Check your connection.") }
    }

    /** Accepting adds each of you to the other's friends and clears the request, all at once. */
    fun accept(from: String, onDone: (String?) -> Unit = {}) {
        val me = uid ?: return onDone("Sign in first.")
        val batch = fs.batch()
        batch.set(fs.collection("users").document(me).collection("friends").document(from), mapOf("since" to FieldValue.serverTimestamp()))
        batch.set(fs.collection("users").document(from).collection("friends").document(me), mapOf("since" to FieldValue.serverTimestamp()))
        batch.delete(fs.collection("friendRequests").document("${from}_$me"))
        batch.commit().addOnSuccessListener { Services.log("friend_request_accepted"); onDone(null) }.addOnFailureListener { onDone("Couldn’t accept. Try again.") }
    }

    fun decline(from: String) {
        val me = uid ?: return
        fs.collection("friendRequests").document("${from}_$me").delete()
    }

    fun cancel(to: String) {
        val me = uid ?: return
        fs.collection("friendRequests").document("${me}_$to").delete()
    }

    fun remove(friend: String) {
        val me = uid ?: return
        val batch = fs.batch()
        batch.delete(fs.collection("users").document(me).collection("friends").document(friend))
        batch.delete(fs.collection("users").document(friend).collection("friends").document(me))
        batch.commit()
    }

    // -------------------------------------------------------------- contacts

    /** Turns a contact's number into the international format and hashes it the same way the server does. */
    fun hashNumber(raw: String, defaultCountry: String = "91"): String? {
        val digits = raw.filter(Char::isDigit)
        val e164 = when {
            raw.trim().startsWith("+") -> "+$digits"
            digits.length == 10 -> "+$defaultCountry$digits"
            digits.startsWith("0") && digits.length == 11 -> "+$defaultCountry${digits.drop(1)}"
            digits.length in 11..15 -> "+$digits"
            else -> return null
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(("askday-contacts-v1:$e164").toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Which of these phone numbers belong to Askday accounts. Returns number → person. */
    fun matchContacts(numbers: List<String>, onDone: (Map<String, CloudUser>) -> Unit) {
        if (!Services.firebaseReady || uid == null) return onDone(emptyMap())
        val byHash = HashMap<String, String>()
        for (n in numbers) hashNumber(n)?.let { byHash[it] = n }
        if (byHash.isEmpty()) return onDone(emptyMap())
        functions.getHttpsCallable("matchContacts").call(mapOf("hashes" to byHash.keys.take(2000))).addOnSuccessListener { result ->
            val matches = ((result.getData() as? Map<*, *>)?.get("matches") as? List<*>).orEmpty()
            val out = HashMap<String, CloudUser>()
            for (m in matches) {
                val map = m as? Map<*, *> ?: continue
                val number = byHash[map["hash"] as? String] ?: continue
                out[number] = CloudUser(map["uid"] as? String ?: continue, map["name"] as? String ?: "", map["username"] as? String, map["photoUrl"] as? String)
            }
            Services.log("contacts_matched", "count" to out.size)
            onDone(out)
        }.addOnFailureListener {
            Services.recordError(it)
            onDone(emptyMap())
        }
    }

    // -------------------------------------------------------------- presence

    private fun statusValue(state: String) = mapOf("state" to state, "last" to ServerValue.TIMESTAMP)

    private fun startPresence(me: String) {
        val db = rtdb ?: return
        val mine = db.getReference("status/$me")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                if (s.getValue(Boolean::class.java) != true) return
                // When this phone disconnects, the server marks you offline by itself.
                mine.onDisconnect().setValue(statusValue(if (showOnline) "offline" else "hidden"))
                mine.setValue(statusValue(if (showOnline) "online" else "hidden"))
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        connectedListener = l
        db.getReference(".info/connected").addValueEventListener(l)
    }

    /** Called when the app goes to the background or comes back. */
    fun setActive(active: Boolean) {
        val me = uid ?: return
        val db = rtdb ?: return
        db.getReference("status/$me").setValue(statusValue(if (!showOnline) "hidden" else if (active) "online" else "offline"))
    }

    fun setShowOnline(show: Boolean) {
        showOnline = show
        setActive(true)
    }

    private fun watchPresence(ids: List<String>) {
        val db = rtdb ?: return
        for (id in ids) {
            if (presenceListeners.containsKey(id)) continue
            val l = object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val state = s.child("state").getValue(String::class.java)
                    val last = s.child("last").getValue(Long::class.java)
                    _presence.value = _presence.value + (id to if (state == "hidden" || state == null) Presence(false, null) else Presence(state == "online", last))
                }
                override fun onCancelled(e: DatabaseError) {}
            }
            presenceListeners[id] = l
            db.getReference("status/$id").addValueEventListener(l)
        }
    }

    /** "Online now", "Active 5 min ago" or nothing if they hide it. */
    fun presenceLabel(p: Presence?, now: Long = System.currentTimeMillis()): String? {
        if (p == null) return null
        if (p.online) return "Online now"
        val last = p.lastSeen ?: return null
        val min = (now - last) / 60_000
        return when {
            min < 1 -> "Active just now"
            min < 60 -> "Active $min min ago"
            min < 24 * 60 -> "Active ${min / 60} h ago"
            else -> "Active ${min / (24 * 60)} d ago"
        }
    }
}
