// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Public Minecraft loot and NeoForge break-event boundary; no mining/search/reference algorithm. */
public final class MiningLoot {
    private MiningLoot() { }
    public static boolean capture(ServerLevel level, BlockPos pos, ItemStack tool, MachineActionOwner owner, PendingDrops custody,
                                  Predicate<List<ItemStack>> fits) {
        return capture(level, pos, tool, owner, custody, fits, null);
    }
    public static boolean capture(ServerLevel level, BlockPos pos, ItemStack tool, MachineActionOwner owner, PendingDrops custody,
                                  Predicate<List<ItemStack>> fits, MiningPayment.Permit payment) {
        return capture(level, pos, tool, owner, custody, fits, payment, () -> {});
    }
    public static boolean capture(ServerLevel level, BlockPos pos, ItemStack tool, MachineActionOwner owner, PendingDrops custody,
                                  Predicate<List<ItemStack>> fits, MiningPayment.Permit payment, Runnable accountRemoval) {
        if (!level.getServer().isSameThread() || owner == null || !owner.canAct() || !custody.isEmpty()
                || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || !level.getWorldBorder().isWithinBounds(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir()) return payment == null;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        var actor = FakePlayerFactory.get(level, owner.actorProfile());
        var oldTool = actor.getMainHandItem().copy(); var oldPosition = actor.position();
        if (!custody.beginWorldChange()) return false;
        try {
            actor.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
            actor.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            var event = new BlockEvent.BreakEvent(level, pos, state, actor);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled() || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    || level.getBlockState(pos) != state) return false;
            var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), actor, tool.copy());
            if (drops.size() > PendingDrops.MAX_STACKS || !fits.test(drops)
                    || level.getBlockState(pos) != state || !custody.stage(drops)) return false;
            try {
                if (payment != null && !payment.fund()) { custody.cancel(); return false; }
                // Payment inventory callbacks may invalidate the previously checked world target.
                if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                        || level.getBlockState(pos) != state) { custody.cancel(); return false; }
            } catch (RuntimeException | Error failure) {
                // We have not attempted removal. Prepared drops must never escape after a payment/callback failure.
                custody.cancel(); throw failure;
            }
            try {
                if (!level.destroyBlock(pos, false, actor)) { custody.cancel(); return false; }
            } catch (RuntimeException failure) {
                if (level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.getBlockState(pos) == state) custody.cancel();
                else if (payment != null) payment.complete(accountRemoval);
                else accountRemoval.run();
                throw failure;
            }
            if (payment != null) payment.complete(accountRemoval);
            else accountRemoval.run();
            return true;
        } finally {
            try {
                actor.setItemInHand(InteractionHand.MAIN_HAND, oldTool);
                actor.setPos(oldPosition.x, oldPosition.y, oldPosition.z);
            } finally { custody.endWorldChange(); }
        }
    }
}
