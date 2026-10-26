// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entity substitution registry for the independent energy platform.
 *
 * <p>The host registers which blocks are replaced with the core independent transformer and special cable entities on placement or load.
 * This is not hard-coded in core. Thus core does not know any specific mod namespace, and add-ons can register their own
 * controller blocks. Registration occurs during the startup assembly phase. At runtime the registry is read-only.
 */
public final class IndependentSubstitutions {

    /** Transformer substitution entry: target entity type ID + low-voltage threshold. */
    public record Transformer(String entityType, long lowTier) {
        public Transformer {
            if (entityType == null || entityType.isBlank()) throw new IllegalArgumentException("entity type");
            if (lowTier <= 0) throw new IllegalArgumentException("low tier");
        }
    }

    /**
     * Special cable substitution entry: target entity type ID + whether it is a detector cable; 0.1.10.22: the
     * protection kind of a breaker or fuse cable (null for none).
     */
    public record Cable(String entityType, boolean detector,
                        com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay.Kind protection) {
        public Cable {
            if (entityType == null || entityType.isBlank()) throw new IllegalArgumentException("entity type");
            if (detector && protection != null) throw new IllegalArgumentException("a detector cable is not protective");
        }

        public Cable(String entityType, boolean detector) { this(entityType, detector, null); }
    }

    private static final Map<String, Transformer> TRANSFORMERS = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, Cable> CABLES = Collections.synchronizedMap(new LinkedHashMap<>());

    private IndependentSubstitutions() { }

    public static void registerTransformer(String blockId, String entityTypeId, long lowTier) {
        if (blockId == null || blockId.isBlank()) throw new IllegalArgumentException("block id");
        TRANSFORMERS.put(blockId, new Transformer(entityTypeId, lowTier));
    }

    public static void registerCable(String blockId, String entityTypeId, boolean detector) {
        if (blockId == null || blockId.isBlank()) throw new IllegalArgumentException("block id");
        CABLES.put(blockId, new Cable(entityTypeId, detector));
    }

    /** 0.1.10.22 (G5): a breaker or fuse cable. */
    public static void registerProtectiveCable(String blockId, String entityTypeId,
            com.miophas.singularity_iteration.core.runtime.energy.engine.ProtectionRelay.Kind kind) {
        if (blockId == null || blockId.isBlank()) throw new IllegalArgumentException("block id");
        CABLES.put(blockId, new Cable(entityTypeId, false, java.util.Objects.requireNonNull(kind, "kind")));
    }

    private static String idOf(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public static @Nullable Transformer transformerFor(BlockState state) {
        return state == null ? null : TRANSFORMERS.get(idOf(state));
    }

    public static @Nullable Cable cableFor(BlockState state) {
        return state == null ? null : CABLES.get(idOf(state));
    }

    /** Returns true if the block is registered as a special cable (this decides if the redstone signal sides are taken over). */
    public static boolean controlsCable(BlockState state) {
        return cableFor(state) != null;
    }

    /** Returns true if the special cable is a detector cable (only detector cables are signal sources). */
    public static boolean detectorCable(BlockState state) {
        Cable cable = cableFor(state);
        return cable != null && cable.detector();
    }

    public static Map<String, Object> metrics() {
        return Map.of("transformers", TRANSFORMERS.size(), "cables", CABLES.size());
    }
}
