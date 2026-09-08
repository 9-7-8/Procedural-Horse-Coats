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
 * ({@code wiki/breed-designer/}) writes - out of
 * {@code config/horsegenetics/breeds/} and into the {@link Breeds} registry.
 *
 * <p>This is the twin of {@link ModGeneSpecs}, on purpose: same folder shape,
 * same README, same "a broken file is logged and skipped, never fatal" rule. A
 * player who has learnt to drop a gene in has already learnt this.
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

    /** Relative to the instance's {@code config/} folder. */
    public static final String FOLDER = "horsegenetics/breeds";

    private static final String README = """
            Horse Genetics - drop-in breeds
            ===============================

            Every .json file in this folder is loaded as a breed when the game
            starts, in filename order, on top of the breeds the mod ships. Wild
            herds, the cowboy's string, generated stables and the breed spawn
            eggs all draw from the same registry, so a breed you add here turns
            up everywhere a built-in one does.

            Make them with the breed designer: wiki/breed-designer/index.html in
            the mod's repository. It previews the base coat on a real horse and
            writes the file for you.

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
        Path dir = FMLPaths.CONFIGDIR.get().resolve(FOLDER);
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
