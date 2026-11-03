# App languages — NothingBox 1.3.0

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
week dates in the UI follow the app locale. **Known exception:** reminder
notification date ranges currently use the fixed pattern `dd.MM.yyyy`, including
in English. This remains an open localisation discrepancy; see
[PRUEFSTATUS.md](PRUEFSTATUS.md). The paper template and its Monday-based ISO
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
instrumentation APK, runs JVM unit tests, and runs Android lint. It does **not**
run instrumentation tests. The successful CI run for the 1.3.0 source commit
`71ae6376335cf2033f9f85ef8387108eddec07b0` is linked in
[PRUEFSTATUS.md](PRUEFSTATUS.md).

The instrumentation suite currently contains five tests in `IntegrationTest`
and three in `LocalizationTest`. The latter check German/English resource
labels, diagnostic argument order, and custom names in a JSON round trip.
They do not exercise locale selection/persistence or the complete UI.

With exactly one disposable test emulator/phone connected, run:

```sh
bash gradlew :app:connectedDebugAndroidTest
```

Windows PowerShell: `.\gradlew.bat :app:connectedDebugAndroidTest`.
Save the reports in `app/build/reports/androidTests/connected/` and
`app/build/outputs/androidTest-results/connected/`.

Follow [GERAETEVALIDIERUNG-1.3.md](GERAETEVALIDIERUNG-1.3.md) for the ordered
device matrix, fixtures, expected results and evidence template (German).
It covers Android 12-or-lower and Android 13+, locale persistence, scan review,
existing backups/custom names and reminder notifications. Emulator checks are
separated from real-camera and physical-phone background-delivery checks.
All device results remain open until executed and recorded.

The scan review's duplicated week selector was replaced by the intended scanner
notes toggle while localising that screen. Include that toggle in review tests.
