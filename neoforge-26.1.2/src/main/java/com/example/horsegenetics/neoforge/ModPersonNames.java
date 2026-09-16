package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.common.breed.Region;
import com.example.horsegenetics.common.name.PersonNameGenerator;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads a player's own trader names out of {@code <game dir>/phc/names/} and
 * adds them to the tables the mod ships.
 *
 * <p>Same folder rules as {@link ModBreedSpecs}: beside the breeds and genes
 * folders in {@code .minecraft/phc/}, not in {@code config/}, made on first
 * launch with a README in it. A broken file is logged and skipped, never fatal.
 *
 * <p><b>Order does not matter here</b>, unlike genes and breeds. A name changes
 * no registry and lengthens no genotype code; it is read when a cowboy or a
 * horseman is first named and nowhere else. It runs after the breeds only
 * because {@link Region} is what validates the file names, and reading that
 * class is cheaper once the rest of the world is up.
 */
public final class ModPersonNames {

    /** Relative to the game directory. */
    public static final String FOLDER = ModBreedSpecs.ROOT + "/names";

    private static final String README = """
            Horse Genetics - drop-in trader names
            =====================================

            The cowboy and the horseman are named from the part of the world
            their horses came from - a man selling Fjords is a Halvorsen, a man
            selling Andalusians is an Olivares. Every .txt file in this folder
            ADDS names to one of those regions.

            Name a file after the region and which half of the name it holds:

              scandinavia-alpha.txt     given names
              scandinavia-beta.txt      family names

            The regions are:

              north_america          britain_and_ireland    western_europe
              scandinavia            mediterranean          eastern_europe
              central_asia           asia_pacific           africa_near_east
              latin_america

            The format is a comma-separated list. Newlines count as ordinary
            whitespace, so the file can be one long line or one name per line,
            whichever you find easier to edit:

              Sigrid, Torstein, Ragnhild,
              Eivind,
              Marit

            Blank entries are dropped. Duplicates are kept on purpose - a name
            listed twice is simply twice as likely, which is a fair way to make
            a common surname common.

            Notes:
              * Files here ADD to the names the mod ships; they do not replace
                them. To remove a shipped name you have to edit the mod's own
                table. Adding is the thing worth doing without a rebuild.
              * SAVE AS UTF-8. Accented names - Bjornstad, Emile, Makinen - turn
                into question marks if the file is saved as ANSI or Windows-1252,
                and nothing will warn you in game.
              * A file whose name is not "<region>-alpha.txt" or "<region>-beta.txt"
                is ignored and reported in the log, as is one naming a region
                that does not exist. Check the log if a name never turns up.
              * Names are read once at startup, so restart the game after editing.
              * A trader already named keeps the name they were given. New names
                only reach cowboys and horsemen that have not been named yet.
            """;

    private ModPersonNames() {
    }

    /** The drop-in folder on this machine. */
    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER);
    }

    /** Find, read and add. Returns how many extra names the folder supplied. */
    public static int load() {
        Path dir = folder();
        ensureFolder(dir);

        Set<String> known = new LinkedHashSet<>();
        for (Region region : Region.values()) {
            known.add(region.id());
        }

        List<String> problems = PersonNameGenerator.loadFrom(dir, known);
        for (String problem : problems) {
            HorseGenetics.LOGGER.warn("[names] {}", problem);
        }

        int added = 0;
        for (Region region : Region.values()) {
            added += PersonNameGenerator.extrasFor(region.id(), true)
                    + PersonNameGenerator.extrasFor(region.id(), false);
        }
        if (added > 0) {
            HorseGenetics.LOGGER.info("[names] {} extra trader name(s) from {}", added, dir);
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
            HorseGenetics.LOGGER.warn("[names] could not prepare {}: {}", dir, e.toString());
        }
    }
}
