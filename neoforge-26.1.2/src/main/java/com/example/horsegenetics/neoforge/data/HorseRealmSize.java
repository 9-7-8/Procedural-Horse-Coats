package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private int radius = START_RADIUS;
    private final List<Integer> retired = new ArrayList<>();
    private final Set<UUID> horses = new LinkedHashSet<>();

    public static final Codec<HorseRealmSize> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("radius", START_RADIUS).forGetter(HorseRealmSize::radius),
            Codec.INT.listOf().optionalFieldOf("retired", List.of()).forGetter(s -> s.retired),
            UUIDUtil.CODEC.listOf().optionalFieldOf("horses", List.of())
                    .forGetter(s -> List.copyOf(s.horses))
    ).apply(i, HorseRealmSize::new));

    public static final SavedDataType<HorseRealmSize> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_realm_size"),
            HorseRealmSize::new,
            CODEC);

    private HorseRealmSize() {
    }

    private HorseRealmSize(int radius, List<Integer> retired, List<UUID> horses) {
        this.radius = Math.max(START_RADIUS, radius);
        this.retired.addAll(retired);
        this.horses.addAll(horses);
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

    /** This horse is in the realm. Returns true if that is news. */
    public boolean note(UUID horse) {
        if (horses.add(horse)) {
            setDirty();
            return true;
        }
        return false;
    }

    /** This horse is somewhere else now. */
    public void forget(UUID horse) {
        if (horses.remove(horse)) {
            setDirty();
        }
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
