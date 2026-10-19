// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_chunk_loader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.11: server rules and bookkeeping of the SI chunk loaders.
 *
 * <ul>
 *   <li><b>Rules</b> (config section {@code ChunkLoaders}, applied on load and reload): master switch,
 *       chunks per loader, chunks and loaders per player, offline owner timeout, EU per chunk and tick,
 *       random ticks of the loaded chunks, owner-only editing.</li>
 *   <li><b>Registry</b>: every loaded loader reports its owner, granted chunks and state when they
 *       change (never per tick). The per-owner totals behind the player limits are kept up to date, so
 *       a limit check costs one map lookup. {@code /mio_icif chunkloaders} reads it.</li>
 *   <li><b>Parking</b> (saved with the overworld): a loader that stops for a rule (switch off, admin
 *       lock, owner offline, player limit) gives up its own chunk too, so its chunk unloads. The park
 *       list wakes it again when the reason is gone: the owner logs in, an admin re-enables it, the
 *       switch is turned on again or the owner frees part of the budget. Waking loads the one chunk
 *       of the loader with a ticket that the loader then takes over.</li>
 * </ul>
 */
public final class ChunkLoaderPolicy {
    private ChunkLoaderPolicy() { }

    // ------------------------------------------------------------------ rules

    private static volatile boolean enabled = true;
    private static volatile int maxChunksPerLoader = mio_icif_chunk_loader.MAX_CHUNKS;
    private static volatile int maxChunksPerPlayer;
    private static volatile int maxLoadersPerPlayer;
    private static volatile int offlineMinutes = -1;
    private static volatile double euPerChunk = mio_icif_chunk_loader.DEFAULT_EU_PER_CHUNK;
    private static volatile boolean randomTicks = true;
    private static volatile boolean ownerOnlyEdit;
    private static volatile int generation;

    /** Applies the config values (any thread); loaders re-check their rules on their next tick. */
    public static void configure(boolean on, int perLoader, int perPlayer, int loadersPerPlayer, int offline,
                                 double eu, boolean ticks, boolean ownerOnly) {
        boolean wasEnabled = enabled;
        enabled = on;
        maxChunksPerLoader = Math.max(1, Math.min(mio_icif_chunk_loader.MAX_CHUNKS, perLoader));
        maxChunksPerPlayer = Math.max(0, perPlayer);
        maxLoadersPerPlayer = Math.max(0, loadersPerPlayer);
        offlineMinutes = Math.max(-1, offline);
        euPerChunk = Double.isFinite(eu) && eu >= 0 ? eu : mio_icif_chunk_loader.DEFAULT_EU_PER_CHUNK;
        randomTicks = ticks;
        ownerOnlyEdit = ownerOnly;
        generation++;
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        // switch turned on again, or limits raised: wake the loaders parked for these reasons
        if (server != null) server.execute(() -> {
            if (on && !wasEnabled) wakeParked(server, p -> p.reason() == Reason.CONFIG);
            wakeParked(server, p -> p.reason() == Reason.LIMIT);
        });
    }

    public static boolean enabled() { return enabled; }
    public static int maxChunksPerLoader() { return maxChunksPerLoader; }
    public static int maxChunksPerPlayer() { return maxChunksPerPlayer; }
    public static int maxLoadersPerPlayer() { return maxLoadersPerPlayer; }
    public static int offlineMinutes() { return offlineMinutes; }
    public static double euPerChunk() { return euPerChunk; }
    public static boolean randomTicks() { return randomTicks; }
    public static boolean ownerOnlyEdit() { return ownerOnlyEdit; }
    /** Changes with every config load or reload. */
    public static int generation() { return generation; }

    // ------------------------------------------------------------------ registry of loaded loaders

    /** One loaded loader: owner (null for loaders placed before 0.1.10.11), chunks granted now, selected, state. */
    public record Entry(GlobalPos at, @Nullable UUID owner, String ownerName, int granted, int selected, int status) { }

    private static final Map<GlobalPos, Entry> LOADERS = new LinkedHashMap<>();
    /** Per owner: {active loaders, granted chunks}. */
    private static final Map<UUID, int[]> USAGE = new HashMap<>();

    /** Records the state of a loader (server thread; called when something changed). */
    public static void report(Entry entry) {
        forget(entry.at());
        LOADERS.put(entry.at(), entry);
        if (entry.owner() != null && entry.granted() > 0) {
            int[] usage = USAGE.computeIfAbsent(entry.owner(), k -> new int[2]);
            usage[0]++;
            usage[1] += entry.granted();
        }
    }

    /** Drops a loader from the registry (unloaded or removed). Frees its share of the owner's budget. */
    public static void forget(GlobalPos at) {
        Entry old = LOADERS.remove(at);
        if (old == null || old.owner() == null || old.granted() <= 0) return;
        int[] usage = USAGE.get(old.owner());
        if (usage == null) return;
        usage[0]--;
        usage[1] -= old.granted();
        if (usage[0] <= 0) USAGE.remove(old.owner());
    }

    /** {active loaders, granted chunks} of {@code owner}, without the loader at {@code except}. */
    public static int[] usage(UUID owner, GlobalPos except) {
        int[] usage = USAGE.get(owner);
        int loaders = usage == null ? 0 : usage[0], chunks = usage == null ? 0 : usage[1];
        Entry self = LOADERS.get(except);
        if (self != null && owner.equals(self.owner()) && self.granted() > 0) { loaders--; chunks -= self.granted(); }
        return new int[] {Math.max(0, loaders), Math.max(0, chunks)};
    }

    public static List<Entry> entries() { return new ArrayList<>(LOADERS.values()); }

    // ------------------------------------------------------------------ parking

    public enum Reason { CONFIG, ADMIN, OFFLINE, LIMIT }
    public record Parked(GlobalPos at, @Nullable UUID owner, Reason reason) { }

    /** Remembers a loader that stopped for a rule, so it can be woken later. */
    public static void park(ServerLevel level, BlockPos pos, @Nullable UUID owner, Reason reason) {
        Parking parking = Parking.of(level.getServer());
        if (parking == null) return;
        GlobalPos at = GlobalPos.of(level.dimension(), pos.immutable());
        Parked old = parking.parked.get(at);
        if (old != null && old.reason() == reason && java.util.Objects.equals(old.owner(), owner)) return;
        parking.parked.put(at, new Parked(at, owner, reason));
        parking.setDirty();
    }

    public static void unpark(ServerLevel level, BlockPos pos) {
        Parking parking = Parking.of(level.getServer());
        if (parking != null && parking.parked.remove(GlobalPos.of(level.dimension(), pos)) != null) parking.setDirty();
    }

    public static List<Parked> parked(MinecraftServer server) {
        Parking parking = Parking.of(server);
        return parking == null ? List.of() : new ArrayList<>(parking.parked.values());
    }

    /**
     * A loader of {@code owner} gave up chunks: after this tick, wake the owner's loaders that were
     * parked by a player limit (they take the freed budget or park again).
     */
    public static void budgetFreed(MinecraftServer server, @Nullable UUID owner) {
        if (owner == null || maxChunksPerPlayer == 0 && maxLoadersPerPlayer == 0) return;
        server.tell(new net.minecraft.server.TickTask(server.getTickCount(),
            () -> wakeParked(server, p -> p.reason() == Reason.LIMIT && owner.equals(p.owner()))));
    }

    /** Wakes every parked loader that matches (server thread). Returns how many woke. */
    public static int wakeParked(MinecraftServer server, Predicate<Parked> which) {
        int woken = 0;
        for (Parked p : parked(server)) if (which.test(p) && wake(server, p.at())) woken++;
        return woken;
    }

    /**
     * Loads the chunk of the loader at {@code at} with a ticket owned by the loader and hands the
     * ticket over to it; the loader then applies the rules on its next tick (and parks again if the
     * reason still holds). Returns false (and drops the park entry) when no loader is there.
     */
    public static boolean wake(MinecraftServer server, GlobalPos at) {
        ServerLevel level = server.getLevel(at.dimension());
        Parking parking = Parking.of(server);
        if (parking != null && parking.parked.remove(at) != null) parking.setDirty();
        if (level == null) return false;
        BlockPos pos = at.pos();
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        boolean ticking = randomTicks;
        OwnedChunkTickets.CONTROLLER.forceChunk(level, pos, cx, cz, true, ticking);
        level.getChunk(cx, cz);   // full load now, so the block entity exists
        if (level.getBlockEntity(pos) instanceof mio_icif_chunk_loader loader && loader.adoptWakeTicket(ticking)) return true;
        OwnedChunkTickets.CONTROLLER.forceChunk(level, pos, cx, cz, false, ticking);
        return false;
    }

    /** Saved list of parked loaders (overworld data storage). */
    static final class Parking extends SavedData {
        private static final String NAME = "mio_icif_chunk_loaders";
        final Map<GlobalPos, Parked> parked = new LinkedHashMap<>();

        @Nullable
        static Parking of(MinecraftServer server) {
            ServerLevel overworld = server.overworld();
            if (overworld == null) return null;
            return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Parking::new, Parking::load, null), NAME);
        }

        static Parking load(CompoundTag tag, HolderLookup.Provider registries) {
            Parking parking = new Parking();
            ListTag list = tag.getList("Parked", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                ResourceLocation dim = ResourceLocation.tryParse(e.getString("Dim"));
                if (dim == null) continue;
                Reason reason;
                try { reason = Reason.valueOf(e.getString("Reason")); } catch (IllegalArgumentException ex) { continue; }
                GlobalPos at = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(e.getLong("Pos")));
                parking.parked.put(at, new Parked(at, e.hasUUID("Owner") ? e.getUUID("Owner") : null, reason));
            }
            return parking;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Parked p : parked.values()) {
                CompoundTag e = new CompoundTag();
                e.putString("Dim", p.at().dimension().location().toString());
                e.putLong("Pos", p.at().pos().asLong());
                if (p.owner() != null) e.putUUID("Owner", p.owner());
                e.putString("Reason", p.reason().name());
                list.add(e);
            }
            tag.put("Parked", list);
            return tag;
        }
    }

    // ------------------------------------------------------------------ events

    public static void install() {
        // the owner is back: wake the loaders parked while they were away
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            MinecraftServer server = event.getEntity().getServer();
            UUID id = event.getEntity().getUUID();
            if (server != null) wakeParked(server, p -> p.reason() == Reason.OFFLINE && id.equals(p.owner()));
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            if (enabled) wakeParked(event.getServer(), p -> p.reason() == Reason.CONFIG);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> { LOADERS.clear(); USAGE.clear(); });
    }
}
