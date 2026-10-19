// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.SuitPackets;
import com.miophas.singularity_iteration.common.suit.SuitRegistry;
import com.miophas.singularity_iteration.common.suit.UnitRef;
import com.miophas.singularity_iteration.core.api.suit.SuitBurstSpec;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry;
import com.miophas.singularity_iteration.core.api.suit.SuitNanoCore;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Nano core rules (0.1.8.5, phase 2 in 0.1.8.6): the reserve and heat of each wearer, the passive
 * nano units, the burst keys and the state sync to the wearer.
 *
 * <p>Reserve: 0..100, regained at {@link NanoConfig#reserveRegen()} points per second while a
 * nano piece with units is worn, each point paid with {@link NanoConfig#euPerReserve()} EU from
 * the suit energy pool. Heat: each burst adds heat; the suit sheds {@link NanoConfig#heatDissipation()}
 * per second (more with the thermoregulating membrane and Mk.II fins). At the cap (100, more with
 * heat-sink fins) it overheats: bursts lock, and without fins the wearer slows down, until the heat
 * falls below the resume level.
 *
 * <p>Tiers (0.1.8.6): {@link #level} is 0 for an inactive unit, 1 for Mk.I and 2 for Mk.II. Each
 * Mk.II value is documented next to its Mk.I value.
 */
public final class NanoSuit {
    private NanoSuit() {}

    public static final float HEAT_CAP = 100;
    /** Order in which unassigned burst keys pick the worn burst units. */
    private static final SuitModuleType[] BURST_PRIORITY = {SuitModuleType.MYOFIBER, SuitModuleType.OPTICAL_CAMO, SuitModuleType.KINETIC_MESH,
        SuitModuleType.EMP_EMITTER, SuitModuleType.RECON_CLOUD};
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    static final ResourceLocation SPRINT_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_myofiber_sprint");
    static final ResourceLocation STEP_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_myofiber_step");
    static final ResourceLocation HF_SPEED_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_hf_sync");
    static final ResourceLocation WATER_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_hydrophobic_water");
    static final ResourceLocation SWIM_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_hydrophobic_swim");
    static final ResourceLocation GRIP_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_gecko_grip");
    static final ResourceLocation OVERHEAT_ID = ResourceLocation.fromNamespaceAndPath("mio_icif", "nano_overheat_slow");

    private static final DustParticleOptions REPAIR_DUST = new DustParticleOptions(new Vector3f(0.42F, 1.0F, 0.68F), 0.7F);
    private static final DustParticleOptions SETAE_DUST = new DustParticleOptions(new Vector3f(0.6F, 0.88F, 0.36F), 0.5F);

    private static boolean bootstrapped;
    /** Client side of the public view: the last synced state of the local player (set by the client set-up). */
    public static volatile java.util.function.Function<Player, NanoCoreState> CLIENT_VIEW;

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        SuitNanoCore.setView(new SuitNanoCore.View() {
            @Override public float reserve(Player player) { NanoCoreState s = existing(player); return s == null ? 0 : s.reserve; }
            @Override public float heat(Player player) { NanoCoreState s = existing(player); return s == null ? 0 : s.heat; }
            @Override public float heatCap(Player player) { return NanoSuit.heatCap(player); }
            @Override public boolean overheated(Player player) { NanoCoreState s = existing(player); return s != null && s.overheated; }
            @Nullable
            private NanoCoreState existing(Player player) {
                if (player.level().isClientSide) return CLIENT_VIEW == null ? null : CLIENT_VIEW.apply(player);
                return player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
            }
        });
    }

    // ------------------------------------------------------------------ queries

    /** The nano core of a player (created on first use; server side). */
    public static NanoCoreState state(Player player) {
        return player.getData(SuitRegistry.NANO_CORE);
    }

    /** True when the nano core is online: a worn nano piece carries at least one unit. */
    public static boolean wearsNano(Player player) {
        return nanoPiece(player) != null;
    }

    /**
     * The worn nano piece that hosts the nano core (the chest first), or null. Only a piece with
     * installed units counts: a plain nano suit has no lattice in use, no core and no HUD.
     */
    @Nullable
    public static ItemStack nanoPiece(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (hostsCore(chest)) return chest;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (hostsCore(s)) return s;
        }
        return null;
    }

    private static boolean hostsCore(ItemStack stack) {
        return SuitModules.isNanoPiece(stack) && !SuitModules.installed(stack).ids().isEmpty();
    }

    /** True when a worn suit piece of either class carries a unit (the shared passives run then). */
    private static boolean wearsUnits(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = player.getItemBySlot(slot);
            if (SuitModules.isSuitPiece(s) && !SuitModules.installed(s).ids().isEmpty()) return true;
        }
        return false;
    }

    /** A built-in nano unit works: switched on in the config, installed in a worn piece, switched on and powered. */
    public static boolean active(Player player, SuitModuleType type) {
        return NanoConfig.enabled(type) && SuitModules.isActive(player, type);
    }

    /** 0.1.8.6: 0 when the unit does not work, else its tier (1 = Mk.I, 2 = Mk.II). */
    public static int level(Player player, SuitModuleType type) {
        return active(player, type) ? Math.max(1, SuitModules.tier(player, type)) : 0;
    }

    /** Heat cap: 100, +50 per heat-sink fin unit (Mk.II +75); chest and legs fins add up. */
    public static float heatCap(Player player) {
        float cap = HEAT_CAP;
        if (!NanoConfig.enabled(SuitModuleType.HEAT_SINK_FINS)) return cap;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack piece = player.getItemBySlot(slot);
            int tier = SuitModules.isSuitPiece(piece) ? SuitModules.tier(piece, SuitModuleType.HEAT_SINK_FINS.id()) : 0;
            if (tier > 0 && ArmorFeatureState.isEnabled(piece, SuitModuleType.HEAT_SINK_FINS.featureKey())) cap += tier >= 2 ? 75 : 50;
        }
        return cap;
    }

    /** Bursts unlock again below this heat after an overheat (the config share of the cap). */
    public static float resumeHeat(Player player) {
        return heatCap(player) * NanoConfig.overheatResume() / 100F;
    }

    /**
     * HF sync level 0..3 (0.1.8.6): an active nanosaber in the main hand plus the sync sources —
     * nano myofiber (Mk.I 1, Mk.II 2) and the kinetic armor mesh weave (Mk.I 1, Mk.II 2), capped at 3.
     */
    public static int hfLevel(Player player) {
        ItemStack hand = player.getMainHandItem();
        if (!(hand.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber)
                || !com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.isActive(hand)) return 0;
        return Math.min(3, level(player, SuitModuleType.MYOFIBER) + level(player, SuitModuleType.KINETIC_MESH));
    }

    /** HF sync: level 1 or more. */
    public static boolean hfSync(Player player) {
        return hfLevel(player) > 0;
    }

    /** Attack speed bonus of an HF level: L1 the config value (12 %), L2 x1.25, L3 x1.5. */
    public static double hfAttackSpeed(int level) {
        return level <= 0 ? 0 : NanoConfig.hfAttackSpeed() * (level == 1 ? 1.0 : level == 2 ? 1.25 : 1.5);
    }

    /** Armour penetration of an HF level: L1 the config value (15 %), L2 x1.5, L3 x2 (at most 90 %). */
    public static double hfArmorPen(int level) {
        return level <= 0 ? 0 : Math.min(0.9, NanoConfig.hfArmorPen() * (level == 1 ? 1.0 : level == 2 ? 1.5 : 2.0));
    }

    public static boolean lockJammed(Player player) {
        return player.getExistingData(SuitRegistry.NANO_CORE).map(s -> s.lockJamUntil > player.level().getGameTime()).orElse(false);
    }

    /** 0.1.8.6: the jammer of {@code target} keeps guided weapons of {@code shooter} from locking it. */
    public static boolean jammerBlocksLock(Player target) {
        return level(target, SuitModuleType.JAMMER) > 0 && NanoConfig.jammerVsPlayers();
    }

    /** Jammer range: hostile creatures beyond it lose track (Mk.II: two thirds). */
    public static double jammerRange(int tier) {
        return tier >= 2 ? NanoConfig.jammerRange() * 2.0 / 3.0 : NanoConfig.jammerRange();
    }

    public static boolean guidanceJammed(Player player) {
        return player.getExistingData(SuitRegistry.NANO_CORE).map(s -> s.guidanceJamUntil > player.level().getGameTime()).orElse(false);
    }

    public static boolean camoActive(Player player) {
        return player.getExistingData(SuitRegistry.NANO_CORE).map(NanoCoreState::camoActive).orElse(false);
    }

    public static boolean meshActive(Player player) {
        return player.getExistingData(SuitRegistry.NANO_CORE).map(s -> s.meshTicks > 0).orElse(false);
    }

    /** Longest camouflage run of this wearer in ticks (Mk.II x1.5). */
    public static int camoMaxTicks(Player player) {
        int base = NanoConfig.camoMaxTicks();
        return SuitModules.tier(player, SuitModuleType.OPTICAL_CAMO) >= 2 ? base * 3 / 2 : base;
    }

    /** Fall protection cost factor of the nano boots: shock gel Mk.I 0.3, Mk.II 0.15, else 1. */
    public static float fallCostFactor(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player player)) return 1F;
        int gel = level(player, SuitModuleType.SHOCK_GEL);
        return gel == 0 ? 1F : gel >= 2 ? 0.15F : 0.3F;
    }

    // ------------------------------------------------------------------ burst keys

    /** True when the two units may not share the burst keys (kinetic armor mesh and optical camouflage). */
    public static boolean conflicts(UnitRef a, UnitRef b) {
        if (a == null || b == null || a.builtin() == null || b.builtin() == null) return false;
        SuitModuleType x = a.builtin(), y = b.builtin();
        return x == SuitModuleType.KINETIC_MESH && y == SuitModuleType.OPTICAL_CAMO || x == SuitModuleType.OPTICAL_CAMO && y == SuitModuleType.KINETIC_MESH;
    }

    /**
     * Units the burst keys can fire: burst units on worn nano pieces, in key priority order
     * (built-in dash, camouflage, mesh, EMP, recon, then addon units), and since 0.1.10.0 the
     * quantum action units on worn quantum pieces (one key system for both suit classes).
     */
    public static List<UnitRef> availableBursts(Player player) {
        List<UnitRef> out = nanoBursts(player);
        out.addAll(com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.available(player));
        return out;
    }

    /** Burst units on worn nano pieces only. */
    private static List<UnitRef> nanoBursts(Player player) {
        List<UnitRef> out = new ArrayList<>(6);
        for (SuitModuleType type : BURST_PRIORITY) {
            ItemStack piece = SuitModules.wornPieceWith(player, type);
            if (!piece.isEmpty() && SuitModules.isNanoPiece(piece)) out.add(UnitRef.of(type));
        }
        if (!SuitModuleRegistry.isEmpty()) {
            for (var entry : SuitModuleRegistry.all()) {
                if (entry.burst() == null || !entry.traits().allows(SuitClass.NANO)) continue;
                UnitRef unit = UnitRef.of(entry.spec());
                ItemStack piece = SuitModules.wornPieceWith(player, unit);
                if (!piece.isEmpty() && SuitModules.isNanoPiece(piece)) out.add(unit);
            }
        }
        return out;
    }

    /** The unit that burst key {@code slot} fires now, or null. The secondary never takes a unit that conflicts with the primary. */
    @Nullable
    public static UnitRef effectiveBurst(Player player, String primaryChoice, String secondaryChoice, int slot) {
        List<UnitRef> list = availableBursts(player);
        UnitRef primary = resolve(list, primaryChoice, null);
        if (slot == 0) return primary;
        return resolve(list, secondaryChoice, primary);
    }

    @Nullable
    private static UnitRef resolve(List<UnitRef> list, String choice, @Nullable UnitRef taken) {
        if ("-".equals(choice)) return null;
        if (choice != null && !choice.isEmpty()) {
            for (UnitRef u : list) if (u.key().equals(choice) && free(u, taken)) return u;
        }
        for (UnitRef u : list) if (free(u, taken)) return u;
        return null;
    }

    private static boolean free(UnitRef u, @Nullable UnitRef taken) {
        return taken == null || !taken.key().equals(u.key()) && !conflicts(u, taken);
    }

    /** Server: panel assignment of a burst key. A unit on the other key moves over; a conflicting unit there is cleared. */
    static void assign(ServerPlayer player, int slot, String key) {
        if (key.length() > 128) return;
        NanoCoreState s = state(player);
        UnitRef unit = UnitRef.byKey(key);
        if (slot == 0) {
            s.primary = key;
            if (!key.isEmpty() && (key.equals(s.secondary) || conflicts(unit, UnitRef.byKey(s.secondary)))) s.secondary = "";
        } else {
            s.secondary = key;
            if (!key.isEmpty() && (key.equals(s.primary) || conflicts(unit, UnitRef.byKey(s.primary)))) s.primary = "";
        }
        s.sentHash = 0;
    }

    /** Effective burst costs of a unit. */
    @Nullable
    public static SuitBurstSpec burstSpec(UnitRef unit) {
        return unit.builtin() != null ? NanoConfig.burst(unit.builtin()) : unit.burst();
    }

    /** 0.1.10.0: full cooldown of the key unit (a nano burst or a quantum action), for the HUD sweep. */
    private static int cooldownMax(@Nullable UnitRef unit) {
        if (unit == null) return 0;
        if (com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.isAction(unit)) {
            var spec = com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.spec(unit);
            return spec == null ? 0 : Math.max(1, spec.cooldownTicks());
        }
        SuitBurstSpec spec = burstSpec(unit);
        return spec == null ? 0 : Math.max(1, spec.cooldownTicks());
    }

    // ------------------------------------------------------------------ server tick

    /** Called from {@code SuitServer.onPlayerTick} for every server player. */
    public static void tick(ServerPlayer player) {
        ItemStack core = nanoPiece(player);
        NanoCoreState existing = player.getExistingDataOrNull(SuitRegistry.NANO_CORE);
        boolean units = core != null || wearsUnits(player);
        if (!units && existing == null) return;
        NanoCoreState s = existing != null ? existing : state(player);
        // nothing left to do without units once the core is cold, idle and the client knows it is offline
        if (!units && !s.modifiersOn && !s.camoActive() && s.meshTicks <= 0 && s.heat <= 0 && s.cooldown.isEmpty()
            && s.dashTicks <= 0 && s.sentOffline) return;
        // cooldowns and timers
        if (!s.cooldown.isEmpty()) {
            s.cooldown.replaceAll((k, v) -> v - 1);
            s.cooldown.values().removeIf(v -> v <= 0);
        }
        if (s.dashTicks > 0) {
            s.dashTicks--;
            player.fallDistance = 0;
        }
        if (s.reconTicks > 0) s.reconTicks--;
        if (s.meshTicks > 0 && --s.meshTicks == 0) NanoBursts.endMesh(player, s);
        // heat: thermoregulator x2 (Mk.II x2.5), Mk.II fins +25 %
        int thermo = level(player, SuitModuleType.THERMOREGULATOR);
        float shed = (float) (NanoConfig.heatDissipation() / 20.0) * (thermo >= 2 ? 2.5F : thermo == 1 ? 2F : 1F);
        if (SuitModules.tier(player, SuitModuleType.HEAT_SINK_FINS) >= 2 && active(player, SuitModuleType.HEAT_SINK_FINS)) shed *= 1.25F;
        if (s.heat > 0) s.heat = Math.max(0, s.heat - shed);
        if (s.overheated && s.heat <= resumeHeat(player)) s.overheated = false;
        // reserve: every 10 ticks, paid from the suit energy pool
        if (core != null && s.reserve < 100 && player.tickCount % 10 == 0) {
            float points = (float) Math.min(100 - s.reserve, NanoConfig.reserveRegen() / 2.0);
            long cost = (long) Math.ceil(points * NanoConfig.euPerReserve());
            if (points > 0 && (cost == 0 || SuitModules.canPay(player, core, cost))) {
                if (cost > 0) SuitModules.extract(player, core, cost);
                s.reserve = Math.min(100, s.reserve + points);
            }
        }
        if (s.camoActive()) NanoBursts.tickCamo(player, s);
        passives(player, s, thermo);
        generators(player, s);
        if (player.tickCount % 20 == 0) {
            s.generatedLastSecond = s.generatedThisSecond;
            s.generatedThisSecond = 0;
        }
        sync(player, s, core != null);
    }

    private static void passives(ServerPlayer player, NanoCoreState s, int thermo) {
        int myofiber = level(player, SuitModuleType.MYOFIBER);
        int hf = hfLevel(player);
        int hydro = level(player, SuitModuleType.HYDROPHOBIC);
        int gecko = level(player, SuitModuleType.GECKO_SETAE);
        boolean fins = active(player, SuitModuleType.HEAT_SINK_FINS);
        boolean anyMod = myofiber > 0 || hydro > 0 || gecko > 0 || s.overheated;
        if (anyMod || s.modifiersOn) {
            modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPRINT_ID,
                myofiber > 0 && player.isSprinting() ? NanoConfig.sprintBonus() * (myofiber >= 2 ? 22.0 / 15.0 : 1.0) : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            modifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_ID,
                myofiber > 0 && !player.isShiftKeyDown() ? 0.4 : 0, AttributeModifier.Operation.ADD_VALUE);
            modifier(player.getAttribute(Attributes.ATTACK_SPEED), HF_SPEED_ID, hfAttackSpeed(hf), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            modifier(player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY), WATER_ID, hydro > 0 ? 1.0 : 0, AttributeModifier.Operation.ADD_VALUE);
            modifier(player.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED), SWIM_ID,
                hydro > 0 ? NanoConfig.swimBonus() + (hydro >= 2 ? 0.2 : 0) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            modifier(player.getAttribute(Attributes.MOVEMENT_EFFICIENCY), GRIP_ID, gecko > 0 ? 1.0 : 0, AttributeModifier.Operation.ADD_VALUE);
            modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), OVERHEAT_ID, s.overheated && !fins ? -0.2 : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            s.modifiersOn = anyMod || hf > 0;
        }
        if (hf > s.hfLevel) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_HF_SYNC.get(), SoundSource.PLAYERS,
                0.5F, 0.9F + 0.1F * hf);
        }
        s.hfLevel = hf;
        if (thermo > 0 && player.getTicksFrozen() > 0) player.setTicksFrozen(0);
        int toxin = level(player, SuitModuleType.TOXIN_FILTER);
        if (toxin > 0) {
            if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
            if (player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
            if (toxin >= 2) {                                   // Mk.II also filters hunger and nausea
                if (player.hasEffect(MobEffects.HUNGER)) player.removeEffect(MobEffects.HUNGER);
                if (player.hasEffect(MobEffects.CONFUSION)) player.removeEffect(MobEffects.CONFUSION);
            }
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        // gecko setae: a wall climb (the client drives the motion) never ends in a fall
        if (gecko > 0 && player.isShiftKeyDown() && (player.horizontalCollision
                || !level.noCollision(player, player.getBoundingBox().inflate(0.08, -0.1, 0.08)))) {
            player.fallDistance = 0;
            if (player.tickCount % 4 == 0) {
                level.sendParticles(SETAE_DUST, player.getX(), player.getY() + 0.3, player.getZ(), 2, 0.25, 0.2, 0.25, 0.0);
            }
        }
        // self-repair coat: after 5 s without damage (Mk.II 4 s), 0.5 health every 3 s (Mk.II 1.0 every 2 s)
        int repair = level(player, SuitModuleType.SELF_REPAIR);
        if (repair > 0 && player.getHealth() < player.getMaxHealth() && player.isAlive()
                && now - s.lastHit >= (repair >= 2 ? 80 : 100) && player.tickCount % (repair >= 2 ? 40 : 60) == 0) {
            ItemStack piece = SuitModules.wornPieceWith(player, SuitModuleType.SELF_REPAIR);
            long cost = NanoConfig.repairEu();
            if (cost == 0 || SuitModules.canPay(player, piece, cost)) {
                if (cost > 0) SuitModules.extract(player, piece, cost);
                player.heal(repair >= 2 ? 1.0F : 0.5F);
                level.sendParticles(REPAIR_DUST, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.35, 0.5, 0.35, 0.0);
                if (player.tickCount % 120 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_REPAIR.get(), SoundSource.PLAYERS, 0.35F, 1.2F);
                }
            }
        }
        // jammer: every 10 ticks hostile creatures beyond the range lose track of the wearer
        int jammer = level(player, SuitModuleType.JAMMER);
        if (jammer > 0 && player.tickCount % 10 == 0) {
            double range = jammerRange(jammer);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
                    m -> m.getTarget() == player && m instanceof Enemy && m.distanceToSqr(player) > range * range)) {
                mob.setTarget(null);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getEyeY() + 0.4, mob.getZ(), 6, 0.2, 0.15, 0.2, 0.1);
            }
        }
    }

    /** Kinetic generator and solar skin: they charge their own piece, then the other worn electric armour. */
    private static void generators(ServerPlayer player, NanoCoreState s) {
        int kinetic = level(player, SuitModuleType.KINETIC_GENERATOR);
        if (kinetic > 0) {
            double x = player.getX(), z = player.getZ();
            if (!Double.isNaN(s.lastX) && player.onGround() && !player.isPassenger() && !player.isSpectator()) {
                double d = Math.hypot(x - s.lastX, z - s.lastZ);
                if (d < 1.5) s.walked += d * (player.isSprinting() ? 2 : 1) * (kinetic >= 2 ? 2 : 1);
            }
            s.lastX = x;
            s.lastZ = z;
            if (s.walked >= 1) {
                long eu = (long) Math.floor(s.walked);
                s.walked -= eu;
                s.generatedThisSecond += (int) charge(player, SuitModules.wornPieceWith(player, SuitModuleType.KINETIC_GENERATOR), eu);
            }
        } else {
            s.lastX = Double.NaN;
        }
        int solar = level(player, SuitModuleType.SOLAR_SKIN);
        if (solar > 0 && player.tickCount % 20 == 0) {
            ServerLevel level = player.serverLevel();
            var pos = net.minecraft.core.BlockPos.containing(player.getEyePosition());
            if (level.isDay() && !level.isRainingAt(pos) && level.canSeeSky(pos)) {
                long eu = (long) NanoConfig.solarEuPerTick() * 20L * (solar >= 2 ? 2 : 1);
                s.generatedThisSecond += (int) charge(player, SuitModules.wornPieceWith(player, SuitModuleType.SOLAR_SKIN), eu);
            }
        }
    }

    private static long charge(Player player, ItemStack first, long eu) {
        long left = eu;
        if (first.getItem() instanceof AbstractElectricArmor armor) left -= armor.addEnergy(first, left);
        for (EquipmentSlot slot : ARMOR) {
            if (left <= 0) break;
            ItemStack piece = player.getItemBySlot(slot);
            if (piece != first && piece.getItem() instanceof AbstractElectricArmor armor) left -= armor.addEnergy(piece, left);
        }
        return eu - left;
    }

    private static void modifier(@Nullable AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        if (attribute == null) return;
        if (amount == 0) {
            if (attribute.hasModifier(id)) attribute.removeModifier(id);
            return;
        }
        AttributeModifier current = attribute.getModifier(id);
        if (current == null || current.amount() != amount) attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** Adds heat; at the cap the suit overheats (bursts lock, the camouflage drops). */
    static void addHeat(ServerPlayer player, NanoCoreState s, float heat) {
        float cap = heatCap(player);
        s.heat = Math.min(cap, s.heat + heat);
        if (!s.overheated && s.heat >= cap - 0.01F) {
            s.overheated = true;
            if (s.camoActive()) NanoBursts.endCamo(player, s, 1);
            NanoPackets.broadcast(player, NanoPackets.Fx.simple(player, NanoPackets.FX_OVERHEAT, 0, 0, 0), 48);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.NANO_OVERHEAT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ sync

    static void sync(ServerPlayer player, NanoCoreState s, boolean online) {
        if (++s.syncTimer < 2) return;
        UnitRef p = effectiveBurst(player, s.primary, s.secondary, 0);
        UnitRef q = effectiveBurst(player, s.primary, s.secondary, 1);
        String pk = p == null ? "" : p.key(), qk = q == null ? "" : q.key();
        int pc = p == null ? 0 : s.cooldown(pk), qc = q == null ? 0 : s.cooldown(qk);
        // 0.1.10.0: the keys also serve the quantum actions; "quantum" tells the client that a quantum action is on offer
        boolean quantum = com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.isAction(p)
            || com.miophas.singularity_iteration.common.suit.quantum.QuantumActions.isAction(q);
        // 0.1.10.8: the values of Objects.hash(...) without the array and the boxing
        int hash = com.miophas.singularity_iteration.common.suit.SyncHash.START;
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.overheated);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.camoTicks > 0);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.meshTicks > 0);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.reconTicks > 0);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.primary);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.secondary);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, pk);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, qk);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, pc);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, qc);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, online);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.hfLevel);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, s.generatedLastSecond);
        hash = com.miophas.singularity_iteration.common.suit.SyncHash.add(hash, quantum);
        boolean changed = hash != s.sentHash || Math.abs(s.reserve - s.sentReserve) >= 0.5F || Math.abs(s.heat - s.sentHeat) >= 0.5F
            || s.camoActive() || s.meshTicks > 0;
        if (!changed && s.syncTimer < 20) return;
        s.syncTimer = 0;
        s.sentHash = hash;
        s.sentOffline = !online && !quantum;
        s.sentReserve = s.reserve;
        s.sentHeat = s.heat;
        SuitPackets.send(player, new NanoPackets.CoreSync(s.reserve, s.heat, heatCap(player), s.overheated, s.camoTicks, camoMaxTicks(player),
            s.primary, s.secondary, pk, qk, pc, cooldownMax(p), qc, cooldownMax(q),
            online, s.meshTicks, s.meshMax, s.reconTicks, s.reconMax, s.hfLevel, s.generatedLastSecond, resumeHeat(player), quantum));
    }
}
