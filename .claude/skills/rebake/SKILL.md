---
name: rebake
description: Rebake every derived, checked-in artefact a change invalidated, by walking CLAUDE.md's regenerate table in the right order, reading real exit codes, and committing only what git diff says changed. Use when the owner says "rebake", "run the bakes", "regenerate", "is anything stale".
---

Read `procedures/rebake.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/rebake` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins.
