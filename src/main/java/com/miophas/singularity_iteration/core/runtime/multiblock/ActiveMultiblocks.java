package com.miophas.singularity_iteration.core.runtime.multiblock;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The single active-structure store shared by builtin and addon multiblocks.
 *
 * <p>Structures register here once they form and are removed when they
 * invalidate. Lookup by arbitrary position walks the controller map and then
 * the block membership of each active structure, so addon structures built
 * through {@link GenericMultiblockBuilder} and builtin structures share one
 * discovery path.
 */
public final class ActiveMultiblocks {

    private static final Map<Level, Map<BlockPos, IMultiblockStructure>> ACTIVE = new WeakHashMap<>();

    private ActiveMultiblocks() {}

    /** 框架内部注册点：成形时调用一次，不构成公开兼容承诺。 */
    public static void register(Level level, IMultiblockStructure structure) {
        ACTIVE.computeIfAbsent(level, key -> new HashMap<>())
            .put(structure.getControllerPos(), structure);
    }

    /** 框架内部注销点：拆除时调用一次，不构成公开兼容承诺。 */
    public static void unregister(Level level, IMultiblockStructure structure) {
        Map<BlockPos, IMultiblockStructure> structures = ACTIVE.get(level);
        if (structures != null) {
            structures.remove(structure.getControllerPos(), structure);
        }
    }

    public static @Nullable IMultiblockStructure getStructureAt(Level level, BlockPos pos) {
        Map<BlockPos, IMultiblockStructure> structures = ACTIVE.get(level);
        if (structures == null || pos == null) {
            return null;
        }

        IMultiblockStructure structure = structures.get(pos);
        if (structure != null) {
            return structure;
        }

        for (IMultiblockStructure candidate : structures.values()) {
            if (candidate.isPartOfStructure(pos)) {
                return candidate;
            }
        }

        return null;
    }

    public static @Nullable IMultiblockStructure getStructureByController(Level level, BlockPos controllerPos) {
        Map<BlockPos, IMultiblockStructure> structures = ACTIVE.get(level);
        return structures != null && controllerPos != null ? structures.get(controllerPos) : null;
    }

    public static Collection<IMultiblockStructure> getAllStructures(Level level) {
        Map<BlockPos, IMultiblockStructure> structures = ACTIVE.get(level);
        return structures != null ? Collections.unmodifiableCollection(structures.values()) : Collections.emptyList();
    }

    public static boolean isControllerAt(Level level, BlockPos pos) {
        Map<BlockPos, IMultiblockStructure> structures = ACTIVE.get(level);
        return structures != null && pos != null && structures.containsKey(pos);
    }

    public static boolean isPartOfAnyStructure(Level level, BlockPos pos) {
        return getStructureAt(level, pos) != null;
    }
}
