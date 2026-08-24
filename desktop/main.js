"use strict";

const { app, BrowserWindow, Menu, shell, dialog } = require("electron");
const path = require("node:path");
const fs = require("node:fs");

/** Die Datendatei liegt neben den übrigen App-Daten des Nutzers. */
const dataFile = () => path.join(app.getPath("userData"), "kassenbuch.json");

function createWindow() {
  const win = new BrowserWindow({
    width: 480,
    height: 900,
    minWidth: 360,
    minHeight: 560,
    title: "Kassenbuch",
    titleBarStyle: process.platform === "darwin" ? "hiddenInset" : "default",
    backgroundColor: "#e8eae9",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      // Der Preload hat kein `app`, deshalb den Ordner als Argument mitgeben.
      additionalArguments: ["--kassenbuch-data-dir=" + app.getPath("userData")],
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  // Im fertigen Programm liegt die Oberfläche unter renderer/, beim
  // Entwickeln direkt in web/ – so gibt es nur eine Quelle.
  const packaged = path.join(__dirname, "renderer", "index.html");
  const source = path.join(__dirname, "..", "web", "index.html");
  win.loadFile(fs.existsSync(packaged) ? packaged : source);

  // Links nach draußen im richtigen Browser öffnen, nicht im App-Fenster.
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });

  return win;
}

function buildMenu() {
  const isMac = process.platform === "darwin";
  const template = [
    ...(isMac ? [{ role: "appMenu" }] : []),
    {
      label: "Datei",
      submenu: [
        {
          label: "Datendatei im Finder zeigen",
          click: () => {
            const file = dataFile();
            if (fs.existsSync(file)) shell.showItemInFolder(file);
            else shell.openPath(app.getPath("userData"));
          }
        },
        {
          label: "Datendatei sichern …",
          click: async () => {
            const file = dataFile();
            if (!fs.existsSync(file)) {
              dialog.showMessageBox({
                type: "info",
                message: "Es gibt noch nichts zu sichern.",
                detail: "Sobald du etwas erfasst hast, liegt hier eine Datei."
              });
              return;
            }
            const stamp = new Date().toISOString().slice(0, 10);
            const { canceled, filePath } = await dialog.showSaveDialog({
              title: "Daten sichern",
              defaultPath: `kassenbuch-${stamp}.json`
            });
            if (!canceled && filePath) fs.copyFileSync(file, filePath);
          }
        },
        { type: "separator" },
        isMac ? { role: "close" } : { role: "quit" }
      ]
    },
    { role: "editMenu" },
    {
      label: "Ansicht",
      submenu: [
        { role: "reload", label: "Neu laden" },
        { role: "resetZoom", label: "Originalgröße" },
        { role: "zoomIn", label: "Größer" },
        { role: "zoomOut", label: "Kleiner" },
        { type: "separator" },
        { role: "togglefullscreen", label: "Vollbild" },
        { role: "toggleDevTools", label: "Entwicklerwerkzeuge" }
      ]
    },
    { role: "windowMenu" }
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

app.whenReady().then(() => {
  buildMenu();
  createWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

// Auf dem Mac bleibt die App üblicherweise laufen, bis man sie beendet.
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
