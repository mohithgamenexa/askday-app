package app.askday.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.askday.services.Services
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneContact(val name: String, val phone: String)

/** Reads names and numbers from the phone's contacts. Nothing leaves the phone. */
private fun readContacts(context: Context): List<PhoneContact> {
    val out = mutableListOf<PhoneContact>()
    val seen = HashSet<String>()
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC",
    )?.use { c ->
        val nameAt = c.getColumnIndex(projection[0])
        val numberAt = c.getColumnIndex(projection[1])
        while (c.moveToNext()) {
            val name = c.getString(nameAt) ?: continue
            val number = c.getString(numberAt)?.filter { it.isDigit() || it == '+' } ?: continue
            if (number.count(Char::isDigit) < 7) continue
            if (seen.add(number.filter(Char::isDigit).takeLast(10))) out += PhoneContact(name, number)
        }
    }
    return out
}

private fun inviteText(username: String?) =
    "Join me on Askday: one question a day, answered together." + (username?.let { " Add me: @$it" } ?: "")

private fun inviteBySms(context: Context, phone: String, username: String?) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).putExtra("sms_body", inviteText(username))
    runCatching { context.startActivity(intent) }
    Services.log("invite_contact", "via" to "sms")
}

private fun inviteByWhatsApp(context: Context, phone: String, username: String?) {
    val digits = phone.filter(Char::isDigit).let { if (it.length == 10) "91$it" else it.trimStart('0') }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=" + Uri.encode(inviteText(username))))
    runCatching { context.startActivity(intent) }
    Services.log("invite_contact", "via" to "whatsapp")
}

/**
 * The contacts list: asks for permission with a clear reason first, then shows every contact
 * with Invite buttons. "On Askday" needs the online version, so that section says so honestly.
 */
@Composable
fun ContactsList(compact: Boolean = false) {
    val c = colors
    val db = LocalDB.current
    val context = LocalContext.current
    val username = db.profile?.username
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var contacts by remember { mutableStateOf<List<PhoneContact>?>(null) }
    var query by remember { mutableStateOf("") }
    var invited by remember { mutableStateOf(setOf<String>()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        Services.log("contacts_permission", "granted" to ok)
    }
    LaunchedEffect(granted) {
        if (granted) contacts = withContext(Dispatchers.IO) { runCatching { readContacts(context) }.getOrDefault(emptyList()) }
    }

    if (!granted) {
        AskCard(gap = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientBox(Modifier.size(44.dp), RoundedCornerShape(14.dp)) { AskIcon(Ico.Groups, androidx.compose.ui.graphics.Color.White, 22.dp, modifier = Modifier.align(Alignment.Center)) }
                Column(Modifier.weight(1f)) {
                    Txt("Find your friends in contacts", 15, FontWeight.Bold)
                    Txt("See your contacts here and invite them in one tap. Your contacts stay on your phone.", 13, color = c.muted, lineHeight = 18.sp)
                }
            }
            AskButton("Allow contacts") { ask.launch(Manifest.permission.READ_CONTACTS) }
        }
        return
    }

    val list = contacts
    if (list == null) {
        Txt("Loading your contacts…", 14, color = c.muted)
        return
    }

    if (Services.firebaseReady && app.askday.services.PhoneAuth.currentUid != null) {
        ContactMatches(list.map { it.phone }) { n -> list.firstOrNull { it.phone == n }?.name ?: "" }
    } else AskCard(gap = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AskIcon(Ico.Spark, Brand.accent, 18.dp)
            Txt("On Askday", 15, FontWeight.Bold)
        }
        Txt("Sign in to see which of your contacts already use Askday.", 13, color = c.muted, lineHeight = 18.sp)
    }

    AskField(query, { query = it.take(40) }, "Search ${list.size} contacts")
    val shown = list.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.phone.contains(query.filter(Char::isDigit).ifEmpty { "\u0000" }) }
        .let { if (compact) it.take(30) else it }
    if (shown.isEmpty()) Txt(if (list.isEmpty()) "No contacts with phone numbers found." else "No contacts match “$query”.", 14, color = c.muted)
    AskCard(gap = 14.dp) {
        shown.forEachIndexed { i, ct ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar("contact${ct.phone}", ct.name, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Txt(ct.name, 15, FontWeight.Bold, maxLines = 1)
                    Txt(ct.phone, 13, color = c.muted, maxLines = 1)
                }
                val done = ct.phone in invited
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c.chip).tap(label = "Invite ${ct.name} on WhatsApp") {
                        inviteByWhatsApp(context, ct.phone, username)
                        invited = invited + ct.phone
                    },
                    contentAlignment = Alignment.Center,
                ) { AskIcon(Ico.WhatsApp, c.ink, 18.dp) }
                Box(
                    Modifier.height(32.dp).clip(CircleShape).background(if (done) c.likedBg else c.btnBg)
                        .tap(label = "Invite ${ct.name} by SMS") {
                            inviteBySms(context, ct.phone, username)
                            invited = invited + ct.phone
                        }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt(if (done) "Invited" else "Invite", 13, FontWeight.Bold, if (done) c.likedInk else c.btnFg) }
            }
            if (i < shown.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        }
    }
    if (compact && list.size > 30 && query.isBlank()) Txt("Search to find more of your ${list.size} contacts.", 13, color = c.muted)
}

/** Full-screen contacts, opened from the Friends tab. */
@Composable
fun ContactsScreen() {
    val c = colors
    AskScreen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackBtn { Nav.pop() }
                Column {
                    Txt("Invite contacts", 17, FontWeight.Bold)
                    Txt("Askday is better with your people", 13, color = c.muted)
                }
            }
            ContactsList()
        }
    }
}
