/* The wiki's AI helpers - the part every page shares.
 *
 * Nothing here talks to anybody until the reader puts in a key of their own.
 * The key, the provider and model they chose, and an optional GitHub token live
 * in this browser's localStorage and are sent only to that provider (and the
 * token only to GitHub). There is no server of ours in between: every provider
 * listed below answered a browser's CORS preflight when it was added, which is
 * the whole reason it is listed and Google's Gemini API is not
 * (wiki/ai-setup.html#providers).
 *
 * Loaded by wiki/nav.js on every page, by index.html, and by the two designers.
 * ai-chat.js is the bubble in the corner; the designers' "Draft with AI" button
 * is draft() below, fed by wiki/breed-designer/js/bd-ai.js and
 * wiki/gene-creator/js/gc-ai.js.
 *
 * Classic script, no modules, no build: it has to work from file:// like the
 * rest of the wiki. The one thing that cannot is "Connect with OpenRouter",
 * which needs an https page for OpenRouter to send the reader back to.
 */
window.HG = window.HG || {};
(function (HG) {
  "use strict";
  if (HG.ai) return;

  var HERE = (function () {
    var self = document.currentScript;
    return self && self.src ? self.src.replace(/ai-core\.js(\?.*)?$/, "") : "";
  })();
  var WIKI = HERE + "../";
  var ROOT = HERE + "../../";          // the search index's hrefs are repo-root relative
  var STORE = "phc-ai";
  var REPO = "9-7-8/Procedural-Horse-Coats";
  var SITE = "https://9-7-8.github.io/Procedural-Horse-Coats/";

  (function stylesheet() {
    if (document.querySelector("link[data-phc-ai]")) return;
    var l = document.createElement("link");
    l.rel = "stylesheet";
    l.href = HERE + "ai.css";
    l.setAttribute("data-phc-ai", "");
    document.head.appendChild(l);
  })();

  // ---- providers ----------------------------------------------------------
  //
  // format "openai" is POST {base}/chat/completions with a Bearer key, which
  // OpenRouter and most of the others speak. "anthropic" is the Messages API.
  // Adding one: it must answer a CORS preflight from a github.io origin - check
  // with curl -X OPTIONS before listing it, then add a row to ai-setup.html.

  var PROVIDERS = [
    { id: "openrouter", name: "OpenRouter", format: "openai", base: "https://openrouter.ai/api/v1",
      keys: "https://openrouter.ai/settings/keys", model: "openrouter/free",
      note: "One key for hundreds of models, free ones included. The default." },
    { id: "anthropic", name: "Anthropic (Claude)", format: "anthropic", base: "https://api.anthropic.com/v1",
      keys: "https://platform.claude.com/settings/keys", model: "claude-opus-5",
      note: "Claude, straight from Anthropic. Paid; every model can see images." },
    { id: "openai", name: "OpenAI", format: "openai", base: "https://api.openai.com/v1",
      keys: "https://platform.openai.com/api-keys", model: "", note: "Paid." },
    { id: "groq", name: "Groq", format: "openai", base: "https://api.groq.com/openai/v1",
      keys: "https://console.groq.com/keys", model: "", note: "Fast open models, with a free tier." },
    { id: "mistral", name: "Mistral", format: "openai", base: "https://api.mistral.ai/v1",
      keys: "https://console.mistral.ai/api-keys", model: "", note: "" },
    { id: "deepseek", name: "DeepSeek", format: "openai", base: "https://api.deepseek.com",
      keys: "https://platform.deepseek.com/api_keys", model: "", note: "Text only." },
    { id: "xai", name: "xAI (Grok)", format: "openai", base: "https://api.x.ai/v1",
      keys: "https://console.x.ai", model: "", note: "" },
    { id: "together", name: "Together AI", format: "openai", base: "https://api.together.xyz/v1",
      keys: "https://api.together.ai/settings/api-keys", model: "", note: "" },
    { id: "custom", name: "Another OpenAI-compatible server", format: "openai", base: "", keys: "", model: "",
      note: "LM Studio, Ollama, a proxy of your own - anything that speaks /chat/completions and allows this page." }
  ];

  function providerById(id) {
    for (var i = 0; i < PROVIDERS.length; i++) if (PROVIDERS[i].id === id) return PROVIDERS[i];
    return null;
  }

  // ---- settings: this browser only ----------------------------------------

  function load() {
    var s = null;
    try { s = JSON.parse(window.localStorage.getItem(STORE) || "null"); } catch (e) { s = null; }
    s = s || {};
    return {
      provider: providerById(s.provider) ? s.provider : "openrouter",
      keys: s.keys || {},
      models: s.models || {},
      customBase: s.customBase || "",
      githubToken: s.githubToken || ""
    };
  }

  function save(s) {
    try { window.localStorage.setItem(STORE, JSON.stringify(s)); } catch (e) { /* private window */ }
    document.dispatchEvent(new CustomEvent("hg:ai"));
  }

  function forgetEverything() {
    try { window.localStorage.removeItem(STORE); } catch (e) { /* nothing stored */ }
    try {
      Object.keys(window.sessionStorage).forEach(function (k) {
        if (k.indexOf("phc-ai") === 0) window.sessionStorage.removeItem(k);
      });
    } catch (e) { /* nothing stored */ }
    document.dispatchEvent(new CustomEvent("hg:ai"));
  }

  function current() {
    var s = load();
    var p = providerById(s.provider);
    return {
      provider: p,
      key: s.keys[p.id] || "",
      model: s.models[p.id] || p.model || "",
      base: p.id === "custom" ? s.customBase.replace(/\/+$/, "") : p.base
    };
  }

  /** A key (or a custom server) and a model: enough to send something. */
  function ready() {
    var c = current();
    return !!(c.base && c.model && (c.key || c.provider.id === "custom"));
  }

  function describe() {
    var c = current();
    return ready() ? c.model + " through " + c.provider.name : "not set up";
  }

  // ---- talking to a model --------------------------------------------------

  /**
   * One reply, streamed. opts: { system, messages: [{role, text, images}],
   * onText(textSoFar), signal }. Resolves to the whole reply text.
   * An image is { mediaType, data (base64), dataUrl } - see imageFromFile.
   */
  function send(opts) {
    if (!ready()) return Promise.reject(new Error("The AI helper is not set up yet - it needs a key."));
    var c = current();
    return c.provider.format === "anthropic" ? sendAnthropic(c, opts) : sendOpenAI(c, opts);
  }

  function sendOpenAI(c, opts) {
    var messages = [{ role: "system", content: opts.system }];
    opts.messages.forEach(function (m) {
      if (m.role === "user" && m.images && m.images.length) {
        var parts = [{ type: "text", text: m.text || "(see the picture)" }];
        m.images.forEach(function (img) { parts.push({ type: "image_url", image_url: { url: img.dataUrl } }); });
        messages.push({ role: "user", content: parts });
      } else {
        messages.push({ role: m.role, content: m.text || "" });
      }
    });
    var headers = { "Content-Type": "application/json" };
    if (c.key) headers.Authorization = "Bearer " + c.key;
    if (c.provider.id === "openrouter") {
      // OpenRouter's app attribution; both are on its CORS allow-list.
      headers["HTTP-Referer"] = SITE;
      headers["X-Title"] = "Procedural Horse Genetics wiki";
    }
    return fetch(c.base + "/chat/completions", {
      method: "POST", headers: headers, signal: opts.signal,
      body: JSON.stringify({ model: c.model, messages: messages, stream: true })
    }).then(function (res) {
      if (!res.ok) return failure(res);
      if (!/event-stream/.test(res.headers.get("content-type") || "")) {
        // A server that ignored stream:true and answered in one go.
        return res.json().then(function (j) {
          if (j.error) throw new Error(j.error.message || JSON.stringify(j.error));
          var text = (j.choices && j.choices[0] && j.choices[0].message && j.choices[0].message.content) || "";
          if (opts.onText) opts.onText(text);
          return text;
        });
      }
      var text = "";
      return readSse(res, function (data) {
        if (data === "[DONE]") return;
        var j = JSON.parse(data);
        if (j.error) throw new Error(j.error.message || JSON.stringify(j.error));
        var choice = j.choices && j.choices[0];
        var d = choice && choice.delta && choice.delta.content;
        if (typeof d === "string" && d) {
          text += d;
          if (opts.onText) opts.onText(text);
        }
      }).then(function () {
        if (!text) throw new Error("The model sent back an empty reply. Try again, or pick another model.");
        return text;
      });
    });
  }

  function anthropicHeaders(key) {
    return {
      "Content-Type": "application/json",
      "x-api-key": key,
      "anthropic-version": "2023-06-01",
      // Anthropic's opt-in for calls straight from a browser, which is exactly
      // what a bring-your-own-key page is.
      "anthropic-dangerous-direct-browser-access": "true"
    };
  }

  function sendAnthropic(c, opts) {
    var messages = opts.messages.map(function (m) {
      var content = [];
      if (m.role === "user") {
        (m.images || []).forEach(function (img) {
          content.push({ type: "image", source: { type: "base64", media_type: img.mediaType, data: img.data } });
        });
      }
      content.push({ type: "text", text: m.text || "(see the picture)" });
      return { role: m.role, content: content };
    });
    var body = { model: c.model, max_tokens: 32000, system: opts.system, messages: messages, stream: true };
    var headers = anthropicHeaders(c.key);
    if (/^claude-(opus-5|fable-5-1)/.test(c.model)) {
      // A request the model's safety classifiers decline is re-run server-side
      // on Anthropic's recommended fallback instead of coming back refused.
      headers["anthropic-beta"] = "server-side-fallback-2026-07-01";
      body.fallbacks = "default";
    }
    return fetch(c.base + "/messages", {
      method: "POST", headers: headers, signal: opts.signal, body: JSON.stringify(body)
    }).then(function (res) {
      if (!res.ok) return failure(res);
      var text = "";
      var stop = null;
      return readSse(res, function (data) {
        var j = JSON.parse(data);
        if (j.type === "error") throw new Error((j.error && j.error.message) || "the stream failed");
        if (j.type === "content_block_delta" && j.delta && j.delta.type === "text_delta") {
          text += j.delta.text;
          if (opts.onText) opts.onText(text);
        }
        if (j.type === "message_delta" && j.delta && j.delta.stop_reason) stop = j.delta.stop_reason;
      }).then(function () {
        if (stop === "refusal" && !text.trim()) throw new Error("The model declined to answer that.");
        if (!text) throw new Error("The model sent back an empty reply.");
        return text;
      });
    });
  }

  /** Server-sent events: hands each data: payload to onData, in order. */
  function readSse(res, onData) {
    var reader = res.body.getReader();
    var decoder = new TextDecoder();
    var buf = "";
    function line(l) {
      if (l.indexOf("data:") !== 0) return;         // event: names, : comments, blanks
      var data = l.slice(5).trim();
      if (data) onData(data);
    }
    function pump() {
      return reader.read().then(function (r) {
        if (r.done) {
          if (buf) line(buf);
          return;
        }
        buf += decoder.decode(r.value, { stream: true });
        var i;
        while ((i = buf.indexOf("\n")) >= 0) {
          var l = buf.slice(0, i).replace(/\r$/, "");
          buf = buf.slice(i + 1);
          line(l);
        }
        return pump();
      });
    }
    return pump();
  }

  /** Turn a non-2xx response into an Error a reader can act on. */
  function failure(res) {
    return res.text().then(function (body) {
      var msg = body;
      try {
        var j = JSON.parse(body);
        msg = (j.error && (j.error.message || j.error)) || j.message || body;
      } catch (e) { /* not JSON */ }
      msg = String(msg).slice(0, 300);
      var hint = {
        400: "",
        401: " - the key was refused. Check it was pasted whole, or make a new one.",
        402: " - the account is out of credit.",
        403: " - the key is not allowed to do that.",
        404: " - that model or address does not exist here.",
        429: " - too many requests. Free models have a daily limit; wait, or pick another model."
      }[res.status] || "";
      throw new Error(res.status + ": " + msg + hint);
    });
  }

  // ---- models ----------------------------------------------------------------

  var NOT_CHAT = /embed|whisper|tts|audio|dall-e|moderation|transcri|realtime|rerank|guard|image-gen|imagen|lyria|sora|speech/i;

  function modelsCacheKey(c) { return "phc-ai-models:" + c.provider.id + ":" + c.base; }

  /** Can the model list be fetched yet? OpenRouter's is public; the rest need the key. */
  function canList() {
    var c = current();
    return !!c.base && (c.provider.id === "openrouter" || c.provider.id === "custom" || !!c.key);
  }

  /** [{id, label, image: true|false|null, free}] for the chosen provider. */
  function listModels(force) {
    var c = current();
    var cacheKey = modelsCacheKey(c);
    if (!force) {
      try {
        var hit = JSON.parse(window.sessionStorage.getItem(cacheKey) || "null");
        if (hit) return Promise.resolve(hit);
      } catch (e) { /* no cache */ }
    }
    var req;
    if (c.provider.format === "anthropic") {
      req = fetch(c.base + "/models?limit=100", { headers: anthropicHeaders(c.key) });
    } else {
      var h = {};
      if (c.key) h.Authorization = "Bearer " + c.key;
      req = fetch(c.base + "/models", { headers: h });
    }
    return req.then(function (res) { return res.ok ? res.json() : failure(res); }).then(function (j) {
      var raw = Array.isArray(j) ? j : (j.data || j.models || []);
      var list = raw.map(function (m) {
        if (c.provider.id === "openrouter") {
          var free = m.pricing && Number(m.pricing.prompt) === 0 && Number(m.pricing.completion) === 0;
          var image = ((m.architecture && m.architecture.input_modalities) || []).indexOf("image") >= 0;
          var label = m.id === "openrouter/free" ? "Any free model (openrouter/free)"
            : (m.name || m.id) + (free ? " - free" : "") + (image ? " - sees images" : "");
          return { id: m.id, label: label, image: image, free: free };
        }
        if (c.provider.format === "anthropic") {
          return { id: m.id, label: (m.display_name || m.id), image: true, free: false };
        }
        if (m.type && !/chat|language|code/.test(m.type)) return null;
        return { id: m.id, label: m.id, image: null, free: false };
      }).filter(function (m) { return m && !NOT_CHAT.test(m.id); });
      list.sort(function (a, b) {
        if (a.id === "openrouter/free") return -1;
        if (b.id === "openrouter/free") return 1;
        if (a.free !== b.free) return a.free ? -1 : 1;
        return a.label.localeCompare(b.label);
      });
      try { window.sessionStorage.setItem(cacheKey, JSON.stringify(list)); } catch (e) { /* too big, or private */ }
      return list;
    });
  }

  /** What the cached list says about a model, or null if it has not been listed. */
  function modelInfo(id) {
    try {
      var list = JSON.parse(window.sessionStorage.getItem(modelsCacheKey(current())) || "null") || [];
      for (var i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
    } catch (e) { /* no cache */ }
    return null;
  }

  // ---- images ----------------------------------------------------------------

  /**
   * Read a picture the reader attached, scaled so its long side is at most
   * `max` px - a phone photo is megabytes of tokens otherwise. A small one
   * (a coat texture, a crop) is scaled UP with no smoothing, so a 64 px coat
   * sheet reaches the model as crisp pixels it can actually see.
   */
  function imageFromFile(file, max) {
    max = max || 1024;
    return new Promise(function (resolve, reject) {
      var url = URL.createObjectURL(file);
      var img = new Image();
      img.onload = function () {
        var long = Math.max(img.width, img.height);
        var scale = long > max ? max / long : long < 384 ? Math.floor(384 / long) || 1 : 1;
        var w = Math.max(1, Math.round(img.width * scale));
        var h = Math.max(1, Math.round(img.height * scale));
        var canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        var ctx = canvas.getContext("2d");
        ctx.imageSmoothingEnabled = scale < 1;
        var keepAlpha = /png|gif|webp/.test(file.type);
        if (!keepAlpha) {
          ctx.fillStyle = "#808080";
          ctx.fillRect(0, 0, w, h);
        }
        ctx.drawImage(img, 0, 0, w, h);
        URL.revokeObjectURL(url);
        var type = keepAlpha ? "image/png" : "image/jpeg";
        var dataUrl = canvas.toDataURL(type, 0.88);
        resolve({ mediaType: type, dataUrl: dataUrl, data: dataUrl.slice(dataUrl.indexOf(",") + 1), name: file.name });
      };
      img.onerror = function () {
        URL.revokeObjectURL(url);
        reject(new Error("That file is not a picture this browser can open."));
      };
      img.src = url;
    });
  }

  // ---- the wiki, as context ----------------------------------------------------
  //
  // The same index the search box uses (wiki/search-index.js), scored
  // differently: a chat message is a sentence, so a chunk earns points for each
  // word it has rather than being dropped for missing one.

  var indexLoading = null;
  function wikiIndex() {
    if (HG.searchIndex) return Promise.resolve(HG.searchIndex);
    if (!indexLoading) {
      indexLoading = new Promise(function (resolve, reject) {
        var s = document.createElement("script");
        s.src = WIKI + "search-index.js";
        s.onload = function () {
          if (HG.searchIndex) resolve(HG.searchIndex);
          else reject(new Error("the wiki's search index loaded empty"));
        };
        s.onerror = function () {
          indexLoading = null;
          reject(new Error("could not load the wiki's search index"));
        };
        document.head.appendChild(s);
      });
    }
    return indexLoading;
  }

  var STOP = " about after again also and any are because been before being but can cant could did does doesnt doing dont each even for from get gets got had has have having how into its ive just like make many more most much need not now off once one only other our out over really same should some such than that thats the their them then there these they thing things this those through too under until very was way wee well were what when where which while who why will with would yes yet you your youre ";

  function termsOf(q) {
    var seen = {};
    return q.toLowerCase().replace(/[^a-z0-9_'\-\s]/g, " ").split(/\s+/).filter(function (t) {
      t = t.replace(/^'+|'+$/g, "");
      if (t.length < 3 || STOP.indexOf(" " + t + " ") >= 0 || seen[t]) return false;
      seen[t] = true;
      return true;
    }).slice(0, 14);
  }

  function countIn(body, t, cap) {
    var n = 0;
    var at = body.indexOf(t);
    while (at >= 0 && n < cap) {
      n++;
      at = body.indexOf(t, at + t.length);
    }
    return n;
  }

  function chunkHref(page, chunk) {
    var url = page.href;
    if (chunk.tab && chunk.tab !== "all") url += "?view=" + chunk.tab;
    if (chunk.id) url += "#" + chunk.id;
    return url;
  }

  function excerpt(text, terms, size) {
    if (text.length <= size) return text;
    var low = text.toLowerCase();
    var at = -1;
    terms.forEach(function (t) {
      var i = low.indexOf(t);
      if (i >= 0 && (at < 0 || i < at)) at = i;
    });
    var from = Math.max(0, (at < 0 ? 0 : at) - Math.floor(size / 3));
    var cut = text.slice(from, from + size);
    return (from > 0 ? "…" : "") + cut + (from + size < text.length ? "…" : "");
  }

  /** The best-matching wiki passages: [{title, heading, path, text}]. */
  function wikiSearch(query, limit) {
    limit = limit || 6;
    return wikiIndex().then(function (ix) {
      var ts = termsOf(query);
      if (!ts.length) return [];
      var hits = [];
      ix.pages.forEach(function (page) {
        var title = page.title.toLowerCase();
        page.chunks.forEach(function (chunk) {
          if (chunk._low === undefined) chunk._low = chunk.t.toLowerCase();
          var head = chunk.h.toLowerCase();
          var score = 0;
          var matched = 0;
          ts.forEach(function (t) {
            var inTitle = title.indexOf(t) >= 0;
            var inHead = head.indexOf(t) >= 0;
            var n = countIn(chunk._low, t, 5);
            if (inTitle || inHead || n) matched++;
            score += (inTitle ? 6 : 0) + (inHead ? 4 : 0) + n;
          });
          if (matched) hits.push({ page: page, chunk: chunk, score: score * matched / ts.length });
        });
      });
      hits.sort(function (a, b) { return b.score - a.score; });
      var perPage = {};
      var out = [];
      for (var i = 0; i < hits.length && out.length < limit; i++) {
        var h = hits[i];
        perPage[h.page.href] = (perPage[h.page.href] || 0) + 1;
        if (perPage[h.page.href] > 2) continue;
        out.push({ title: h.page.title, heading: h.chunk.h, path: chunkHref(h.page, h.chunk),
          text: excerpt(h.chunk.t, ts, 1400) });
      }
      return out;
    });
  }

  /**
   * A whole page as plain text, heading by heading, for the designers' prompts:
   * the format reference is the wiki page, so there is no second copy of it to
   * drift. `tabs` narrows to those tab panels (making-a-gene's "file", say).
   */
  function wikiPage(href, tabs) {
    return wikiIndex().then(function (ix) {
      var page = null;
      ix.pages.forEach(function (p) { if (p.href === href) page = p; });
      if (!page) throw new Error("the wiki index has no page " + href);
      return page.chunks.filter(function (c) { return !tabs || tabs.indexOf(c.tab) >= 0; })
        .map(function (c) { return (c.h ? "## " + c.h + "\n" : "") + c.t; }).join("\n\n");
    });
  }

  /** A link the model wrote, made safe and pointed at the right place. */
  function link(href) {
    href = String(href || "").trim();
    if (/^https?:\/\//i.test(href)) return href;
    if (/^[a-z][a-z0-9+.-]*:/i.test(href)) return null;     // javascript:, data: and friends
    if (href.charAt(0) === "#") return href;
    href = href.replace(/^\.?\//, "");
    // "gene-flaxen.html" when the excerpt said "wiki/gene-flaxen.html".
    if (/^[\w-]+\.html/.test(href) && !/^index\.html/.test(href)) href = "wiki/" + href;
    return ROOT + href;
  }

  /** The contents of the first ```tag fence, or null. For "json", a bare fence or a lone {...} will do. */
  function block(text, tag) {
    var re = new RegExp("```" + tag + "[ \\t]*\\n([\\s\\S]*?)```", "i");
    var m = re.exec(text);
    if (m) return m[1];
    if (tag === "json") {
      m = /```[ \t]*\n([\s\S]*?)```/.exec(text);
      if (m && m[1].trim().charAt(0) === "{") return m[1];
      var a = text.indexOf("{");
      var b = text.lastIndexOf("}");
      if (a >= 0 && b > a) return text.slice(a, b + 1);
    }
    return null;
  }

  // ---- GitHub ------------------------------------------------------------------

  var ISSUES = "https://github.com/" + REPO + "/issues";

  /** GitHub's new-issue page with everything filled in; the reader presses Submit. */
  function newIssueUrl(issue) {
    function build(body) {
      return ISSUES + "/new?title=" + encodeURIComponent(issue.title || "") +
        "&body=" + encodeURIComponent(body) +
        (issue.labels && issue.labels.length ? "&labels=" + encodeURIComponent(issue.labels.join(",")) : "");
    }
    var body = issue.body || "";
    var url = build(body);
    // Past ~8 KB a link gets refused along the way; cut the body, and say so.
    while (url.length > 7500 && body.length > 400) {
      body = body.slice(0, Math.floor(body.length * 0.8));
      url = build(body + "\n\n_(Cut short to fit in a link - paste the rest of the log below.)_");
    }
    return url;
  }

  function githubHeaders() {
    var h = { Accept: "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28" };
    var token = load().githubToken;
    if (token) h.Authorization = "Bearer " + token;
    return h;
  }

  /** File the issue as the token's owner. Resolves {url, number}. */
  function createIssue(issue) {
    if (!load().githubToken) return Promise.reject(new Error("No GitHub token is saved."));
    var h = githubHeaders();
    h["Content-Type"] = "application/json";
    return fetch("https://api.github.com/repos/" + REPO + "/issues", {
      method: "POST", headers: h,
      // Labels are dropped silently by GitHub for anyone without push access.
      body: JSON.stringify({ title: issue.title, body: issue.body, labels: issue.labels || [] })
    }).then(function (res) { return res.ok ? res.json() : failure(res); })
      .then(function (j) { return { url: j.html_url, number: j.number }; });
  }

  /** Open or closed issues that look like this one. Unauthenticated search allows ten a minute. */
  function similarIssues(text) {
    var ts = termsOf(text).slice(0, 5);
    if (!ts.length) return Promise.resolve([]);
    var q = encodeURIComponent(ts.join(" ") + " repo:" + REPO + " is:issue");
    return fetch("https://api.github.com/search/issues?per_page=5&q=" + q, { headers: githubHeaders() })
      .then(function (res) { return res.ok ? res.json() : failure(res); })
      .then(function (j) {
        return (j.items || []).map(function (it) {
          return { title: it.title, url: it.html_url, number: it.number, state: it.state };
        });
      });
  }

  // ---- "Connect with OpenRouter" (OAuth PKCE) -------------------------------------

  var PKCE = "phc-ai-pkce";

  function base64url(bytes) {
    var s = "";
    for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
    return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  function canConnect() {
    return window.isSecureContext && /^https?:$/.test(location.protocol) && !!(window.crypto && crypto.subtle);
  }

  function connectOpenRouter() {
    if (!canConnect()) {
      return Promise.reject(new Error("Connecting needs the wiki opened from its web address (https), not from a file. Paste a key instead."));
    }
    var bytes = new Uint8Array(32);
    crypto.getRandomValues(bytes);
    var verifier = base64url(bytes);
    return crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier)).then(function (hash) {
      try {
        window.sessionStorage.setItem(PKCE, JSON.stringify({ verifier: verifier, hash: location.hash }));
      } catch (e) {
        throw new Error("This browser will not store anything for the page, so connecting cannot finish. Paste a key instead.");
      }
      var back = location.origin + location.pathname;
      location.href = "https://openrouter.ai/auth?callback_url=" + encodeURIComponent(back) +
        "&code_challenge=" + base64url(new Uint8Array(hash)) + "&code_challenge_method=S256";
    });
  }

  function finishOpenRouter() {
    var m = /[?&]code=([^&#]+)/.exec(location.search);
    var pending = null;
    try { pending = JSON.parse(window.sessionStorage.getItem(PKCE) || "null"); } catch (e) { pending = null; }
    if (!m || !pending) return;
    try { window.sessionStorage.removeItem(PKCE); } catch (e) { /* gone anyway */ }
    history.replaceState(null, "", location.pathname + (pending.hash || ""));
    fetch("https://openrouter.ai/api/v1/auth/keys", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: decodeURIComponent(m[1]), code_verifier: pending.verifier, code_challenge_method: "S256" })
    }).then(function (res) { return res.ok ? res.json() : failure(res); }).then(function (j) {
      if (!j.key) throw new Error("OpenRouter answered without a key");
      var s = load();
      s.provider = "openrouter";
      s.keys.openrouter = j.key;
      save(s);
      toast("Connected to OpenRouter. The key is saved in this browser and nowhere else.");
    }).catch(function (e) {
      toast("Connecting to OpenRouter did not finish: " + e.message, true);
    });
  }

  // ---- small DOM helpers ------------------------------------------------------------

  function el(tag, attrs, children) {
    var e = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      var v = attrs[k];
      if (v === undefined || v === null || v === false) return;
      if (k === "text") e.textContent = v;
      else if (k.indexOf("on") === 0 && typeof v === "function") e.addEventListener(k.slice(2), v);
      else e.setAttribute(k, v === true ? "" : v);
    });
    (children || []).forEach(function (c) {
      if (c) e.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    });
    return e;
  }

  function toast(message, bad) {
    var t = el("div", { class: "phc-ai phc-ai-toast" + (bad ? " bad" : ""), role: "status", text: message });
    document.body.appendChild(t);
    setTimeout(function () { t.classList.add("gone"); }, 6000);
    setTimeout(function () { t.remove(); }, 6600);
  }

  function field(label, control, hint) {
    return el("div", { class: "phc-ai-field" }, [
      el("span", { class: "phc-ai-label", text: label }),
      control,
      hint ? (typeof hint === "string" ? el("span", { class: "phc-ai-hint", text: hint }) : hint) : null
    ]);
  }

  // ---- the settings form ----------------------------------------------------------
  //
  // One form, used in three places: the setup page (any element with
  // data-ai-settings), the chat panel before a key is in, and the draft dialog.

  function renderSettings(host, opts) {
    opts = opts || {};
    host.innerHTML = "";
    host.classList.add("phc-ai", "phc-ai-settings");
    var s = load();
    var p = providerById(s.provider);
    function redraw() { renderSettings(host, opts); }

    var providerSelect = el("select", { "aria-label": "AI provider" });
    PROVIDERS.forEach(function (x) {
      var o = el("option", { value: x.id, text: x.name });
      if (x.id === p.id) o.selected = true;
      providerSelect.appendChild(o);
    });
    providerSelect.addEventListener("change", function () {
      var t = load();
      t.provider = providerSelect.value;
      save(t);
      redraw();
    });
    host.appendChild(field("Provider", providerSelect, p.note));

    if (p.id === "openrouter") {
      host.appendChild(el("div", { class: "phc-ai-row" }, [
        el("button", { type: "button", class: "phc-ai-btn primary", text: "Connect with OpenRouter",
          onclick: function () { connectOpenRouter().catch(function (e) { toast(e.message, true); }); } }),
        el("span", { class: "phc-ai-hint", text: "sign in or sign up there, and the key comes back here by itself" })
      ]));
    }

    if (p.id === "custom") {
      var baseInput = el("input", { type: "url", placeholder: "http://localhost:1234/v1", value: s.customBase, spellcheck: "false" });
      baseInput.addEventListener("change", function () {
        var t = load();
        t.customBase = baseInput.value.trim();
        save(t);
        redraw();
      });
      host.appendChild(field("Server address", baseInput,
        "Everything before /chat/completions. The server has to allow requests from this page (CORS)."));
    }

    var keyInput = el("input", { type: "password", autocomplete: "off", spellcheck: "false",
      placeholder: p.id === "custom" ? "only if your server wants one" : "paste your key", value: s.keys[p.id] || "" });
    var show = el("button", { type: "button", class: "phc-ai-btn", text: "Show", onclick: function () {
      keyInput.type = keyInput.type === "password" ? "text" : "password";
      show.textContent = keyInput.type === "password" ? "Show" : "Hide";
    } });
    var saveKey = el("button", { type: "button", class: "phc-ai-btn primary", text: "Save", onclick: function () {
      var t = load();
      var v = keyInput.value.trim();
      if (v) t.keys[p.id] = v; else delete t.keys[p.id];
      save(t);
      redraw();
      toast(v ? "Key saved in this browser." : "Key removed.");
    } });
    var forget = s.keys[p.id] ? el("button", { type: "button", class: "phc-ai-btn danger", text: "Forget key", onclick: function () {
      var t = load();
      delete t.keys[p.id];
      save(t);
      redraw();
      toast("Key forgotten - it is no longer stored in this browser.");
    } }) : null;
    host.appendChild(field("API key", el("div", { class: "phc-ai-inline" }, [keyInput, show, saveKey, forget]),
      p.keys ? el("span", { class: "phc-ai-hint" }, [
        "Stored only in this browser. ",
        el("a", { href: p.keys, target: "_blank", rel: "noopener", text: "Get a " + p.name + " key" })
      ]) : "Stored only in this browser."));

    var modelSelect = el("select", { "aria-label": "Model" });
    var modelInput = el("input", { type: "text", placeholder: "or type a model id", spellcheck: "false" });
    var modelHint = el("span", { class: "phc-ai-hint" });
    function setModel(id) {
      var t = load();
      if (id) t.models[p.id] = id; else delete t.models[p.id];
      save(t);
      explain();
    }
    function explain() {
      var c = current();
      var info = modelInfo(c.model);
      modelHint.textContent = !c.model ? "Pick a model once the list loads."
        : info && info.image === true ? "This model can see pictures."
        : info && info.image === false ? "This model reads text only - pictures you attach will not reach it."
        : "";
    }
    function fill(force) {
      var c = current();
      modelSelect.innerHTML = "";
      if (!canList()) {
        modelSelect.appendChild(el("option", { value: c.model, text: c.model || "save a key to see the models" }));
        explain();
        return;
      }
      modelSelect.appendChild(el("option", { value: c.model, text: (c.model || "no model yet") + " - loading the list…" }));
      listModels(force).then(function (list) {
        var now = current().model;
        modelSelect.innerHTML = "";
        if (!now) modelSelect.appendChild(el("option", { value: "", text: "pick a model…" }));
        var known = list.some(function (m) { return m.id === now; });
        if (now && !known) modelSelect.appendChild(el("option", { value: now, text: now }));
        list.forEach(function (m) {
          var o = el("option", { value: m.id, text: m.label });
          if (m.id === now) o.selected = true;
          modelSelect.appendChild(o);
        });
        explain();
      }, function (e) {
        modelSelect.innerHTML = "";
        modelSelect.appendChild(el("option", { value: c.model, text: (c.model || "no model") + " - the list did not load: " + e.message }));
        explain();
      });
    }
    modelSelect.addEventListener("change", function () { setModel(modelSelect.value); });
    modelInput.addEventListener("change", function () {
      var v = modelInput.value.trim();
      if (!v) return;
      setModel(v);
      modelInput.value = "";
      fill(false);
    });
    var refresh = el("button", { type: "button", class: "phc-ai-btn", text: "Refresh", onclick: function () { fill(true); } });
    host.appendChild(field("Model", el("div", { class: "phc-ai-inline" }, [modelSelect, refresh]), modelHint));
    host.appendChild(el("div", { class: "phc-ai-inline narrow" }, [modelInput]));
    fill(false);

    var result = el("span", { class: "phc-ai-hint" });
    host.appendChild(el("div", { class: "phc-ai-row" }, [
      el("button", { type: "button", class: "phc-ai-btn", text: "Test it", onclick: function () {
        result.textContent = "asking…";
        send({ system: "Reply with the single word OK.", messages: [{ role: "user", text: "Are you there?" }] })
          .then(function (t) { result.textContent = "It answered: " + t.trim().slice(0, 80); },
                function (e) { result.textContent = e.message; });
      } }),
      result
    ]));

    var tokenInput = el("input", { type: "password", autocomplete: "off", spellcheck: "false",
      placeholder: "ghp_… (optional)", value: s.githubToken });
    var gh = el("details", { class: "phc-ai-gh" }, [
      el("summary", { text: "GitHub token, for sending bug reports (optional)" }),
      el("p", { class: "phc-ai-hint", text: "Without one, a finished bug report opens GitHub with everything filled in and you press Submit. With a classic token that has the public_repo scope, the helper files it for you." }),
      el("div", { class: "phc-ai-inline" }, [
        tokenInput,
        el("button", { type: "button", class: "phc-ai-btn primary", text: "Save", onclick: function () {
          var t = load();
          t.githubToken = tokenInput.value.trim();
          save(t);
          redraw();
          toast(t.githubToken ? "GitHub token saved in this browser." : "GitHub token removed.");
        } }),
        s.githubToken ? el("button", { type: "button", class: "phc-ai-btn danger", text: "Forget token", onclick: function () {
          var t = load();
          t.githubToken = "";
          save(t);
          redraw();
          toast("GitHub token forgotten.");
        } }) : null
      ])
    ]);
    if (s.githubToken || opts.openGithub) gh.open = true;
    host.appendChild(gh);

    host.appendChild(el("div", { class: "phc-ai-row" }, [
      opts.compact ? el("a", { href: WIKI + "ai-setup.html", text: "How to get a key, and what gets sent where" }) : null,
      el("button", { type: "button", class: "phc-ai-btn danger ghost", text: "Forget everything", onclick: function () {
        if (!window.confirm("Remove every key, the GitHub token and the model choices from this browser?")) return;
        forgetEverything();
        redraw();
        toast("Everything the AI helper stored in this browser is gone.");
      } })
    ]));
  }

  // ---- the designers' "Draft with AI" dialog ------------------------------------------
  //
  // opts: {
  //   title, intro, placeholder,
  //   system()          -> Promise<string>: the format, the catalogue, examples
  //   current()         -> the file open now, as text, or "" when it is blank
  //   validate(reply)   -> { ok, value, error, warnings, verdict, summary }, checked by the tool's own rules
  //   apply(value)      -> load it into the tool
  //   restore(text)     -> put back what current() returned
  // }
  // A reply that does not load goes back to the model with the parser's words,
  // up to three tries, so the reader only ever gets a file the game accepts.

  var TRIES = 3;

  function draft(opts) {
    var overlay = el("div", { class: "phc-ai phc-ai-overlay" });
    var box = el("div", { class: "phc-ai-dialog", role: "dialog", "aria-modal": "true", "aria-label": opts.title });
    overlay.appendChild(box);
    var controller = null;
    var history = [];
    var images = [];
    var before = null;

    function close() {
      if (controller) controller.abort();
      document.removeEventListener("hg:ai", refreshSetup);
      document.removeEventListener("keydown", onKey);
      overlay.remove();
    }
    function onKey(e) { if (e.key === "Escape") close(); }
    document.addEventListener("keydown", onKey);
    overlay.addEventListener("mousedown", function (e) { if (e.target === overlay) close(); });

    box.appendChild(el("div", { class: "phc-ai-dialog-head" }, [
      el("h2", { text: opts.title }),
      el("button", { type: "button", class: "phc-ai-icon", "aria-label": "Close", text: "×", onclick: close })
    ]));
    box.appendChild(el("p", { class: "phc-ai-hint", text: opts.intro }));

    var setup = el("div", { class: "phc-ai-setup" });
    var using = el("p", { class: "phc-ai-using" });
    function refreshSetup() {
      var ok = ready();
      setup.hidden = ok;
      if (!ok && !setup.childNodes.length) renderSettings(setup, { compact: true });
      using.innerHTML = "";
      if (ok) {
        using.appendChild(document.createTextNode("Using " + describe() + ". "));
        using.appendChild(el("a", { href: "#", text: "change", onclick: function (e) {
          e.preventDefault();
          setup.hidden = !setup.hidden;
          if (!setup.childNodes.length) renderSettings(setup, { compact: true });
        } }));
      }
      go.disabled = !ok;
    }
    document.addEventListener("hg:ai", refreshSetup);

    var prompt = el("textarea", { rows: "4", placeholder: opts.placeholder });
    var thumbs = el("div", { class: "phc-ai-thumbs" });
    var fileInput = el("input", { type: "file", accept: "image/*", multiple: true });
    fileInput.addEventListener("change", function () {
      var files = Array.prototype.slice.call(fileInput.files || [], 0, 4 - images.length);
      fileInput.value = "";
      files.forEach(function (f) {
        imageFromFile(f).then(function (img) { images.push(img); drawThumbs(); }, function (e) { toast(e.message, true); });
      });
    });
    function drawThumbs() {
      thumbs.innerHTML = "";
      images.forEach(function (img, i) {
        thumbs.appendChild(el("span", { class: "phc-ai-thumb" }, [
          el("img", { src: img.dataUrl, alt: img.name || "attached picture" }),
          el("button", { type: "button", "aria-label": "Remove picture", text: "×",
            onclick: function () { images.splice(i, 1); drawThumbs(); } })
        ]));
      });
    }
    var hasCurrent = !!(opts.current() || "").trim();
    var refine = el("input", { type: "checkbox" });
    refine.checked = hasCurrent;
    var refineRow = hasCurrent ? el("label", { class: "phc-ai-check" }, [refine, " Change the one open now (untick to start from scratch)"]) : null;

    var go = el("button", { type: "button", class: "phc-ai-btn primary", text: "Write it" });
    var stop = el("button", { type: "button", class: "phc-ai-btn", text: "Stop", hidden: true,
      onclick: function () { if (controller) controller.abort(); } });
    var status = el("p", { class: "phc-ai-status" });
    var raw = el("pre", { class: "phc-ai-raw" });
    var rawBox = el("details", { class: "phc-ai-rawbox" }, [el("summary", { text: "What the AI wrote" }), raw]);
    var outcome = el("div", { class: "phc-ai-outcome" });

    box.appendChild(using);
    box.appendChild(setup);
    box.appendChild(prompt);
    box.appendChild(el("div", { class: "phc-ai-row" }, [
      el("label", { class: "phc-ai-btn file" }, ["Attach a picture", fileInput]),
      thumbs
    ]));
    if (refineRow) box.appendChild(refineRow);
    box.appendChild(el("div", { class: "phc-ai-row" }, [go, stop]));
    box.appendChild(status);
    box.appendChild(outcome);
    box.appendChild(rawBox);
    refreshSetup();

    function setStatus(text, kind) {
      status.textContent = text;
      status.className = "phc-ai-status" + (kind ? " " + kind : "");
    }

    function attempt(system, n) {
      controller = new AbortController();
      raw.textContent = "";
      return send({ system: system, messages: history, signal: controller.signal,
        onText: function (t) { raw.textContent = t; } })
        .then(function (reply) {
          history.push({ role: "assistant", text: reply });
          var r;
          try { r = opts.validate(reply); } catch (e) { r = { ok: false, error: e.message }; }
          if (r.ok) return r;
          if (n >= TRIES) throw new Error("After " + TRIES + " tries it still did not load: " + r.error);
          setStatus("Try " + n + " did not load (" + r.error + ") - sending that back to fix…");
          history.push({ role: "user", text: "That did not load. The game's parser said:\n" + r.error +
            "\n\nFix it and send the whole file again, in one ```json block." });
          return attempt(system, n + 1);
        });
    }

    go.addEventListener("click", function () {
      var words = prompt.value.trim();
      if (!words && !images.length) {
        setStatus("Say what you want first, or attach a picture.", "bad");
        return;
      }
      var info = modelInfo(current().model);
      var sendImages = images.slice();
      if (sendImages.length && info && info.image === false) {
        toast("The model you picked cannot see pictures, so only your words go to it.", true);
        sendImages = [];
      }
      var text = words || "Make it look like the picture.";
      if (!history.length && refine.checked && hasCurrent) {
        text += "\n\nChange this file, the one open now, rather than starting over:\n```json\n" + opts.current() + "\n```";
      }
      history.push({ role: "user", text: text, images: sendImages });
      prompt.value = "";
      images = [];
      drawThumbs();
      go.disabled = true;
      stop.hidden = false;
      outcome.innerHTML = "";
      setStatus("Writing…");
      Promise.resolve(opts.system()).then(function (system) {
        return attempt(system, 1);
      }).then(function (r) {
        setStatus("✓ " + (r.verdict || "It loads.") + (r.summary ? " " + r.summary : ""), "good");
        if (r.warnings && r.warnings.length) {
          outcome.appendChild(el("ul", { class: "phc-ai-warnings" }, r.warnings.map(function (w) { return el("li", { text: w }); })));
        }
        var put = el("button", { type: "button", class: "phc-ai-btn primary", text: "Use it", onclick: function () {
          if (before === null) before = opts.current();
          opts.apply(r.value);
          put.disabled = true;
          put.textContent = "In use";
          back.hidden = false;           // before may be "": putting back a blank one is a reset
          prompt.placeholder = "Anything to change? Say so and it will rewrite it.";
          go.textContent = "Change it";
        } });
        var back = el("button", { type: "button", class: "phc-ai-btn", text: "Put back what I had", hidden: true, onclick: function () {
          opts.restore(before);
          back.hidden = true;
          put.disabled = false;
          put.textContent = "Use it again";
        } });
        outcome.appendChild(el("div", { class: "phc-ai-row" }, [put, back]));
      }).catch(function (e) {
        if (e && e.name === "AbortError") {
          setStatus("Stopped.");
          history.pop();
        } else {
          setStatus(e.message, "bad");
          rawBox.open = true;
        }
      }).then(function () {
        controller = null;
        go.disabled = !ready();
        stop.hidden = true;
      });
    });

    document.body.appendChild(overlay);
    prompt.focus();
  }

  // ---- boot ---------------------------------------------------------------------

  function boot() {
    Array.prototype.forEach.call(document.querySelectorAll("[data-ai-settings]"), function (host) {
      renderSettings(host, { openGithub: host.hasAttribute("data-ai-github") });
    });
    finishOpenRouter();
  }

  HG.ai = {
    PROVIDERS: PROVIDERS, WIKI: WIKI, ROOT: ROOT, ISSUES: ISSUES,
    load: load, save: save, current: current, ready: ready, describe: describe, forgetEverything: forgetEverything,
    send: send, listModels: listModels, modelInfo: modelInfo, imageFromFile: imageFromFile,
    wikiSearch: wikiSearch, wikiPage: wikiPage, link: link, block: block,
    newIssueUrl: newIssueUrl, createIssue: createIssue, similarIssues: similarIssues,
    connectOpenRouter: connectOpenRouter,
    renderSettings: renderSettings, draft: draft, el: el, toast: toast
  };

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot);
  else boot();
})(window.HG);
