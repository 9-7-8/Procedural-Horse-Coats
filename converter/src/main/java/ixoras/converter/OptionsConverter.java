package ixoras.converter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A player's key bindings live in .minecraft/options.txt under the translation key of each binding
 * ("key_key.horsegenetics.blow_whistles:key.keyboard.k"), so the rename resets them all. This rewrites that one small
 * file. Because it IS the player's live file, it first writes a copy "options.txt.before-ixoras" beside it (never
 * overwritten if it already exists), then replaces options.txt. The caller must have asked the player first.
 *
 * <p>UNVERIFIED against 26.1.2: that key bindings are still saved as "key_<translation key>:<key>" lines; the rewrite
 * is the same token rule as everywhere, so it is correct whatever the line shape, but it is only worth doing if the
 * game still stores bindings by that key.
 */
final class OptionsConverter {
    private OptionsConverter() {
    }

    /** Returns the number of tokens rewritten (0 means the file was left exactly as it was). */
    static long convert(Path options, String oldNs, String newNs, PrintStream log) throws IOException {
        if (!Files.isRegularFile(options)) {
            throw new IOException("not a file: " + options);
        }
        byte[] raw = Files.readAllBytes(options);
        String text = new String(raw, StandardCharsets.UTF_8);
        Rewriter rw = new Rewriter(oldNs, newNs);
        String out = rw.rewrite(text);
        if (rw.count == 0) {
            log.println("options.txt: no " + oldNs + " key bindings found; left alone.");
            return 0;
        }
        Path backup = options.resolveSibling(options.getFileName() + ".before-ixoras");
        if (!Files.exists(backup)) {
            Files.copy(options, backup, StandardCopyOption.COPY_ATTRIBUTES);
        }
        Files.write(options, out.getBytes(StandardCharsets.UTF_8));
        log.println("options.txt: " + rw.count + " key binding(s) moved to " + newNs + ". Your original is kept as "
                + backup.getFileName() + ".");
        return rw.count;
    }
}
