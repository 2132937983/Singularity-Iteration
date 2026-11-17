// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.List;
import java.util.Objects;

/**
 * The input types of the fair-share round ({@link FairShareDistributor}): conductor domains and shared storage.
 * 0.1.11.32: the packet priority walk that this class also held (distributionMode=legacy) was removed.
 */
public final class DomainDistributor {
    private DomainDistributor() { }

    /** A source and receiver ID backed by the same storage snapshot. */
    public record SharedStorage(int source, int receiver, long capacity) {
        public SharedStorage {
            if (source < 0 || receiver < 0 || capacity < 0)
                throw new IllegalArgumentException("Invalid shared storage binding");
        }
    }

    /** One conductor domain: its entries (source IDs in window order) and the route costs of each entry. */
    public static final class Domain {
        final int[] sources;
        final List<RouteCosts> routes;

        public Domain(int[] sourceIds, List<? extends RouteCosts> sourceRoutes) {
            sources = Objects.requireNonNull(sourceIds, "sourceIds").clone();
            routes = List.copyOf(sourceRoutes);
            if (sources.length != routes.size()) throw new IllegalArgumentException("Domain source vector lengths");
        }
    }
}
