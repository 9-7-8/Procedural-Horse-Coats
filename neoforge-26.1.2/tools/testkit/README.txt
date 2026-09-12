The test world, and the drop-in files the kit cannot make
=========================================================

HOW TO START TESTING
--------------------
  ./gradlew :neoforge-26.1.2:runClient

Then press "Spawn Test Horse World" on the title screen. That makes a fresh
creative world with cheats on, hands you batch 1 of the test kit, and tells you
in chat where the nearest plains village and dark forest are.

The world is throwaway - it is deleted when the client shuts down - so there is
nothing to clean up and nothing to keep. Press the button again for a new one;
it will be the SAME world, because the seed is fixed (below).

  /testkit            list the batches, and the two commands that make it night
  /testkit <n>        swap your hotbar for batch n, with a legend in chat
  /testkit village    the plains village teleport again
  /testkit forest     the dark forest teleport again

Batch 1 is always the next thing that needs a person. Batches are deleted as
they are worked through and the rest move up, so if batch 1 looks familiar,
nobody has re-aimed it - see DebugTestWorldHandler.BATCHES.


THE SEED IS FIXED, AND WHY
--------------------------
Every test world generates from DebugTitleScreenButton.TEST_WORLD_SEED, and it
spawns you in PLAINS.

It used to be a random seed, which meant every test world was a different
world: one session opened in a ravine, the next spent ten minutes swimming, and
"this coat looks wrong" and "I am standing in a dark forest at dusk" are hard to
tell apart from a screenshot. A coat is read against the ground it stands on,
and plains is the ground every gene page's picture was judged against.

The claim checks itself rather than being believed. On login the mod says which
biome you ACTUALLY landed in - green if it is plains, red with a teleport to the
nearest plains if worldgen has moved under the seed. A dedicated server prints
the same reading at startup, which is how the seed was chosen in the first
place: boot runServer on a candidate seed and read the line back.

  [Debug] world seed <n> - spawn BlockPos{...} is minecraft:plains (PLAINS ...)

If you change the seed, re-verify it the same way. The whole value of a fixed
seed is that a screenshot from one session and one from the next are the same
place.


THE DROP-IN FILES
-----------------
Four of the things wiki/verification.html asks you to check are not items -
they are files in phc/ that must exist before the game starts, and the kit
cannot write them. They lived only in neoforge-26.1.2/run/phc/, which is
.gitignore'd, so they existed on one machine and nowhere else.

They are NOT installed by default, and the dev run is deliberately stock: the
breed drop-ins move the Friesian to dark forest only and switch the Morgan off,
which is exactly the sort of thing that makes an unrelated test confusing. Put
them in only when you are testing them.

  cp -r neoforge-26.1.2/tools/testkit/breeds/*.json  neoforge-26.1.2/run/phc/breeds/
  cp neoforge-26.1.2/tools/testkit/genes/aurora.json neoforge-26.1.2/run/phc/genes/
  cp neoforge-26.1.2/tools/testkit/breed-spawning.toml neoforge-26.1.2/run/phc/

For a real install the same files go in .minecraft/phc/. Delete them to put the
run back to stock - none of them is meant to stay.

What each one is for, and what is still unchecked, is on the wiki:
wiki/verification.html (the test-kit note at the top).


TWO THINGS THAT WILL WASTE AN HOUR IF NOBODY SAYS THEM
------------------------------------------------------
* Do not rebuild while the client is running. A `:neoforge-26.1.2:build` under
  a live client crashes it with ClassNotFoundException on the next class it
  loads, and the crash report blames whatever it happened to be doing.

* runServer and runClient share run/, so a server cannot boot while the client
  holds the world: a DirectoryLock IOException means your game is open, not
  that something is broken.
