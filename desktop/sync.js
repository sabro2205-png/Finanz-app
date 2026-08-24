"use strict";

/**
 * Kopiert die Oberfläche aus web/ nach renderer/.
 *
 * electron-builder paketiert nur, was unterhalb dieser package.json liegt –
 * ein Verweis auf ../web würde im fertigen Programm fehlen. Es bleibt bei
 * einer einzigen Quelle: web/index.html.
 */
const fs = require("node:fs");
const path = require("node:path");

const source = path.join(__dirname, "..", "web", "index.html");
const targetDir = path.join(__dirname, "renderer");
const target = path.join(targetDir, "index.html");

if (!fs.existsSync(source)) {
  console.error("Nicht gefunden: " + source);
  process.exit(1);
}

fs.mkdirSync(targetDir, { recursive: true });
fs.copyFileSync(source, target);
console.log("Oberfläche übernommen: web/index.html -> desktop/renderer/index.html");
