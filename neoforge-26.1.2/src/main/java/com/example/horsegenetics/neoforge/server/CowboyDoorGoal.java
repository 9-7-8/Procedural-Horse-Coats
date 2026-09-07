package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.jspecify.annotations.Nullable;

/**
 * Lets the {@link Cowboy} open a door he has walked into - <b>both leaves of it
 * if it is a double door</b>, and <b>only in daylight</b>.
 *
 * <h2>Why not {@code OpenDoorGoal}</h2>
 * Vanilla's goal opens the one door block the path runs through. The barn's
 * openings are <i>pairs</i> of doors - two leaves, same facing, opposite hinge -
 * and one leaf is a one-block gap. Minecraft's ground pathfinder rounds a mob up
 * to {@code floor(width + 1)} blocks across, so a one-block gap is not a gap at
 * all to a horse, and the whole point of the man walking to the barn is the
 * string of horses walking in behind him. So this opens the partner leaf too.
 *
 * <p>It also does not close anything. {@code OpenDoorGoal}'s close-behind is
 * wrong here twice over: the herd is following him and would be shut out, and
 * the barn doors standing open is what the doorway headroom in
 * {@code tools/barn/bake-barn.py} was cleared for.
 *
 * <h2>Daylight only</h2>
 * Two halves, and both are needed. {@link #canUse()} refuses after dark, and
 * {@link Cowboy} turns the navigator's own {@code canOpenDoors} off with it -
 * because that flag is what makes the pathfinder treat a shut door as a way
 * through. Leave it on with this goal switched off and he paths at a door he
 * will not open and stands there shoving it.
 *
 * <p>"Day" is {@link Level#isBrightOutside()}, the same test the gene effects
 * use for their {@code day} trigger, so the mod has one definition of daytime.
 * It reads sky darkening rather than the clock, so a thunderstorm counts as
 * night and he will wait it out - which is the behaviour a shared definition
 * buys, and is why this does not hand-roll a clock comparison instead.
 */
public final class CowboyDoorGoal extends DoorInteractGoal {

    /** The other leaf of a double door, or null if this one stands alone. */
    private @Nullable BlockPos partnerPos;

    public CowboyDoorGoal(Mob mob) {
        super(mob);
    }

    @Override
    public boolean canUse() {
        return this.mob.level().isBrightOutside() && super.canUse();
    }

    /**
     * One shot: open and let go. Nothing needs to be held open, because nothing
     * is ever closed again - see the class note. Re-firing next tick is free,
     * since {@link DoorBlock#setOpen} does nothing to a door already open.
     */
    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        this.partnerPos = partnerOf(this.doorPos);
        setOpen(true);
        if (this.partnerPos != null) {
            setOpen(this.partnerPos, true);
        }
    }

    /**
     * The other half of a double door: the neighbour to either side along the
     * wall that is a wooden door of the same facing and the opposite hinge.
     *
     * <p>Both sides are tried rather than working out which way the hinge points,
     * because the hinge tells you which way a leaf swings and not which side its
     * partner is on - and getting that backwards fails silently, as a barn door
     * that opens half way.
     */
    private @Nullable BlockPos partnerOf(BlockPos pos) {
        Level level = this.mob.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return null;
        }
        Direction facing = state.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        for (Direction side : new Direction[] {facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos neighbour = pos.relative(side);
            BlockState other = level.getBlockState(neighbour);
            if (DoorBlock.isWoodenDoor(other)
                    && other.getValue(DoorBlock.FACING) == facing
                    && other.getValue(DoorBlock.HINGE) != hinge) {
                return neighbour;
            }
        }
        return null;
    }

    /**
     * {@link DoorInteractGoal#setOpen} only ever touches {@code doorPos}, so the
     * partner leaf is swung by hand. The upper half follows on its own - a
     * {@link DoorBlock} copies {@code open} to its other half when its
     * neighbour changes.
     */
    private void setOpen(BlockPos pos, boolean open) {
        Level level = this.mob.level();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(this.mob, level, state, pos, open);
        }
    }
}
