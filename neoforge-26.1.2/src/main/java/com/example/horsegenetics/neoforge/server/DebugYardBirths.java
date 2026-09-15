package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.SpliceCategory;
import com.example.horsegenetics.common.genetics.SpliceSafety;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Severity;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.horsegenetics.neoforge.server.DebugTestYard.EAST_MIN;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AE;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AE_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AF;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AF_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AJ;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AJ_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AK;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.ROW_AK_D;
import static com.example.horsegenetics.neoforge.server.DebugTestYard.WEST_MIN;

/**
 * <b>Rows AE and AF-west: what a foal is handed at birth</b> - its dam's bond (a quarter of it), its breed label
 * (the cross table), its starburst dial (drifted, not re-rolled) and its name (gap 8's pattern). Every foal of these
 * pens is read the moment it joins the world, matched to its pen by the record's mother, logged with PASS or FAIL, and
 * taken away a few seconds later so the pens do not fill.
 *
 * <table>
 *   <tr><th>row</th><th>west</th><th>east</th></tr>
 *   <tr><td>AE</td><td>BOND; CROSS SAME</td><td>CROSS TWO; CROSS BACK</td></tr>
 *   <tr><td>AF</td><td>CROSS OUT; STARBURST DRIFT</td><td>(dhampir, {@link DebugYardDhampir})</td></tr>
 * </table>
 */
@EventBusSubscriber
final class DebugYardBirths {

    private DebugYardBirths() {
    }

    private static final String PLAIN = "horsegenetics.scn4a=N/N";

    /** One pen's pair, keyed by the dam's record id. {@code born[0]} counts its foals. */
    private record Pen(String name, String kind, UUID damEntity, UUID sireEntity, String expect, int[] born) {}

    private static final Map<UUID, Pen> BY_DAM_RECORD = new HashMap<>();
    private static final Set<UUID> SEEN = new HashSet<>();

    static void build(ServerLevel level, int gy, int cx, int mouthZ) {
        int west = cx + WEST_MIN;
        int east = cx + EAST_MIN + 1;
        BY_DAM_RECORD.clear();
        SEEN.clear();
        try {
            pair(level, gy, west, mouthZ + ROW_AE, ROW_AE_D, "BOND", "bond", PLAIN, null, null, "",
                    List.of("BOND", "a bond-80 mare:", "her foal starts at", "a quarter, 20"));
            pair(level, gy, west + 9, mouthZ + ROW_AE, ROW_AE_D, "CROSS SAME", "breed", PLAIN, "arabian", "arabian",
                    "arabian", List.of("CROSS SAME", "Arabian x Arabian:", "an Arabian foal", ""));
            pair(level, gy, east, mouthZ + ROW_AE, ROW_AE_D, "CROSS TWO", "breed", PLAIN, "arabian", "friesian",
                    "cross:arabian+friesian", List.of("CROSS TWO", "Arabian x Friesian:", "an Arabian x", "Friesian cross"));
            pair(level, gy, east + 9, mouthZ + ROW_AE, ROW_AE_D, "CROSS BACK", "breed", PLAIN, "cross:arabian+friesian",
                    "arabian", "cross:arabian+friesian", List.of("CROSS BACK", "cross x Arabian:", "the same cross", ""));
            pair(level, gy, west, mouthZ + ROW_AF, ROW_AF_D, "CROSS OUT", "breed", PLAIN, "cross:arabian+friesian",
                    "thoroughbred", "mixed", List.of("CROSS OUT", "cross x outsider", "(Thoroughbred):", "Mixed"));
            pair(level, gy, west + 9, mouthZ + ROW_AF, ROW_AF_D, "STARBURST DRIFT", "drift", "horsegenetics.starburst=C/C",
                    null, null, "", List.of("STARBURST DRIFT", "C/C x C/C: a foal's", "size and hue sit", "near its parents'"));
            // Rows AJ-AK: one mare per splice carrot, armed as if she had eaten it and re-armed after every foal.
            String[][] splices = {
                    {"SPLICE DILUTION", "gene_splice:dilution"}, {"SPLICE WHITE", "gene_splice:white"},
                    {"SPLICE MARKING", "gene_splice:marking"}, {"SPLICE PERFORMANCE", "gene_splice:performance"},
                    {"SPLICE MAGICAL", "gene_splice:magical"}, {"SPLICE UNTHEMED", "gene_splice"}};
            int[][] at = {{west, ROW_AJ, ROW_AJ_D}, {west + 9, ROW_AJ, ROW_AJ_D}, {east, ROW_AJ, ROW_AJ_D},
                    {east + 9, ROW_AJ, ROW_AJ_D}, {west, ROW_AK, ROW_AK_D}, {west + 9, ROW_AK, ROW_AK_D}};
            for (int i = 0; i < splices.length; i++) {
                pair(level, gy, at[i][0], mouthZ + at[i][1], at[i][2], splices[i][0], "splice", PLAIN, null, null,
                        splices[i][1], List.of(splices[i][0], "mare armed with", splices[i][1].replace("gene_splice", "splice"),
                                "new allele: in theme"));
            }
            ActionTrace.log("test yard", "birth pens built (rows AE-AF: bond, cross same/two/back/out, starburst drift;"
                    + " rows AJ-AK: the six splice carrots; every pen also logs foal names for gap 8)");
        } catch (RuntimeException e) {
            HorseGenetics.LOGGER.warn("[Debug] test yard: rows AE-AF (births) failed to build", e);
        }
    }

    private static void pair(ServerLevel level, int gy, int x0, int z0, int depth, String name, String kind, String code,
                             @Nullable String damBreed, @Nullable String sireBreed, String expect, List<String> sign) {
        DebugYardUnattended.pen(level, gy, x0, z0, 9, depth, name, Blocks.GRASS_BLOCK.defaultBlockState(), sign);
        Horse mare = DebugYardUnattended.horse(level, gy, x0 + 3.5, z0 + 6.5, Sex.FEMALE, code, true, name + " MARE");
        Horse stud = DebugYardUnattended.horse(level, gy, x0 + 5.5, z0 + 6.5, Sex.MALE, code, true, name + " STUD");
        if (mare == null || stud == null) {
            return;
        }
        breed(mare, damBreed, name + " MARE");
        breed(stud, sireBreed, name + " STUD");
        if (kind.equals("bond")) {
            mare.setData(ModAttachments.HORSE_CARE.get(), mare.getData(ModAttachments.HORSE_CARE.get()).withBond(80));
        }
        DebugYardFertility.inHeat(mare);
        if (kind.equals("splice")) {
            arm(mare, expect);
        }
        DebugWorldWatch.watchBreeding(name, DebugTestYard.box(x0, gy, z0, x0 + 9, gy + 3, z0 + depth));
        BY_DAM_RECORD.put(HorseRecords.of(mare).id(), new Pen(name, kind, mare.getUUID(), stud.getUUID(), expect, new int[1]));
    }

    private static void breed(Horse h, @Nullable String token, String label) {
        if (token == null) {
            return;
        }
        HorseRecords.apply(h, HorseRecords.of(h).withBreed(token));
        DebugTestYard.label(h, label);      // applying a record clears the label
    }

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (BY_DAM_RECORD.isEmpty() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Horse foal) || !foal.isBaby() || !HorseRecords.hasRealRecord(foal)) {
            return;
        }
        HorseRecord rec = HorseRecords.of(foal);
        Pen pen = rec.motherId().map(BY_DAM_RECORD::get).orElse(null);
        if (pen == null || !SEEN.add(foal.getUUID())) {
            return;
        }
        int n = ++pen.born()[0];
        Horse dam = level.getEntity(pen.damEntity()) instanceof Horse d ? d : null;
        Horse sire = level.getEntity(pen.sireEntity()) instanceof Horse s ? s : null;
        HorseRecord damRec = dam == null ? null : HorseRecords.of(dam);
        HorseRecord sireRec = sire == null ? null : HorseRecords.of(sire);
        String who = "foal " + n + " \"" + rec.displayName() + "\" of \"" + (damRec == null ? "?" : damRec.displayName())
                + "\" and \"" + (sireRec == null ? "?" : sireRec.displayName()) + "\"";
        String detail = switch (pen.kind()) {
            case "bond" -> {
                int fb = foal.getData(ModAttachments.HORSE_CARE.get()).bond();
                int db = dam == null ? -1 : dam.getData(ModAttachments.HORSE_CARE.get()).bond();
                yield "foal bond " + fb + ", dam bond " + db + ", expect " + (db / 4) + ": "
                        + (dam != null && fb == db / 4 ? "PASS" : "FAIL");
            }
            case "breed" -> {
                String got = rec.breed().orElse("feral_mixed");
                yield "foal breed " + got + " (" + rec.lineage().displayName() + "), expect " + pen.expect() + ": "
                        + (got.equals(pen.expect()) ? "PASS" : "FAIL");
            }
            case "splice" -> {
                if (dam != null) {
                    arm(dam, pen.expect());     // a conception used the carrot up; the next cover gets another
                }
                yield damRec == null || sireRec == null ? "a parent is gone - nothing to compare"
                        : splice(pen.expect(), damRec, sireRec, rec);
            }
            default -> damRec == null || sireRec == null ? "a parent is gone - no drift to read"
                    : drift(damRec.epigenomeCode(), sireRec.epigenomeCode(), rec.epigenomeCode());
        };
        ActionTrace.log("test yard", pen.name() + ": " + who + " - " + detail);
        DebugYardHerd.after(level, 100, foal::discard);     // read; out of the way before the next one
    }

    // ------------------------------------------------------------------
    // splice carrots
    // ------------------------------------------------------------------

    /** What feeding the carrot does to a horse, without the carrot: its token appended to the armed list. */
    private static void arm(Horse h, String token) {
        h.setData(ModAttachments.ARMED_CARROTS.get(), h.getData(ModAttachments.ARMED_CARROTS.get()).plus(List.of(token)));
    }

    /**
     * Verification &sect;0-AJ: "The spliced locus should be in the carrot's theme every time" and "No themed carrot may
     * ever produce a foal with a disorder". Nothing records which locus a splice chose, so the pen finds it the way
     * {@code SpliceOutcome.spliceReached} does: an allele on the foal that neither parent carries. Eye loci are skipped -
     * {@code Eyes.force} rewrites them after the draw and invents alleles with no carrot involved - and so are sex-linked
     * placeholders. A splice can also go unseen, when the foal draws a substitute copy a parent already had.
     */
    private static String splice(String token, HorseRecord dam, HorseRecord sire, HorseRecord foal) {
        SpliceCategory want = token.startsWith("gene_splice:")
                ? SpliceCategory.valueOf(token.substring("gene_splice:".length()).toUpperCase(java.util.Locale.ROOT)) : null;
        Genotype dg = dam.genotype();
        Genotype sg = sire.genotype();
        Genotype fg = foal.genotype();
        List<String> novel = new ArrayList<>();
        boolean allInTheme = true;
        for (Gene gene : Genes.codeOrder()) {
            // Both tests: the eye classes are not all in the eye package - the 09:26 run counted eye_sclera_left and
            // eye_sector_colour_left as new alleles, which Eyes.force can make with no carrot at all.
            if (gene.getClass().getPackageName().endsWith(".eye") || gene.key().startsWith("horsegenetics.eye_")) {
                continue;
            }
            AllelePair fp = fg.pair(gene);
            if (fp == null) {
                continue;
            }
            AllelePair dp = dg.pair(gene);
            AllelePair sp = sg.pair(gene);
            for (Allele a : new Allele[]{fp.first(), fp.second()}) {
                if (gene.isPlaceholder(a) || (dp != null && dp.has(a)) || (sp != null && sp.has(a))) {
                    continue;
                }
                SpliceCategory got = SpliceCategory.of(gene);
                boolean ok = want == null ? SpliceSafety.isSafe(gene) : got == want;
                allInTheme &= ok;
                novel.add(gene.key() + " " + a.token() + " (" + (got == null ? "no theme" : got.name().toLowerCase(java.util.Locale.ROOT))
                        + (ok ? "" : ", OUT OF THEME") + ")");
                break;
            }
        }
        List<String> disorders = new ArrayList<>();
        HorseTraits.resolve(fg, foal.epigenome(), true).conditions().stream()
                .filter(c -> c.severity() != Severity.INFORMATIONAL)
                .forEach(c -> disorders.add(c.name() + " [" + c.severity() + "]"));
        String verdict = novel.isEmpty() ? "no splice seen (a splice can hand down a copy a parent already had)"
                : allInTheme ? "PASS (in theme)" : "FAIL (out of theme)";
        return "new alleles: " + (novel.isEmpty() ? "none" : String.join(", ", novel)) + " | conditions above informational: "
                + (disorders.isEmpty() ? "none" : String.join(", ", disorders)) + " - " + verdict
                + (disorders.isEmpty() ? "" : "; A DISORDER - check whether it sits on the new locus");
    }

    // ------------------------------------------------------------------
    // starburst drift
    // ------------------------------------------------------------------

    private static final String STARBURST = "horsegenetics.starburst=";
    private static final Pattern VALUE = Pattern.compile("(?:^|[^A-Za-z])(p|size|hue):(-?[0-9]+(?:\\.[0-9]+)?)");

    /**
     * Each foal copy against the parent copy with the same priority (drift keeps priority, so that is the copy it came
     * from), with the size and hue it moved. EpiDrift's steps are tiny - 99% under 0.7% of a value's span - so anything
     * past 0.02 of size or 10 degrees of hue, or a copy that matches no parent, reads as a re-roll.
     */
    private static String drift(String damEpi, String sireEpi, String foalEpi) {
        String d = segment(damEpi);
        String s = segment(sireEpi);
        String f = segment(foalEpi);
        List<Map<String, Double>> parents = new ArrayList<>();
        for (String c : (d + "/" + s).split("/")) {
            parents.add(values(c));
        }
        StringBuilder sb = new StringBuilder("dam " + d + " | sire " + s + " | foal " + f + " |");
        boolean ok = true;
        for (String c : f.split("/")) {
            Map<String, Double> fv = values(c);
            Double p = fv.get("p");
            Map<String, Double> from = null;
            for (Map<String, Double> pv : parents) {
                if (p != null && pv.get("p") != null && Math.abs(pv.get("p") - p) < 1e-9) {
                    from = pv;
                    break;
                }
            }
            if (from == null) {
                sb.append(" a copy matches no parent's priority;");
                ok = false;
                continue;
            }
            double ds = delta(fv, from, "size");
            double dh = delta(fv, from, "hue");
            sb.append(String.format(" size moved %+.4f, hue %+.2f;", ds, dh));
            if (Math.abs(ds) > 0.02 || Math.abs(dh) > 10.0) {
                ok = false;
            }
        }
        return sb + (ok ? " PASS (drifted, not re-rolled)" : " FAIL (re-rolled, or a copy could not be matched)");
    }

    private static String segment(String epi) {
        for (String part : epi.split(";")) {
            if (part.startsWith(STARBURST)) {
                return part.substring(STARBURST.length());
            }
        }
        return "?";
    }

    private static Map<String, Double> values(String copy) {
        Map<String, Double> out = new LinkedHashMap<>();
        Matcher m = VALUE.matcher(copy);
        while (m.find()) {
            out.putIfAbsent(m.group(1), Double.parseDouble(m.group(2)));
        }
        return out;
    }

    private static double delta(Map<String, Double> foal, Map<String, Double> parent, String key) {
        Double a = foal.get(key);
        Double b = parent.get(key);
        return a == null || b == null ? Double.NaN : a - b;
    }
}
