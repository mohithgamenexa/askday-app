package app.askday.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.askday.MainActivity
import app.askday.R
import app.askday.data.DB
import app.askday.data.DEMO_QUESTIONS
import app.askday.data.qkey
import app.askday.data.questionIndexFor

// Notifications are scheduled on the phone itself, so they need no server:
// the daily drop, "everyone's in", and a reminder 30 minutes before unlock.

private const val CHANNEL = "daily"
private const val MAX_SCHEDULED = 40

object Notifier {
    private data class Planned(val at: Long, val title: String, val body: String, val groupId: String)

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Daily questions", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun plan(d: DB, now: Long): List<Planned> {
        val me = d.me ?: return emptyList()
        val prefs = d.profile?.notif ?: return emptyList()
        val out = mutableListOf<Planned>()
        if (prefs.daily) {
            val next = app.askday.data.Social.momentDropFor(now).let { if (it <= now) app.askday.data.Social.momentDropFor(now + 86_400_000L) else it }
            out += Planned(next, "It’s time to Askday", "Today’s question just dropped. Answer in the next 3 hours to be on time.", "")
        }
        for (g in d.groups.values) {
            val next = g.nextDropAt
            if (prefs.daily && next != null) {
                out += Planned(next, "${g.name}: today’s question", DEMO_QUESTIONS[questionIndexFor(g.seed, g.day + 1)].text, g.id)
            }
            val q = g.currentId?.let { d.questions[g.id]?.get(it) } ?: continue
            if (q.unlocked) continue
            val answered = me in q.answeredBy
            if (!answered && prefs.reminders) {
                out += Planned(q.unlockAt - 30 * 60_000, "30 minutes left at ${g.name}", "Answer now to keep your ${g.streak}-day streak alive.", g.id)
            }
            if (answered && prefs.unlocked) {
                val times = d.sim[qkey(g.id, q.id)]?.answerAt?.values.orEmpty()
                val everyoneAt = if (times.isNotEmpty() && times.all { it != null }) times.maxOf { it!! } else Long.MAX_VALUE
                val at = minOf(everyoneAt, q.unlockAt)
                out += Planned(at, if (everyoneAt <= q.unlockAt) "Everyone’s in at ${g.name}" else "Answers unlocked at ${g.name}", "Go see what everyone said.", g.id)
            }
        }
        return out.filter { it.at > now + 2000 }.sortedBy { it.at }.take(MAX_SCHEDULED)
    }

    private var lastKey = ""

    /** Keeps the phone's scheduled notifications in line with the data. */
    fun reschedule(context: Context, d: DB) {
        val planned = plan(d, System.currentTimeMillis())
        val key = planned.joinToString("|") { "${it.at}${it.title}" }
        if (key == lastKey) return
        lastKey = key
        val alarms = context.getSystemService(AlarmManager::class.java)
        for (i in 0 until MAX_SCHEDULED) {
            PendingIntent.getBroadcast(context, i, Intent(context, NotifyReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { alarms.cancel(it); it.cancel() }
        }
        planned.forEachIndexed { i, p ->
            val intent = Intent(context, NotifyReceiver::class.java)
                .putExtra("title", p.title).putExtra("body", p.body).putExtra("groupId", p.groupId).putExtra("id", i)
            val pending = PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.at, pending)
        }
    }
}

class NotifyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val open = PendingIntent.getActivity(
            context,
            intent.getIntExtra("id", 0),
            Intent(context, MainActivity::class.java).putExtra("groupId", intent.getStringExtra("groupId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(intent.getStringExtra("title"))
            .setContentText(intent.getStringExtra("body"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(intent.getStringExtra("body")))
            .setColor(0xFF0A8A80.toInt())
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(intent.getIntExtra("id", 0) + 100, n) }
    }
}
