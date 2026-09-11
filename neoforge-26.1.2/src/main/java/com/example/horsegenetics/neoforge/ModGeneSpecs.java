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
 * ({@code wiki/gene-creator/}) writes - out of {@code .minecraft/phc/genes/}
 * and into the {@link Genes} registry.
 *
 * <p><b>Beside the breeds and the settings</b>, in the mod's one folder
 * ({@link ModBreedSpecs#ROOT}): {@code phc/genes/}, {@code phc/breeds/} and the
 * three {@code .toml} files are everything a player is told to touch. It was
 * {@code config/horsegenetics/genes/} through 0.5.000, and that folder is no
 * longer read - a gene left there is not loaded, and a saved horse's segment
 * for it is dropped on load like any unregistered gene's.
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

    /** Relative to the game directory - {@code .minecraft} for the vanilla launcher. */
    public static final String FOLDER = ModBreedSpecs.ROOT + "/genes";

    /** The drop-in folder on this machine. */
    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER);
    }

    private static final String README = """
            Horse Genetics - drop-in genes
            ==============================

            Every .json file in this folder is loaded as a gene when the game
            starts, in filename order. Wild horses will carry it, foals will
            inherit it, and it gets its own segment in the genotype code.

            Make them with the gene creator, which previews the gene on a 3D
            horse and writes the file for you:

              https://9-7-8.github.io/Procedural-Horse-Coats/wiki/gene-creator/

            This folder sits beside the mod's others in .minecraft/phc/:
            breeds/ for drop-in breeds, and breed-spawning.toml, server.toml
            and client.toml for the settings.

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
        Path dir = folder();
        ensureFolder(dir);

        // Reading this triggers Genes' class initialiser, which is what loads
        // the gene files shipped in the jar. So `shipped` is the count of those,
        // and everything loadAndRegister adds on top is a player's own.
        int shipped = Genes.loaded().size();
        List<String> errors = GeneSpecLoader.loadAndRegister(dir);
        int added = Genes.loaded().size() - shipped;

        for (String error : errors) {
            HorseGenetics.LOGGER.error("[genes] {}", error);
        }
        // Said every launch, not only when a drop-in was found. It is two lines,
        // and it is the first thing worth knowing from a log somebody pasted:
        // how many genes this build has, and therefore what shape a genotype
        // code from it is.
        HorseGenetics.LOGGER.info("[genes] {} shipped gene file(s), {} dropped in from {}",
                shipped, added, dir);
        HorseGenetics.LOGGER.info("[genes] the genotype code has {} segments", Genes.codeOrder().size());
        if (added > 0) {
            StringBuilder names = new StringBuilder();
            for (SpecGene gene : Genes.loaded()) {
                names.append(names.isEmpty() ? "" : ", ").append(gene.key());
            }
            HorseGenetics.LOGGER.info("[genes] data-driven genes: {}", names);
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
