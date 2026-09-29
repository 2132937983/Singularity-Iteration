// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.tool.IMiningDrill;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Independent diamond-tier drill; successful paid mining uses ordinary block loot. */
@SuppressWarnings("deprecation")
public class mio_icif_diamond_driller extends mio_icif_tool_elc implements IMiningDrill {
    public static final int DIAMOND_DRILLER_MAX_ENERGY = 30_000;
    public static final int DIAMOND_DRILLER_TRANSFER_LIMIT = 100;
    public static final int DIAMOND_DRILLER_ENERGY_PER_USE = 80;
    public static final float DIAMOND_DRILLER_EFFICIENCY = 16.0F;

    public mio_icif_diamond_driller(Properties properties) { this(properties, 0); }
    public mio_icif_diamond_driller(Properties properties, int initialEnergy) {
        super(properties, DIAMOND_DRILLER_MAX_ENERGY,
                Math.clamp(initialEnergy, 0, DIAMOND_DRILLER_MAX_ENERGY), "diamond_driller",
                DIAMOND_DRILLER_TRANSFER_LIMIT, DIAMOND_DRILLER_ENERGY_PER_USE, 1);
    }
    private static boolean mineable(BlockState state) {
        return (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL))
                && !state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return hasEnoughEnergy(stack) && mineable(state) ? DIAMOND_DRILLER_EFFICIENCY : 1.0F;
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return hasEnoughEnergy(stack) && mineable(state);
    }
    @Override public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return hasEnoughEnergy(stack) && (ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability));
    }
    @Override public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return state.getDestroySpeed(level, pos) >= 0 && (hasEnoughEnergy(player.getMainHandItem())
                || !state.requiresCorrectToolForDrops());
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0) consumeEnergy(stack);
        return true;
    }
    @Override public long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        return state.getDestroySpeed(world, pos) == 0 ? 0 : DIAMOND_DRILLER_ENERGY_PER_USE;
    }
    @Override public int breakTime(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(world, pos);
        return hardness < 0 ? -1 : (int)Math.ceil(hardness * 30.0D / getDestroySpeed(stack, state));
    }
    @Override public boolean breakBlock(ItemStack stack, BlockGetter world, BlockPos pos, BlockState expected) {
        if (!(world instanceof ServerLevel level) || !level.hasChunkAt(pos)
                || !level.getBlockState(pos).equals(expected) || expected.getDestroySpeed(level, pos) < 0
                || !isCorrectToolForDrops(stack, expected)) return false;
        var actor = FakePlayerFactory.getMinecraft(level);
        if (!level.mayInteract(actor, pos) || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, expected, actor)).isCanceled()) return false;
        if (!level.getBlockState(pos).equals(expected)) return false;
        long cost = energyUse(stack, world, pos, expected);
        var blockEntity = level.getBlockEntity(pos);
        var drops = Block.getDrops(expected, level, pos, blockEntity, actor, stack);
        if (!consumeEnergy(stack, cost)) return false;
        if (!level.destroyBlock(pos, false, actor)) { addEnergy(stack, cost); return false; }
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);
        return true;
    }
    @Override public boolean isEnchantable(ItemStack stack) { return true; }
    @Override public int getEnchantmentValue() { return Tiers.DIAMOND.getEnchantmentValue(); }
}
