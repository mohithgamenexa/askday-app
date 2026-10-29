package app.askday.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.askday.data.Social
import app.askday.data.act

/** Username and birthday. The birthday uses three plain boxes (day, month, year), so typing is never reordered. */
@Composable
fun DetailsScreenV2() {
    val c = colors
    var username by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val numbers = KeyboardOptions(keyboardType = KeyboardType.Number)
    AskScreen {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            OnboardingProgress(3)
            Label("Step 3 · Make it yours")
            Display("Pick a username", 32)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Username", 14, FontWeight.Bold)
                AskField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' }.take(20) }, "aarav.k", focusBorder = true)
                Txt("Friends find you with @${username.ifBlank { "username" }}.", 13, color = c.muted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("Birthday", 14, FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AskField(day, { day = it.filter(Char::isDigit).take(2) }, "DD", Modifier.weight(1f), keyboard = numbers)
                    AskField(month, { month = it.filter(Char::isDigit).take(2) }, "MM", Modifier.weight(1f), keyboard = numbers)
                    AskField(year, { year = it.filter(Char::isDigit).take(4) }, "YYYY", Modifier.weight(1.6f), keyboard = numbers)
                }
                Txt("You need to be 13 or older. Your birthday is never shown on your profile.", 13, color = c.muted, lineHeight = 18.sp)
            }
            Box(Modifier.height(12.dp))
            ErrorText(error)
            AskButton("Continue", enabled = username.length >= 3 && day.isNotEmpty() && month.isNotEmpty() && year.length == 4) {
                error = act { Social.saveDetails(it, username, "$day/$month/$year") }
                if (error == null) {
                    // Make sure nobody else has this username; undo it locally if they do.
                    val uid = app.askday.services.PhoneAuth.currentUid
                    if (uid != null) app.askday.services.Cloud.claimUsername(uid, username) { err ->
                        if (err != null) {
                            error = err
                            act { it.profile?.username = null }
                        }
                    }
                }
            }
        }
    }
}
