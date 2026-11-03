# Gerätevalidierung — NothingBox 1.3.0

**Status: geplant, nicht ausgeführt.** Diese Matrix ist kein Nachweis bestandener
Gerätetests. Der belegte Stand und die bekannten Lücken stehen in
[PRUEFSTATUS.md](PRUEFSTATUS.md). Spracharchitektur:
[LOCALIZATION.md](LOCALIZATION.md).

## 0. Teststand und Daten vorbereiten

- Zu prüfenden Commit und APK-SHA-256 notieren; Version **1.3.0 / Code 7** prüfen.
  Dokumentierter Quellstand: `71ae6376335cf2033f9f85ef8387108eddec07b0`.
  Debug-Testläufe und die Prüfung der tatsächlich verteilten signierten APK
  getrennt protokollieren.
- Ausschließlich Testinstallationen oder gesicherte Testtelefone verwenden.
  Genau einen Emulator/ein Telefon pro Instrumentationslauf verbinden.
- Echtes ZIP aus einer Version vor 1.3.0 vorbereiten: bekannte ISO-Daten,
  Nullwert und fehlender Wert, Dezimalwerte, Faktorzuordnungen, Zusatznamen
  (z. B. „Meetings“, „Familie“ und ein Name mit Umlaut), Scanbilder und
  gespeicherte Diagnosen. Erwartete Inhalte vorab protokollieren.
- Für Upgrade-Tests alte APK, ihren Versionscode und denselben Signierschlüssel
  bereithalten. Nicht durch Deinstallation einen fehlgeschlagenen Upgrade-Test
  in einen Neuinstallationstest verwandeln.
- Referenzbild und Papierbogen mit bekannten Punkten/Faktoren vorbereiten.
  Für Trendtexte Daten für drei abgeschlossene Wochen mit mindestens vier
  Werten je Größe und Woche bereitstellen.
- Für Reminder einen separaten Ausgangsstand anlegen: Vorwoche vollständig
  leer, aktuelle Woche nicht pausiert, Reminder aktiviert. Vor jedem
  Negativfall diesen Stand wiederherstellen. Ein Ausschalten/Einschalten des
  Reminders löscht eine Wochenpause nicht.
- Gerätezeitzone und Testdatum protokollieren; für Reminder bedeutet Vorwoche
  den letzten abgeschlossenen Montag–Sonntag-Zeitraum.

## Umgebungen und feste Reihenfolge

| Kennung | Umgebung | Zweck |
|---|---|---|
| E12 | Emulator Android 12 / API 31 | AppCompat-Locale-Persistenz bis Android 12 |
| E13 | Emulator Android 13 / API 33 | System-App-Sprachen und Benachrichtigungsberechtigung |
| P12 | Physisches Telefon Android 8–12; konkrete API notieren | Reale Kamera und Hintergrundzustellung des älteren Pfads |
| P13 | Physisches Telefon Android 13+; konkrete API notieren | Reale Kamera, Berechtigungen und Hintergrundzustellung des neueren Pfads |

**Reihenfolge:** E12 Schritte 1–6 vollständig, danach E13 Schritte 1–6,
danach P12 Schritte 7–9, zuletzt P13 Schritte 7–9. Auf jedem Emulator zuerst
den Neuinstallationslauf 1–5 durchführen; Schritt 6 ist ein separater
Upgrade-/Backup-Lauf. Fehlt eine Umgebung, als **BLOCKIERT** erfassen.

## Emulatoren: Schritte 1–6

Die Prüfungen sind auch auf Testtelefonen ausführbar; die Emulatoren erlauben
reproduzierbare Ausgangszustände. Sie ersetzen keine reale Kameraaufnahme oder
OEM-spezifische Prüfung der Hintergrundzustellung.

| Nr. | Genaue Ausführung | Erwartetes Ergebnis |
|---|---|---|
| 1 | Bei genau einem verbundenen Emulator `bash gradlew :app:connectedDebugAndroidTest` bzw. unter Windows `.\gradlew.bat :app:connectedDebugAndroidTest` ausführen. | Alle **acht** Tests bestehen: fünf in `IntegrationTest`, drei in `LocalizationTest`; HTML-/XML-Bericht sichern. |
| 2 | System auf Deutsch stellen. Im Dashboard über das Sprachsymbol zwischen Scans und Menü **Deutsch → Englisch → Systemsprache** wählen. Nach jeder Wahl den Sprachdialog neu öffnen, App verlassen, mit `adb shell am kill de.jfd.straingauge` den Hintergrundprozess beenden und App neu öffnen. | Oberfläche und Auswahl bleiben korrekt. Auch bei gleicher App-/Systemsprache bleibt der tatsächlich gewählte Radiobutton erhalten; „Systemsprache“ wird nicht durch „Deutsch“ ersetzt. |
| 3 | Im Systemmodus System **Deutsch → Englisch → Französisch** wechseln; jeweils App und Sprachdialog öffnen. Danach explizit Deutsch wählen und System auf Englisch setzen. Auf E13 anschließend in Androids App-Sprachen Englisch wählen und in der App zurück auf Systemsprache wechseln. | Systemmodus folgt DE/EN; Französisch zeigt deutsche Texte. Explizites Deutsch bleibt bei englischem System Deutsch. E13-Systemauswahl und App-Auswahl stimmen überein. |
| 4 | Je auf Deutsch und Englisch nacheinander Dashboard, Stress-Statistik, Wochenstatistik, Trendtexte, Wochenwahl, Import-/Export-/Löschdialoge und Icon-Tooltips prüfen; Löschdialog abbrechen. Mit Tageswerten 4/3/7/8 den Stressindex prüfen und einen unvollständigen Tag vergleichen. System-Theme hell → dunkel wechseln. | Texte, Zahlen und UI-Daten folgen der Sprache; keine fehlenden Texte, rohen Platzhalter oder unbedienbaren Dialoge. Stressindex **3,0** bzw. **3.0**; unvollständiger Tag liefert keinen Index. Zeiträume bleiben synchron; beide Themes sind lesbar. |
| 5 | Referenzbild aus Galerie importieren → Review → Hinweise ein-/ausblenden → Papierwoche wählen → Werte/Faktoren prüfen → speichern → gespeicherten Scan öffnen. Auf Englisch wiederholen; bekannte historische und unbekannte Diagnosen vergleichen. | Genau eine beabsichtigte Wochenwahl; Hinweise-Toggle funktioniert. Gespeicherte Woche/Werte stimmen mit Vorlage bzw. Korrektur überein. Bekannte Diagnosen werden nur bei Anzeige übersetzt; unbekannte Texte bleiben unverändert. |
| 6 | Separater Teststand: alte APK mit Testdaten installieren → mit derselben Signatur 1.3.0 darüberinstallieren → Daten vergleichen. Altes ZIP importieren → Sprache wechseln → ZIP und CSV exportieren → ZIP in leerer Testinstallation importieren → dasselbe ZIP erneut importieren. | Upgrade erhält Bestandsdaten. ZIP erhält Namen, ISO-Daten, Werte, Faktoren, Bilder und archivierte Hinweise; keine doppelten Scans beim erneuten Import. CSV behält Schema, Semikolon, Dezimalpunkt und leere Felder für fehlende Werte. |

Berichte aus Schritt 1:
`app/build/reports/androidTests/connected/` und
`app/build/outputs/androidTest-results/connected/`.
Bei abweichender Testanzahl zunächst Änderung/Filterung des Testumfangs klären.

## Physische Telefone: Schritte 7–9

| Nr. | Genaue Ausführung | Erwartetes Ergebnis |
|---|---|---|
| 7 | Auf dem Telefon zuerst Schritt 2 wiederholen. Papierbogen auf Deutsch fotografieren → Review → Hinweise-Toggle → Woche wählen → prüfen/korrigieren → speichern → erneut öffnen. Unter Englisch wiederholen. | Kameraberechtigung, Vorschau, Aufnahme und Speicherung funktionieren. Bekannte Werte/Faktoren werden korrekt gelesen oder Unsicherheiten zur Korrektur angezeigt. Gespeicherte Korrekturen bleiben erhalten. |
| 8 | Reminder-Ausgangsstand aus Schritt 0 herstellen. Auf P13 zunächst Benachrichtigungsberechtigung verweigern und Prüfen auslösen, dann erlauben; auf beiden Telefonen auch App-/Kanalbenachrichtigungen erlauben. Je unter Deutsch, Englisch und Systemsprache alte Meldung entfernen und **Erinnerung jetzt prüfen** auslösen. Meldung antippen; neu auslösen → Wochenpause betätigen → erneut prüfen. Danach separat Vorwocheneintrag anlegen und prüfen; schließlich ohne Vorwocheneintrag den Reminder deaktivieren. | Verweigerte Berechtigung: keine Meldung, kein Absturz, verständlicher Hinweis. Erlaubt: Titel, Text und Aktion in App-Sprache, korrekte Vorwoche, Antippen öffnet App. Pause verhindert erneute Meldung. Bereits ein Vorwocheneintrag unterdrückt sie. Deaktivieren entfernt Meldung und beendet die Planung. Datumsformat-Ausnahme unten erfassen. |
| 9 | Reminder-Ausgangsstand wiederherstellen, Sprache vorab wählen. App in Hintergrund, Bildschirm aus, regulären nächsten 06:00-Termin abwarten. Danach Telefon neu starten, gegebenenfalls entsperren, App ungeöffnet lassen und nächsten Termin prüfen. Für Deutsch und Englisch wiederholen. Alarmfreigabe, Akku-/Hintergrundregeln und Soll-/Ist-Zeit erfassen. Zusätzlich einen Lauf ohne Freigabe für exakte Alarme protokollieren. | Meldung kommt auch ohne offene Activity und nach Neustart in gewählter Sprache mit korrekter Vorwoche. Mit exaktem Alarm Termin prüfen; ohne Freigabe Hinweis auf mögliche Verzögerung und tatsächliche Zustellung erfassen. Ausbleibende Zustellung bleibt offen bzw. fehlerhaft, nicht automatisch PASS. |

**Kein „Stopp erzwingen“** vor Schritt 9; das ist kein normaler
Hintergrundzustand. Die Testaktion aus Schritt 8 ersetzt Schritt 9 nicht.
Vor jedem Sprachlauf die Ausgangslage wiederherstellen; bei Wochenwechsel
Vorwoche und Snooze-Zustand neu prüfen.

## Ergebnisprotokoll

Pro Umgebung und Schritt eine Zeile ausfüllen, bei Schritt 8/9 je Sprach- und
Berechtigungsfall getrennt. Originalberichte und Screenshots mit dem Protokoll
aufbewahren; keine privaten Backup-Inhalte veröffentlichen.

| Umgebung / Gerät / API | Schritt / Sprache / Fall | Commit / APK-SHA-256 / Buildtyp | Datum / Zeitzone / Soll-Ist-Zeit | PASS / FAIL / BLOCKIERT / OFFEN | Nachweis / Fehler |
|---|---|---|---|---|---|
| Noch nicht ausgeführt | — | — | — | OFFEN | — |

## Noch zu schließende Lücken

- Keine ausgeführten Geräteergebnisse werden durch diese Dokumentationsänderung
  hinzugefügt. CI kompiliert die Instrumentationstests nur.
- Drei Lokalisierungstests prüfen Ressourcen, Platzhalter und einen
  JSON-Rundlauf; sie automatisieren weder Auswahl/Persistenz noch den
  vollständigen Review-/ZIP-/Reminder-Ablauf.
- Echte Altbackups, signaturkompatible Upgrades, Kameraaufnahmen und reguläre
  Hintergrundzustellung brauchen eigene Nachweise.
- `Reminders.kt` verwendet für Datumsbereiche fest `dd.MM.yyyy`, auch unter
  Englisch. Als bekannte Abweichung dokumentieren; vor Freigabe beheben oder
  ausdrücklich als akzeptierte Einschränkung festhalten. Die Datumsanzeige
  nicht allein wegen korrekter Textübersetzung als bestanden markieren.
- E12/E13 belegen die zwei Locale-Verfahren, keine Vollabdeckung aller
  unterstützten Versionen. Android 8/API 26 und weitere aktuelle
  Android-/Herstellerstände bleiben ohne zusätzliche Läufe offen.
- Die tatsächlich verteilte signierte APK braucht einen eigenen Lauf der
  manuellen Matrix; ein Debug-Instrumentationslauf genügt dafür nicht.

Eine belastbare Freigabe setzt nachvollziehbare Ergebnisse der vorgesehenen
Umgebungen und eine Entscheidung zu verbleibenden Abweichungen voraus.
