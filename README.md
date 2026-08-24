# Finanz-App

Eine Android-App für die private Finanzübersicht: Einnahmen und Ausgaben pro Monat
erfassen, Monate miteinander vergleichen und das Ersparte getrennt über das Jahr verfolgen.

## Funktionen

### Reiter „Monat"
- Einnahmen und Ausgaben für einen einzelnen Monat erfassen
- Ausgaben sind gestaffelt nach **Miete**, **Finanzierung**, **Abos** und **Sonstige Ausgaben** –
  je Kategorie werden Summe, Anteil an den Gesamtausgaben und die einzelnen Posten angezeigt
- Die Ausgaben werden automatisch von den Einnahmen abgezogen („Bleibt übrig")
- Fortschrittsbalken zeigt, wie viel Prozent der Einnahmen bereits verplant sind
- Direkter Vergleich mit dem Vormonat („Gegenüber Juli: +120,50 €")
- Über die Pfeile oder einen Tipp auf den Monatsnamen zwischen allen Monaten wechseln

### Wiederkehrende Posten
Miete, Finanzierungen und Abos lassen sich als **monatlich wiederkehrend** anlegen.
Sie tauchen dann automatisch in jedem Folgemonat auf und müssen nicht neu eingegeben werden.

Beim Bearbeiten eines laufenden Postens gibt es zwei Möglichkeiten:
- **„Ab &lt;Monat&gt; beenden"** – der Posten läuft bis zum Vormonat weiter, die Vergangenheit
  bleibt unverändert (z. B. ein gekündigtes Abo)
- **„Komplett löschen"** – der Posten verschwindet aus allen Monaten

### Reiter „Jahr"
- Alle zwölf Monate eines Jahres im Vergleich, als Balkendiagramm und als Tabelle
- Jahressummen für Einnahmen, Ausgaben, Ergebnis und Durchschnitt pro aktivem Monat
- Ein Tipp auf einen Monat springt direkt in den Monatsreiter

### Reiter „Erspartes"
- Sparbeträge je Monat erfassen (mehrere Posten pro Monat möglich, z. B. Tagesgeld und ETF)
- Negative Beträge gelten als Entnahme
- Diagramm mit der monatlichen Sparrate und der kumulierten Entwicklung über das Jahr
- Kennzahlen: Stand zu Jahresbeginn und -ende, Summe des Jahres, Ø pro Sparmonat, bester Monat
- **Das Ersparte wird bewusst getrennt geführt und beeinflusst weder Einnahmen noch Ausgaben
  noch das Monatsergebnis.**

## Technik

| | |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose, Material 3 (inkl. dynamischer Farben ab Android 12) |
| Datenhaltung | Room (lokale SQLite-Datenbank, keine Cloud, keine Netzwerkrechte) |
| Diagramme | Compose Canvas, ohne externe Bibliotheken |
| minSdk / targetSdk | 26 (Android 8.0) / 35 |

Die App fordert **keine Berechtigungen** an und sendet keine Daten. Alles bleibt auf dem Gerät.

### Aufbau

```
app/src/main/java/de/sabro/finanzapp/
├── data/        Room-Entitäten, DAO, Datenbank, Repository
├── domain/      FinanceCalculator – reine Rechenlogik, ohne Android-Abhängigkeiten
├── ui/          MainActivity, ViewModel, Screens (month/year/savings), Dialoge, Diagramme
└── util/        Monatsindex (Period) und Geldformatierung (Money)
```

Monate werden als fortlaufender Index `jahr * 12 + (monat - 1)` gespeichert. Dadurch lässt
sich die Gültigkeit wiederkehrender Posten mit einem einfachen Zahlenvergleich abfragen
(`startPeriod <= monat AND (endPeriod IS NULL OR endPeriod >= monat)`).

Beträge werden durchgehend als **Cent in `Long`** gespeichert – keine Rundungsfehler
durch Gleitkommazahlen.

## Bauen

Voraussetzung: Android Studio (Ladybug oder neuer) oder ein installiertes Android SDK
mit API 35 und JDK 17.

```bash
# In Android Studio: Ordner öffnen, Gradle-Sync abwarten, auf "Run" klicken.

# Auf der Kommandozeile:
./gradlew assembleDebug        # APK bauen -> app/build/outputs/apk/debug/
./gradlew installDebug         # direkt auf ein angeschlossenes Gerät installieren
./gradlew test                 # Unit-Tests der Rechenlogik ausführen
```

## Tests

`app/src/test/` enthält Unit-Tests für die Rechenlogik:

- Ausgaben werden korrekt von den Einnahmen abgezogen
- Staffelung und Summenbildung je Ausgabenkategorie
- Gültigkeit einmaliger, laufender und beendeter Posten über Monatsgrenzen hinweg
- Hochrechnung laufender Posten in der Jahresansicht
- Kumulierte Sparentwicklung inklusive Entnahmen
- Erspartes verändert das Monatsergebnis nicht
- Parsen deutscher und englischer Betragsschreibweisen, Schutz gegen Division durch Null
