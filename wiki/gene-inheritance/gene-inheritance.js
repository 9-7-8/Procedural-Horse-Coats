// The inheritance table on a gene page's gameplay tab: every genotype at this
// locus, and the horse each one gives.
//
// USING IT. One line, on the gameplay panel:
//
//     <div class="gene-inheritance" data-gene="horsegenetics.flaxen"></div>
//
// It asks the mod (DesignerApi.geneInheritanceJson, through the same wasm the
// gene preview and the horse designer run) for the alleles and the phenotype of
// every pair, and draws the grid from that. Nothing about any particular gene
// is written here, so a gene that gains an allele gains a row and a column and
// no page is edited - and a wiki that would otherwise be quoting a combination
// table it has to keep in step by hand cannot fall out of step at all.
//
// It draws a grid only for a locus with a small enough alphabet to read one -
// GRID_MAX alleles. Above that a square is unreadable and actively offputting,
// which is the opposite of what the gameplay tab is for, so it falls back to a
// plain list of the outcomes. A page can force the list with data-style="list".
//
// Needs the wiki over http, like every other wasm-backed widget here.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // Three alleles is nine cells, which reads at a glance. Four is sixteen and
  // already wants scrolling on a phone.
  var GRID_MAX = 3;

  var HERE = (function () {
    var self = document.currentScript;
    if (!self || !self.src) return "";
    return self.src.replace(/gene-inheritance\.js(\?.*)?$/, "");
  })();
  var WIKI = HERE + "../";

  function script(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement("script");
      s.src = src;
      s.onload = resolve;
      s.onerror = function () { reject(new Error("could not load " + src)); };
      document.head.appendChild(s);
    });
  }

  var booted = null;
  function boot() {
    if (booted) return booted;
    booted = Promise.resolve()
      .then(function () { return HG.java ? null : script(WIKI + "horse-designer/js/java.js"); })
      .then(function () { return HG.java.load(); });
    return booted;
  }

  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  // A phenotype name for a player. The mod's ids are kebab-case tokens meant
  // for a genotype code; "strong-flaxen" is fine to read, "wild" is not.
  function phenotype(cell) {
    if (cell.wild) { return "no change"; }
    return String(cell.name || "").replace(/-/g, " ");
  }

  function mount(host) {
    var key = host.getAttribute("data-gene");
    var forced = host.getAttribute("data-style");
    host.classList.add("gene-inheritance");
    host.innerHTML = '<div class="gi-boot">Asking the mod for this gene&hellip;</div>';

    boot().then(function (api) {
      var gene = JSON.parse(api.geneInheritanceJson(key));
      if (gene.missing) {
        host.innerHTML = '<div class="gi-boot gi-failed">No gene is registered as <code>'
          + esc(key) + "</code>.</div>";
        return;
      }
      var grid = forced !== "list" && gene.alleles.length <= GRID_MAX;
      host.innerHTML = grid ? drawGrid(gene) : drawList(gene);
    }, function (err) {
      host.innerHTML = '<div class="gi-boot gi-failed">The inheritance table could not load: '
        + esc(err.message) + "."
        + (location.protocol === "file:"
          ? " This page is open from a file:// path, and asking the mod is a fetch -"
          + " serve the repo with any static server, or read it on the published wiki."
          : "")
        + "</div>";
    });
  }

  /**
   * The square. Rows and columns are the alleles one parent can hand down, so
   * a cell is one foal's genotype - and it carries the phenotype too, because
   * a table of genotypes alone is exactly the thing a player cannot read.
   */
  function drawGrid(gene) {
    var al = gene.alleles;
    var by = {};
    gene.cells.forEach(function (c) {
      by[c.a + "/" + c.b] = c;
      by[c.b + "/" + c.a] = c;    // the pair is unordered
    });

    var h = '<div class="table-wrap"><table class="gi-grid">';
    h += "<thead><tr><th></th>";
    al.forEach(function (a) {
      h += '<th scope="col"><code>' + esc(a.token) + "</code></th>";
    });
    h += "</tr></thead><tbody>";

    al.forEach(function (rowAllele) {
      h += '<tr><th scope="row"><code>' + esc(rowAllele.token) + "</code></th>";
      al.forEach(function (colAllele) {
        var c = by[rowAllele.token + "/" + colAllele.token];
        if (!c) {
          h += '<td class="gi-none" aria-label="no such horse">&mdash;</td>';
          return;
        }
        h += '<td class="' + (c.wild ? "gi-wild" : "") + '">'
          + '<code class="gi-geno">' + esc(c.tokens) + "</code>"
          + '<span class="gi-pheno">' + esc(phenotype(c))
          + (c.varies ? ' <em title="varies from horse to horse">varies</em>' : "")
          + "</span></td>";
      });
      h += "</tr>";
    });
    h += "</tbody></table></div>";
    h += '<p class="gi-key">Each cell is one foal&rsquo;s pair of copies and the horse it makes.'
      + " The pair is unordered, so the table is symmetric.</p>";
    return h;
  }

  /**
   * Too many alleles for a square. A player does not need the whole matrix -
   * they need to know what they can end up with - so this lists the distinct
   * outcomes with an example genotype for each.
   */
  function drawList(gene) {
    var seen = {};
    var out = [];
    gene.cells.forEach(function (c) {
      var name = phenotype(c);
      if (seen[name]) { seen[name].push(c.tokens); return; }
      seen[name] = [c.tokens];
      out.push({ name: name, wild: c.wild, description: c.description, cell: c });
    });
    var h = '<ul class="outcomes">';
    out.forEach(function (o) {
      var combos = seen[o.name];
      h += "<li><span class=\"what\">" + esc(o.name) + "</span>"
        + '<span class="looks">' + esc(o.description || "")
        + ' <span class="gi-combos">' + esc(combos.slice(0, 3).join(", "))
        + (combos.length > 3 ? " and " + (combos.length - 3) + " more" : "")
        + "</span></span></li>";
    });
    h += "</ul>";
    h += '<p class="gi-key">' + gene.alleles.length
      + " versions of this gene exist, so the full grid is on the coding tab."
      + " These are the horses they add up to.</p>";
    return h;
  }

  function start() {
    var hosts = document.querySelectorAll(".gene-inheritance[data-gene]");
    Array.prototype.forEach.call(hosts, mount);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})(window.HG);
