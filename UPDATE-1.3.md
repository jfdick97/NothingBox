# Update auf NothingBox 1.3.0

## Vor dem Update

1. In der bisherigen App unter Menü eine vollständige ZIP-Sicherung exportieren und außerhalb der App aufbewahren. CSV allein ist keine wiederherstellbare Sicherung.
2. Bestehende App installiert lassen. Projektänderungen zuvor committen oder den bisherigen Projektordner als Rückfallmöglichkeit behalten.
3. Den 1.3.0-Quellstand öffnen: In Android Studio den Ordner mit `settings.gradle.kts` auswählen. Gradle-JVM 17 oder 21 sowie SDK 35 verwenden; eingecheckte AGP-/Kotlin-Versionen zunächst beibehalten.
4. Dasselbe Telefon auswählen und **Run → app** ausführen beziehungsweise eine mit dem bisherigen Schlüssel signierte 1.3.0-APK als Update installieren.

Application-ID bleibt `de.jfd.straingauge`. Android akzeptiert ein Update nur mit passender Signatur und zulässigem Versionscode. Bei Signatur- oder Downgrade-Fehlern nicht deinstallieren und keine Daten löschen: zuerst den bisherigen Schlüssel bzw. den Versionsstand klären. Android Studio verwendet auf demselben Rechner üblicherweise denselben Debug-Schlüssel; auf einem anderen Rechner ist das nicht garantiert.

## Nach dem Update

1. Unter Menü Version **1.3.0** prüfen. Die Version stammt aus `app/build.gradle.kts` (`versionName = "1.3.0"`, `versionCode = 7`).
2. Vorhandene Wochen, Werte, benannte Zusatzfaktoren sowie gespeicherte Scans und Bilder mit dem Stand vor dem Update vergleichen.
3. Über das **Sprachsymbol zwischen Scans und Menü** Deutsch → Englisch → Systemsprache wählen. Nach jeder Auswahl den Dialog erneut öffnen und die App neu starten; Auswahl und Texte müssen erhalten bleiben.
4. Unter den Diagrammen die Stress-Statistik einblenden. Der Stressindex wird nur für Tage mit allen vier Angaben berechnet. Zeitraumauswahl, Wochenstatistik und Belastungsfaktoren prüfen.
5. Unter **Menü → Erinnerungen** Benachrichtigungen erlauben; auf Android 13+ die Laufzeitberechtigung beachten. Bei leerer Vorwoche und ohne aktuelle Wochenpause **Erinnerung jetzt prüfen** auslösen.
6. Für die vollständige Abnahme die [Geräte-Validierungsmatrix](GERAETEVALIDIERUNG-1.3.md) durchlaufen. Ein erfolgreicher Testknopf ersetzt die reguläre 06:00-Zustellung nicht.

## Funktionsumfang und Kompatibilität

- Deutsche und englische Oberfläche mit persistenter Sprachwahl; Android 8–12 über AppCompat, Android 13+ mit System-App-Sprachen. Nicht unterstützte Systemsprachen fallen auf Deutsch zurück.
- Stressindex: `(Anforderungen + Anspannung + (10 − Energie) + (10 − Handlungsspielraum)) / 4`, einschließlich Verlauf, Mittelwert und Standardabweichung.
- Wochenstatistik, Trendhinweise, Erinnerungen sowie systemabhängiges helles/dunkles Design sind im [README](README.md) beschrieben.
- Gegenüber dem Stand vor der Lokalisierung unverändert: Room-Schema, Application-ID, ISO-Speicherdaten, Messgrößen-/Faktorpositionen und ZIP-Format 2. ZIP-Import unterstützt weiterhin die Formate 1 und 2; CSV behält sein maschinenlesbares Format.
- Benannte Zusatzfaktoren werden nicht übersetzt. Erkannte alte Scannerhinweise werden nur bei der Anzeige übersetzt; archivierte Hinweise werden nicht umgeschrieben.
- Bei einer sehr alten Installation mit Room-Schema 1 bleibt die additive Migration 1→2 erforderlich. Sie stammt aus dem früheren OnePager-Update; 1.3.0 führt für die Sprachwahl keine zusätzliche Schemaänderung ein.

Die Anleitung [UPDATE-0.2.md](UPDATE-0.2.md) dokumentiert ausschließlich den historischen Schritt auf 0.2. Der [Prüfstatus](PRUEFSTATUS.md) hält die noch offenen Geräteprüfungen und das derzeit feste Datumsformat in Erinnerungen fest.
