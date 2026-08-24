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

### Wiederkehrende Posten mit Zeitraum
Miete, Finanzierungen und Abos lassen sich als **monatlich wiederkehrend** anlegen.
Über die Felder **von / bis** wird der Zeitraum festgelegt:

- **von** – ab welchem Monat der Posten gilt (nicht zwingend der gerade angezeigte)
- **bis** – bis zu welchem Monat, oder **unbefristet** für laufende Kosten wie Miete

Für befristete Posten zeigt der Dialog die Laufzeit und die Gesamtsumme an
(„Laufzeit 29 Monate · Gesamt 4.335,50 €"), die Monatsliste die Restlaufzeit
(„Jan 2026 – Mai 2028 · noch 22×"). Das eignet sich für Finanzierungen mit fester
Ratenzahl.

Beim Bearbeiten eines laufenden Postens gibt es zusätzlich:
- **„Ab &lt;Monat&gt; beenden"** – der Posten läuft bis zum Vormonat weiter, die Vergangenheit
  bleibt unverändert (z. B. ein gekündigtes Abo)
- **„Komplett löschen"** – der Posten verschwindet aus allen Monaten

### Reiter „Jahr"
- Alle zwölf Monate eines Jahres im Vergleich, als Balkendiagramm und als Tabelle
- Jahressummen für Einnahmen, Ausgaben, Ergebnis und Durchschnitt pro aktivem Monat
- Ein Tipp auf einen Monat springt direkt in den Monatsreiter

### Reiter „Erspartes"
- **Spartöpfe**: das Ersparte wird auf benannte Töpfe verteilt (Notgroschen, Urlaubskasse,
  Neues Auto …). Jeder Topf hat eine Farbe und ein freiwilliges Sparziel, dessen Fortschritt
  angezeigt wird
- Eine Filterleiste schaltet zwischen allen Töpfen und einem einzelnen um; Kennzahlen,
  Diagramm und Prognose beziehen sich dann nur auf diesen Topf
- Die Karte „Aufteilung" zeigt Inhalt und Anteil jedes Topfes
- Beim Löschen eines Topfes lassen sich seine Buchungen in einen anderen verschieben,
  statt sie zu verlieren; der letzte Topf bleibt bestehen
- Buchungen je Monat erfassen, mit freiwilliger Notiz
- Negative Beträge gelten als Entnahme aus dem Topf
- Diagramm mit der monatlichen Sparrate und der kumulierten Entwicklung über das Jahr
- Kennzahlen: Stand zu Jahresbeginn und -ende, Summe des Jahres, Ø pro Sparmonat, bester Monat
- **Das Ersparte wird bewusst getrennt geführt und beeinflusst weder Einnahmen noch Ausgaben
  noch das Monatsergebnis.**

### Sparprognose
Aus dem bisherigen Sparverhalten wird hochgerechnet, wo der Sparstand am Jahresende
voraussichtlich steht, und wie viel in zwölf Monaten zusammengekommen sein dürfte.

Grundlage ist der Durchschnitt über **alle Monate mit Einträgen, die nicht in der Zukunft
liegen** – auch über Jahresgrenzen hinweg. Zwei Regeln halten die Rechnung ehrlich:

- Ein vergangener Monat ohne Eintrag ist eine Tatsache (nichts gespart) und wird nicht
  hochgerechnet. Nur künftige Monate ohne Eintrag werden mit dem Durchschnitt gefüllt.
- Entnahmen (negative Beträge) senken den Durchschnitt genauso, wie sie den Stand senken.

Im Diagramm ist die Prognose als gestrichelte Linie mit hohlen Punkten von den erfassten
Werten abgesetzt. Ohne erfasste Monate erscheint statt einer Zahl ein Hinweis –
es wird nichts erfunden.

## Web-Vorschau

`web/index.html` ist eine eigenständige Web-Version zum Ausprobieren – eine einzelne HTML-Datei
ohne Abhängigkeiten. Einfach im Browser öffnen (Doppelklick genügt) oder auf einen beliebigen
Webserver legen.

Sie bildet alle drei Reiter und dieselbe Rechenlogik ab (die Funktionen aus `FinanceCalculator`
sind 1:1 nach JavaScript portiert). Unterschiede zur Android-App:

- Daten liegen im `localStorage` des jeweiligen Browsers, nicht in einer Datenbank –
  sie sind also gerätegebunden und gehen beim Leeren der Browserdaten verloren
- Manche Umgebungen verweigern der Seite das Speichern (privater Modus, blockierte
  Site-Daten, ein abgeschotteter iframe ohne `allow-same-origin`). Die Seite prüft das
  beim Start mit einem Schreib-Lese-Test und beim Speichern durch Zurücklesen; schlägt es
  fehl, erscheint auf jedem Reiter eine Warnung statt eines stillen Datenverlusts
- Über den Datenbank-Knopf oben rechts lassen sich alle Daten als Text sichern und wieder
  einspielen. Bewusst zum Kopieren und Einfügen statt als Download: Downloads sind in
  abgeschotteten Rahmen blockiert – also genau dort, wo die Sicherung am nötigsten ist
- Beim ersten Öffnen sind Beispieldaten geladen; ein Klick auf „Alles löschen und leer starten"
  entfernt sie endgültig
- Die Sparentwicklung ist in zwei Felder mit gemeinsamer Monatsachse geteilt, weil Sparrate
  und Gesamtstand um Größenordnungen auseinanderliegen (die Android-App macht das genauso)

## Desktop-Version (macOS)

`desktop/` enthält eine Electron-Hülle um dieselbe Oberfläche. Es gibt weiterhin nur eine
Quelle: `web/index.html`. Ein Sync-Schritt kopiert sie vor dem Start und vor dem Paketieren
nach `desktop/renderer/`, weil electron-builder nur paketiert, was unterhalb seiner
`package.json` liegt.

Voraussetzung ist Node.js (`brew install node` oder von nodejs.org).

```bash
cd desktop
npm install        # lädt Electron, dauert beim ersten Mal ein paar Minuten
npm start          # App direkt starten
npm run dist       # fertige .app und .dmg bauen -> desktop/dist/
```

Der wichtigste Unterschied zur Browser-Version: **die Daten liegen in einer echten Datei**
unter `~/Library/Application Support/Kassenbuch/kassenbuch.json`, nicht im localStorage.
Damit entfällt die Fragilität der Browser-Ablage. Geschrieben wird erst in eine Nebendatei
und dann umbenannt, damit ein Absturz mitten im Schreiben die vorhandenen Daten nicht
zerstört. Über **Datei → Datendatei im Finder zeigen** kommt man direkt hin, über
**Datei → Datendatei sichern …** an eine Kopie.

Die Seite entscheidet selbst, welche Ablage sie benutzt: Reicht das Preload-Skript eine
Brücke durch (`window.kassenbuchStore`), schreibt sie in die Datei, sonst in den
localStorage. Derselbe Code läuft also unverändert im Browser und auf dem Desktop.

Die `.app` ist nicht signiert und nicht notarisiert. Selbst gebaut ist das kein Problem –
macOS setzt das Quarantäne-Merkmal nur bei heruntergeladenen Dateien. Willst du sie auf ein
anderes Gerät kopieren, braucht es dort einmal Rechtsklick → Öffnen.

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
├── data/        Room-Entitäten, DAO, Datenbank (inkl. Migration 1→2), Repository
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
- Sparprognose: Durchschnittsbildung nur aus Monaten bis heute, Entnahmen drücken den
  Durchschnitt, vergangene Leermonate werden nicht hochgerechnet, Vorjahre zählen mit
- Spartöpfe: getrennte Summen je Topf, Anteile ohne Verzerrung durch negative Bestände,
  Zielfortschritt zwischen 0 und 100 %, Filter auf einen einzelnen Topf
- Restlaufzeit und Gültigkeit befristeter Posten über Jahresgrenzen hinweg
- Parsen deutscher und englischer Betragsschreibweisen, Schutz gegen Division durch Null
