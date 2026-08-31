# Breeding & pedigree - how it works today

This is the single source of truth for the breeding / horse-record system.
**Keep it current**: whenever the code that assigns records, combines genetic
codes, tracks ancestry, or shows names changes, update this file in the same
change. README.md deliberately carries none of this.

Everything here is implemented and compiles. The core breeding round-trip is
**owner-verified as of 2026-08-30**: breeding two horses in-game produces a
foal with stats between the parents, a correctly combined genetic code, and an
inherited first/last name. The remaining unchecked details (paper dump,
attribute application, panel tint, legacy saves) are listed under "Not
verified" at the bottom.

---

## What "breeding" covers

1. **Genetic-code combination** - given two parents' codes, produce a child's.
2. **Stat inheritance** - a foal's `speed` / `health` numbers, rolled from the
   parents (random for now; Mendelian later).
3. **The horse record** - the per-horse data bag (`HorseRecord`): identity,
   sex, name, genetic code, parents, stats.
4. **The ancestry database** - a server-wide store of every record, queryable
   for a horse's ancestors.
5. **Integration** - attaching a record to a live `Horse`, persisting it,
   showing the name, pushing stats onto the entity's attributes, syncing it
   to clients, and the in-game surfaces: the paper chat dump, the name-tag
   rename, the stick tame, the inventory-screen panel, and the family-tree
   screen.

Layer 1 (`common/`, pure Java) owns 1-4. Layer 2 (`neoforge-26.1.2/`) only
translates - it never decides *how* codes combine, *what* an ancestor is, or
the *band* a foal's stats are rolled from.

---

## Layer 1 - the domain model (`common/`)

### `genetics/Genotype.breedWith(Genotype other, Rng)`

Three loci now (full phenotype table in CLAUDE.md): a **6-character** code -
E-locus, then A-locus, then W-locus (`E`/`e`, `A`/`a`, `W`/`w`). Legacy
4-character codes (pre-W) still parse and are read as `ww`.

`breedWith` is Mendelian segregation: the child gets **one allele from each
parent at each locus**, each drawn 50/50 between that parent's two alleles.
Six `Rng.nextBoolean()` draws, in order:

| draw | picks |
|------|-------|
| 1 | child's E allele from `this` parent (`true` = first allele) |
| 2 | child's E allele from `other` parent |
| 3 | child's A allele from `this` parent |
| 4 | child's A allele from `other` parent |
| 5 | child's W allele from `this` parent |
| 6 | child's W allele from `other` parent |

The result goes through `Genotype.of`, which canonicalizes allele order
(dominant first), so `breedWith` is symmetric in its two parents. Example:
`EEAAww x eeaaww` is **always** `EeAaww`. `W` is dominant over everything -
one `W` allele from either parent makes the foal solid white.

### `horse/HorseStats.rollFoalStat(double parentA, double parentB, Rng)`

How a foal's numeric stats come from its parents - **not genetic yet**, a
placeholder until these fold into the Mendelian model. One `Rng.nextFloat()`
draw, uniform in the band:

```
[ 0.75 * min(parentA, parentB) , 1.5 * max(parentA, parentB) ]
```

so a foal can meaningfully out- or under-do both parents. Applied
independently to **speed** and **health** (two draws per foal). There is
**no upper cap** - a determined breeder can push a line to absurd numbers.
{@link HorseRecord} then rounds the stored value up: health to a whole
number, speed to 3 decimals.

### `genetics/GeneticCodeCombiner.combine(String motherCode, String fatherCode, Rng)`

The string-level seam so callers never touch `Genotype`. Parses both codes,
calls `mother.breedWith(father, rng)`, returns `.toCode()`. Throws
`IllegalArgumentException` if either code is malformed. Symmetric - argument
order only matters for which parent id gets recorded as mother vs father,
not for the child code.

### `horse/Sex`

`MALE` / `FEMALE`. Vanilla horses have no sex; this is mod-assigned and used
to decide which parent is the dam / sire, to gate breeding (needs one of
each), and for display. `Sex.label(boolean adult)` gives the horse term:
**stallion / mare** for adults, **colt / filly** for foals.

### `horse/HorseRecord`

A record (immutable value type):

| field | type | notes |
|-------|------|-------|
| `id` | `UUID` | the entity's own `getUUID()` |
| `sex` | `Sex` | |
| `firstName` | `String` | alpha half of the registered name; may be blank |
| `lastName` | `String` | beta half; may be blank - **but not both blank** (enforced by the name-tag handler) |
| `barnName` | `Optional<String>` | free-form display override, clamped to 16 chars; owner-editable |
| `geneticCode` | `String` | a `Genotype` code |
| `motherId` / `fatherId` | `Optional<UUID>` | dam / sire, if bred |
| `tamedBy` | `Optional<String>` | username of whoever tamed it |
| `bredBy` | `Optional<String>` | username of whoever bred this foal |
| `generation` | `int` | **0** for a foundation horse; a child of two foundations is **1**; otherwise `1 + max(dam.generation, sire.generation)`, fixed at birth. Not the family-tree column depth. |
| `speed` | `double` | movement-speed attribute value, **rounded up to 3 decimals** by the ctor, uncapped. **`0.0` = not recorded yet**. Founder copies the entity's; foal is a `HorseStats.rollFoalStat`. |
| `health` | `double` | max-health attribute value, **rounded up to a whole number** by the ctor, uncapped. Same `0.0` = unrecorded convention. |
| `parentStats` | `Optional<ParentStats>` | low/high of the two parents' speed & health at birth (`ParentStats(speedMin, speedMax, healthMin, healthMax)`), for the UI to colour this horse's stats. Absent for founders / pre-field records. |

- `displayName()` = `barnName` if present, else `"firstName lastName"` (stripped).
- `attribution()` = `bredBy` if present, else `tamedBy` - "who is this horse's human".
- `hasName()` = any name part non-blank or a barn name set (the blank sentinel record has none).
- `hasStats()` = `speed > 0 && health > 0` - i.e. the stat fields are filled in.
- `ceilSpeed(v)` / `ceilHealth(v)` - the rounding the ctor applies; also public for callers.
- `ParentStats.rankSpeed(v)` / `rankHealth(v)` -> `1` above both parents, `0` between, `-1` below both.

Factories: `founder(id, sex, first, last, code)` (generation 0, stats `0.0`,
no parentStats), `bred(id, sex, first, last, code, motherId, fatherId,
generation)`. Copy helpers: `withNames`, `withBarnName`, `withTamedBy`,
`withBredBy`, `withStats(speed, health)`, `withParentStats(ParentStats)`. The
constructor null-checks required fields, normalizes `null` `Optional`s,
strips/clamps `barnName` to 16 chars, clamps `generation` to `>= 0`, and
rounds both stats up.

### `horse/HorseDatabase` + `horse/InMemoryHorseDatabase`

Interface: `record(HorseRecord)`, `lookup(UUID)`, `ancestorsOf(UUID, depth)`.

`ancestorsOf(id, depth)` is a breadth-first walk up the mother/father links:

- **nearest generation first** - parents, then grandparents, ...
- `depth` = how many generations: `1` = parents only, `2` = + grandparents.
- `depth <= 0` returns an empty list.
- the horse itself is never included.
- **each ancestor appears at most once**, even under inbreeding (a horse
  reachable through both the maternal and paternal line is listed once).
- an ancestor referenced by id but **absent from the database is skipped**,
  and that branch is not explored further through it.

`InMemoryHorseDatabase` is a plain `HashMap<UUID, HorseRecord>`. Not
thread-safe - in the mod, every call is on the server thread. `all()` gives
a snapshot for serialization; `record` overwrites by id.

---

## Layer 2 - integration (`neoforge-26.1.2/`)

### `data/HorseRecordCodecs`

`MapCodec<HorseRecord>` / `Codec<HorseRecord>`, kept **out of** the Layer-1
type so `HorseRecord` stays free of DataFixerUpper.

NBT keys: `id`, `sex`, `first_name`, `last_name`, `barn_name`,
`genetic_code`, `mother_id`, `father_id`, `tamed_by`, `bred_by`,
`generation`, `speed`, `health`, `parent_stats` (all the optional ones
`optionalFieldOf`, `first_name` / `last_name` default `""`, `generation` /
`speed` / `health` default `0`). `parent_stats` is a nested compound
(`speed_min`, `speed_max`, `health_min`, `health_max`) via `PARENT_STATS`.
Old saves without the newer keys load fine and get backfilled on next join.
`id` / parent ids use `UUIDUtil.STRING_CODEC`; `sex` is `Codec.STRING.xmap`
over the enum name. The record now has 14 components - still under the
`RecordCodecBuilder.group` limit of 16.
Also exposes `STREAM_CODEC` / `LIST_STREAM_CODEC` (`ByteBufCodecs.fromCodec`,
NBT over the wire) for the sync payloads.

### `data/ModAttachments.HORSE_RECORD`

A NeoForge Data Attachment whose **value type is the Layer-1 `HorseRecord`
itself**. Registered with `HorseRecordCodecs.MAP_CODEC` for serialization,
so it persists in the entity's NBT with no hand-rolled save/load.

The default value is a **sentinel**: `HorseRecord.founder(holder.getUUID(),
Sex.FEMALE, "", "", "eeaa")` - built via the `AttachmentType.builder(Function<
IAttachmentHolder, T>)` overload so it can read the holder entity's own
UUID. Its blank first/last name is how the code tells "never assigned" from a
real record (`HorseRecords.hasRealRecord` -> `hasName()`). No `copyOnDeath`
(meaningless for a horse).

### `data/HorseAncestryData extends SavedData implements HorseDatabase`

The persistent ancestry store. A thin wrapper: it holds an
`InMemoryHorseDatabase` and forwards every call to it. What it adds:

- **dirty-tracking**: `record(...)` only calls `setDirty()` when the record
  actually changed (`HorseRecord` is a value type, so an unchanged
  re-`record` compares equal and is a no-op).
- **persistence**: `CODEC` is a `RecordCodecBuilder` over
  `{ "horses": [ <HorseRecord>, ... ] }`; `SavedDataType` id is
  `horsegenetics:horse_ancestry`.
- **scope**: server-global, not per-level. `HorseAncestryData.get(server)` =
  `server.getDataStorage().computeIfAbsent(TYPE)`
  (`MinecraftServer#getDataStorage()`), so a parent stays lookup-able even
  when its entity is unloaded, in another dimension, or dead.

### `server/HorseRecords` - the adapter

The only class that talks to both sides. No genetics / naming / lookup logic
lives here.

| method | does |
|--------|------|
| `hasRealRecord(horse)` | `of(horse).hasName()` |
| `of(horse)` | `horse.getData(HORSE_RECORD)` |
| `apply(horse, record)` | write the attachment, `setCustomName(record.displayName())` + `setCustomNameVisible(true)`, `HorseAncestryData.record(record)`, **and `sendToPlayersTrackingEntity` a `HorseRecordSyncPayload`** (all when on a `ServerLevel`) |
| `newFounder(horse, rng[, sex])` | founder record; random genotype / name; sex random or forced; **`.withStats(entitySpeed(horse), entityHealth(horse))`** |
| `entitySpeed / entityHealth (horse)` | `horse.getAttributeValue(MOVEMENT_SPEED / MAX_HEALTH)` |
| `applyStatsToEntity(horse, record, fullHeal)` | push `record.speed()` / `record.health()` onto the entity's attribute base values; `fullHeal` sets current HP to the new max (newborn) vs just clamps it (reload). No-op for `0.0`. |
| `backfillStatsIfMissing(horse)` | if `!of(horse).hasStats()`, `apply(of(horse).withStats(entitySpeed, entityHealth))` |
| `newNameParts(rng)` | `HorseNameGenerator.generateParts` - `{first, last}` |
| `rename(horse, first, last)` | `apply(of(horse).withNames(first, last))` - name-tag hook |
| `setBarnName(horse, text)` | `apply(of(horse).withBarnName(...))`; blank -> clear |
| `setTamedBy(horse, username)` | `apply(of(horse).withTamedBy(username))`, only if still empty |
| `randomSex(rng)` | `nextBoolean() ? MALE : FEMALE` |
| `rng(horse)` | `new NeoRng(horse.getRandom())` |

---

## Runtime flows

### A. Wild / natural spawn - founder record

`HorseGeneticsEventHandler.onHorseJoin` on `EntityJoinLevelEvent` (server
side, any `Horse`) -> `ensureRecordAndCoat(horse)`:

1. `rng` from `horse.getRandom()`.
2. If the horse has **no real record** -> `apply(horse, newFounder(...))`:
   random genotype, random sex, generated name, no parents, `speed` /
   `health` copied from the entity's current attribute values.
3. Otherwise (bred or reloaded) -> `backfillStatsIfMissing`, re-`record()` it
   into the global DB (idempotent, change-guarded), `applyStatsToEntity(...,
   fullHeal=false)` so a reloaded horse keeps its rolled attributes, and
   re-set the custom name if it's missing.
4. **Coat**: if the coat attachment is missing or still the `"eeaa"`
   sentinel, derive it from `Genotype.parse(of(horse).geneticCode())` via
   `CoatGenerator.generate`, store `HorseCoatAttachment`, and send the coat
   sync packet.

Step 4 means the coat is **always derived from the record's genetic code** -
a bred foal's coat matches the genes it actually inherited.

The horse-dimension pens spawn horses with `addFreshEntity` but pre-apply
the founder record first (via `newFounder(horse, rng, sex)`) so each pen ends
up with exactly **one mare and one stallion**. `onHorseJoin` then sees a real
record and only fills in the coat.

### B. Real breeding - combined record

`HorseBreedingHandler.onBabySpawn` on `BabyEntitySpawnEvent` (fired from
`Animal#spawnChildFromBreeding` *before* the foal is added to the world):

1. Require `parentA`, `parentB`, and `child` to all be `Horse`.
2. `ensureParentRecord(parent)` for each parent: if a parent predates the
   mod / was never assigned, give it a founder record now.
3. **Sex gate**: if the two parents have the **same sex**, `event.setCanceled(
   true)` and stop - no foal (breeding needs one mare and one stallion).
4. **Dam / sire**: the `FEMALE` parent is the dam, the `MALE` one the sire.
   Before this, `ensureParentRecord` also **backfills each parent's stats**
   from its entity if the record's are still `0.0`, so the roll has real
   numbers to work from.
5. `childCode = GeneticCodeCombiner.combine(dam.geneticCode(),
   sire.geneticCode(), rng)`.
6. `childGeneration = 1 + max(dam.generation(), sire.generation())`.
7. **Stats**: `childSpeed = HorseStats.rollFoalStat(dam.speed(),
   sire.speed(), rng)`, `childHealth = rollFoalStat(dam.health(),
   sire.health(), rng)` -> `.withStats(...)` (band `[0.75*min, 1.5*max]`,
   rounded up, uncapped); plus `.withParentStats(ParentStats.of(dam.speed(),
   sire.speed(), dam.health(), sire.health()))` so the UI can colour the
   foal's numbers against its parents.
8. **Name**: `HorseNames.breed({dam.first, dam.last}, {sire.first, sire.last},
   rng)` - the foal gets the first name of one parent and the last name of
   the other (never both halves from one).
9. `bredBy` = `event.getCausedByPlayer()`'s username, if a player caused it.
10. **Auto-tame**: if the dam entity `isTamed()`, `child.setTamed(true)` +
    `child.setOwner(dam.getOwner())`, and `tamedBy` = the dam owner's username
    (when the owner is a player).
11. `HorseRecords.apply(child, childRecord)` then
    `applyStatsToEntity(child, childRecord, fullHeal=true)` so the foal
    spawns with its rolled speed / max health (and full HP). If the child has
    no `ServerLevel` yet, flow A step 3 re-records it when the foal joins.
12. The foal's **coat** is derived from `childCode` in flow A step 4. Foals
    render the vanilla `*_baby` texture for the phenotype (no bay leg
    compositing on the baby model).

### C. Paper inspector - chat dump

`HorsePaperInspectHandler.onEntityInteract` on
`PlayerInteractEvent.EntityInteract` (server side):

- **Trigger**: the held item `is(Items.PAPER)` and the target is a `Horse`.
- Chat lines: name, `id`, the **horse term** (`sex.label(!isBaby())` -
  stallion / mare / colt / filly), `generation`, `genetic code`, **`speed`
  (3dp) / `health` (whole)** (or `(unrolled)` when `!hasStats()`), a
  **`vs parents`** line (`above both` / `between` / `below both`) when
  `parentStats` is present, `bred by`, `tamed by`, `sire` / `dam` (resolved
  to names via `HorseAncestryData.lookup`, or `unknown` for a founder), and
  `ancestors (3 gen)` from `ancestorsOf(id, 3)`.
- `player.sendSystemMessage(...)`, `event.setCanceled(true)`,
  `setCancellationResult(InteractionResult.SUCCESS)`.

### D. Name-tag rename (first / last)

`HorseInteractionHandler.onEntityInteract`: a **renamed** name tag
(`Items.NAME_TAG` with a `CUSTOM_NAME`) on a horse that already has a real
record. The tag's text is split on the **first space** - the part before is
the new `firstName`, the rest is the new `lastName`. Either may be blank but
**not both** (blank-both is ignored). Then `HorseRecords.rename(horse,
first, last)`, the tag is **consumed** (`stack.shrink(1)` unless creative),
and the event is cancelled (so vanilla doesn't also name the entity with the
raw string / double-consume).

### E. Barn name

Free-form display override, editable at will from the horse inventory screen
(an `EditBox` + a "Set" button, `HorseScreenHooks`). "Set" sends a
`SetBarnNamePayload(entityId, text)`; the server checks the player is within
8 blocks, then `HorseRecords.setBarnName` (blank clears it, otherwise clamp
to `HorseRecord.MAX_BARN_NAME` = 16). When a barn name is set it's what
`displayName()` returns.

### F. Debug-dimension right-clicks

`HorseInteractionHandler`, `DEBUG_LEVEL` only:

- **Stick** on an untamed horse -> `tameWithName(player)`, event cancelled.
- **Clock** on a foal -> `horse.setAge(0)` (instant adult), event cancelled.

### G. Tamer tracking

Wild-horse taming resolves inside the horse's own tick (after the bucking
logic), not during the interact packet, so we can't catch it in the
interaction handler. `HorseOwnerTrackingHandler` (`EntityTickEvent.Post`)
instead: every ~40 ticks, any tamed horse whose record has no `tamedBy` yet
gets its **owner's** username recorded (`horse.getOwner()`, or the online
`ServerPlayer` for `getOwnerReference().getUUID()`). Once set, the check
short-circuits.

`Player#getGameProfile().name()` - authlib `GameProfile` is a record, no
`getName()`.

### H. Client sync + inventory screen + family tree

The record attachment is server-only, so it's pushed to clients:

- `HorseRecordSyncPayload` (S->C, entity id + record) is sent on
  `PlayerEvent.StartTracking` and from `HorseRecords.apply` (spawn / breed /
  rename / barn-name / tamer). The client stores it in
  `ClientHorseRecordCache` keyed by both entity id and record UUID.
- `HorseScreenHooks` (`@EventBusSubscriber(Dist.CLIENT)`) hangs a metadata
  panel (display name, `(first last)` only if a barn name is set, horse term
  + `gen N`, genetic code, **`speed` / `health`** - tinted green/amber/red
  by `parentStats.rank*` - and bred-by / tamed-by), an **editable barn-name
  field** (see flow E), and a **"Family Tree"** button off the vanilla
  `HorseInventoryScreen` (`ScreenEvent.Init.Post` / `Render.Post`). It's a
  **grey vanilla-style panel to the left of the horse GUI**, behind a
  collapsible tab button on its edge - additive, so a second horse-inventory
  mod isn't clobbered. Reads the horse off `player.getVehicle()` and its
  record from the cache.
- `FamilyTreeScreen` is a pedigree chart: the subject is the right-hand
  column, each column left is one chart-step older, out to
  **great-grandparents** (column 3), doubling each step. **Within every pair
  the sire is the top box, the dam the bottom box.** Each box shows the
  display name, the horse term, **`by <breeder-or-tamer>`** (from
  `record.attribution()`), and a small **coat swatch** - the horse's flat
  coat texture (`GeneticHorseRenderer.coatTextureFor`) blitted small, which
  is one textured quad, no extra memory. **No genetic code.** Connectors are
  three-segment elbows. On open / every re-centre it sends
  `FamilyTreeRequestPayload(rootId)`; the server replies
  `FamilyTreeDataPayload` = root + `ancestorsOf(root, 3)`, merged into
  `ClientHorseRecordCache`. Clicking a known ancestor box re-roots the tree.
  A foundation horse shows empty parent boxes. (Chart column != the record's
  `generation` number.)

---

## Persistence summary

| data | where | survives / scope |
|------|-------|----------|
| a horse's own `HorseRecord` | entity NBT, via the `HORSE_RECORD` attachment | world save/reload, chunk unload, dimension change |
| the ancestry table | `HorseAncestryData` SavedData (`horsegenetics:horse_ancestry`) | **per world** - it's `MinecraftServer#getDataStorage()`, which writes to `<save>/data/horsegenetics/horse_ancestry.dat`, so it's created per save and deleted when the save folder is deleted |
| generated bay-leg textures | in-memory `DynamicTexture`s in the client `TextureManager` (`GeneticCoatTextureFactory.CACHE`) | session only; `ClientLifecycleHandler` releases them and clears the client caches on `ClientPlayerNetworkEvent.LoggingOut`, so a world's textures don't leak into the next world |

`HorseRecord`s round-trip through `HorseRecordCodecs` - the same codec on
both sides.

---

## Not verified yet

Everything compiles and `runServer` boots clean (payloads, attachment,
SavedData, all handlers register).

**Owner-verified in-game (2026-08-30):** breeding a mare + stallion rolls a
foal whose speed/health sit between the parents, with a correctly combined
genetic code, and whose first/last name is inherited from the two parents.

What no automated check has covered yet:

- The rest of the foal record: `generation` = 1 + max(parents); `bredBy` set;
  auto-tamed to the dam's owner if the dam was tamed.
- Foal `speed` / `health` round up and are actually applied to the entity's
  attributes (a foal from two fast parents is fast; lineages can climb with no
  cap). Founder stats copy the entity's spawned attribute values.
- The inventory-panel `speed` / `health` tint (green above both parents,
  amber above one, red below both) matches `parentStats`.
- A `W` allele from either parent produces a solid-white foal
  (`horse_white` texture); legacy 4-char saved codes still load.
- Same-sex breeding is actually blocked.
- Name tag: splits on the first space into first/last, rejects both-blank,
  is consumed on use.
- Barn name: the inventory `EditBox` + "Set" button round-trips through
  `SetBarnNamePayload` and drives `displayName()`.
- **"Tamed by" actually populates now** (via `HorseOwnerTrackingHandler`) -
  the previous version relied on a same-tick check that missed wild-horse
  taming.
- Foal texture uses `*_baby`, not a stretched adult one.
- The panel and **`FamilyTreeScreen`** render, the tree boxes are clickable
  (sire-top / dam-bottom), the coat swatch shows. This SDK's reworked GUI
  API (`GuiGraphicsExtractor.extractRenderState`,
  `mouseClicked(MouseButtonEvent, boolean)`, `EditBox`) - compiled but never
  seen, layout is a first guess.
- Attachment + SavedData surviving save -> quit -> reload; the texture /
  cache cleanup firing on world exit.

## Known limitations / rough edges

- **Stat inheritance is random, not genetic.** `HorseStats.rollFoalStat` is a
  placeholder; the plan is to fold speed / health into the Mendelian model
  later. Jump strength isn't tracked at all yet.
- **White (`W`) is a plain on/off dominant** - no true-white subtleties, no
  interaction with E/A other than masking them. Wild frequency is
  `Genotype.WILD_WHITE_ALLELE_ODDS` (1 in 50 per allele).
- No inbreeding prevention, no generational effects, no sex-linked loci -
  the three E/A/W loci combine independently.
- Name-generation output is rough and slated for a rework (deferred by the
  owner). The rule - `<alpha> <space> <beta>` - is fixed.
- `HorseRecord.geneticCode` and `HorseCoatAttachment.genotypeCode` both hold
  the genotype string. The coat derives from the record so they stay
  consistent, but this redundancy is a likely future consolidation.
- `ancestorsOf` only walks through records present in the database; a gap in
  the recorded chain ends that branch.
- The family tree needs a fresh server request per re-centre; there's no
  local caching of "the whole tree" beyond what's arrived so far.

## File map

```
common/src/main/java/com/example/horsegenetics/common/
  genetics/Genotype.java            # breedWith(...)
  genetics/GeneticCodeCombiner.java # combine(motherCode, fatherCode, Rng)
  horse/Sex.java                    # + label(adult) -> stallion/mare/colt/filly
  horse/HorseRecord.java            # + speed / health (rounded up, uncapped), parentStats, withStats/withParentStats
  horse/HorseStats.java             # rollFoalStat(a, b, Rng) -> [0.75*min, 1.5*max]
  horse/ParentStats.java            # (speedMin,speedMax,healthMin,healthMax) + rankSpeed/rankHealth
  horse/HorseDatabase.java
  horse/InMemoryHorseDatabase.java
  name/HorseNameGenerator.java      # generateParts -> {first, last}; + resources/.../names/*.txt
  name/HorseNames.java              # breed({dam}, {sire}, rng) -> foal name parts

neoforge-26.1.2/src/main/java/com/example/horsegenetics/neoforge/
  data/HorseRecordCodecs.java             # Codec + StreamCodec for HorseRecord
  data/ModAttachments.java                # HORSE_RECORD
  data/HorseAncestryData.java             # SavedData over the DB
  network/HorseRecordSyncPayload.java     # S->C, one record
  network/FamilyTreeRequestPayload.java   # C->S, "tree rooted at UUID"
  network/FamilyTreeDataPayload.java      # S->C, root + ancestors
  network/SetBarnNamePayload.java         # C->S, set/clear barn name
  network/ModNetworking.java              # registers the above + request/barn-name handlers
  server/HorseRecords.java                # the adapter
  server/HorseGeneticsEventHandler.java   # natural spawn -> founder, + StartTracking sync
  server/HorseBreedingHandler.java        # BabyEntitySpawnEvent -> bred (name combine, bredBy, auto-tame, same-sex gate)
  server/HorsePaperInspectHandler.java    # paper -> chat dump
  server/HorseInteractionHandler.java     # name-tag first/last (consumed), stick tame, clock age-up
  server/HorseOwnerTrackingHandler.java   # EntityTickEvent -> fill in tamedBy from the owner
  client/ClientHorseRecordCache.java      # client store of synced records
  client/ClientLifecycleHandler.java      # clears caches + generated textures on world exit
  client/HorseScreenHooks.java            # inventory panel + barn-name box + Family Tree button
  client/FamilyTreeScreen.java            # the pedigree chart (with coat swatches)

common/src/test/java/com/example/horsegenetics/common/
  genetics/GeneticCodeCombinerTest.java
  horse/HorseRecordTest.java
  horse/HorseStatsTest.java
  horse/InMemoryHorseDatabaseTest.java
  horse/SexTest.java
  name/HorseNamesTest.java
```
