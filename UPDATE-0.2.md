# Update auf 0.2 – OnePager mit L-Marken

## Dein vorhandenes Projekt aktualisieren

1. In der bisherigen App eine ZIP-Sicherung exportieren.
2. Das neue Projektarchiv in einen **neuen Ordner** entpacken. In Android Studio dieses neue `StrainGauge`-Projekt über **Open** öffnen. So bleibt auch dein bisheriger Quellcode als Rückfallmöglichkeit erhalten.
3. Gradle synchronisieren lassen. AGP und Kotlin wurden nicht geändert; ein AGP-Upgrade ist für dieses Update nicht erforderlich. Gradle-JVM 17 oder 21 verwenden.
4. Dasselbe Smartphone anschließen und **Run → app** starten. Die bisherige App auf dem Telefon dabei installiert lassen.
5. Die neue Version wird mit derselben Application-ID über die alte installiert. Beim ersten Datenbankzugriff fügt die Migration zwei Namensfelder hinzu. Bestehende Tage, Werte, Scans und Fotos bleiben erhalten.
6. Unter Menü steht Version **0.2.0**. Dort lässt sich das neue künstliche Testblatt auswerten.

Beim Bauen auf demselben Computer verwendet Android Studio üblicherweise denselben Debug-Signierschlüssel. Bei einer Signaturfehlermeldung nicht durch Deinstallation fortfahren: Die Sicherung behalten und zunächst den bisher verwendeten Schlüssel beziehungsweise Rechner verwenden.

## Dein Foto ausprobieren

- **+ → Vorhandenes Foto öffnen** und `20261018_113932.jpg` auswählen.
- Als Wochenbeginn `2026-10-12` eintragen.
- Den Namen der ersten Zusatzzeile manuell eintragen, beispielsweise genau wie auf deinem Blatt. Die Handschrift wird nicht automatisch gelesen.
- Alle Werte und Faktoren anhand des vergrößerbaren Erkennungsbilds prüfen und dann speichern.
- Das Projekt enthält `ERKENNUNG_BEISPIEL.jpg` mit der Auswertung durch den Host-Test und dein Foto als Regressionseingabe unter `tools/testdata`.

## Was geändert wurde

- Neues Formularprofil direkt aus der linken PDF-Hälfte; alle 28 horizontalen Punktfelder und 70 Faktor-Kreise.
- Visuelle Registrierung mit ORB/RANSAC, Bestätigung und Verfeinerung aller vier L-Marken und Prüfung des Orientierungspunkts oben links.
- Zusätzlicher lokaler Abgleich jedes Skalenfelds, um kleine Papierwölbungen auszugleichen. Das ersetzt kein ebenes Blatt bei starken Falten.
- Kontinuierliche Messwerte, als Double mit zwei berechneten Nachkommastellen gespeichert; Oberfläche zeigt eine Nachkommastelle. Die Stellenzahl ist keine Zusicherung der Messgenauigkeit.
- Acht feste und zwei benannte Zusatzfaktoren; Namen werden beim Speichern der Woche an die jeweiligen Tagesdatensätze angehängt. Dadurch behalten historische Einträge ihren Namen, auch wenn eine andere Woche die Zusatzzeile anders nutzt. Die Tagesbearbeitung kann diese Namen anschließend pro Eintrag ändern.
- Dashboard zählt Zusatzfaktoren nach ihrem gespeicherten Namen; CSV und vollständige ZIP-Sicherung berücksichtigen sie.
- Room-Datenbankversion 2 mit rein additiver Migration; keine destruktive Neuerstellung.
- Sicherungsformat 2; alte Sicherungen im Format 1 bleiben importierbar. Eine alte App-Version kann neue Format-2-Sicherungen nicht einlesen.
- Frühere BC-02-Formulare mit ArUco-Markern bleiben automatisch erkennbar.

## Genauigkeit und Prüfung

Die Zahlen im Beispiel sind algorithmisch aus den Punktpositionen berechnet. Bei deinem Foto unterscheiden sich einzelne Ergebnisse je nach Registrierung leicht, weil Papier und Stiftpunkte nicht ideal sind. Eine Nachkommastelle ist eine sinnvolle Anzeige, aber nicht automatisch eine Genauigkeit von 0,1. Die Prüfansicht bleibt Teil des Workflows.
