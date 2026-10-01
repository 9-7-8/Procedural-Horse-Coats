---
name: test-tiers
description: Choose the cheapest tests that can prove a change - tier 0 compile and node checks, tier 1 one test class, tier 2 gametests and browser checks, tier 3 the full suite only for a release or on request - and the release set that runs every code test. Use when the owner says "which tests should I run", "run all the tests", "run the full suite", "test everything".
---

Read `procedures/test-tiers.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/test-tiers` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins.
