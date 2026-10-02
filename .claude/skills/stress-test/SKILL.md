---
name: stress-test
description: Measure what horses cost per server tick - a dedicated server spawns horses in stages and logs ms/tick per stage with a JFR profile, read by the mod's own entry points - and measure fixes before and after. Use when the owner says "stress test", "how much do horses cost", "profile the horses", "measure ms per tick".
---

Read `procedures/stress-test.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/stress-test` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins.
