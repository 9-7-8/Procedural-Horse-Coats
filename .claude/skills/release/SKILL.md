---
name: release
description: Ship a release: fetch first, bump only the last number, boot a server, audit the whole tag range for the note, build from a clean tagged tree, publish the jar on GitHub and the note on the wiki. Use when the owner says "ship a release", "cut a release", "new version", "tag it".
---

Read `procedures/release.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/release` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
