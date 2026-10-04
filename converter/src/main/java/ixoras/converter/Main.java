package ixoras.converter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * ixoras world converter: moves a world saved under the old namespace to the new one, into a NEW folder.
 *
 * <pre>
 *   java -jar ixoras-converter.jar "&lt;world folder&gt;" [--dry-run] [--yes] [--packs &lt;pack&gt; ...]
 *                                  [--old horsegenetics] [--new ixoras_horses] [--self-test]
 * </pre>
 *
 * Exit codes: 0 done (or nothing to do), 1 bad arguments or refusal, 2 the conversion hit errors (the output
 * folder is left in place, marked CONVERSION-INCOMPLETE.txt; nothing is deleted).
 *
 * <p>WRITTEN, NEVER RUN. The first run belongs on a COPY of the live world (see tools/rename/runbook).
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.exit(run(args, System.out));
    }

    static int run(String[] args, PrintStream log) throws Exception {
        String oldNs = "horsegenetics";
        String newNs = "ixoras_horses";
        boolean dry = false;
        boolean yes = false;
        List<Path> packs = new ArrayList<>();
        Path world = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dry-run":
                    dry = true;
                    break;
                case "--yes":
                    yes = true;
                    break;
                case "--old":
                    oldNs = args[++i];
                    break;
                case "--new":
                    newNs = args[++i];
                    break;
                case "--packs":
                    while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        packs.add(Path.of(args[++i]));
                    }
                    break;
                case "--self-test":
                    return SelfTest.run(log);
                default:
                    if (args[i].startsWith("--") || world != null) {
                        log.println("unknown or repeated argument: " + args[i]);
                        return 1;
                    }
                    world = Path.of(args[i]);
            }
        }
        if (world == null && packs.isEmpty()) {
            log.println("usage: java -jar ixoras-converter.jar <world folder> [--dry-run] [--yes] [--packs <pack> ...]");
            return 1;
        }
        int code = 0;
        if (world != null) {
            code = convertWorld(world, oldNs, newNs, dry, log);
        }
        if (code == 0 && !packs.isEmpty()) {
            new PackConverter(oldNs, newNs, log).convertAll(packs, yes, dry, new Scanner(System.in));
        }
        return code;
    }

    static int convertWorld(Path world, String oldNs, String newNs, boolean dry, PrintStream log) throws IOException {
        if (!WorldConverter.looksLikeWorld(world)) {
            log.println("refusing: " + world + " has no level.dat, so it is not a Minecraft world folder.");
            return 1;
        }
        // Step 1: a scan of the input. Zero tokens and no old-named folders means there is nothing to do.
        WorldConverter scan = new WorldConverter(world, oldNs, newNs, true, log);
        long found = scan.run(null);
        if (!scan.errors.isEmpty()) {
            log.print(scan.report(null));
            log.println("refusing: the world could not be read cleanly (see ERRORS). Nothing was written.");
            return 2;
        }
        if (found == 0 && scan.renamedPaths.isEmpty()) {
            log.println("Nothing to convert: no " + oldNs + " tokens or folders found. Already converted, or this world never used the mod. Nothing written.");
            return 0;
        }
        if (dry) {
            log.print(scan.report(null));
            return 0;
        }
        Path out = WorldConverter.outputFor(world);
        if (Files.exists(out)) {
            log.println("refusing: " + out + " already exists. Remove or rename it yourself; this tool never deletes anything.");
            return 1;
        }
        long need = WorldConverter.neededBytes(world);
        long free = Files.getFileStore(out.getParent()).getUsableSpace();
        if (free < need) {
            log.println("refusing: needs about " + need + " bytes free beside the world, only " + free + " available. Nothing written.");
            return 1;
        }
        Map<String, String> before = WorldConverter.fingerprint(world);
        Files.createDirectories(out);
        WorldConverter conv = new WorldConverter(world, oldNs, newNs, false, log);
        conv.run(out);
        if (!conv.errors.isEmpty()) {
            Files.writeString(out.resolve("CONVERSION-INCOMPLETE.txt"), "This folder is a failed conversion. Do not load it.\n" + conv.report(out));
            log.print(conv.report(out));
            log.println("FAILED: the output is marked CONVERSION-INCOMPLETE.txt and the original is untouched.");
            return 2;
        }
        // Step 2: verify. The output must hold no old tokens, and the original must be byte-identical.
        WorldConverter verify = new WorldConverter(out, oldNs, newNs, true, log);
        long left = verify.run(null);
        Map<String, String> after = WorldConverter.fingerprint(world);
        boolean untouched = before.equals(after);
        String report = conv.report(out) + "verify: old tokens left in the output: " + left + ", old-named paths left: "
                + verify.renamedPaths.size() + ", original byte-identical: " + untouched + "\n";
        Files.writeString(out.resolveSibling(out.getFileName() + ".report.txt"), report);
        Files.writeString(out.resolve("converted-by-ixoras-converter.txt"),
                "Converted from the " + oldNs + " namespace to " + newNs + " by ixoras world converter " + WorldConverter.VERSION + ".\n");
        log.print(report);
        if (left != 0 || !verify.renamedPaths.isEmpty() || !untouched || !verify.errors.isEmpty()) {
            log.println("VERIFY FAILED: see the report above. Do not load the converted folder.");
            return 2;
        }
        log.println("Done. Load \"" + out.getFileName() + "\" with the new mod. The original was not changed.");
        return 0;
    }
}
