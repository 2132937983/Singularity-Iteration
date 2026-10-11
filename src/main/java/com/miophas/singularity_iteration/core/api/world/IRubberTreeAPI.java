package com.miophas.singularity_iteration.core.api.world;

import com.miophas.singularity_iteration.core.api.block.IRubberWood;
import com.miophas.singularity_iteration.core.api.item.ITreeTapItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rubber tree API.
 *
 * <p>This API gives addon mods access to the checks and harvest logic of the mio_icif rubber tree (resin harvest) system. Addon mods can use it to:
 * <ul>
 *   <li>identify the tree taps and rubber wood of this mod;</li>
 *   <li>use the same harvest logic in their own block or item interactions, without a dependency on specific block or item classes;</li>
 *   <li>add custom logs to the system through {@link IRubberWood}, and add custom taps through
 *       {@link ITreeTapItem}.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * IRubberTreeAPI rubber = MioIcifAPI.instance().getRubberTreeAPI();
 *
 * // Check whether the held item is a tree tap
 * if (rubber.isTreeTap(stack) && rubber.canHarvest(state)) {
 *     RubberHarvestResult result = rubber.tryHarvest(level, pos, state, player, hand);
 * }
 * }</pre>
 *
 * <p>All methods except {@link #tryHarvest}, {@link #tryRegrowResin} and {@link #dropResin} are pure queries.
 * They do not change the world. The methods that change the world have an effect only on the server side.
 */
public interface IRubberTreeAPI {

    // ========== Resin item ==========

    /**
     * Returns the resin (raw rubber) item.
     *
     * @return the resin item
     */
    Item getResinItem();

    /**
     * Makes an item stack with the given number of resin items.
     *
     * @param count the stack size
     * @return the resin item stack
     */
    ItemStack createResinStack(int count);

    // ========== Tree tap checks ==========

    /**
     * Returns true if the item stack is a tree tap (it implements {@link ITreeTapItem}).
     *
     * @param stack the item stack
     * @return true if the stack is a tree tap
     */
    boolean isTreeTap(ItemStack stack);

    /**
     * Returns true if the item stack is an electric tree tap (it implements {@link ITreeTapItem} and is an electric tool).
     *
     * @param stack the item stack
     * @return true if the stack is an electric tree tap
     */
    boolean isElectricTreeTap(ItemStack stack);

    // ========== Rubber wood checks ==========

    /**
     * Returns true if the block state is rubber wood (it implements {@link IRubberWood}).
     *
     * @param state the block state
     * @return true if the block is rubber wood
     */
    boolean isRubberWood(BlockState state);

    /**
     * Returns true if the rubber wood now contains resin that a player can harvest.
     *
     * @param state the block state
     * @return true if the block contains resin
     */
    boolean hasResin(BlockState state);

    /**
     * Returns true if the rubber wood meets the harvest conditions (for example, it has a tap hole).
     *
     * @param state the block state
     * @return true if the block meets the harvest conditions
     */
    boolean isTappable(BlockState state);

    /**
     * Returns true if a player can harvest the rubber wood now (it meets the harvest conditions and contains resin).
     *
     * @param state the block state
     * @return true if the block can be harvested
     */
    boolean canHarvest(BlockState state);

    /**
     * Returns true if the given tool can harvest this rubber wood.
     *
     * <p>This is true when the tool is a tree tap, the block can be harvested, and (for an electric tool) the tool has sufficient energy.
     *
     * @param state the block state
     * @param tool  the tool item stack
     * @return true if the tool can harvest the block
     */
    boolean canUseTapOn(BlockState state, ItemStack tool);

    // ========== Harvest and regrowth ==========

    /**
     * Tries to harvest resin from the rubber wood.
     *
     * <p>This method runs only on the server side and only when the conditions are met. It uses tap durability (or electric energy),
     * drops resin, and sets the block to the state without resin.
     *
     * @param level  the world
     * @param pos    the block position
     * @param state  the block state
     * @param player the player
     * @param hand   the hand that interacts
     * @return the harvest result; {@link RubberHarvestResult#NONE} if no harvest occurred
     */
    RubberHarvestResult tryHarvest(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand);

    /**
     * Calculates the number of resin items from one harvest. It does not change the world.
     *
     * <p>The {@link ITreeTapItem} parameters of the tap set the range of the count.
     *
     * @param tool   the tap item stack
     * @param random the random source
     * @return the resin count, or 0 if the tool is not a tree tap
     */
    int rollResinCount(ItemStack tool, RandomSource random);

    /**
     * Drops the given number of resin items between the block and the player.
     *
     * <p>The drop position follows the default rule of this mod (one block above the midpoint of the line from the block center to the player).
     *
     * @param level  the world
     * @param pos    the block position
     * @param player the player
     * @param count  the number of items
     */
    void dropResin(Level level, BlockPos pos, Player player, int count);

    /**
     * Tries to let the rubber wood regrow resin naturally (the random tick logic of the block).
     *
     * <p>This method has an effect only on the server side. It can succeed only when the block meets the regrowth conditions now.
     *
     * @param level  the world
     * @param pos    the block position
     * @param state  the block state
     * @param random the random source
     * @return true if resin regrew during this call
     */
    boolean tryRegrowResin(Level level, BlockPos pos, BlockState state, RandomSource random);

    /**
     * Returns the chance that rubber wood regrows resin on each random tick.
     *
     * @return the regrowth chance (0.0 to 1.0)
     */
    float getResinRegrowChance();
}
