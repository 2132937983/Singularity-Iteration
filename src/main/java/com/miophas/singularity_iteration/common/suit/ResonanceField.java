// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Resonance Deconstruction Field, server side (0.1.8.0).
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>Active when the unit is installed in the boots or leggings and switched on, the wearer
 *       walks (more than 0.02 blocks per tick horizontally), does not crouch, and may build
 *       (not adventure or spectator mode).</li>
 *   <li>Each tick the field breaks at most {@link #MAX_PER_TICK} blocks, nearest first, in a
 *       sphere of {@link #RADIUS} blocks around the waist. Blocks below the feet level stay (the
 *       wearer does not fall).</li>
 *   <li>Skipped blocks: air, fluids, unbreakable blocks, blocks harder than
 *       {@link #MAX_HARDNESS}, blocks with a block entity (containers, spawners, machines) and
 *       the block tag {@code mio_icif:resonance_immune}.</li>
 *   <li>Protection: the NeoForge block break event is fired as the wearer (claims can cancel
 *       it) and spawn protection applies.</li>
 *   <li>Drops: as mined with a netherite tool of the right type (pickaxe, shovel, axe, hoe;
 *       no enchantment). They go into the inventory; what does not fit drops at the feet.
 *       Experience orbs appear at the block.</li>
 *   <li>Energy: {@link #BASE_EU} + {@link #EU_PER_HARDNESS} x hardness EU per block from the
 *       suit (the full-set pool applies). Without energy the field stops.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ResonanceField {
    private ResonanceField() {}

    public static final double RADIUS = 2.5;
    public static final int MAX_PER_TICK = 6;
    public static final float MAX_HARDNESS = 30.0F;
    public static final long BASE_EU = 60, EU_PER_HARDNESS = 40;
    public static final TagKey<Block> IMMUNE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("mio_icif", "resonance_immune"));

    /** 0.1.10.1 Mk.II: radius +50 % and half as many more blocks per tick. */
    public static final double RADIUS_MK2 = RADIUS * 1.5;
    public static final int MAX_PER_TICK_MK2 = 9;

    private static final Map<UUID, Vec3> LAST = new HashMap<>();
    private static final List<BlockPos> OFFSETS = offsets(RADIUS), OFFSETS_MK2 = offsets(RADIUS_MK2);

    private static List<BlockPos> offsets(double radius) {
        List<BlockPos> out = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) for (int dy = 0; dy <= 3; dy++) for (int dz = -r; dz <= r; dz++) {
            // centre at the waist (feet + 1): dy 0 = feet layer, dy 3 = above the head
            double cy = dy - 1.0;
            if (dx * dx + cy * cy + dz * dz <= radius * radius) out.add(new BlockPos(dx, dy, dz));
        }
        out.sort((a, b) -> Integer.compare(a.distManhattan(BlockPos.ZERO.above()), b.distManhattan(BlockPos.ZERO.above())));
        return out;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        Vec3 now = player.position();
        Vec3 last = LAST.put(player.getUUID(), now);
        if (last == null || player.isShiftKeyDown() || player.isSpectator() || !player.mayBuild()) return;
        double moved = Math.hypot(now.x - last.x, now.z - last.z);
        if (moved < 0.02 || moved > 4) return;                                // standing, or a teleport
        if (!SuitModules.isActive(player, SuitModuleType.RESONANCE_FIELD)) return;
        ItemStack piece = SuitModules.wornPieceWith(player, SuitModuleType.RESONANCE_FIELD);
        BlockPos feet = player.blockPosition();
        boolean mk2 = SuitModules.tier(piece, SuitModuleType.RESONANCE_FIELD.id()) >= 2;
        int limit = mk2 ? MAX_PER_TICK_MK2 : MAX_PER_TICK;
        int broken = 0;
        for (BlockPos off : mk2 ? OFFSETS_MK2 : OFFSETS) {
            if (broken >= limit) break;
            BlockPos pos = feet.offset(off);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0 || hardness > MAX_HARDNESS || state.hasBlockEntity() || state.is(IMMUNE)) continue;
            long cost = BASE_EU + (long) Math.ceil(hardness * EU_PER_HARDNESS);
            if (!SuitModules.canPay(player, piece, cost)) break;
            if (!level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) continue;
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) continue;
            SuitModules.extract(player, piece, cost);
            shatter(level, player, pos, state);
            broken++;
        }
        if (broken > 0 && player.tickCount % 3 == 0) {
            // standing-wave ring at the feet
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 + player.tickCount * 0.3;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, now.x + Math.cos(a) * 1.3, now.y + 0.1, now.z + Math.sin(a) * 1.3, 1, 0, 0.02, 0, 0);
            }
        }
    }

    private static void shatter(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        ItemStack tool = toolFor(state);
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, tool);
        int xp = state.getExpDrop(level, pos, null, player, tool);
        level.levelEvent(2001, pos, Block.getId(state));                    // break particles and sound
        level.removeBlock(pos, false);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            ItemStack rest = drop.copy();
            if (!player.getInventory().add(rest) && !rest.isEmpty()) Block.popResource(level, player.blockPosition(), rest);
        }
        if (xp > 0) ExperienceOrb.award(level, Vec3.atCenterOf(pos), xp);
    }

    /** A netherite tool of the right type for the drops (no enchantment), else no tool. */
    static ItemStack toolFor(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return new ItemStack(Items.NETHERITE_PICKAXE);
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return new ItemStack(Items.NETHERITE_SHOVEL);
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return new ItemStack(Items.NETHERITE_AXE);
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return new ItemStack(Items.NETHERITE_HOE);
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        LAST.clear();
    }
}
