package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.7.32: menu slot parity (the GESU module crash), and the other fixes of this round.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round32GameTests {
    private Round32GameTests() {}

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }

    private static String compare(MenuProvider provider, Player player, String what, BlockPos pos) {
        AbstractContainerMenu server = provider.createMenu(1, player.getInventory(), player);
        if (server == null) return null;
        AbstractContainerMenu client;
        try {
            // the client builds its copy from the registered factory (no block entity)
            // menus opened with extra data get the block position, as ServerPlayer#openMenu writes it
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.registryAccess());
            buf.writeBlockPos(pos);
            client = server.getType().create(1, player.getInventory(), buf);
        } catch (RuntimeException e) {
            return what + ": client factory threw " + e;
        }
        if (client.slots.size() != server.slots.size())
            return what + " (" + server.getClass().getSimpleName() + "): server " + server.slots.size() + " slots, client " + client.slots.size();
        return null;
    }

    /** Every SI block entity that opens a menu builds the same slot list on both sides. */
    @GameTest(batch = "round32_menus", template = "empty", timeoutTicks = 40)
    public static void everyMenuHasTheSameSlotsOnServerAndClient(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!e.getKey().location().getNamespace().equals("mio_icif") || !(e.getValue() instanceof EntityBlock eb)) continue;
            BlockEntity be;
            try {
                be = eb.newBlockEntity(h.absolutePos(new BlockPos(1, 1, 1)), e.getValue().defaultBlockState());
            } catch (RuntimeException ex) { continue; }
            if (!(be instanceof MenuProvider provider)) continue;   // never attached to the level
            try {
                String p = compare(provider, player, e.getKey().location().getPath(), be.getBlockPos());
                if (p != null) problems.add(p);
                checked++;
            } catch (RuntimeException ex) {
                // menus that need a placed, formed or bound block are covered by the formed tests
            }
        }
        h.assertTrue(checked > 40, "only " + checked + " menus checked");
        h.assertTrue(problems.isEmpty(), "slot mismatch: " + String.join("; ", problems));
        h.succeed();
    }

    /** Right-clicking a GESU input / output module opens the core's menu with matching slots. */
    @GameTest(batch = "round32_gesu_menu", template = "reactor_loop", timeoutTicks = 60)
    public static void gesuModulesOpenTheCoreMenuWithMatchingSlots(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 3);
        for (Direction d : Direction.values())
            h.setBlock(core.relative(d), block(d == Direction.UP ? "wiring/block_gesu_output_iv"
                : d == Direction.DOWN ? "wiring/block_gesu_output_luv" : "wiring/block_gesu_input_iv"));
        h.setBlock(core, block("wiring/block_gesu_core"));
        h.runAfterDelay(10, () -> {
            var player = h.makeMockServerPlayerInLevel();
            for (Direction d : new Direction[]{Direction.NORTH, Direction.UP, Direction.DOWN}) {
                BlockPos at = h.absolutePos(core.relative(d));
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), d, at, false);
                h.getLevel().getBlockState(at).useWithoutItem(h.getLevel(), player, hit);
                AbstractContainerMenu open = player.containerMenu;
                h.assertTrue(open != player.inventoryMenu, "module " + d + " opened no menu");
                AbstractContainerMenu client = open.getType().create(2, player.getInventory());
                h.assertTrue(client.slots.size() == open.slots.size(),
                    "module " + d + ": server " + open.slots.size() + " slots, client " + client.slots.size());
                player.closeContainer();
            }
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ helpers
    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";
    private static Class<?> common(String name) {
        try { return Class.forName(COMMON + name); } catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }
    private static net.minecraft.world.item.ItemStack item(String path) { return new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static net.minecraft.world.level.block.state.BlockState facing(String path, Direction side) {
        var s = block(path).defaultBlockState();
        var p = s.getBlock().getStateDefinition().getProperty("facing");
        return p instanceof net.minecraft.world.level.block.state.properties.DirectionProperty d && d.getPossibleValues().contains(side) ? s.setValue(d, side) : s;
    }
    @SuppressWarnings("unchecked")
    private static net.minecraft.world.level.block.state.BlockState with(net.minecraft.world.level.block.state.BlockState s, String prop, boolean v) {
        var p = (net.minecraft.world.level.block.state.properties.Property<Boolean>) s.getBlock().getStateDefinition().getProperty(prop);
        return s.setValue(p, v);
    }

    /** Cable laid through an iron scaffold, foamed and set: reinforced stone with a working cable inside. */
    @GameTest(batch = "round32_foam", template = "reactor_loop", timeoutTicks = 120)
    public static void cableEmbeddedInReinforcedStoneStillConducts(GameTestHelper h) {
        BlockPos box = new BlockPos(2, 2, 2), cable = box.east(), sink = cable.east();
        h.setBlock(cable, block("build/block_scaffold_iron"));
        // place the cable into the scaffold the way a player does
        var player = h.makeMockServerPlayerInLevel();
        var stack = item("wiring/cable/block_tin_cable_1").copyWithCount(4);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        BlockPos at = h.absolutePos(cable);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.UP, at, false);
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        var placed = h.getBlockState(cable);
        h.assertTrue(placed.getBlock().getClass().getSimpleName().equals("mio_icif_block_wire"), "cable not placed into the iron scaffold: " + placed);
        h.assertTrue(placed.getValue(placed.getBlock().getStateDefinition().getProperty("foam_reinforced")).equals(true), "scaffold not kept around the cable");
        // spray it like a scaffold structure, then let it set
        Object sprayer = item("build/item_cf_sprayer").getItem();
        Class<?> target = common("item.build.CFSprayerItem$Target");
        Object scaffoldTarget = Enum.valueOf((Class) target, "SCAFFOLD");
        int sprayed = (int) call(sprayer, "sprayFoam", h.getLevel(), at, null, scaffoldTarget, 10);
        h.assertTrue(sprayed == 1, "spraying the scaffolded cable foamed " + sprayed + " blocks");
        call(common("block.wire.mio_icif_block_wire"), "hardenFoam", h.getLevel(), at, h.getBlockState(cable));
        var set = h.getBlockState(cable);
        h.assertTrue((boolean) call(common("block.wire.mio_icif_block_wire"), "isEmbedded", set), "foam did not set around the cable: " + set);
        h.assertTrue(set.getBlock().getExplosionResistance() >= 0 && set.getExplosionResistance(h.getLevel(), at, null) >= 1000,
            "embedded cable is not blast proof: " + set.getExplosionResistance(h.getLevel(), at, null));
        // power still flows through it
        h.setBlock(box, facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(sink, facing("wiring/block_eesu", Direction.EAST));
        long[] start = {-1};
        h.runAfterDelay(5, () -> {
            com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity src = h.getBlockEntity(box);
            src.getEnergyStorageInternal().setStored(src.getEnergyStorageInternal().getCapacity());
            start[0] = ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity) h.getBlockEntity(sink)).getEnergyStorageInternal().getAmount();
        });
        h.runAfterDelay(60, () -> {
            long got = ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity) h.getBlockEntity(sink)).getEnergyStorageInternal().getAmount() - start[0];
            h.assertTrue(got > 0, "no power through the cable embedded in reinforced stone");
            h.succeed();
        });
    }

    /** A wireless node with no target is a receiver and feeds what it gets into adjacent machines. */
    @GameTest(batch = "round32_wireless", template = "reactor_loop", timeoutTicks = 100)
    public static void wirelessReceiverNodeSuppliesNeighbours(GameTestHelper h) {
        BlockPos tx = new BlockPos(1, 2, 1), rx = new BlockPos(4, 2, 4), sink = rx.east();
        h.setBlock(tx, block("wiring/block_wireless_power_transmission_node"));
        h.setBlock(rx, block("wiring/block_wireless_power_transmission_node"));
        h.setBlock(sink, facing("wiring/block_mfe", Direction.EAST));
        h.runAfterDelay(2, () -> {
            var transmitter = h.getBlockEntity(tx);
            call(transmitter, "setTargetPosition", h.absolutePos(rx));
        });
        h.onEachTick(() -> {
            if (h.getTick() < 3 || h.getTick() > 60) return;
            com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity t = h.getBlockEntity(tx);
            t.getEnergyStorageInternal().setStored(t.getEnergyStorageInternal().getCapacity());   // keep the transmitter fed
        });
        h.runAfterDelay(80, () -> {
            com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity mfe = h.getBlockEntity(sink);
            h.assertTrue((boolean) call(h.getBlockEntity(rx), "isReceiver"), "unlinked node is not a receiver");
            h.assertTrue(mfe.getEnergyStorageInternal().getAmount() > 0, "the machine next to the receiver node got no power");
            h.succeed();
        });
    }

    /** No input ceiling: any tier, any amount; several items per tick when the buffer allows. */
    @GameTest(batch = "round32_mt", template = "empty", timeoutTicks = 40)
    public static void molecularTransformerIsUncappedAndParallel(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, block("producer/block_molecular_transformer"));
        var mt = (com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        h.assertTrue(mt.getSinkTier() == Integer.MAX_VALUE, "molecular transformer can still be overvolted");
        h.assertTrue(mt.getEnergyStorageInternal().getMaxReceive() == Long.MAX_VALUE, "receive is capped at " + mt.getEnergyStorageInternal().getMaxReceive());
        ((net.neoforged.neoforge.items.IItemHandlerModifiable) mt.getItemHandler()).setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_WOOL, 10));
        mt.getEnergyStorageInternal().setStored(10_000_000L);
        h.runAfterDelay(4, () -> {
            var out = mt.getItemHandler().getStackInSlot(1);
            h.assertTrue(out.is(net.minecraft.world.item.Items.LAPIS_BLOCK) && out.getCount() == 10,
                "10 x 1M EU recipes with 10M EU buffered should finish together, got " + out);
            h.succeed();
        });
    }

    @GameTest(batch = "round32_cable", template = "empty", timeoutTicks = 20)
    public static void superconductingCableHasNoCeiling(GameTestHelper h) {
        var cable = (com.miophas.singularity_iteration.core.api.energy.ICableBlock) block("wiring/cable/block_superconducting_cable");
        var tier = (com.miophas.singularity_iteration.core.api.energy.storage.CableTier) cable.getCableTier();
        h.assertTrue(tier.getConductorBreakdownEnergy() == Long.MAX_VALUE && tier.getPowerRating() == Long.MAX_VALUE,
            "superconducting cable still limited: " + tier.getPowerRating() + " / " + tier.getConductorBreakdownEnergy());
        h.succeed();
    }

    /** Solar helmets feed the advanced quantum chestplate; hybrid / ultimate keep the quantum helmet traits. */
    @GameTest(batch = "round32_helmet", template = "empty", timeoutTicks = 40)
    public static void solarHelmetChargesChestAndKeepsQuantumTraits(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var helmet = item("armor/item_armor_hybrid_solar_helmet");
        var chest = item("armor/item_armor_advanced_quantum_chestplate");
        var api = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI();
        ((com.miophas.singularity_iteration.core.api.item.IBatteryItem) helmet.getItem()).setEnergy(helmet, 1_000_000);
        ((com.miophas.singularity_iteration.core.api.item.IBatteryItem) chest.getItem()).setEnergy(chest, 0);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        player.setAirSupply(10);
        var features = com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.features(helmet);
        h.assertTrue(features.stream().anyMatch(f -> f.featureKey().equals("water_breathing"))
            && features.stream().anyMatch(f -> f.featureKey().equals("auto_food")), "hybrid solar helmet lost the quantum helmet traits");
        h.onEachTick(() -> player.getInventory().tick());
        h.runAfterDelay(20, () -> {
            var worn = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            long charged = ((com.miophas.singularity_iteration.core.api.item.IBatteryItem) worn.getItem()).getEnergy(worn);
            h.assertTrue(charged > 0, "the solar helmet did not charge the advanced quantum chestplate");
            h.assertTrue(player.getAirSupply() > 10, "the helmet's air supply trait did not work");
            h.succeed();
        });
    }

    /** Ring / creative flight never runs the chest jetpack (it drained the quantum chestplate). */
    @GameTest(batch = "round32_flight", template = "empty", timeoutTicks = 30)
    public static void creativeFlightDoesNotDrainTheChestJetpack(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var chest = item("armor/item_armor_advanced_quantum_chestplate");
        var battery = (com.miophas.singularity_iteration.core.api.item.IBatteryItem) chest.getItem();
        battery.setEnergy(chest, 1_000_000);
        var jet = (com.miophas.singularity_iteration.core.api.item.IJetpackItem) chest.getItem();
        jet.setMode(chest, com.miophas.singularity_iteration.core.api.item.IJetpackItem.JetpackMode.HOVER);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.setPos(player.getX(), player.getY() + 5, player.getZ());
        player.setOnGround(false);
        long before = battery.getEnergy(chest);
        for (int i = 0; i < 10; i++)
            com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController.tick(player, chest, jet, 10, 0.3, 0.2, 0.1, true);
        h.assertTrue(battery.getEnergy(chest) == before, "ring / creative flight drained the chest jetpack: " + (before - battery.getEnergy(chest)) + " EU");
        h.succeed();
    }

    /** Tool traits are declared, toggleable and honoured. */
    @GameTest(batch = "round32_tools", template = "empty", timeoutTicks = 20)
    public static void electricToolsHaveTraits(GameTestHelper h) {
        var saber = item("item_tool_nanosaber");
        if (saber.isEmpty() || saber.is(net.minecraft.world.item.Items.AIR)) saber = item("tools/item_tool_nanosaber");
        var wrench = item("item_tool_wrench_elc");
        for (var stack : new net.minecraft.world.item.ItemStack[]{saber, wrench}) {
            var features = com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.features(stack);
            h.assertTrue(features.size() >= 2, stack + " has no console traits");
            String key = features.get(0).featureKey();
            h.assertTrue(com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(stack, key), key + " should start enabled");
            com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.toggle(stack, key);
            h.assertTrue(!com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(stack, key), key + " did not toggle off");
        }
        h.succeed();
    }

    /** Futures commodities come from the datapack, with period and unlock fields. */
    @GameTest(batch = "round32_futures", template = "empty", timeoutTicks = 20)
    public static void futuresCommoditiesComeFromDatapacks(GameTestHelper h) {
        Class<?> manager = common("future.FutureCommodityManager");
        var list = (List<?>) call(manager, "getCommodities");
        h.assertTrue(list.size() >= 50, "datapack commodities not loaded: " + list.size());
        var map = new java.util.LinkedHashMap<String, Object>();
        var json = com.google.gson.JsonParser.parseString(
            "{\"item\":\"minecraft:diamond\",\"base_price\":900,\"volatility\":0.2,\"period_days\":3,\"unlock\":{\"advancement\":\"minecraft:story/mine_diamond\"}}").getAsJsonObject();
        call(common("future.FutureCommodityLoader"), "parseEntry", json, "mineral", map);
        Object diamond = map.get("minecraft:diamond");
        h.assertTrue(diamond != null && (int) call(diamond, "getPeriodDays") == 3
            && call(diamond, "getUnlockAdvancement").toString().equals("minecraft:story/mine_diamond"), "entry fields not parsed");
        h.assertTrue((boolean) call(diamond, "repricesOn", 6L) && !(boolean) call(diamond, "repricesOn", 7L), "period_days not honoured");
        call(common("future.FutureCommodityLoader"), "parseEntry",
            com.google.gson.JsonParser.parseString("{\"item\":\"minecraft:diamond\",\"remove\":true}").getAsJsonObject(), "mineral", map);
        h.assertTrue(!map.containsKey("minecraft:diamond"), "remove did not drop the entry");
        h.succeed();
    }

    @GameTest(batch = "round32_net", template = "empty", timeoutTicks = 20)
    public static void networkChannelVersionIsTheModVersion(GameTestHelper h) {
        String version = (String) call(common("network.mio_icif_Network"), "protocolVersion");
        String mod = net.neoforged.fml.ModList.get().getModContainerById("mio_icif").orElseThrow().getModInfo().getVersion().toString();
        h.assertTrue(version.equals(mod), "channel version " + version + " != mod version " + mod);
        h.succeed();
    }
}
