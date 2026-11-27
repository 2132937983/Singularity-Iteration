// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.armory.blade.BladeFxPacket;
import com.miophas.singularity_iteration.common.armory.blade.CollabBlade;
import com.miophas.singularity_iteration.common.armory.blade.Entanglement;
import com.miophas.singularity_iteration.common.compat.slashblade.SiSlashBladeContent.Timeline;
import com.miophas.singularity_iteration.common.compat.slashblade.SiSlashBladeContent.Variant;
import com.miophas.singularity_iteration.common.suit.SmartGuidance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.1.11.235 (SlashBlade collab B3): the quantum blade's slash art and special effect.
 *
 * <ul>
 *   <li><b>Collapse cut</b> ({@code mio_icif:collapse_cut}): the blade is in superposition over every target the suit has
 *       locked in its HUD frame and every target the blade has entangled (at most 6, within 32 blocks; with neither, the
 *       nearest enemies ahead within 12). A probability cloud (cyan, a little violet) opens on each target, interference
 *       fringes ring out from the wielder and close in, the clouds tighten, and on tick 10 the wave function collapses: a
 *       cyan judgement cut falls on every target at once, with a hit of the blade. Just: the cuts are critical. Super: it
 *       collapses twice. The coherence (or EU) is paid in {@link SiSlashBladeServer#onSlashArt} (B1).</li>
 *   <li><b>Entanglement</b> ({@code mio_icif:entangle}): each hit entangles the target for 10 seconds (at most 6,
 *       {@link Entanglement}); an entangled target glints while the blade is in the hand. The collapse cut finds them even
 *       without the suit's guidance locks.</li>
 * </ul>
 */
final class SiQuantumBlade {
    private SiQuantumBlade() {}

    static final int CYAN = 0x4DE6FF, CORE = 0xC8FAFF, VIOLET = 0x9B7CFF, DARK = 0x2B3440;
    /** The targets chosen on the first tick, by wielder, until the collapse. */
    private static final Map<UUID, List<Integer>> PENDING = new ConcurrentHashMap<>();

    static ComboState collapseCut(Variant v) {
        Timeline t = new Timeline();
        t.put(1, user -> {
            if (!(user.level() instanceof ServerLevel level)) return;
            List<LivingEntity> targets = choose(user, level);
            List<Integer> ids = new ArrayList<>();
            int[] packed = new int[targets.size() + 1];
            packed[0] = user.getId();
            for (int i = 0; i < targets.size(); i++) {
                int id = targets.get(i).getId();
                ids.add(id);
                packed[i + 1] = id;
            }
            PENDING.put(user.getUUID(), ids);
            int flags = v == Variant.SUPER ? BladeFxPacket.FLAG_SUPER : v == Variant.JUST ? BladeFxPacket.FLAG_JUST : BladeFxPacket.FLAG_NORMAL;
            PacketDistributor.sendToPlayersNear(level, null, user.getX(), user.getY(), user.getZ(), 64,
                BladeFxPacket.of(BladeFxPacket.QUANTUM_CAST, user.position(), user.getLookAngle(), 3.6F, 9F, flags, packed));
            com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.debug("collapse cut: {} targets", ids.size());
            SiSlashBladeContent.sound(user, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.9F);
        });
        t.put(10, user -> collapse(user, v == Variant.JUST));
        if (v == Variant.SUPER) {
            t.put(18, user -> collapse(user, true));
        }
        t.put(v == Variant.SUPER ? 19 : 11, user -> PENDING.remove(user.getUUID()));
        return SiSlashBladeContent.state(1900, 1923, t, "judgement_cut_slash_just_sheath");
    }

    /** The suit's locks and the blade's entangled targets; else the nearest enemies ahead. */
    static List<LivingEntity> choose(LivingEntity user, ServerLevel level) {
        List<LivingEntity> out = new ArrayList<>();
        // 0.1.11.237 (B5): with a quantum core the superposition spans more targets
        int max = user instanceof ServerPlayer p && com.miophas.singularity_iteration.common.suit.quantum.QuantumCore.corePiece(p) != null
            ? CollabBlade.COLLAPSE_MAX_CORE : CollabBlade.COLLAPSE_MAX;
        if (user instanceof ServerPlayer player) {
            List<Integer> ids = Entanglement.targets(SmartGuidance.locks(player),
                Entanglement.of(player.getUUID()).live(level.getGameTime()), max * 2);
            for (int id : ids) {
                Entity e = level.getEntity(id);
                if (e instanceof LivingEntity living && living.isAlive() && living != user && SmartGuidance.valid(player, e)
                    && living.distanceToSqr(user) <= CollabBlade.COLLAPSE_RANGE * CollabBlade.COLLAPSE_RANGE
                    && out.size() < max) out.add(living);
            }
        }
        if (!out.isEmpty()) return out;
        Vec3 look = user.getLookAngle();
        double r = CollabBlade.COLLAPSE_FALLBACK_RANGE;
        List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(r),
            e -> e != user && e.isAlive() && e instanceof Enemy && e.distanceToSqr(user) <= r * r
                && e.position().subtract(user.position()).normalize().dot(look) >= 0.3));
        near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(user)));
        for (LivingEntity e : near) if (out.size() < max) out.add(e);
        return out;
    }

    private static List<LivingEntity> pending(LivingEntity user, ServerLevel level) {
        List<LivingEntity> out = new ArrayList<>();
        for (int id : PENDING.getOrDefault(user.getUUID(), List.of()))
            if (level.getEntity(id) instanceof LivingEntity living && living.isAlive()) out.add(living);
        return out;
    }

    /** The collapse: a cyan judgement cut, a blade hit, and razor-sharp client 3D space slices on every pending target. */
    static void collapse(LivingEntity user, boolean critical) {
        if (!(user.level() instanceof ServerLevel level)) return;
        List<LivingEntity> targets = pending(user, level);
        int[] ids = new int[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity e = targets.get(i);
            ids[i] = e.getId();
            Vec3 at = e.position().add(0, e.getBbHeight() * 0.5, 0);
            EntityJudgementCut cut = new EntityJudgementCut(RegistryEvents.JudgementCut, level);
            cut.setPos(at.x, at.y, at.z);
            cut.setOwner(user);
            cut.setColor(CYAN);
            cut.setIsCritical(critical);
            level.addFreshEntity(cut);
            AttackManager.doMeleeAttack(user, e, true, true, 1.0F);
        }
        if (ids.length > 0) {
            int flags = critical ? BladeFxPacket.FLAG_CRITICAL : 0;
            PacketDistributor.sendToPlayersNear(level, null, user.getX(), user.getY(), user.getZ(), 64,
                BladeFxPacket.of(BladeFxPacket.QUANTUM_COLLAPSE, user.position(), user.getLookAngle(), 0F, 0F, flags, ids));
        }
        SiSlashBladeContent.sound(user, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 0.6F);
        SiSlashBladeContent.sound(user, SoundEvents.ENDERMAN_TELEPORT, 0.5F, 1.8F);
    }

    static boolean entangles(ItemStack stack) {
        return BladeStateAccess.of(stack).map(s -> s.hasSpecialEffect(SiSlashBladeContent.ENTANGLE.getId())).orElse(false);
    }

    @SubscribeEvent
    static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser(), target = event.getTarget();
        if (user == null || target == null || !(user.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getBlade();
        if (CollabBlade.of(stack) != CollabBlade.QUANTUM || !entangles(stack)) return;
        Entanglement.of(user.getUUID()).add(target.getId(), level.getGameTime());
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.55, 0);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 48,
            BladeFxPacket.of(BladeFxPacket.QUANTUM_ENTANGLE_HIT, at, Vec3.ZERO, 0F, 0F, 0));
    }

    @SubscribeEvent
    static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.isSelected() || level.getGameTime() % 10 != 0) return;
        if (!(event.getEntity() instanceof ServerPlayer holder) || CollabBlade.of(event.getBlade()) != CollabBlade.QUANTUM) return;
        List<Integer> live = Entanglement.of(holder.getUUID()).live(level.getGameTime());
        if (live.isEmpty()) return;
        int[] ids = new int[live.size()];
        for (int i = 0; i < live.size(); i++) ids[i] = live.get(i);
        PacketDistributor.sendToPlayer(holder,
            BladeFxPacket.of(BladeFxPacket.QUANTUM_ENTANGLE_MARK, holder.position(), Vec3.ZERO, 0F, 0F, 0, ids));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Entanglement.forget(event.getEntity().getUUID());
        PENDING.remove(event.getEntity().getUUID());
    }
}
