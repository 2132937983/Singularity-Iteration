// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 0.1.11.80: listeners told how much EU a generator stored (both generation paths of the energy block entities: the
 * unified {@code produce} and the legacy {@code apiGenerateEnergy}). The mod uses it for the clean energy total of the
 * "Clean Energy Ambassador" advancement; addons may listen too. Called on the server thread, often (every generating
 * tick of every generator), so a listener must be cheap and must not change the block entity.
 */
public final class EnergyProductionListeners {
    private EnergyProductionListeners() {}

    /** Told the EU a block entity just stored (more than 0). */
    @FunctionalInterface
    public interface Listener {
        void produced(BlockEntity source, double eu);
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void register(Listener listener) {
        if (listener != null && !LISTENERS.contains(listener)) LISTENERS.add(listener);
    }

    /** Tells every listener; a no-op without listeners or for nothing stored. */
    public static void fire(BlockEntity source, double eu) {
        if (!(eu > 0) || LISTENERS.isEmpty() || source.getLevel() == null || source.getLevel().isClientSide()) return;
        for (Listener l : LISTENERS) l.produced(source, eu);
    }
}
