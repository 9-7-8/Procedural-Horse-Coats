potential-assets/ - third-party art we may want later

NOT PART OF THE MOD. intake/ is outside every Gradle module, so nothing in this
folder is packaged into the jar and nothing here ships in a release. These packs
are kept here only so they are on hand if we decide to use something from them
elsewhere in the game.

Three packs, different rights
====================================
pixel-art-textures/      Texture tiles by FlakDeau. CC0 per the store page: free
                         to use and redistribute. COMMITTED to git (about 32 MB).
                         Read the discrepancy note in its SOURCE-AND-LICENSE.txt.
raven-fantasy-hd-free/   Icons by Clockwork Raven Studios. Use in a free mod is
                         allowed, redistribution of the pack is not. The IMAGES
                         ARE NOT IN GIT: the owner places them on the host machine
                         by hand; only the notes, the licence PDF and MANIFEST.txt
                         are committed. .gitignore keeps the images out.

pixelated-patterns/      32x32 and 64x64 patterns by NormalMap_Games - NOT YET ON DISK, licence
                         unknown. Pick up on the host machine (see its notes). Possibly the best
                         fit for the mesh greyscale bases, at the mod's own pixel scale.

The rule for using anything from here
=====================================
1. Copy ONLY the specific files you need out of a pack into the module that
   uses them.
2. Add each use to that pack's USAGE.txt in the same change - file, where it
   went, whether modified. Empty USAGE.txt means nothing from that pack is in
   the game.
3. Add the pack to THIRD_PARTY_NOTICES.md (which ships in the jar) the first
   time anything from it is used, with the credit line in its notes. For the
   Raven icons the notice also has to say users may not reuse or redistribute
   them, and the CC BY-NC grant must be carved back for those files.
4. Never publish either pack whole (release zip, wiki download, editor asset
   bundle). Only the individual files used.
5. A session that finds the Raven (or pixelated-patterns) images missing has not hit a bug. See
   raven-fantasy-hd-free/MANIFEST.txt.
