package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.Arrays;
import java.util.List;

/**
 * 0.1.10.14 player feedback: the Large Fabricator takes any EU or FE its input buffers have room for
 * and pools them; the Replicator stops when its crystal leaves and Stop discards the copy; the Scanner
 * never locks the scanned item in and forgets an unfinished scan when broken; the Transformer Upgrade
 * names the Safety Key; only the Electric Jetpack keeps a ceiling; the Industrial Diamond is a diamond
 * in recipes; the storage box item counts its stacks; the earmuffs are worn on the head.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class PlayerFeedbackGameTests {
    private PlayerFeedbackGameTests() {}

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(si(path)); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(si(path)); }

    private static void setStored(BlockEntity be, long eu) { call(call(be, "getEnergyStorageInternal"), "setStored", eu); }
    private static long stored(BlockEntity be) { return (Long) call(be, "getStoredEnergy"); }

    // ---- Large Fabricator ----

    private static int uuMatter(BlockEntity core) { return ((FluidTank) call(core, "getUuMatterTank")).getFluidAmount(); }

    /** 500,000 + 300,000 EU in two input modules pay one millibucket (800,000); one EU less pays nothing. */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void largeFabricatorPoolsItsInputModules(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        h.setBlock(core, block("producer/block_large_fabricator_core"));
        h.setBlock(core.east(), block("producer/block_large_fabricator_input_iv"));
        h.setBlock(core.north(), block("producer/block_large_fabricator_input_iv"));
        BlockEntity coreBe = h.getBlockEntity(core), a = h.getBlockEntity(core.east()), b = h.getBlockEntity(core.north());
        setStored(a, 500_000L);
        setStored(b, 299_999L);
        call(coreBe, "processFabrication", h.getLevel(), coreBe.getBlockPos());
        h.assertTrue(uuMatter(coreBe) == 0 && stored(a) + stored(b) == 799_999L, "made UU-matter from 799,999 EU");
        setStored(b, 300_000L);
        call(coreBe, "processFabrication", h.getLevel(), coreBe.getBlockPos());
        h.assertTrue(uuMatter(coreBe) == 1, "two modules with 800,000 EU together made " + uuMatter(coreBe) + " mB");
        h.assertTrue(stored(a) == 0 && stored(b) == 0, "EU left " + stored(a) + " / " + stored(b));
        h.succeed();
    }

    /** IC2 demand = room in the buffer: far more than one IV packet per tick, and Forge Energy too. */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void largeFabricatorInputTakesForgeEnergyWithoutARate(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("producer/block_large_fabricator_input_iv"));
        BlockEntity input = h.getBlockEntity(pos);
        long eu = (Long) call(input, "receiveEnergy", 100_000L, false);
        h.assertTrue(eu == 100_000L, "took " + eu + " of 100,000 EU in one tick");
        IEnergyStorage fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos), Direction.UP);
        h.assertTrue(fe != null && fe.canReceive(), "no Forge Energy input: " + fe);
        int took = fe.receiveEnergy(2_000_000, false);
        h.assertTrue(took == 2_000_000 && stored(input) == 600_000L, "took " + took + " FE, stored " + stored(input) + " EU");
        h.succeed();
    }

    // ---- Replicator ----

    private static final String STONE_DUST = "resource/item_stone_dust";

    /** A crystal memory holding the pattern of stone dust, at its price-book quote. */
    private static ItemStack crystal(GameTestHelper h) {
        ItemStack dust = new ItemStack(item(STONE_DUST));
        var quote = UuQuoteBook.quote(h.getLevel().getServer(), dust);
        h.assertTrue(quote != null, "stone dust has no UU-matter price");
        ItemStack crystal = new ItemStack(item("resource/item_crystal_memory"));
        h.assertTrue((Boolean) call(crystal.getItem(), "tryStoreData", crystal, dust, quote.buckets(), 844_800L), "the crystal refused the pattern");
        return crystal;
    }

    /** A replicator with energy, UU-matter and the stone dust crystal, set to copy once. */
    private static BlockEntity replicator(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, block("producer/block_replicator_elc"));
        BlockEntity replicator = h.getBlockEntity(pos);
        setStored(replicator, 2_000_000L);
        ((FluidTank) call(replicator, "getUuMatterTank")).fill(new FluidStack(BuiltInRegistries.FLUID.get(si("uumatter")), 8_000),
            IFluidHandler.FluidAction.EXECUTE);
        ((IItemHandlerModifiable) call(replicator, "getItemHandler")).setStackInSlot(2, crystal(h));
        call(replicator, "generateOnce");
        return replicator;
    }

    private static double processed(BlockEntity replicator) { return (Double) call(replicator, "getProcessedUuBuckets"); }
    private static String mode(BlockEntity replicator) { return String.valueOf(call(replicator, "getWorkMode")); }

    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 100)
    public static void replicatorStopsWhenItsCrystalIsTakenOut(GameTestHelper h) {
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1));
        var slots = (IItemHandler) call(replicator, "getItemHandler");
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) > 0, "no copy started"))
            .thenExecute(() -> {
                h.assertTrue((Boolean) call(replicator, "isReplicating"), "not replicating mid-copy");
                h.assertTrue(!slots.extractItem(2, 1, false).isEmpty(), "the crystal did not come out");
            })
            .thenIdle(2)
            .thenExecute(() -> h.assertTrue(processed(replicator) == 0 && mode(replicator).equals("STOPPED")
                && !(Boolean) call(replicator, "isReplicating"),
                "still copying without the crystal: " + processed(replicator) + " " + mode(replicator)))
            .thenSucceed();
    }

    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 100)
    public static void replicatorStopDiscardsTheCopy(GameTestHelper h) {
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1));
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) > 0, "no copy started"))
            .thenExecute(() -> call(replicator, "stopGeneration"))
            .thenExecute(() -> h.assertTrue(processed(replicator) == 0 && mode(replicator).equals("STOPPED"),
                "Stop kept the copy: " + processed(replicator) + " " + mode(replicator)))
            .thenIdle(3)
            .thenExecute(() -> {
                h.assertTrue(processed(replicator) == 0, "copying after Stop: " + processed(replicator));
                call(replicator, "generateOnce");
            })
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) > 0, "Single did not start a new copy"))
            .thenSucceed();
    }

    // ---- Scanner ----

    private static final int SCANNER_SLOT = 0, MEMORY_SLOT = 2;

    /** A scanner with energy, stone dust and a blank crystal: it starts scanning. */
    private static BlockEntity scanner(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, block("producer/block_scanner_elc"));
        BlockEntity scanner = h.getBlockEntity(pos);
        setStored(scanner, 512_000L);
        var slots = (IItemHandlerModifiable) call(scanner, "getItemHandler");
        slots.setStackInSlot(SCANNER_SLOT, new ItemStack(item(STONE_DUST)));
        slots.setStackInSlot(MEMORY_SLOT, new ItemStack(item("resource/item_crystal_memory")));
        return scanner;
    }

    private static int progress(BlockEntity scanner) { return (Integer) call(scanner, "getProgress"); }
    private static String state(BlockEntity scanner) { return String.valueOf(call(scanner, "getScanState")); }

    /** No storage left: the scan ends (IC2 reset) and the item can be taken out. */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 100)
    public static void scannerEndsTheScanWhenItsCrystalIsTakenOut(GameTestHelper h) {
        BlockEntity scanner = scanner(h, new BlockPos(1, 1, 1));
        var slots = (IItemHandler) call(scanner, "getItemHandler");
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(progress(scanner) > 0, "no scan started: " + state(scanner)))
            .thenExecute(() -> h.assertTrue(!slots.extractItem(MEMORY_SLOT, 1, false).isEmpty(), "the crystal did not come out"))
            .thenIdle(2)
            .thenExecute(() -> {
                h.assertTrue(progress(scanner) == 0 && state(scanner).equals("NO_STORAGE"),
                    "scan kept without storage: " + progress(scanner) + " " + state(scanner));
                ItemStack out = slots.extractItem(SCANNER_SLOT, 1, false);
                h.assertTrue(out.is(item(STONE_DUST)), "the scanned item is stuck: " + out);
            })
            .thenSucceed();
    }

    /** Mid-scan a player takes the item (the scan ends), a hopper may not. */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 100)
    public static void scannerItemLeavesByHandNotByHopper(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockEntity scanner = scanner(h, pos);
        var slots = (IItemHandler) call(scanner, "getItemHandler");
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(progress(scanner) > 0, "no scan started: " + state(scanner)))
            .thenExecute(() -> {
                var sided = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.DOWN);
                h.assertTrue(sided != null && sided.extractItem(SCANNER_SLOT, 1, true).isEmpty(), "automation pulls the item mid-scan");
                h.assertTrue(slots.extractItem(SCANNER_SLOT, 1, false).is(item(STONE_DUST)), "the GUI slot is locked mid-scan");
            })
            .thenIdle(2)
            .thenExecute(() -> h.assertTrue(progress(scanner) == 0 && state(scanner).equals("IDLE"),
                "scan kept after the item left: " + progress(scanner) + " " + state(scanner)))
            .thenSucceed();
    }

    /** Breaking a scanner mid-scan spills its slots and keeps no scan in the dropped scanner (IC2). */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 100)
    public static void brokenScannerSpillsAndForgetsTheScan(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockEntity scanner = scanner(h, pos);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(progress(scanner) > 0, "no scan started: " + state(scanner)))
            .thenExecute(() -> {
                h.getLevel().destroyBlock(h.absolutePos(pos), true);
                List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(2));
                boolean dust = drops.stream().anyMatch(e -> e.getItem().is(item(STONE_DUST)));
                boolean crystal = drops.stream().anyMatch(e -> e.getItem().is(item("resource/item_crystal_memory")));
                boolean packed = drops.stream().anyMatch(e -> e.getItem().has(DataComponents.BLOCK_ENTITY_DATA));
                drops.forEach(Entity::discard);
                h.assertTrue(dust && crystal && !packed, "drops: dust " + dust + ", crystal " + crystal + ", packed scanner " + packed);
            })
            .thenSucceed();
    }

    // ---- Transformer Upgrade, jetpacks, Industrial Diamond, storage box, earmuffs ----

    private static boolean hasLine(List<Component> lines, String key) {
        return lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents t && t.getKey().equals(key));
    }

    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void transformerUpgradeNamesTheSafetyKey(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        Class<?> keys = Round33GameTests.common("network.mio_icif_KeyboardManager");
        h.assertFalse((Boolean) call(keys, "mayTakeTransformerUpgrade", player), "taken without the Safety Key");
        call(keys, "processKeyUpdate", player, 1 << 4);
        h.assertTrue((Boolean) call(keys, "mayTakeTransformerUpgrade", player), "refused with the Safety Key held");
        call(keys, "processKeyUpdate", player, 0);
        player.discard();
        var lines = new ItemStack(item("upgrade/transformer_upgrade")).getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
        h.assertTrue(hasLine(lines, "item.mio_icif.upgrade.transformer.safety"), "no Safety Key line: " + lines);
        h.succeed();
    }

    private static boolean ceiling(String path) { return ((IJetpackItem) item(path)).hasHeightLimit(); }

    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void onlyTheElectricJetpackHasACeiling(GameTestHelper h) {
        h.assertTrue(ceiling("armor/item_armor_jetpack_electric"), "the Electric Jetpack lost its ceiling");
        h.assertFalse(ceiling("armor/item_armor_advanced_jetpack"), "the Advanced Electric Jetpack has a ceiling");
        h.assertFalse(ceiling("armor/item_armor_quantum_chestplate"), "the Quantum Chestplate has a ceiling");
        h.succeed();
    }

    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void industrialDiamondIsADiamondInRecipes(GameTestHelper h) {
        ItemStack diamond = new ItemStack(item("normal/item_industrial_diamond"));
        h.assertTrue(diamond.is(Tags.Items.GEMS_DIAMOND), "not in c:gems/diamond");
        var recipes = h.getLevel().getRecipeManager();
        for (String id : new String[] {"shaped_40_teleporter", "shaped_137_iridium_plate", "shaped_263_diamond_cutblade",
                "shaped_289_diamond_heat_vent", "shaped_311_diamond_driller", "powder/diamond_to_diamond_dust",
                "mets/shaped_advanced_macerator", "mets/diamond_lens", "asp_irradiant_reinforced_plate"}) {
            var holder = recipes.byKey(si(id)).orElse(null);
            h.assertTrue(holder != null, id + " is missing");
            h.assertTrue(holder.value().getIngredients().stream().anyMatch(i -> i.test(diamond)), id + " refuses the industrial diamond");
            h.assertTrue(holder.value().getIngredients().stream().anyMatch(i -> i.test(new ItemStack(Items.DIAMOND))), id + " refuses a diamond");
        }
        h.succeed();
    }

    /** Two filled slots of a wooden box (27): the dropped box says 2 / 27 stacks (it said nothing before). */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void storageBoxItemCountsItsStacks(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("build/block_wood_storage"));
        BlockEntity box = h.getBlockEntity(pos);
        var handler = (IItemHandler) call(box, "getItemHandler");
        handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        handler.insertItem(5, new ItemStack(Items.DIRT, 3), false);
        ItemStack stack = new ItemStack(block("build/block_wood_storage"));
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(box.saveWithId(h.getLevel().registryAccess())));
        var lines = stack.getTooltipLines(Item.TooltipContext.of(h.getLevel()), null, TooltipFlag.NORMAL);
        var line = lines.stream().map(Component::getContents)
            .filter(c -> c instanceof TranslatableContents t && t.getKey().equals("tooltip.mio_icif.storage_box.items")).findFirst();
        h.assertTrue(line.isPresent(), "no item count line: " + lines);
        Object[] args = ((TranslatableContents) line.get()).getArgs();
        h.assertTrue(Integer.valueOf(2).equals(args[0]) && Integer.valueOf(27).equals(args[1]), "line " + Arrays.toString(args));
        h.succeed();
    }

    /**
     * The bare MV cable from the Metal Former (extruding), from crafting and from a placed, unpainted cable
     * that is broken again are the same stack: one item, no extra data components.
     */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 400)
    public static void extrudedCablesStackWithCraftedAndBrokenOnes(GameTestHelper h) {
        BlockPos former = new BlockPos(1, 1, 1), cable = new BlockPos(3, 1, 1);
        h.setBlock(former, block("producer/block_metal_former"));
        BlockEntity machine = h.getBlockEntity(former);
        setStored(machine, 100_000L);
        try {
            Class<?> modes = Class.forName("com.miophas.singularity_iteration" + ".common.blockentity.producer.mio_icif_metal_former$MetalFormerMode");
            call(machine, "setMode", modes.getField("EXTRUDING").get(null));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        var slots = (IItemHandlerModifiable) call(machine, "getItemHandler");
        slots.setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
        h.setBlock(cable, block("wiring/cable/block_cable_o"));
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(!slots.getStackInSlot(2).isEmpty(), "the Metal Former made nothing yet"))
            .thenExecute(() -> {
                ItemStack extruded = slots.getStackInSlot(2);
                ItemStack crafted = h.getLevel().getRecipeManager().byKey(si("shapeless_178_uninsulated_copper_cable")).orElseThrow()
                    .value().getResultItem(h.getLevel().registryAccess());
                var state = h.getBlockState(cable);
                List<ItemStack> drops = Block.getDrops(state, h.getLevel(), h.absolutePos(cable), h.getBlockEntity(cable));
                ItemStack broken = drops.stream().filter(s -> s.is(item("wiring/cable/block_cable_o"))).findFirst().orElse(ItemStack.EMPTY);
                h.assertTrue(extruded.is(item("wiring/cable/block_cable_o")), "extruded " + extruded);
                h.assertTrue(ItemStack.isSameItemSameComponents(extruded, crafted), "extruded " + extruded.getComponentsPatch() + " / crafted " + crafted.getComponentsPatch());
                h.assertTrue(ItemStack.isSameItemSameComponents(extruded, broken), "extruded " + extruded.getComponentsPatch() + " / broken " + broken + " " + broken.getComponentsPatch());
            })
            .thenSucceed();
    }

    /** The cable items lying around {@code pos} (taken out of the world). */
    private static List<ItemStack> dropsAt(GameTestHelper h, BlockPos pos) {
        var entities = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(1));
        List<ItemStack> drops = entities.stream().map(ItemEntity::getItem).toList();   // as dropped: a copy would run the load clean-up
        entities.forEach(Entity::discard);
        return drops;
    }

    /**
     * A placed bare cable broken by a survival player, by hand and with the SI wrench (and the drop the
     * wrench pick-up builds), drops a plain item that stacks with a crafted one. Before 0.1.10.15 a
     * wrench added {id, energy} block-entity data; such old items are cleaned when they load.
     */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 40)
    public static void brokenBareCablesDropPlainItems(GameTestHelper h) {
        Item bare = item("wiring/cable/block_cable_o");
        ItemStack crafted = h.getLevel().getRecipeManager().byKey(si("shapeless_178_uninsulated_copper_cable")).orElseThrow()
            .value().getResultItem(h.getLevel().registryAccess());
        var player = h.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos byHand = new BlockPos(1, 1, 1), byWrench = new BlockPos(5, 1, 1), dismantled = new BlockPos(9, 1, 1);
        for (BlockPos p : new BlockPos[] {byHand, byWrench, dismantled}) h.setBlock(p, block("wiring/cable/block_cable_o"));
        h.startSequence()
            .thenIdle(2)
            .thenExecute(() -> {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(byHand)), "the player could not break the cable");
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(item("item_tool_wrench")));
                h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(byWrench)), "the player could not break the cable with a wrench");
                // the wrench pick-up (mio_icif_wrench.dismantle) drops through the loot with the wrench as tool
                var state = h.getBlockState(dismantled);
                Block.dropResources(state, h.getLevel(), h.absolutePos(dismantled), h.getBlockEntity(dismantled), player, new ItemStack(item("item_tool_wrench_elc")));
                h.getLevel().removeBlock(h.absolutePos(dismantled), false);
            })
            .thenIdle(1)
            .thenExecute(() -> {
                for (BlockPos p : new BlockPos[] {byHand, byWrench, dismantled}) {
                    List<ItemStack> drops = dropsAt(h, p).stream().filter(s -> s.is(bare)).toList();
                    h.assertTrue(drops.size() == 1, p + " dropped " + drops);
                    ItemStack drop = drops.get(0);
                    h.assertTrue(drop.getComponentsPatch().isEmpty() && ItemStack.isSameItemSameComponents(drop, crafted),
                        p + " dropped a cable with data " + drop.getComponentsPatch());
                }
                // a cable dropped by an older version (wrench, {id, energy}) loses the data when it loads
                ItemStack old = new ItemStack(bare);
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.putString("id", "mio_icif:wire");
                tag.putLong("energy", 0L);
                old.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                ItemStack loaded = ItemStack.parse(h.getLevel().registryAccess(), old.save(h.getLevel().registryAccess())).orElseThrow();
                h.assertTrue(ItemStack.isSameItemSameComponents(loaded, crafted), "an old wrenched cable still has " + loaded.getComponentsPatch());
                player.discard();
            })
            .thenSucceed();
    }

    /**
     * Trees on crop sticks give wood (IC2 CropBaseSapling): every harvest of a grown tree is one log, a
     * sapling now and then, an apple now and then for oak only; never leaves.
     */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void treeCropsDropLogs(GameTestHelper h) {
        BlockPos rack = new BlockPos(2, 2, 2);
        h.setBlock(rack.below(), net.minecraft.world.level.block.Blocks.FARMLAND);
        h.setBlock(rack, block("crop/stick"));
        var crop = (com.miophas.singularity_iteration.core.api.crop.IPlanter) h.getBlockEntity(rack);
        String[][] trees = {{"oak_sapling", "oak"}, {"spruce_sapling", "spruce"}, {"dark_oak_sapling", "dark_oak"}};
        for (String[] tree : trees) {
            var plant = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", tree[0]);
            h.assertTrue(plant != null, tree[0] + " is not a crop");
            crop.setPlant(plant);
            crop.setGrowthStage(plant.getMaxGrowthStage());
            Item log = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(tree[1] + "_log"));
            Item sapling = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(tree[1] + "_sapling"));
            int saplings = 0, apples = 0;
            for (int i = 0; i < 400; i++) {
                ItemStack[] harvest = plant.getHarvest(crop);
                h.assertTrue(harvest.length >= 1 && harvest[0].is(log) && harvest[0].getCount() == 1, tree[0] + " gave " + Arrays.toString(harvest));
                for (ItemStack drop : harvest) {
                    h.assertTrue(drop.is(log) || drop.is(sapling) || drop.is(Items.APPLE), tree[0] + " dropped " + drop);
                    if (drop.is(sapling)) saplings++;
                    if (drop.is(Items.APPLE)) apples++;
                }
            }
            h.assertTrue(saplings > 50 && saplings < 150, tree[0] + ": " + saplings + " saplings in 400 harvests (25% expected)");
            h.assertTrue(tree[0].equals("oak_sapling") ? apples > 50 && apples < 150 : apples == 0, tree[0] + ": " + apples + " apples in 400 harvests");
        }
        h.succeed();
    }

    /** Feeds one {@code item} to the scrap module (from an empty value) and returns the scrap value it added. */
    private static float scrapValueAdded(BlockEntity module, ItemStack item) {
        try {
            var field = module.getClass().getDeclaredField("scrapValue");
            field.setAccessible(true);
            field.setFloat(module, 0F);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        var slots = (IItemHandler) call(module, "getItemHandler");
        ItemStack rest = slots.insertItem(0, item, false);
        if (!rest.isEmpty()) return -1F;   // refused
        call(module, "processScrapSlot");
        return (Float) call(module, "getScrapValue");
    }

    /**
     * Option 3 (0.1.10.15): the scrap module adds the matter-amplifier value × 10 (METS): IC2 scrap 50,000,
     * scrap box 450,000, SI thorium scrap 3,600,000, and any Mass Fabricator amplifier recipe × 10.
     */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void scrapModuleTakesTenTimesTheAmplifierValue(GameTestHelper h) {
        var api = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getRecipeRegistrationAPI();
        h.assertTrue(api.registerMassFabricatorAmplifier(ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "feedback_echo_shard"),
            net.minecraft.world.item.crafting.Ingredient.of(Items.ECHO_SHARD), 12_345), "amplifier recipe refused");
        api.syncToRecipeManager();
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block("producer/block_large_fabricator_scrap"));
        BlockEntity module = h.getBlockEntity(pos);
        float scrap = scrapValueAdded(module, new ItemStack(item("normal/item_scrap")));
        float box = scrapValueAdded(module, new ItemStack(item("normal/item_scrapbox")));
        float thorium = scrapValueAdded(module, new ItemStack(item("normal/item_thorium_scrapbox")));
        float shard = scrapValueAdded(module, new ItemStack(Items.ECHO_SHARD));
        float dirt = scrapValueAdded(module, new ItemStack(Items.DIRT));
        h.assertTrue(scrap == 50_000F && box == 450_000F && thorium == 3_600_000F,
            "scrap " + scrap + ", scrap box " + box + ", thorium scrap " + thorium);
        h.assertTrue(shard == 123_450F, "an amplifier recipe of 12,345 added " + shard);
        h.assertTrue(dirt == -1F, "dirt was taken as scrap");
        h.succeed();
    }

    /** The core GUI values survive the 16-bit container sync: 3,650,000 scrap value and 200,000 mB of UU-matter. */
    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void largeFabricatorGuiShowsLargeValues(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        h.setBlock(core.west(), block("producer/block_large_fabricator_scrap"));
        h.setBlock(core, block("producer/block_large_fabricator_core"));
        BlockEntity coreBe = h.getBlockEntity(core), module = h.getBlockEntity(core.west());
        try {
            var field = module.getClass().getDeclaredField("scrapValue");
            field.setAccessible(true);
            field.setFloat(module, 3_650_000F);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        ((FluidTank) call(coreBe, "getUuMatterTank")).fill(new FluidStack(BuiltInRegistries.FLUID.get(si("uumatter")), 100_000), IFluidHandler.FluidAction.EXECUTE);
        var player = h.makeMockServerPlayerInLevel();
        Class<?> menuType = Round33GameTests.common("menu.producer.LargeFabricatorCoreMenu");
        Object server, client;
        try {
            server = menuType.getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, coreBe.getClass())
                .newInstance(1, player.getInventory(), coreBe);
            client = menuType.getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class).newInstance(1, player.getInventory());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        call(server, "broadcastChanges");
        call(server, "broadcastChanges");
        // what ClientboundContainerSetDataPacket does: every data slot of the menu travels as a short
        try {
            var field = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("dataSlots");
            field.setAccessible(true);
            var slots = (List<?>) field.get(server);
            for (int i = 0; i < slots.size(); i++)
                call(client, "setData", i, (int) (short) ((net.minecraft.world.inventory.DataSlot) slots.get(i)).get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        player.discard();
        int shownScrap = (Integer) call(client, "getScrapValue"), shownUu = (Integer) call(client, "getFluidAmount");
        h.assertTrue(shownScrap == 3_650_000, "the GUI shows a scrap value of " + shownScrap + " (server menu " + call(server, "getScrapValue")
            + ", core " + call(coreBe, "getScrapValue") + ", module " + call(module, "getScrapValue") + ")");
        h.assertTrue(shownUu == (Integer) call(coreBe, "getTotalUuMatterAmount") && shownUu > 32_767, "the GUI shows " + shownUu + " mB of UU-matter");
        h.succeed();
    }

    @GameTest(batch = "player_feedback", template = "empty", timeoutTicks = 20)
    public static void earmuffsAreWornOnTheHead(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        ItemStack muffs = new ItemStack(item("armor/item_soundproof_earmuffs"));
        Class<?> earmuffs = Round33GameTests.common("item.armor.mio_icif_soundproof_earmuffs");
        h.assertFalse((Boolean) call(earmuffs, "isWorn", player), "worn before they were put on");
        h.assertTrue(player.getEquipmentSlotForItem(muffs) == EquipmentSlot.HEAD, "not a head item");
        player.setItemSlot(EquipmentSlot.HEAD, muffs);
        boolean worn = (Boolean) call(earmuffs, "isWorn", player);
        player.discard();
        h.assertTrue(worn, "not worn in the helmet slot");
        h.assertTrue(muffs.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "head"))), "not in curios:head");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(si("soundproof_earmuffs")).isPresent(), "no recipe");
        h.succeed();
    }
}
