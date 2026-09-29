// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Paid laser work is limited by a persisted range and block budget. */
public class mio_icif_laser_bullet extends IndependentElectricProjectile {
    private mio_icif_laser_miner.Mode mode = mio_icif_laser_miner.Mode.MINING;
    private int remainingBlocks = mode.blocks;

    public mio_icif_laser_bullet(EntityType<? extends mio_icif_laser_bullet> type, Level level) {
        super(type, level);
    }
    public mio_icif_laser_bullet(EntityType<? extends mio_icif_laser_bullet> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }
    public void configure(mio_icif_laser_miner.Mode mode) {
        this.mode = mode;
        remainingBlocks = mode.blocks;
        setRange(mode.range);
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        damage(hit.getEntity(), mode.damage);
        detonate(hit.getLocation());
        discard();
    }

    @Override protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel level)) return;
        if (mode.explosion > 0.0F) {
            detonate(hit.getLocation());
            discard();
            return;
        }
        if (remainingBlocks <= 0 || !(getOwner() instanceof ServerPlayer player) || !player.mayBuild()) {
            discard();
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) { discard(); return; }
        var state = level.getBlockState(pos);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0
                || (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state))
                || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()
                || !level.getBlockState(pos).equals(state)) { discard(); return; }
        var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        if (mode == mio_icif_laser_miner.Mode.SUPER_HEAT) {
            var smelted = new ArrayList<ItemStack>();
            for (ItemStack drop : drops) {
                var input = new SingleRecipeInput(drop);
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
                if (recipe.isEmpty()) { smelted.add(drop); continue; }
                ItemStack output = recipe.get().value().assemble(input, level.registryAccess());
                if (output.isEmpty()) { smelted.add(drop); continue; }
                // Preserve count even if a modded recipe emits multiple items per input.
                for (int count = 0; count < drop.getCount(); count++) smelted.add(output.copy());
            }
            drops = smelted;
        }
        if (!level.destroyBlock(pos, false, player)) { discard(); return; }
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);
        if (--remainingBlocks <= 0) discard();
        else resumeAtImpact(hit.getLocation());
    }

    private void detonate(net.minecraft.world.phys.Vec3 center) {
        boolean nuclear = mode == mio_icif_laser_miner.Mode.NUCLEAR_EXPLOSIVE;
        if (nuclear && !Singularity_Iteration_Config.ENABLE_LASER_NUCLEAR_EXPLOSIVE.get()) return;
        explode(center, mode.explosion, !nuclear || Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get());
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ScexLaserMode", mode.ordinal());
        tag.putInt("ScexLaserBlocks", remainingBlocks);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        mode = mio_icif_laser_miner.Mode.byIndex(tag.getInt("ScexLaserMode"));
        remainingBlocks = Math.clamp(tag.getInt("ScexLaserBlocks"), 0, mode.blocks);
    }
}
