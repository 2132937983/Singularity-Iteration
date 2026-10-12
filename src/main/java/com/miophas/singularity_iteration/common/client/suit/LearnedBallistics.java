// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

/**
 * Universal ballistic calculator, learned profiles (0.1.8.0).
 *
 * <p>A weapon of a different mod (for example a Tinkers' Construct bow, whose speed comes from
 * its stats, or the Industrial Foregoing infinity trident) has no built-in profile. When the
 * local player fires, the client watches the new projectile for one tick and measures:
 * <ul>
 *   <li>launch speed: |v0 - shooter velocity| (the vertical shooter velocity only off the ground,
 *       as vanilla adds it);</li>
 *   <li>drag: horizontal speed after one tick / before;</li>
 *   <li>gravity: v0.y * drag - v1.y.</li>
 * </ul>
 * The samples are stored per item id with the draw time of the shot (at most
 * {@link #MAX_SAMPLES}). A weapon with a draw time gets the vanilla draw curve scaled to its
 * full-draw speed. The profiles are saved in {@code config/mio_icif-ballistics.json}.
 *
 * <p>Limits: a sample in a fluid or with less than 0.05 blocks per tick of horizontal speed is
 * not used. A weapon whose speed changes with its stats learns the current stats (the newest
 * samples win).
 */
@OnlyIn(Dist.CLIENT)
public final class LearnedBallistics {
    private LearnedBallistics() {}

    static final int MAX_SAMPLES = 12;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One shot: draw ticks, speed, gravity, drag. */
    record Sample(int use, double speed, double gravity, double drag) { }

    private static final Map<String, List<Sample>> SAMPLES = new HashMap<>();
    private static boolean loaded, dirty;
    private static long lastSave;

    /** A projectile of the local player, waiting for its first tick. */
    private record Watch(Projectile projectile, String item, int use, Vec3 v0, Vec3 shooter, int born) { }
    private static final List<Watch> WATCH = new ArrayList<>();

    // ---- what the player fired: the item that was in use and released, else the hand item
    private static String usingItem = "";
    private static int usingTicks, releasedAt = -100;
    private static String releasedItem = "";
    private static int releasedTicks;

    private static Path file() { return FMLPaths.CONFIGDIR.get().resolve("mio_icif-ballistics.json"); }

    private static void load() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (!Files.isRegularFile(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Map<String, List<Sample>> data = GSON.fromJson(r, new TypeToken<Map<String, List<Sample>>>() { }.getType());
            if (data != null) SAMPLES.putAll(data);
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.warn("Ballistic calculator: cannot read {}: {}", f, e.toString());
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) { GSON.toJson(SAMPLES, w); }
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.warn("Ballistic calculator: cannot write {}: {}", file(), e.toString());
        }
    }

    /** The learned profile of {@code stack} for a shot after {@code useTicks} of draw, or null. */
    @Nullable
    public static BallisticSolver.Profile profile(ItemStack stack, int useTicks) {
        load();
        List<Sample> list = SAMPLES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (list == null || list.isEmpty()) return null;
        double[] g = list.stream().mapToDouble(Sample::gravity).sorted().toArray();
        double[] d = list.stream().mapToDouble(Sample::drag).sorted().toArray();
        double gravity = g[g.length / 2], drag = Math.max(0.5, Math.min(1.0, d[d.length / 2]));
        double max = 0;
        int fullDraw = Integer.MAX_VALUE;
        boolean charged = false;
        for (Sample s : list) { max = Math.max(max, s.speed()); charged |= s.use() > 0; }
        for (Sample s : list) if (s.use() > 0 && s.speed() >= max * 0.97) fullDraw = Math.min(fullDraw, s.use());
        double speed = max;
        boolean ready = true;
        if (charged && fullDraw != Integer.MAX_VALUE) {
            // not drawing: show the full-draw path (as for the vanilla bow)
            if (useTicks > 0) {
                speed = max * com.miophas.singularity_iteration.core.api.item.BallisticProfile.drawPower(useTicks, fullDraw);
                ready = useTicks >= fullDraw;
            } else {
                ready = false;
            }
        }
        double waterDrag = gravity > 0.04 ? 0.6 : 0.8;
        return new BallisticSolver.Profile("LEARN", Math.max(0.05, speed), Math.max(0, gravity), drag, waterDrag, ready);
    }

    // ------------------------------------------------------------------ observation
    /** Client tick: remember what the player draws and releases; finish the watched projectiles. */
    public static void tick(LocalPlayer player) {
        if (player.isUsingItem()) {
            usingItem = id(player.getUseItem());
            usingTicks = player.getTicksUsingItem();
        } else if (!usingItem.isEmpty()) {
            releasedItem = usingItem;
            releasedTicks = usingTicks;
            releasedAt = player.tickCount;
            usingItem = "";
        }
        for (Iterator<Watch> it = WATCH.iterator(); it.hasNext(); ) {
            Watch w = it.next();
            Projectile p = w.projectile();
            if (p.isRemoved() || player.tickCount - w.born() > 4) { it.remove(); continue; }
            if (p.tickCount < 1) continue;
            it.remove();
            measure(w, p.getDeltaMovement());
        }
        if (dirty && System.currentTimeMillis() - lastSave > 10_000) {
            dirty = false;
            lastSave = System.currentTimeMillis();
            save();
        }
    }

    /** A projectile entered the client level. Called from the entity join event. */
    public static void onProjectile(Projectile p) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || p.getOwner() != player) return;
        String item;
        int use;
        if (player.tickCount - releasedAt <= 3 && !releasedItem.isEmpty()) {
            item = releasedItem;
            use = releasedTicks;
        } else {
            ItemStack hand = player.getMainHandItem();
            if (hand.isEmpty() || hand.getItem() instanceof BlockItem) hand = player.getOffhandItem();
            if (hand.isEmpty()) return;
            item = id(hand);
            use = 0;
        }
        Vec3 own = player.getDeltaMovement();
        WATCH.add(new Watch(p, item, use, p.getDeltaMovement(), new Vec3(own.x, player.onGround() ? 0 : own.y, own.z), player.tickCount));
        if (WATCH.size() > 16) WATCH.remove(0);
    }

    private static void measure(Watch w, Vec3 v1) {
        Projectile p = w.projectile();
        if (p.isInWater() || p.isInLava()) return;
        Vec3 v0 = w.v0();
        double h0 = Math.sqrt(v0.x * v0.x + v0.z * v0.z), h1 = Math.sqrt(v1.x * v1.x + v1.z * v1.z);
        if (h0 < 0.05) return;
        double drag = h1 / h0;
        double gravity = p.isNoGravity() ? 0 : v0.y * drag - v1.y;
        double speed = v0.subtract(w.shooter()).length();
        if (!(speed > 0.05) || drag > 1.05 || drag < 0.3 || Math.abs(gravity) > 0.5) return;   // thrust or a hit: not a free flight
        load();
        List<Sample> list = SAMPLES.computeIfAbsent(w.item(), k -> new ArrayList<>());
        list.add(new Sample(w.use(), round(speed), round(Math.max(0, gravity)), round(drag)));
        while (list.size() > MAX_SAMPLES) list.remove(0);
        dirty = true;
    }

    private static double round(double v) { return Math.round(v * 10000) / 10000.0; }

    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Forgets the learned profile of {@code item} (for example after a weapon rebuild). */
    public static void forget(String item) {
        load();
        if (SAMPLES.remove(item) != null) dirty = true;
    }
}
