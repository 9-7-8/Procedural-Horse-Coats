package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads the player's own breeds - the JSON files the breed designer
 * ({@code wiki/breed-designer/}) writes - out of {@code <game dir>/phc/breeds/}
 * and into the {@link Breeds} registry.
 *
 * <p><b>The folder is in the game directory, not in {@code config/}.</b> That is
 * {@code .minecraft/phc/breeds/} for a vanilla launcher and the instance folder
 * for any other, and it is where a player who has only the jar is told to look:
 * it is made on the first launch with the mod installed (this runs from the mod
 * constructor), it carries a README, and the Breeds tab of the H menu has a
 * button that opens it. A breed is the thing a player is invited to make, so
 * its folder should not be three levels into a directory of settings files.
 *
 * <p>Same rules as {@link ModGeneSpecs} otherwise: a broken file is logged and
 * skipped, never fatal.
 *
 * <p><b>It runs after {@link ModGeneSpecs}</b>, and that order is the whole
 * reason this is a separate call rather than a static initialiser. A breed file
 * is mostly references to genes; loading breeds before the drop-in genes exist
 * would mean every breed that named one lost that locus with a warning, which is
 * the failure mode the warning exists to report and the last one you want to
 * cause yourself.
 *
 * <p>Unlike a gene, a breed <b>does not</b> change the shape of the genotype
 * code, so adding or removing one costs no saved horse anything. That is what
 * makes breeds the safe thing for a player to fiddle with.
 */
public final class ModBreedSpecs {

    /**
     * The mod's own folder in the game directory - {@code .minecraft} for the
     * vanilla launcher. It holds the breeds folder and the three settings
     * files, so everything a player is told to touch is in one place.
     */
    public static final String ROOT = "phc";

    /** Relative to the game directory. */
    public static final String FOLDER = ROOT + "/breeds";

    /**
     * A settings file's name as NeoForge's {@code registerConfig} takes it,
     * putting the file in {@code .minecraft/phc/} rather than {@code config/}.
     *
     * <p>NeoForge resolves a config file name against the config directory with
     * a plain {@code Path.resolve} and creates missing parents
     * ({@code ConfigTracker.openConfig} / {@code setupConfigFile}, FML 11.0.15),
     * so a leading {@code ..} is taken literally. That is read from source, not
     * seen documented: <b>unverified API usage</b>. The name is a constant
     * rather than a computed relative path because a SERVER config is synced to
     * clients by file name, and both ends must spell it the same.
     *
     * <p>One consequence worth knowing: a SERVER config's per-world override is
     * looked up as {@code <world>/serverconfig/<name>}, which for this name is
     * {@code <world>/phc/server.toml} - so a world can carry its own copy there.
     */
    public static String configFile(String name) {
        return "../" + ROOT + "/" + name;
    }

    /**
     * The drop-in folder on this machine. The client asks too, to open it: on
     * a single-player world the integrated server and the client share a game
     * directory, so it is the folder the breeds were read from. On a dedicated
     * server the breeds are the server's, and the button opens the player's own
     * copy - which is where they would make a breed to send to the server's
     * owner, so it is still the right folder to open.
     */
    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER);
    }

    private static final String README = """
            Horse Genetics - drop-in breeds
            ===============================

            Every .json file in this folder is loaded as a breed when the game
            starts, in filename order, on top of the breeds the mod ships. Wild
            herds, the cowboy's string, generated stables and the breed spawn
            eggs all draw from the same registry, so a breed you add here turns
            up everywhere a built-in one does.

            Make them with the breed designer, which walks you through a breed
            one step at a time and shows the horses it makes as you go:

              https://9-7-8.github.io/Procedural-Horse-Coats/wiki/breed-designer/

            Export the breed there, save the .json into this folder, and restart
            the game. The Breeds tab of the H menu has a button that opens this
            folder.

            The breeds the mod ships are changed in ../breed-spawning.toml, one
            folder up: switch any of them off, move them to other biomes, make
            them rarer or commoner - or switch them all off, and Feral Mixed
            too, and your world has only the breeds in this folder. The mod's
            other settings are beside it (server.toml, client.toml).

            Notes:
              * A breed's "id" must be lower case and unique. A file whose id
                collides with one already loaded is ignored.
              * A breed may name genes this install has not got - a gene from a
                pack you are not running, say. Each one is reported in the log
                and that locus is rolled wild; the rest of the breed still
                loads. This is deliberate, so a breed can be shared between
                installs that do not have identical gene lists.
              * "spawn" is a checklist: wild, cowboy, spawn_egg, stable. Leave
                it out and the breed is allowed all four. Write "spawn": [] and
                it comes from nowhere at all.
              * A wild breed's herds turn up in the biomes it lists - any biome,
                including modded ones, as long as a horse can stand there. They
                appear as new chunks are generated, so explore to find them.
              * "spawn_time" is "day" or "night" to found herds only then.
              * Unlike a gene, a breed does NOT change the genotype code, so
                adding or removing one will not invalidate horses you already
                have.
              * A file that fails to load is reported in the log and skipped;
                the others still load.
            """;

    private ModBreedSpecs() {
    }

    /** Find, parse and register. Returns how many breeds the folder added. */
    public static int load() {
        Path dir = folder();
        ensureFolder(dir);

        int before = Breeds.all().size();
        List<String> problems = Breeds.loadFrom(dir);
        int added = Breeds.all().size() - before;

        for (String problem : problems) {
            HorseGenetics.LOGGER.warn("[breeds] {}", problem);
        }
        HorseGenetics.LOGGER.info("[breeds] {} breed(s) registered ({} from {})",
                Breeds.all().size(), added, dir);
        if (HorseGenetics.LOGGER.isDebugEnabled()) {
            for (Breed breed : Breeds.all()) {
                HorseGenetics.LOGGER.debug("[breeds]   {} - {} ({})",
                        breed.id(), breed.name(), breed.sources());
            }
        }
        return added;
    }

    /**
     * Create the folder on first run and leave a note in it. A folder that
     * exists is discoverable; one the player has to guess the name of is not.
     */
    private static void ensureFolder(Path dir) {
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            HorseGenetics.LOGGER.warn("[breeds] could not prepare {}: {}", dir, e.toString());
        }
    }
}
