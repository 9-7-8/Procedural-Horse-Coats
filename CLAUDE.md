# Horse Genetics - NeoForge 26.1.2 Mod

Procedural horses. A Mendelian genotype of **allele objects** drives a
**generated coat texture** - genes restrict red/black pigment per pixel, the
survivors are looked up in a gradient and multiplied onto a white-horse
template. Every horse also carries a name, a pedigree, resolved-not-rolled body
stats, and an **epigenome** (a priority + seed on *each allele copy*, inherited
with the allele). There is a breed system, a gameplay layer of items and
carrots, a hay-bale portal to a horse dimension, and two browser tools.
Long-term aim is a 1.12.2 backport, which is why the logic is quarantined in a
game-free module.

---

## Keep this file small

**This file is the standing rules. It is not a log, a reference, or a record of
what was built.** It was 5 490 lines once; almost all of it was material that
had a wiki page of its own, and the cost was that the rules nobody may break sat
buried under 3 000 lines of session history.

**The budget is 300 lines.** Before adding anything here, apply the test:

> **Would I need this on *every* task, before I know what the task is?**

If no, it goes in the wiki and gets a pointer here at most. In particular:

| Do NOT put here | It belongs in |
|---|---|
| What a session built, measured, or decided | `wiki/session-log.html` |
| Anything with a date on it | `wiki/session-log.html` |
| How a gene, system or class works | its own wiki page (see the map) |
| A defect, a gap, or an unchecked assumption | `wiki/known-gaps.html` |
| Something to go look at in-game | `wiki/verification.html` |
| Work not started yet | `wiki/roadmap.html` |
| An API quirk of this SDK | `wiki/api-notes.html` |
| **A derived number** (gene counts, catalogue sizes, test counts) | the code that computes it |

**That last row is a repeat offender.** A number written into prose is stale the
next time the code moves, and three sessions running have found stale ones by
hand - see `wiki/known-gaps.html#gap-13`. Prefer naming the accessor
(`Genes.codeOrder().size()`) over quoting its value.

**When you add a line here, look for one to delete.** If this file grows past
300 lines, that is the signal to move a section out, not to let it ride.

---

## Where everything is written

The only two markdown files in the repo are `README.md` and this one.
**Everything else is the wiki**: `index.html` (the hub) + `wiki/*.html`.
Each page below is the **single source of truth** for its subject - update it in
the same change as the code, and never copy it back into here.

| Subject | Page |
|---|---|
| **Why the mod is built this way** - read before any design call | `wiki/philosophy.html` |
| The Mendelian model as implemented; the gene registry + priorities | `wiki/genetics-model.html` |
| **Each individual gene** - alleles, painter, frequency, outcomes | `wiki/gene-*.html` |
| The three-phase coat pipeline | `wiki/pipeline.html` |
| Eye colour, the two hooks, both heterochromias | `wiki/eye-colour.html` |
| Body space, `HorseSkinGeometry`, the vanilla model tables | `wiki/body-space.html` |
| Breeding, pedigree, horse records, stat inheritance | `wiki/breeding.html` |
| Breeds, herd spawning, the stat curve, cross/mixed labels | `wiki/breeds.html` |
| Speed / health / jump / size, disorders, the two lethal paths | `wiki/horse-body.html` |
| Gated healing, bond tiers, herds, the shared slow tick | `wiki/horse-care.html` |
| Every item and every recipe; shearing; the spawn egg | `wiki/items.html` |
| Breeding carrots, splices, the gene database, research papers | `wiki/carrots.html` |
| The cowboy, the horseman, transfer papers, the barn | `wiki/villagers.html` |
| Hay portals, the horse dimension, the pens | `wiki/horse-dimension.html` |
| The data-driven gene file format (masks, ops, header) | `wiki/gene-format.html` |
| The `effects` block - every verb, trigger, flag, and how to add one | `wiki/gene-effects.html` |
| The wider (mostly unbuilt) trait / effect architecture | `wiki/horse-traits.html` |
| Writing a gene; the class-by-class API reference | `wiki/modding.html`, `wiki/api-reference.html` |
| **Module split, packages, data flow, build setup, running the game** | `wiki/architecture.html` |
| **NeoForge 26.1.2 API quirks** - read before touching an unfamiliar system | `wiki/api-notes.html` |
| Living beside other mods | `wiki/compatibility.html` |
| **What is broken / unproven / half-built** | `wiki/known-gaps.html` |
| **The `runClient` checklist** - update after every play session | `wiki/verification.html` |
| **Unbuilt work**, priority-ordered; `#settled` records closed calls | `wiki/roadmap.html` |
| **What each session built, and why** | `wiki/session-log.html` |

`README.md` is **user-facing only** - what the mod does, how to play it,
install, licence. No status, no architecture, no API notes.

Two rules about the backlog page, both learned the hard way:

- **Link the roadmap by anchor, never by `§` number** (`#defects`,
  `#health-genes`, `#settled`). It was renumbered once and will be again.
- **Only unbuilt work lives there.** When something ships, *delete* it and write
  it up on its own page. "Marked shipped" is not "moved", and only moving it
  keeps one source of truth.

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
   mirroring the screen's `variantPair` / `enforceSexLinkage` / `applyGenome` /
   `randomizeGenes` under the same names; `js/gui.js` copies the screen's layout
   constants **by value**. A deliberate divergence goes in the comment on both
   files, not left to be discovered.
6. **No legacy or back-compat code.** Dev only, single tester, no saves worth
   keeping - when a format changes, change it and move on. No genotype-code
   padding, no attachment field fallbacks.
7. **The wiki has one nav.** A new page goes in the `SECTIONS` array in
   `wiki/nav.js` and nowhere else. (`wiki/gene-creator/` and
   `wiki/horse-designer/` are the exceptions - they are apps, not pages, and own
   their own chrome.)
8. **Flag genuinely unverified API usage in a comment**, the way the existing
   code does. More useful to the next session than silent confidence.
9. **When you resolve something recorded as a gap or as unverified, update that
   page in the same change.**

Two multi-file contracts worth knowing before you start:

- **A new mask or op** lands in four places: `SpecSchema.java`,
  `SpecPainter.java`, `wiki/gene-creator/js/{schema,spec-engine}.js`, and
  `wiki/gene-format.html`. Then re-bake fixtures and re-run parity.
- **A new `effects` verb** lands in four: a `record` on `GeneAbility`, one
  `register(new AbilityType(...))`, a `case` in the NeoForge translator
  (`server/GeneAbilityHandler`, or `GeneYieldHandler` for an interaction), and
  `wiki/gene-effects.html`. A verb with a client-render component also needs a
  `RenderLayer`. None of it touches `SpecSchema` or the parity check - effects
  do not paint.
- **The gene-carrot recipe** lands in four: `KnownGeneSpliceRecipe` (what it
  requires), `SpliceRecipeDisplay` (the canonical slot layout), `RarityItems`
  (the tier->item table, deliberately off `common/`), and
  `wiki/gene-carrot/gene-carrot.js`, which redraws both for the wiki. The wiki
  cannot read the last two - they are Minecraft-side - so a change there is
  silent on the pages until it is made here too.

---

## Architecture in one screen

Three Gradle modules, split deliberately. Full detail:
`wiki/architecture.html`.

- **`common/`** - pure Java, the whole genetics / coat / trait / breed model.
  This is the part that survives a version port unchanged. Subpackages:
  `genetics/`, `genetics/spec/`, `coat/`, `coat/pattern/`, `coat/skin/`,
  `trait/`, `breed/`, `horse/`, `name/`.
- **`neoforge-26.1.2/`** - everything Minecraft-specific, by concern: `client/`,
  `data/`, `network/`, `menu/`, `server/`, `block/`, `item/`. Its job is to
  **translate** - build and read `common` types and shuttle them in and out of
  Minecraft's systems. The logic stays in `common/`.
- **`web/`** - `common/` compiled to WebAssembly by TeaVM, for the wiki's horse
  designer. Three classes and no logic. Not shipped in the mod.

When adding a feature, put as much as possible in `common/` and keep the
NeoForge module thin. That is what makes a future `forge-1.12.2/` module cheap.

---

## Build & test

```bash
./gradlew :common:test               # pure-Java logic, no Minecraft - fastest loop
./gradlew :neoforge-26.1.2:build     # full compile + jar
./gradlew :neoforge-26.1.2:runClient # launch the game with the mod
./gradlew :neoforge-26.1.2:runServer # headless; does not auto-stop, kill it after `Done`

./gradlew :common:bakeSpecFixtures             # what the real Java spec engine produces...
node wiki/gene-creator/tools/check-parity.mjs  # ...and does the creator's JS agree?
./gradlew :web:bakeDesignerAssets              # recompile common/ to wasm for the designer
```

Requires JDK 25 (auto-provisioned). Crash reports land in
`neoforge-26.1.2/run/crash-reports/` - read the newest.

**The owner's last play session is on disk - read it rather than asking.**
`neoforge-26.1.2/run/logs/latest.log`, and `debug.log` beside it for more.
A bug report of the shape "it still doesn't work" is usually answerable from
there directly, and `server/DebugAnnounce` writes this mod's own diagnostics to
both the log and chat so they survive the chat scrolling away.

**Regenerate what you invalidate.** Every artefact below is derived, checked in,
and fails *silently* when stale:

| If you touched | Re-run | Commit |
|---|---|---|
| any gene, or a coat deliberately moved | `:common:test`, then delete and regenerate the golden file | `common/src/test/resources/coat-golden.txt` |
| **anything in `common/` or `web/`** | `:web:bakeDesignerAssets` | `wiki/horse-designer/wasm/web.wasm` |
| `spec/`, `SpecSchema`, `AbilityType`, `HorseSkinGeometry`, the noise classes | `:common:bakeSpecFixtures` **then** `check-parity.mjs` | `wiki/gene-creator/fixtures/expected.json` |
| the coat PNGs or the name tables | `:common:bakeCreatorAssets` + `:web:bakeDesignerAssets` | the regenerated assets |
| either `tools/barn/*.source.nbt` | `python neoforge-26.1.2/tools/barn/bake-barn.py` | `data/horsegenetics/structure/cowboy_barn.nbt` |
| `tools/barn/cowboy_house.source.nbt` | `python neoforge-26.1.2/tools/barn/bake-house.py` | `data/horsegenetics/structure/cowboy_house.nbt` |

**Re-baking is part of the parity check, not a chore beside it.**
`expected.json` is a checked-in snapshot of the Java, so a stale one makes
`check-parity.mjs` green **by definition** - which is exactly how a UV swap hid
for a day while the creator drew every horse with its spine and belly patches
exchanged. Likewise, a `git status` showing `common/` changed and
`wiki/horse-designer/wasm/` unchanged means the designer is running yesterday's
mod.

---

## Status

Deliberately a set of pointers - the numbers live in the code and the detail
lives on a page.

- **`common/`** compiles and its JUnit suite is green. **`neoforge-26.1.2/`**
  compiles and assembles. **`runServer`** boots clean. **Creator parity** is
  green. Confirm all four rather than trusting this line.
- **What has actually been seen in-game is a small fraction of what is built.**
  `wiki/verification.html` is the authority on which is which, and is the first
  thing to read before claiming something works.
- Recent work is summarised newest-first in `wiki/session-log.html`; what is
  known to be wrong is in `wiki/known-gaps.html`.
- **Machine caveat (this dev laptop):** hybrid graphics. `java.exe` / `javaw.exe`
  are pinned to the NVIDIA GPU and the FML early splash is disabled through a
  deliberately git-tracked `run/config/fml.toml`, or the JVM hard-crashes in the
  AMD GL driver. Details: `wiki/architecture.html#running`.

---

## Ending a session

The routine for **"end the session"** / "wrap up" / "we're done for today". It
is a fixed order - the docs pass comes *after* the code is pushed, so it reviews
where the session actually landed rather than narrating it mid-change.

1. **Regenerate what the session invalidated** (the table under Build & test),
   then **fix the twin** if `CustomHorseSpawnScreen` or the designer changed
   (hard rule 5).
2. **Build green**: `:common:test`, `:neoforge-26.1.2:build`,
   `check-parity.mjs`. Don't push red - and if you must, say so in the commit
   message and put it at the top of `wiki/verification.html`.
3. **Commit and push the code.** Read `git status --short` first; the repo has a
   `.gitignore`, so if build or run output appears then a pattern is wrong - fix
   the pattern, don't `git add` around it. Work directly on **`main`**; don't
   branch. One descriptive commit: what changed and *why*, not a file list.
4. **Then update the docs to what is true now** - not "what I changed today".
   Walk the map above and honour the source-of-truth rule. At minimum:
   `wiki/session-log.html` (a new dated entry at the top),
   `wiki/verification.html` (delete what the owner confirmed in-game, add what
   is newly unplayed *and where to look*), `wiki/known-gaps.html` (delete what
   closed, add what was discovered), plus any gene or system page the session
   moved, and `wiki/nav.js` if a page was added.
5. **Audit this file and put it back under budget.** Not "did I add anything" -
   that is too easy to answer *no* to without looking. Actually run it:

   ```bash
   wc -l CLAUDE.md                            # must be <= 300
   git diff HEAD~1 -- CLAUDE.md               # what did this session add?
   grep -nE '20[0-9]{2}-[0-9]{2}-[0-9]{2}|[0-9]{3,}' CLAUDE.md   # dates + derived numbers
   ```

   Then take **every line the session added here**, run it through the test and
   the routing table at the top of this file, and move what fails. The two
   greps above catch the usual offenders: a date means it is session-log
   material, and a long number is almost always a derived value that should be
   an accessor name instead.

   Over 300 lines is not a nudge, it is the signal to **move a whole section
   out** - the way the first audit moved four. Trimming words to squeeze under
   the line misses the point: the budget exists so the hard rules stay findable,
   and a file that is 299 lines of history has already failed.
6. **Commit and push the doc update as its own commit.** Step 4 always leaves
   the tree dirty; a session must not end with unpushed doc changes.
7. **Verify clean**: `git status --short` empty and `git log origin/main..HEAD`
   empty.
8. **Stop** with a short summary - what shipped, what is newly waiting in
   `wiki/verification.html`, and the one thing the next session should pick up
   first. Then stop: no new work, no "while I'm here" refactors.

---

## License

CC BY-NC 4.0 (see `LICENSE`). Forks and derivatives are welcome without asking
but must credit the original repo and link back, and no portion may appear in a
paid derivative with no free version available. Donations on an otherwise-free
derivative are fine. Check a third-party licence is compatible before vendoring.
