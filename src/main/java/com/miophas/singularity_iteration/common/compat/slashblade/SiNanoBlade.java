// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.armory.blade.BladeCore;
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
        // the timeline starts at tick 1
        t.put(1, user -> {
            if (user.level() instanceof ServerLevel level) standingWave(level, user, 8.0);
            SiSlashBladeContent.sound(user, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 2.0F);
        });
        int cuts = v == Variant.SUPER ? 6 : 3;
        float[] rolls = {0F, 30F, -30F, 15F, -15F, 0F};
        for (int i = 0; i < cuts; i++) {
            final float roll = rolls[i];
            final boolean last = i == cuts - 1;
            t.put(4 + i * (v == Variant.SUPER ? 3 : 4), user -> {
                if (!(user.level() instanceof ServerLevel)) return;
                AttackManager.doSlash(user, roll, Vec3.ZERO, false, false, 0.6);
                Drive.doSlash(user, roll, 0F, 10, last ? HOT : ORANGE, Vec3.ZERO, false, 0.8, KnockBacks.cancel, 1.8F);
                SiSlashBladeContent.sound(user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.7F);
            });
        }
        if (v == Variant.SUPER) t.put(13, user -> {
            if (user.level() instanceof ServerLevel level) standingWave(level, user, 10.0);
        });
        if (v == Variant.JUST) t.put(16, user -> {
            if (!(user.level() instanceof ServerLevel)) return;
            Drive.doSlash(user, -90F, 0F, 14, HOT, Vec3.ZERO, true, 1.2, KnockBacks.cancel, 2.0F);
            SiSlashBladeContent.sound(user, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.4F);
        });
        return SiSlashBladeContent.state(1600, 1659, t, "drive_vertical_end");
    }

    /**
     * A standing wave along the user's look, {@code length} blocks long: two envelopes of {@link #HALF_WAVES} half-waves
     * (orange dust at the antinodes), white sparks at the nodes.
     */
    static void standingWave(ServerLevel level, LivingEntity user, double length) {
        Vec3 look = user.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        Vec3 up = right.normalize().cross(look).normalize();
        Vec3 from = user.getEyePosition().add(look.scale(0.8)).add(0, -0.25, 0);
        int n = 60;
        for (int i = 0; i <= n; i++) {
            double x = i / (double) n;
            double amp = 0.45 * Math.sin(Math.PI * HALF_WAVES * x);
            Vec3 at = from.add(look.scale(length * x));
            var dust = SiSlashBladeContent.dust(Math.abs(amp) > 0.3 ? HOT : ORANGE, 1.1F);
            SiSlashBladeContent.particle(level, dust, at.add(up.scale(amp)));
            SiSlashBladeContent.particle(level, dust, at.add(up.scale(-amp)));
        }
        for (int k = 0; k <= HALF_WAVES; k++) {
            Vec3 node = from.add(look.scale(length * k / HALF_WAVES));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, node.x, node.y, node.z, 4, 0.05, 0.05, 0.05, 0.02);
        }
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
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.15);
            if (BladeResources.redHot(c)) level.sendParticles(SiSlashBladeContent.dust(ORANGE, 1.0F), at.x, at.y, at.z, 5, 0.2, 0.2, 0.2, 0);
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
        // a hot edge in the hand shimmers and smokes
        if (event.isSelected() && c.heat() >= 50 && level.getGameTime() % 6 == 0) {
            Vec3 hand = holder.position().add(holder.getLookAngle().scale(0.5)).add(0, holder.getBbHeight() * 0.55, 0);
            level.sendParticles(SiSlashBladeContent.dust(BladeResources.redHot(c) ? HOT : ORANGE, 0.6F), hand.x, hand.y, hand.z, 2, 0.15, 0.1, 0.15, 0);
            if (c.overheated()) level.sendParticles(ParticleTypes.SMOKE, hand.x, hand.y, hand.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }
}
