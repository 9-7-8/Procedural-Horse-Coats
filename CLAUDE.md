# Horse Genetics - NeoForge 26.1.2 Mod
Procedural horses. A Mendelian genotype of **allele objects** drives a
**generated coat texture** - genes restrict red/black pigment per pixel, the
survivors are looked up in a gradient and multiplied onto a white-horse
template. Every horse also carries a name, a pedigree, resolved-not-rolled body
stats, and an **epigenome** (literal named numbers on *each allele copy*,
inherited with the allele and slightly drifted each breeding). There is a breed
system, a gameplay layer of items and carrots, a hay-bale portal to a horse
dimension, and two browser tools.
Long-term aim is a 1.12.2 backport, which is why the logic is quarantined in a
game-free module.

---
## Keep this file small
**This file is the standing rules. It is not a log, a reference, or a record of
what was built.** It was thousands of lines once, almost all of it material with
a wiki page of its own, and the cost was that the rules nobody may break sat
buried under session history.
**The budget is 300 lines.** Before adding anything here, apply the test:
> **Would I need this on *every* task, before I know what the task is?**

If no, it goes in the wiki and gets a pointer here at most. In particular:

| Do NOT put here | It belongs in |
|---|---|
| What a session built, measured, or decided | `wiki/session-log/` |
| Anything with a date on it | `wiki/session-log/` |
| How a gene, system or class works | its own wiki page (see the map) |
| A defect, gap, unchecked assumption, or open follow-up | the owning page's **Verification tab** |
| Something to go look at in-game | a **Verification tab on that thing's own page** |
| Work not started yet | a **Roadmap tab on that thing's own page** |
| A design call - made, or still open | `wiki/decisions.html` |
| An API quirk of this SDK | `wiki/api-notes.html` |
| **A derived number** (gene counts, catalogue sizes, test counts) | the code that computes it |

**That last row is a repeat offender** (`wiki/known-gaps.html#gap-13`) - name the
accessor (`Genes.codeOrder().size()`), never quote its value.
**When you add a line here, look for one to delete.** If this file grows past
300 lines, that is the signal to move a section out, not to let it ride.

---
## Where everything is written
The only markdown files in the repo are `README.md`, this one, and
`THIRD_PARTY_NOTICES.md` - which is a licence artefact that ships inside the
jar, not documentation, and must stay plain text. Everything else is the wiki.
**Everything else is the wiki**: `index.html` (the hub) + `wiki/*.html`.
Each page below is the **single source of truth** for its subject - update it in
the same change as the code, and never copy it back into here.

| Subject | Page |
|---|---|
| **Why the mod is built this way** - read before any design call | `wiki/philosophy.html` |
| The Mendelian model as implemented; the gene registry + priorities | `wiki/genetics-model.html` |
| **Each individual gene** - alleles, painter, frequency, outcomes | `wiki/gene-*.html` |
| The three-phase coat pipeline | `wiki/pipeline.html` |
| Eye colour - the thirteen loci, and the genes that request one | `wiki/eye-colour.html` |
| Body space, `HorseSkinGeometry`, the vanilla model tables | `wiki/body-space.html` |
| Breeding, pedigree, horse records, stat inheritance | `wiki/breeding.html` |
| Breeds: spawning, regions, stat curve, cross labels; then the lore and the entries | `wiki/breeds.html`, `wiki/breed-book.html` |
| Speed / health / jump / size, disorders, the two lethal paths | `wiki/horse-body.html` |
| Gated healing, bond tiers, herds, the shared slow tick | `wiki/horse-care.html` |
| The item roster; then one page per item | `wiki/items.html`, `wiki/item-*.html` |
| Breeding carrots, splices, the gene database, research papers | `wiki/carrots.html` |
| The cowboy, the horseman, transfer papers, the barn | `wiki/villagers.html` |
| Hay portals; the public realm, then the F6 debug corridor and its pens | `wiki/horse-realm.html`, `wiki/horse-dimension.html` |
| **Generated stables - and who made each building** | `wiki/stables.html` |
| The breed file format - a breed is JSON, not Java | `wiki/breed-format.html` |
| The wider (mostly unbuilt) trait / effect architecture | `wiki/horse-traits.html` |
| **Making a gene - ALL of it.** Shapes, the file format, masks and ops, the `effects` block, the Java path, the prompt, what to re-bake | `wiki/making-a-gene.html` |
| The class-by-class API reference | `wiki/api-reference.html` |
| **Module split, packages, data flow, build setup, running the game** | `wiki/architecture.html` |
| **NeoForge 26.1.2 API quirks** - read before touching an unfamiliar system | `wiki/api-notes.html` |
| **Technique** - how to find things out here; traps between two pages | `wiki/coding-notes.html` |
| Living beside other mods | `wiki/compatibility.html` |
| **What is open, unproven, or needs a decision** | the owning page's Verification tab; generated index at `wiki/verification.html` |
| **What is waiting to be looked at** - generated; every page's Verification tab, one sentence each | `wiki/verification.html` |
| **What is planned and not built** - generated; every page's Roadmap tab, one sentence each | `wiki/roadmap.html` |
| **Design calls** - settled (do not reopen) and still open | `wiki/decisions.html` |
| **What each session built, and why** | `wiki/session-log/` |

`README.md` is **user-facing only** - what the mod does, how to play it,
install, licence. No status, no architecture, no API notes.
**Never link a plan at `roadmap.html#anchor`** - it has none any more; link the
Roadmap tab that holds it (`undead-horses.html#skeleton-gene`). When something
ships, *delete* it from that tab and write it up on the same page's Gameplay and
Coding tabs - "marked shipped" is not "moved".
**Look things up in `wiki/text/`, not `wiki/*.html`.** Same prose, markup
stripped, one file per page or tab, plus `wiki/text/index.txt` (heading to
`file#anchor`), baked by `bake-agent-text.mjs`. For a dated record, choose one
file under `wiki/session-log/` from its index; those files are not baked.

---
## Hard rules
1. **`common/` imports nothing from Minecraft or NeoForge.** Not even DFU
   codecs. This is what makes the backport cheap; if you want to import
   something Minecraft-related there, stop and put it in the NeoForge module.
2. **`common/` uses no Java 9+ APIs.** It has three targets - NeoForge, TeaVM
   (the browser), and one day Java 8. `System.getLogger` and
   `Long.parseUnsignedLong` were both removed for this; see `CommonLog` and
   `Epigenome.parseUnsignedHex`. TeaVM compiles only the *reachable* graph, so
   adding an `@JSExport` can surface a missing JDK method that was always there
   - build after adding one.
3. **Never port a gene to JavaScript.** The horse designer compiles `common/` to
   WebAssembly (`:web`), so the browser runs the real thing. The hand-written
   ports under `wiki/gene-creator/js/` are legacy the creator still depends on -
   do not extend them, and do not add a second one. If something in the browser
   seems to need genetics, it needs an `@JSExport` on `web/DesignerApi`.
4. **Never pre-generate a subset of coats and render only those.** The premise
   of the mod is a functionally infinite space; a tool that ships 900 baked
   horses quietly asserts otherwise. (Owner's call.)
5. **`wiki/horse-designer/` and `CustomHorseSpawnScreen` are one screen in two
   places - change both.** The designer's model is `web/HorseEditor.java`,
   mirroring `applyGenome` / `stamp` / `randomizable` / `topUpMagical` /
   `addRandom` / `enforceSexLinkage` by name; identical rules live once in
   `common/EditorRules`. `js/gui.js` copies the layout constants **by value**. A
   deliberate divergence goes in the comment on both files.
6. **No legacy or back-compat code.** Dev only, single tester, no saves worth
   keeping - when a format changes, change it and move on. No genotype-code
   padding, no attachment field fallbacks.
   **Never regenerate `genes/index.json` from the folder** - an absence in it is
   a killswitch, and a rebuild turns a parked gene back on. Gap 162.
7. **The wiki has one page list.** A new page goes in the `SECTIONS` array in
   `wiki/pages.js` - which the sidebar *and* the landing page both read - and
   nowhere else, with the `views` it belongs in. (`wiki/gene-creator/`,
   `wiki/horse-designer/` and `wiki/breed-designer/` are the exceptions - they
   are apps, not pages, and own their own chrome.) **No gene page goes in by
   hand.** The `BEGIN`/`END generated` spans in `wiki/pages.js` *and* in
   `index.html` belong to `:common:bakeGeneWikiPages`, which lists every
   registered gene that has a page, grouped by `GeneFamily` - the same grouping
   the two gene editors offer. A gene page is registered by existing. Its
   sidebar label is the page's own `<h1>` and its `views` are the tab panels it
   carries, so write the page and re-run the task. A hand-written landing card
   is harvested and put back verbatim.
8. **Flag genuinely unverified API usage in a comment**, the way the existing
   code does. More useful to the next session than silent confidence.
9. **Every open check is written on the page for the thing, never on `wiki/verification.html`.**
   (Owner, and it is absolute.) Defects, runtime checks, design calls, documentation
   contradictions, compatibility checks and other unresolved follow-ups all live on the owning
   page's **Verification tab** - `data-tab="verification"` per `wiki/tabs.js` - opening with
   **one sentence in a `note verify-summary` box**. `wiki/verification.html` exists to do
   **nothing except collect the pages that have such a tab**: it is generated by
   `bake-verification-index.mjs` and **nothing is ever hand-written onto it**. The old
   `wiki/known-gaps.html` page is only a compatibility index for old anchors, not a source of
   truth and not a second backlog.
   **When an open check is resolved it MOVES: off the Verification tab, onto the Coding or
   Gameplay tab** of the same page, as what became true, when, and on what evidence. No
   "Closed" entries and no "Confirmed" notes remain behind; a Verification tab only ever
   shrinks, and a page with nothing left to check loses the tab and leaves the index by itself.
   **`wiki/roadmap.html` is the same machinery for the other list**: a plan goes on a
   **Roadmap tab** (`data-tab="roadmap"`, one sentence in a `note roadmap-summary` box),
   `bake-roadmap-index.mjs` collects them, nothing is hand-written there either, and **a
   plan whose subject has no page means writing the page** - Roadmap tab only, growing the
   others as it ships. `wiki/horse-gear.html` is the worked example. (Owner.)
**Adding a mask, an op, an `effects` verb or a gene-carrot recipe touches four
or five files each and drifts silently if you miss one** - the four lists are
on `wiki/making-a-gene.html#contracts`, read it before you start, not after.
**A per-wood block family's asset shape is written twice** - double gates and
jumps both: `tools/bake-*.mjs` for vanilla's woods at author time,
`compat/Generated*` for other mods' at run time. Change one, change the other -
a drift shows as a modded block that is a purple cube, and logs nothing.
`check-block-models.mjs` catches only the author-time half.
- **A gene or item page is three tabs** - `<section class="tab-panel"
  data-tab="gameplay|coding|science">` inside `article.doc`, per
  `wiki/tabs.js`. Gameplay is the default, is written for a player who does not
  want the model, and is where health goes as a *sentence*. Design rationale
  goes on **Science**, not Coding. A page with no panels is left alone.

---
## Architecture in one screen
Three Gradle modules - `common/` (pure Java: the whole genetics / coat / trait
/ breed model, the part that survives a version port), `neoforge-26.1.2/`
(everything Minecraft-specific, whose job is to **translate**), and `web/`
(`common/` compiled to WebAssembly by TeaVM for the wiki, not shipped in the
mod). Packages, data flow and the build: `wiki/architecture.html`.
**When adding a feature, put as much as possible in `common/` and keep the
NeoForge module thin.** That is what makes a future `forge-1.12.2/` cheap.

---
## Build & test

```bash
./gradlew :common:test --tests '*XTest'  # one class, seconds - the working loop (see below)
./gradlew :neoforge-26.1.2:build     # full compile + jar
./gradlew :neoforge-26.1.2:runClient  # launch the game (runServer too - architecture.html#running)
./gradlew :common:bakeSpecFixtures             # what the real Java spec engine produces...
node wiki/gene-creator/tools/check-parity.mjs  # ...and does the creator's JS agree?
./gradlew :web:bakeDesignerAssets              # recompile common/ to wasm for the designer
node wiki/tools/check-links.mjs                # every href, #fragment, id and page footer
node neoforge-26.1.2/tools/check-recipes.mjs   # no two recipes claim the same inputs
node neoforge-26.1.2/tools/check-block-models.mjs # no blockstate points at a model nobody wrote
node wiki/tools/check-gene-tabs.mjs             # a new natural gene page brings a science tab
node wiki/tools/bake-verification-index.mjs && node wiki/tools/bake-roadmap-index.mjs # rebuild verification.html and roadmap.html
node neoforge-26.1.2/tools/check-progress-tasks.mjs # every checklist task is hooked, and is an advancement
```
Requires JDK 25 (auto-provisioned); crash reports land in `neoforge-26.1.2/run/crash-reports/`.
**Never run the full `:common:test` suite unless the owner asks** - it is ten
minutes and they will not wait through it; use `--tests` and say in the summary
what a full run would still need to check. (Owner's rule.)
**The owner's last play session is on disk - read it rather than asking.**
`neoforge-26.1.2/run/logs/latest.log`, and `debug.log` beside it for more.
A bug report of the shape "it still doesn't work" is usually answerable from
there directly; `server/DebugAnnounce` writes this mod's own diagnostics to
the log and chat so they survive the chat scrolling away.
**Regenerate what you invalidate.** Every artefact below is derived, checked in,
and fails *silently* when stale:

| If you touched | Re-run | Commit |
|---|---|---|
| any gene, or a coat deliberately moved | **two** goldens move, and both write their actual to `common/build/` on a mismatch - run the class, check *which rows* moved, copy back. `*CoatBakeGoldenTest` is every gene in seconds; `*CoatPipelineGoldenTest` is 39 whole horses and also seconds (only the *whole* suite is ten minutes) | `common/src/test/resources/coat-bake-golden.txt`, `coat-golden.txt` |
| **anything in `common/` or `web/`** | `:web:bakeDesignerAssets`, then `node wiki/tools/check-designer-boots.mjs` - TeaVM compiles only the *reachable* graph, so the task goes green and the page comes up blank | `wiki/horse-designer/wasm/web.wasm` |
| any breed, or `BreedSpecWriter` | `:common:bakeBreedFiles`, then `:common:bakeGeneCensus` - the census's breed column is read off the breed sheets, and nothing guards it the way `BreedFilesTest` guards the files. A breed whose size, or whose printed height on `breeds.html` / `breed-book.html`, moved also wants `node wiki/tools/check-breed-heights.mjs` - those are hand-typed, and it found three wrong. `BreedFilesTest` passing is not "the pool is right": that is `*BreedsTest` per breed, and `BreedBandsReachTest` for a band on a knob no pinned pair reads | `common/.../horsegenetics/breeds/`, `wiki/horse-designer/assets/breeds.json` **and** `wiki/gene-census.html` |
| `spec/`, `SpecSchema`, `AbilityType`, `HorseSkinGeometry`, the noise classes | `:common:bakeSpecFixtures` **then** `check-parity.mjs`; geometry also moves `:common:bakeGeneIcons` | `wiki/gene-creator/fixtures/expected.json`, `wiki/assets/gene-icons/` |
| the coat PNGs, the name tables, **or `example-genes/`** | `:common:bakeCreatorAssets` + `:web:bakeDesignerAssets` | the regenerated assets, incl. `wiki/gene-creator/js/examples.js` |
| a gene's layers or masks | `*DeadLayerTest` (seconds) - a layer that paints nothing fails it; fix the gene, then **delete its line** | `common/src/test/resources/dead-layers.txt` |
| **any file in `horsegenetics/genes/`, or any gene page's `<h1>`, tabs, or `Verified` block** | `:common:bakeGeneBundle`, `:common:bakeGeneIcons`, `:common:bakeGeneWikiPages`, `:common:bakeMarkingFacts`, `:common:bakeGeneCensus`, `:common:bakeUnverifiedGenes`, then `node wiki/tools/bake-gene-timeline.mjs` | `wiki/horse-designer/assets/genes.json`, `wiki/assets/gene-icons/`, the gene's `wiki/gene-*.html`, `wiki/timeline-of-genes.html`, `wiki/gene-census.html` (**every** gene's alleles, not only data-driven ones - a new gene is on it by existing, but only after the bake), `wiki/breed-designer/assets/marking-facts.json`, `common/.../horsegenetics/unverified-genes.txt` (the horse dimension's coat column - a stale one shows pens for confirmed genes and hides the rest) **and the generated spans of `wiki/pages.js` and `index.html`** |
| `GeneFamily`, or a gene's priority (it may change family) | `:common:bakeGeneWikiPages` | the same two spans, plus every gene page's eyebrow |
| **any wiki prose at all** | `node wiki/tools/build-search-index.mjs`, then `check-links.mjs` (hrefs + `#fragments`, and a footer whose See-also or blurb outlived its tabs; `--orphans`), then `node wiki/tools/bake-agent-text.mjs` | `wiki/search-index.js`, `wiki/text/` |
| a page's tab panels, or a section moved between tabs | `node wiki/tools/sync-page-views.mjs` | `wiki/pages.js` |
| **any Verification tab, or any Roadmap tab** - added, deleted, or its summary sentence reworded | `node wiki/tools/bake-verification-index.mjs` and `node wiki/tools/bake-roadmap-index.mjs` - each hard-fails on a tab with no summary box rather than write an index that omits a page | `wiki/verification.html`, `wiki/roadmap.html` |
| **`ProgressTask`** - a checklist task added, deleted or retitled | `node neoforge-26.1.2/tools/bake-advancements.mjs` (a new task needs an icon in its `ICON` map first), then `check-progress-tasks.mjs`. Every task is also an advancement, and the bake is the only thing that makes it one | `neoforge-26.1.2/.../data/horsegenetics/advancement/` |
| any AI goal added, removed or re-prioritised | `node wiki/tools/bake-behaviour-hierarchy.mjs` | `wiki/behaviour-hierarchy.html` |
| a wood, or any geometry, of the **double gates** | `node neoforge-26.1.2/tools/bake-double-gates.mjs` - then the recipe row below, since it writes recipes too. **It only writes**: removing a wood means deleting that wood's files and lang keys by hand | the regenerated `blockstates/`, `models/block/`, `items/`, `recipe/`, `loot_table/blocks/`, both `data/minecraft/tags/block/` files **and** the merged `lang/en_us.json` |
| a wood, or any geometry, of the **jumps** | `node neoforge-26.1.2/tools/bake-jumps.mjs` - then the recipe row below. It **merges** `mineable/axe`, which the gates are also in, so re-run `bake-double-gates.mjs` after it and check both families are still in that tag. Geometry is written twice more: `block/JumpBlock`'s VoxelShape, and `compat/GeneratedJumps` | the regenerated `blockstates/`, `models/block/`, `items/`, `recipe/`, `loot_table/blocks/`, `data/minecraft/tags/block/mineable/axe.json` **and** the merged `lang/en_us.json` |
| a cart recipe, model or lang key, **or `CartKind`** | `node neoforge-26.1.2/tools/bake-carts.mjs` - vanilla's twelve woods only. Modded woods are `compat/GeneratedCarts` at run time and **the two must agree**; a drift is a modded cart that will not craft, and logs nothing | the regenerated `models/item/`, `items/`, `recipe/` and the merged `lang/en_us.json` |
| any file in `data/horsegenetics/recipe/` | `node neoforge-26.1.2/tools/bake-recipe-reference.mjs` | `assets/horsegenetics/recipe_reference.json` |
| a **breed egg texture** in `assets/horsegenetics/textures/item/breed_egg/` | `node neoforge-26.1.2/tools/bake-breed-eggs.mjs` - it hard-fails on a filename that is not a breed id, since a case on a value no horse carries never draws | the regenerated `models/item/breed_egg/` **and** `items/breed_spawn_egg.json` |
| `tools/villagers/equestrian.source.png`, the one piece of villager art | `tools/villagers/bake-profession-hats.ps1` - it writes **five** files: a hat colour per equestrian and the cowboy's own uncoloured copy. The hat is **two** rectangles on the sheet, crown and brim; recolouring only the crown gives a villager a teal hat with a brown underside | the five PNGs under `assets/.../textures/entity/villager/` |
| either `tools/barn/*.source.nbt`, **or `bake-barn.py` itself** | `python neoforge-26.1.2/tools/barn/bake-barn.py` | `data/horsegenetics/structure/cowboy_barn.nbt` |
| any `tools/stables/*.source.nbt`, **or `bake-stables.py` itself** | `python neoforge-26.1.2/tools/stables/bake-stables.py` | the regenerated `data/horsegenetics/structure/*.nbt` |

**Re-baking is part of the parity check, not a chore beside it.**
`expected.json` is a checked-in snapshot of the Java, so a stale one makes
`check-parity.mjs` green **by definition** - which is exactly how a UV swap hid
for a day while the creator drew every horse with its spine and belly patches
exchanged. Likewise, a `git status` showing `common/` changed and
`wiki/horse-designer/wasm/` unchanged means the designer is running yesterday's
mod.

---
## Status
Pointers only - the numbers live in the code, the detail on a page.
- **`common/`** compiles, the suite is green, **`neoforge-26.1.2/`** assembles,
  **`runServer`** boots clean, **creator parity** green. Confirm, don't trust.
- **What has actually been seen in-game is a small fraction of what is built.**
  `wiki/verification.html` is the authority on which is which - read it first.

---
## Ending a session
The routine for **"end the session"** / "wrap up" / "we're done for today" - a
fixed order, since the docs pass comes *after* the code is pushed and reviews
where the session landed rather than narrating it mid-change.
1. **Regenerate what the session invalidated** (the table under Build & test),
   then **fix the twin** if `CustomHorseSpawnScreen` or the designer changed
   (hard rule 5).
2. **Build green**: `:neoforge-26.1.2:build`, `check-parity.mjs`. Don't push red -
   if you must, say so in the commit message and in `wiki/known-gaps.html`.
3. **Commit and push the code.** Read `git status --short` first; the repo has a
   `.gitignore`, so if build or run output appears then a pattern is wrong - fix
   the pattern, don't `git add` around it. Work directly on **`main`**; don't
   branch. One descriptive commit: what changed and *why*, not a file list.
4. **Then update the docs to what is true now** - not "what I changed today".
   Walk the map above and honour the source-of-truth rule. At minimum:
   a new dated file in `wiki/session-log/` and its index entry,
   the **Verification tab of each page the session built on** (what is newly
   unplayed *and where to look*; delete only what the owner confirmed in-game,
   then re-bake the index), `wiki/known-gaps.html` (delete what closed, add
   what was discovered), plus any gene or system page the session moved, and
   `wiki/nav.js` if a page was added.
5. **Audit this file and put it back under budget.** Not "did I add anything" -
   that is too easy to answer *no* to without looking. Actually run it:

   ```bash
   wc -l CLAUDE.md                            # must be <= 300
   git diff HEAD~1 -- CLAUDE.md               # what did this session add?
   grep -nE '20[0-9]{2}-[0-9]{2}-[0-9]{2}|[0-9]{3,}' CLAUDE.md   # dates + derived numbers
   ```   Then run **every line the session added here** through the test and the
   routing table at the top, and move what fails: a date means session-log
   material, a long number a derived value that should be an accessor name.
   Over 300 lines is the signal to **move a whole section out**, not to trim
   words to squeeze under it - 299 lines of history has already failed.
6. **Commit and push the doc update as its own commit.** Step 4 always leaves
   the tree dirty; a session must not end with unpushed doc changes.
7. **Verify clean**: `git status --short` empty and `git log origin/main..HEAD`
   empty.
8. **Kill every process this session started** - Gradle daemons and workers, any
   `runClient` / `runServer`, and every shell left running in the background.
   They outlive the session and sit on the owner's RAM for days. Kill the game
   and server processes *first*, then `./gradlew --stop`: a daemon still running
   a build ignores the stop. Leave the owner's own terminals alone, and if a
   client is up that you did not launch, ask before closing it.
9. **Stop** with a short summary - what shipped, what is newly waiting in
   `wiki/verification.html`, and the one thing the next session should pick up
   first. Then stop: no new work, no "while I'm here" refactors.

---
## License
CC BY-NC 4.0 (see `LICENSE`). Forks and derivatives are welcome without asking
but must credit the original repo and link back, and no portion may appear in a
paid derivative with no free version available. Donations on an otherwise-free
derivative are fine. Check a third-party licence is compatible before vendoring.
