---
name: fix-bug
description: Take ONE open GitHub bug issue, fix it, close it on evidence, and end the session. Use when the owner says "fix a bug", "fix the next bug", "fix #N", or "work the issues". Bugs live only in GitHub issues - never in intake/ and never on a Verification tab.
---

# Fix a bug

One run = **one issue**, fixed, proven, written up, pushed, and the session ended. Never a
second issue in the same conversation. The owner clears context and re-runs this for the
next one: each bug's classes, pages and tests are unrelated to the last one's, and a fresh
context that reads only what it needs is cheaper than one carrying them all.

**Where bugs live.** Every bug is a GitHub issue (CLAUDE.md hard rule 9). Not `intake/`,
which is for new features only, and not a Verification tab, which is for checks on
things nobody has seen yet. The labels:
- `bug` - on every defect;
- `found-in-yard` - a test-yard run found it;
- `needs-owner` - it waits on her answer, and every routine skips it until she removes the label.

**Where this sits beside "run tests".** The run-tests routine finds bugs in the yard, files
every one, and fixes the small ones in passing. This routine takes one issue on
deliberately, from anyone: a tester, the yard, or the owner. The two share the same labels
and the same small/large split (step 4).

---
## 0. Sync
1. `git pull --ff-only origin main`. Cloud routines push to main too.
2. `gh issue list --state open --label bug --search "-label:needs-owner" --limit 200`.
   Testers do not always label, so also look at the unlabelled open issues and label any
   that are bugs.
3. Run `node wiki/tools/check-roadmap-execution.mjs` before editing anything.

---
## 1. Pick one
If the owner named an issue, take that one, even if it is `needs-owner`: naming it is her
answer. Read her latest comment on it first.

Otherwise rank, highest first:
1. **Damage**: a crash, a lost horse or item, or a save that no longer loads (hard rule 10).
2. **Wrong behaviour players hit on a released jar**: testers' reports, the live server's
   logs. Real players are on those jars, and they cannot rebuild from source.
3. **Wrong behaviour found only in the yard** (`found-in-yard`).
4. **Wrong but only cosmetic** - last.

Within a level, prefer an issue whose cause is already written in it, then the oldest.
Read the **whole thread**, every comment, before deciding anything. Say in one line which
issue you took and why.

---
## 2. Is it already fixed?
Many reports describe something a later commit already changed. Before writing any code:
1. Grep `wiki/session-log/` and `wiki/text/` for a keyword from the issue, and search the
   commit log: `git log --date=short --pretty='%h %ad %s' -S 'someIdentifier' -- path/to/Owner.java`.
2. **Date the report against the fix.** This is the step that decides it, and skipping
   it closes a live bug as stale. A session log saying "fixed" is not proof:
   - testers and the owner's server run a **released jar**, so check which release carries
     the fix (`git tag --contains <commit>`) against the version they reported;
   - a report written *after* the fix shipped, especially one saying "still", means the
     fix did not work. Read the code. (The barn-name box was built, fixed once for the
     wrong cause, and invisible to every player for seventeen days.)
3. **Fixed and released before the report** -> comment with the commit and the release
   that carries it, ask the reporter to retest on that version, and leave the issue open
   until they answer. **Fixed but not released yet** -> comment with the commit and that
   it rides the next release. That counts as this run's outcome only if nothing else
   is left to do on the issue; otherwise carry on to step 3.

---
## 3. Find the owning code
In this order, stopping as soon as you have it:
1. The issue itself: a `file:line`, a log line, or a stack trace.
2. **The symptom map**, `wiki/roadmap-execution.html#symptom-map` (read it in
   `wiki/text/roadmap-execution.txt`). It names the classes behind the common failure
   shapes: horse-info opening, the menu gesture, bond and stalling, natural cover,
   filters, and more. Read that row before searching the repository at all.
3. The owning wiki page, from CLAUDE.md's map, in `wiki/text/`.
4. The owner's last play session: `neoforge-26.1.2/run/logs/latest.log` and
   `debug.log`. A rolled `*.log.gz` holds the part before midnight. `server/DebugAnnounce`
   writes this mod's own diagnostics there.
5. An Explore agent, for anything still open-ended.

**Finding things without burning context:**
- Read `wiki/text/`, never `wiki/*.html`. Start at `wiki/text/index.txt`.
- Never grep or read `wiki/search-index.js`.
- Grep narrowly.
- Read a long routing page once, by the section you need.
- Batch independent reads into one parallel round.

Never touch the live server (`26.1.2-neoforge-server` is another Claude's); read logs
the owner or a report hands you.

---
## 4. Small or large
- **Small - fix it now.** The cause is clear from the evidence and the fix is obvious and
  local. It involves no design call, nothing near `wiki/philosophy.html`, nothing settled on
  `wiki/decisions.html`, and no balance change.
- **Large - report it, don't fix it.** Any of these makes a bug large: the cause is
  unclear, there is more than one reasonable fix, or it raises a design, balance or
  philosophy question.
  1. Comment once on the issue: what you found (evidence, `file:line`) and 2-4 fix
     options with your recommendation first.
  2. Add `needs-owner`.
  3. If no code has been written yet, go back to step 1 and take the next issue. The
     write-up is not this run's fix.

A save-format change is *not* automatically large any more: under hard rule 10 it is
fine as long as it ships with its migration and a test that loads the old shape. Ask
only if no migration can be written.

---
## 5. Fix it
- **Reproduce first.** Before editing, write one sentence of hypothesis and one check
  that could disprove it. Make the check a failing test:
  - a unit test in `common/` if the rule lives there (most should);
  - otherwise a gametest;
  - otherwise a yard pen that logs its own PASS/FAIL.
  If it can only be reproduced by hand, write the exact steps into the issue.
- Edit the **smallest owning slice**, then run the check straight away.
- **How a fix is shaped here:**
  - The rule goes in `common/`, returning a typed result rather than chat text. The
    NeoForge side turns that result into chat, config, persistence and navigation.
  - Config that decides gameplay is server-authoritative.
  - For AI and pathing bugs, check reachability and path-failure states before changing
    movement rules, collision or fence logic.
  - Keep a horse's history (pedigree, log) separate from its current ownership. A sold
    horse stays in its family tree.
  - Extend the existing path. Never add a second, screen-only or handler-only copy of
    a rule.
  - If the spawn-egg screen or the designer is involved, change both (rule 5).
  - Flag any API you have not verified (rule 8).
- **Prove the test can fail.** Break the fix on purpose once and watch the test go red.
- **Tests by name only** (`--tests '*XTest'`), never the full suite.
- If the fix is gametest-sensitive, run HEAD without your diff (`git stash`) before
  blaming the diff for a red result.
- Run every row of CLAUDE.md's **regenerate** table the change touched.
- If a fix turns out larger than step 4 judged, stop and treat it as large, rather than
  widening the run.

---
## 6. Close it on evidence
- Commit as `Fix #N: <what was wrong>` - say what was wrong and why, not which files
  changed.
- Do not use `Fixes #N` unless the proof is already in hand. Pushing to main
  auto-closes the issue.
- **A test proves it, and no runtime question remains** -> close the issue with a comment
  naming the commit and the test.
- **Only the game can prove it** -> leave it open, and comment: "fixed in `<commit>`,
  waiting on `<pen name>` / an in-game check: `<what to look for>`." The run-tests
  routine closes it on the yard PASS. A fix that only her eyes can confirm stays open
  for her.
- If the fix changes behaviour a wiki page describes, update that page's Gameplay or
  Coding tab in the same change. The issue holds the history; the page holds what is
  true now.
- Everything you post on GitHub ends with the attribution footer.

---
## 7. End the session
Run CLAUDE.md's **"Ending a session"** routine in full: regenerate, build green, code
commit, docs commit, budget audit, verify clean, and kill every process you started.

Write the session log as `wiki/session-log/d<date>-<slug>.txt` (plus its index entry), with:
- the issue (number, title, reporter);
- the cause, and the fix commit;
- the proof: which test or pen, and whether the issue is closed or waiting;
- anything found on the way, filed as new issues, with their numbers.

**Release:** none unless the owner asks. When the fix is to a crash or data loss on a
released jar, *offer* one in the report - testers are on that jar. A release, when she
says yes, bumps the last number only.

---
## 8. Stop
Report in a few lines:
1. the issue, and whether it is closed or waiting (and on what);
2. the next issue by step 1's ranking;
3. any `needs-owner` issue you labelled this run, with its question in one line.

Then stop. Do not start that next issue.
