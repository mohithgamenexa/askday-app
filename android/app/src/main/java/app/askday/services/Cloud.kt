package app.askday.services

import android.net.Uri
import app.askday.data.DB
import app.askday.data.Profile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.File

/**
 * Phase B1: accounts on the server.
 *
 * users/{uid}            public profile: name, username, photo, status (any signed-in person can read)
 * users/{uid}/private/me phone number, birthday, packs (only you can read)
 * usernames/{username}   → { uid }, so usernames are unique
 * Storage: avatars/{uid}.jpg
 *
 * The phone keeps its own copy (Store) for speed and offline use; this mirrors it to the server.
 */
object Cloud {
    private val db get() = FirebaseFirestore.getInstance()
    val ready get() = Services.firebaseReady

    /** What was last sent, so unchanged profiles aren't uploaded again. */
    private var lastSent: String? = null
    private var lastPhoto: String? = null

    // ------------------------------------------------------------ usernames

    /** Claims a username for this account. Fails if someone else has it. */
    fun claimUsername(uid: String, username: String, onDone: (error: String?) -> Unit) {
        if (!ready) return onDone(null) // offline-only builds keep usernames on the phone
        val name = username.trim().removePrefix("@").lowercase()
        val ref = db.collection("usernames").document(name)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val owner = snap.getString("uid")
            if (owner != null && owner != uid) throw IllegalStateException("taken")
            if (owner == null) tx.set(ref, mapOf("uid" to uid, "at" to FieldValue.serverTimestamp()))
            null
        }.addOnSuccessListener { onDone(null) }
            .addOnFailureListener { e ->
                onDone(if (e.message?.contains("taken") == true || e.cause?.message?.contains("taken") == true) "@$name is taken. Try another." else "Couldn’t check that username. Check your connection.")
            }
    }

    // ------------------------------------------------------------- profile

    /** Uploads your profile when it changes. Called after every local change; cheap when nothing changed. */
    fun syncProfile(d: DB) {
        if (!ready) return
        val uid = d.me ?: return
        if (PhoneAuth.currentUid != uid) return
        val p = d.profile ?: return
        val public = mapOf(
            "name" to p.name,
            "username" to p.username,
            "status" to d.status,
            "createdAt" to p.createdAt,
            "level" to app.askday.data.Waves.level(app.askday.data.Waves.xp(d)),
        )
        val private = mapOf(
            "phone" to p.phone,
            "birthday" to p.birthday,
            "packs" to p.packs,
            "onboarded" to d.onboarded,
            "notif" to mapOf("daily" to p.notif.daily, "unlocked" to p.notif.unlocked, "nudges" to p.notif.nudges, "reminders" to p.notif.reminders),
        )
        val signature = public.toString() + private.toString()
        if (signature != lastSent) {
            lastSent = signature
            val user = db.collection("users").document(uid)
            user.set(public + ("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            user.collection("private").document("me").set(private, SetOptions.merge())
        }
        val photo = p.photo
        if (photo != null && photo != lastPhoto && !photo.startsWith("http")) {
            lastPhoto = photo
            uploadAvatar(uid, photo)
        }
    }

    private fun uploadAvatar(uid: String, path: String) {
        val file = File(path)
        if (!file.exists()) return
        val ref = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")
        ref.putFile(Uri.fromFile(file), com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build())
            .continueWithTask { ref.downloadUrl }
            .addOnSuccessListener { url -> db.collection("users").document(uid).set(mapOf("photoUrl" to url.toString()), SetOptions.merge()) }
            .addOnFailureListener { Services.recordError(it) }
    }

    /**
     * After signing in on a phone: if this account already has a profile on the server
     * (for example, you changed phones), brings it back so you skip onboarding.
     */
    fun restore(uid: String, onRestored: (Profile?, Map<String, Any?>?) -> Unit) {
        if (!ready) return onRestored(null, null)
        val user = db.collection("users").document(uid)
        user.get().addOnSuccessListener { pub ->
            if (!pub.exists()) return@addOnSuccessListener onRestored(null, null)
            user.collection("private").document("me").get().addOnSuccessListener { priv ->
                val profile = Profile(
                    id = uid,
                    name = pub.getString("name") ?: "",
                    createdAt = pub.getLong("createdAt") ?: System.currentTimeMillis(),
                    username = pub.getString("username"),
                    phone = priv.getString("phone"),
                    birthday = priv.getString("birthday"),
                    packs = (priv.get("packs") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(),
                )
                onRestored(profile, priv.data)
            }.addOnFailureListener { onRestored(null, null) }
        }.addOnFailureListener { onRestored(null, null) }
    }

    /** After any sign-in: bring back the server profile, if there is one. */
    fun afterSignIn(uid: String) {
        restore(uid) { profile, priv ->
            if (profile == null) return@restore
            app.askday.data.act { d ->
                if (d.me == uid && d.profile == null) {
                    d.profile = profile
                    d.onboarded = priv?.get("onboarded") == true
                }
            }
        }
    }

    /** Deletes your server profile and username (for "Delete account"). */
    fun deleteAccount(d: DB) {
        if (!ready) return
        val uid = d.me ?: return
        d.profile?.username?.let { db.collection("usernames").document(it).delete() }
        db.collection("users").document(uid).collection("private").document("me").delete()
        db.collection("users").document(uid).delete()
        FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg").delete()
        lastSent = null
        lastPhoto = null
    }
}
