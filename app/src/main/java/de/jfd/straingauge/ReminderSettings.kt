package de.jfd.straingauge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ReminderSettings() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    var enabled by remember {
        mutableStateOf(Reminders.isEnabled(context))
    }

    var notificationsAllowed by remember {
        mutableStateOf(Reminders.canNotify(context))
    }

    var exactAllowed by remember {
        mutableStateOf(Reminders.canUseExactAlarms(context))
    }

    var paused by remember {
        mutableStateOf(Reminders.isPausedThisWeek(context))
    }

    fun refresh() {
        enabled = Reminders.isEnabled(context)
        notificationsAllowed = Reminders.canNotify(context)
        exactAllowed = Reminders.canUseExactAlarms(context)
        paused = Reminders.isPausedThisWeek(context)
    }

    fun openNotificationSettings() {
        context.startActivity(
            Intent(
                AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra(
                AndroidSettings.EXTRA_APP_PACKAGE,
                context.packageName
            )
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Reminders.setEnabled(context, true)
            }

            refresh()
        }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }

        owner.lifecycle.addObserver(observer)

        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Erinnerungen",
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("An fehlenden Wochenrückblick erinnern")

                Text(
                    text = "Täglich um 6:00 Uhr, " +
                            "solange die Vorwoche noch nicht erfasst ist.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    if (!checked) {
                        Reminders.setEnabled(context, false)
                    } else if (
                        Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else if (!Reminders.canNotify(context)) {
                        openNotificationSettings()
                    } else {
                        Reminders.setEnabled(context, true)
                    }

                    refresh()
                }
            )
        }

        if (!notificationsAllowed) {
            Text(
                text = "Benachrichtigungen müssen in Android " +
                        "erlaubt sein. Nach einer Ablehnung kannst du " +
                        "sie über das Glocken-Symbol freigeben.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (enabled && paused) {
            Text(
                text = "Für diese Kalenderwoche pausiert. " +
                        "Ab nächstem Montag wieder aktiv.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (enabled && !exactAllowed) {
            Text(
                text = "Ohne Freigabe für genaue Alarme kann " +
                        "Android die Erinnerung nach 6:00 Uhr zustellen. " +
                        "Über das Wecker-Symbol kannst du die " +
                        "Zeitfreigabe erteilen.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppIconButton(
                icon = Icons.Filled.Notifications,
                label = "Android-Benachrichtigungseinstellungen",
                onClick = {
                    openNotificationSettings()
                }
            )

            if (
                enabled &&
                !exactAllowed &&
                Build.VERSION.SDK_INT >= 31
            ) {
                AppIconButton(
                    icon = Icons.Filled.Alarm,
                    label = "Genaue Erinnerungszeit erlauben",
                    onClick = {
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
                )
            }

            if (enabled) {
                AppIconButton(
                    icon = Icons.Filled.PlayArrow,
                    label = "Erinnerung jetzt prüfen",
                    onClick = {
                        context.sendBroadcast(
                            Intent(
                                context,
                                ReminderReceiver::class.java
                            ).setAction(
                                Reminders.ACTION_CHECK
                            )
                        )
                    }
                )
            }
        }
    }
}
