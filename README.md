# Eine Woche im Blick – Android-App 0.2

Lokale Android-App zum Fotografieren und Auswerten des **MyNothingBoxStrainGauge OnePagers, linke Hälfte**. Dashboard beim Start; **+ unten rechts** öffnet die Kamera. Keine Freitext- oder Datumserkennung, kein Server, keine Internetberechtigung.

**Du hast Version 0.1 bereits installiert? Zuerst `UPDATE-0.2.md` lesen.** Ein Update mit demselben Signierschlüssel erhält die Daten; die App auf dem Telefon installiert lassen.

## Start mit Android Studio

1. Archiv vollständig entpacken.
2. Android Studio → Open → Ordner `StrainGauge` mit `settings.gradle.kts` öffnen.
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
4. Den Montag der Papierwoche als `JJJJ-MM-TT` eintragen. Das Datum wird nicht aus Handschrift gelesen.
5. Die Tage Mo–So durchgehen; Skalenwerte und Belastungsfaktoren prüfen beziehungsweise korrigieren.
6. Namen für benutzte Zusatzzeilen manuell eintragen. Zwei Namensfelder gelten beim Speichern für die Woche und werden an jedem erfassten Tag gespeichert. Dadurch können spätere Wochen dieselbe Zusatzzeile anders benennen, ohne alte Daten umzubenennen.
7. Speichern. Nur Tage mit mindestens einem Skalenwert oder Faktor werden übernommen. Bereits vorhandene Tage erfordern eine ausdrückliche Entscheidung: beibehalten oder den ganzen Tag ersetzen, einschließlich leerer Skalenfelder.

Die automatische Punktmessung speichert Double-Werte mit zwei berechneten Nachkommastellen; die Anzeige und manuellen Regler arbeiten mit einer Nachkommastelle. Diese Formatierung gibt keine garantierte Messgenauigkeit vor. Bei Rahmenberührung, schwachen oder mehreren Markierungen werden Hinweise angezeigt. Die Erkennung ist klassische Bildverarbeitung ohne KI-Dienst.

## Dashboard und gespeicherte Daten

- Vier zeitlich ausgerichtete Diagramme mit gleicher Skala 0–10.
- Zeiträume 4 Wochen, 3 Monate, alles; fehlende Werte und Tage erzeugen Lücken.
- Mittelwerte und letzter Messwert je Größe.
- Separate Wochenmittelwerte mit Auswahl der Kalenderwoche und Anzahl erfasster Werte.
- Häufigkeit der festen und benannten Zusatzfaktoren im gewählten Zeitraum.
- Tageskarten öffnen die Bearbeitung. Zusatznamen in dieser Ansicht gelten für den bearbeiteten Tag. Tage können gelöscht werden.
- **Scans** zeigt Original, entzerrtes Foto und Erkennungshinweise. Scan-Fotos können separat gelöscht werden; Tageswerte bleiben dann ohne Bildverweis erhalten.

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

Die neue Erkennung läuft vollständig mit OpenCV auf dem Gerät:

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

## Tests und Build

Windows im Android-Studio-Terminal:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

Linux/macOS:

```bash
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
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

Android-Integrationstests liegen unter `app/src/androidTest`: alte und neue native Erkennung, ZIP-/Room-Rundlauf ohne Import-Duplikate, ungültige Archivpfade und Datenmigration 1→2. Auf einem verbundenen Gerät: `./gradlew connectedDebugAndroidTest` beziehungsweise `gradlew.bat` unter Windows. Der tatsächlich erreichte Prüfstand steht in `PRUEFSTATUS.md`.

## Erste Prüfung auf deinem Smartphone

1. Menü → künstliches Testblatt öffnen; abbrechen: keine Datenänderung.
2. Dein Foto über **+ → Vorhandenes Foto öffnen** auswählen und die Prüfansicht mit `ERKENNUNG_BEISPIEL.jpg` vergleichen.
3. Woche 2026-10-12 und Namen der ersten Zusatzzeile eintragen, Zahlen und Faktoren prüfen und speichern.
4. App schließen/öffnen: Werte bleiben erhalten; benannter Zusatzfaktor erscheint im Dashboard.
5. ZIP und CSV exportieren; ZIP wieder importieren: keine neuen doppelten Scans.
6. Einen Wert ändern und Sicherung importieren: Konflikt muss sichtbar sein; Ersetzen stellt den gesicherten Wert her.
7. Ein eigenes neues Foto aufnehmen und im Flugmodus wiederholen.

## Werkzeugversionen

Kotlin 2.0.21, AGP 8.9.2, Gradle 8.11.1, Compose BOM 2024.12.01, Room 2.6.1, CameraX 1.4.1, OpenCV 4.10.0. Für die Aktualisierung ist kein Wechsel dieser Versionen nötig. Die OpenCV-Android-Bibliothek wird vorgefertigt eingebunden; kein eigenes NDK-Projekt ist erforderlich.
