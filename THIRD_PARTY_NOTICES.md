# Third-Party Notices

## Item icons (vet's kit, freedom stick, golden carrot seeds)

Three item sprites in this mod are icons from **Raven Fantasy HD Ultimate
(free files)** by Caio Carlos of Clockwork Raven Studios
(https://clockworkraven.itch.io), used unmodified:

    assets/horsegenetics/textures/item/vet_kit.png
    assets/horsegenetics/textures/item/freedom_stick.png
    assets/horsegenetics/textures/item/golden_carrot_seeds.png

Credit, as the author's own licence suggests it:

    Icons by Caio Carlos of the Clockwork Raven - Additional Art Assets
    (clockworkraven.itch.io). Used under the Clockwork Raven User Asset
    License Agreement.

### These three files are NOT covered by this mod's licence

Horse Genetics is MIT (code) and CC BY-SA 4.0 (art), which let you reuse and
redistribute it. Those grants are **carved back for the three files listed above**, because it is not ours
to give. The Clockwork Raven User Asset License Agreement permits their use in a
free mod on the condition that the mod's users are told they may not take the
assets on, so:

  * You may **not** extract, reuse, redistribute or sell these three sprites,
    modified or unmodified, as assets.
  * You may **not** include them in an asset pack, library or collection.
  * They may **not** be used to train a machine-learning model.
  * A fork of this mod inherits those restrictions on these three files, and
    inherits nothing more than this mod itself holds.

Everything else in this repository is under the mod's own licences as normal
(`LICENSE` for code, `LICENSE-ART` for art). If
you want these icons for your own project, get them from the author's store page
above - they are free.

Only these three files come from that pack; the rest of the pack is not
distributed with this mod, is not in this repository, and never should be.

## Carts (horse-drawn wagons, plows, seed drills, reapers, supply and animal carts)

The cart system in this mod (Java package
`com.example.horsegenetics.neoforge.carts`, the seven `Cart*` classes in
`com.example.horsegenetics.neoforge.mixin`, and the assets and data they
register) is derived from **UsefulCarts**, used under the MIT License.

Lineage:

  1. **AstikorCarts** — the original mod, by MennoMax
                        (credited authors: MennoMax, paul101)
                        Forge, Minecraft 1.12.2 – 1.19.2
  2. **NiftyCarts** — Fabric port by jmb05 / jmb19905
                      https://github.com/jmb05/nifty-carts
                      https://modrinth.com/mod/niftycarts
  3. **UsefulCarts** — unofficial Minecraft 26.1.2 / 26.2 port of NiftyCarts
                       https://github.com/Andrewwwwwwwwwwwwwww/usefulcarts-mc26.1.2
                       (source commit 1b66a257a6f867092ab44f7269bd4ff6b850c198)
                       https://modrinth.com/mod/useful-carts
  4. **NeoForge 26.1.2 port** — prepared for Horse Genetics
                                (Wicked Dreamer Studios), then integrated here.

Copyright (c) 2019 MennoMax
Copyright (c) 2023 jmb19905

Licensed under the MIT License; full text in `LICENSES/UsefulCarts-MIT.txt`.

The UsefulCarts porter added no copyright line of their own, so crediting them
is courtesy rather than a legal requirement. It is done anyway.

Not in this lineage, and no code from either was used: **AstikorCarts Redux**
and **AstikorCarts Deluxe**. Do not credit them as sources, and do not copy
anything from them without adding their own notices.

### Assets

The item textures (`assets/horsegenetics/textures/item/*_wagon.png` and the
other 71), the three GUI textures, the wagon chest texture and the three `.ogg`
cart sounds come from the same MIT repository and are covered by this notice.

**Cart entity textures are not redistributed at all.** Each is assembled at
runtime from the player's own vanilla block textures by
`mixin/CartModelManagerMixin`, so no Mojang art ships for the carts. (Mojang art
does ship for the horse coats - see the next section.)

### Modifications from upstream UsefulCarts

  - Ported from Fabric (Loom, Fabric API, Forge Config API Port) to NeoForge
    26.1.2. Registration moved into NeoForge `RegisterEvent` handlers; network
    payload ids moved out of the `minecraft` namespace; the access widener
    replaced by an access transformer and an `@Invoker` mixin.
  - Fixed Y/Z velocity interpolation in `CartInterpolationHandler` (upstream
    used the X component for all three axes).
  - Namespace, package and entrypoints changed to integrate into Horse
    Genetics. The two `@Mod` classes became `HorseCarts.init` and an
    `@EventBusSubscriber` client class.
  - **The hand cart was not ported.** It is player-pulled and never involves a
    horse. Its entity, item, model, renderer, textures and recipes are absent.
  - **Advancements and their criterion triggers were not ported** (11
    advancements, 7 triggers, and `SimpleBlockPredicate`).
  - **The slow-speed toggle was not ported** — its keybind, its network
    payload, its config value, and the `ClientPacketListener` mixin that
    existed only to add a line to the mount overlay.
  - **Per-cart `pull_speed` config was removed.** How fast a horse pulls a cart
    is now a function of that horse's genetics — see
    `common/cart/CartDraft.java` and `wiki/carts.html`. This also removes
    upstream's wagon `pull_speed` bug, whose default (`0.0`) lay outside its own
    declared range (`-1.0` to `-0.2`) and which NeoForge corrected with a
    warning on every config load.
  - **Carts exist in every wood, not twelve.** Upstream keyed carts on vanilla's
    `WoodType` from a hard-coded table; they are now keyed on `CartWood`, built
    from vanilla's twelve plus one per wood any other loaded mod adds. The
    runtime texture assembler resolves each wood's sprites and falls back to
    planks where a modded log sprite is absent.
  - Items, models and recipes for modded woods are generated at runtime by
    `compat/GeneratedCarts`; vanilla's are baked by
    `neoforge-26.1.2/tools/bake-carts.mjs`.
  - Added: a per-horse lifetime haulage tally, shown on the horse information
    screen; a `horsegenetics:draft_power` entity attribute for animal-labour
    mods; and a second rider on horses with a high pulling score.
  - The wagon roof textures were being rebuilt once per wood (sixteen dyes,
    twelve times over). Moved out of the wood loop.
  - `wheel` renamed to `cart_wheel` to keep the host namespace unambiguous.

## Generated stables (three buildings)

The three buildings the mod generates as stables were made by other people and
downloaded from Planet Minecraft:

  * Horse Stable, by Seuchendoktor
  * Stables, by Alpha_One_Seventeen
  * TM U Stable, by Tyiarrah

Sources and sizes are on `wiki/stables.html`. They are **not** covered by this
mod's licences. Planet Minecraft submissions carry no uniform licence and the
terms of these three have not yet been checked one by one; until they are,
treat the buildings as not freely reusable and do not extract them.

## Horse coat sheets (Mojang)

The coat sheets below are **Minecraft textures by Mojang Studios**, converted to
this mod's layout. They are not covered by this mod's licence: Mojang's art is
Mojang's, and is shipped here only because the mod paints its coats onto the same
vanilla horse mesh, inside Minecraft, for Minecraft players, under Mojang's terms
for mods (the Minecraft EULA and Usage Guidelines). Do not reuse them outside a
Minecraft mod.

Each is the vanilla 64px sheet scaled 2x by nearest neighbour, with the leg and
ear patches that vanilla shares copied into the room this mod's mesh gives each
leg and ear. `common/sheet-sources/manifest.txt` lists every one of them with its
source file, and `./gradlew :common:convertHorseSheets` regenerates them from it;
a new sheet converted from Mojang art is a new line there and here.

    assets/horsegenetics/textures/entity/horse/horse_white.png
        from minecraft:textures/entity/horse/horse_white.png
    assets/horsegenetics/textures/entity/horse/horse_white_baby.png
        from minecraft:textures/entity/horse/horse_white_baby.png
    assets/horsegenetics/textures/entity/horse/horse_skeleton.png
        from minecraft:textures/entity/horse/horse_skeleton.png
    assets/horsegenetics/textures/entity/horse/horse_skeleton_baby.png
        from minecraft:textures/entity/horse/horse_skeleton_baby.png
    assets/horsegenetics/textures/entity/horse/horse_zombie.png
        from minecraft:textures/entity/horse/horse_zombie.png
    assets/horsegenetics/textures/entity/horse/horse_zombie_baby.png
        from minecraft:textures/entity/horse/horse_zombie_baby.png

The unconverted 64px originals of the two white sheets ship beside them
(`horse_white_vanilla64.png`, `horse_white_baby_vanilla64.png`); the undead
originals are kept in the repository only, under `common/sheet-sources/minecraft/`.

## Where the rest is written

The plain-language version of all of this, with a table of every exception, is
`wiki/licensing.html`.
