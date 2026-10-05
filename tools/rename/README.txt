RENAME MACHINERY - WRITTEN 2026-10-04; converter self-test passed on synthetic data; nothing run on a real world

Everything here and in converter/ was written from a cloud session while another change was in flight on the owner's
machine, so NOTHING was executed against the repo, a real world or the Gradle build. The converter's Java was compiled
with plain javac into a scratch folder and its --self-test (synthetic world, temp folder) PASSED on 2026-10-04 after one
fix it found (translation keys such as item.horsegenetics.x were being left behind). The node scripts were only
syntax-checked (node --check). No existing file in the repo was edited: settings.gradle.kts, neoforge.mods.toml and
every source file are as they were. Plan: GitHub issue #142 (the rename, label `rename`; pieces #143-#145).

WHAT IS HERE
  converter/                          the standalone world converter (a new Gradle module, NOT yet in settings.gradle.kts)
    build.gradle.kts                  how to turn it on (one line in settings.gradle.kts) and what it builds
    src/main/java/ixoras/converter/   Main, WorldConverter, PackConverter, RegionFile, Nbt, Rewriter, Lz4, SelfTest
  tools/rename/
    rename.config.json                the settings; apply.mjs refuses while a value is TODO (newPackage, configFolder.new)
    common.mjs                        helpers
    plan.mjs                          READ-ONLY inventory of what apply would change
    apply.mjs                         the mechanical rename (dry run by default; --apply to do it; leaves it uncommitted)
    check.mjs                         READ-ONLY leftovers check after apply
    apply-alias.mjs                   adds the old-prefix READ alias to Genes.java (after apply)
    staged/read-old-prefix.patch      the same alias as a patch for a PRE-rename tree (reference)
    staged/LegacyPrefixTest.java.txt  the test to add for the alias (not compiled)
    staged/OldWorldGuard.java.txt     the refuse-to-load guard, all UNVERIFIED NeoForge API (not compiled)
    runbook-live-cutover.txt          the owner's steps for the real server, written, never to be run by a session
  converter/launchers/                Convert Worlds.bat and convert-worlds.sh (double-click launchers; Java search UNVERIFIED)

THE ORDER (matches the treatment: converter first, it gates everything)
 0. Settled (owner, 2026-10-04): newPackage ixoras_horses, configFolder.new ixoras_horses, class renames, intake/ rewritten. No TODO is left in rename.config.json. The freeze (nothing else lands on main) starts first.
 1. Turn the converter on:  add  include(":converter")  to settings.gradle.kts.
    ./gradlew :converter:jar      then      java -jar converter/build/libs/ixoras-converter.jar --self-test
    The self-test builds a synthetic world, converts it, checks zero old tokens, a byte-identical original and a no-op
    second run. If it fails, fix the converter before going near a real world.
 2. Dry-run it on a COPY of the live world:   java -jar ... "<copy>" --dry-run     then convert it. Read the report.
    Load the converted copy in the NEW jar only after step 4 below exists. The treatment's "done when" is in the report.
 3. node tools/rename/plan.mjs                      read the counts against the treatment's (about 8400 mentions)
    node tools/rename/apply.mjs                     dry run
    git switch -c rename-ixoras ; node tools/rename/apply.mjs --apply     leaves the result UNCOMMITTED
    node tools/rename/apply-alias.mjs --apply       then add LegacyPrefixTest from staged/
    node tools/rename/check.mjs
 4. Build the mod-side pieces the scripts do not write: the config-folder auto-move (16 Java files read "phc"), the
    refuse-to-load guard (staged/OldWorldGuard.java.txt, verify its UNVERIFIED items first), the world converter's
    release notes. Then walk procedures/rebake.txt in full (the only copy of what to regenerate, in order): the coat
    goldens (they carry gene keys), :web:bakeDesignerAssets and check-designer-boots, the gene wiki pages, wiki/text,
    the search index, check-parity, check-links, :neoforge-26.1.2:build, runGameTest, a server boot to "Done (".
 5. The release is the compat release 0.6.0 (owner confirms at release time). The LIVE cutover is a later, separate step
    run by the owner from runbook-live-cutover.txt.

MANUAL EDITS AFTER apply.mjs (the script cannot do them)
 - Add "formerly Horse Genetics" to the README opening, the mod description in neoforge.mods.toml and the 0.6.0 release notes (the display
   step rewrote the name everywhere else).
 - Declare the old mod id incompatible in neoforge.mods.toml (a [[dependencies.ixoras_horses]] entry with modId = "horsegenetics",
   type = "incompatible").
 - mods.toml authors / displayURL / issueTrackerURL / logo for the new brand (Open: owner).
 - The browser tools and drop-in file loaders read the OLD prefix too for 0.6 (designer and CustomHorseSpawnScreen together).
 - The repo rename (ixoras-horse-overhaul) and every wiki/README/release link, on the day 0.6.0 is released.

WHAT THE SCRIPTS DO NOT DO (so nobody assumes they did)
 - the config/breed folder rename (phc/ to the new name) and its first-launch auto-move;
 - the refuse-to-load guard and any NeoForge API call (all UNVERIFIED);
 - the browser-side localStorage keys of the wiki tools beyond the plain text replace (saved designs keyed by the old
   prefix are orphaned unless the designers also read the old keys: an Open, hard rule 5 applies to both screens);
 - the GitHub repo rename, release links, CI;
 - any re-bake, build or test.

WHAT THE REPLACE RULE TOUCHES, AND WHAT IT LEAVES
 - Rewrites every case-sensitive lowercase "horsegenetics" in tracked text files outside the excluded prefixes (listed in
   rename.config.json: intake/, wiki/session-log/, wiki/text/ (generated), converter/, tools/rename/, LICENSES/, build
   output). Capitalised "HorseGenetics" (the class name) is NOT touched by the id rule; the display rule handles
   "Horse Genetics". Binary files are never touched.
 - Renames every path segment or file name containing the old id (assets/horsegenetics, data/horsegenetics,
   horsegenetics.mixins.json, horsegenetics-carts.toml, ...) with git mv.
 - KNOWN RISKS to read in the dry run: system properties named horsegenetics.* (the Gradle runs set them and the code
   reads them: both are rewritten together); the coat golden files and any stored genotype in tests (rewritten, then
   re-run the two goldens: they must show NO change in coats); wiki prose that talks about the old name on purpose.

THE QUESTION "DO THE RENAME AND THE FOUR-JAR SPLIT AT ONCE?" (owner asked; recommendation)
 No: keep them separate, rename first.
 - The rename is mechanical and has one hard gate (a converter that proves itself on a copy of the live world). The split
   is a structural refactor with a different gate (the combination matrix). Together, a failure cannot be attributed.
 - The rename is the compat release (0.6.0, saves converted). The split is designed to keep every saved key, so it can
   ship as patch releases. Merging them would make every split piece wait on the converter.
 - Doing the rename FIRST is what makes the split cheaper: the packages move once, and the split's move script is written
   against the final names. The split's Base piece starts from the renamed tree.
 - If the owner still wants one release: ship the rename alone as 0.6.0, then the split's first piece as 0.6.1.
