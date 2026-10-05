// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_chestplate_advanced_quantum;
import com.miophas.singularity_iteration.common.item.armor.mio_icif_chestplate_quantum;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Viltrum flight of the quantum chestplates (server half). The feature switch in the equipment
 * console grants free flight; a double jump starts it. The client half
 * ({@code ViltrumFlightClient}) replaces creative-flight movement with an inertial model:
 * thrust along the view axis, speed build-up, glide, banking, a sonic boom at
 * {@link #BOOM_SPEED}.
 *
 * <p>Power: {@link #BASE_COST} EU/t plus {@link #SPEED_COST} x speed^2 (speed in blocks/tick).
 * Cruise (1.1 b/t) costs about 42 EU/t, full boost (3.2 b/t) about 313 EU/t: a full quantum
 * chestplate (10 M EU) gives about 4 h of cruise or 26 min of full boost.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ViltrumFlight {
    private ViltrumFlight() {}

    public static final String FEATURE = "viltrum_flight";
    public static final double CRUISE_SPEED = 1.1;
    public static final double BOOST_SPEED = 3.2;
    public static final double BOOM_SPEED = 2.4;
    public static final long BASE_COST = 6;
    public static final double SPEED_COST = 30;
    public static final long MIN_ENERGY = 1_000;
    private static final int BILL_INTERVAL = 10;

    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "viltrum_flight");
    private static final AttributeModifier MODIFIER = new AttributeModifier(MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

    private static final Map<UUID, Vec3> lastPos = new HashMap<>();
    private static final Map<UUID, Double> bill = new HashMap<>();

    static {
        // opt-in: the switch starts off so the jetpack keeps its usual controls
        ArmorFeatureState.registerDefault(FEATURE, false);
    }

    /** Touch the class during mod construction so the default is registered early. */
    public static void bootstrap() { }

    public static boolean isFlightChest(ItemStack chest) {
        return chest.getItem() instanceof mio_icif_chestplate_quantum || chest.getItem() instanceof mio_icif_chestplate_advanced_quantum;
    }

    /** Switch on, quantum chestplate worn, power left. */
    public static boolean canUse(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isFlightChest(chest) && ArmorFeatures.isEnabled(chest, FEATURE) && SuitModules.energy(chest) >= MIN_ENERGY;
    }

    /** Energy cost of one tick at the given speed (blocks per tick). */
    public static double costPerTick(double speed) {
        return BASE_COST + SPEED_COST * speed * speed;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean creative = player.isCreative() || player.isSpectator();
        boolean use = canUse(player);
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        boolean granted = flight != null && flight.hasModifier(MODIFIER_ID);
        if (use && !creative) {
            if (flight != null && !granted) flight.addTransientModifier(MODIFIER);
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (granted) {
            flight.removeModifier(MODIFIER_ID);
            if (!creative && flight.getValue() <= 0 && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        Vec3 now = player.position();
        Vec3 before = lastPos.put(player.getUUID(), now);
        if (!use || creative || !player.getAbilities().flying || before == null) {
            bill.remove(player.getUUID());
            return;
        }
        double speed = Math.min(BOOST_SPEED * 1.5, now.distanceTo(before));
        double owed = bill.getOrDefault(player.getUUID(), 0.0) + costPerTick(speed);
        if (player.tickCount % BILL_INTERVAL == 0) {
            SuitModules.extract(player.getItemBySlot(EquipmentSlot.CHEST), (long) Math.ceil(owed));
            owed = 0;
        }
        bill.put(player.getUUID(), owed);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastPos.remove(event.getEntity().getUUID());
        bill.remove(event.getEntity().getUUID());
    }
}
