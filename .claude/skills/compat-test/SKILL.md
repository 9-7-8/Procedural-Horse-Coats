---
name: compat-test
description: Prove the modded-material layer against the synthetic mod - build the fake jar (and update it first if the scan rules moved), boot a dev server three times for the generate / current / wipe fingerprint states, read the compat lines and the error grep, remove the jar, restore run/, write it up. Use when the owner says "compat test", "test the compat layer", "test modded materials", "boot with the fake mod".
---

Read `procedures/compat-test.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/compat-test` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
