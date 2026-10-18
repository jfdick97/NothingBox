# Prüfstatus – Version 0.2.0, 18. Oktober 2026

## Abgeschlossen

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

## Noch auf Android auszuführen

Die fünf Android-Integrationstests sind kompiliert und beigelegt, wurden hier aber nicht auf einer Android-Laufzeit ausgeführt. Sie prüfen: alte native Erkennung, neue native OnePager-Erkennung, ZIP-/Room-Rundlauf mit Zusatznamen und ohne doppelte Scans, ungültige Archivpfade sowie die Room-Migration 1→2 mit alten Daten.

Insbesondere die Android-spezifische JNI-Ausführung und die Room-Schemavalidierung nach der Migration sind damit noch nicht als auf einem Gerät bestanden zu verstehen. Die Host-Prüfung des Migrations-SQL ist eine zusätzliche Prüfung, kein Ersatz dafür.

Bitte die neue App auf deinem Smartphone mit dem vorhandenen Foto testen und bei einer neuen Kameraaufnahme die markierten Punkte mit den angezeigten Werten vergleichen. Die vollständige Geräte-Checkliste steht in README.md. Zum Aktualisieren einer bestehenden Installation UPDATE-0.2.md verwenden.
