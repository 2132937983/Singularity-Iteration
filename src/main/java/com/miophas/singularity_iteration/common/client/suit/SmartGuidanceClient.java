// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.fcs.MobTrack;
import com.miophas.singularity_iteration.common.suit.SmartGuidance;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Smart Trajectory Guidance, HUD and lock logic (0.1.8.0).
 *
 * <p>While the unit is on and the player holds a ranged weapon, a large search frame opens in
 * the middle of the view ({@link #FRAME_W} x {@link #FRAME_H} of the screen). A hostile creature
 * (or one that attacks the player, or the last one the player hit) inside the frame, in line of
 * sight and within {@link SmartGuidance#RANGE} blocks, builds up a lock in {@link #LOCK_TICKS}
 * ticks: three triangles close in on it. A full lock shows a red diamond and its number. The
 * lock decays in {@link #DECAY_TICKS} ticks outside the frame. At most
 * {@link SmartGuidance#MAX_LOCKS} locks; the client sends the locked ids when they change.
 *
 * <p>0.1.10.23 (P5): a full lock whose creature goes behind cover or out of the frame stays in
 * memory for {@link SmartGuidance#lockMemory} ticks (1 s, Mk.II 2 s) at its predicted position
 * ({@code fcs.MobTrack}, a hollow diamond "MEM"): it is locked again at once when the creature
 * shows up inside the gate around that position, and dropped when the memory runs out, the creature
 * shows up elsewhere (a teleport) or the track keeps missing. Before, a lock was lost on the first
 * tick out of sight and took 0.6 s in the frame to come back. (P6): the locks are ordered by the
 * {@code fcs.TargetPriority} score (nearest first before), so the lock limit keeps the most
 * dangerous creatures.
 */
@OnlyIn(Dist.CLIENT)
public final class SmartGuidanceClient {
    private SmartGuidanceClient() {}

    static final float FRAME_W = 0.46F, FRAME_H = 0.36F;
    static final int LOCK_TICKS = 12, DECAY_TICKS = 6, MAX_CANDIDATES = 24;
    private static final int RED = 0xFF4D6D, WHITE = 0xFFFFFF;

    private static final Map<Integer, Float> PROGRESS = new HashMap<>();
    private static final List<Integer> LOCKED = new ArrayList<>();
    /** 0.1.10.23 (P5): the track of each full lock. */
    private static final Map<Integer, MobTrack> TRACKS = new HashMap<>();
    private static int[] sent = new int[0];
    private static boolean active;

    static boolean active() { return active; }

    public static void tick(Minecraft mc, LocalPlayer player) {
        boolean on = FcsClientConfig.hudEnabled() && FcsState.active(SuitModuleType.SMART_GUIDANCE) && holdsRanged(player);
        if (!on) {
            if (active || sent.length > 0) { PROGRESS.clear(); LOCKED.clear(); TRACKS.clear(); send(new int[0]); }
            active = false;
            return;
        }
        active = true;
        var window = mc.getWindow();
        float w = window.getGuiScaledWidth(), h = window.getGuiScaledHeight();
        float x0 = w * (0.5F - FRAME_W / 2), x1 = w * (0.5F + FRAME_W / 2), y0 = h * (0.5F - FRAME_H / 2), y1 = h * (0.5F + FRAME_H / 2);
        double range = SmartGuidance.RANGE;
        List<LivingEntity> candidates = new ArrayList<>(mc.level.getEntitiesOfClass(LivingEntity.class,
            new AABB(player.position(), player.position()).inflate(range), e -> eligible(player, e)));
        candidates.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        java.util.Set<Integer> inside = new java.util.HashSet<>();
        for (int i = 0; i < candidates.size() && i < MAX_CANDIDATES; i++) {
            LivingEntity e = candidates.get(i);
            float[] p = FcsProjection.project(e.getBoundingBox().getCenter());
            if (p == null || p[0] < x0 || p[0] > x1 || p[1] < y0 || p[1] > y1 || !player.hasLineOfSight(e)) continue;
            inside.add(e.getId());
        }
        // 0.1.10.23 (P5): full locks keep a track; a creature seen again elsewhere (a teleport) loses its lock
        int memoryTicks = SmartGuidance.lockMemory(player);
        for (Iterator<Map.Entry<Integer, MobTrack>> it = TRACKS.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity e = mc.level.getEntity(en.getKey());
            MobTrack track = en.getValue();
            if (!(e instanceof LivingEntity l) || !l.isAlive()) { it.remove(); continue; }
            if (inside.contains(en.getKey())) {
                if (track.unseen() > 0 && !track.accepts(e.getX(), e.getY(), e.getZ())) {
                    it.remove();
                    PROGRESS.remove(en.getKey());
                    inside.remove(en.getKey());
                    continue;
                }
                track.observe(e.getX(), e.getY(), e.getZ());
            } else {
                track.coast();
            }
            if (track.lost(memoryTicks)) { it.remove(); PROGRESS.put(en.getKey(), 0.99F); }
        }
        float step = 1.0F / lockTicks(player);
        for (int id : inside) PROGRESS.merge(id, step, (a, b) -> Math.min(1, a + b));
        for (Iterator<Map.Entry<Integer, Float>> it = PROGRESS.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            boolean alive = mc.level.getEntity(en.getKey()) instanceof LivingEntity l && l.isAlive();
            if (!alive) { it.remove(); TRACKS.remove(en.getKey()); continue; }
            if (!inside.contains(en.getKey()) && !TRACKS.containsKey(en.getKey())) {
                float next = en.getValue() - 1.0F / DECAY_TICKS;
                if (next <= 0) it.remove(); else en.setValue(next);
            }
        }
        List<Integer> locked = new ArrayList<>();
        for (var en : PROGRESS.entrySet()) {
            if (en.getValue() < 1) continue;
            locked.add(en.getKey());
            if (!TRACKS.containsKey(en.getKey()) && mc.level.getEntity(en.getKey()) instanceof LivingEntity l) {
                MobTrack track = new MobTrack(com.miophas.singularity_iteration.common.suit.TargetRating.flies(l));
                track.observe(l.getX(), l.getY(), l.getZ());
                TRACKS.put(en.getKey(), track);
            }
        }
        // 0.1.10.23 (P6): highest priority first (nearest first when the predictor has no score)
        locked.sort((a, b) -> {
            double pa = SensorTracker.priority(a), pb = SensorTracker.priority(b);
            if (!Double.isNaN(pa) && !Double.isNaN(pb) && pa != pb) return Double.compare(pb, pa);
            var ea = mc.level.getEntity(a); var eb = mc.level.getEntity(b);
            return Double.compare(ea == null ? 1e9 : ea.distanceToSqr(player), eb == null ? 1e9 : eb.distanceToSqr(player));
        });
        int max = SmartGuidance.maxLocks(player);                     // 0.1.10.1 Mk.II and Predictive Guidance
        while (locked.size() > max) TRACKS.remove(locked.remove(locked.size() - 1));
        for (int id : locked) {
            if (!LOCKED.contains(id)) {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    com.miophas.singularity_iteration.common.registry.mio_icif_sounds.FCS_LOCK.get(), 1.35F, 0.4F));
            }
        }
        LOCKED.clear();
        LOCKED.addAll(locked);
        int[] ids = locked.stream().mapToInt(Integer::intValue).toArray();
        if (!Arrays.equals(ids, sent)) send(ids);
    }

    /**
     * 0.1.10.1: ticks to a full lock: {@link #LOCK_TICKS}, 30 % less with the Ballistic Sync synergy,
     * and 2 inside one's own time dilation (the Frozen Volley combo locks at once).
     */
    static int lockTicks(LocalPlayer player) {
        var q = com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient.sync();
        if (q != null && q.dilationTicks() > 0) return 2;
        boolean sync = com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(player,
            com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.BALLISTIC_SYNC);
        return sync ? Math.round(LOCK_TICKS * 0.7F) : LOCK_TICKS;
    }

    /** 0.1.10.1: the locked entity ids, nearest first (Ballistic Sync draws their guided paths). */
    public static List<Integer> locked() {
        return List.copyOf(LOCKED);
    }

    private static boolean eligible(LocalPlayer player, LivingEntity e) {
        if (e == player || !e.isAlive() || e.isSpectator() || player.isAlliedTo(e)) return false;
        if (e instanceof Player) return false;                                   // no player locks on the client side
        if (e instanceof Enemy) return true;
        if (e instanceof Mob mob && mob.getTarget() == player) return true;
        return e == player.getLastHurtMob() || e == player.getLastHurtByMob();
    }

    private static boolean holdsRanged(LocalPlayer player) {
        return ranged(player, player.getMainHandItem()) || ranged(player, player.getOffhandItem());
    }

    private static boolean ranged(LocalPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem) return true;
        return BallisticSolver.profile(player, stack) != null;
    }

    private static void send(int[] ids) {
        sent = ids;
        var connection = Minecraft.getInstance().getConnection();
        var payload = new SmartGuidance.Locks(ids);
        if (connection != null && connection.hasChannel(payload.type())) PacketDistributor.sendToServer(payload);
    }

    public static void clear() {
        PROGRESS.clear();
        LOCKED.clear();
        TRACKS.clear();
        sent = new int[0];
        active = false;
    }

    // ------------------------------------------------------------------ HUD
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.options.hideGui) return;
        float w = g.guiWidth(), h = g.guiHeight();
        float fx = w * (0.5F - FRAME_W / 2), fy = h * (0.5F - FRAME_H / 2), fw = w * FRAME_W, fh = h * FRAME_H;
        long time = mc.level.getGameTime();
        float pulse = 0.55F + 0.2F * (float) Math.sin(time * 0.25F);
        FcsDraw.brackets(g, fx, fy, fw, fh, Math.min(fw, fh) * 0.12F, 1.5F, FcsDraw.argb(RED, 0.85F * pulse + 0.15F));
        // fine scan line moving down the frame
        float scan = fy + fh * ((time % 40 + delta.getGameTimeDeltaPartialTick(false)) / 40F);
        FcsDraw.line(g, fx + 4, scan, fx + fw - 4, scan, 0.6F, FcsDraw.argb(RED, 0.25F));
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int memoryTicks = mc.player == null ? MobTrack.MEMORY_MK1 : SmartGuidance.lockMemory(mc.player);
        for (var en : PROGRESS.entrySet()) {
            if (!(mc.level.getEntity(en.getKey()) instanceof LivingEntity e)) continue;
            MobTrack track = TRACKS.get(en.getKey());
            if (track != null && track.unseen() > 0 && LOCKED.contains(en.getKey())) {
                // 0.1.10.23 (P5): memory: a hollow diamond at the predicted position, the memory left as a bar
                double[] at = new double[3];
                track.predicted(at);
                float[] mp = FcsProjection.project(new Vec3(at[0], at[1] + e.getBbHeight() * 0.55, at[2]));
                if (mp == null) continue;
                float left = 1 - track.unseen() / (float) memoryTicks;
                int col = FcsDraw.argb(RED, 0.55F + 0.35F * left);
                FcsDraw.line(g, mp[0], mp[1] - 6, mp[0] + 6, mp[1], 1F, col);
                FcsDraw.line(g, mp[0] + 6, mp[1], mp[0], mp[1] + 6, 1F, col);
                FcsDraw.line(g, mp[0], mp[1] + 6, mp[0] - 6, mp[1], 1F, col);
                FcsDraw.line(g, mp[0] - 6, mp[1], mp[0], mp[1] - 6, 1F, col);
                FcsDraw.text(g, "MEM " + (LOCKED.indexOf(en.getKey()) + 1), mp[0] + 9, mp[1] - 9, col, 0.55F);
                g.fill((int) (mp[0] - 6), (int) (mp[1] + 9), (int) (mp[0] - 6 + 12 * Math.max(0, left)), (int) (mp[1] + 10), col);
                continue;
            }
            Vec3 c = e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
            float[] p = FcsProjection.project(c);
            if (p == null) continue;
            float k = en.getValue();
            boolean locked = LOCKED.contains(en.getKey());
            float r = locked ? 7 : 22 - 15 * k;
            float spin = locked ? 0 : time * 0.15F + partial * 0.15F;
            int color = locked ? FcsDraw.argb(RED, 1.0F) : FcsDraw.argb(lerp(WHITE, RED, k), 0.5F + 0.5F * k);
            for (int i = 0; i < 3; i++) {
                double a = spin + i * Math.PI * 2 / 3;
                float tx = p[0] + (float) Math.cos(a) * r, ty = p[1] + (float) Math.sin(a) * r;
                float ix = p[0] + (float) Math.cos(a) * (r - 4), iy = p[1] + (float) Math.sin(a) * (r - 4);
                float px = (float) -Math.sin(a) * 3, py = (float) Math.cos(a) * 3;
                FcsDraw.triangle(g, ix, iy, tx + px, ty + py, tx - px, ty - py, color);
            }
            if (locked) {
                FcsDraw.quad(g, p[0], p[1] - 4, p[0] + 4, p[1], p[0], p[1] + 4, p[0] - 4, p[1], FcsDraw.argb(RED, 0.9F));
                FcsDraw.text(g, String.valueOf(LOCKED.indexOf(en.getKey()) + 1), p[0] + 9, p[1] - 9, FcsDraw.argb(RED, 1.0F), 0.6F);
            }
        }
        String label = "SMART LOCK " + LOCKED.size() + "/" + (mc.player == null ? SmartGuidance.MAX_LOCKS : SmartGuidance.maxLocks(mc.player));
        FcsDraw.textCentered(g, label, w / 2, fy + fh + 4, FcsDraw.argb(RED, 0.9F), 0.6F);
    }

    private static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }
}
