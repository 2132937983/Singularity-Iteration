// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 独立特种线缆（检测/分流）的实体替换与红石信号面判定。
 *
 * <p>替换表见 {@link IndependentSubstitutions}，由宿主注册。
 */
public final class IndependentCableFactory {

    private IndependentCableFactory() { }

    private static boolean enabled() {
        return IndependentEnergyMode.feature("specialCables");
    }

    public static boolean controls(BlockState state) {
        return enabled() && IndependentSubstitutions.controlsCable(state);
    }

    public static boolean detector(BlockState state) {
        return controls(state) && IndependentSubstitutions.detectorCable(state);
    }

    public static BlockEntity create(BlockEntityType<?> requested, BlockPos at, BlockState state) {
        if (!controls(state)) return null;
        var entry = IndependentSubstitutions.cableFor(state);
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE
            .getOptional(ResourceLocation.parse(entry.entityType())).orElseThrow();
        if (requested != null && requested != type) return null;
        if (!type.isValid(state)) throw new IllegalStateException("Special cable type does not admit observed block");
        return new IndependentSpecialCableBlockEntity(type, at, state, detector(state));
    }

    public static boolean suppressTicker(BlockEntity tile) {
        return enabled() && tile instanceof IndependentSpecialCableBlockEntity;
    }
}
