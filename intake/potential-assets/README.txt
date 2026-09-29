potential-assets/ - third-party art we may want later

NOT PART OF THE MOD. intake/ is outside every Gradle module, so nothing in this
folder is packaged into the jar and nothing here ships in a release. These packs
are kept here only so they are on hand if we decide to use something from them
elsewhere in the game.

Five packs, different rights
====================================
pixel-art-textures/      Texture tiles by FlakDeau. CC0 per the store page: free
                         to use and redistribute. COMMITTED to git (about 32 MB).
                         Read the discrepancy note in its SOURCE-AND-LICENSE.txt.
raven-fantasy-hd-free/   Icons by Clockwork Raven Studios. Use in a free mod is
                         allowed, redistribution of the pack is not. The IMAGES
                         ARE NOT IN GIT: the owner places them on the host machine
                         by hand; only the notes, the licence PDF and MANIFEST.txt
                         are committed. .gitignore keeps the images out.
                         ON DISK since 2026-09-29, and MANIFEST.txt's checks pass.
pixelated-patterns/      32x32 and 64x64 patterns by NormalMap_Games. The 64x64
                         half is ON DISK (125 files); the 32x32 zip is not. Licence
                         STILL UNKNOWN - the store page states none and neither zip
                         carries one, so the images are NOT IN GIT and nothing may be
                         copied out until the owner asks the author. Possibly the best
                         fit for the mesh greyscale bases, at the mod's own pixel scale.
internet-pattern-book/   331 Geocities background tiles archived by HYPERTELEX,
                         ON DISK since 2026-09-29. NOT IN GIT. The murkiest of the
                         four: the uploader says outright that the patterns are not
                         theirs, so nobody has licensed them to us and no tile has a
                         known author. The owner decided on 2026-09-29 to keep them
                         anyway - her reasoning is recorded in its SOURCE-AND-LICENSE.txt
                         and is not to be reopened. Web tiles, not game art: read its
                         notes on sizes and on the 116 files whose extension lies
                         before writing anything that reads the folder.

piiixl-textures3/        Seamless 16x16 tiles by piiixl. NOT ON DISK - queued
                         2026-09-29, downloads not supplied yet. The only pack here
                         with a real written licence, and it is the one that may be
                         unusable: its No Extraction clause forbids shipping the
                         assets where others can pull them out as standalone files,
                         which is what a PNG in a mod jar is. Ask the author before
                         anything from it enters the repo. 16x16 and seamless is
                         otherwise the best fit of the five.

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
   them, and the CC BY-NC grant must be carved back for those files. For the
   Internet Pattern Book the notice says where the tile came from and that its
   original author is unknown.
4. Never publish any pack whole (release zip, wiki download, editor asset
   bundle). Only the individual files used.
5. A session that finds a pack's images missing has not hit a bug: only
   pixel-art-textures is in git, and piiixl-textures3 has never been downloaded.
   See raven-fantasy-hd-free/MANIFEST.txt, and each pack's SOURCE-AND-LICENSE.txt
   for the link to fetch it again.
6. Two of the five are blocked on a question only the owner can ask the author -
   pixelated-patterns (no licence stated at all) and piiixl-textures3 (a licence
   that appears to forbid what a mod jar does). Neither is a judgement call for a
   session to make on its own.
