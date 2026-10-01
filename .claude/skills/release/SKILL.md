---
name: release
description: Create a new release: decide with the owner whether one is due (Claude offers, she decides), run every code test, bump only the last number, audit the whole tag range for the note, publish the jar on GitHub and the note on the wiki. Use when the owner says "ship a release", "cut a release", "new version", "tag it", "is it worth a release".
---

Read `procedures/release.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/release` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins.
