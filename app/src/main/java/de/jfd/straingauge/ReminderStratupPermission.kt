package de.jfd.straingauge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun ReminderStartupPermissions() {
    val context = LocalContext.current

    val preferences = remember(context) {
        context.getSharedPreferences(
            "weekly_reminders",
            Context.MODE_PRIVATE
        )
    }

    var showExactAlarmExplanation by rememberSaveable {
        mutableStateOf(false)
    }

    fun checkExactAlarmPermission() {
        if (
            Reminders.isEnabled(context) &&
            Reminders.canNotify(context) &&
            Build.VERSION.SDK_INT >= 31 &&
            !Reminders.canUseExactAlarms(context)
        ) {
            showExactAlarmExplanation = true
        }

        Reminders.scheduleNext(context)
    }

    val notificationPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                checkExactAlarmPermission()
            }
        }

    LaunchedEffect(Unit) {
        Reminders.createChannel(context)

        val alreadyAsked = preferences.getBoolean(
            "initial_reminder_permissions_requested",
            false
        )

        if (!alreadyAsked && Reminders.isEnabled(context)) {
            // Vor der Anfrage speichern, damit spätere Starts
            // nicht erneut automatisch nach Berechtigungen fragen.
            preferences.edit()
                .putBoolean(
                    "initial_reminder_permissions_requested",
                    true
                )
                .apply()

            val needsNotificationPermission =
                Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED

            if (needsNotificationPermission) {
                notificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                checkExactAlarmPermission()
            }
        }
    }

    if (showExactAlarmExplanation) {
        AlertDialog(
            onDismissRequest = {
                showExactAlarmExplanation = false
            },
            title = {
                Text("Erinnerung um 6:00 Uhr")
            },
            text = {
                Text(
                    "Die App erinnert dich morgens, wenn für " +
                            "die Vorwoche noch keine Tageswerte vorliegen.\n\n" +
                            "Damit Android die Erinnerung gezielt um " +
                            "6:00 Uhr zustellen kann, erlaube bitte " +
                            "„Wecker und Erinnerungen“ auf der " +
                            "folgenden Einstellungsseite.\n\n" +
                            "Ohne diese Freigabe kann die Erinnerung " +
                            "später eintreffen."
                )
            },
            confirmButton = {
                AppIconButton(
                    icon = Icons.Filled.Alarm,
                    label = "Zeitfreigabe öffnen",
                    onClick = {
                        showExactAlarmExplanation = false

                        if (Build.VERSION.SDK_INT >= 31) {
                            context.startActivity(
                                Intent(
                                    AndroidSettings
                                        .ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse(
                                        "package:${context.packageName}"
                                    )
                                )
                            )
                        }
                    }
                )
            },
            dismissButton = {
                AppIconButton(
                    icon = Icons.Filled.Close,
                    label = "Vorerst überspringen",
                    onClick = {
                        showExactAlarmExplanation = false
                    }
                )
            }
        )
    }
}