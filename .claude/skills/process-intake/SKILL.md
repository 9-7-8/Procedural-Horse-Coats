---
name: process-intake
description: Take ONE unit of work out of intake/, implement it, and end the session. Use when the owner says "process intake", "pick something from intake", or "do the next intake item". Ranks every unit in the folder (small + high impact first, then anything doable without the owner, then the big ones) and builds exactly one.
---

# Process intake

One run = **one unit** out of `intake/`, built, written up, retired, pushed, and the
session ended. Never a second unit in the same conversation - the owner clears context
and re-runs this for the next one (`intake/START HERE.txt`, "One bullet, one context
window").

Intake is for **new features only**. Anything in the folder that is not a new feature
is not a unit for this routine; leave it where it is.

This file is the *routine*. It does not hold the queue: what is in intake/ today is
read fresh every run, and nothing below names a current item on purpose.

---
## 0. Get the inbox current
1. `git pull --ff-only origin main` - `intake/To do.txt` is edited on github.com and the
   local copy is routinely stale. Cloud routines also push to main.
2. `git status --short` - untracked files in `intake/` are the owner queuing material.
   Commit around them; never flag, add or delete them.
3. Read `intake/START HERE.txt` and `intake/Implementation advice.txt` (once each), then
   run `node wiki/tools/check-roadmap-execution.mjs`.

---
## 1. List the units
A **unit** is the smallest thing that ships alone - not a file:
- each bullet in `To do.txt` (its first line is permanent; never delete it, never count it);
- each **piece / slice / tier** a treatment names (`Head parts` has three slices,
  `Horse exits` two pieces). A treatment that names no split is one unit.

Not units - leave them alone:
- `START HERE.txt`, `Implementation advice.txt`, `How to make an intake document.txt`;
- `potential-assets/` - art packs, not work. **Use anything in them freely** for the
  unit you are building - copy, crop, recolour, transform - with no need to ask.
  Document each use the way earlier uses are documented: the rules at the bottom
  of `potential-assets/README.txt` (the pack's `USAGE.txt`, `THIRD_PARTY_NOTICES.md`
  on a pack's first use, never the whole pack in git or a release);
- `*-draft/` and `tools/` folders - supporting material for a treatment. They retire
  with the treatment that uses them, never on their own;
- a treatment that says it is **PLAN ONLY** with stages still unchecked is a unit
  only at its *first* stage.

Read a treatment's header, its "What already exists" section, and any "Size and
order" / "Order" section - not the whole file - to list its units. Use an Explore
agent if more than a handful of treatments need sizing; this step must not eat the
context the build needs.

---
## 2. Throw out what is already done
For every unit you might pick: grep `wiki/session-log/` and `wiki/text/` for its
keywords, and look for its classes in the code. **Date it before deleting it**: get
the date the unit was written (`git log --date=short -S 'a phrase' -- intake/`) and
the date the matching work landed. A unit written *after* that work asks for more
than was built, so it is still open.

Already done -> delete its text from intake/ (step 7) and note it in the session
log. That deletion does **not** count as this run's unit; carry on and pick one.

---
## 3. Score each remaining unit
Three questions, answered from the treatment - write the answers down as a table
in your reply before picking, one row per unit:

| | Question | How to tell |
|---|---|---|
| **Size** | S, M or L? | **S** = one screen or class, one test class, few rows of the regenerate table. **M** = one session, a new gene or system slice, several bakes. **L** = the treatment says multi-session, or it is a *foundation* other units wait on (a new part kind, a shared base class, a new sheet region, a structure bake, an entity type). |
| **Impact** | What does a player get? | Highest first: **finishes** something that has shipped (a missing piece of a screen, item or system players already use) -> **unblocks** other units -> **new content**. A unit the owner flagged in her own words ("most important", "next") goes to the top of its tier. |
| **Owner** | Can it be built without her? | **No** if it has an `Open:` line that is a *design* call (not a mechanism the builder may pick), says "confirm with the owner", designs or redesigns a breed (breeds are designed with her unless a thorough spec exists), or touches `wiki/philosophy.html`. Art from `potential-assets/` never needs her. `UNVERIFIED` lines do **not** need her - they need reading the source. "Recommended" lines are the scoper's default and can be taken. |

Also note **depends on**: a unit that needs another unit's foundation (a part that
needs a sheet region or colour base class another treatment adds) cannot go first.

---
## 4. Pick, in this order
1. **Tier 1 - small and high impact.** Size S, impact finishes or unblocks. Owner calls
   are allowed here *only if* they fit in one question round (step 5).
2. **Tier 2 - anything you can do without her.** Any size S or M, no owner design
   calls, dependencies already shipped.
3. **Tier 3 - the big ones.** Size L, or anything with open design calls. Build its
   foundation slice, or only its first slice. If it is not routed onto a wiki
   Roadmap tab yet, routing it is part of the unit (rule 9).

Within a tier, break ties by: unblocks the most other units -> fewest regenerated
artefacts -> oldest in `git log -- intake/`.

Never pick: anything that needs the live server (`26.1.2-neoforge-server` is another
Claude's).

Say in **one line** what you picked, which tier, and why the runner-up lost. Then
carry on: no confirmation wait, unless step 5 asks something.

---
## 5. Ask once, up front, or not at all
If the pick has owner calls, ask **all of them in one `AskUserQuestion` round
before any code** - 2-4 concrete options each, recommended one first, the
treatment's "Recommended" line as the default. If she does not answer, or the
answers open new questions, drop back to the best Tier 2 unit and leave this one's
questions written into its treatment.

Anything decided *without* her while building goes in the session log under "Calls
made without the owner", and on `wiki/decisions.html` when it is a design call.

---
## 6. Build it
- **Before editing:** one sentence of hypothesis and one check that could disprove it
  (Implementation advice). Resolve every `UNVERIFIED` line the unit relies on from the
  26.1.2 sources or a gametest first. A treatment may cite an API or a past case that
  does not exist, so grep for each one before writing code against it.
- Read the owning wiki page in `wiki/text/`, then only the classes the treatment names.
- `common/` first, the NeoForge module thin, hard rules 1-10 as always. Rule 10 is the
  one a feature breaks without noticing: anything saved that the unit changes ships
  with its migration, and a test that loads the old shape. If it touches the
  spawn-egg screen or the designer, change both (rule 5).
- Tests by name only (`--tests '*XTest'`), never the full suite. Break the new test on
  purpose once, to prove it can fail.
- Run every row of CLAUDE.md's **regenerate** table the change touched.
- If the unit turns out bigger than its score, ship the part that stands alone, put the
  rest back in the treatment as its next piece, and say so. Do not widen the run.

---
## 7. Retire it from intake/
Per START HERE step 3: delete the shipped unit's **text**, with no "done" marker and no
strike-through.
- Last piece of a treatment -> delete the whole file, and any `*-draft/` folder or
  `Implementation advice.txt` section that only it used. Grep `intake/` for the folder
  name first; another treatment may still read it.
- Not the last piece -> delete that piece's section, and leave a one-line pointer to
  where it now lives on the wiki if the remaining pieces refer to it.
- A `To do.txt` bullet -> delete it, and its heading if the heading is now empty.

What shipped goes on the owning page's **Gameplay / Coding** tabs; what nobody has seen
in a game goes on its **Verification** tab; what is still planned goes on its **Roadmap**
tab (and a page is written if the subject has none).

---
## 8. End the session
Run CLAUDE.md's **"Ending a session"** routine in full: regenerate, build green
(`:neoforge-26.1.2:build`, `check-parity.mjs`, `check-designer-boots.mjs` if `common/`
or `web/` moved), code commit, then a separate docs commit, budget audit, verify clean,
kill every process you started.

**Release** - do what the treatment's `Release:` line says (the owner answered it while
scoping; see `How to make an intake document.txt`, Part 4 section M):
- *own version* -> after the docs commit, cut it: read `mod_version` in
  `gradle.properties` against `git tag`, bump the LAST number only, boot a server to
  `Done(` with no ERROR lines, tag, and publish a GitHub release with the jar from that
  clean tagged tree. Put the release note on `wiki/releases.html` too. If the line says
  "when the last piece ships" and this is not the last piece, do not cut one.
- *rides the next release* -> no release.
- *bigger bump* -> stop and ask her first. The middle number never moves without her word.
- no `Release:` line (an older treatment) -> no release; name it as a question in step 9.

The session log file is `wiki/session-log/d<date>-<slug>.txt`, shaped like the earlier
intake sessions (`d2026-10-01-antlers.txt`, `-breed-climate-rule.txt`, `-chaos-mob-loci.txt`):
- the owner's request, quoted;
- **Done**: the unit, the commit, what is green, and which treatment file or section was retired;
- **Calls made without the owner**;
- **Found on the way**: stale docs, wrong `UNVERIFIED` claims;
- **Tests run by name**, and what a full `:common:test` run would still check;
- **Where to look first** in game: the Verification tab anchors.

---
## 9. Stop
Report in a few lines:
1. what shipped, and the tier it came from;
2. what is newly waiting in `wiki/verification.html`;
3. **the next unit by this ranking**, its tier, and any owner question it will need, so
   she can answer it before the next run.

Then stop. Do not start that next unit.
