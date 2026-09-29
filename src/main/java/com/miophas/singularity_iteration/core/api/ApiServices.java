package com.miophas.singularity_iteration.core.api;

import java.util.Objects;

/** Bootstrap binding for the public facade. Content implementations are supplied by the host mod. */
public final class ApiServices {
    private static volatile MioIcifAPI instance;

    private ApiServices() {}

    public static MioIcifAPI get() {
        MioIcifAPI result = instance;
        if (result == null) throw new IllegalStateException("Singularity core services are not initialized yet");
        return result;
    }

    /** Called once by the host during mod construction, before content is created. */
    public static synchronized void install(MioIcifAPI services) {
        Objects.requireNonNull(services, "services");
        if (instance != null && instance != services) throw new IllegalStateException("Core services already installed");
        instance = services;
    }
}
