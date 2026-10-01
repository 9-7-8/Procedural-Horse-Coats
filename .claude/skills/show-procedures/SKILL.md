---
name: show-procedures
description: List every procedure this project has - its slash command, the words that start it, where it runs and what it leaves - plus the candidates not written yet. Use when the owner says "show procedures", "show local procedures", "what procedures do we have", or "list the routines".
---

Read `procedures/INDEX.txt` and show the owner:

1. The table of procedures: command, where it runs, the words that start it, and what it
   leaves. Keep it as a table.
2. "How they fit together", as it is written there.
3. The candidate routines that are not written yet, one line each.

Then check the folder against the index: list `procedures/*.txt` and `.claude/skills/*/`.
Report any procedure file missing from the index, any index row with no file, and any
procedure with no slash-command wrapper (FORMAT.txt and look-it-up.txt need none).

Do not run any procedure. This command only lists them.
