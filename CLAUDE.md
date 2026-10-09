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
| A defect | a **GitHub issue** (`bug`; `needs-owner` if it waits on the owner) |
| A gap, unchecked assumption, or open follow-up | the owning page's **Verification tab** |
| Something to go look at in-game | a **Verification tab on that thing's own page** |
| Work not started yet | a **Roadmap tab on that thing's own page** |
| A feature wanted, in the build queue | a **GitHub issue** (`feature` for something genuinely new, `enhancement` for an improvement to something already in the game, or `port` for a port; `idea` until scoped, then `scoped`) |
| A design call - made, or still open | `wiki/decisions.html` |
| An API quirk of this SDK | `wiki/api-notes.html` |
| **A derived number** (gene counts, catalogue sizes, test counts) | the code that computes it |

**That last row is a repeat offender** (`wiki/known-gaps.html#gap-13`) - name the
accessor (`Genes.codeOrder().size()`), never quote its value.
**When you add a line here, look for one to delete.** If this file grows past
300 lines, move a section out. The audit is `procedures/audit-claude-md.txt`.

---
## Where everything is written
The only markdown files in the repo are `README.md`, this one, and
`THIRD_PARTY_NOTICES.md` - which is a licence artefact that ships inside the
jar, not documentation, and must stay plain text - plus the slash-command
wrappers in `.claude/skills/`, which only point at `procedures/*.txt`.
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
**Look things up in `wiki/text/`, not `wiki/*.html`.** Same prose, markup
stripped, one file per page or tab, plus `wiki/text/index.txt` (heading to
`file#anchor`), baked by `bake-agent-text.mjs`. For a dated record, choose one
file under `wiki/session-log/` from its index; those files are not baked.

---
## Hard rules
1. **`common/` imports nothing from Minecraft or NeoForge.** Not even DFU
   codecs. This is what makes the backport cheap; if you want to import
   something Minecraft-related there, stop and put it in the NeoForge module.
2. **`common/`'s portability is from *Minecraft*, not from Java.** Two
   constraints: no third-party library, and no JDK API TeaVM lacks -
   `System.getLogger`, `Long.parseUnsignedLong` and empty `Map.of()` went for
   that, and `CommonPortabilityTest` guards them. Java 17 is fine; a Java 8
   backport pays a source downgrade, mostly the records.
3. **Never port a gene to JavaScript.** The horse designer compiles `common/` to
   WebAssembly (`:web`), so the browser runs the real thing. The hand-written
   ports under `wiki/gene-creator/js/` are legacy the creator still depends on -
   do not extend them, and do not add a second one. If something in the browser
   seems to need genetics, it needs an `@JSExport` on `web/DesignerApi` - and
   TeaVM compiles only the *reachable* graph, so build `:web` after adding one.
4. **Never pre-generate a subset of coats and render only those.** The premise
   of the mod is a functionally infinite space; a tool that ships 900 baked
   horses quietly asserts otherwise. (Owner's call.)
5. **`wiki/horse-designer/` and `CustomHorseSpawnScreen` are one screen in two
   places - change both.** The designer's model is `web/HorseEditor.java`,
   mirroring `applyGenome` / `stamp` / `randomizable` / `topUpMagical` /
   `addRandom` / `enforceSexLinkage` by name; identical rules live once in
   `common/EditorRules`. `js/gui.js` copies the layout constants **by value**. A
   deliberate divergence goes in the comment on both files.
6. **Never regenerate `genes/index.json` from the folder** - an absence in it is
   a killswitch, and a rebuild turns a parked gene back on. Gap 162.
7. **The wiki has one page list.** A new page goes in the `SECTIONS` array in
   `wiki/pages.js` and nowhere else (the gene creator, horse designer and breed
   designer are apps, not pages). **No gene page goes in by hand**: the `BEGIN`/`END
   generated` spans in `pages.js` and `index.html` belong to `:common:bakeGeneWikiPages`.
   How: `procedures/add-a-wiki-page.txt`, and `add-a-gene.txt` for genes.
8. **Flag genuinely unverified API usage in a comment**, the way the existing
   code does. More useful to the next session than silent confidence.
9. **Every open check is written on the page for the thing, never on `wiki/verification.html`.**
   (Owner, absolute.) **Bugs are GitHub issues instead** (`bug`, or `tuning`), and so are features (`feature`), improvements (`enhancement`) and ideas. A check goes on
   the page's **Verification tab**, a plan on its **Roadmap tab**, each opening with one
   summary sentence. `verification.html` and `roadmap.html` are generated from those tabs
   and never hand-written; `known-gaps.html` only redirects old anchors. A plan whose
   subject has no page means writing the page. **A resolved check or a shipped plan
   MOVES** to the same page's Coding or Gameplay tab: `procedures/close-a-check.txt`.
10. **Saves keep loading.** A world from any release since the baseline on `wiki/releases.html#saves`
   loads in every later one of its `0.X` series: a change to anything saved brings its migration.
   The version is `0.<compat>.<patch>` (`.<n>` more for an interim jar of a big themed release). Patch: every release, but releasing is the owner's call,
   offered only for a substantial batch, never a handful of fixes (`procedures/release.txt`). Compat:
   only a substantial feature that cannot keep saves loading; only it may break them.
11. **Ask the owner with the question tool** (AskUserQuestion: 2-4 options, recommendation
   first) - every question, unless it is genuinely too open-ended for options. (Owner.)
12. **Stop processes only with `tools/stop-dev-java.ps1`** - never Stop-Process, taskkill or
   kill, by PID or by pattern; a hook refuses them. The live server (`26.1.2-neoforge-server`,
   not ours, players on it) shares our `nogui` command line and changes PID on restart; a
   pattern kill took it down once. Anything the script leaves alone, ask the owner. (Owner.)
**Adding a mask, an op, an `effects` verb or a gene-carrot recipe touches four or five
files** - the lists are on `wiki/making-a-gene.html#contracts`; `procedures/add-a-gene.txt`
walks them. A gene or item page is three tabs (gameplay, coding, science): `add-a-wiki-page`.

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
./gradlew :common:test --tests '*XTest'  # one class, seconds - the working loop
./gradlew :neoforge-26.1.2:build          # full compile + jar
./gradlew :neoforge-26.1.2:runClient       # the game (launch it per procedures/launch-game.txt)
```
Requires JDK 25 (auto-provisioned); crash reports land in `neoforge-26.1.2/run/crash-reports/`.
**Never run the full `:common:test` suite unless the owner asks or a release is being
cut** - it is about twenty minutes. Use `--tests`, and say what a full run would still check.
Every other test and check, and when each is worth it: `procedures/test-tiers.txt`.
**The owner's last play session is on disk - read it rather than asking.**
`neoforge-26.1.2/run/logs/latest.log`, and `debug.log` beside it for more.
A bug report of the shape "it still doesn't work" is usually answerable from
there directly; `server/DebugAnnounce` writes this mod's own diagnostics to
the log and chat so they survive the chat scrolling away.
**Regenerate what you invalidate.** Dozens of artefacts are derived, checked in, and
fail *silently* when stale - a stale parity snapshot is green by definition. After any
change, walk `procedures/rebake.txt`: it holds the table of what to re-run for what (the
only copy), and the order inside each row.

---
## Status
- **What has actually been seen in-game is a small fraction of what is built.**
  `wiki/verification.html` is the authority on which is which - read it first.

---
## Procedures
Repeatable routines are plain text in `procedures/` - the list is `procedures/INDEX.txt`,
the template `procedures/FORMAT.txt` - and each is also a slash command. When the owner
says a procedure's words ("end the session", "run tests", "process intake", "fix a bug",
"report a bug", "ship a release", "launch the game", the rest in the index), read that
file and follow it; never work a routine from memory. **"End the session" is
`procedures/end-session.txt`**: regenerate, build, code commit, procedure review, a new
session log (`write-session-log`), docs commit, this file's audit, verify clean, kill, summarise.

---
## License
Code is MIT (`LICENSE`); original art is CC BY-SA 4.0 (`LICENSE-ART`,
`LICENSES/CC-BY-SA-4.0.txt`); third-party pieces keep their own terms. The
single source of truth is `wiki/licensing.html`. Check a third-party licence is
compatible before vendoring, and add it to `THIRD_PARTY_NOTICES.md` and that page.
