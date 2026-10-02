---
name: release
description: Create a new release: releases are the owner's call - a session only OFFERS one once a substantial batch has piled up (roughly ten-plus player-facing fixes or a substantial feature; never a handful of fixes), and cuts it on her yes or when she asks. Every release bumps the third number; the second number is a compatibility indicator, moved only for a substantial feature that breaks saves, confirmed with the owner. Runs every code test, audits the whole tag range for the note, publishes the jar on GitHub and the note on the wiki. Use when the owner says "ship a release", "cut a release", "new version", "tag it", "is it worth a release".
---

Read `procedures/release.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/release` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins.
