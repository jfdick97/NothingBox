package de.jfd.straingauge

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    private fun context(language: String): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(app.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return app.createConfigurationContext(configuration)
    }

    @Test fun dashboardLabelsHaveGermanAndEnglishResources() {
        val de = context("de")
        val en = context("en")
        assertEquals("Anforderungen", de.getString(R.string.metric_demands))
        assertEquals("Demands", en.getString(R.string.metric_demands))
        assertEquals("Stress-Statistik", de.getString(R.string.ui_stress_statistics))
        assertEquals("Stress statistics", en.getString(R.string.ui_stress_statistics))
        assertEquals("Systemsprache", de.getString(R.string.language_system))
        assertEquals("System default", en.getString(R.string.language_system))
    }

    @Test fun diagnosticArgumentsKeepTheirOrder() {
        val en = context("en")
        val note = en.getString(
            R.string.scan_scale_field_could_not_be_recognised_reliably,
            "Mon", "Energy"
        )
        assertEquals("Mon, Energy: scale field could not be recognised reliably.", note)
    }

    @Test fun customFactorNamesAreNeverTranslated() {
        val original = DayEntry(
            date = "2026-10-12", demands = 4.0, tension = 3.0,
            energy = 7.0, agency = 8.0, factors = 768,
            custom1 = "Meetings", custom2 = "Familie"
        )
        val json = org.json.JSONArray().put(BackupJson.day(original))
        val restored = BackupJson.parseDays(json).single()
        assertEquals(original, restored)
        assertEquals(listOf("Meetings", "Familie"), restored.selectedFactors())
    }
}
