package skywa1ker404.absencium.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import skywa1ker404.absencium.MainActivity
import skywa1ker404.absencium.R
import skywa1ker404.absencium.appContainer
import skywa1ker404.absencium.domain.Reminder
import skywa1ker404.absencium.domain.ReminderKind
import skywa1ker404.absencium.domain.lowAttendanceText
import skywa1ker404.absencium.domain.nextReminder
import skywa1ker404.absencium.logging.DebugLogger
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps one alarm for the next reminder. It uses an inexact alarm that may run while the phone is idle, so no
 * exact-alarm permission is needed and the battery is spared; a reminder can arrive a little late, never early.
 */
object ReminderScheduler {
    private const val CHANNEL = "reminders"
    private const val ACTION = "skywa1ker404.absencium.REMINDER"

    /** Works out the next reminder from the current settings and records, and sets (or cancels) the alarm. */
    suspend fun reschedule(context: Context) {
        val app = context.applicationContext
        val container = app.appContainer
        val alarms = app.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(app, null)
        val settings = container.settingsRepository.settings.first()
        // Events carry their own reminders, so they count even when every general reminder is off.
        val events = container.database.eventDao().observeAll().first()
        val next = nextReminder(container.calendarRepository.data.first(), settings, LocalDateTime.now(), events)
        if (next == null) {
            alarms.cancel(pending)
            return
        }
        val at = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(app, next))
        DebugLogger.i("Reminders", "Next ${next.kind} reminder at ${next.at}")
    }

    internal fun pendingIntent(context: Context, reminder: Reminder?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION)
        if (reminder != null) {
            intent.putExtra("key", reminder.key).putExtra("kind", reminder.kind.name)
                .putExtra("title", reminder.title).putExtra("text", reminder.text)
        }
        // One slot for the single alarm; setting it again replaces the previous one.
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal suspend fun post(context: Context, kind: ReminderKind, key: String, title: String, text: String) {
        if (!canNotify(context)) return
        val container = context.applicationContext.appContainer
        var body = text
        if (kind == ReminderKind.LOW_ATTENDANCE) {
            body = lowAttendanceText(container.calendarRepository.data.first(), container.settingsRepository.settings.first()) ?: return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_logo_a)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        // The key makes the id, so the same reminder never shows twice.
        manager.notify(key.hashCode(), notification)
    }
}

/** Posts the reminder that was due, then sets the alarm for the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val kind = runCatching { ReminderKind.valueOf(intent.getStringExtra("kind").orEmpty()) }.getOrNull()
                if (kind != null) {
                    ReminderScheduler.post(context, kind, intent.getStringExtra("key").orEmpty(), intent.getStringExtra("title").orEmpty(), intent.getStringExtra("text").orEmpty())
                }
                ReminderScheduler.reschedule(context)
            } catch (e: Exception) {
                DebugLogger.e("Reminders", "Could not post a reminder", e)
            } finally {
                result.finish()
            }
        }
    }
}

/** Sets the alarm again after a restart, a clock or time-zone change, or an app update, when Android has dropped it. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderScheduler.reschedule(context)
            } catch (e: Exception) {
                DebugLogger.e("Reminders", "Could not set the reminder alarm", e)
            } finally {
                result.finish()
            }
        }
    }
}
