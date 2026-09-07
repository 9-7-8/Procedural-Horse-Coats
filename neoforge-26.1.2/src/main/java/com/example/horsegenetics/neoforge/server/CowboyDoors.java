package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Opening and shutting doors for a man who is on a horse.
 *
 * <h2>Why this exists at all</h2>
 * A horse cannot open a door and a mounted rider is not steering, so without
 * this the cowboy would spend every night standing outside his own barn. And a
 * door opened <b>one leaf at a time</b> is no use to him either: a single open
 * door leaves a one-block gap, and a horse's hitbox will not fit through one.
 * Every method here therefore works on <b>every door in an area</b> rather than
 * on the one door somebody bumped into - which gets both leaves of a double
 * door, and all four of the barn's, for free and without having to work out
 * which leaf is paired with which.
 *
 * <h2>Only his own barn</h2>
 * {@code cowboy_barn.nbt} is 15x6x7 with the cowboy generated at its centre,
 * and has two double doors at each end. {@link #BARN_RADIUS} is a horizontal
 * half-span comfortably around that, whichever way the jigsaw rotated the
 * building - so "the barn's doors" is simply "the doors near home", and no
 * rotation maths is needed.
 *
 * <p>Nothing here touches a door anywhere else, and that is deliberate. An
 * earlier version opened whatever was in front of the horse, which does let him
 * through any door he meets - and also leaves a trail of open front doors
 * through a village he rides past every day, which is a good way to get its
 * inhabitants eaten. The barn is the only building he actually has to get into,
 * so it is the only one he is given the keys to.
 */
public final class CowboyDoors {

    /** Half-span of the box that counts as "the barn", around the cowboy's home block. */
    public static final int BARN_RADIUS = 8;

    /** How far below and above home the barn box reaches. */
    private static final int BARN_BELOW = 2;
    private static final int BARN_ABOVE = 5;

    private CowboyDoors() {
    }

    /** Open or shut every door in the barn, all four double doors at once. */
    public static void setBarnDoors(ServerLevel level, BlockPos home, boolean open, Entity opener) {
        setDoors(level, home, BARN_RADIUS, BARN_BELOW, BARN_ABOVE, open, opener);
    }

    /**
     * Is the barn actually shut? Every door near home closed, and at least one
     * door there to be closed.
     *
     * <p>This is what makes {@code CowboyRoutine.sheltered} mean something. Being
     * <i>inside</i> a building is only shelter if the building is shut: an open
     * doorway is not a wall, and a man standing in his own barn at noon with all
     * four doors folded back is as exposed as a man standing in the field. Asking
     * the world rather than trusting a flag also survives the cases a flag cannot
     * see - a player who opened them, a creeper who removed them, a reload that
     * lost the flag.
     *
     * <p>The "at least one" clause matters: a barn whose doors have been broken
     * off would otherwise pass vacuously, and report the safest possible state at
     * exactly the moment it is the least true.
     */
    public static boolean barnIsShut(ServerLevel level, BlockPos home) {
        boolean sawADoor = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -BARN_RADIUS; dx <= BARN_RADIUS; dx++) {
            for (int dy = -BARN_BELOW; dy <= BARN_ABOVE; dy++) {
                for (int dz = -BARN_RADIUS; dz <= BARN_RADIUS; dz++) {
                    cursor.set(home.getX() + dx, home.getY() + dy, home.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof DoorBlock door)) {
                        continue;
                    }
                    if (state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                        continue;
                    }
                    sawADoor = true;
                    if (door.isOpen(state)) {
                        return false;
                    }
                }
            }
        }
        return sawADoor;
    }

    private static void setDoors(ServerLevel level, BlockPos centre, int radius,
                                 int below, int above, boolean open, Entity opener) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -below; dy <= above; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof DoorBlock door)) {
                        continue;
                    }
                    // Only the lower half: setOpen updates both halves, and
                    // touching the upper one would just do the work twice.
                    if (state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                        continue;
                    }
                    if (door.isOpen(state) == open) {
                        continue;
                    }
                    door.setOpen(opener, level, state, cursor.immutable(), open);
                }
            }
        }
    }
}
