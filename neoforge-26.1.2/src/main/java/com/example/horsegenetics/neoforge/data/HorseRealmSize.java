package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>How big the horse realm is, and why.</b> The field is a <b>circle centred
 * on the origin</b> that starts at {@value #START_RADIUS} blocks and
 * <b>grows with the number of horses living in it</b>, never shrinking.
 *
 * <h2>Why a circle that grows, instead of a field that is simply large</h2>
 * The realm used to be sixteen thousand blocks square, and the problem with that
 * was not that it was too small. It was that a horse turned out into it could
 * walk far enough that nobody would ever find it again - the field was
 * <i>bounded</i>, which was the promise, but not <b>small enough to search</b>,
 * which is what the promise was for. A circle sized to its own population keeps
 * the herd within walking distance of the way in, and only gets bigger when
 * there are genuinely enough animals to need the room. (Owner's call.)
 *
 * <p>A circle rather than a square because the field has one entrance at the
 * centre now, and a circle is the shape where <i>everywhere</i> is the same
 * distance-ish from it. The corners of a square are 40% further out than the
 * edges, and the corners are exactly where a lost horse ends up.
 *
 * <h2>The density, and the arithmetic nobody should have to redo</h2>
 * {@value #BLOCKS_PER_HORSE} square blocks per horse - ten chunks each. So the
 * radius needed for {@code n} horses is {@code sqrt(n * BLOCKS_PER_HORSE / PI)},
 * and the starting circle covers about 8.04 million square blocks, which is
 * <b>room for roughly 3,100 horses before it grows at all</b>.
 *
 * <p><b>That number is the whole reason this constant is what it is.</b> The
 * brief said one horse per ten <i>blocks</i>, which works out at eight hundred
 * thousand horses in the starting circle - a threshold no server reaches, making
 * the growth dead code. Ten chunks each puts the first growth somewhere a real
 * stud farm can actually arrive at. See the realm's page.
 *
 * <h2>It can never shrink, and that is load-bearing</h2>
 * {@link #radius} only ever increases. A field that shrank would put its own
 * perimeter wall <i>inside itself</i>, with horses on the wrong side of it and
 * no way to walk back - and it would do that at the exact moment somebody
 * removed horses, which is the moment they are least expecting the world to move.
 *
 * <p>Growing does move the wall, though, and the old ring has to be taken down
 * or it becomes an invisible fence in the middle of the field. {@link #retired}
 * is the list of radii whose rings are still standing somewhere;
 * {@code HorseRealmTerrain} clears them as those chunks load and they leave the
 * list only when the whole ring has been walked. Growth is rare, so this list is
 * short.
 *
 * <h2>The census counts horses, not sightings</h2>
 * {@link #horses} is a set of UUIDs rather than a number, because the thing that
 * maintains it is a per-horse tick that cannot know whether it has already
 * counted itself. It cannot use {@code HorseWhereabouts}: that only records
 * <b>tamed</b> horses, and the realm's population is precisely the ones that are
 * not.
 *
 * <p>It is <b>self-correcting rather than hooked</b>, the same shape as
 * {@code HorseRealmFeral}: a horse ticking in the realm adds itself, a horse
 * ticking anywhere else removes itself. No exit needs a hook, and a horse in an
 * unloaded chunk keeps its place in the count - which is right, since it is
 * still out there taking up room.
 *
 * <h2>It also remembers where each one was standing</h2>
 * Added 2026-09-26, and it turns the census from a number into an <b>index</b>.
 * The realm's table is meant to list every horse in the field and let a player
 * buy any of them, and most of the field is unloaded most of the time - so both
 * halves need to reach a horse no chunk is holding. The row itself comes from
 * the ancestry database, which knows every horse that ever existed; what it
 * cannot say is <i>where</i>, and without that {@code RealmClaim} has nothing to
 * load in order to fetch the animal.
 *
 * <p>A position is exact rather than stale, which is the part that makes this
 * work at all: an unloaded entity does not move. The last tick before it
 * unloaded is where it still is, however long ago that was.
 *
 * <p>One known drift, in the safe direction: a horse taken out of the realm
 * <i>inside a stasis chamber</i> never ticks anywhere again, so it stays
 * counted. That makes the field slightly larger than it needs to be, and the
 * field never shrinks anyway.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class HorseRealmSize extends SavedData {

    /** The starting radius, in blocks: 100 chunks. */
    public static final int START_RADIUS = 1600;

    /** Square blocks of field per horse - ten chunks each. See the class doc. */
    public static final int BLOCKS_PER_HORSE = 2560;

    /**
     * One horse in the field, and the block it was last seen standing on.
     * {@code at} is only ever read by {@code RealmClaim}, to know which chunk to
     * pull in when somebody buys a horse out of country nobody has loaded.
     */
    public record Resident(UUID horse, BlockPos at) {

        public static final Codec<Resident> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Resident::horse),
                BlockPos.CODEC.fieldOf("at").forGetter(Resident::at)
        ).apply(i, Resident::new));
    }

    private int radius = START_RADIUS;
    private final List<Integer> retired = new ArrayList<>();

    /**
     * Insertion-ordered on purpose: it is the order the realm table arrives in,
     * and an order that wandered between two refreshes would reshuffle a list
     * somebody is reading. Oldest resident first, which is also the stablest
     * thing available - nothing about a wild horse changes to move it.
     */
    private final Map<UUID, BlockPos> horses = new LinkedHashMap<>();

    public static final Codec<HorseRealmSize> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("radius", START_RADIUS).forGetter(HorseRealmSize::radius),
            Codec.INT.listOf().optionalFieldOf("retired", List.of()).forGetter(s -> s.retired),
            // "residents", not the old "horses": the field changed shape from a
            // bare UUID list to UUID-plus-position, and a new name means an
            // existing save loads CLEANLY with an empty census rather than
            // failing to parse the file that also holds the field's radius.
            // Losing the radius would put the wall inside the field.
            //
            // The census does then have to be rebuilt, and it rebuilds itself:
            // HorseRealmCensus enrols any horse that ticks in the realm. That
            // covers every horse somebody loads, and only that - a horse in
            // country nobody walks back into stays missing from the table until
            // they do. One lap of the field fixes it.
            Resident.CODEC.listOf().optionalFieldOf("residents", List.of())
                    .forGetter(HorseRealmSize::residents)
    ).apply(i, HorseRealmSize::new));

    public static final SavedDataType<HorseRealmSize> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_realm_size"),
            HorseRealmSize::new,
            CODEC);

    private HorseRealmSize() {
    }

    private HorseRealmSize(int radius, List<Integer> retired, List<Resident> horses) {
        this.radius = Math.max(START_RADIUS, radius);
        this.retired.addAll(retired);
        for (Resident resident : horses) {
            this.horses.put(resident.horse(), resident.at());
        }
    }

    public static HorseRealmSize get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    /** The field's radius in blocks. Everything inside is field; outside is wall. */
    public int radius() {
        return radius;
    }

    /** How many horses the census believes are living in the realm. */
    public int horseCount() {
        return horses.size();
    }

    /** Every horse in the field and where it was last standing, oldest first. */
    public List<Resident> residents() {
        List<Resident> out = new ArrayList<>(horses.size());
        for (Map.Entry<UUID, BlockPos> entry : horses.entrySet()) {
            out.add(new Resident(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    /** Where this horse was last seen standing, if the census has it. */
    public Optional<BlockPos> where(UUID horse) {
        return Optional.ofNullable(horses.get(horse));
    }

    /** Rings that are still standing and should not be - see the class doc. */
    public List<Integer> retiredRings() {
        return List.copyOf(retired);
    }

    /** A ring has been taken down everywhere it could be. */
    public void ringCleared(int oldRadius) {
        if (retired.remove(Integer.valueOf(oldRadius))) {
            setDirty();
        }
    }

    // ------------------------------------------------------------------
    // The census
    // ------------------------------------------------------------------

    /**
     * This horse is in the realm, standing here. Returns true if the horse
     * itself is news - a horse that has merely <i>moved</i> is not.
     *
     * <p>The position is written every time regardless, because it costs a map
     * put and being wrong about it costs a claim. {@link #setDirty} is only
     * raised for a genuinely new resident or a real move, so a field of
     * stationary horses does not re-save the whole census every scan.
     */
    public boolean note(UUID horse, BlockPos at) {
        BlockPos was = horses.put(horse, at);
        if (was == null) {
            setDirty();
            return true;
        }
        if (!was.equals(at)) {
            setDirty();
        }
        return false;
    }

    /** This horse is somewhere else now. Returns true if that is news. */
    public boolean forget(UUID horse) {
        if (horses.remove(horse) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Growing
    // ------------------------------------------------------------------

    /**
     * The radius {@code count} horses need, never below {@link #START_RADIUS}.
     * {@code area = PI * r * r >= count * BLOCKS_PER_HORSE}.
     */
    public static int radiusFor(int count) {
        if (count <= 0) {
            return START_RADIUS;
        }
        double needed = Math.sqrt((double) count * BLOCKS_PER_HORSE / Math.PI);
        return Math.max(START_RADIUS, (int) Math.ceil(needed));
    }

    /**
     * Grow to fit the census, if it has outgrown the field.
     *
     * @return the old radius if the field grew, or {@code -1} if it did not -
     *         so the caller knows which ring has just become rubbish
     */
    public int growToFit() {
        int wanted = radiusFor(horses.size());
        if (wanted <= radius) {
            return -1;
        }
        int was = radius;
        radius = wanted;
        // The old wall is now standing inside the field. Somebody has to go and
        // take it down; HorseRealmTerrain does, as those chunks load.
        if (!retired.contains(was)) {
            retired.add(was);
        }
        setDirty();
        HorseGenetics.LOGGER.info("[realm] {} horses need {} blocks of radius - the field grows from {}",
                horses.size(), wanted, was);
        return was;
    }
}
