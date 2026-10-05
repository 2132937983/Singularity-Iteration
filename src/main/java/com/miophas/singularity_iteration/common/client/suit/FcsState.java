// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Per-tick snapshot of which units are live on the local player, plus shared sensor results. */
public final class FcsState {
    private FcsState() {}

    private static final Set<SuitModuleType> ACTIVE = EnumSet.noneOf(SuitModuleType.class);
    private static long drain;
    static BallisticSolver.Solution ballistic;
    @Nullable static Entity leadTarget;
    @Nullable static Vec3 leadPoint;
    /** ESP outline set (entity ids) and their outline colours. */
    static final IntOpenHashSet GLOW = new IntOpenHashSet();
    static final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap GLOW_COLOR = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();

    static void update(@Nullable LocalPlayer player) {
        ACTIVE.clear();
        drain = 0;
        if (player == null) return;
        for (SuitModuleType type : SuitModuleType.values()) {
            if (SuitModules.isActive(player, type)) {
                ACTIVE.add(type);
                drain += type.drainPerTick();
            }
        }
    }

    public static boolean active(SuitModuleType type) { return ACTIVE.contains(type); }
    public static Set<SuitModuleType> activeSet() { return ACTIVE; }
    public static long drain() { return drain; }

    /** Any unit that draws on the HUD. */
    public static boolean anyHud() {
        for (SuitModuleType type : ACTIVE) if (type.needsVisor()) return true;
        return false;
    }

    // ------------------------------------------------------------------ ESP outline hooks (mixins)

    public static boolean shouldGlow(Entity entity) {
        return !GLOW.isEmpty() && GLOW.contains(entity.getId());
    }

    public static int glowColor(Entity entity) {
        return GLOW_COLOR.getOrDefault(entity.getId(), -1);
    }

    static void clear() {
        ACTIVE.clear();
        GLOW.clear();
        GLOW_COLOR.clear();
        ballistic = null;
        leadTarget = null;
        leadPoint = null;
    }
}
