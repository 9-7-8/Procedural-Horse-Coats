---
name: end-session
description: End the working session the fixed way: regenerate, build green, code commit, docs commit, CLAUDE.md budget audit, verify clean, kill processes, summarise. Use when the owner says "end the session", "wrap up", "we're done for today".
---

Read `procedures/end-session.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/end-session` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
