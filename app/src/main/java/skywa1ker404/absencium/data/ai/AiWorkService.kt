package skywa1ker404.absencium.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import skywa1ker404.absencium.logging.DebugLogger

/**
 * Keeps Absencium running while an AI answer is being written, so closing the app or leaving the chat does not stop it. It shows
 * Android's required "working" notification only while an answer is in flight and stops itself when none is left. It does nothing else.
 */
class AiWorkService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "AI answers in progress", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shown only while Absencium AI is writing an answer, so it can finish when the app is closed."
                    setShowBadge(false)
                },
            )
        }
        val notification = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(this))
            .setContentTitle("Absencium AI is writing an answer")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(ID, notification)
            }
        } catch (e: Exception) {
            // Android may refuse (app not allowed to start it right now). The answer still runs while the app process lives.
            DebugLogger.e("AiWorkService", "The foreground service could not start", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "ai_work"
        private const val ID = 4107

        /** Starts or stops the service as answers begin and end. Never throws. */
        fun setBusy(context: Context, busy: Boolean) {
            try {
                val intent = Intent(context, AiWorkService::class.java)
                if (busy) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
                } else {
                    context.stopService(intent)
                }
            } catch (e: Exception) {
                DebugLogger.e("AiWorkService", "The foreground service could not be changed", e)
            }
        }
    }
}
