package app.askday.services

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthTooManyRequestsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/** Phone number sign-in with a one-time SMS code, sent by Firebase Authentication. */
object PhoneAuth {
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    val currentUid: String? get() = if (Services.firebaseReady) FirebaseAuth.getInstance().currentUser?.uid else null
    val currentPhone: String? get() = if (Services.firebaseReady) FirebaseAuth.getInstance().currentUser?.phoneNumber else null

    /**
     * Sends a code to [phone] (with country code, like +919876543210).
     * [onSignedIn] runs if Android verifies the number by itself (it can read the SMS automatically).
     */
    fun sendCode(
        activity: Activity,
        phone: String,
        resend: Boolean,
        onCodeSent: () -> Unit,
        onSignedIn: (uid: String, phone: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!Services.firebaseReady) return onError("Phone login needs Firebase. Add google-services.json to the app first.")
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = signIn(credential, onSignedIn, onError)
            override fun onVerificationFailed(e: FirebaseException) = onError(friendly(e))
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                resendToken = token
                onCodeSent()
            }
        }
        val builder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        val token = resendToken
        if (resend && token != null) builder.setForceResendingToken(token)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    /** Checks the code the person typed. */
    fun verifyCode(code: String, onSignedIn: (uid: String, phone: String) -> Unit, onError: (String) -> Unit) {
        val id = verificationId ?: return onError("Send a code first.")
        signIn(PhoneAuthProvider.getCredential(id, code), onSignedIn, onError)
    }

    private fun signIn(credential: PhoneAuthCredential, onSignedIn: (String, String) -> Unit, onError: (String) -> Unit) {
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
            val user = if (task.isSuccessful) task.result?.user else null
            if (task.isSuccessful && user != null) onSignedIn(user.uid, user.phoneNumber ?: "")
            else onError(task.exception?.let { friendly(it) } ?: "Couldn’t sign in. Try again.")
        }
    }

    fun signOut() {
        if (Services.firebaseReady) FirebaseAuth.getInstance().signOut()
        verificationId = null
        resendToken = null
    }

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "That code or number isn’t right. Check it and try again."
        is FirebaseAuthTooManyRequestsException -> "Too many attempts. Wait a little and try again."
        else -> "We couldn’t send the code. Check your connection and try again."
    }
}
