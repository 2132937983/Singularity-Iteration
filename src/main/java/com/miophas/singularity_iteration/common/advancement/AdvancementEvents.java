// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.advancement;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_items_armors;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_quantum_sword;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_steel_shield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * 0.1.11.79: the advancements granted from game events rather than from a machine or an item:
 * <ul>
 *   <li>"Sorry, Not Tomorrow": the Steel Riot Shield blocks a projectile;</li>
 *   <li>"Forgot to Charge": a player starves or drowns wearing the Quantum Helmet (a charged helmet refills air and food,
 *       so such a death means it ran dry);</li>
 *   <li>"You Know Nothing of Power": a player's overclocked Quantum Sword kills a mob in full netherite holding a sword.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class AdvancementEvents {
    private AdvancementEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.isCanceled() || !event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getUseItem().getItem() instanceof mio_icif_steel_shield && event.getDamageSource().is(DamageTypeTags.IS_PROJECTILE))
            SiAdvancements.grant(player, "shield", "blocked");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        DamageSource source = event.getSource();
        LivingEntity dead = event.getEntity();
        if (dead instanceof ServerPlayer player && (source.is(DamageTypes.STARVE) || source.is(DamageTypes.DROWN))
                && player.getItemBySlot(EquipmentSlot.HEAD).is(mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get()))
            SiAdvancements.grant(player, "forgot_charge", "died");
        if (source.getEntity() instanceof ServerPlayer killer && source.getDirectEntity() == killer && fullNetheriteSwordsman(dead)) {
            var sword = killer.getMainHandItem();
            if (sword.getItem() instanceof mio_icif_quantum_sword && mio_icif_quantum_sword.isHyperState(sword))
                SiAdvancements.grant(killer, "know_nothing", "kill");
        }
    }

    /** True for a mob (not a player) in netherite helmet, chestplate, leggings and boots, holding any sword. */
    public static boolean fullNetheriteSwordsman(LivingEntity e) {
        return !(e instanceof net.minecraft.world.entity.player.Player)
            && e.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET)
            && e.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
            && e.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS)
            && e.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS)
            && e.getMainHandItem().is(ItemTags.SWORDS);
    }
}
