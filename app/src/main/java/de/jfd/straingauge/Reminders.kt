package de.jfd.straingauge

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Reminders {
    const val ACTION_CHECK =
        "de.jfd.straingauge.REMINDER_CHECK"

    const val ACTION_SNOOZE =
        "de.jfd.straingauge.REMINDER_SNOOZE"

    const val EXTRA_WEEK = "reminder_week"

    private const val PREFS = "weekly_reminders"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PAUSED_WEEK = "paused_week"

    private const val CHANNEL_ID = "weekly_review_reminders"
    private const val NOTIFICATION_ID = 600

    private val dateFormat =
        DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, false)

    fun isPausedThisWeek(context: Context): Boolean {
        val currentWeek = mondayOf(LocalDate.now()).toString()

        return preferences(context)
            .getString(KEY_PAUSED_WEEK, null) == currentWeek
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        scheduleNext(context)
    }

    fun pauseWeek(context: Context, week: String) {
        val currentWeek = mondayOf(LocalDate.now()).toString()

        // Eine alte Benachrichtigung darf nicht versehentlich
        // eine neue Kalenderwoche pausieren.
        if (week == currentWeek) {
            preferences(context)
                .edit()
                .putString(KEY_PAUSED_WEEK, week)
                .apply()
        }

        cancelNotification(context)
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wochenrückblick",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description =
                "Erinnert an eine noch nicht erfasste Vorwoche."
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        if (
            !NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
        ) {
            return false
        }

        val channel = context
            .getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)

        return channel?.importance !=
                NotificationManager.IMPORTANCE_NONE
    }

    fun canUseExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true

        return context
            .getSystemService(AlarmManager::class.java)
            .canScheduleExactAlarms()
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).setAction(ACTION_CHECK)

        return PendingIntent.getBroadcast(
            context,
            600,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNext(context: Context) {
        createChannel(context)

        val manager =
            context.getSystemService(AlarmManager::class.java)

        val pendingIntent = alarmIntent(context)

        if (!isEnabled(context)) {
            manager.cancel(pendingIntent)
            cancelNotification(context)
            return
        }

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        var next = now.toLocalDate()
            .atTime(6, 0)
            .atZone(zone)

        if (!next.isAfter(now)) {
            next = now.toLocalDate()
                .plusDays(1)
                .atTime(6, 0)
                .atZone(zone)
        }

        val triggerAt = next.toInstant().toEpochMilli()

        if (canUseExactAlarms(context)) {
            try {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Falls die Freigabe zwischenzeitlich entzogen wurde:
                // mit ungenauer Zustellung weiterarbeiten.
            }
        }

        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID)
    }

    suspend fun checkAndNotify(context: Context) {
        if (!isEnabled(context) || isPausedThisWeek(context)) {
            return
        }

        val currentWeek = mondayOf(LocalDate.now())
        val previousWeek = currentWeek.minusWeeks(1)
        val previousSunday = previousWeek.plusDays(6)

        val hasData = withContext(Dispatchers.IO) {
            val database = AppDatabase.open(context)

            try {
                database.store().hasEntriesInRange(
                    previousWeek.toString(),
                    previousSunday.toString()
                )
            } finally {
                database.close()
            }
        }

        // Mit den Menüaktionen synchronisieren:
        // Deaktivieren oder Snooze während der Prüfung berücksichtigen.
        withContext(Dispatchers.Main) {
            if (
                !isEnabled(context) ||
                isPausedThisWeek(context)
            ) {
                return@withContext
            }

            if (hasData) {
                cancelNotification(context)
            } else if (canNotify(context)) {
                showNotification(
                    context,
                    currentWeek,
                    previousWeek
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        context: Context,
        currentWeek: LocalDate,
        previousWeek: LocalDate
    ) {
        // Die Berechtigung wurde unmittelbar zuvor geprüft.
        val openApp = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            601,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val snooze = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_WEEK, currentWeek.toString())
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            602,
            snooze,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val range =
            "${previousWeek.format(dateFormat)} – " +
                    previousWeek.plusDays(6).format(dateFormat)

        val text =
            "Für die Woche $range sind noch keine Tageswerte gespeichert."

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_week_reminder)
            .setContentTitle("Dein Wochenrückblick fehlt noch")
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(text)
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                "Diese Woche pausieren",
                snoozePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }
}