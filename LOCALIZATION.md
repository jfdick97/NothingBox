# App languages

NothingBox supports German, English, and the device's language preference. Choose
**the language icon between saved scans and the menu in the dashboard toolbar**. The app uses AppCompat's per-app locale API and
stores the choice across launches, including on Android 8–12. Android 13+ also
exposes the supported languages in system app settings. An unsupported system
language falls back to the German text resources.

## Implementation

- Default text: `app/src/main/res/values/strings.xml` (German).
- English: `app/src/main/res/values-en/strings.xml`.
- Supported languages: `app/src/main/res/xml/locales_config.xml`.
- `LanguageSettings` is shown in a dialog opened by the dashboard language icon.
- `AppStrings` retains only the Application context and resolves resources using
  `ContextCompat.getContextForLanguage`. It works in Compose event callbacks,
  ViewModels, and reminder receivers, including before an Activity is opened.
- The Activity and all native theme variants use AppCompat. Compose still uses
  the existing Material 3 palettes, typography, and edge-to-edge setup.
- `DefaultStrings` is generated from the default XML; it lets the existing pure
  JVM model/backup tests run without Android resources. Runtime Android code uses
  the actual resource system, not this fallback.

Metric positions and factor bit positions are unchanged. Room schema version,
application ID, stored ISO dates, and the ZIP manifest keys/version are unchanged.
User-entered extra-factor names are preserved verbatim. Displayed numbers and
week dates follow the app locale. The paper template and its Monday-based ISO
weeks remain unchanged. CSV keeps its existing column schema, separator, and
machine-readable decimal-point values.

Recognised historical German/English scanner diagnostics are translated when
shown, without rewriting the archived notes. Unrecognised diagnostic text and
external exception messages remain verbatim.

## Editing translations

Keep resource names and positional placeholders identical across languages.
Strings are quoted in XML to preserve spaces and explicit newline escapes.
After editing the default resources, regenerate and check the JVM fallback:

```sh
python3 tools/check_localization.py --update-fallback
python3 tools/check_localization.py
```

For another language, add its `values-…/strings.xml`, a locale-config entry, and a
selector option. Retain the existing metric/factor order.

## Validation

The GitHub workflow checks language coverage and placeholders, builds the app and
instrumentation APK, runs JVM unit tests, and runs Android lint. Instrumentation
assertions in `LocalizationTest` need an emulator or connected phone to execute.

Manual device checks:

1. Switch German → English → system default; reopen the language dialog and restart the app.
2. Verify the choice and its radio button, including when system and app language
   happen to be the same.
3. Check dashboard, stress statistics, trend messages, week picker, scan review,
   import/export/delete dialogs, icon tooltips, and saved scan diagnostics.
4. Verify custom factor names, dates, and values are unchanged after a ZIP round
   trip and a language switch.
5. Enable a reminder and use its test action in each language. Grant notification
   permission first; ensure the previous week has no entries and is not snoozed.
6. Repeat on Android 12 or lower and Android 13+ to cover both locale mechanisms.

The scan review's duplicated week selector was replaced by the intended scanner
notes toggle while localising that screen.
