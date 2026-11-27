// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.armory.blade.BladeCore;
import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.armory.blade.BladeResources;
import com.miophas.singularity_iteration.common.armory.blade.CollabBlade;
import com.miophas.singularity_iteration.common.compat.slashblade.SiSlashBladeContent.Timeline;
import com.miophas.singularity_iteration.common.compat.slashblade.SiSlashBladeContent.Variant;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.Drive;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.1.11.234 (SlashBlade collab B2): the nano blade's slash art and special effect.
 *
 * <ul>
 *   <li><b>Piezo cleave</b> ({@code mio_icif:piezo_cleave}): the piezo stack rings up a standing wave along the line of the
 *       cut (orange antinodes, white nodes), then three cuts follow each other within 8 ticks, flat and at plus and minus
 *       30 degrees: the afterimages of a blade moving faster than the eye. Just: a fourth, vertical critical cut. Super:
 *       six cuts and the wave rung again on the fourth.</li>
 *   <li><b>Piezo edge</b> ({@code mio_icif:piezo_edge}): a hit with EU in the blade adds vibration damage that ignores
 *       armour ({@link BladeResources#piezoDamage}: 2, up to 6 as the edge heats); a red-hot edge (heat 70 and up) sets
 *       the target on fire. Carried, the nanobots mend 1 durability every 40 ticks for 400 EU of the blade's own; a
 *       mended blade is no longer broken. A hot edge (heat 50 and up) in the hand shimmers and smokes.</li>
 * </ul>
 * The slash art's EU and heat are paid in {@link SiSlashBladeServer#onSlashArt} (B1).
 */
final class SiNanoBlade {
    private SiNanoBlade() {}

    static final int ORANGE = 0xFF7A1F, HOT = 0xFFC46B;
    /** Half-wavelengths of the standing wave along the cut. */
    static final int HALF_WAVES = 3;

    static ComboState piezoCleave(Variant v) {
        Timeline t = new Timeline();
        int vFlag = v == Variant.SUPER ? BladeFxPacket.FLAG_SUPER : v == Variant.JUST ? BladeFxPacket.FLAG_JUST : BladeFxPacket.FLAG_NORMAL;
        // the timeline starts at tick 1
        t.put(1, user -> {
            if (user.level() instanceof ServerLevel level) standingWave(level, user, 8.0, vFlag);
            SiSlashBladeContent.sound(user, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 2.0F);
        });
        int cuts = v == Variant.SUPER ? 6 : 3;
        float[] rolls = {0F, 30F, -30F, 15F, -15F, 0F};
        for (int i = 0; i < cuts; i++) {
            final float roll = rolls[i];
            final boolean last = i == cuts - 1;
            t.put(4 + i * (v == Variant.SUPER ? 3 : 4), user -> {
                if (!(user.level() instanceof ServerLevel level)) return;
                AttackManager.doSlash(user, roll, Vec3.ZERO, false, false, 0.6);
                Drive.doSlash(user, roll, 0F, 10, last ? HOT : ORANGE, Vec3.ZERO, false, 0.8, KnockBacks.cancel, 1.8F);
                Vec3 origin = user.getEyePosition().add(0, -0.25, 0);
                int flags = (v == Variant.SUPER ? BladeFxPacket.FLAG_SUPER : 0) | (last ? BladeFxPacket.FLAG_RED_HOT : 0);
                PacketDistributor.sendToPlayersNear(level, null, origin.x, origin.y, origin.z, 64,
                    BladeFxPacket.of(BladeFxPacket.NANO_SLASH, origin, user.getLookAngle(), roll, v == Variant.SUPER ? 3.4F : 3.1F, flags));
                SiSlashBladeContent.sound(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.7F);
            });
        }
        if (v == Variant.SUPER) t.put(13, user -> {
            if (user.level() instanceof ServerLevel level) standingWave(level, user, 10.0, BladeFxPacket.FLAG_SUPER);
        });
        if (v == Variant.JUST) t.put(16, user -> {
            if (!(user.level() instanceof ServerLevel level)) return;
            Drive.doSlash(user, -90F, 0F, 14, HOT, Vec3.ZERO, true, 1.2, KnockBacks.cancel, 2.0F);
            Vec3 origin = user.getEyePosition().add(0, -0.2, 0);
            PacketDistributor.sendToPlayersNear(level, null, origin.x, origin.y, origin.z, 64,
                BladeFxPacket.of(BladeFxPacket.NANO_SLASH, origin, user.getLookAngle(), -90F, 4.4F, BladeFxPacket.FLAG_JUST));
            SiSlashBladeContent.sound(user, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.4F);
        });
        return SiSlashBladeContent.state(1600, 1659, t, "drive_vertical_end");
    }

    /** Sends the client 3D standing-wave ribbon + node piezo arcs along the user's look. */
    static void standingWave(ServerLevel level, LivingEntity user, double length, int flags) {
        Vec3 look = user.getLookAngle().normalize();
        Vec3 from = user.getEyePosition().add(look.scale(0.8)).add(0, -0.25, 0);
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, 64,
            BladeFxPacket.of(BladeFxPacket.NANO_WAVE, from, look, (float) length, HALF_WAVES, flags));
    }

    static void emitOverheatVent(ServerLevel level, LivingEntity user, float heat) {
        Vec3 hand = user.position().add(user.getLookAngle().scale(0.5)).add(0, user.getBbHeight() * 0.58, 0);
        PacketDistributor.sendToPlayersNear(level, null, hand.x, hand.y, hand.z, 48,
            BladeFxPacket.of(BladeFxPacket.NANO_OVERHEAT, hand, user.getLookAngle(), heat, 0F, 0));
        SiSlashBladeContent.sound(user, SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.6F);
    }

    static boolean hasEdge(ItemStack stack) {
        return BladeStateAccess.of(stack).map(s -> s.hasSpecialEffect(SiSlashBladeContent.PIEZO_EDGE.getId())).orElse(false);
    }

    @SubscribeEvent
    static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser(), target = event.getTarget();
        if (user == null || target == null || user.level().isClientSide) return;
        ItemStack stack = event.getBlade();
        if (CollabBlade.of(stack) != CollabBlade.NANO || !hasEdge(stack)) return;
        BladeCore c = CollabBlade.core(stack);
        float extra = BladeResources.piezoDamage(c);
        if (extra <= 0) return;
        int invulnerable = target.invulnerableTime;
        target.invulnerableTime = 0;
        target.hurt(user.damageSources().magic(), extra);
        target.invulnerableTime = Math.max(target.invulnerableTime, invulnerable);
        if (BladeResources.redHot(c)) target.igniteForSeconds(2);
        if (user.level() instanceof ServerLevel level) {
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
            int flags = BladeResources.redHot(c) ? BladeFxPacket.FLAG_RED_HOT : 0;
            PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 48,
                BladeFxPacket.of(BladeFxPacket.NANO_HIT, at, user.getLookAngle(), c.heat(), extra, flags));
        }
    }

    @SubscribeEvent
    static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity holder)) return;
        ItemStack stack = event.getBlade();
        if (CollabBlade.of(stack) != CollabBlade.NANO || !hasEdge(stack)) return;
        BladeCore c = CollabBlade.core(stack);
        BladeCore mended = BladeResources.mend(c, level.getGameTime(), stack.getDamageValue());
        if (mended != null) {
            stack.setDamageValue(stack.getDamageValue() - 1);
            CollabBlade.set(stack, mended);
            if (stack.getDamageValue() == 0) BladeStateAccess.of(stack).filter(ISlashBladeState::isBroken).ifPresent(s -> s.setBroken(false));
        }
        // a hot edge in the hand arcs subtly, and vents smoke while overheated
        if (event.isSelected() && c.heat() >= 50 && level.getGameTime() % 8 == 0) {
            Vec3 hand = holder.position().add(holder.getLookAngle().scale(0.5)).add(0, holder.getBbHeight() * 0.55, 0);
            if (!c.overheated()) {
                PacketDistributor.sendToPlayersNear(level, null, hand.x, hand.y, hand.z, 32,
                    BladeFxPacket.of(BladeFxPacket.NANO_HEAT_ARC, hand, holder.getLookAngle(), c.heat(), 0F, 0));
            } else {
                level.sendParticles(ParticleTypes.SMOKE, hand.x, hand.y, hand.z, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
