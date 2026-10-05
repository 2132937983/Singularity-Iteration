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
import net.minecraft.server.level.ServerLevel;
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
 * Special Maneuver Mode of the quantum chestplates (server half, 0.1.7.34).
 *
 * <p>One console switch arms the whole package: free flight, the throttle flight model, the
 * maneuver body pose, vapour cone, wake trails, shock sounds and the flight HUD. The client half
 * ({@code ManeuverModeClient}) owns the motion; this class grants flight, bills the power, and
 * relays each flyer's throttle and stick state to the players who can see the flyer (they need it
 * for the pose and the effects).
 *
 * <p>Flight model (shared constants): the sprint / boost key opens the throttle by
 * {@link #THROTTLE_UP} per tick and the throttle closes twice as fast when released. Cruise speed is
 * throttle x {@link #MAX_SPEED}. The velocity is rewritten every tick towards the view axis with a
 * grip that falls as the speed rises, so the flyer drifts wide through fast turns. The shock barrier
 * is at throttle {@link #SHOCK_THROTTLE}.
 *
 * <p>Power: {@link #BASE_COST} EU/t plus {@link #SPEED_COST} x speed^2 (speed in blocks/tick).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ManeuverMode {
    private ManeuverMode() {}

    public static final String FEATURE = "special_maneuver";
    /** Top cruise speed in blocks per tick (80 m/s). */
    public static final double MAX_SPEED = 4.0;
    public static final float THROTTLE_UP = 0.017F;
    public static final float THROTTLE_DOWN = 0.034F;
    public static final float SHOCK_THROTTLE = 0.6F;
    /** Velocity grip at zero and at full throttle: lower grip = longer drift. */
    public static final double GRIP_SLOW = 0.34, GRIP_FAST = 0.11;
    public static final double CRUISE_SPEED = 1.1;
    public static final double BOOST_SPEED = MAX_SPEED;
    public static final double BOOM_SPEED = MAX_SPEED * SHOCK_THROTTLE;
    public static final long BASE_COST = 6;
    public static final double SPEED_COST = 30;
    public static final long MIN_ENERGY = 1_000;
    private static final int BILL_INTERVAL = 10;

    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "special_maneuver");
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

    /** The chestplate arms the mode (switch on), regardless of power. */
    public static boolean isArmed(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isFlightChest(chest) && ArmorFeatures.isEnabled(chest, FEATURE);
    }

    /** Switch on, quantum chestplate worn, power left. */
    public static boolean canUse(Player player) {
        return isArmed(player) && SuitModules.energy(player.getItemBySlot(EquipmentSlot.CHEST)) >= MIN_ENERGY;
    }

    /** Energy cost of one tick at the given speed (blocks per tick). */
    public static double costPerTick(double speed) {
        return BASE_COST + SPEED_COST * speed * speed;
    }

    /** Velocity grip for a throttle setting. */
    public static double grip(float throttle) {
        return GRIP_SLOW + (GRIP_FAST - GRIP_SLOW) * Math.max(0, Math.min(1, throttle));
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
        double speed = Math.min(MAX_SPEED * 1.5, now.distanceTo(before));
        double owed = bill.getOrDefault(player.getUUID(), 0.0) + costPerTick(speed);
        if (player.tickCount % BILL_INTERVAL == 0) {
            SuitModules.extract(player.getItemBySlot(EquipmentSlot.CHEST), (long) Math.ceil(owed));
            owed = 0;
        }
        bill.put(player.getUUID(), owed);
    }

    /** Client -> server: the flyer's throttle and stick; relayed to everyone who can see the flyer. */
    public static void relayState(ServerPlayer player, float throttle, float forward, float sideways, boolean locked) {
        if (!isArmed(player)) throttle = 0;
        var state = new SuitPackets.ManeuverState(player.getId(), clamp(throttle), clamp(forward, -1), clamp(sideways, -1), locked);
        for (ServerPlayer viewer : ((ServerLevel) player.level()).players()) {
            if (viewer != player && viewer.distanceToSqr(player) <= 160 * 160) SuitPackets.send(viewer, state);
        }
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static float clamp(float v, float min) { return Math.max(min, Math.min(1, v)); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastPos.remove(event.getEntity().getUUID());
        bill.remove(event.getEntity().getUUID());
    }
}
