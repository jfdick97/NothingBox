package de.jfd.straingauge

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

@Composable
fun LanguageSettings(enabled: Boolean = true) {
    var selected by remember {
        mutableStateOf(
            AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = tr(R.string.language_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = tr(R.string.language_description),
            style = MaterialTheme.typography.bodySmall
        )
        listOf(
            "" to R.string.language_system,
            "de" to R.string.language_german,
            "en" to R.string.language_english
        ).forEach { (tag, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = selected == tag,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = {
                            val locales = if (tag.isEmpty()) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(tag)
                            }
                            selected = tag
                            AppCompatDelegate.setApplicationLocales(locales)
                        }
                    )
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = selected == tag,
                    onClick = null,
                    enabled = enabled
                )
                Text(tr(label))
            }
        }
    }
}
