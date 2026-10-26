// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.fcs.ProjectileWarning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 0.1.10.23 (FCS P7): the incoming projectile warning of the Threat Sensor. Each client tick it looks at the
 * projectiles within {@link #RANGE} blocks that the wearer did not fire (arrows and tridents, thrown items, llama spit,
 * fireballs and wither skulls) and keeps those that come within {@link #WARN_MISS} blocks of the wearer in the next
 * 2 s ({@code fcs.ProjectileWarning}: their own gravity, drag and push; the wearer keeps its motion). The HUD draws a
 * marker at the bearing of each with its countdown, and an arrow to step aside for the ones that hit; the first tick a
 * hit is seen it beeps. Homing projectiles (shulker bullets) and rockets are left out: their path is not ballistic.
 *
 * <p>Offline test ({@code verify-si1123}): of the skeleton arrows seen one tick after the shot, with half of the
 * wearers changing their walk during the flight, 98.6 % of the warned hits hit and 98.4 % of the hits were warned (ghast
 * fireballs 91 %); stepping along the arrow after 0.2 s avoided 77 % of the arrow hits (44 % in a random direction).
 * Fireballs are slow: any step avoided 98 %.
 */
@OnlyIn(Dist.CLIENT)
public final class ProjectileWatch {
    private ProjectileWatch() {}

    /** Search box half size (blocks). */
    public static final double RANGE = 48;
    /** Projectiles that pass farther than this (blocks from the grown box) are not shown. */
    public static final double WARN_MISS = 1.5;
    /** At most this many warnings, soonest first. */
    public static final int MAX = 12;

    /** One warning: the projectile, its closest approach, and whether it is new this tick. */
    public record Alert(Projectile projectile, ProjectileWarning.Warning warning) {
        /** Seconds to the closest approach. */
        public float seconds() { return (float) (warning.ticks() / 20.0); }
    }

    private static List<Alert> alerts = List.of();
    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet warnedHits = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    private static int beepCooldown;

    /** The warnings of the last tick, soonest first. */
    public static List<Alert> alerts() { return alerts; }

    public static void clear() {
        alerts = List.of();
        warnedHits.clear();
    }

    /** One client tick ({@code on}: the Threat Sensor is active on the HUD). */
    public static void tick(Minecraft mc, ClientLevel level, LocalPlayer player, boolean on) {
        if (beepCooldown > 0) beepCooldown--;
        if (!on) { if (!alerts.isEmpty()) clear(); return; }
        List<Alert> out = new ArrayList<>();
        Vec3 own = player.getDeltaMovement();
        double[] feet = {player.getX(), player.getY(), player.getZ()};
        double[] wearer = {own.x, player.onGround() ? 0 : own.y, own.z};
        it.unimi.dsi.fastutil.ints.IntOpenHashSet hitsNow = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
        boolean newHit = false;
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(RANGE), p -> p.isAlive() && p.getOwner() != player)) {
            ProjectileWarning.Ballistics b = ballistics(p);
            if (b == null) continue;
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 1.0E-4) continue;                   // stuck in a block
            ProjectileWarning.Warning w = ProjectileWarning.closest(new double[] {p.getX(), p.getY(), p.getZ()}, new double[] {v.x, v.y, v.z}, b,
                feet, wearer, player.getBbWidth() / 2, player.getBbHeight());
            if (w == null || w.miss() > WARN_MISS) continue;
            out.add(new Alert(p, w));
            if (w.hit()) {
                hitsNow.add(p.getId());
                if (!warnedHits.contains(p.getId())) newHit = true;
            }
        }
        out.sort((a, c) -> Double.compare(a.warning().ticks(), c.warning().ticks()));
        alerts = out.size() > MAX ? List.copyOf(out.subList(0, MAX)) : List.copyOf(out);
        warnedHits.clear();
        warnedHits.addAll(hitsNow);
        if (newHit && beepCooldown == 0) {
            beepCooldown = 6;
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.FCS_BLAST_BEEP.get(), 1.7F, 0.55F));
        }
    }

    /** The rules of a projectile, or null for one that is not ballistic. */
    static ProjectileWarning.Ballistics ballistics(Projectile p) {
        if (p instanceof AbstractArrow) return ProjectileWarning.ARROW;
        if (p instanceof ThrowableProjectile) return ProjectileWarning.THROWN;
        if (p instanceof LlamaSpit) return ProjectileWarning.SPIT;
        if (p instanceof AbstractHurtingProjectile h) {
            double margin = p instanceof LargeFireball ? 0.5 : 0.3;
            return new ProjectileWarning.Ballistics(0, 0.95, h.accelerationPower, margin);
        }
        return null;
    }
}
