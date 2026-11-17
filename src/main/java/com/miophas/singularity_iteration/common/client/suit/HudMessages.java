// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.5: the situational messages of the passive suit HUD. Each source posts a message while its situation holds
 * (a threat, an incoming projectile, a charge about to blow, low visor energy, a sonar result); the HUD shows the two
 * with the highest priority as a text log at the top of the view and a small status line at the bottom left. A
 * message stays {@link #HOLD_MS} after its last post. The detail view lists the recent ones.
 *
 * <p>Priorities: threat 100 &gt; incoming 95 &gt; blast 90 &gt; ground 80 &gt; energy 60 &gt; sonar 20.
 */
public final class HudMessages {
    private HudMessages() {}

    /** Levels: note, warning, critical. */
    public static final int NOTE = 0, WARN = 1, CRIT = 2;
    public static final int P_THREAT = 100, P_INCOMING = 95, P_BLAST = 90, P_GROUND = 80, P_ENERGY = 60, P_SONAR = 20;
    /** How long a message stays after its last post, in milliseconds. */
    public static final long HOLD_MS = 1500;
    /** Messages on screen at once. */
    public static final int SHOWN = 2;

    /** A live message: a label ("Threat") and its detail line. */
    public record Msg(String id, int priority, int level, Component label, Component detail, long until) { }
    /** A logged message for the detail view: the game time it first came up. */
    public record Logged(long gameTime, int level, Component label, Component detail) { }

    private static final Map<String, Msg> LIVE = new LinkedHashMap<>();
    private static final Deque<Logged> HISTORY = new ArrayDeque<>();
    private static final int HISTORY_MAX = 6;

    /** Posts or refreshes the message {@code id}. */
    public static void post(String id, int priority, int level, Component label, Component detail) {
        long now = System.currentTimeMillis();
        Msg old = LIVE.put(id, new Msg(id, priority, level, label, detail, now + HOLD_MS));
        if (old == null || old.level < level) {
            var mc = Minecraft.getInstance();
            HISTORY.addFirst(new Logged(mc.level == null ? 0 : mc.level.getGameTime(), level, label, detail));
            while (HISTORY.size() > HISTORY_MAX) HISTORY.removeLast();
        }
    }

    /** The live messages, most important first. */
    public static List<Msg> live() {
        long now = System.currentTimeMillis();
        LIVE.values().removeIf(m -> m.until < now);
        List<Msg> list = new ArrayList<>(LIVE.values());
        list.sort(Comparator.comparingInt(Msg::priority).reversed());
        return list;
    }

    public static List<Logged> history() { return List.copyOf(HISTORY); }

    public static void clear() { LIVE.clear(); HISTORY.clear(); }

    // ------------------------------------------------------------------ sources

    /** Client tick: posts the messages of the situations that hold now. {@code hud} is the HUD switch. */
    public static void tick(Minecraft mc, LocalPlayer player, boolean hud) {
        if (!hud) return;
        threats(mc, player);
        if (FcsState.active(SuitModuleType.THREAT_SENSOR)) incoming(player);
        if (FcsState.active(SuitModuleType.BLAST_WARNING)) blasts(player);
        energy(player);
        if (FcsState.active(SuitModuleType.ORE_SCANNER)) sonar();
        if (ManeuverModeClient.isFlying()) flight(mc, player);
    }

    /** 0.1.11.5: in flight: the ground coming up fast (less than 3 s to impact), and a low chest charge. */
    private static void flight(Minecraft mc, LocalPlayer player) {
        double vs = (player.getY() - player.yo) * 20;
        if (vs < -6 && mc.level != null) {
            var from = player.position();
            var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(from, from.add(0, -64, 0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.ANY, player));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                double drop = from.y - hit.getLocation().y, seconds = drop / -vs;
                if (seconds < 3) post("ground", P_GROUND, seconds < 1.5 ? CRIT : WARN, Component.translatable("hud.mio_icif.msg.ground"),
                    Component.translatable("hud.mio_icif.msg.ground.detail", Math.round(drop), String.format(java.util.Locale.ROOT, "%.1f", seconds)));
            }
        }
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor armor) {
            long max = armor.getMaxEnergy(chest);
            float f = max <= 0 ? 1 : SuitModules.energy(chest) / (float) max;
            if (f < 0.2F) post("flight_energy", P_ENERGY + 1, f < 0.08F ? CRIT : WARN, Component.translatable("hud.mio_icif.msg.flight_energy"),
                Component.translatable("hud.mio_icif.msg.energy.detail", AsciiHud.bar(f, 10), Math.round(f * 100)));
        }
    }

    private static void threats(Minecraft mc, LocalPlayer player) {
        var map = SuitSensorData.threats();
        if (map.isEmpty() || mc.level == null) return;
        int red = 0, yellow = 0;
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (var e : map.int2ByteEntrySet()) {
            Entity en = mc.level.getEntity(e.getIntKey());
            if (en == null) continue;
            if (e.getByteValue() == 2) red++; else yellow++;
            double d = en.distanceTo(player);
            if (d < best && (e.getByteValue() == 2 || red == 0)) { best = d; nearest = en; }
        }
        if (nearest == null) return;
        Component where = direction(player, nearest.position());
        post("threat", P_THREAT, red > 0 ? CRIT : WARN, Component.translatable("hud.mio_icif.msg.threat"),
            Component.translatable("hud.mio_icif.msg.threat.detail", where, red + yellow, Math.round(best)));
    }

    private static void incoming(LocalPlayer player) {
        var alerts = ProjectileWatch.alerts();
        if (alerts.isEmpty()) return;
        int hits = 0;
        float soonest = Float.MAX_VALUE;
        Vec3 from = null;
        for (var a : alerts) {
            if (a.warning().hit()) hits++;
            if (a.seconds() < soonest) { soonest = a.seconds(); from = a.projectile().position(); }
        }
        post("incoming", P_INCOMING, hits > 0 ? CRIT : WARN, Component.translatable("hud.mio_icif.msg.incoming"),
            Component.translatable("hud.mio_icif.msg.incoming.detail", from == null ? Component.empty() : direction(player, from),
                alerts.size(), String.format(java.util.Locale.ROOT, "%.1f", soonest)));
    }

    private static void blasts(LocalPlayer player) {
        SensorTracker.Blast worst = null;
        double dist = 0;
        for (var b : SensorTracker.blasts()) {
            double d = b.entity().distanceTo(player);
            if (worst == null || b.seconds() < worst.seconds()) { worst = b; dist = d; }
        }
        if (worst == null) return;
        boolean inside = dist <= worst.radius();
        post("blast", P_BLAST, inside ? CRIT : WARN, Component.translatable("hud.mio_icif.msg.blast"),
            Component.translatable(inside ? "hud.mio_icif.msg.blast.inside" : "hud.mio_icif.msg.blast.detail",
                direction(player, worst.entity().position()), Math.round(dist), String.format(java.util.Locale.ROOT, "%.1f", worst.seconds())));
    }

    private static void energy(LocalPlayer player) {
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor armor)) return;
        long max = armor.getMaxEnergy(helmet);
        if (max <= 0) return;
        float f = SuitModules.energy(helmet) / (float) max;
        if (f >= 0.2F) return;
        post("energy", P_ENERGY, f < 0.08F ? CRIT : WARN, Component.translatable("hud.mio_icif.msg.energy"),
            Component.translatable("hud.mio_icif.msg.energy.detail", AsciiHud.bar(f, 10), Math.round(f * 100)));
    }

    private static void sonar() {
        var hits = OreScanner.hits();
        if (hits.isEmpty() || System.currentTimeMillis() - OreScanner.echoStartMs() > 3000) return;
        post("sonar", P_SONAR, NOTE, Component.translatable("hud.mio_icif.msg.sonar"),
            Component.translatable("hud.mio_icif.msg.sonar.detail", hits.size()));
    }

    /** The direction of {@code at} relative to where the wearer looks: ahead, ahead right, right, ... (8 sectors). */
    static Component direction(LocalPlayer player, Vec3 at) {
        double dx = at.x - player.getX(), dz = at.z - player.getZ();
        float world = (float) Math.toDegrees(Math.atan2(-dx, dz));   // Minecraft yaw of the direction
        float rel = Mth.wrapDegrees(world - player.getYRot());          // 0 ahead, + to the right
        int sector = Math.floorMod(Math.round(rel / 45F), 8);
        String[] keys = {"ahead", "ahead_right", "right", "behind_right", "behind", "behind_left", "left", "ahead_left"};
        return Component.translatable("hud.mio_icif.dir." + keys[sector]);
    }

    // ------------------------------------------------------------------ drawing

    /** The message log at the top of the view: at most {@link #SHOWN} lines. */
    public static void render(GuiGraphics g, int w, int top, float scale) {
        List<Msg> list = live();
        if (list.isEmpty()) return;
        int n = Math.min(SHOWN, list.size());
        float lh = AsciiHud.LINE * scale;
        // one width for both lines, measured
        float labelW = 0, wid = 0;
        for (int i = 0; i < n; i++) labelW = Math.max(labelW, AsciiHud.width(list.get(i).label, scale));
        float prefix = AsciiHud.width("> !! ", scale);
        for (int i = 0; i < n; i++) wid = Math.max(wid, prefix + labelW + AsciiHud.width("  ", scale) + AsciiHud.width(list.get(i).detail, scale));
        float x = (w - wid) / 2F, pad = 3 * scale;
        g.fill((int) (x - pad), (int) (top - pad), (int) (x + wid + pad), (int) (top + n * lh + pad - 2 * scale), AsciiHud.GLASS);
        float pulse = 0.65F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 120.0);
        for (int i = 0; i < n; i++) {
            Msg m = list.get(i);
            float y = top + i * lh;
            int col = m.level == CRIT ? AsciiHud.CRIT : m.level == WARN ? AsciiHud.WARN : AsciiHud.OK;
            String mark = m.level == CRIT ? "!!" : m.level == WARN ? "! " : "· ";
            AsciiHud.text(g, ">", x, y, AsciiHud.DIM, scale);
            FcsDraw.text(g, AsciiHud.t(mark), x + AsciiHud.width("> ", scale), y, m.level == CRIT ? FcsDraw.argb(col, pulse) : FcsDraw.argb(col, 1F), scale);
            AsciiHud.text(g, m.label, x + prefix, y, col, scale);
            AsciiHud.text(g, m.detail, x + prefix + labelW + AsciiHud.width("  ", scale), y, AsciiHud.TEXT, scale);
        }
        if (list.size() > n) AsciiHud.textRight(g, "+" + (list.size() - n), x + wid, top + n * lh, AsciiHud.DIM, scale * 0.8F);
    }

    /** The status line at the bottom left: "visor OK" or "visor · n messages". */
    public static void status(GuiGraphics g, int h, float scale) {
        int n = live().size();
        int top = 0;
        for (Msg m : live()) top = Math.max(top, m.level);
        int col = n == 0 ? AsciiHud.DIM : top == CRIT ? AsciiHud.CRIT : top == WARN ? AsciiHud.WARN : AsciiHud.OK;
        Component c = n == 0 ? Component.translatable("hud.mio_icif.status.ok") : Component.translatable("hud.mio_icif.status.messages", n);
        float y = h - 14 - AsciiHud.LINE * scale;
        AsciiHud.text(g, "■ ", 12, y, col, scale);
        AsciiHud.text(g, c, 12 + AsciiHud.width("■ ", scale), y, col, scale);
    }
}
