// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor.audit;

import com.miophas.singularity_iteration.core.runtime.energy.OverloadBlastLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.12: the reactor incident log of a server (saved with the overworld, the last {@value #LIMIT}
 * incidents). An incident is a reactor meltdown (nuclear blast, local explosion, or a meltdown with the
 * explosions switched off) or the removal of a reactor that was running or hot. It holds the blackbox of
 * the reactor (cycles, layout, melts), the place, the time, the owner, the players nearby and the causes
 * that {@link ReactorAccidentAnalyzer} found. The audit screen and {@code /mio_icif reactor audit} read it.
 */
public final class ReactorIncidents {
    private ReactorIncidents() {}

    public static final int LIMIT = 64;
    public static final String NUCLEAR = "nuclear", LOCAL = "local", CONTAINED = "contained", REMOVED = "removed";
    /** Custom data keys of the blackbox item. */
    public static final String ITEM_ID = "IncidentId", ITEM_CAUSE = "IncidentCause", ITEM_TIME = "IncidentTime";

    static final class Log extends SavedData {
        private static final String NAME = "mio_icif_reactor_incidents";
        int nextId = 1;
        final LinkedHashMap<Integer, CompoundTag> incidents = new LinkedHashMap<>();

        static Log load(CompoundTag tag, HolderLookup.Provider registries) {
            Log log = new Log();
            log.nextId = Math.max(1, tag.getInt("Next"));
            ListTag list = tag.getList("Incidents", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                var incident = list.getCompound(i);
                log.incidents.put(incident.getInt("Id"), incident);
            }
            return log;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("Next", nextId);
            ListTag list = new ListTag();
            for (var incident : incidents.values()) list.add(incident);
            tag.put("Incidents", list);
            return tag;
        }
    }

    @Nullable
    static Log log(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return null;
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Log::new, Log::load, null), Log.NAME);
    }

    /** The facts of the reactor at the event, besides its blackbox. */
    public record Facts(String type, long eventTime, @Nullable UUID owner, String ownerName, int powerMillis,
                        String mode, int feedRate, long hull, long maxHull) { }

    /**
     * Analyses the blackbox, saves the incident and returns its id (0 when there is no log).
     */
    public static int record(ServerLevel level, BlockPos pos, ReactorBlackbox box, Facts facts) {
        Log log = log(level.getServer());
        if (log == null) return 0;
        var samples = box.samples();
        var melts = box.melts();
        String dimension = level.dimension().location().toString();
        long since = facts.eventTime() - 20L * (ReactorBlackbox.SAMPLES + 1);
        var blasts = new ArrayList<ReactorAccidentAnalyzer.Blast>();
        for (var b : OverloadBlastLog.near(dimension, pos.getX(), pos.getY(), pos.getZ(), 16, since))
            blasts.add(new ReactorAccidentAnalyzer.Blast(b.x(), b.y(), b.z(), b.gameTime()));
        var findings = ReactorAccidentAnalyzer.analyze(samples, melts, facts.eventTime(), REMOVED.equals(facts.type()), blasts);

        CompoundTag tag = new CompoundTag();
        int id = log.nextId++;
        tag.putInt("Id", id);
        tag.putString("Type", facts.type());
        tag.putLong("Time", System.currentTimeMillis());
        tag.putLong("GameTime", facts.eventTime());
        tag.putLong("Day", level.getDayTime() / 24000L + 1);
        tag.putString("Dim", dimension);
        tag.putInt("X", pos.getX()); tag.putInt("Y", pos.getY()); tag.putInt("Z", pos.getZ());
        if (facts.owner() != null) tag.putUUID("Owner", facts.owner());
        tag.putString("OwnerName", facts.ownerName());
        ListTag nearby = new ListTag();
        for (var player : level.players())
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 * 64)
                nearby.add(StringTag.valueOf(player.getGameProfile().getName()));
        tag.put("Nearby", nearby);
        tag.putInt("Power", facts.powerMillis());
        tag.putString("Mode", facts.mode());
        tag.putInt("Feed", facts.feedRate());
        tag.putLong("Hull", facts.hull());
        tag.putLong("MaxHull", facts.maxHull());
        tag.put("Samples", ReactorBlackbox.saveSamples(samples));
        tag.put("Layout", ReactorBlackbox.saveLayout(box.layout()));
        tag.put("Melts", ReactorBlackbox.saveMelts(melts));
        ListTag list = new ListTag();
        for (var finding : findings) {
            CompoundTag f = new CompoundTag();
            f.putString("Code", finding.code());
            ListTag args = new ListTag();
            for (var arg : finding.args()) args.add(StringTag.valueOf(arg));
            f.put("Args", args);
            list.add(f);
        }
        tag.put("Findings", list);
        log.incidents.put(id, tag);
        while (log.incidents.size() > LIMIT) log.incidents.remove(log.incidents.keySet().iterator().next());
        log.setDirty();
        com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.info(
            "Reactor incident #{} ({}) at {} {}: {}", id, facts.type(), dimension, pos.toShortString(), findings.get(0).code());
        return id;
    }

    @Nullable
    public static CompoundTag get(MinecraftServer server, int id) {
        Log log = log(server);
        return log == null ? null : log.incidents.get(id);
    }

    /** All incidents, newest first. */
    public static List<CompoundTag> all(MinecraftServer server) {
        Log log = log(server);
        if (log == null) return List.of();
        var out = new ArrayList<>(log.incidents.values());
        java.util.Collections.reverse(out);
        return out;
    }

    public static int clear(MinecraftServer server) {
        Log log = log(server);
        if (log == null) return 0;
        int n = log.incidents.size();
        log.incidents.clear();
        log.setDirty();
        return n;
    }

    /** An operator sees every incident; a player the incidents of their reactors and those they were near. */
    public static boolean mayView(ServerPlayer player, CompoundTag incident) {
        if (player.hasPermissions(2)) return true;
        if (incident.hasUUID("Owner") && incident.getUUID("Owner").equals(player.getUUID())) return true;
        ListTag nearby = incident.getList("Nearby", Tag.TAG_STRING);
        String name = player.getGameProfile().getName();
        for (int i = 0; i < nearby.size(); i++) if (name.equals(nearby.getString(i))) return true;
        return false;
    }

    /** Opens the audit screen of {@code incident} for {@code player}. */
    public static void open(ServerPlayer player, CompoundTag incident) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.miophas.singularity_iteration.common.network.ReactorAuditPacket(incident.copy()));
    }

    /** The main cause code of an incident. */
    public static String mainCause(CompoundTag incident) {
        ListTag list = incident.getList("Findings", Tag.TAG_COMPOUND);
        return list.isEmpty() ? "unknown" : list.getCompound(0).getString("Code");
    }

    /**
     * Drops the blackbox of incident {@code id} at the site: it cannot burn or blow up, never despawns
     * and glows, so it can be found after a blast.
     */
    public static void dropBlackbox(ServerLevel level, BlockPos pos, int id) {
        if (id <= 0) return;
        CompoundTag incident = get(level.getServer(), id);
        ItemStack stack = new ItemStack(com.miophas.singularity_iteration.common.item.reactor.mio_icif_reactors.REACTOR_BLACKBOX.get());
        CompoundTag data = new CompoundTag();
        data.putInt(ITEM_ID, id);
        if (incident != null) { data.putString(ITEM_CAUSE, mainCause(incident)); data.putLong(ITEM_TIME, incident.getLong("Time")); }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        entity.setInvulnerable(true);
        entity.setUnlimitedLifetime();
        entity.setGlowingTag(true);
        entity.setDeltaMovement(0, 0.25, 0);
        level.addFreshEntity(entity);
    }

    /** The incident id written on a blackbox item, or 0. */
    public static int idOf(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(ITEM_ID);
    }
}
