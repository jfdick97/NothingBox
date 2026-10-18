package de.jfd.straingauge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val appContext = context.applicationContext

        when (intent.action) {
            Reminders.ACTION_SNOOZE -> {
                val week = intent.getStringExtra(
                    Reminders.EXTRA_WEEK
                ) ?: return

                Reminders.pauseWeek(appContext, week)
                return
            }

            Reminders.ACTION_CHECK -> {
                // Zuerst die nächste tägliche Prüfung planen.
                Reminders.scheduleNext(appContext)
            }

            else -> return
        }

        if (
            !Reminders.isEnabled(appContext) ||
            Reminders.isPausedThisWeek(appContext)
        ) {
            return
        }

        val pendingResult = goAsync()

        val powerManager = appContext.getSystemService(
            PowerManager::class.java
        )

        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StrainGauge:WeeklyReminder"
        ).apply {
            setReferenceCounted(false)
        }

        try {
            wakeLock.acquire(10_000L)
        } catch (exception: Exception) {
            Log.e(
                "WeeklyReminder",
                "Prüfung konnte nicht gestartet werden.",
                exception
            )
            pendingResult.finish()
            return
        }

        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        ).launch {
            try {
                withTimeout(8_000L) {
                    Reminders.checkAndNotify(appContext)
                }
            } catch (exception: Exception) {
                Log.e(
                    "WeeklyReminder",
                    "Erinnerungsprüfung fehlgeschlagen.",
                    exception
                )
            } finally {
                try {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        Reminders.scheduleNext(context.applicationContext)
    }
}