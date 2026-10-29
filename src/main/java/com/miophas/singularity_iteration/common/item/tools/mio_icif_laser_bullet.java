// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        level().playSound(null, getX(), getY(), getZ(), com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ENERGY_HIT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.2F);
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
        if (random.nextInt(3) == 0) level.playSound(null, pos, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ENERGY_HIT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.4F);
        if (mode == mio_icif_laser_miner.Mode.THREE_BY_THREE) {
            // 0.1.10.36: the 3x3 around the hit block, in the plane of the face that was hit. The outer eight skip blocks
            // with a block entity (chests, machines); the aimed block is mined like any other.
            Direction.Axis normal = hit.getDirection().getAxis();
            Direction.Axis first = normal == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
            Direction.Axis second = normal == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                BlockPos target = pos.relative(first, a).relative(second, b);
                if ((a != 0 || b != 0) && level.isLoaded(target) && level.getBlockEntity(target) != null) continue;
                mine(level, player, target);
            }
            discard();
            return;
        }
        if (!mine(level, player, pos)) { discard(); return; }
        if (--remainingBlocks <= 0) discard();
        else resumeAtImpact(hit.getLocation());
    }

    /** Mines one block for {@code player} with a plain diamond pickaxe (protection and break events first); false: left as is. */
    private boolean mine(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0
                || (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state))
                || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()
                || !level.getBlockState(pos).equals(state)) return false;
        var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        if (mode == mio_icif_laser_miner.Mode.SUPER_HEAT) drops = mio_icif_laser_miner.superHeat(level, drops);
        if (!level.destroyBlock(pos, false, player)) return false;
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);
        return true;
    }

    private void detonate(net.minecraft.world.phys.Vec3 center) {
        boolean nuclear = mode == mio_icif_laser_miner.Mode.NUCLEAR_EXPLOSIVE;
        if (nuclear && !Singularity_Iteration_Config.ENABLE_LASER_NUCLEAR_EXPLOSIVE.get()) return;
        boolean terrain = !nuclear || Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get();
        if (!nuclear) {
            explode(center, mode.explosion, terrain);
            return;
        }
        // 0.1.10.11: Ultra-High Energy Blast: its own sound and a plasma blast on the clients
        // (core flash, shock rings, plasma column, sparks, screen flash and shake) instead of the
        // vanilla explosion puffs; terrain and protection events are the vanilla ones. 0.1.10.36: every entity within the
        // blast radius takes 32767 (LaserUltraBlastDamage).
        if (explode(center, mode.explosion, terrain, net.minecraft.core.particles.ParticleTypes.FLASH,
                net.minecraft.core.particles.ParticleTypes.FLASH,
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.LASER_ULTRA_BLAST, LaserUltraBlastDamage::new)
                && level() instanceof ServerLevel level) {
            com.miophas.singularity_iteration.common.network.mio_icif_Network.sendLaserBlast(level, center.x, center.y, center.z, mode.explosion);
        }
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
