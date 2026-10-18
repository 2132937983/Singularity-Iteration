// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Radiation shielding by blocks (0.1.10.5).
 *
 * <p>Radiation from a point (a hot reactor, a radioactive block, the centre of a nuclear blast
 * zone) travels in a straight line. Every block on the line between the source and the creature
 * lets only a part of it through:
 * <ul>
 *   <li>{@link #STRONG} ({@code mio_icif:radiation_shielding}: lead blocks, {@code c:storage_blocks/lead}):
 *       {@link #STRONG_FACTOR} (one lead block stops 90 %);</li>
 *   <li>{@link #WEAK} ({@code mio_icif:radiation_shielding_weak}: reinforced stone, obsidian, iron
 *       and steel blocks and similar): {@link #WEAK_FACTOR} each.</li>
 * </ul>
 * Below {@link #BLOCKED} nothing gets through (two lead blocks, or one lead block and two weak
 * blocks). The creature is exposed as much as the better of two lines allows: to its eyes and to
 * its middle, so a creature that looks over a wall is exposed. Only the last {@link #MAX_STEPS}
 * blocks before the creature count, and blocks in unloaded chunks count as air (no chunk loads).
 * Data packs add blocks to the two tags.
 */
public final class RadiationShielding {
    private RadiationShielding() {}

    public static final TagKey<Block> STRONG = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("mio_icif", "radiation_shielding"));
    public static final TagKey<Block> WEAK = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("mio_icif", "radiation_shielding_weak"));
    public static final double STRONG_FACTOR = 0.1, WEAK_FACTOR = 0.5, BLOCKED = 0.05;
    public static final int MAX_STEPS = 48;

    /**
     * Share of the radiation from {@code source} that reaches {@code target}: 1 on an open line,
     * 0 when the line is blocked. {@code sourceBlock} (the block that radiates) does not shield.
     */
    public static double transmission(Level level, Vec3 source, LivingEntity target, @Nullable BlockPos sourceBlock) {
        double eyes = transmission(level, source, target.getEyePosition(), sourceBlock);
        if (eyes >= 1.0) return 1.0;
        return Math.max(eyes, transmission(level, source, target.getBoundingBox().getCenter(), sourceBlock));
    }

    /** Share of the radiation from {@code source} that reaches the point {@code target}. */
    public static double transmission(Level level, Vec3 source, Vec3 target, @Nullable BlockPos sourceBlock) {
        Vec3 from = source;
        double length = target.distanceTo(source);
        if (length > MAX_STEPS) from = target.add(source.subtract(target).scale(MAX_STEPS / length));
        double[] share = {1.0};
        BlockGetter.traverseBlocks(from, target, share, (s, pos) -> {
            if (sourceBlock != null && pos.equals(sourceBlock)) return null;
            if (!level.hasChunkAt(pos)) return null;
            BlockState state = level.getBlockState(pos);
            if (state.is(STRONG)) s[0] *= STRONG_FACTOR;
            else if (state.is(WEAK)) s[0] *= WEAK_FACTOR;
            return s[0] < BLOCKED ? Boolean.TRUE : null;
        }, s -> Boolean.FALSE);
        return share[0] < BLOCKED ? 0.0 : share[0];
    }

    /** Centre of a block as a radiation source. */
    public static Vec3 centre(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }
}
