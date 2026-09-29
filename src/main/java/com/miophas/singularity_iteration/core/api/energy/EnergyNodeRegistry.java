package com.miophas.singularity_iteration.core.api.energy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Register profiles during mod setup, before the first server starts. No namespace is reserved. */
public final class EnergyNodeRegistry {
    private static final Map<ResourceLocation, EnergyNodeProfile> PROFILES = new LinkedHashMap<>();
    private static final List<Predicate<BlockState>> ADMISSION = new ArrayList<>();
    private static volatile Map<ResourceLocation, EnergyNodeProfile> snapshot = Map.of();
    private static volatile Map<ResourceLocation, Long> conductors = Map.of();
    private static volatile List<Predicate<BlockState>> admission = List.of();
    private static boolean frozen;

    private EnergyNodeRegistry() {}

    public static synchronized void register(ResourceLocation blockId, EnergyNodeProfile profile) {
        if (frozen) throw new IllegalStateException("Energy profiles are frozen after server startup");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(profile, "profile");
        if (PROFILES.putIfAbsent(blockId, profile) != null)
            throw new IllegalArgumentException("Duplicate energy profile: " + blockId);
        snapshot = Map.copyOf(PROFILES);
        var losses = new LinkedHashMap<ResourceLocation, Long>();
        PROFILES.forEach((id, value) -> { if (value.conductorLossMilliEu() >= 0) losses.put(id, value.conductorLossMilliEu()); });
        conductors = Map.copyOf(losses);
    }

    /** Host migration policy for existing registered content; new addons should register explicit profiles. */
    public static synchronized void registerAdmissionPolicy(Predicate<BlockState> policy) {
        if (frozen) throw new IllegalStateException("Energy profiles are frozen after server startup");
        ADMISSION.add(Objects.requireNonNull(policy));
        admission = List.copyOf(ADMISSION);
    }

    public static synchronized void freeze() { frozen = true; }
    public static boolean admits(BlockState state) {
        if (!state.hasBlockEntity()) return false;
        if (snapshot.containsKey(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
        for (var policy : admission) if (policy.test(state)) return true;
        return false;
    }

    public static EnergyNodeProfile profile(ResourceLocation id) { return snapshot.get(id); }
    public static Map<ResourceLocation, Long> conductors() { return conductors; }
    public static Long conductorLoss(ResourceLocation id) { return conductors.get(id); }
    public static boolean isGenerator(ResourceLocation id) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.GENERATOR; }
    public static boolean standardProcessor(ResourceLocation id) { var p = profile(id); return p != null && p.standardProcessor(); }
    public static boolean specialCable(ResourceLocation id) { var p = profile(id); return p != null && p.specialCable(); }
    public static boolean measuredBlast(ResourceLocation id) { var p = profile(id); return p != null && p.measuredBlast(); }
    public static Long fuseLimit(ResourceLocation id) { var p = profile(id); return p != null && p.fuseLimit() >= 0 ? p.fuseLimit() : null; }
    public static Long storagePacket(ResourceLocation id) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.STORAGE && p.outputPacket() > 0 ? p.outputPacket() : null; }
    public static long generatorPacket(ResourceLocation id, long fallback) { var p = profile(id); return p != null && p.role() == EnergyNodeProfile.Role.GENERATOR && p.outputPacket() > 0 ? p.outputPacket() : fallback; }
    /** True when the generator's packet must be recomputed from its live source tier each refresh. */
    public static boolean dynamicTier(ResourceLocation id) { var p = profile(id); return p != null && p.dynamicTier(); }
    public static long storagePacket(ResourceLocation id, long fallback) { Long packet = storagePacket(id); return packet == null ? fallback : packet; }
}
