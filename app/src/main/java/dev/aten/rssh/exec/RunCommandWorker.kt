package dev.aten.rssh.exec

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.aten.rssh.R
import dev.aten.rssh.appContainer

class RunCommandWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val bound = container.db.commands().getWithHost(inputData.getLong(KEY_COMMAND_ID, -1))
        if (bound == null) {
            toast(applicationContext.getString(R.string.toast_command_missing))
            return Result.failure()
        }
        val outcome = container.runner.run(bound.host, bound.command.command, bound.command.timeoutSec)
        toast(ResultFormatter.format(bound.command.label, outcome))
        return Result.success()
    }

    // Only used on Android 10–11, where expedited work runs as a foreground service.
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_running),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(applicationContext.getString(R.string.notification_running))
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun toast(text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val KEY_COMMAND_ID = "commandId"
        private const val CHANNEL_ID = "running"
        private const val NOTIFICATION_ID = 1
    }
}
