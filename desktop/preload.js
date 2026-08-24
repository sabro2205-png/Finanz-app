"use strict";

/**
 * Reicht der Seite eine schmale, synchrone Dateiablage durch – nur diese eine
 * Datei, keine weiteren Node-Fähigkeiten. Synchron, damit die Seite denselben
 * Code wie im Browser benutzen kann; die Datei ist klein genug dafür.
 *
 * Den Ordner bestimmt der Hauptprozess und übergibt ihn als Argument; im
 * Preload gibt es kein `app`.
 */
const { contextBridge } = require("electron");
const path = require("node:path");
const fs = require("node:fs");
const os = require("node:os");

const PREFIX = "--kassenbuch-data-dir=";
const fromArgs = process.argv.find(a => a.startsWith(PREFIX));
const userData = fromArgs
  ? fromArgs.slice(PREFIX.length)
  : path.join(os.homedir(), ".kassenbuch");

const file = path.join(userData, "kassenbuch.json");

contextBridge.exposeInMainWorld("kassenbuchStore", {
  path: () => file,

  read: () => {
    if (!fs.existsSync(file)) return null;
    return fs.readFileSync(file, "utf8");
  },

  write: text => {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    // Erst daneben schreiben, dann umbenennen: ein Absturz mitten im
    // Schreiben darf die vorhandenen Daten nicht zerstören.
    const tmp = file + ".tmp";
    fs.writeFileSync(tmp, text, "utf8");
    fs.renameSync(tmp, file);
  }
});
