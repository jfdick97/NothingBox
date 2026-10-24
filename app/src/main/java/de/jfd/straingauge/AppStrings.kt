package de.jfd.straingauge

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.util.Locale

/** Resource access shared by Compose callbacks, the model, and background receivers.
 * Only the Application context is retained; never an Activity or a View. */
object AppStrings {
    @Volatile
    private var application: Context? = null

    internal val context: Context? get() = application

    fun initialize(context: Context) {
        application = context.applicationContext
    }

    val locale: Locale
        get() = application?.let {
            ContextCompat.getContextForLanguage(it)
                .resources.configuration.locales[0]
        } ?: Locale.GERMANY

    fun text(@StringRes id: Int, vararg args: Any?): String {
        val context = application
        if (context != null) {
            return ContextCompat.getContextForLanguage(context)
                .getString(id, *args)
        }
        // Model/backup JVM tests intentionally have no Android Application.
        val default = DefaultStrings.values.getValue(id)
        return if (args.isEmpty()) default
        else String.format(Locale.GERMANY, default, *args)
    }
}

internal fun tr(@StringRes id: Int, vararg args: Any?): String =
    AppStrings.text(id, *args)

class NothingBoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppStrings.initialize(this)
    }
}
