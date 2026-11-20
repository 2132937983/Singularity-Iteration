package com.miophas.singularity_iteration.common.blockentity.fusion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.92: which fusion controller a port serves. The controller links its ports when its structure forms and unlinks them
 * when it breaks; each change invalidates the port's capabilities, so pipes look again. 0.1.11.97: the fuel injection port and
 * the disruption mitigation port have slots of their own now ({@link FusionItemPortBlockEntity}); their screens look up here
 * whether they are linked.
 */
public final class FusionPortRegistry {
    private FusionPortRegistry() {}

    private static final Map<Level, Map<BlockPos, BlockPos>> LINKS = Collections.synchronizedMap(new WeakHashMap<>());

    static void link(Level level, BlockPos port, BlockPos controller) {
        BlockPos before;
        synchronized (LINKS) { before = LINKS.computeIfAbsent(level, l -> new HashMap<>()).put(port.immutable(), controller.immutable()); }
        if (!controller.equals(before)) level.invalidateCapabilities(port);
    }

    static void unlink(Level level, BlockPos port) {
        BlockPos before;
        synchronized (LINKS) {
            Map<BlockPos, BlockPos> at = LINKS.get(level);
            before = at == null ? null : at.remove(port);
        }
        if (before != null) level.invalidateCapabilities(port);
    }

    /** The formed controller a port serves, if its chunk is loaded. */
    public static @Nullable FusionControllerBlockEntity controller(Level level, BlockPos port) {
        BlockPos c;
        synchronized (LINKS) {
            Map<BlockPos, BlockPos> at = LINKS.get(level);
            c = at == null ? null : at.get(port);
        }
        if (c == null || !level.isLoaded(c)) return null;
        return level.getBlockEntity(c) instanceof FusionControllerBlockEntity controller && controller.serves(port) ? controller : null;
    }
}
