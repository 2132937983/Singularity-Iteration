package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.util.mio_icif_tags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.171: the one place that says what a wrench does to a block. Every SI wrench (the manual wrench, the electric wrench),
 * the wrench mining rules ({@code WrenchHarvest}) and other mods' wrenches ({@code ForeignWrenchDismantling}) ask here.
 *
 * <p>Before, each wrench kept its own copy of these rules and they drifted apart: the electric wrench never dismantled a block
 * without a facing (multiblock casings and ports, the reactor vessel, machine hulls, the reactor chamber), and with its "front
 * dismantle" setting off it dismantled nothing at all, though the setting promised sneak-dismantling.
 */
public final class WrenchRules {
    private WrenchRules() {}

    /** What a right-click with a wrench does to a block it can take apart. */
    public enum Action { DISMANTLE, ROTATE, ROTATE_OPPOSITE, NONE }

    /**
     * A block a wrench takes apart: one of {@code mio_icif:wrenchable}, the only tag the wrenches read (0.1.11.172). It holds
     * the SI machines, cables and pipes, storage boxes, multiblock casings and ports and machine hulls. {@code c:machine} still
     * marks what is a machine (its salvage, the settings its item carries), but no longer what a wrench takes apart, and
     * {@code c:wrench_can_damaged} is gone. Cables and pipes keep their own handling of a plain click (their faces).
     */
    public static boolean dismantlable(BlockState state) {
        return state.is(mio_icif_tags.WRENCHABLE);
    }

    /** The facing a wrench turns ({@code facing} or {@code horizontal_facing}), or {@code null} for a block without one. */
    @Nullable
    public static Direction facing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return null;
    }

    /**
     * What a right-click does to a {@link #dismantlable} block (a storage box is always dismantled, before this is asked).
     *
     * <p>With front dismantle (always for the manual wrench, a setting of the electric wrench, on by default): a block without a
     * facing is dismantled; a click on its front dismantles it; a click on another face turns it to face that way; a sneaking
     * click turns it to face away from the clicked face (a sneaking click on its front turns the front to the back). 0.1.11.172
     * made a sneaking click dismantle, as other mods' wrenches do; the user wanted the turn back (0.1.11.173).
     *
     * <p>Without front dismantle (the electric wrench's safety against misclicks): only a sneaking click dismantles; a plain
     * click turns the block to face the clicked face, and does nothing on its front or on a block without a facing.
     *
     * <p>Other mods' wrenches (Create's, for one) are not asked here: a sneaking click with one dismantles
     * ({@code ForeignWrenchDismantling}), their own mods' way.
     */
    public static Action action(BlockState state, Direction clicked, boolean sneaking, boolean frontDismantle) {
        Direction facing = facing(state);
        if (!frontDismantle) {
            if (sneaking) return Action.DISMANTLE;
            return facing == null || facing == clicked ? Action.NONE : Action.ROTATE;
        }
        if (facing == null) return Action.DISMANTLE;
        if (sneaking) return Action.ROTATE_OPPOSITE;
        return facing == clicked ? Action.DISMANTLE : Action.ROTATE;
    }
}
