// Draw an item page's own sprites at the top of it.
//
//   <div class="item-art" data-items="horse_whistle,golden_whistle"></div>
//
// The sprites and their captions are baked into assets/item-art/ by
// `wiki/tools/bake-item-art.mjs`, which also refuses to finish while any shipped
// sprite is on no page at all.
//
// Built here rather than written into the HTML so that a renamed item, or a
// redrawn sprite, needs one bake and no edit to twenty pages - the same bargain
// `block-preview.js` makes next door.
(function () {
  "use strict";

  function build(el) {
    var ids = (el.getAttribute("data-items") || "").split(",")
      .map(function (s) { return s.trim(); }).filter(Boolean);
    if (!ids.length) return;
    var names = window.HG_ITEM_NAMES || {};

    // An optional line above the row. Needed where two items share a display
    // name and the pictures would otherwise repeat themselves with no
    // explanation - an occupied stasis chamber is called the same thing in game
    // as an empty one.
    var note = el.getAttribute("data-note");
    if (note) {
      var h = document.createElement("p");
      h.className = "item-art-note";
      h.textContent = note;
      el.appendChild(h);
    }

    // Sixteen-square art blown up eight times has to stay crisp; the CSS does
    // that with image-rendering, and the width is set here so a sprite that is
    // 32 or 64 square lands at the same on-screen size as one that is 16.
    var list = document.createElement("ul");
    list.className = "item-art-row";
    ids.forEach(function (id) {
      var li = document.createElement("li");
      var img = document.createElement("img");
      img.src = "assets/item-art/" + id + ".png";
      img.alt = names[id] || id;
      img.loading = "lazy";
      var cap = document.createElement("span");
      cap.textContent = names[id] || id;
      li.appendChild(img);
      li.appendChild(cap);
      list.appendChild(li);
    });
    el.appendChild(list);
  }

  function start() {
    var nodes = document.querySelectorAll(".item-art");
    Array.prototype.forEach.call(nodes, build);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
}());
