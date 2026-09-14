#!/usr/bin/env node
// The AI helpers, with the AI replaced by a stub: does the chat, the bug-report
// form and both designers' "Draft with AI" do the right thing with a reply?
//
//   node wiki/tools/check-ai.mjs [--shot-dir dir]
//
// Run it after touching wiki/ai/, js/bd-ai.js or js/gc-ai.js. No key and no
// network to a provider: window.fetch is swapped for one that answers the chat
// endpoints with canned streams (split mid-line, as a real stream arrives) and
// passes everything else - the wasm, the search index - through untouched.
// What it cannot tell you is whether a real model writes good files; that is a
// person with a key (wiki/verification.html#ai-helpers).

import { open, checks, findChrome, sleep } from "./chrome.mjs";
import { pathToFileURL } from "node:url";
import { resolve, extname, join } from "node:path";
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";

if (!findChrome()) {
  console.log("no Chrome found - skipping the AI helpers check (set $CHROME to run it)");
  process.exit(0);
}

const SHOTS = process.argv.includes("--shot-dir") ? process.argv[process.argv.indexOf("--shot-dir") + 1] : null;
const c = checks();

// Installed into a page. __aiReplies is a queue of reply texts; __aiCalls records
// every request that reached a model.
const STUB = `(function () {
  window.__aiCalls = [];
  window.__aiReplies = [];
  var real = window.__realFetch || window.fetch;
  window.__realFetch = real;
  function stream(sse) {
    var enc = new TextEncoder();
    var a = Math.floor(sse.length / 3), b = Math.floor(sse.length * 2 / 3);
    return new Response(new ReadableStream({ start: function (ctl) {
      ctl.enqueue(enc.encode(sse.slice(0, a)));
      ctl.enqueue(enc.encode(sse.slice(a, b)));
      ctl.enqueue(enc.encode(sse.slice(b)));
      ctl.close();
    } }), { status: 200, headers: { "content-type": "text/event-stream" } });
  }
  function thirds(text) {
    var a = Math.floor(text.length / 3), b = Math.floor(text.length * 2 / 3);
    return [text.slice(0, a), text.slice(a, b), text.slice(b)];
  }
  function json(o) { return new Response(JSON.stringify(o), { status: 200, headers: { "content-type": "application/json" } }); }
  window.fetch = function (url, init) {
    url = String(url);
    if (/chat\\/completions$/.test(url) || /\\/v1\\/messages$/.test(url)) {
      var call = { url: url, headers: (init && init.headers) || {}, body: JSON.parse(init.body) };
      window.__aiCalls.push(call);
      var text = window.__aiReplies.length ? window.__aiReplies.shift() : "OK";
      if (/messages$/.test(url)) {
        var ev = function (type, data) { return "event: " + type + "\\ndata: " + JSON.stringify(data) + "\\n\\n"; };
        var sse = ev("message_start", { type: "message_start", message: { id: "m", content: [] } }) +
          ev("content_block_start", { type: "content_block_start", index: 0, content_block: { type: "text", text: "" } });
        thirds(text).forEach(function (t) { sse += ev("content_block_delta", { type: "content_block_delta", index: 0, delta: { type: "text_delta", text: t } }); });
        sse += ev("message_delta", { type: "message_delta", delta: { stop_reason: "end_turn" } }) + ev("message_stop", { type: "message_stop" });
        return Promise.resolve(stream(sse));
      }
      var out = ": OPENROUTER PROCESSING\\n\\n";
      thirds(text).forEach(function (t) { out += "data: " + JSON.stringify({ choices: [{ delta: { content: t } }] }) + "\\n\\n"; });
      return Promise.resolve(stream(out + "data: [DONE]\\n\\n"));
    }
    if (/openrouter\\.ai\\/api\\/v1\\/models|anthropic\\.com\\/v1\\/models/.test(url)) {
      return Promise.resolve(json({ data: [
        { id: "openrouter/free", name: "Free router", pricing: { prompt: "0", completion: "0" }, architecture: { input_modalities: ["text", "image"] } },
        { id: "some/text-only:free", name: "Text only", pricing: { prompt: "0", completion: "0" }, architecture: { input_modalities: ["text"] } }
      ] }));
    }
    if (/api\\.github\\.com\\/search/.test(url)) return Promise.resolve(json({ items: [{ title: "An older one", html_url: "https://github.com/x/y/issues/1", number: 1, state: "open" }] }));
    return real.apply(this, arguments);
  };
  localStorage.setItem("phc-ai", JSON.stringify({ provider: "openrouter", keys: { openrouter: "sk-or-test" }, models: {} }));
  document.dispatchEvent(new CustomEvent("hg:ai"));
})()`;

async function waitFor(page, expr, tries = 80) {
  for (let i = 0; i < tries; i++) {
    if (await page.evaluate(expr).catch(() => false)) return true;
    await sleep(150);
  }
  return false;
}

// Drive a draft dialog: open it, type, press Write it, wait for a verdict.
const DRAFT = (words) => `(async function () {
  document.getElementById("btn-ai").click();
  var dlg = document.querySelector(".phc-ai-dialog");
  dlg.querySelector(":scope > textarea").value = ${JSON.stringify(words)};
  Array.prototype.find.call(dlg.querySelectorAll("button"), function (b) { return b.textContent === "Write it"; }).click();
  var st = dlg.querySelector(".phc-ai-status");
  for (var i = 0; i < 150 && !/good|bad/.test(st.className); i++) await new Promise(function (r) { setTimeout(r, 100); });
  return { text: st.textContent, good: /good/.test(st.className), calls: window.__aiCalls.length };
})()`;

const USE_IT = `(function () {
  var dlg = document.querySelector(".phc-ai-dialog");
  Array.prototype.find.call(dlg.querySelectorAll("button"), function (b) { return b.textContent === "Use it"; }).click();
  return true;
})()`;

const CLOSE = `(function () { var o = document.querySelector(".phc-ai-overlay"); if (o) o.querySelector(".phc-ai-icon").click(); return true; })()`;

// ---- 1. a wiki page: the setup form, the chat, a bug report, both request formats

{
  const page = await open(pathToFileURL(resolve("wiki/ai-setup.html")).href, { port: 9226 });
  try {
    c.ok("the helpers load on a wiki page", await waitFor(page, `!!(window.HG && HG.ai && document.getElementById("phc-ai-bubble"))`), "");
    const form = await page.evaluate(`document.querySelectorAll("[data-ai-settings] select option").length`);
    c.ok("the setup page draws the settings form", form >= 9, form + " options");
    c.ok("with no key, nothing is ready", !(await page.evaluate(`localStorage.removeItem("phc-ai"), HG.ai.ready()`)), "");

    await page.evaluate(STUB);
    c.ok("a saved key and the default model make it ready", await page.evaluate(`HG.ai.ready() && HG.ai.current().model === "openrouter/free"`), "");

    const hits = await page.evaluate(`HG.ai.wikiSearch("flaxen mane and tail colour", 6)`);
    c.ok("the wiki lookup finds the flaxen page", hits.some((h) => /flaxen/i.test(h.path)), JSON.stringify(hits.map((h) => h.path)));

    await page.evaluate(`(function () {
      var issue = { title: 'Foal "frozen" after portal', body: "## What happened\\nIt froze.\\n\\n## Log\\n    at com.example.Foo", labels: ["bug"] };
      window.__aiReplies.push("Flaxen is covered on [its page](wiki/gene-flaxen.html#science).\\n\\n\`\`\`bug-report\\n" +
        JSON.stringify(issue) + "\\n\`\`\`\\n\\nCheck it over.");
    })()`);
    await page.evaluate(`(function () {
      HG.aiChat.open();
      var panel = document.querySelector(".phc-ai-panel");
      panel.querySelector(".phc-ai-foot textarea").value = "My foal froze after a hay portal";
      panel.querySelector(".phc-ai-foot form").requestSubmit();
    })()`);
    c.ok("the chat reply arrives and renders a bug report form",
      await waitFor(page, `!!document.querySelector(".phc-ai-panel .phc-ai-card input")`), "");
    const chat = await page.evaluate(`(function () {
      var call = window.__aiCalls[0];
      var card = document.querySelector(".phc-ai-card");
      var link = document.querySelector(".phc-ai-msg.assistant a");
      return {
        title: card.querySelector("input").value,
        body: card.querySelector("textarea").value,
        link: link && link.getAttribute("href"),
        system: call.body.messages[0].content,
        referer: call.headers["HTTP-Referer"],
        auth: call.headers.Authorization,
        after: document.querySelector(".phc-ai-msg.assistant").textContent.indexOf("Check it over") >= 0
      };
    })()`);
    c.ok("the report's title survives quotes inside it", chat.title === 'Foal "frozen" after portal', chat.title);
    c.ok("the report's body keeps its log lines", /\n    at com\.example\.Foo/.test(chat.body), JSON.stringify(chat.body));
    c.ok("text after the report is still shown", chat.after, "");
    c.ok("a wiki link in a reply points into the wiki", /wiki\/gene-flaxen\.html#science$/.test(chat.link || ""), chat.link);
    c.ok("the model was given matching wiki excerpts and the page", /Wiki excerpts/.test(chat.system) && /AI helpers/.test(chat.system), chat.system.slice(0, 200));
    c.ok("the key goes as a Bearer header, with OpenRouter's attribution", chat.auth === "Bearer sk-or-test" && !!chat.referer, "");
    c.ok("similar issues are listed on the form", await waitFor(page, `/An older one/.test(document.querySelector(".phc-ai-card").textContent)`), "");

    const url = await page.evaluate(`HG.ai.newIssueUrl({ title: "t", body: "x".repeat(20000), labels: ["bug"] })`);
    c.ok("a long report still fits in a GitHub link", url.length <= 7600 && /cut%20short/i.test(url), url.length + " chars");
    c.ok("a model's javascript: link is not made clickable", (await page.evaluate(`HG.ai.link("javascript:alert(1)")`)) === null, "");

    const persisted = await page.evaluate(`JSON.parse(sessionStorage.getItem("phc-ai-chat")).length`);
    c.ok("the conversation is kept for the next page", persisted === 2, persisted + " messages");

    // The Anthropic format, through the same chat.
    await page.evaluate(`(function () {
      var s = HG.ai.load(); s.provider = "anthropic"; s.keys.anthropic = "sk-ant-test"; HG.ai.save(s);
      window.__aiReplies.push("Hello from Claude.");
      var panel = document.querySelector(".phc-ai-panel");
      panel.querySelector(".phc-ai-foot textarea").value = "hi";
      panel.querySelector(".phc-ai-foot form").requestSubmit();
    })()`);
    c.ok("an Anthropic reply streams into the chat", await waitFor(page, `/Hello from Claude/.test(document.querySelector(".phc-ai-log").textContent)`), "");
    const ant = await page.evaluate(`(function () { var k = window.__aiCalls[window.__aiCalls.length - 1]; return { url: k.url, h: k.headers, b: k.body }; })()`);
    c.ok("Anthropic gets its browser-access header and the key", ant.h["anthropic-dangerous-direct-browser-access"] === "true" && ant.h["x-api-key"] === "sk-ant-test", JSON.stringify(ant.h));
    c.ok("Anthropic gets the system prompt in its own field, and history alternates",
      typeof ant.b.system === "string" && ant.b.messages.map((m) => m.role).join(",") === "user,assistant,user", ant.b.messages.map((m) => m.role).join(","));
    c.ok("Claude Opus 5 is sent with the server-side fallback", ant.b.model === "claude-opus-5" && ant.b.fallbacks === "default" && /server-side-fallback/.test(ant.h["anthropic-beta"] || ""), "");

    await page.evaluate(`HG.ai.forgetEverything()`);
    c.ok("Forget everything leaves no key behind", !(await page.evaluate(`HG.ai.ready() || !!localStorage.getItem("phc-ai")`)), "");
    if (SHOTS) await page.screenshot(join(SHOTS, "ai-chat.png"));
    c.ok("nothing on the wiki page's console", page.errors().length === 0, page.errors().join(" ;; "));
  } finally {
    page.close();
  }
}

// ---- 2. the gene creator: a bad reply goes back, a good one loads

{
  const page = await open(pathToFileURL(resolve("wiki/gene-creator/index.html")).href, { port: 9227 });
  try {
    c.ok("the gene creator started", await waitFor(page, `!!(window.HG && HG.ui && HG.ui.state.spec && HG.ai && HG.ui.state.lastBake)`), "");
    await page.evaluate(STUB);
    await page.evaluate(`(function () {
      var good = JSON.parse(JSON.stringify(HG.examples[Object.keys(HG.examples)[0]]));
      good.key = "custom.ai_test";
      good.name = "AI test gene";
      var bad = JSON.parse(JSON.stringify(good));
      bad.key = "Not A Key";
      window.__aiReplies.push("Here it is.\\n\`\`\`json\\n" + JSON.stringify(bad) + "\\n\`\`\`");
      window.__aiReplies.push("Fixed.\\n\`\`\`json\\n" + JSON.stringify(good, null, 2) + "\\n\`\`\`");
    })()`);
    const r = await page.evaluate(DRAFT("a silver gene"));
    c.ok("a gene the creator refuses is sent back, and the fix is accepted", r.good && r.calls === 2, JSON.stringify(r));
    const second = await page.evaluate(`window.__aiCalls[1].body.messages.slice(-1)[0].content`);
    c.ok("the retry tells the model what was wrong", /did not load/.test(second) && /Key must be/.test(second), second.slice(0, 160));
    const sys = await page.evaluate(`window.__aiCalls[0].body.messages[0].content`);
    c.ok("the gene prompt carries the format page, the vocabulary and examples",
      /# The gene file format/.test(sys) && /DAPPLES/.test(sys) && /traversal/.test(sys) && /Example - /.test(sys), sys.length + " chars");
    await page.evaluate(USE_IT);
    c.ok("Use it loads the gene into the creator", await waitFor(page, `HG.ui.state.spec.key === "custom.ai_test"`), "");
    if (SHOTS) await page.screenshot(join(SHOTS, "ai-gene-draft.png"));
    await page.evaluate(CLOSE);
    c.ok("nothing on the gene creator's console", page.errors().length === 0, page.errors().join(" ;; "));
  } finally {
    page.close();
  }
}

// ---- 3. the breed designer (served over http - it will not run from a file)

{
  const TYPES = { ".html": "text/html", ".js": "text/javascript", ".mjs": "text/javascript", ".css": "text/css",
    ".json": "application/json", ".wasm": "application/wasm", ".png": "image/png", ".svg": "image/svg+xml" };
  const root = resolve(".");
  const server = createServer(async (req, res) => {
    try {
      const path = decodeURIComponent(new URL(req.url, "http://x").pathname);
      const file = resolve(join(root, path));
      if (!file.startsWith(root)) throw new Error("outside");
      const body = await readFile(file);
      res.writeHead(200, { "content-type": TYPES[extname(file)] || "application/octet-stream" });
      res.end(body);
    } catch {
      res.writeHead(404);
      res.end();
    }
  });
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  const port = server.address().port;
  const page = await open(`http://127.0.0.1:${port}/wiki/breed-designer/index.html`, { port: 9228, match: "http://127.0.0.1" });
  try {
    c.ok("the breed designer booted", await waitFor(page, `!!(window.HG && HG.bd && HG.bd.api && HG.ai && document.getElementById("boot").classList.contains("gone"))`, 200), "");
    await page.evaluate(STUB);
    await page.evaluate(`(function () {
      var id = JSON.parse(HG.bd.api.breedCatalogJson()).filter(function (b) { return !b.magical && b.id !== "feral_mixed"; })[0].id;
      var good = JSON.parse(HG.bd.api.breedFileJson(id));
      good.id = "ai_test_breed";
      good.name = "AI Test Breed";
      var bad = JSON.parse(JSON.stringify(good));
      bad.genes = Object.assign({ "horsegenetics.not_a_gene": [{ pair: "X/x", weight: 1 }] }, bad.genes || {});
      window.__aiReplies.push("\`\`\`json\\n" + JSON.stringify(bad) + "\\n\`\`\`");
      window.__aiReplies.push("\`\`\`json\\n" + JSON.stringify(good) + "\\n\`\`\`");
    })()`);
    const r = await page.evaluate(DRAFT("a test breed"));
    c.ok("a breed naming a gene that does not exist is sent back, and the fix is accepted", r.good && r.calls === 2, JSON.stringify(r));
    const second = await page.evaluate(`window.__aiCalls[1].body.messages.slice(-1)[0].content`);
    c.ok("the retry quotes the game's parser", /no gene "horsegenetics\.not_a_gene"/.test(second), second.slice(0, 200));
    const sys = await page.evaluate(`window.__aiCalls[0].body.messages[0].content`);
    c.ok("the breed prompt carries the format page, the live catalogue and real files",
      /# The breed file format/.test(sys) && /horsegenetics\.extension \|/.test(sys) && /Example - the mod's own/.test(sys), sys.length + " chars");
    await page.evaluate(USE_IT);
    c.ok("Use it loads the breed into the designer", await waitFor(page, `HG.bd.state.id === "ai_test_breed"`), "");
    const back = await page.evaluate(`(function () {
      var b = Array.prototype.find.call(document.querySelectorAll(".phc-ai-dialog button"), function (x) { return x.textContent === "Put back what I had"; });
      if (!b || b.hidden) return "hidden";
      b.click();
      return HG.bd.state.id;
    })()`);
    c.ok("Put back what I had restores the breed that was open", back !== "ai_test_breed", back);
    if (SHOTS) await page.screenshot(join(SHOTS, "ai-breed-draft.png"));
    await page.evaluate(CLOSE);
    c.ok("nothing on the breed designer's console", page.errors().length === 0, page.errors().join(" ;; "));
  } finally {
    page.close();
    server.close();
  }
}

process.exit(c.report("AI helpers"));
