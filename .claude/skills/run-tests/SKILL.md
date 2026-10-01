---
name: run-tests
description: The self-piloted test-yard loop: read the last run, close what passed, build pens, launch into the yard, file every bug as an issue, fix the small ones, report. Use when the owner says "run tests", "run the yard", "test run".
---

Read `procedures/run-tests.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/run-tests` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
