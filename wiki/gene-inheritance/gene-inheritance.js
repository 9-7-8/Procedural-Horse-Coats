// The Punnett square on a gene page's gameplay tab: pick the two parents, see
// what the foal can be.
//
// USING IT. One line, on the gameplay panel:
//
//     <div class="gene-inheritance" data-gene="horsegenetics.flaxen"></div>
//
// It asks the mod (DesignerApi.geneInheritanceJson, through the same wasm the
// gene preview and the horse designer run) for the alleles and the phenotype of
// every pair, and draws the square from that. Nothing about any particular gene
// is written here, so a gene that gains an allele gains a parent option and no
// page is edited - and a wiki that would otherwise be quoting a combination
// table it has to keep in step by hand cannot fall out of step at all.
//
// WHAT CHANGED, AND WHY. This used to be an allele x allele grid: every allele
// down the side, every allele across the top, one cell per genotype. That is
// the whole locus, which sounds better than it reads - it answers "what
// genotypes exist" when the question a player actually has is "I own these two
// horses, what can they throw". It also fell apart above three alleles, where a
// four- or five-allele locus is sixteen or twenty-five cells of mostly
// irrelevant genotypes, and dropped to a plain list instead.
//
// So the parents are now CHOSEN - one across the top, one down the side - and
// the square is the four gametes those two can pair, which is four cells for
// any locus however many alleles it has. Each distinct phenotype gets its own
// colour, and anything that does not present - a wild type, a silent carrier -
// is grey, so "how often does this actually show" is answerable by looking
// rather than by reading four genotype strings.
//
// BOTH PARENTS START HETEROZYGOUS, which is the interesting cross and the one
// worth defaulting to: two homozygotes have nothing to say, and a carrier x
// carrier is the whole reason a punnett square exists.
//
// Needs the wiki over http, like every other wasm-backed widget here.
window.HG = window.HG || {};
(function (HG) {
  "use strict";

  // Phenotype colours, in order of first appearance. Distinct in hue rather
  // than pretty: the square's whole job is "are these two cells the same
  // outcome", so the palette is chosen for separation, and it never has to
  // stretch far because a single locus rarely shows more than a handful of
  // outcomes at once. Anything past the end wraps.
  var HUES = [200, 28, 288, 140, 340, 55, 250, 170];

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

  // ------------------------------------------------------------------
  // The model
  // ------------------------------------------------------------------

  /** Every genotype at this locus, keyed both ways round - the pair is unordered. */
  function index(gene) {
    var by = {};
    gene.cells.forEach(function (c) {
      by[c.a + "/" + c.b] = c;
      by[c.b + "/" + c.a] = c;
    });
    return by;
  }

  /**
   * The cross both parents start on: heterozygous, and heterozygous for
   * something worth looking at.
   *
   * The variant allele beside the wild type, when the gene has a wild type -
   * which is almost all of them, and is the carrier x carrier cross. A locus
   * with no wild-type allele (the accretion field, the LUT) gets its first two
   * alleles instead, which is the same idea with nothing to prefer.
   */
  function defaultPair(gene, by) {
    var al = gene.alleles;
    var wild = null;
    var i;
    for (i = 0; i < al.length; i++) {
      if (al[i].isDefault) { wild = al[i]; break; }
    }
    for (i = 0; i < al.length; i++) {
      if (wild && al[i].token !== wild.token && by[al[i].token + "/" + wild.token]) {
        return [al[i].token, wild.token];
      }
    }
    for (i = 1; i < al.length; i++) {
      if (by[al[0].token + "/" + al[i].token]) {
        return [al[0].token, al[i].token];
      }
    }
    return [al[0].token, al[0].token];
  }

  /** Every genotype a parent may be set to, in the mod's own order. */
  function parentOptions(gene) {
    return gene.cells.map(function (c) {
      return { a: c.a, b: c.b, tokens: c.tokens, label: c.tokens + " - " + phenotype(c) };
    });
  }

  /**
   * The four foals, as a 2x2 of gametes. Each parent hands down one of its two
   * copies with equal chance, so every cell is a quarter - which is why no
   * cell carries a number of its own and the tally underneath does the counting.
   */
  function square(by, sire, dam) {
    var rows = [];
    for (var i = 0; i < 2; i++) {
      var row = [];
      for (var j = 0; j < 2; j++) {
        row.push(by[sire[i] + "/" + dam[j]] || null);
      }
      rows.push(row);
    }
    return rows;
  }

  /**
   * Phenotype -> { colour index, how many of the four cells }. Wild types and
   * anything else that does not present share the grey and take no colour, so
   * the colours only ever mean "these are different visible outcomes".
   */
  function outcomes(rows) {
    var order = [];
    var seen = {};
    var next = 0;
    rows.forEach(function (row) {
      row.forEach(function (c) {
        if (!c) return;
        var name = phenotype(c);
        if (!seen[name]) {
          seen[name] = { name: name, wild: !!c.wild, hue: c.wild ? -1 : next++, count: 0 };
          order.push(seen[name]);
        }
        seen[name].count++;
      });
    });
    return { list: order, by: seen };
  }

  // ------------------------------------------------------------------
  // The view
  // ------------------------------------------------------------------

  function tint(entry) {
    if (!entry || entry.wild || entry.hue < 0) { return ""; }
    var h = HUES[entry.hue % HUES.length];
    return ' style="--gi-hue:' + h + '"';
  }

  function options(opts, chosen) {
    return opts.map(function (o) {
      return '<option value="' + esc(o.a + "/" + o.b) + '"'
        + (o.a + "/" + o.b === chosen ? " selected" : "")
        + ">" + esc(o.label) + "</option>";
    }).join("");
  }

  function cellHtml(c, tally) {
    if (!c) {
      return '<td class="gi-none" aria-label="no such horse">&mdash;</td>';
    }
    var name = phenotype(c);
    var entry = tally.by[name];
    return '<td class="gi-cell' + (c.wild ? " gi-wild" : " gi-shows") + '"' + tint(entry) + ">"
      + '<code class="gi-geno">' + esc(c.tokens) + "</code>"
      + '<span class="gi-pheno">' + esc(name)
      + (c.varies ? ' <em title="varies from horse to horse">varies</em>' : "")
      + "</span></td>";
  }

  function draw(host, gene, by, opts, sireKey, damKey) {
    var sire = sireKey.split("/");
    var dam = damKey.split("/");
    var rows = square(by, sire, dam);
    var tally = outcomes(rows);

    var h = '<div class="gi-parents">'
      + '<label class="gi-parent"><span>Parent across the top</span>'
      + '<select class="gi-pick" data-side="dam">' + options(opts, damKey) + "</select></label>"
      + '<label class="gi-parent"><span>Parent down the side</span>'
      + '<select class="gi-pick" data-side="sire">' + options(opts, sireKey) + "</select></label>"
      + "</div>";

    h += '<div class="table-wrap"><table class="gi-grid"><thead><tr><th></th>';
    dam.forEach(function (t) {
      h += '<th scope="col"><code>' + esc(t) + "</code></th>";
    });
    h += "</tr></thead><tbody>";
    rows.forEach(function (row, i) {
      h += '<tr><th scope="row"><code>' + esc(sire[i]) + "</code></th>";
      row.forEach(function (c) { h += cellHtml(c, tally); });
      h += "</tr>";
    });
    h += "</tbody></table></div>";

    h += '<ul class="gi-tally">';
    tally.list.forEach(function (o) {
      h += '<li class="' + (o.wild ? "gi-wild" : "gi-shows") + '"' + tint(o) + ">"
        + '<span class="gi-swatch" aria-hidden="true"></span>'
        + '<span class="gi-tally-name">' + esc(o.name) + "</span>"
        + '<span class="gi-tally-odds">' + o.count + " in 4 &middot; "
        + Math.round(o.count * 25) + "%</span></li>";
    });
    h += "</ul>";
    h += '<p class="gi-key">Each cell is one copy from each parent, and every cell is equally'
      + ' likely. A different colour is a different horse; grey is a foal this gene does not'
      + ' show on.</p>';

    host.innerHTML = h;

    Array.prototype.forEach.call(host.querySelectorAll(".gi-pick"), function (sel) {
      sel.addEventListener("change", function () {
        if (sel.getAttribute("data-side") === "dam") {
          draw(host, gene, by, opts, sireKey, sel.value);
        } else {
          draw(host, gene, by, opts, sel.value, damKey);
        }
      });
    });
  }

  function mount(host) {
    var key = host.getAttribute("data-gene");
    host.classList.add("gene-inheritance");
    host.innerHTML = '<div class="gi-boot">Asking the mod for this gene&hellip;</div>';

    boot().then(function (api) {
      var gene = JSON.parse(api.geneInheritanceJson(key));
      if (gene.missing) {
        host.innerHTML = '<div class="gi-boot gi-failed">No gene is registered as <code>'
          + esc(key) + "</code>.</div>";
        return;
      }
      var by = index(gene);
      var start = defaultPair(gene, by);
      var startKey = start[0] + "/" + start[1];
      draw(host, gene, by, parentOptions(gene), startKey, startKey);
    }, function (err) {
      host.innerHTML = '<div class="gi-boot gi-failed">The punnett square could not load: '
        + esc(err.message) + "."
        + (location.protocol === "file:"
          ? " This page is open from a file:// path, and asking the mod is a fetch -"
          + " serve the repo with any static server, or read it on the published wiki."
          : "")
        + "</div>";
    });
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
