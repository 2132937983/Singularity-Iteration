package example.sicore;

import com.miophas.singularity_iteration.core.api.suit.SuitBurstSpec;
import com.miophas.singularity_iteration.core.api.suit.SuitClass;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleTraits;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * GameTests of 0.1.8.5: nano suit units (lattice points, suit classes, capacitor, station,
 * burst keys, EMP, camouflage) and the public traits API.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round185GameTests {

    private static Object check(ItemStack piece, String unit) {
        return Round33GameTests.call(Round33GameTests.common("suit.SuitModules"), "check", piece, Round33GameTests.unitType(unit));
    }

    /** A nano piece counts lattice points: 6 LP in a chestplate, +2 per densifier; class rules hold. */
    @GameTest(template = "empty", batch = "round185_rules")
    public static void nanoPiecesCountLatticePoints(GameTestHelper h) {
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        Class<?> modules = Round33GameTests.common("suit.SuitModules");
        h.assertTrue((int) Round33GameTests.call(modules, "latticeCapacity", chest) == 6, "nano chestplate lattice is not 6 LP");
        h.assertTrue(Round33GameTests.install(chest, "emp_emitter"), "EMP emitter (3 LP) did not fit");
        h.assertTrue(Round33GameTests.install(chest, "optical_camo"), "optical camouflage (3 LP) did not fit");
        h.assertTrue(String.valueOf(check(chest, "thermoregulator")).equals("NO_LATTICE"), "a full lattice took more: " + check(chest, "thermoregulator"));
        h.assertTrue((boolean) Round33GameTests.call(modules, "densify", chest), "densifier refused");
        h.assertTrue((int) Round33GameTests.call(modules, "latticeCapacity", chest) == 8, "densifier did not add 2 LP");
        h.assertTrue(String.valueOf(check(chest, "thermoregulator")).equals("OK"), "thermoregulator (2 LP) does not fit after a densifier");
        Round33GameTests.call(modules, "densify", chest);
        h.assertTrue(!(boolean) Round33GameTests.call(modules, "densify", chest), "a third densifier was accepted");
        // classes: quantum-only units do not fit nano pieces, nano-only units do not fit quantum pieces, shared ones fit both
        h.assertTrue(String.valueOf(check(chest, "deflector")).equals("WRONG_CLASS"), "deflector on a nano piece: " + check(chest, "deflector"));
        ItemStack quantum = Round33GameTests.charged("armor/item_armor_quantum_chestplate");
        h.assertTrue(String.valueOf(check(quantum, "emp_emitter")).equals("WRONG_CLASS"), "EMP on a quantum piece: " + check(quantum, "emp_emitter"));
        h.assertTrue(String.valueOf(check(quantum, "threat_sensor")).equals("OK"), "threat sensor no longer fits a quantum chestplate");
        h.assertTrue(String.valueOf(check(chest, "threat_sensor")).equals("OK"), "shared threat sensor does not fit a nano chestplate");
        ItemStack helmet = Round33GameTests.charged("armor/item_armor_quantum_helmet");
        h.assertTrue(String.valueOf(check(helmet, "toxin_filter")).equals("OK"), "shared toxin filter does not fit a quantum helmet");
        h.succeed();
    }

    /** The micro-capacitor lattice adds 250 000 EU to its nano piece; the switch list stays free of it. */
    @GameTest(template = "empty", batch = "round185_rules")
    public static void microCapacitorAddsStorage(GameTestHelper h) {
        ItemStack helmet = Round33GameTests.item("armor/item_armor_nano_helmet");
        var armor = (AbstractElectricArmor) helmet.getItem();
        long base = armor.getMaxEnergy(helmet);
        h.assertTrue(Round33GameTests.install(helmet, "micro_capacitor"), "capacitor did not fit the nano helmet");
        h.assertTrue(armor.getMaxEnergy(helmet) == base + 250_000, "capacity " + armor.getMaxEnergy(helmet) + ", expected " + (base + 250_000));
        armor.setEnergy(helmet, armor.getMaxEnergy(helmet));
        h.assertTrue(armor.getEnergy(helmet) == base + 250_000, "the extra capacity does not store energy");
        h.assertTrue(ArmorFeatures.features(helmet).stream().noneMatch(f -> f.featureKey().equals("fcs_micro_capacitor")),
            "passive capacitor shows a switch");
        h.succeed();
    }

    /** The station takes nano pieces and the densifier; a densifier on a quantum piece is refused. */
    @GameTest(template = "reactor_loop", batch = "round185_station", timeoutTicks = 40)
    public static void stationTakesNanoPiecesAndDensifier(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_quantum_modification_station")));
        Object be = h.getLevel().getBlockEntity(h.absolutePos(at));
        IItemHandlerModifiable slots = (IItemHandlerModifiable) Round33GameTests.call(be, "getItemHandler");
        slots.setStackInSlot(1, Round33GameTests.charged("armor/item_armor_nano_chestplate"));
        slots.setStackInSlot(2, Round33GameTests.item("module/item_nano_lattice_densifier"));
        h.assertTrue(String.valueOf(Round33GameTests.call(be, "evaluate")).equals("WORKING"), "densifier on nano: " + Round33GameTests.call(be, "evaluate"));
        slots.setStackInSlot(2, Round33GameTests.unit("myofiber"));
        h.assertTrue(String.valueOf(Round33GameTests.call(be, "evaluate")).equals("WRONG_SLOT"), "myofiber in a chestplate: " + Round33GameTests.call(be, "evaluate"));
        slots.setStackInSlot(1, Round33GameTests.charged("armor/item_armor_quantum_chestplate"));
        slots.setStackInSlot(2, Round33GameTests.item("module/item_nano_lattice_densifier"));
        h.assertTrue(String.valueOf(Round33GameTests.call(be, "evaluate")).equals("WRONG_CLASS"), "densifier on quantum: " + Round33GameTests.call(be, "evaluate"));
        h.succeed();
    }

    /** Burst keys pick worn burst units in order (dash, camouflage, EMP); an assignment moves a unit between keys. */
    @GameTest(template = "empty", batch = "round185_keys")
    public static void burstKeysResolve(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        Round33GameTests.install(chest, "emp_emitter");
        Round33GameTests.install(chest, "optical_camo");
        ItemStack legs = Round33GameTests.charged("armor/item_armor_nano_leggings");
        Round33GameTests.install(legs, "myofiber");
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        Class<?> nano = Round33GameTests.common("suit.nano.NanoSuit");
        List<?> bursts = (List<?>) Round33GameTests.call(nano, "availableBursts", player);
        h.assertTrue(bursts.size() == 3, "expected 3 burst units, got " + bursts);
        h.assertTrue(key(Round33GameTests.call(nano, "effectiveBurst", player, "", "", 0)).equals("myofiber"), "auto primary is not the dash");
        h.assertTrue(key(Round33GameTests.call(nano, "effectiveBurst", player, "", "", 1)).equals("optical_camo"), "auto secondary is not the camouflage");
        h.assertTrue(key(Round33GameTests.call(nano, "effectiveBurst", player, "emp_emitter", "", 0)).equals("emp_emitter"), "primary choice ignored");
        h.assertTrue(key(Round33GameTests.call(nano, "effectiveBurst", player, "emp_emitter", "", 1)).equals("myofiber"), "auto secondary did not skip the primary");
        h.assertTrue(Round33GameTests.call(nano, "effectiveBurst", player, "-", "-", 0) == null, "\"-\" still fires a unit");
        Round33GameTests.call(nano, "assign", player, 1, "myofiber");
        Round33GameTests.call(nano, "assign", player, 0, "myofiber");
        Object state = Round33GameTests.call(nano, "state", player);
        h.assertTrue("".equals(ArmoryServiceGameTests.field(state, "secondary")), "a unit stayed on both keys");
        h.succeed();
    }

    private static String key(Object unitRef) {
        return unitRef == null ? "" : String.valueOf(Round33GameTests.call(unitRef, "key"));
    }

    /** EMP: a hostile arrow in range is erased and the reserve and heat are paid; an overheated suit refuses. */
    @GameTest(template = "empty", batch = "round185_emp", timeoutTicks = 20)
    public static void empErasesProjectilesAndPaysCosts(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(h.absoluteVec(new Vec3(2, 2, 2)));
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        Round33GameTests.install(chest, "emp_emitter");
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        var skeleton = h.spawn(EntityType.SKELETON, new BlockPos(8, 2, 8));
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        arrow.setOwner(skeleton);
        arrow.setPos(player.position().add(1.5, 1, 0));
        h.getLevel().addFreshEntity(arrow);
        Class<?> nano = Round33GameTests.common("suit.nano.NanoSuit");
        Object state = Round33GameTests.call(nano, "state", player);
        Class<?> bursts = Round33GameTests.common("suit.nano.NanoBursts");
        Round33GameTests.call(nano, "assign", player, 0, "emp_emitter");
        Round33GameTests.call(bursts, "request", player, 0, 0F, 0F);
        h.assertTrue(arrow.isRemoved(), "the EMP left the hostile arrow");
        float reserve = (float) ArmoryServiceGameTests.field(state, "reserve");
        float heat = (float) ArmoryServiceGameTests.field(state, "heat");
        h.assertTrue(Math.abs(reserve - 65) < 0.01F, "reserve after EMP " + reserve + ", expected 65");
        h.assertTrue(Math.abs(heat - 40) < 0.01F, "heat after EMP " + heat + ", expected 40");
        h.assertTrue((int) Round33GameTests.call(state, "cooldown", "emp_emitter") > 0, "no cooldown after the EMP");
        h.succeed();
    }

    /** Optical camouflage: starts, a hit ends it with a cooldown; the reserve drains while it runs. */
    @GameTest(template = "empty", batch = "round185_camo", timeoutTicks = 40)
    public static void camouflageEndsOnHit(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        Round33GameTests.install(chest, "optical_camo");
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        Class<?> nano = Round33GameTests.common("suit.nano.NanoSuit");
        Class<?> bursts = Round33GameTests.common("suit.nano.NanoBursts");
        Object state = Round33GameTests.call(nano, "state", player);
        Round33GameTests.call(nano, "assign", player, 0, "optical_camo");
        Round33GameTests.call(bursts, "request", player, 0, 0F, 0F);
        h.assertTrue((boolean) Round33GameTests.call(state, "camoActive"), "camouflage did not start");
        // a mock player is not ticked by a connection: run the nano core tick by hand
        for (int i = 0; i < 10; i++) Round33GameTests.call(nano, "tick", player);
        float reserve = (float) ArmoryServiceGameTests.field(state, "reserve");
        h.assertTrue(reserve < 100, "camouflage did not drain the reserve");
        h.assertTrue((boolean) Round33GameTests.call(state, "camoActive"), "camouflage ended by itself");
        // generic_kill bypasses the spawn protection of the fresh player; 1 point is a hit
        player.hurt(player.damageSources().genericKill(), 1.0F);
        h.assertTrue(!(boolean) Round33GameTests.call(state, "camoActive"), "a hit did not end the camouflage");
        h.assertTrue((int) Round33GameTests.call(state, "cooldown", "optical_camo") > 0, "no cooldown after the camouflage");
        h.succeed();
    }

    /** Public API: traits validate their input, and the registry demands both burst parts. */
    @GameTest(template = "empty", batch = "round185_api")
    public static void traitsApiValidates(GameTestHelper h) {
        SuitModuleTraits nano = SuitModuleTraits.nano(2, new SuitBurstSpec(10, 5, 20, false));
        h.assertTrue(nano.allows(SuitClass.NANO) && !nano.allows(SuitClass.QUANTUM), "nano traits allow the wrong classes");
        h.assertTrue(SuitModuleTraits.QUANTUM_ONLY.allows(SuitClass.QUANTUM), "QUANTUM_ONLY does not allow the quantum class");
        boolean threw = false;
        try {
            new SuitModuleTraits(0, EnumSet.of(SuitClass.NANO), null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        h.assertTrue(threw, "0 lattice points accepted");
        threw = false;
        try {
            com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry.register(ExampleSonarUnit.SPEC, null, nano, null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        h.assertTrue(threw, "a burst spec without a burst behavior was accepted");
        h.succeed();
    }
}
