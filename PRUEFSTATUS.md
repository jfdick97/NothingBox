# Prüfstatus — NothingBox 1.3.0

Dokumentationsstand: **3. November 2026**. Geprüfter Quellstand:
`71ae6376335cf2033f9f85ef8387108eddec07b0`
(`versionName = "1.3.0"`, `versionCode = 7`).
Dieser Bericht trennt vorhandene Nachweise von noch auszuführenden Prüfungen.
Die Dokumentationsaktualisierung selbst liefert keine neuen Android-Laufzeitergebnisse.

## Belegt für den 1.3.0-Quellstand

Der [erfolgreiche GitHub-Actions-Lauf vom 24. Oktober 2026](https://github.com/jfdick97/NothingBox/actions/runs/40108482787)
gehört zum obigen Commit. Der [Workflow](.github/workflows/android.yml) führt aus:

- Lokalisierungsprüfung: Sprachabdeckung, Platzhalter, Ressourcenreferenzen,
  XML und Konsistenz des generierten JVM-Fallbacks.
- `:app:assembleDebug` und `:app:assembleDebugAndroidTest`:
  App-APK und Instrumentations-APK kompilieren.
- `:app:testDebugUnitTest` und `:app:lintDebug`:
  JVM-Tests und Android lint.

Auch [PR #1 „Add German and English app languages“](https://github.com/jfdick97/NothingBox/pull/1)
dokumentiert erfolgreiche Build-/CI-Prüfungen und ausdrücklich noch nicht
ausgeführte Instrumentationstests. Er wurde am 24. Oktober 2026 gemergt.
Ein erfolgreicher Build und ein gemergter PR sind keine Gerätefreigabe.

## Noch nicht durch Geräteergebnisse belegt

| Bereich | Vorhandener Umfang | Offener Nachweis |
|---|---|---|
| Android-Integration | Fünf Tests in `IntegrationTest`: BC-02/OpenCV-JNI, OnePager/OpenCV-JNI, ZIP-/Room-Rundlauf ohne doppelte Scans, ungültige Archivpfade und Migration 1→2 | Ausführung auf Android; Host-/JVM-Tests ersetzen JNI und Room-Schemavalidierung nicht |
| Lokalisierung | Drei Tests in `LocalizationTest`: Ressourcenlabels, Platzhalterreihenfolge und JSON-Rundlauf mit benannten Zusatzfaktoren | Ausführung plus manuelle Sprachwahl-/Persistenzprüfung |
| Oberfläche / Review | Lokalisierte Ressourcen und Hinweise-Toggle implementiert | Deutsch/Englisch/Systemsprache, beide Themes, Wochenwahl, Dialoge und reale Kamera |
| Bestandsdaten | Kompatibilitätsregeln im Code und Tests vorhanden | Echtes Altbackup, signaturkompatibles Upgrade und ZIP-Rundlauf mit Namen/Bildern |
| Erinnerungen | Testaktion und Alarm-/Receiver-Pfad implementiert | Berechtigungen, Snooze, echte 06:00-Zustellung und Neustart ohne offene Activity |
| Android-/APK-Abdeckung | minSdk 26, zwei Locale-Verfahren bis API 32 bzw. ab API 33 | Android 12-oder-niedriger und Android 13+ auf Emulator/Telefon; verteilte signierte APK |

**Aktionsplan:** [GERAETEVALIDIERUNG-1.3.md](GERAETEVALIDIERUNG-1.3.md) enthält
Umgebungen, feste Reihenfolge, Testdaten, Befehle, Soll-Ergebnisse und ein
Ergebnisprotokoll. Bis Nachweise vorliegen, bleiben die Zeilen offen.

## Bekannte Abweichung und Grenzen

- Erinnerungsbenachrichtigungen formatieren Datumsbereiche in `Reminders.kt`
  fest als `dd.MM.yyyy`, auch bei englischer App-Sprache. Das weicht von einer
  durchgehend localeabhängigen Datumsanzeige ab. Vor Freigabe korrigieren oder
  ausdrücklich als Einschränkung akzeptieren.
- Der Workflow enthält kein `connectedDebugAndroidTest`; die acht
  Instrumentationstests sind durch seine grüne Anzeige nicht als ausgeführt
  zu verstehen.
- Der Reminder-Testknopf belegt keine reguläre Zustellung um 06:00 Uhr.
  Ohne Freigabe für exakte Alarme kann Android die Zustellung verzögern.
- Die Mindestmatrix deckt zwei Locale-Verfahren ab, aber nicht alle
  unterstützten Android-/Herstellerstände; insbesondere API 26 bleibt ohne
  eigenen Lauf offen. Debug-Prüfungen ersetzen keinen Test der verteilten APK.

## Dokumentationsprüfung vom 3. November 2026

Für diese reine Dokumentationsänderung lokal bestanden:

- 28 relative Dokumentationslinks, ausgeglichene Codeblöcke und die
  PowerShell-Aufrufe geprüft.
- Versions-/Abhängigkeitsangaben mit den Gradle-Dateien und die Testanzahl
  mit dem Quellcode abgeglichen.
- `python3 tools/check_localization.py`: 275 Standardtexte und 272 englische
  Übersetzungen; Platzhalter, Referenzen, XML und JVM-Fallback konsistent.
- `git diff --check`: keine Whitespace-Fehler.

App-Build, JVM-/Host-Regressionen und Gerätetests wurden dafür nicht neu
ausgeführt. Die oben verlinkte CI bleibt der vorhandene Build-Nachweis.

## Historischer Prüfstand 0.2.0 — 18. Oktober 2026

Die folgenden Ergebnisse sind als Historie übernommen. Sie wurden
für diese Dokumentationsaktualisierung nicht erneut ausgeführt und gelten nicht
als aktuelle Gerätevalidierung von 1.3.0.

### Damals abgeschlossen

- Android-App und Android-Testpaket erfolgreich gebaut: `assembleDebug assembleDebugAndroidTest` mit JDK 17, SDK 35 und Build Tools 35.0.0.
- **13 JVM-Datentests bestanden**, keine Fehler und keine übersprungenen Tests. Neue Fälle prüfen benannte Zusatzfaktoren, JSON-Rundlauf, ältere Einträge ohne Zusatznamen, Pflichtnamen, Namenslänge und Umbenennungskonflikte.
- **9 OnePager-Hostfälle bestanden**: bereitgestelltes Foto, Drehung um 90°, Abdunklung, leichte Unschärfe, leeres Formular, synthetische kontinuierliche Werte mit beiden Zusatzzeilen, doppelte Punkte, fehlende L-Marke, fehlender Orientierungspunkt.
- Zusätzlich die rechte PDF-Hälfte als falsche Eingabe geprüft: wird zurückgewiesen.
- Auf dem realen Foto werden alle **28 Punktpositionen** gelesen; die 70 Faktorpositionen ergeben die visuell gegengeprüften Auswahlmuster einschließlich Zusatzzeile 1 am Donnerstag. Die Zahlenvergleiche nutzen eine Toleranz von 0,22 gegenüber ungefähren visuell gegengeprüften Referenzen; das ist keine Aussage über eine garantierte absolute Messgenauigkeit.
- **7 bisherige BC-02-Hostfälle weiterhin bestanden**.
- Die tatsächlichen ALTER-TABLE-Anweisungen der Migration 1→2 auf einer SQLite-Datenbank mit vorhandenen Beispieldaten ausgeführt: alte Werte, NULL, echte Null und Zeitstempel bleiben unverändert; beide neuen Felder erhalten den leeren Standardwert.
- `lintDebug` erfolgreich, keine Fehler. Verbleibende Hinweise betreffen optionale Entwicklungs-/Darstellungsverbesserungen (unter anderem KSP statt kapt, Bitmap-KTX und Launcher-Symbol).
- Das gebaute App-Manifest enthält Kamera und die interne AndroidX-Receiver-Berechtigung, keine Internetberechtigung.
- Erkennungsbild des realen Fotos visuell geprüft; es liegt als `ERKENNUNG_BEISPIEL.jpg` bei.

### Damals noch auf Android auszuführen

Die fünf Android-Integrationstests sind kompiliert und beigelegt, wurden hier aber nicht auf einer Android-Laufzeit ausgeführt. Sie prüfen: alte native Erkennung, neue native OnePager-Erkennung, ZIP-/Room-Rundlauf mit Zusatznamen und ohne doppelte Scans, ungültige Archivpfade sowie die Room-Migration 1→2 mit alten Daten.

Insbesondere die Android-spezifische JNI-Ausführung und die Room-Schemavalidierung nach der Migration sind damit noch nicht als auf einem Gerät bestanden zu verstehen. Die Host-Prüfung des Migrations-SQL ist eine zusätzliche Prüfung, kein Ersatz dafür.

Bitte die neue App auf deinem Smartphone mit dem vorhandenen Foto testen und bei einer neuen Kameraaufnahme die markierten Punkte mit den angezeigten Werten vergleichen. Für den aktuellen Stand gelten [README.md](README.md), [UPDATE-1.3.md](UPDATE-1.3.md) und die [Geräte-Validierungsmatrix](GERAETEVALIDIERUNG-1.3.md). [UPDATE-0.2.md](UPDATE-0.2.md) bleibt als historische Anleitung erhalten.
