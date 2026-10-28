package app.askday.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.askday.MainActivity
import app.askday.R
import app.askday.data.DB
import app.askday.data.Social
import app.askday.data.Store
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home screen widget: today's Askday question, and whether you've answered. */
class AskdayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Store.init(context)
        update(context, Store.db.value)
    }

    companion object {
        fun update(context: Context, d: DB) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AskdayWidget::class.java))
            if (ids.isEmpty()) return
            val now = System.currentTimeMillis()
            val m = d.moment
            val me = d.me
            val clock = SimpleDateFormat("h:mm a", Locale.getDefault())
            val isToday = m != null && m.dayKey == Social.dayKey(now)
            val answered = m?.answers?.any { it.uid == me } == true
            val friends = m?.answers?.count { it.uid != me } ?: 0
            val status = when {
                m == null -> "Open Askday to get started"
                !isToday -> "Next question drops at " + clock.format(Date(Social.momentDropFor(now).let { if (it <= now) Social.momentDropFor(now + 86_400_000L) else it }))
                answered -> "Answered · $friends friends in"
                now < m.onTimeUntil -> "Answer before " + clock.format(Date(m.onTimeUntil)) + " to be on time"
                else -> "Answer now · $friends friends answered"
            }
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val views = RemoteViews(context.packageName, R.layout.widget_askday).apply {
                setTextViewText(R.id.widget_question, m?.text ?: "One question. Everyone. Answer to see.")
                setTextViewText(R.id.widget_status, status)
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
