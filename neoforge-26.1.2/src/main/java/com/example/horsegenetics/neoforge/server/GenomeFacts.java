package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.HorseDiet;
import com.example.horsegenetics.common.genetics.genes.PassificationGene;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Facts about a horse's genome, parsed once per genome rather than once per tick</b> (2026-10-02, widened 2026-10-06).
 *
 * <p>The stress run's profile put {@code LycanthropyHandler.onTick} at 12.9% of all server time and
 * {@code SunSensitivityHandler.onTick} at 7.4% with a thousand ordinary horses - neither gene carried by any of them.
 * Both parsed the full genetic code (and lycanthropy the full epigenome) of every horse on every check interval,
 * only to find the locus wild-type. These facts are what those checks need first; each is read off one parse, kept
 * against the codes it came from, and re-derived only when a code changes.
 *
 * <p>The lag audit (#201) found the same parse in the diet, favourite-food and passification lookups, run per tick
 * by {@code CrouchFeedGoal}, per scan by {@code HorseCareHandler} and per search by {@code HungerFoodGoal}, and lazily
 * by five goals on every horse's first tick. They all read from here now: one genotype and one epigenome parse per
 * horse per genome, whoever asks first.
 *
 * <p><b>Eviction</b> (#200): an entry leaves when its horse leaves the level, and everything goes on server stop. The
 * size cap is only a backstop now; it used to be the only eviction, and clearing the whole map made every loaded
 * horse re-parse on the same tick.
 *
 * <p><b>Fast hit:</b> a reloaded horse's record holds new String objects equal to the cached ones, and
 * {@code equals} on two distinct 10-20 KB strings walks them every call. A content-equal hit is re-stored holding
 * the record's own strings, so the next check is the identity test.
 */
@EventBusSubscriber
final class GenomeFacts {

    private GenomeFacts() {
    }

    /** Backstop only: entries are evicted on leave. Far above any loaded-horse count a server reaches. */
    private static final int CAP = 16384;

    /**
     * What the checks need; {@code code} and {@code epigenome} are the codes they were read from. {@code lethal}
     * is filled on first ask: it needs the full trait resolution, which only the newborn check pays for.
     */
    private static final class Facts {
        final String code;
        final String epigenome;
        final boolean lycanCarrier;
        final boolean sunSensitive;
        final HorseDiet diet;
        final String favourite;
        final List<PassificationGene.Route> routes;
        volatile Boolean lethal;

        Facts(String code, String epigenome, boolean lycanCarrier, boolean sunSensitive, HorseDiet diet,
              String favourite, List<PassificationGene.Route> routes, Boolean lethal) {
            this.code = code;
            this.epigenome = epigenome;
            this.lycanCarrier = lycanCarrier;
            this.sunSensitive = sunSensitive;
            this.diet = diet;
            this.favourite = favourite;
            this.routes = routes;
            this.lethal = lethal;
        }

        Facts restrung(String code, String epigenome) {
            return new Facts(code, epigenome, lycanCarrier, sunSensitive, diet, favourite, routes, lethal);
        }
    }

    private static final Facts NONE = new Facts("", "", false, false, HorseDiet.NORMAL, null, List.of(),
            Boolean.FALSE);

    private static final Map<UUID, Facts> CACHE = new ConcurrentHashMap<>();

    /** Does this horse carry any lycanthropy allele at all? A wild-type horse can never shift. */
    static boolean lycanCarrier(Horse horse) {
        return of(horse).lycanCarrier;
    }

    /** Does daylight burn this horse? */
    static boolean sunSensitive(Horse horse) {
        return of(horse).sunSensitive;
    }

    /** This horse's diet, or {@link HorseDiet#NORMAL} without a real record or for codes that do not parse. */
    static HorseDiet diet(Horse horse) {
        return of(horse).diet;
    }

    /** The food-preference locus's favourite item id, or null. */
    static String favourite(Horse horse) {
        return of(horse).favourite;
    }

    /** Every passification route into this horse, or an empty list. */
    static List<PassificationGene.Route> passificationRoutes(Horse horse) {
        return of(horse).routes;
    }

    /** Is this foal one that dies at birth? Keyed on both codes, since traits read the epigenome too. */
    static boolean lethalAtBirth(Horse horse, HorseRecord record) {
        Facts f = of(horse, record);
        Boolean known = f.lethal;
        if (known != null) {
            return known;
        }
        boolean lethal;
        try {
            lethal = HorseRecords.traitsOf(record).viability()
                    == com.example.horsegenetics.common.trait.Viability.LETHAL_AT_BIRTH;
        } catch (RuntimeException bad) {
            lethal = false;
        }
        f.lethal = lethal;
        return lethal;
    }

    /** Drop one horse's facts - it has left the level. */
    static void forget(UUID id) {
        CACHE.remove(id);
    }

    @SubscribeEvent
    static void onEntityLeave(EntityLeaveLevelEvent event) {
        // Server side only: the client fires this too, and in singleplayer the map is shared.
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Horse horse) {
            forget(horse.getUUID());
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        CACHE.clear();
    }

    private static Facts of(Horse horse) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return NONE;
        }
        return of(horse, HorseRecords.of(horse));
    }

    private static Facts of(Horse horse, HorseRecord record) {
        String code = record.geneticCode();
        String epi = record.epigenomeCode();
        UUID id = horse.getUUID();
        Facts f = CACHE.get(id);
        if (f != null) {
            if (f.code == code && f.epigenome == epi) {
                return f;
            }
            if (f.code.equals(code) && f.epigenome.equals(epi)) {
                f = f.restrung(code, epi);
                CACHE.put(id, f);
                return f;
            }
        }
        f = derive(code, epi);
        if (CACHE.size() > CAP) {
            CACHE.clear();
        }
        CACHE.put(id, f);
        return f;
    }

    private static Facts derive(String code, String epi) {
        Genotype gt;
        try {
            gt = Genotype.parse(code);
        } catch (RuntimeException bad) {
            return new Facts(code, epi, false, false, HorseDiet.NORMAL, null, List.of(), null);
        }
        boolean lycan = false;
        boolean sun = false;
        String favourite = null;
        try {
            lycan = !wild(Genes.LYCAN, gt.pair(Genes.LYCAN));
            sun = Genes.SUN_SENSITIVITY.isSensitive(gt.pair(Genes.SUN_SENSITIVITY));
            favourite = Genes.FOOD_PREFERENCE.favouriteOf(gt.pair(Genes.FOOD_PREFERENCE));
        } catch (RuntimeException bad) {
            // each answer falls back to "nothing special", as the per-call versions did
        }
        HorseDiet diet = HorseDiet.NORMAL;
        List<PassificationGene.Route> routes = List.of();
        try {
            Epigenome epigenome = Epigenome.parse(epi);
            diet = HorseDiet.resolve(gt, epigenome);
            routes = Genes.PASSIFICATION.routesOf(gt.pair(Genes.PASSIFICATION),
                    GeneEpigenetics.forGene(Genes.PASSIFICATION, gt, epigenome));
        } catch (RuntimeException bad) {
            // an unparseable epigenome: a normal diet and no routes, as before
        }
        return new Facts(code, epi, lycan, sun, diet, favourite, routes, null);
    }

    private static boolean wild(Gene gene, AllelePair pair) {
        return pair == null || (pair.first() == gene.defaultAllele() && pair.second() == gene.defaultAllele());
    }
}
