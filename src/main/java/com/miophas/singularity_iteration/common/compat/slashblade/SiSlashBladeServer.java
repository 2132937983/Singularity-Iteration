// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.compat.slashblade;

import com.miophas.singularity_iteration.common.armory.ArmoryMaintenance;
import com.miophas.singularity_iteration.common.armory.blade.BladeCore;
import com.miophas.singularity_iteration.common.armory.blade.BladeEnergyStorage;
import com.miophas.singularity_iteration.common.armory.blade.BladeResources;
import com.miophas.singularity_iteration.common.armory.blade.CollabBlade;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumCore;
import com.miophas.singularity_iteration.common.suit.quantum.QuantumCore;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 0.1.11.233 (SlashBlade collab B1): the server side of the collab blades on SlashBlade's events. Loaded only by
 * {@link SiSlashBlade} when SlashBlade is installed.
 *
 * <ul>
 *   <li>which blade a stack is: SlashBlade's translation key ({@link CollabBlade#resolver});</li>
 *   <li>an FE item energy storage on {@code slashblade:slashblade} for the collab blades: the Armory and FE chargers fill them;</li>
 *   <li>every tick a carried blade runs {@link BladeResources#tick} (venting, containment);</li>
 *   <li>a slash art is paid ({@link BladeResources#slashArt}) or refused, with the reason on the action bar;</li>
 *   <li>hits heat the nano edge, kills feed the singularity;</li>
 *   <li>a blade the Armory has fully repaired is no longer broken.</li>
 * </ul>
 */
final class SiSlashBladeServer {
    private SiSlashBladeServer() {}

    static void register(IEventBus modEventBus) {
        CollabBlade.resolver = SiSlashBladeServer::resolve;
        ArmoryMaintenance.afterRepair = SiSlashBladeServer::afterRepair;
        modEventBus.addListener(SiSlashBladeServer::capabilities);
        NeoForge.EVENT_BUS.register(SiSlashBladeServer.class);
        // 0.1.11.234 (B2): slash arts, special effects and their combo states
        SiSlashBladeContent.register(modEventBus);
        NeoForge.EVENT_BUS.register(SiNanoBlade.class);
    }

    static CollabBlade resolve(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) return null;
        return BladeStateAccess.of(stack).map(s -> CollabBlade.byTranslationKey(s.getTranslationKey())).orElse(null);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM)
            if (item instanceof ItemSlashBlade)
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> BladeEnergyStorage.of(stack), item);
    }

    private static void afterRepair(ItemStack stack) {
        try {
            if (stack.getDamageValue() == 0 && CollabBlade.of(stack) != null)
                BladeStateAccess.of(stack).filter(ISlashBladeState::isBroken).ifPresent(s -> s.setBroken(false));
        } catch (RuntimeException ignored) {
            // a SlashBlade without the member: the blade stays broken until it is used
        }
    }

    @SubscribeEvent
    static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        if (event.getLevel().isClientSide) return;
        ItemStack stack = event.getBlade();
        CollabBlade kind = CollabBlade.of(stack);
        if (kind == null) return;
        BladeCore before = stack.get(com.miophas.singularity_iteration.common.registry.mio_icif_data_components.BLADE_CORE.get());
        BladeCore c = CollabBlade.core(stack);
        BladeCore after = BladeResources.tick(c, kind);
        if (before == null || !after.equals(before)) CollabBlade.set(stack, after);
    }

    @SubscribeEvent
    static void onSlashArt(SlashBladeEvent.PerformSlashArtEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user == null || user.level().isClientSide || event.getType() == SlashArts.ArtsType.Fail) return;
        ItemStack stack = user.getMainHandItem();
        CollabBlade kind = CollabBlade.of(stack);
        if (kind == null) return;
        BladeCore c = CollabBlade.core(stack);
        BladeResources.Coherence coherence = BladeResources.Coherence.NO_CORE;
        if (kind == CollabBlade.QUANTUM && user instanceof ServerPlayer sp && QuantumCore.corePiece(sp) != null)
            coherence = SuitQuantumCore.trySpendCoherence(sp, CollabBlade.QUANTUM_SA_COHERENCE)
                ? BladeResources.Coherence.PAID : BladeResources.Coherence.SHORT;
        BladeResources.Spend spend = BladeResources.slashArt(c, kind, coherence);
        if (spend.ok()) {
            CollabBlade.set(stack, spend.core());
        } else {
            event.setCanceled(true);
            if (user instanceof Player p) p.displayClientMessage(Component.translatable(spend.refusal().langKey()), true);
        }
    }

    @SubscribeEvent
    static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser();
        if (user == null || user.level().isClientSide) return;
        ItemStack stack = event.getBlade();
        CollabBlade kind = CollabBlade.of(stack);
        if (kind != CollabBlade.NANO) return;
        CollabBlade.set(stack, BladeResources.hit(CollabBlade.core(stack), kind));
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        ItemStack stack = killer.getMainHandItem();
        if (CollabBlade.of(stack) != CollabBlade.SINGULARITY) return;
        LivingEntity dead = event.getEntity();
        boolean big = dead instanceof Player || dead.getType().is(Tags.EntityTypes.BOSSES);
        CollabBlade.set(stack, BladeResources.kill(CollabBlade.core(stack), CollabBlade.SINGULARITY, big));
    }
}
