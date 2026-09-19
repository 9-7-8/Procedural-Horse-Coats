package com.example.horsegenetics.common.cart;

import com.example.horsegenetics.common.progress.ProgressTask;

/**
 * <b>The vehicles a horse can be hitched to, and how hard each one is to
 * pull.</b>
 *
 * <p>{@link #load()} is the only number here, and it is deliberately the only
 * thing that distinguishes one vehicle from another as far as the draught model
 * is concerned. Everything else about a cart - its inventory, its field work,
 * how many people sit on it - lives in the NeoForge module; this enum exists so
 * that {@link CartDraft} can be a pure function and a unit test can ask "how
 * fast does <i>this</i> horse pull <i>that</i> wagon" without a running game.
 *
 * <h2>Reading a load</h2>
 * A load is not a mass. It is the ratio of the work the vehicle asks for to the
 * work an ordinary horse ({@link com.example.horsegenetics.common.trait.HorseTraits#BASE_PULL},
 * moving at {@link com.example.horsegenetics.common.trait.HorseTraits#BASE_SPEED})
 * can supply. So {@code 0.55} on the wagon means an ordinary horse spends
 * rather more than a third of itself just moving the thing; see
 * {@link CartDraft} for what that does to its speed.
 *
 * <p>{@link #load()} is the <b>empty</b> vehicle - the tare. What is in it is
 * {@link #cargoShare()}, and the two meet in {@link CartDraft#loaded}.
 *
 * <p>The ordering is the order the loads run in, heaviest first, which is also
 * the order the creative tab lists them in. Keep it that way - it is the only
 * place a reader can see the whole ladder at once.
 *
 * <p>The hand cart is not here. Upstream's hand cart is pulled by a player on
 * foot and never touches a horse, so it was not ported; there is nothing for a
 * draught model to say about it.
 */
public enum CartKind {

    /**
     * The covered wagon. Seats riders, carries the most, and one passenger
     * drives the horse from the box seat. The heaviest thing a horse pulls -
     * and the one whose load grows the most, since three chests and twelve rows
     * of cargo can be bolted onto it.
     */
    WAGON("wagon", 0.55, 0.60),

    /**
     * The plow. Lighter than the wagon as a vehicle, but it is dragging a
     * blade through soil rather than rolling on top of it, which is why it
     * sits second rather than near the bottom with the other implements.
     */
    PLOW("plow", 0.45, 0.0),

    /** The reaper. A cutting bar in the crop, so heavier than the seed drill. */
    REAPER("reaper", 0.40, 0.0),

    /** The seed drill. It only has to open the ground, not turn it. */
    SEED_DRILL("seed_drill", 0.35, 0.0),

    /** The supply cart. A chest on two wheels, and it is felt when the chest is full. */
    SUPPLY_CART("supply_cart", 0.30, 0.50),

    /** The animal cart. Two seats, and two cows are not two empty seats. */
    ANIMAL_CART("animal_cart", 0.25, 0.50);

    /**
     * The load a stationary machine puts on a horse - a treadmill, a mill, a
     * winch. Not a cart, so not an enum constant, but it is the same kind of
     * number and it belongs beside the others rather than buried in the compat
     * layer that reads it. See {@link CartDraft#workRate}.
     */
    public static final double MACHINE_LOAD = 0.50;

    private final String id;
    private final double load;
    private final double cargoShare;

    CartKind(final String id, final double load, final double cargoShare) {
        this.id = id;
        this.load = load;
        this.cargoShare = cargoShare;
    }

    /**
     * The registry path of this vehicle's entity and the suffix of its items -
     * {@code oak_wagon}, {@code spruce_plow}. Lower snake case, and the string
     * the NeoForge module registers under, so renaming one here renames the
     * entity.
     */
    public String id() {
        return this.id;
    }

    /**
     * How much of an ordinary horse this vehicle uses up <b>empty</b>. See the
     * class note, and {@link #cargoShare()} for what filling it adds.
     */
    public double load() {
        return this.load;
    }

    /**
     * <b>How much heavier a full one is than an empty one</b>, as a fraction of
     * {@link #load()}. A full wagon asks {@code 1 + 0.60} times what a bare one
     * does; see {@link CartDraft#loaded}.
     *
     * <p><b>Zero on all three field implements</b>, and that is the interesting
     * entry. A plow's load is the blade in the soil, not the three tools racked
     * on it - a plow does not become half again as hard to drag because you
     * gave it a second hoe, and a player who has to choose between fitting the
     * tool and pulling the plow is choosing between a working plow and a useless
     * one. The carrying vehicles are the ones where a full load is a real
     * decision, so they are the ones that charge for it.
     */
    public double cargoShare() {
        return this.cargoShare;
    }

    /**
     * The Getting Started task this vehicle finishes when it is actually
     * driven - one sub-chapter of the Farming and haulage chapter per vehicle.
     *
     * <p>A {@code switch} rather than an eighth constructor argument, so that
     * {@code common/cart} does not have to hold a reference to
     * {@code common/progress} in every constant. It is exhaustive, so adding a
     * vehicle without adding its task will not compile - which is the point.
     */
    public ProgressTask driveTask() {
        return switch (this) {
            case WAGON -> ProgressTask.DRIVE_WAGON;
            case PLOW -> ProgressTask.DRIVE_PLOW;
            case REAPER -> ProgressTask.DRIVE_REAPER;
            case SEED_DRILL -> ProgressTask.DRIVE_SEED_DRILL;
            case SUPPLY_CART -> ProgressTask.DRIVE_SUPPLY_CART;
            case ANIMAL_CART -> ProgressTask.DRIVE_ANIMAL_CART;
        };
    }

    /** The constant whose {@link #id()} is {@code id}, or null if there is none. */
    public static CartKind byId(final String id) {
        for (final CartKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
