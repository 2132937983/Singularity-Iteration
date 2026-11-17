// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets;
import com.miophas.singularity_iteration.common.armory.WingmanCommandPackets.Unit;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.armory.WingmanOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Client view of the quantum command (0.1.10.2): the last squad payload of the server, the
 * selection shared by the command wheel, the holo table and the squad HUD, and the order sender.
 */
public final class SquadClient {
    private SquadClient() {}

    @Nullable private static WingmanCommandPackets.Squad squad;
    private static int receivedAt = -1000;
    /** Client ticks (counted by {@link CommandWheelClient}). */
    static int ticks;
    /** Selected Wingman ({@link WingmanCommandPackets#ALL} = the whole squad). */
    private static UUID selected = WingmanCommandPackets.ALL;

    static void accept(WingmanCommandPackets.Squad s) {
        squad = s;
        receivedAt = ticks;
        if (!WingmanCommandPackets.ALL.equals(selected) && unit(selected) == null) selected = WingmanCommandPackets.ALL;
    }

    static void clear() {
        squad = null;
        selected = WingmanCommandPackets.ALL;
    }

    /** True while the command link works (a squad payload with the link on, less than 3 s old). */
    public static boolean commander() {
        WingmanCommandPackets.Squad s = squad;
        return s != null && s.commander() && ticks - receivedAt < 60;
    }

    public static boolean datalink() {
        WingmanCommandPackets.Squad s = squad;
        return s != null && s.datalink();
    }

    public static int squadMode() {
        WingmanCommandPackets.Squad s = squad;
        return s == null ? -1 : s.squadMode();
    }

    public static int holdRadius() {
        WingmanCommandPackets.Squad s = squad;
        return s == null || s.holdRadius() <= 0 ? 10 : s.holdRadius();
    }

    public static int suppressRadius() {
        WingmanCommandPackets.Squad s = squad;
        return s == null || s.suppressRadius() <= 0 ? 14 : s.suppressRadius();
    }

    /** 0.1.11.40 (U-8): the server's quantum recall cooldown (config), for the sweep of the recall sector. */
    public static int recallCooldownMax() {
        WingmanCommandPackets.Squad s = squad;
        return s == null || s.recallCooldownMax() <= 0 ? 600 : s.recallCooldownMax();
    }

    /** Ticks until the next quantum recall (counts down between payloads). */
    public static int recallCooldown() {
        WingmanCommandPackets.Squad s = squad;
        return s == null ? 0 : Math.max(0, s.recallCooldown() - (ticks - receivedAt));
    }

    /** All Wingmen of the squad view (loaded ones first, then the unloaded ones). */
    public static List<Unit> units() {
        WingmanCommandPackets.Squad s = squad;
        return s == null || !commander() ? List.of() : s.units();
    }

    /** The Wingmen that can take an order now: with the owner and active. */
    public static List<Unit> orderable() {
        List<Unit> out = new ArrayList<>();
        for (Unit u : units()) if (u.where() == 0 && u.state() == WingmanEntity.ACTIVE) out.add(u);
        return out;
    }

    @Nullable
    public static Unit unit(UUID id) {
        for (Unit u : units()) if (u.id().equals(id)) return u;
        return null;
    }

    public static UUID selected() { return selected; }

    /** The selected Wingman, or null for the whole squad. */
    @Nullable
    public static Unit selectedUnit() {
        return WingmanCommandPackets.ALL.equals(selected) ? null : unit(selected);
    }

    public static void select(UUID id) {
        selected = id == null ? WingmanCommandPackets.ALL : id;
    }

    /** Steps the selection through "all" and the orderable Wingmen. */
    public static void cycle(int direction) {
        List<UUID> options = new ArrayList<>();
        options.add(WingmanCommandPackets.ALL);
        for (Unit u : orderable()) options.add(u.id());
        int i = Math.max(0, options.indexOf(selected));
        selected = options.get(Math.floorMod(i + direction, options.size()));
    }

    /** Display index (0-based) of a Wingman in the squad view, or -1. */
    public static int index(Unit u) {
        List<Unit> list = units();
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(u.id())) return i;
        return -1;
    }

    /**
     * 0.1.11.40 (U-5): the number of a Wingman: its Armory suit (1..6). It stays the same while Wingmen deploy, return,
     * load or unload (before: the place in the list, so W2 could become W3). The table digits and the 1..9 keys use it.
     */
    public static int number(Unit u) {
        return Math.floorMod(u.set(), 32) + 1;
    }

    /** Short name: "W1" ..; two Wingmen of the same suit number (two Armories) get a letter: "W1", "W1b". */
    public static String label(Unit u) {
        String name = "W" + number(u);
        int same = 0;
        for (Unit other : units()) {
            if (other.id().equals(u.id())) break;
            if (number(other) == number(u)) same++;
        }
        return same == 0 ? name : name + (char) ('a' + same);
    }

    /** 0.1.11.40: the orderable Wingman with number {@code n} (a 1..9 key), or null. */
    @Nullable
    public static Unit byNumber(int n) {
        for (Unit u : orderable()) if (number(u) == n) return u;
        return null;
    }

    /** 0.1.11.40 (U-4): "unloaded" / "other dimension" for a Wingman that cannot take orders. */
    public static String whereText(Unit u) {
        return net.minecraft.network.chat.Component.translatable(u.where() == 2 ? "gui.mio_icif.command.where.unloaded"
            : "gui.mio_icif.command.where.other_dim").getString();
    }

    /**
     * 0.1.11.41 (decision 1 C): the flight time a Wingman has left from its suit charge and its upkeep reserve: "m:ss", "∞"
     * (no upkeep), the Armory paying, or "" (unknown: an unloaded Wingman).
     */
    public static String enduranceText(int seconds) {
        if (seconds == -1) return "∞";
        if (seconds == -2) return net.minecraft.network.chat.Component.translatable("gui.mio_icif.wingman.endurance.armory").getString();
        if (seconds < 0) return "";
        return com.miophas.singularity_iteration.common.armory.WingmanService.formatSeconds(seconds);
    }

    /** 0.1.11.41: the colour of {@link #enduranceText}: red under one minute, amber under three. */
    public static int enduranceColor(int seconds) {
        if (seconds < 0) return 0x9FDFFF;
        return seconds < 60 ? 0xFF5A4C : seconds < 180 ? 0xFFB333 : 0x9FDFFF;
    }

    /** 0.1.11.40 (U-4): the language key of the short order tag (chips). */
    public static String orderShortKey(WingmanOrder o) {
        return "gui.mio_icif.wingman.order_short." + o.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static WingmanOrder order(Unit u) {
        return WingmanOrder.byId(u.order());
    }

    /** Position of a Wingman: the interpolated entity when the client has it, else the last payload position. */
    public static Vec3 position(Unit u, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && u.entityId() >= 0) {
            Entity e = mc.level.getEntity(u.entityId());
            if (e instanceof WingmanEntity) return e.getPosition(partial);
        }
        return new Vec3(u.x(), u.y(), u.z());
    }

    /** The order point of a Wingman with an anchored order, else null. */
    @Nullable
    public static Vec3 orderPoint(Unit u) {
        return order(u).anchored() ? new Vec3(u.ox(), u.oy(), u.oz()) : null;
    }

    /** Sends one command (nothing when the server does not have the channel). */
    public static void send(byte action, UUID unit, byte arg, @Nullable Vec3 point, int target) {
        var connection = Minecraft.getInstance().getConnection();
        Vec3 p = point == null ? Vec3.ZERO : point;
        var payload = new WingmanCommandPackets.Command(action, unit, arg, p.x, p.y, p.z, target);
        if (connection != null && connection.hasChannel(payload.type())) PacketDistributor.sendToServer(payload);
    }
}
