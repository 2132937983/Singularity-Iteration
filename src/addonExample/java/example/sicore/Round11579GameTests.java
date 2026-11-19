package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.79: the code-granted advancements. Items: opening a Scrap Box, a Wind Meter reading, a Painter that really
 * recolors a block (the same color does not count). Machines: a machine broken without a wrench that became its hull
 * (a BatBox, which has no hull, does not count), a newly built GESU, a firing Tesla Coil, a Kinetic Generator making EU
 * from kinetic energy (to players within 16 blocks). Events: the Steel Riot Shield blocking a projectile (not a melee
 * hit), starving in the Quantum Helmet, an overclocked Quantum Sword killing a mob in full netherite with a sword (not
 * without overclock). The data criteria of 0.1.11.78: Chainsaw Butcher, Like a Boss (both bosses), wearing the full
 * Quantum armor.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11579GameTests {
    private Round11579GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static AdvancementHolder adv(GameTestHelper h, String path) {
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(id(path));
        h.assertTrue(holder != null, "no advancement " + path);
        return holder;
    }

    private static boolean done(GameTestHelper h, ServerPlayer player, String path) {
        return player.getAdvancements().getOrStartProgress(adv(h, path)).isDone();
    }

    /** A survival mock player standing at {@code at} (inside the test structure, so blocks there are within 16 blocks). */
    private static ServerPlayer playerAt(GameTestHelper h, BlockPos at) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 v = h.absoluteVec(Vec3.atCenterOf(at));
        player.teleportTo(v.x, v.y, v.z);
        return player;
    }

    @GameTest(template = "empty", batch = "round11579_items", timeoutTicks = 20)
    public static void itemsGrantTheirAdvancements(GameTestHelper h) {
        ServerPlayer player = playerAt(h, new BlockPos(1, 2, 1));
        // Scrap Box
        player.setItemInHand(InteractionHand.MAIN_HAND, item("normal/item_scrapbox"));
        player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(done(h, player, "scrap_box"), "opening a Scrap Box did not grant Lucky Draw");
        // Wind Meter (a charged one; a reading costs energy)
        ItemStack meter = item("item_tool_windmeter");
        ((IBatteryItem) meter.getItem()).setEnergy(meter, 10_000L);
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        meter.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(done(h, player, "wind_meter"), "a Wind Meter reading did not grant Wind Tester");
        // Painter: red on red wool does nothing, red on white wool paints
        BlockPos wool = new BlockPos(2, 1, 2);
        h.setBlock(wool, Blocks.RED_WOOL);
        ItemStack painter = item("build/item_painter_red");
        player.setItemInHand(InteractionHand.MAIN_HAND, painter);
        BlockPos abs = h.absolutePos(wool);
        painter.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        h.assertTrue(!done(h, player, "painter"), "painting a block its own color granted I Am a Painter");
        h.setBlock(wool, Blocks.WHITE_WOOL);
        painter.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        h.assertBlockPresent(Blocks.RED_WOOL, wool);
        h.assertTrue(done(h, player, "painter"), "painting a block did not grant I Am a Painter");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11579_salvage", timeoutTicks = 20)
    public static void aMachineBrokenWithoutAWrench(GameTestHelper h) {
        ServerPlayer player = playerAt(h, new BlockPos(1, 2, 1));
        BlockPos batbox = new BlockPos(3, 1, 1), macerator = new BlockPos(3, 1, 3);
        h.setBlock(batbox, BuiltInRegistries.BLOCK.get(id("wiring/block_bat_box")));
        h.setBlock(macerator, BuiltInRegistries.BLOCK.get(id("producer/block_powder_elc")));
        h.getLevel().destroyBlock(h.absolutePos(batbox), true, player);
        h.assertTrue(!done(h, player, "broken_machine"), "a BatBox (it drops itself) granted A Cold Shell");
        h.getLevel().destroyBlock(h.absolutePos(macerator), true, player);
        h.assertTrue(done(h, player, "broken_machine"), "a Macerator broken without a wrench did not grant A Cold Shell");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11579_gesu", timeoutTicks = 40)
    public static void aNewGesuGrantsTheHoarder(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        ServerPlayer player = playerAt(h, new BlockPos(0, 2, 0));
        h.setBlock(core, BuiltInRegistries.BLOCK.get(id("wiring/block_gesu_core")));
        String[] modules = {"wiring/block_gesu_input_iv", "wiring/block_gesu_output_iv", "wiring/block_gesu_output_luv",
            "wiring/block_gesu_output_iv", "wiring/block_gesu_output_iv", "wiring/block_gesu_output_iv"};
        Direction[] faces = Direction.values();
        for (int i = 0; i < 6; i++) h.setBlock(core.relative(faces[i]), BuiltInRegistries.BLOCK.get(id(modules[i])));
        h.succeedWhen(() -> h.assertTrue(done(h, player, "gesu"), "a GESU built next to the player did not grant Mad Energy Hoarder"));
    }

    @GameTest(template = "empty", batch = "round11579_tesla", timeoutTicks = 80)
    public static void aPoweredTeslaCoilFires(GameTestHelper h) {
        BlockPos coil = new BlockPos(2, 1, 2);
        ServerPlayer player = playerAt(h, new BlockPos(0, 2, 0));
        h.setBlock(coil, BuiltInRegistries.BLOCK.get(id("producer/block_tesla")));
        call(call(h.getBlockEntity(coil), "getEnergyStorageInternal"), "setStored", 20_000L);
        h.setBlock(coil.east(), Blocks.REDSTONE_BLOCK);
        h.succeedWhen(() -> h.assertTrue(done(h, player, "tesla"), "a powered Tesla Coil did not grant Tesla Trooper"));
    }

    @GameTest(template = "empty", batch = "round11579_kinetic", timeoutTicks = 100)
    public static void aKineticGeneratorRunningOnKinetics(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        ServerPlayer player = playerAt(h, new BlockPos(0, 2, 0));
        h.setBlock(at, BuiltInRegistries.BLOCK.get(id("generator/block_kinetic_generator")));
        Object generator = h.getBlockEntity(at);
        h.onEachTick(() -> {
            try {
                var f = generator.getClass().getDeclaredField("kineticStorage");
                f.setAccessible(true);
                call(f.get(generator), "receiveKinetic", 1000L, false);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
        h.succeedWhen(() -> h.assertTrue(done(h, player, "kinetic_running"), "a Kinetic Generator making EU from KU did not grant Full of Kinetics"));
    }

    @GameTest(template = "empty", batch = "round11579_events", timeoutTicks = 20)
    public static void eventAdvancements(GameTestHelper h) {
        ServerPlayer player = playerAt(h, new BlockPos(1, 2, 1));
        // the Steel Riot Shield: a melee hit does not count, an arrow does
        player.setItemInHand(InteractionHand.OFF_HAND, item("item_tool_steel_shield"));
        player.startUsingItem(InteractionHand.OFF_HAND);
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        NeoForge.EVENT_BUS.post(new LivingShieldBlockEvent(player, new DamageContainer(player.damageSources().mobAttack(zombie), 4), true));
        h.assertTrue(!done(h, player, "shield"), "a blocked melee hit granted Sorry, Not Tomorrow");
        var arrow = EntityType.ARROW.create(h.getLevel());
        NeoForge.EVENT_BUS.post(new LivingShieldBlockEvent(player, new DamageContainer(player.damageSources().arrow(arrow, zombie), 4), true));
        h.assertTrue(done(h, player, "shield"), "a blocked arrow did not grant Sorry, Not Tomorrow");
        // starving in the Quantum Helmet
        player.setItemSlot(EquipmentSlot.HEAD, item("armor/item_armor_quantum_helmet"));
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().starve()));
        h.assertTrue(done(h, player, "forgot_charge"), "starving in the Quantum Helmet did not grant Forgot to Charge");
        // an overclocked Quantum Sword kills a mob in full netherite holding a sword
        Zombie knight = EntityType.ZOMBIE.create(h.getLevel());
        for (var e : new Object[][]{{EquipmentSlot.HEAD, Items.NETHERITE_HELMET}, {EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE},
                {EquipmentSlot.LEGS, Items.NETHERITE_LEGGINGS}, {EquipmentSlot.FEET, Items.NETHERITE_BOOTS}, {EquipmentSlot.MAINHAND, Items.IRON_SWORD}})
            knight.setItemSlot((EquipmentSlot) e[0], new ItemStack((net.minecraft.world.item.Item) e[1]));
        ItemStack sword = item("item_tool_quantum_sword");
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(knight, player.damageSources().playerAttack(player)));
        h.assertTrue(!done(h, player, "know_nothing"), "a Quantum Sword without overclock granted You Know Nothing of Power");
        CompoundTag hyper = new CompoundTag();
        hyper.putBoolean("QuantumSwordHyper", true);
        sword.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(hyper));
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(knight, player.damageSources().playerAttack(player)));
        h.assertTrue(done(h, player, "know_nothing"), "an overclocked Quantum Sword kill did not grant You Know Nothing of Power");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11579_data", timeoutTicks = 40)
    public static void dataCriteriaOfTheTree(GameTestHelper h) {
        ServerPlayer player = playerAt(h, new BlockPos(1, 2, 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, item("item_tool_iron_chainsaw"));
        var creeper = EntityType.CREEPER.create(h.getLevel());
        CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(player, creeper, player.damageSources().playerAttack(player));
        h.assertTrue(done(h, player, "chainsaw_creeper"), "a Creeper killed with the chainsaw did not grant Chainsaw Butcher");
        player.setItemInHand(InteractionHand.MAIN_HAND, item("item_tool_laser_miner"));
        CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(player, EntityType.ENDER_DRAGON.create(h.getLevel()), player.damageSources().playerAttack(player));
        h.assertTrue(!done(h, player, "like_a_boss"), "Like a Boss granted after the dragon alone");
        CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(player, EntityType.WITHER.create(h.getLevel()), player.damageSources().playerAttack(player));
        h.assertTrue(done(h, player, "like_a_boss"), "the dragon and the Wither killed with the laser did not grant Like a Boss");
        for (var e : new Object[][]{{EquipmentSlot.HEAD, "helmet"}, {EquipmentSlot.CHEST, "chestplate"}, {EquipmentSlot.LEGS, "leggings"}, {EquipmentSlot.FEET, "boots"}})
            player.setItemSlot((EquipmentSlot) e[0], item("armor/item_armor_quantum_" + e[1]));
        h.succeedWhen(() -> {
            CriteriaTriggers.TICK.trigger(player);
            h.assertTrue(done(h, player, "quantum_set"), "wearing the full Quantum armor did not grant Schrödinger Approves");
        });
    }
}
