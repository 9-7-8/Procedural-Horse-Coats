---
name: write-session-log
description: Write this session's own log: a NEW file wiki/session-log/d<date>-<HHMM>-<slug>.txt that collides with no other, with every section (asked, done with proof, decided, found, procedures changed, not run, release, pick up first), and its entry at the top of wiki/session-log.html. end-session runs it. Use when the owner says "write the session log", "log this session", "write up the session".
---

Read `procedures/write-session-log.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/write-session-log` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
