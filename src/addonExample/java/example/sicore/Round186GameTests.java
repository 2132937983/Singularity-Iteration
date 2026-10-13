package example.sicore;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * GameTests of 0.1.8.6 (nano suit phase 2): Mk.II tiers and the station upgrade, Mk.II drain,
 * heat-sink fins, the mesh/camouflage key rule, the kinetic mesh damage cut, the recon burst,
 * self-repair on a quantum piece and the nano to quantum rebuild.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round186GameTests {

    private static Class<?> modules() { return Round33GameTests.common("suit.SuitModules"); }
    private static Class<?> nano() { return Round33GameTests.common("suit.nano.NanoSuit"); }
    private static Class<?> bursts() { return Round33GameTests.common("suit.nano.NanoBursts"); }

    private static Object ref(String id) {
        return Round33GameTests.call(Round33GameTests.common("suit.UnitRef"), "of", Round33GameTests.unitType(id));
    }

    private static boolean install(ItemStack piece, String id, int tier) {
        return (boolean) Round33GameTests.call(modules(), "install", piece, ref(id), tier);
    }

    private static int tier(ItemStack piece, String id) {
        return (int) Round33GameTests.call(modules(), "tier", piece, id);
    }

    static void set(Object target, String name, Object value) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new AssertionError(name, e);
            }
        }
        throw new AssertionError("no field " + name);
    }

    /** Mk.II over Mk.I is an in-place upgrade: same LP, the tier is stored, removal clears it; the station wants one item. */
    @GameTest(template = "reactor_loop", batch = "round186_tiers", timeoutTicks = 40)
    public static void mk2UpgradesInPlace(GameTestHelper h) {
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        h.assertTrue(install(chest, "emp_emitter", 1), "Mk.I EMP did not fit");
        int used = (int) Round33GameTests.call(modules(), "latticeUsed", chest);
        h.assertTrue(install(chest, "emp_emitter", 2), "Mk.II did not upgrade the Mk.I");
        h.assertTrue(tier(chest, "emp_emitter") == 2, "tier is not 2 after the upgrade");
        h.assertTrue((int) Round33GameTests.call(modules(), "latticeUsed", chest) == used, "the upgrade took extra LP");
        h.assertTrue(!install(chest, "emp_emitter", 2), "a second Mk.II was accepted");
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_quantum_modification_station")));
        Object be = h.getLevel().getBlockEntity(h.absolutePos(at));
        IItemHandlerModifiable slots = (IItemHandlerModifiable) Round33GameTests.call(be, "getItemHandler");
        ItemStack fresh = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        install(fresh, "optical_camo", 1);
        slots.setStackInSlot(1, fresh);
        ItemStack mk2 = Round33GameTests.item("module/item_module_optical_camo_mk2");
        slots.setStackInSlot(2, mk2.copyWithCount(2));
        h.assertTrue(String.valueOf(Round33GameTests.call(be, "evaluate")).equals("UPGRADE_ONE_AT_A_TIME"), "two Mk.II: " + Round33GameTests.call(be, "evaluate"));
        slots.setStackInSlot(2, mk2.copyWithCount(1));
        h.assertTrue(String.valueOf(Round33GameTests.call(be, "evaluate")).equals("UPGRADING"), "one Mk.II: " + Round33GameTests.call(be, "evaluate"));
        Round33GameTests.call(modules(), "remove", chest, ref("emp_emitter"));
        h.assertTrue(tier(chest, "emp_emitter") == 0, "the removed unit kept a tier");
        h.succeed();
    }

    /** A Mk.II unit draws 20 % less; the jammer (4 EU/t) draws 64 instead of 80 EU per second. */
    @GameTest(template = "empty", batch = "round186_tiers")
    public static void mk2DrawsLess(GameTestHelper h) {
        Object jammer = Round33GameTests.unitType("jammer");
        long mk1 = (long) Round33GameTests.call(modules(), "drainPerSecond", jammer, 1);
        long mk2 = (long) Round33GameTests.call(modules(), "drainPerSecond", jammer, 2);
        h.assertTrue(mk1 == 80 && mk2 == 64, "drain per second Mk.I " + mk1 + ", Mk.II " + mk2);
        h.succeed();
    }

    /** Heat-sink fins add to the heat cap: chest Mk.I +50 and legs Mk.II +75. */
    @GameTest(template = "empty", batch = "round186_core")
    public static void finsRaiseTheHeatCap(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        install(chest, "heat_sink_fins", 1);
        ItemStack legs = Round33GameTests.charged("armor/item_armor_nano_leggings");
        install(legs, "heat_sink_fins", 2);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        h.assertTrue((float) Round33GameTests.call(nano(), "heatCap", player) == 150F, "cap with chest fins " + Round33GameTests.call(nano(), "heatCap", player));
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        h.assertTrue((float) Round33GameTests.call(nano(), "heatCap", player) == 225F, "cap with both fins " + Round33GameTests.call(nano(), "heatCap", player));
        h.succeed();
    }

    /** The kinetic mesh and the optical camouflage never share the keys; the secondary skips the conflicting unit. */
    @GameTest(template = "empty", batch = "round186_core")
    public static void meshAndCamoDoNotShareKeys(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        Round33GameTests.call(modules(), "densify", chest);
        Round33GameTests.call(modules(), "densify", chest);
        h.assertTrue(install(chest, "kinetic_mesh", 1) && install(chest, "optical_camo", 1) && install(chest, "emp_emitter", 1), "units did not fit 10 LP");
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        Object second = Round33GameTests.call(nano(), "effectiveBurst", player, "kinetic_mesh", "optical_camo", 1);
        h.assertTrue(second != null && "emp_emitter".equals(String.valueOf(Round33GameTests.call(second, "key"))),
            "secondary with the mesh on primary: " + second);
        Round33GameTests.call(nano(), "assign", player, 0, "kinetic_mesh");
        Round33GameTests.call(nano(), "assign", player, 1, "optical_camo");
        Object state = Round33GameTests.call(nano(), "state", player);
        h.assertTrue("".equals(ArmoryServiceGameTests.field(state, "primary")), "the mesh stayed on the primary next to the camouflage");
        h.succeed();
    }

    /** Kinetic mesh: a hit while it runs costs less health than the same hit without it. */
    @GameTest(template = "empty", batch = "round186_mesh", timeoutTicks = 20)
    public static void meshCutsDamage(GameTestHelper h) {
        ServerPlayer plain = h.makeMockServerPlayerInLevel();
        ServerPlayer meshed = h.makeMockServerPlayerInLevel();
        for (ServerPlayer p : new ServerPlayer[]{plain, meshed}) {
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            set(p, "spawnInvulnerableTime", 0);
            ItemStack chest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
            install(chest, "kinetic_mesh", 1);
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            p.setHealth(20);
        }
        Round33GameTests.call(nano(), "assign", meshed, 0, "kinetic_mesh");
        Round33GameTests.call(bursts(), "request", meshed, 0, 0F, 0F);
        h.assertTrue((boolean) Round33GameTests.call(nano(), "meshActive", meshed), "the mesh did not start");
        plain.hurt(plain.damageSources().generic(), 10F);
        meshed.hurt(meshed.damageSources().generic(), 10F);
        float lostPlain = 20 - plain.getHealth(), lostMeshed = 20 - meshed.getHealth();
        h.assertTrue(lostPlain > 0 && lostMeshed < lostPlain * 0.8F, "lost " + lostMeshed + " with the mesh, " + lostPlain + " without");
        h.succeed();
    }

    /** Recon cloud: the burst starts the glow timer and the cooldown. */
    @GameTest(template = "empty", batch = "round186_core")
    public static void reconStartsTimer(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack helmet = Round33GameTests.charged("armor/item_armor_nano_helmet");
        install(helmet, "recon_cloud", 2);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        Round33GameTests.call(nano(), "assign", player, 0, "recon_cloud");
        Round33GameTests.call(bursts(), "request", player, 0, 0F, 0F);
        Object state = Round33GameTests.call(nano(), "state", player);
        h.assertTrue((int) ArmoryServiceGameTests.field(state, "reconTicks") == 300, "Mk.II recon glow is not 15 s");
        h.assertTrue((int) Round33GameTests.call(state, "cooldown", "recon_cloud") > 0, "no cooldown after the recon");
        h.succeed();
    }

    /** Self-repair works on a quantum piece (shared unit) without a nano core. */
    @GameTest(template = "empty", batch = "round186_core")
    public static void selfRepairHealsOnQuantum(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack helmet = Round33GameTests.charged("armor/item_armor_quantum_helmet");
        h.assertTrue(install(helmet, "self_repair", 1), "self-repair did not fit the quantum helmet");
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setHealth(10);
        Object state = Round33GameTests.call(nano(), "state", player);
        set(state, "lastHit", -100_000L);
        set(player, "tickCount", 60);
        Round33GameTests.call(nano(), "tick", player);
        h.assertTrue(player.getHealth() > 10, "no repair: health " + player.getHealth());
        h.succeed();
    }

    /** Nano to quantum rebuild: shared units move over, nano-only units and densifiers come back. */
    @GameTest(template = "empty", batch = "round186_core")
    public static void rebuildKeepsEverything(GameTestHelper h) {
        ItemStack nanoChest = Round33GameTests.charged("armor/item_armor_nano_chestplate");
        Round33GameTests.call(modules(), "densify", nanoChest);
        install(nanoChest, "threat_sensor", 1);
        install(nanoChest, "emp_emitter", 2);
        ItemStack quantum = Round33GameTests.charged("armor/item_armor_quantum_chestplate");
        List<?> back = (List<?>) Round33GameTests.call(Round33GameTests.common("suit.nano.NanoRebuild"), "transfer", nanoChest, quantum,
            (java.util.function.Consumer<ItemStack>) s -> { });
        h.assertTrue(tier(quantum, "threat_sensor") == 1, "the shared threat sensor did not move over");
        boolean emp = back.stream().anyMatch(o -> o instanceof ItemStack s
            && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals("module/item_module_emp_emitter_mk2"));
        boolean densifier = back.stream().anyMatch(o -> o instanceof ItemStack s
            && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals("module/item_nano_lattice_densifier") && s.getCount() == 1);
        h.assertTrue(emp, "the Mk.II EMP did not come back: " + back);
        h.assertTrue(densifier, "the densifier did not come back: " + back);
        h.succeed();
    }
}
