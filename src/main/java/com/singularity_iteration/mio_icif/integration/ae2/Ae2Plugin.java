package com.singularity_iteration.mio_icif.integration.ae2;

import com.singularity_iteration.mio_icif.Singularity_Iteration;

@SuppressWarnings("null")
public class Ae2Plugin {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        if (!AE2Compat.isAE2Loaded()) return;
        Singularity_Iteration.LOGGER.info("[Ae2Plugin] AE2 compatibility layer enabled. AE2 energy acceptors will be handled by wire scanning.");
    }
}