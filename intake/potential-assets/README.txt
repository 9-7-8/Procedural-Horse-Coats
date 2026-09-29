potential-assets/ - third-party art we may want later

NOT PART OF THE MOD. intake/ is outside every Gradle module, so nothing in this
folder is packaged into the jar and nothing here ships in a release. These packs
are kept here only so they are on hand if we decide to use something from them
elsewhere in the game.

Redistribution note. Keeping a pack in this repository is itself a form of
redistribution, separate from shipping it in the jar. Each folder's own
SOURCE-AND-LICENSE.txt says what that pack's licence permits. Read it before
pushing, and before copying anything out of here into a module.

The rule for using anything from here
=====================================
1. Copy ONLY the specific files you need out of the pack into the module that
   uses them (a modified or unmodified copy is then a "content file in your
   project", which is the use both licences below contemplate).
2. Add each use to that pack's USAGE.txt in the same change - file name, where
   it went in the mod, whether modified. Empty USAGE.txt means nothing from that
   pack is in the game.
3. Add the pack to THIRD_PARTY_NOTICES.md (which ships in the jar) the first
   time anything from it is used, with whatever credit its licence asks for.
   Credit is voluntary for pixel-art-textures and unconfirmed for raven-fantasy-hd
   (see its file), so the default is to credit anyway.
4. Never publish the pack itself (a release zip, a wiki download, a gene-creator
   asset bundle). Only the individual files used.

Packs
=====
raven-fantasy-hd-free/   Raven Fantasy HD Ultimate - free sampler (icons), Clockwork Raven Studios
pixel-art-textures/      "PNG - Pixel Art Textures" (block-style texture tiles), author not named in the pack
