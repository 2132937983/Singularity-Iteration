// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Registers the ponder plugin (0.1.8.0). Called only when the Ponder library is loaded. */
@OnlyIn(Dist.CLIENT)
public final class PonderIntegration {
    private PonderIntegration() {}

    public static void register() {
        PonderIndex.addPlugin(new SiPonderPlugin());
    }
}
