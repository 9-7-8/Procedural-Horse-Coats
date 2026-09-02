package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.spec.GeneSpecLoader;
import com.example.horsegenetics.common.genetics.spec.SpecGene;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads the player's own genes - the JSON files the gene creator
 * ({@code wiki/gene-creator/}) writes - out of
 * {@code config/horsegenetics/genes/} and into the {@link Genes} registry.
 *
 * <p>This is the "and then upload them to the game" half of the tool. Save the
 * file the creator hands you into that folder, restart, and wild horses carry
 * the gene. No rebuild, no Java.
 *
 * <p><b>Timing matters.</b> It runs from the mod constructor, which is the
 * earliest hook there is, because every registration lengthens the genotype code
 * by a segment - a gene registered after something has already parsed a code
 * would leave that code unparseable. Nothing here touches a Minecraft registry,
 * so running this early is safe.
 *
 * <p>A broken file is logged and skipped, never fatal: one bad gene must not
 * cost the player the rest of their collection, and certainly must not stop the
 * game booting.
 */
public final class ModGeneSpecs {

    /** Relative to the instance's {@code config/} folder. */
    public static final String FOLDER = "horsegenetics/genes";

    private static final String README = """
            Horse Genetics - drop-in genes
            ==============================

            Every .json file in this folder is loaded as a gene when the game
            starts, in filename order. Wild horses will carry it, foals will
            inherit it, and it gets its own segment in the genotype code.

            Make them with the gene creator: wiki/gene-creator/index.html in the
            mod's repository. It previews the gene on a 3D horse and writes the
            file for you.

            Notes:
              * A gene's "key" must be "<yourmodid>.<gene>" and must be unique.
              * "priority" decides where the gene sits relative to other
                drop-in genes - lower runs first. Two people who drop the same
                files in a different order get the same horses.
              * Adding or removing a gene changes the genotype code's shape, so
                horses saved before the change will not load. That is expected
                for now (this is a dev mod with no save compatibility).
              * A file that fails to load is reported in the log and skipped;
                the others still load.
            """;

    private ModGeneSpecs() {}

    /** Find, parse and register. Returns how many genes were added. */
    public static int load() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(FOLDER);
        ensureFolder(dir);

        int before = Genes.loaded().size();
        List<String> errors = GeneSpecLoader.loadAndRegister(dir);
        int added = Genes.loaded().size() - before;

        for (String error : errors) {
            HorseGenetics.LOGGER.error("[genes] {}", error);
        }
        if (added > 0) {
            StringBuilder names = new StringBuilder();
            for (SpecGene gene : Genes.loaded()) {
                names.append(names.isEmpty() ? "" : ", ").append(gene.key());
            }
            HorseGenetics.LOGGER.info("[genes] loaded {} gene(s) from {}: {}", added, dir, names);
            HorseGenetics.LOGGER.info("[genes] the genotype code now has {} segments", Genes.codeOrder().size());
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
            HorseGenetics.LOGGER.warn("[genes] could not prepare {}: {}", dir, e.toString());
        }
    }
}
