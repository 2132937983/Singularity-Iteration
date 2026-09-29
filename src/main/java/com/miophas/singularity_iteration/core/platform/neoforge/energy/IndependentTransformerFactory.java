// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.IndependentEnergyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 独立变压器的实体替换：登记过的方块在放置与读档时得到 core 的独立实体。
 *
 * <p>替换表见 {@link IndependentSubstitutions}，由宿主注册。
 */
public final class IndependentTransformerFactory {

    private static final AtomicLong PLACED = new AtomicLong(), LOADED = new AtomicLong(), TICKERS = new AtomicLong();

    private IndependentTransformerFactory() { }

    private static boolean enabled() {
        return IndependentEnergyMode.feature("transformers");
    }

    private static BlockEntityType<?> type(BlockState state) {
        if (!enabled()) return null;
        var entry = IndependentSubstitutions.transformerFor(state);
        if (entry == null) return null;
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE
            .getOptional(ResourceLocation.parse(entry.entityType())).orElseThrow();
        if (!type.isValid(state)) throw new IllegalStateException("Observed transformer type does not admit block");
        return type;
    }

    public static BlockEntity placed(BlockPos position, BlockState state) {
        var type = type(state);
        if (type == null) return null;
        PLACED.incrementAndGet();
        return create(type, position, state);
    }

    public static BlockEntity loaded(BlockEntityType<?> requested, BlockPos position, BlockState state) {
        var type = type(state);
        if (type == null || type != requested) return null;
        LOADED.incrementAndGet();
        return create(type, position, state);
    }

    private static BlockEntity create(BlockEntityType<?> type, BlockPos position, BlockState state) {
        long low = IndependentSubstitutions.transformerFor(state).lowTier();
        return new IndependentTransformerBlockEntity(type, position, state, low, 1);
    }

    public static boolean suppressTicker(BlockEntity entity) {
        if (!enabled() || !(entity instanceof IndependentTransformerBlockEntity)) return false;
        TICKERS.incrementAndGet();
        return true;
    }

    public static Map<String, Object> metrics() {
        return Map.of("enabled", enabled(), "placed", PLACED.get(), "loaded", LOADED.get(),
            "suppressed_tickers", TICKERS.get());
    }
}
