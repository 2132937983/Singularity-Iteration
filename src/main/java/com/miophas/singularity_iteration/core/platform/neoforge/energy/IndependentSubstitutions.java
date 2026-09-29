// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 独立能源平台的实体替换登记表。
 *
 * <p>哪些方块在放置/读档时被替换成 core 的独立变压器与特种线缆实体，由宿主注册，
 * 而不是写死在 core 里：core 因此不认识任何具体模组命名空间，附属也能登记自己的
 * 控制器方块。注册在启动装配阶段完成，运行期只读。
 */
public final class IndependentSubstitutions {

    /** 变压器替换项：目标实体类型 ID + 低压阈值。 */
    public record Transformer(String entityType, long lowTier) {
        public Transformer {
            if (entityType == null || entityType.isBlank()) throw new IllegalArgumentException("entity type");
            if (lowTier <= 0) throw new IllegalArgumentException("low tier");
        }
    }

    /** 特种线缆替换项：目标实体类型 ID + 是否为检测线缆。 */
    public record Cable(String entityType, boolean detector) {
        public Cable {
            if (entityType == null || entityType.isBlank()) throw new IllegalArgumentException("entity type");
        }
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

    private static String idOf(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public static @Nullable Transformer transformerFor(BlockState state) {
        return state == null ? null : TRANSFORMERS.get(idOf(state));
    }

    public static @Nullable Cable cableFor(BlockState state) {
        return state == null ? null : CABLES.get(idOf(state));
    }

    /** 该方块是否登记为特种线缆（决定红石信号面是否被接管）。 */
    public static boolean controlsCable(BlockState state) {
        return cableFor(state) != null;
    }

    /** 该特种线缆是否为检测线缆（检测线缆才是信号源）。 */
    public static boolean detectorCable(BlockState state) {
        Cable cable = cableFor(state);
        return cable != null && cable.detector();
    }

    public static Map<String, Object> metrics() {
        return Map.of("transformers", TRANSFORMERS.size(), "cables", CABLES.size());
    }
}
