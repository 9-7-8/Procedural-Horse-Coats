The drop-in files the test kit cannot make
==========================================

Spawn Test Horse World hands out a hotbar of items. Four of the things
wiki/verification.html asks you to check are not items - they are files in
phc/ that must exist before the game starts, and the kit cannot write them.

They lived only in neoforge-26.1.2/run/phc/, which is .gitignore'd, so they
existed on one machine and nowhere else. They are here so a fresh clone can
reproduce those checks.

To use them, copy into the dev run's drop-in folder and restart:

  cp -r neoforge-26.1.2/tools/testkit/breeds/*.json  neoforge-26.1.2/run/phc/breeds/
  cp neoforge-26.1.2/tools/testkit/genes/aurora.json neoforge-26.1.2/run/phc/genes/
  cp neoforge-26.1.2/tools/testkit/breed-spawning.toml neoforge-26.1.2/run/phc/

For a real install the same files go in .minecraft/phc/. Delete them to put
the run back to stock - none of them is meant to stay.

What each one is for, and what is still unchecked, is on the wiki:
wiki/verification.html (the test-kit note at the top).
