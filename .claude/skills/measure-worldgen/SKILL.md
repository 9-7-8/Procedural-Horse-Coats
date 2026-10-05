---
name: measure-worldgen
description: Measure how often worldgen does something, headlessly - Structure.generate over fixed seeds via the census gametests (HomesteadCensus, StableSiteCensus, or a new census), a control run on the old code and a run on the change with the same seeds, the two [census] lines compared and written up on the structure's page. Use when the owner says "measure worldgen", "how often does it generate", "did the worldgen change help", "census the villages", "census the stables".
---

Read `procedures/measure-worldgen.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/measure-worldgen` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
