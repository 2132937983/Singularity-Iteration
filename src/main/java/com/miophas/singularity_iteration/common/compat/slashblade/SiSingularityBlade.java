// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.armory.blade.BladeCore;
import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.armory.blade.BladeResources;
import com.miophas.singularity_iteration.common.armory.blade.CollabBlade;
import com.miophas.singularity_iteration.common.armory.blade.SingularityFxPacket;
import com.miophas.singularity_iteration.common.compat.slashblade.SiSlashBladeContent.Timeline;
import com.miophas.singularity_iteration.common.compat.slashblade.SiSlashBladeContent.Variant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.1.11.236 (SlashBlade collab B4): the singularity blade's slash art and special effect.
 *
 * <ul>
 *   <li><b>Event horizon</b> ({@code mio_icif:event_horizon}): the containment opens for a moment and the micro-singularity
 *       is set loose where the wielder looks (at most 7 blocks, stopped by blocks). On the screens nearby the picture bends
 *       round it into an Einstein ring with a black horizon and a magenta photon ring ({@code SingularityLens}); a tilted
 *       accretion disk of magenta and hot orange dust turns round it; it pulls in everything within 8 blocks, and what
 *       comes within 2.2 is torn (magic damage every 5 ticks). On tick 22 it evaporates in a Hawking burst: a magenta
 *       flash, a hit of the blade on everything within 5 blocks, thrown outward. Just: the tearing is critical. Super: the
 *       horizon is larger, holds 10 ticks longer and bursts within 7 blocks. Its EU and mass are paid in
 *       {@link SiSlashBladeServer#onSlashArt} (B1).</li>
 *   <li><b>Tidal shear</b> ({@code mio_icif:tidal_shear}): a hit adds magic damage of mass / 20 (at most 5,
 *       {@link BladeResources#tidalDamage}) and draws the target a little toward the wielder.</li>
 * </ul>
 */
final class SiSingularityBlade {
    private SiSingularityBlade() {}

    static final int MAGENTA = 0xFF2BD6, HOT = 0xFF8A3D, WHITE = 0xFFE6FA;
    private static final Map<UUID, Vec3> CENTER = new ConcurrentHashMap<>();

    static ComboState eventHorizon(Variant v) {
        boolean big = v == Variant.SUPER;
        int burst = big ? 32 : 22;
        float radius = big ? 1.3F : 0.9F;
        double pull = big ? 10 : 8, burstRange = big ? 7 : 5;
        Timeline t = new Timeline();
        t.put(1, user -> {
            if (!(user.level() instanceof ServerLevel level)) return;
            Vec3 c = lookPoint(user, 7.0);
            CENTER.put(user.getUUID(), c);
            PacketDistributor.sendToPlayersNear(level, null, c.x, c.y, c.z, 96,
                new SingularityFxPacket(c.x, c.y, c.z, radius, burst - 1));
            SiSlashBladeContent.sound(user, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 0.6F);
            SiSlashBladeContent.sound(user, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.5F);
        });
        for (int tick = 2; tick < burst; tick++) {
            final int k = tick;
            t.put(tick, user -> {
                if (!(user.level() instanceof ServerLevel level)) return;
                Vec3 c = CENTER.get(user.getUUID());
                if (c == null) return;
                for (LivingEntity e : SiSlashBladeContent.targets(user, c, pull)) {
                    Vec3 to = c.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
                    double d = to.length();
                    if (d < 0.3) continue;
                    double s = 0.14 * (1 - d / pull);
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.8).add(to.normalize().scale(Math.max(0.03, s))));
                    e.hurtMarked = true;
                    if (k % 5 == 0 && d <= 2.2) {
                        int inv = e.invulnerableTime;
                        e.invulnerableTime = 0;
                        e.hurt(user.damageSources().magic(), v == Variant.JUST ? 4.5F : 3F);
                        e.invulnerableTime = Math.max(inv, e.invulnerableTime);
                    }
                }
            });
        }
        t.put(burst, user -> {
            if (!(user.level() instanceof ServerLevel level)) return;
            Vec3 c = CENTER.remove(user.getUUID());
            if (c == null) return;
            for (LivingEntity e : SiSlashBladeContent.targets(user, c, burstRange)) {
                AttackManager.doMeleeAttack(user, e, true, true, big ? 1.5F : 1.0F);
                Vec3 out = e.position().subtract(c);
                e.setDeltaMovement(e.getDeltaMovement().add(out.lengthSqr() < 1.0E-4 ? new Vec3(0, 0.6, 0) : out.normalize().scale(1.1).add(0, 0.4, 0)));
                e.hurtMarked = true;
            }
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.5F);
        });
        return SiSlashBladeContent.state(2200, 2277, t, "void_slash_sheath");
    }

    /** The point the user looks at, stopped by blocks, at most {@code reach} away. */
    static Vec3 lookPoint(LivingEntity user, double reach) {
        Vec3 eye = user.getEyePosition();
        Vec3 end = eye.add(user.getLookAngle().scale(reach));
        var hit = user.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        return hit.getLocation().subtract(user.getLookAngle().scale(0.6));
    }

    static boolean shears(ItemStack stack) {
        return BladeStateAccess.of(stack).map(s -> s.hasSpecialEffect(SiSlashBladeContent.TIDAL_SHEAR.getId())).orElse(false);
    }

    @SubscribeEvent
    static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser(), target = event.getTarget();
        if (user == null || target == null || !(user.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getBlade();
        if (CollabBlade.of(stack) != CollabBlade.SINGULARITY || !shears(stack)) return;
        BladeCore c = CollabBlade.core(stack);
        float extra = BladeResources.tidalDamage(c);
        if (extra > 0) {
            int inv = target.invulnerableTime;
            target.invulnerableTime = 0;
            target.hurt(user.damageSources().magic(), extra);
            target.invulnerableTime = Math.max(inv, target.invulnerableTime);
        }
        Vec3 to = user.position().subtract(target.position());
        if (to.lengthSqr() > 1.0E-4) {
            target.setDeltaMovement(target.getDeltaMovement().add(to.normalize().scale(0.2)));
            target.hurtMarked = true;
        }
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 48,
            BladeFxPacket.of(BladeFxPacket.SINGULARITY_TIDAL, at, to.lengthSqr() > 1.0E-4 ? to.normalize() : Vec3.ZERO, c.mass(), 0F, 0));
    }

    @SubscribeEvent
    static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.isSelected() || level.getGameTime() % 10 != 0) return;
        if (!(event.getEntity() instanceof LivingEntity holder) || CollabBlade.of(event.getBlade()) != CollabBlade.SINGULARITY) return;
        BladeCore c = CollabBlade.core(event.getBlade());
        if (!c.unstable() || c.mass() <= 0) return;
        Vec3 hand = holder.position().add(holder.getLookAngle().scale(0.5)).add(0, holder.getBbHeight() * 0.55, 0);
        PacketDistributor.sendToPlayersNear(level, null, hand.x, hand.y, hand.z, 32,
            BladeFxPacket.of(BladeFxPacket.SINGULARITY_UNSTABLE, hand, Vec3.ZERO, c.mass(), 0F, 0));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CENTER.remove(event.getEntity().getUUID());
    }
}
