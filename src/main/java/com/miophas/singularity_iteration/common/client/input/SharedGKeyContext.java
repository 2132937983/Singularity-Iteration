// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.input;

import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Key conflict context of the two SI actions that share the default key G (0.1.10.2): the tool
 * mode toggle and the quantum command wheel. They act by context — with a toggleable electric tool
 * in a hand, G toggles the tool; otherwise G opens the command wheel — so they do not conflict with
 * each other. Against every other in-game key they conflict as usual (the Controls screen marks a
 * clash in red). Both keys stay separately rebindable.
 */
public enum SharedGKeyContext implements IKeyConflictContext {
    INSTANCE;

    @Override
    public boolean isActive() {
        return KeyConflictContext.IN_GAME.isActive();
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return other != this && (other == KeyConflictContext.UNIVERSAL || other == KeyConflictContext.IN_GAME);
    }
}
