// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
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
 * <p>One console switch arms the full package: free flight, the throttle flight model, the
 * maneuver body pose, the vapour cone, the wake trails, the shock sounds and the flight HUD.
 *
 * <p><b>Server and client split.</b>
 * <ul>
 *   <li>The client half ({@code ManeuverModeClient}) owns the motion. It calculates the velocity
 *       and sets it on the local player. Vanilla sends the position to the server, as for all
 *       player movement. This class does not check the speed.</li>
 *   <li>This class gives flight permission (creative flight attribute and {@code mayfly}). It
 *       takes the power from the chestplate. It relays the throttle and stick state of each flyer
 *       to the players near the flyer. The other clients need this state for the pose and the
 *       effects.</li>
 *   <li>Server checks on the relayed state: the packet handler drops values that are not finite.
 *       {@link #relayState} sets the throttle to 0 when the chestplate is not armed. It clamps the
 *       throttle to 0..1 and the stick to -1..1. It does not check the energy or the flight state.
 *       The packet handler applies {@code PacketRateLimit.MANEUVER_INPUT} first.</li>
 * </ul>
 *
 * <p><b>Flight model (shared constants).</b> The sprint or boost key opens the throttle by
 * {@link #THROTTLE_UP} per tick. When the key is released, the throttle closes by
 * {@link #THROTTLE_DOWN} per tick (two times as fast). Thus 0 to full takes 59 ticks (2.9 s),
 * and full to 0 takes 30 ticks (1.5 s). The target speed is throttle x {@link #MAX_SPEED}. Each tick
 * the client moves the velocity towards the view axis by the fraction {@link #grip}. The grip falls
 * when the throttle rises. Thus the flyer drifts wide through fast turns. The shock barrier is at
 * throttle {@link #SHOCK_THROTTLE}.
 *
 * <p><b>Power.</b> {@link #BASE_COST} EU/t plus {@link #SPEED_COST} x speed^2. Speed is in blocks
 * per tick. Examples: hover 6 EU/t, the shock barrier (2.4 blocks/t) 179 EU/t, full speed
 * (4 blocks/t) 486 EU/t.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ManeuverMode {
    private ManeuverMode() {}

    /** Armor feature key of the console switch. */
    public static final String FEATURE = "special_maneuver";
    /** Top cruise speed in blocks per tick (80 m/s, 288 km/h). */
    public static final double MAX_SPEED = 4.0;
    /** Throttle increase per tick while the boost key is down. */
    public static final float THROTTLE_UP = 0.017F;
    /** Throttle decrease per tick while the boost key is up and the throttle is not locked. */
    public static final float THROTTLE_DOWN = 0.034F;
    /**
     * Throttle of the shock barrier, 0..1. When the throttle crosses it upwards, the client starts
     * the shock ring and the boom. The HUD shows Mach 1 at this throttle.
     */
    public static final float SHOCK_THROTTLE = 0.6F;
    /**
     * Velocity grip at throttle 0 and at throttle 1. The grip is the fraction of the difference
     * between target velocity and current velocity that the client removes in one tick. A lower
     * grip gives a longer drift. The velocity error decays by {@code (1 - grip)} per tick. Thus the
     * time constant is about 2.4 ticks at GRIP_SLOW and about 8.6 ticks at GRIP_FAST.
     */
    public static final double GRIP_SLOW = 0.34, GRIP_FAST = 0.11;
    /** Not used in the current code. */
    public static final double CRUISE_SPEED = 1.1;
    /** Not used in the current code. */
    public static final double BOOST_SPEED = MAX_SPEED;
    /** Speed at the shock barrier in blocks per tick (2.4 blocks/t, 48 m/s). The HUD uses it as Mach 1. */
    public static final double BOOM_SPEED = MAX_SPEED * SHOCK_THROTTLE;
    /** Base energy cost in EU per tick while the player flies. */
    public static final long BASE_COST = 6;
    /** Speed cost factor in EU per tick per (blocks per tick)^2. */
    public static final double SPEED_COST = 30;
    /** Minimum chestplate energy in EU to use the mode. */
    public static final long MIN_ENERGY = 1_000;
    /** Ticks between two energy extractions. The cost accumulates between them. */
    private static final int BILL_INTERVAL = 10;

    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "special_maneuver");
    /** +1 on the creative flight attribute. It is transient, so it is not saved with the player. */
    private static final AttributeModifier MODIFIER = new AttributeModifier(MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

    /** Position of each player at the last server tick. The bill uses it to measure the speed. */
    private static final Map<UUID, Vec3> lastPos = new HashMap<>();
    /** Energy owed by each flyer since the last extraction, in EU. */
    private static final Map<UUID, Double> bill = new HashMap<>();

    static {
        // opt-in: the switch starts off so the jetpack keeps its usual controls
        ArmorFeatureState.registerDefault(FEATURE, false);
    }

    /** Touch the class during mod construction so the default is registered early. */
    public static void bootstrap() { }

    /** True for the quantum chestplate and the advanced quantum chestplate. */
    public static boolean isFlightChest(ItemStack chest) {
        return chest.getItem() instanceof mio_icif_chestplate_quantum;
    }

    /** The chestplate arms the mode (switch on), regardless of power. */
    public static boolean isArmed(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isFlightChest(chest) && ArmorFeatures.isEnabled(chest, FEATURE);
    }

    /** Switch on, quantum chestplate worn, power left. */
    public static boolean canUse(Player player) {
        // 0.1.8.0: a full set shares its energy pool with the chestplate.
        return isArmed(player) && SuitModules.energy(player, player.getItemBySlot(EquipmentSlot.CHEST)) >= MIN_ENERGY;
    }

    /**
     * Energy cost of one tick: {@code BASE_COST + SPEED_COST * speed^2}. The square makes high speed
     * expensive.
     *
     * @param speed the speed in blocks per tick.
     * @return the cost in EU for one tick.
     */
    public static double costPerTick(double speed) {
        return BASE_COST + SPEED_COST * speed * speed * dragFactor(speed);
    }

    /** Speed in blocks per tick where the aerodynamic deflection field starts and where it is complete. */
    public static final double FIELD_START = 1.0, FIELD_FULL = 1.8;
    /** Drag removed by a complete deflection field (0.1.8.0). */
    public static final double FIELD_DRAG_CUT = 0.30;

    /**
     * Drag multiplier of the speed cost (0.1.8.0). The aerodynamic deflection field forms in cruise
     * between {@link #FIELD_START} and {@link #FIELD_FULL} blocks per tick (smoothstep) and removes
     * up to {@link #FIELD_DRAG_CUT} of the speed cost. Examples: the shock barrier (2.4 blocks/t)
     * costs 127 EU/t instead of 179 EU/t, full speed (4 blocks/t) 342 EU/t instead of 486 EU/t.
     */
    public static double dragFactor(double speed) {
        double x = Math.max(0, Math.min(1, (speed - FIELD_START) / (FIELD_FULL - FIELD_START)));
        return 1 - FIELD_DRAG_CUT * x * x * (3 - 2 * x);
    }

    /**
     * Velocity grip for a throttle setting. It is a linear blend from {@link #GRIP_SLOW} at
     * throttle 0 to {@link #GRIP_FAST} at throttle 1. The method clamps the throttle to 0..1.
     *
     * <p>Why a low grip at high speed gives drift: in a turn, the client removes only the fraction
     * {@code grip} of the old velocity part per tick. At full throttle the old part falls by 11 % per
     * tick. Thus after a 90 degree turn at 4 blocks per tick, the flyer still moves about
     * {@code 4 / 0.11} = 36 blocks in the old direction.
     */
    public static double grip(float throttle) {
        return GRIP_SLOW + (GRIP_FAST - GRIP_SLOW) * Math.max(0, Math.min(1, throttle));
    }

    /**
     * Server tick of each player.
     *
     * <p>Flight permission: when {@link #canUse} is true and the player is not in creative or
     * spectator mode, the method adds the flight modifier and sets {@code mayfly}. Otherwise it
     * removes the modifier. It clears {@code mayfly} and {@code flying} only when no other source
     * gives flight.
     *
     * <p>Bill: the speed is the distance moved since the last tick, in blocks. The method clamps it
     * to 1.5 x {@link #MAX_SPEED}. Thus a teleport costs no more than 6 blocks per tick. The cost
     * accumulates in {@link #bill}. Every {@link #BILL_INTERVAL} ticks of {@code tickCount}, the
     * method takes the owed energy, rounded up, from the chestplate. When the player stops flying,
     * the method removes the owed energy without a charge.
     */
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
            // keep mayfly when another modifier still gives flight
            if (!creative && flight.getValue() <= 0 && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        Vec3 now = player.position();
        Vec3 before = lastPos.put(player.getUUID(), now);
        // 0.1.10.3: no bill while riding (a mount moves the player, not the thrusters)
        if (!use || creative || !player.getAbilities().flying || player.isPassenger() || before == null) {
            bill.remove(player.getUUID());
            return;
        }
        double speed = Math.min(MAX_SPEED * 1.5, now.distanceTo(before));
        double owed = bill.getOrDefault(player.getUUID(), 0.0) + costPerTick(speed);
        if (player.tickCount % BILL_INTERVAL == 0) {
            SuitModules.extract(player, player.getItemBySlot(EquipmentSlot.CHEST), (long) Math.ceil(owed));
            owed = 0;
        }
        bill.put(player.getUUID(), owed);
    }

    /**
     * Relays the throttle and stick state of a flyer. The packet handler calls it on the server
     * thread when the flyer sends {@code ManeuverInput}. The handler drops values that are not
     * finite.
     *
     * <p>The method sets the throttle to 0 when the chestplate is not armed. It clamps the throttle
     * to 0..1 and the stick values to -1..1. It sends the state to each other player in the same
     * level within 160 blocks. It does not test if the receiver tracks the flyer.
     *
     * @param player   the flyer.
     * @param throttle the throttle, 0..1.
     * @param forward  the forward stick, -1..1.
     * @param sideways the sideways stick, -1..1.
     * @param locked   true when the throttle is locked.
     */
    public static void relayState(ServerPlayer player, float throttle, float forward, float sideways, boolean locked) {
        if (!isArmed(player)) throttle = 0;
        var state = new SuitPackets.ManeuverState(player.getId(), clamp(throttle), clamp(forward, -1), clamp(sideways, -1), locked);
        for (ServerPlayer viewer : ((ServerLevel) player.level()).players()) {
            if (viewer != player && viewer.distanceToSqr(player) <= 160 * 160) SuitPackets.send(viewer, state);
        }
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static float clamp(float v, float min) { return Math.max(min, Math.min(1, v)); }

    /** Removes the per-player state when a player logs out. This bounds the memory of the maps. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastPos.remove(event.getEntity().getUUID());
        bill.remove(event.getEntity().getUUID());
    }
}
