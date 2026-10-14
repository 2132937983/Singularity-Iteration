package example.sicore;

import com.miophas.singularity_iteration.core.api.suit.SuitBandwidth;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumCore;
import com.miophas.singularity_iteration.core.api.suit.SuitSynergies;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.1 (quantum phase Q2): the entanglement links (hub, units, capacity, compute
 * core), the compute bandwidth (the heaviest 0.1.9.2 suit fits the budget; Mk.II saturates; the
 * core lifts the budget), Predictive Guidance, the Quantum Tunnel discount, the Resonant
 * Deflection burst, the Afterimage Decoy and the public synergy and bandwidth API.
 * The SI common classes are reached by reflection (the addon module may only use the core API).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1101GameTests {
    private static final String LINKS = "suit.quantum.QuantumLinks", BANDWIDTH = "suit.quantum.QuantumBandwidth",
        SYNERGIES = "suit.quantum.QuantumSynergies", CORE = "suit.quantum.QuantumCore", ACTIONS = "suit.quantum.QuantumActions";
    private static final int LINKED = 0, UNLINKED = 1, NO_HUB = 2, FULL = 3, MISSING_UNITS = 4, UNKNOWN = 5;

    static ItemStack piece(String path, String... units) {
        ItemStack stack = Round1100GameTests.charged("armor/item_armor_quantum_" + path);
        for (String u : units) {
            if (!Round1100GameTests.install(stack, u, 1)) throw new AssertionError("install " + u + " on " + path);
        }
        return stack;
    }

    static int toggle(ServerPlayer p, String key) {
        return (Integer) Round33GameTests.call(Round33GameTests.common(LINKS), "toggle", p, key);
    }

    static boolean active(ServerPlayer p, String key) {
        return (Boolean) Round33GameTests.call(Round33GameTests.common(LINKS), "active", p, key);
    }

    static int capacity(ServerPlayer p) {
        return (Integer) Round33GameTests.call(Round33GameTests.common(LINKS), "capacity", p);
    }

    static float floatField(Object target, String name) {
        try {
            return target.getClass().getField(name).getFloat(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    static void setLong(Object target, String name, long value) {
        try {
            target.getClass().getField(name).setLong(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    /** No hub, missing units, full hub, unlink, unknown key; the compute core opens a third link. */
    @GameTest(template = "empty", batch = "round1101_links", timeoutTicks = 20)
    public static void linksNeedHubUnitsAndCapacity(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemSlot(EquipmentSlot.HEAD, piece("helmet", "SMART_GUIDANCE", "BEHAVIOR_PREDICTOR", "ENTITY_ESP", "BALLISTIC"));
        h.assertTrue(toggle(p, "predictive_guidance") == NO_HUB, "a link without a quantum chestplate");
        ItemStack chest = piece("chestplate", "THREAT_SENSOR");
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        h.assertTrue(capacity(p) == 2, "base capacity " + capacity(p));
        h.assertTrue(toggle(p, "fleet_datalink") == MISSING_UNITS, "fleet datalink linked without the relay");
        h.assertTrue(toggle(p, "predictive_guidance") == LINKED, "predictive guidance did not link");
        h.assertTrue(toggle(p, "ballistic_sync") == LINKED, "ballistic sync did not link");
        h.assertTrue(toggle(p, "threat_sight") == FULL, "a third link fit a base hub");
        h.assertTrue(active(p, "predictive_guidance") && active(p, "ballistic_sync"), "linked synergies are not active");
        h.assertTrue(toggle(p, "no_such_synergy") == UNKNOWN, "an unknown key was not refused");
        // the compute core: one more link
        Round1100GameTests.install(chest, "COMPUTE_CORE", 1);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        h.assertTrue(capacity(p) == 3, "capacity with a compute core " + capacity(p));
        h.assertTrue(toggle(p, "threat_sight") == LINKED, "the third link did not fit with a compute core");
        h.assertTrue(toggle(p, "predictive_guidance") == UNLINKED && !active(p, "predictive_guidance"), "unlink failed");
        h.succeed();
    }

    /** The heaviest 0.1.9.2 suit loads exactly 20 QB (not saturated); a Mk.II saturates it; a compute core lifts the budget to 28. */
    @GameTest(template = "empty", batch = "round1101_bandwidth", timeoutTicks = 20)
    public static void legacyMaxFitsTheBudget(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack helmet = piece("helmet", "ENTITY_ESP", "BEHAVIOR_PREDICTOR", "HOLOMAP", "SMART_GUIDANCE");
        ItemStack chest = piece("chestplate", "THREAT_SENSOR", "BLAST_WARNING", "DEFLECTOR");
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        p.setItemSlot(EquipmentSlot.LEGS, piece("leggings", "GRID_TELEMETRY", "RESONANCE_FIELD"));
        p.setItemSlot(EquipmentSlot.FEET, piece("boots", "ORE_SCANNER", "RESONANCE_FIELD"));   // the field twice: counted once
        h.assertTrue(SuitBandwidth.load(p) == 20 && SuitBandwidth.budget(p) == 20, "legacy load " + SuitBandwidth.load(p) + "/" + SuitBandwidth.budget(p));
        h.assertTrue(!(Boolean) Round33GameTests.call(Round33GameTests.common(BANDWIDTH), "saturated", p), "the legacy maximum saturates");
        Round1100GameTests.install(helmet, "ENTITY_ESP", 2);                // Mk.II: +1 QB
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        h.assertTrue(SuitBandwidth.load(p) == 21 && (Boolean) Round33GameTests.call(Round33GameTests.common(BANDWIDTH), "saturated", p),
            "a Mk.II ESP did not saturate: " + SuitBandwidth.load(p));
        h.assertTrue((Boolean) Round33GameTests.call(Round33GameTests.common("suit.SuitModules"), "expand", chest), "the chest bay was refused");
        Round1100GameTests.install(chest, "COMPUTE_CORE", 1);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        h.assertTrue(SuitBandwidth.budget(p) == 28 && !(Boolean) Round33GameTests.call(Round33GameTests.common(BANDWIDTH), "saturated", p),
            "compute core budget " + SuitBandwidth.budget(p));
        h.succeed();
    }

    /** Predictive Guidance: +2 locks and +4 degrees of turn; Mk.II guidance on top. */
    @GameTest(template = "empty", batch = "round1101_synergies", timeoutTicks = 20)
    public static void predictiveGuidanceAddsLocks(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack helmet = piece("helmet", "SMART_GUIDANCE", "BEHAVIOR_PREDICTOR");
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        p.setItemSlot(EquipmentSlot.CHEST, piece("chestplate"));
        Class<?> guidance = Round33GameTests.common("suit.SmartGuidance");
        h.assertTrue((Integer) Round33GameTests.call(guidance, "maxLocks", p) == 6, "Mk.I locks");
        h.assertTrue(toggle(p, "predictive_guidance") == LINKED, "link failed");
        h.assertTrue((Integer) Round33GameTests.call(guidance, "maxLocks", p) == 8, "locks with the synergy");
        h.assertTrue((Double) Round33GameTests.call(guidance, "turnDegrees", p) == 18.0, "turn with the synergy");
        Round1100GameTests.install(helmet, "SMART_GUIDANCE", 2);
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        h.assertTrue((Integer) Round33GameTests.call(guidance, "maxLocks", p) == 12, "Mk.II locks with the synergy");
        h.assertTrue((Double) Round33GameTests.call(guidance, "turnDegrees", p) == 24.0, "Mk.II turn with the synergy");
        h.succeed();
    }

    /** Quantum Tunnel: a phase shift right after a blink costs half; later it costs the full price. */
    @GameTest(template = "empty", batch = "round1101_combos", timeoutTicks = 20)
    public static void quantumTunnelHalvesThePhaseCost(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemSlot(EquipmentSlot.CHEST, piece("chestplate", "PHASE_MODULATOR"));
        Object state = Round33GameTests.call(Round33GameTests.common(CORE), "state", p);
        Object keys = Round33GameTests.call(Round33GameTests.common("suit.nano.NanoSuit"), "state", p);
        Object unit = Round1100GameTests.unitRef("PHASE_MODULATOR");
        setLong(state, "lastBlink", h.getLevel().getGameTime());
        Round33GameTests.call(Round33GameTests.common(ACTIONS), "request", p, keys, unit, Vec3.ZERO);
        float spent = 100F - SuitQuantumCore.coherence(p);
        h.assertTrue(Math.abs(spent - 17.5F) < 0.01F, "tunnel phase shift cost " + spent);
        h.succeed();
    }

    /** Resonant Deflection: enough deflected damage bursts on the hostile creatures around. */
    @GameTest(template = "empty", batch = "round1101_synergies", timeoutTicks = 20)
    public static void resonantDeflectionBursts(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemSlot(EquipmentSlot.CHEST, piece("chestplate", "DEFLECTOR"));
        p.setItemSlot(EquipmentSlot.LEGS, piece("leggings", "RESONANCE_FIELD"));
        h.assertTrue(toggle(p, "resonant_deflection") == LINKED && active(p, "resonant_deflection"), "resonant deflection is not active");
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0, 2, 1.5));
        float before = zombie.getHealth();
        Object state = Round33GameTests.call(Round33GameTests.common(CORE), "state", p);
        Round33GameTests.call(Round33GameTests.common(SYNERGIES), "deflected", p, 12F);         // 25 % = 3: below the threshold
        h.assertTrue(zombie.getHealth() == before && floatField(state, "resonance") > 2.9F, "an early burst or no charge");
        Round33GameTests.call(Round33GameTests.common(SYNERGIES), "deflected", p, 24F);         // +6 = 9: bursts
        h.assertTrue(zombie.getHealth() < before && floatField(state, "resonance") == 0F, "no burst: " + zombie.getHealth());
        h.succeed();
    }

    /** Afterimage Decoy: a hunter loses the wearer and may not take him back while the decoy lasts. */
    @GameTest(template = "empty", batch = "round1101_synergies", timeoutTicks = 20)
    public static void afterimageDecoyDrawsHunters(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.setItemSlot(EquipmentSlot.HEAD, piece("helmet", "BEHAVIOR_PREDICTOR"));
        p.setItemSlot(EquipmentSlot.CHEST, piece("chestplate"));
        p.setItemSlot(EquipmentSlot.LEGS, piece("leggings", "PHASE_JUMP_DRIVE"));
        h.assertTrue(toggle(p, "afterimage_decoy") == LINKED, "decoy link failed");
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(4.5, 2, 1.5));
        zombie.setTarget(p);
        Object state = Round33GameTests.call(Round33GameTests.common(CORE), "state", p);
        Round33GameTests.call(Round33GameTests.common(SYNERGIES), "decoy", p, state, p.position());
        h.assertTrue(zombie.getTarget() == null, "the hunter kept its target");
        h.assertTrue((Boolean) Round33GameTests.call(Round33GameTests.common(SYNERGIES), "drawnByDecoy", zombie, p), "the hunter is not drawn");
        h.succeed();
    }

    /** The synergy and bandwidth API: addon entries register and resolve; bad ids and costs are refused. */
    @GameTest(template = "empty", batch = "round1101_api")
    public static void synergyAndBandwidthApi(GameTestHelper h) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "test_synergy");
        SuitSynergies.register(id, CoreExampleMod.ID + ":test_unit", "entity_esp");
        h.assertTrue(SuitSynergies.get(id.toString()) != null, "the registered synergy is missing");
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) Round33GameTests.call(Round33GameTests.common(LINKS), "allKeys");
        h.assertTrue(keys.contains(id.toString()) && keys.indexOf("predictive_guidance") == 0, "the link list " + keys);
        h.assertTrue(refused(() -> SuitSynergies.register(ResourceLocation.withDefaultNamespace("bad"), "a:b", "entity_esp")), "a minecraft: synergy");
        h.assertTrue(refused(() -> SuitSynergies.register(ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "same"), "entity_esp", "entity_esp")),
            "a synergy of one unit with itself");
        ResourceLocation unit = ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "test_unit");
        SuitBandwidth.register(unit, 5);
        h.assertTrue(SuitBandwidth.cost(unit.toString()) == 5 && SuitBandwidth.cost(CoreExampleMod.ID + ":unknown") == 0, "addon cost");
        h.assertTrue(refused(() -> SuitBandwidth.register(unit, SuitBandwidth.MAX_COST + 1)), "a cost over the maximum");
        h.assertTrue(refused(() -> SuitBandwidth.register(ResourceLocation.withDefaultNamespace("bad"), 1)), "a minecraft: unit cost");
        h.succeed();
    }

    private static boolean refused(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
