# NothingBox – Android-App 1.3.0

Lokale Android-App zum Fotografieren und Auswerten des **MyNothingBoxStrainGauge OnePagers, linke Hälfte**. Dashboard beim Start; **+ unten rechts** öffnet die Kamera. Keine Freitext- oder Datumserkennung, kein Server, keine Internetberechtigung.

**Vorhandene Installation aktualisieren:** zuerst [UPDATE-1.3.md](UPDATE-1.3.md) lesen und eine ZIP-Sicherung exportieren. Ein Update benötigt dieselbe Application-ID und denselben Signierschlüssel; die App auf dem Telefon installiert lassen.

## Stand 1.3.0

- Deutsch, Englisch oder Systemsprache über das **Sprachsymbol zwischen Scans und Menü** im Dashboard. Die Auswahl wird gespeichert; Android 13+ bietet zusätzlich die App-Sprache in den Systemeinstellungen.
- Einblendbare Stress-Statistik mit kombiniertem Stressindex, Mittelwert, Standardabweichung und Verlauf.
- Wochenstatistik, Trendhinweise, synchronisierte Zeitraumauswahl und Erinnerungen an eine noch leere Vorwoche.
- Helles und dunkles Design folgen dem System-Theme.
- Version **1.3.0**, `versionCode = 7`; die Anzeige im Menü kommt aus `BuildConfig.VERSION_NAME`. Beide Versionswerte werden in `app/build.gradle.kts` gepflegt.

Diese Übersicht beschreibt den Funktionsumfang von 1.3.0, keine abgeschlossene Gerätefreigabe. Der [Prüfstatus](PRUEFSTATUS.md) trennt CI-Nachweise von offenen Gerätetests.

| Anleitung | Inhalt |
|---|---|
| [QUICKSTART.txt](QUICKSTART.txt) | Projekt öffnen und erste Auswertung |
| [UPDATE-1.3.md](UPDATE-1.3.md) | Bestehende Installation sicher aktualisieren |
| [LOCALIZATION.md](LOCALIZATION.md) | Sprachwahl, Ressourcen und Übersetzungspflege (Englisch) |
| [GERAETEVALIDIERUNG-1.3.md](GERAETEVALIDIERUNG-1.3.md) | Geordnete Emulator-/Telefonmatrix mit Soll-Ergebnissen |
| [PRUEFSTATUS.md](PRUEFSTATUS.md) | Belegte Prüfungen und verbleibende Lücken |

## Start mit Android Studio

1. Repository klonen oder Quellcodearchiv vollständig entpacken.
2. Android Studio → Open → Projektordner mit `settings.gradle.kts` öffnen (bei einem Clone üblicherweise `NothingBox`).
3. Gradle-Sync abwarten. Der erste Build auf dem Rechner benötigt Internet zum Herunterladen der Bibliotheken; die fertige App arbeitet offline.
4. Bei Bedarf SDK Platform **35** und Build Tools **35.0.0** im SDK Manager installieren. Gradle-JVM: **JDK 17 oder 21**. Versionen von AGP/Kotlin zum Einstieg unverändert lassen.
5. Smartphone mit aktiviertem USB-Debugging per Datenkabel verbinden, Debugging-Anfrage bestätigen, als Gerät auswählen und **Run → app** drücken.
6. Ohne Ausdruck: **Menü → Künstliches Testblatt auswerten**. Erst „Woche speichern“ legt Einträge an. Die Namen der synthetischen Zusatzfaktoren sind im Demo-Modus vorbelegt.

Das Projekt läuft ab Android 8/API 26. Windows: Gegebenenfalls Hersteller-USB-Treiber installieren, wenn das Gerät nicht erkannt wird.

## Unterstützte Vorlagen

- **`OnePager.pdf`**: neue englische Vorlage mit L-Ecken und Orientierungspunkt. Nur die linke Hälfte wird ausgewertet. Möglichst eben, vollständig und scharf aufnehmen. Der kleine Orientierungspunkt gehört oben links zur linken Seite.
- **`Arbeitsblatt.pdf`**: frühere BC-02-Vorlage mit vier ArUco-Markern. Sie wird weiterhin automatisch erkannt; nur Seite 1 ist relevant.

Die freie Position eines Punktes im gestrichelten OnePager-Kästchen liefert einen kontinuierlichen Wert 0–10. Die Höhe des Punktes im Kästchen beeinflusst den Wert nicht. Bei BC-02 wird weiterhin der ausgefüllte Kreis 0–10 gelesen.

Weitere Layouts sind nicht automatisch kompatibel. Alle Geometrien liegen als Profile in `app/src/main/assets`.

## Scan-Ablauf

1. **+** öffnen; Kamera verwenden oder vorhandenes Foto wählen.
2. Die App richtet die Seite aus und markiert erkannte Punkte/Kreise. Grün bedeutet erkannt, Orange mehrdeutig. Ein leeres Feld bleibt NULL und wird nicht zur Zahl null.
3. Original oder Erkennungsbild antippen: mit zwei Fingern zoomen und verschieben.
4. Über die Wochenwahl den Montag der Papierwoche auswählen. Intern wird das Datum als `JJJJ-MM-TT` gespeichert; es wird nicht aus Handschrift gelesen.
5. Die Tage Mo–So durchgehen; Skalenwerte und Belastungsfaktoren prüfen beziehungsweise korrigieren.
6. Namen für benutzte Zusatzzeilen manuell eintragen. Zwei Namensfelder gelten beim Speichern für die Woche und werden an jedem erfassten Tag gespeichert. Dadurch können spätere Wochen dieselbe Zusatzzeile anders benennen, ohne alte Daten umzubenennen.
7. Speichern. Nur Tage mit mindestens einem Skalenwert oder Faktor werden übernommen. Bereits vorhandene Tage erfordern eine ausdrückliche Entscheidung: beibehalten oder den ganzen Tag ersetzen, einschließlich leerer Skalenfelder.

Die automatische Punktmessung speichert Double-Werte mit zwei berechneten Nachkommastellen; die Anzeige und manuellen Regler arbeiten mit einer Nachkommastelle. Diese Formatierung gibt keine garantierte Messgenauigkeit vor. Bei Rahmenberührung, schwachen oder mehreren Markierungen werden Hinweise angezeigt. Die Erkennung ist klassische Bildverarbeitung ohne KI-Dienst.

## Dashboard und gespeicherte Daten

- Vier zeitlich ausgerichtete Diagramme mit gleicher Skala 0–10.
- Zeiträume 4 Wochen, 3 Monate, alles; fehlende Werte und Tage erzeugen Lücken.
- Mittelwerte und letzter Messwert je Größe.
- Separate Wochenmittelwerte mit Auswahl der Kalenderwoche, Anzahl erfasster Werte und Standardabweichung; diese wird ab zwei vorhandenen Werten berechnet.
- Einblendbare Stress-Statistik unter den Diagrammen; dieselbe Zeitraumauswahl gilt für Diagramme, Stress-Statistik und Belastungsfaktoren.
- Trendhinweise betrachten die drei abgeschlossenen Vorwochen. Je Größe sind mindestens vier Werte in jeder dieser Wochen erforderlich.
- Häufigkeit der festen und benannten Zusatzfaktoren im gewählten Zeitraum.
- Tageskarten öffnen die Bearbeitung. Zusatznamen in dieser Ansicht gelten für den bearbeiteten Tag. Tage können gelöscht werden.
- **Scans** zeigt Original, entzerrtes Foto und Erkennungshinweise. Scan-Fotos können separat gelöscht werden; Tageswerte bleiben dann ohne Bildverweis erhalten.

## Sprache und Darstellung

Das **Sprachsymbol zwischen Scans und Menü** öffnet Deutsch, Englisch und Systemsprache. Bei Systemsprache folgt die App der Gerätesprache; nicht unterstützte Systemsprachen verwenden die deutschen Texte. Eine explizite Auswahl bleibt auch dann erhalten, wenn sie gerade mit der Systemsprache übereinstimmt. Android 8–12 verwendet die AppCompat-Speicherung, Android 13+ zusätzlich die systemweite App-Sprachintegration.

Zahlen und Datumsanzeigen der Oberfläche folgen der App-Sprache. Benannte Zusatzfaktoren bleiben exakt so gespeichert und angezeigt, wie sie eingegeben wurden. Erkannte historische deutsche/englische Scannerhinweise werden nur zur Anzeige übersetzt; unbekannte Hinweise und externe Fehlermeldungen bleiben unverändert. Papierlayout, Montag als Wochenbeginn und das maschinenlesbare Speicher-/Exportformat ändern sich nicht.

**Bekannte Ausnahme:** Die Datumsbereiche in Erinnerungsbenachrichtigungen verwenden aktuell auch unter Englisch `dd.MM.yyyy`. Die Abweichung ist im [Prüfstatus](PRUEFSTATUS.md) festgehalten.

## Stressindex

Die einblendbare Stress-Statistik berechnet für jeden vollständig erfassten Tag:

```text
Stressindex = (Anforderungen + Anspannung + (10 − Energie) + (10 − Handlungsspielraum)) / 4
```

Der Index liegt zwischen 0 und 10; höhere Werte stehen für höhere kombinierte Belastung. Fehlt eine der vier Angaben, wird für diesen Tag kein Index berechnet. Mittelwert und Standardabweichung beziehen sich auf die vollständigen Tage im gewählten Zeitraum. Beispiel: 4, 3, 7, 8 ergibt einen Index von 3,0.

## Erinnerungen

Unter **Menü → Erinnerungen** lassen sich tägliche Erinnerungen aktivieren/deaktivieren und über **Erinnerung jetzt prüfen** testen. Die Funktion ist standardmäßig aktiviert; Android- und Kanalberechtigungen müssen Benachrichtigungen erlauben. Ab Android 13 ist zusätzlich die Benachrichtigungsberechtigung erforderlich.

Die App prüft täglich um 06:00 Uhr in der Gerätezeitzone, ob die gesamte Vorwoche noch ohne Einträge ist. Bereits ein gespeicherter Tag in der Vorwoche unterdrückt die Erinnerung; es wird nicht auf sieben vollständige Tage geprüft. **Für diese Woche pausieren** unterdrückt weitere Erinnerungen für die aktuelle Kalenderwoche.

Ab Android 12 kann für die genaue Alarmzeit eine zusätzliche Freigabe nötig sein. Ohne sie verwendet die App einen ungenauen Alarm; Android kann die Zustellung verzögern. Die Testaktion prüft den Benachrichtigungspfad, aber nicht die tatsächliche Zustellung im Hintergrund um 06:00 Uhr.

## Export und Import

**Menü → Vollständige Sicherung exportieren** schreibt eine ZIP-Datei:

- `manifest.json`: Formatversion, Werte einschließlich NULL, Zusatznamen, ursprüngliche Erkennung, Scan-Zuordnung, Formular-ID, Hinweise und Zeitstempel.
- `images/`: Original und entzerrtes Foto je Scan.
- SHA-256-Prüfsummen für die Bilder.

Neue Sicherungen verwenden Format **2**. Der Import unterstützt Format **1 und 2**; Version 0.1 der App kann Format 2 nicht lesen.

Der Import prüft Format, Wertebereiche, Datums-Duplikate, Bildverweise, Pfade, Größenlimits und Prüfsummen. Eine Vorschau nennt neue und vorhandene Tage und zeigt abweichende Daten. Standardmäßig bleiben vorhandene Tage erhalten. Identische Fotos/Scans werden beim erneuten Import wiedererkannt; Scans ohne verbleibende Tageswerte werden ebenfalls wiederhergestellt. Datenbankänderungen erfolgen in einer Transaktion.

**CSV exportieren** ist für Python/Excel: UTF-8 mit BOM, Semikolon, Dezimalpunkt, eine Zeile pro Tag. Faktorennamen einschließlich der Zusatzzeilen stehen im Textfeld. Fehlende Werte sind leere Zellen. Für die Wiederherstellung die ZIP-Sicherung verwenden, nicht CSV.

```python
import pandas as pd
werte = pd.read_csv("Wochenblick.csv", sep=";", encoding="utf-8-sig")
werte["Datum"] = pd.to_datetime(werte["Datum"])
```

Sicherungen sind unverschlüsselt. Den Zielort wählst du im Android-Dateidialog; die App selbst lädt nichts ins Internet hoch. Automatische Android-Cloud-Backups und die Übertragung der App-Daten per Android-Datentransfer sind ausgeschlossen. Vor Deinstallation oder Handywechsel bewusst exportieren.

Unterstützte Sicherungsgröße: bis 250 MB entpackt, 25 MB je Foto, 8 MB Manifest, 10.000 Bilder. Export und Import prüfen dieselben Grenzen.

## Erkennung und Grenzen

Die OnePager-Erkennung läuft vollständig mit OpenCV auf dem Gerät:

1. Gedruckte Merkmale mit ORB vergleichen und eine RANSAC-Homographie bestimmen.
2. Alle vier L-Marken in lokalen Suchbereichen bestätigen und zur Verfeinerung benutzen.
3. Orientierungspunkt oben links und Skalenlayout prüfen.
4. Jedes Skalenfeld lokal an der gedruckten Vorlage ausrichten; Punktkomponenten erkennen und ihre horizontale Position auswerten.
5. Zehn Faktorzeilen je Tag anhand der Kreisfüllung lesen.

Stark gewölbtes Papier, Falten, Bewegungsunschärfe, sehr kleine Punkte und starke Schatten bleiben mögliche Fehlerquellen. Nicht gespeicherte Scans sind temporär; nach Beenden des App-Prozesses müssen sie neu geöffnet werden. Die automatische Erkennung handschriftlicher Zusatznamen und Datumswerte ist bewusst nicht enthalten.

`ERKENNUNG_BEISPIEL.jpg` zeigt die Host-Auswertung deines bereitgestellten Fotos. Das Original befindet sich als Regressionseingabe unter `tools/testdata/onepager-photo.jpg`. Das in der App aufrufbare Beispiel unter `assets/onepager_sample.jpg` ist dagegen synthetisch und enthält keine abfotografierte Handschrift.

## Projektstruktur

| Datei | Aufgabe |
|---|---|
| `OnePagerScanner.kt` | Neue Registrierung, L-Marken, kontinuierliche Punkte; automatische Auswahl des alten Scanners |
| `Scanner.kt` | Frühere BC-02-Auswertung und gemeinsame Bild-/EXIF-Hilfen |
| `Data.kt` | Room-Datenbank, additive Migration 1→2, Datenvalidierung und Konfliktregeln |
| `Backup.kt` | CSV, ZIP-Format 2, Import der Formate 1/2, Validierung und Prüfsummen |
| `AppModel.kt` | Scan-Prüfung, Zusatznamen und Speicherung |
| `MainActivity.kt` | Dashboard, Wochenmittel, Prüfansicht, Eintragsbearbeitung, Import/Export-Menü |
| `CameraScreen.kt` | CameraX und Kameraberechtigung |
| `assets/onepager_profile.json` | Exakte Feld- und Markenpositionen aus der neuen PDF |
| `tools/onepager_recognition.py` | Host-Referenzalgorithmus für Diagnose und Regression |
| `Statistics.kt` / `TrendCard.kt` | Stressindex, Streuung und Trendbewertung |
| `LanguageSettings.kt` / `AppStrings.kt` | Persistente App-Sprachwahl und localeabhängige Ressourcen |
| `res/values/strings.xml` / `res/values-en/strings.xml` | Deutsche und englische Texte |
| `Reminders.kt` / `ReminderReceiver.kt` | Alarmplanung, Benachrichtigungen und Wochenpause |
| `tools/check_localization.py` | Sprachabdeckung, Platzhalter und generierter JVM-Fallback |

## Tests und Build

Windows im Android-Studio-Terminal:

```powershell
py -3 tools/check_localization.py
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Linux/macOS:

```bash
python3 tools/check_localization.py
bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Die APK entsteht unter `app/build/outputs/apk/debug/app-debug.apk`. Bei eigener dauerhafter Verteilung einen eigenen Release-Signierschlüssel aufbewahren. Das Projektarchiv enthält Quellcode, keine vorgefertigte APK mit einem fremden Signierschlüssel.

Host-Tests:

```bash
python -m pip install numpy opencv-python-headless
python tools/verify_onepager.py
python tools/verify_recognition.py
python tools/verify_migration.py
```

Die OnePager-Tests prüfen dein Foto, Drehung, Abdunklung, leichte Unschärfe, leere Felder, synthetische kontinuierliche Werte, beide Zusatzzeilen, doppelte Punkte sowie fehlende Marken/Punkt. Die Zahlenreferenzen für das reale Foto sind ungefähre visuell gegengeprüfte Vergleichswerte, keine Messnormal-Kalibrierung. Das Host-Skript bildet den Android-Algorithmus nach; es ersetzt keinen ausgeführten Android-JNI-Test.

Android-Tests unter `app/src/androidTest` umfassen **fünf Integrationstests** (native Erkennung, ZIP/Room, Archivpfade und Migration 1→2) sowie **drei Lokalisierungstests** (Ressourcen, Platzhalter und JSON-Rundlauf mit Zusatznamen). `assembleDebugAndroidTest` kompiliert sie lediglich.

Auf genau einem verbundenen Testgerät/-emulator ausführen:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Unter Linux/macOS: `bash gradlew :app:connectedDebugAndroidTest`. Berichte liegen unter `app/build/reports/androidTests/connected/` und `app/build/outputs/androidTest-results/connected/`. Die GitHub-CI führt diesen Gerätetask derzeit nicht aus. Die [Geräte-Validierungsmatrix](GERAETEVALIDIERUNG-1.3.md) ergänzt die Tests um Locale-Persistenz, Kamera, Bestandsdaten und Erinnerungen; der belegte Stand steht in [PRUEFSTATUS.md](PRUEFSTATUS.md).

## Erste Prüfung auf deinem Smartphone

Dies ist ein kurzer Funktionstest. Für die vollständige 1.3.0-Prüfung anschließend die [Geräte-Validierungsmatrix](GERAETEVALIDIERUNG-1.3.md) verwenden.

1. Menü → künstliches Testblatt öffnen; abbrechen: keine Datenänderung.
2. Dein Foto über **+ → Vorhandenes Foto öffnen** auswählen und die Prüfansicht mit `ERKENNUNG_BEISPIEL.jpg` vergleichen.
3. Woche ab 2026-10-12 auswählen und Namen der ersten Zusatzzeile eintragen, Zahlen und Faktoren prüfen und speichern.
4. App schließen/öffnen: Werte bleiben erhalten; benannter Zusatzfaktor erscheint im Dashboard.
5. ZIP und CSV exportieren; ZIP wieder importieren: keine neuen doppelten Scans.
6. Einen Wert ändern und Sicherung importieren: Konflikt muss sichtbar sein; Ersetzen stellt den gesicherten Wert her.
7. Ein eigenes neues Foto aufnehmen und im Flugmodus wiederholen.

## Werkzeugversionen

Kotlin 2.0.21, AGP 8.9.2, Gradle 8.11.1, Compose BOM 2024.12.01, Room 2.6.1, CameraX 1.4.1, OpenCV 4.12.0, AppCompat 1.7.1. Maßgeblich sind die eingecheckten Gradle-Dateien; für den Einstieg keine zusätzlichen Versions-Upgrades durchführen. Die OpenCV-Android-Bibliothek wird vorgefertigt eingebunden; kein eigenes NDK-Projekt ist erforderlich.
