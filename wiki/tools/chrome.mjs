// Drive a wiki page in headless Chrome, over the DevTools protocol.
//
// The wiki's two big tools are programs - the gene creator runs the spec
// engine in JS, the horse designer runs common/ as WebAssembly - and neither
// is reachable from a static checker. check-links.mjs can tell you the page
// exists; only a browser can tell you the page WORKS.
//
// No Puppeteer, and deliberately no dependency at all: Node 24 has a
// WebSocket, and Chrome has --remote-debugging-port, which between them is
// the whole harness. Adding a devDependency to a repo whose tools are all
// zero-install would cost more than these forty lines.
//
// Every page here is opened over file://, because that is the constraint the
// tools are built to (see making-a-gene.html#the-shape) - so a check that
// passed on a local server and failed off disk would be worse than useless.
//
//   import { open } from "./chrome.mjs";
//   const page = await open(url);
//   await page.evaluate("document.title");
//   page.close();

import { spawn } from "node:child_process";
import { existsSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

/** Where Chrome is, on each platform. First hit wins; override with $CHROME. */
const CANDIDATES = [
  process.env.CHROME,
  "C:/Program Files/Google/Chrome/Application/chrome.exe",
  "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  "/usr/bin/google-chrome",
  "/usr/bin/chromium",
  "/usr/bin/chromium-browser"
];

export function findChrome() {
  for (const path of CANDIDATES) {
    if (path && existsSync(path)) return path;
  }
  return null;
}

/**
 * Open `pageUrl` in a headless Chrome and return a handle on it.
 *
 * @param pageUrl   a file:// URL
 * @param options.port      debugging port (pick a free one per concurrent run)
 * @param options.readyWhen a JS expression polled until it is truthy, so the
 *                          caller decides what "loaded" means - the creator
 *                          runs a parity self-check on boot, and a fixed sleep
 *                          was flaky against it
 */
export async function open(pageUrl, options = {}) {
  const chromePath = findChrome();
  if (!chromePath) {
    throw new Error("no Chrome found - set $CHROME to its path, or skip this check");
  }
  const port = options.port || 9222;
  const profile = mkdtempSync(join(tmpdir(), "hg-chrome-"));
  const chrome = spawn(chromePath, [
    "--headless=new", "--disable-gpu", `--remote-debugging-port=${port}`,
    `--user-data-dir=${profile}`, "--allow-file-access-from-files",
    "--window-size=1600,1200", pageUrl
  ], { stdio: "ignore" });

  let target = null;
  for (let i = 0; i < 80 && !target; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
      target = list.find((t) => t.type === "page" && t.url.startsWith("file:"));
    } catch { /* not listening yet */ }
    if (!target) await sleep(250);
  }
  if (!target) {
    chrome.kill();
    throw new Error("Chrome never came up on port " + port);
  }

  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r) => ws.addEventListener("open", r, { once: true }));

  let id = 0;
  const waiting = new Map();
  const logs = [];
  ws.addEventListener("message", (m) => {
    const msg = JSON.parse(m.data);
    if (msg.id && waiting.has(msg.id)) {
      waiting.get(msg.id)(msg);
      waiting.delete(msg.id);
    } else if (msg.method === "Runtime.consoleAPICalled") {
      logs.push(msg.params.type + ": "
        + msg.params.args.map((a) => a.value ?? a.description).join(" "));
    } else if (msg.method === "Runtime.exceptionThrown") {
      const d = msg.params.exceptionDetails;
      logs.push("EXCEPTION: " + (d.exception?.description || d.text));
    }
  });

  function send(method, params = {}) {
    const n = ++id;
    ws.send(JSON.stringify({ id: n, method, params }));
    return new Promise((r) => waiting.set(n, r));
  }

  async function evaluate(expression) {
    const r = await send("Runtime.evaluate",
      { expression, returnByValue: true, awaitPromise: true });
    const ex = r.result?.exceptionDetails;
    if (ex) throw new Error(ex.exception?.description || ex.text);
    return r.result.result.value;
  }

  /** A left-button click at page coordinates, as a real pointer sequence. */
  async function click(x, y) {
    await send("Input.dispatchMouseEvent",
      { type: "mousePressed", x, y, button: "left", buttons: 1, clickCount: 1, pointerType: "mouse" });
    await send("Input.dispatchMouseEvent",
      { type: "mouseReleased", x, y, button: "left", buttons: 0, clickCount: 1, pointerType: "mouse" });
  }

  async function mouse(type, x, y, button = "left", buttons = 1) {
    await send("Input.dispatchMouseEvent",
      { type, x, y, button, buttons, clickCount: 1, pointerType: "mouse" });
  }

  async function screenshot(file) {
    const s = await send("Page.captureScreenshot", { format: "png" });
    writeFileSync(file, Buffer.from(s.result.data, "base64"));
  }

  await send("Runtime.enable");
  await send("Page.enable");

  const ready = options.readyWhen || "document.readyState === 'complete'";
  for (let i = 0; i < 80; i++) {
    if (await evaluate(ready).catch(() => false)) break;
    await sleep(250);
  }

  return {
    send, evaluate, click, mouse, screenshot, logs, sleep,
    /** Console output that looks like a failure rather than a note. */
    errors: () => logs.filter((l) => /error|EXCEPTION|NoSuchMethod|ClassNotFound/i.test(l)),
    close: () => { ws.close(); chrome.kill(); }
  };
}

export function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

/** A tiny assertion collector, so a check reads as a list of claims. */
export function checks() {
  const passed = [], failed = [];
  return {
    ok(name, condition, detail) {
      (condition ? passed : failed).push(name + (detail === undefined ? "" : " -> " + detail));
      return condition;
    },
    report(what) {
      for (const line of passed) console.log("  ok   " + line);
      for (const line of failed) console.log("  FAIL " + line);
      if (failed.length) {
        console.log(`${what}: ${failed.length} of ${passed.length + failed.length} checks failed`);
        return 1;
      }
      console.log(`${what} OK - ${passed.length} checks`);
      return 0;
    }
  };
}
