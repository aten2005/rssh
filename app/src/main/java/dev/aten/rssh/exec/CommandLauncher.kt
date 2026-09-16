package dev.aten.rssh.exec

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf

object CommandLauncher {
    fun enqueue(context: Context, commandId: Long) {
        val request = OneTimeWorkRequestBuilder<RunCommandWorker>()
            .setInputData(workDataOf(RunCommandWorker.KEY_COMMAND_ID to commandId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        // KEEP: a double tap while the command is still running doesn't run it twice.
        WorkManager.getInstance(context)
            .enqueueUniqueWork("run-command-$commandId", ExistingWorkPolicy.KEEP, request)
    }
}
