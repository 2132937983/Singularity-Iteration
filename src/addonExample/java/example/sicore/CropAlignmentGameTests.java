package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.runtime.crop.PlantHybridization;
import com.miophas.singularity_iteration.core.runtime.processing.CropSeedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests real registered content through public contracts; reflection only drives isolated machine cycles. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CropAlignmentGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static PlantType plant(String name) { return MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", name); }
    private static IPlanter crop(GameTestHelper h, BlockPos pos, String species, int stage) {
        h.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(pos, block("crop/stick"));
        IPlanter crop = (IPlanter) h.getBlockEntity(pos);
        if (species != null) { crop.setPlant(plant(species)); crop.setGrowthStage(stage); }
        return crop;
    }
    private static void cycle(IPlanter crop) { invoke(crop, "performCropCycle", new Class<?>[0]); }
    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            var method = target.getClass().getDeclaredMethod(name, types); method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(name, failure); }
    }
    private static void use(GameTestHelper h, BlockPos pos, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = h.absolutePos(pos);
        h.getBlockState(pos).useItemOn(stack, h.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void crossingAndSuppliesUseRealBlockInteractions(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        IPlanter crop = crop(h, pos, null, 0);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(crop.getWater() == 0 && crop.getNutrients() == 0, "Fresh crop resources must be zero");
        ItemStack sticks = new ItemStack(block("crop/stick"), 2);
        use(h, pos, player, sticks);
        h.assertTrue(crop.isHybridBase() && sticks.getCount() == 1, "Second stick must create crossing base");
        use(h, pos, player, new ItemStack(Items.WHEAT_SEEDS));
        h.assertTrue(crop.getPlant() == null && crop.isHybridBase(), "Crossing base accepted ordinary seeds");
        crop.setNutrients(99);
        use(h, pos, player, item("resource/item_fertilizer"));
        h.assertTrue(crop.getNutrients() == 199, "Manual fertilizer was capped at 100");
        ItemStack weedEx = item("normal/item_weedex");
        use(h, pos, player, weedEx);
        h.assertTrue(crop.getWeedControl() == 50 && player.getMainHandItem().getDamageValue() == 1,
            "Weed-EX can must transfer one 50 mB dose");
        use(h, pos, player, player.getMainHandItem());
        h.assertTrue(crop.getWeedControl() == 100 && player.getMainHandItem().getDamageValue() == 2, "Second Weed-EX dose incorrect");
        crop.setHybridBase(false);
        use(h, pos, player, new ItemStack(Items.POPPY, 3));
        h.assertTrue(crop.getPlant() == null, "Three flowers incorrectly satisfied the four-item base seed requirement");
        ItemStack flowers = new ItemStack(Items.POPPY, 5);
        use(h, pos, player, flowers);
        h.assertTrue(crop.getPlant() != null && flowers.getCount() == 1, "Base flower planting must consume exactly four items");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void weedExPreventsInvasionUntilItsReserveIsSpent(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        IPlanter weed = crop(h, pos, "weed", 3);
        IPlanter[] neighbors = new IPlanter[4];
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int i = 0; i < directions.length; i++) {
            neighbors[i] = crop(h, pos.relative(directions[i]), "wheat", 3);
            neighbors[i].setWeedControl(5); neighbors[i].setResilience(0);
        }
        long seed = 84;
        int selected = RandomSource.create(seed).nextInt(4);
        h.getLevel().random.setSeed(seed);
        invoke(weed, "spreadWeed", new Class<?>[0]);
        h.assertTrue(neighbors[selected].getPlant() == plant("wheat") && neighbors[selected].getWeedControl() == 0,
            "Weed-EX must prevent invasion and consume five units");
        h.getLevel().random.setSeed(seed);
        invoke(weed, "spreadWeed", new Class<?>[0]);
        h.assertTrue(neighbors[selected].getPlant() == plant("weed"), "Unprotected zero-resistance crop resisted invasion");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=300)
    public static void growthRunsOncePer256Ticks(GameTestHelper h) {
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "netherwart", 1);
        crop.setNutrients(150); crop.setWater(150);
        h.runAfterDelay(250, () -> {
            h.assertTrue(crop.getProgress() == 0 && crop.getWater() == 150, "Growth ran before the 256-tick boundary");
            h.runAfterDelay(8, () -> {
                h.assertTrue(crop.getProgress() > 0 && crop.getWater() == 149 && crop.getNutrients() == 149,
                    "One crop cycle did not perform growth and resource decay");
                h.succeed();
            });
        });
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void rootsAndSpecialTickCannotBypassStageProgression(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 3, 3);
        IPlanter crop = crop(h, pos, "ferru", 3);
        h.setBlock(pos.below(2), Blocks.IRON_BLOCK);
        h.assertTrue(crop.getPlant().canGrow(crop) && crop.isBlockBelow("blockIron"), "Metal roots did not pass through farmland");
        h.setBlock(pos.below(2), Blocks.AIR); h.setBlock(pos.below(3), Blocks.IRON_BLOCK);
        h.assertTrue(!crop.getPlant().canGrow(crop), "Roots crossed an air gap");
        h.setBlock(pos.below(2), Blocks.SOUL_SAND);
        crop.setPlant(plant("netherwart")); crop.setGrowthStage(1); crop.setProgress(950); crop.setWater(200); crop.setNutrients(199);
        cycle(crop);
        h.assertTrue(crop.getGrowthStage() == 2 && crop.getProgress() == 0, "Nether wart special tick lost normal stage advancement");
        h.assertTrue(crop.getWater() == 199 && crop.getNutrients() == 198, "Resource decay must happen once per cycle");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void poorEnvironmentKillsAndResistanceCanPreventDeath(GameTestHelper h) {
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "diareed", 1);
        crop.setGrowthSpeed(31); crop.setYield(31); crop.setResilience(0);
        h.getLevel().random.setSeed(1234);
        for (int i = 0; i < 20 && crop.getPlant() != null; i++) cycle(crop);
        h.assertTrue(crop.getPlant() == null, "High-tier unsupported crop never died");
        crop.setPlant(plant("diareed")); crop.setGrowthStage(1);
        crop.setGrowthSpeed(31); crop.setYield(31); crop.setResilience(31);
        for (int i = 0; i < 20; i++) cycle(crop);
        h.assertTrue(crop.getPlant() != null && crop.getProgress() == 0, "Resistance 31 must survive but cannot grow in insufficient conditions");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void fourParentsParticipateAndSingleNeighborSpreads(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 2, 5);
        IPlanter target = crop(h, pos, null, 0);
        target.setWater(200); target.setNutrients(199); target.setHybridBase(true);
        int[] gains = {0, 8, 20, 31}; int i = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            IPlanter parent = crop(h, pos.relative(direction), "wheat", 7);
            parent.setGrowthSpeed(30); parent.setYield(gains[i++]);
        }
        long seed = 0;
        for (;; seed++) {
            RandomSource random = RandomSource.create(seed); boolean all = true;
            for (int j = 0; j < 4; j++) if (random.nextInt(16) > 6) all = false;
            if (all) break;
        }
        h.getLevel().random.setSeed(seed);
        h.assertTrue(PlantHybridization.forceHybridize(target), "Four eligible parents failed to cross");
        h.assertTrue(target.getYield() >= 10 && target.getYield() <= 18, "Inheritance did not average all four parents with +/-4 mutation");
        h.assertTrue(!target.isHybridBase() && target.getScanLevel() == 0 && target.getGrowthStage() == 1, "Offspring state invalid");
        h.assertTrue(PlantHybridization.calculateRatio(plant("wheat"), plant("wheat")) == 500, "Same-species weight incorrect");
        h.assertTrue(PlantHybridization.participationBase(30, 31) == 2, "High resistance did not suppress participation");
        target.reset(); target.setHybridBase(true);
        for (Direction direction : Direction.Plane.HORIZONTAL) if (direction != Direction.NORTH) h.setBlock(pos.relative(direction), Blocks.AIR);
        IPlanter parent = (IPlanter) h.getBlockEntity(pos.north());
        boolean spread = false;
        for (int attempt = 0; attempt < 100 && !spread; attempt++) spread = PlantHybridization.trySpread(target);
        h.assertTrue(spread && target.getGrowthSpeed() == parent.getGrowthSpeed() && target.getYield() == parent.getYield(), "Single-parent spreading must copy exact traits");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void harvestDistributionAndSeedBagsPreserveTraits(GameTestHelper h) {
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 6);
        h.assertTrue(!crop.getPlant().canBeHarvested(crop), "Wheat harvested before size seven");
        crop.setGrowthStage(7); crop.setYield(31); crop.setGrowthSpeed(12); crop.setResilience(15); crop.setScanLevel(4);
        h.getLevel().random.setSeed(8432);
        int sum = 0, zeros = 0;
        for (int i = 0; i < 20000; i++) { int n = crop.getPlant().calculateDropCount(crop); sum += n; if (n == 0) zeros++; }
        h.assertTrue(sum / 20000.0 > 2.30 && sum / 20000.0 < 2.55 && zeros > 1000, "Harvest distribution diverged from IC2 Gaussian with zero yields");
        for (int i = 0; i < 100; i++) {
            crop.setGrowthStage(7);
            var drops = crop.doHarvest();
            h.assertTrue(crop.getGrowthStage() == 2, "Wheat must regrow from size two");
            for (ItemStack drop : drops) h.assertTrue(drop.is(Items.WHEAT) && drop.getCount() == 1, "Yield was multiplied twice or normal harvest produced seeds");
        }
        ItemStack seed = crop.getPlant().getSeedItem(crop);
        var traits = CropSeedData.traits(seed);
        h.assertTrue(seed.getItem() instanceof ICropSeedItem && traits.growth() == 12 && traits.yield() == 31
            && traits.resilience() == 15 && traits.scan() == 4, "Seeds lost genetics or scan level");
        crop.setGrowthSpeed(1); crop.setYield(1); crop.setResilience(1);
        h.assertTrue(crop.getPlant().getSeedItem(crop).is(Items.WHEAT_SEEDS), "Low-stat vanilla crop should return vanilla seeds");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void correctedCatalogAndSpecialStagesReachRuntime(GameTestHelper h) {
        int matched = 0;
        for (PlantType plant : MioIcifAPI.instance().getCropAPI().getAllPlants()) {
            if (!plant.getModId().equals("mio_icif") || plant.getTypeId().equals("titanium") || plant.getTypeId().equals("uranium")) continue;
            h.assertTrue(plant.usesIc2CropCycle(), "Built-in crop still uses old time units: " + plant.getTypeId()); matched++;
        }
        h.assertTrue(matched == 52, "Missing original species");
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "coffee", 4);
        h.assertTrue(crop.getPlant().getStats().getLevel() == 7 && crop.getPlant().canBeHarvested(crop)
            && crop.getPlant().getGains(crop).length == 0, "Coffee stage four must be harvestable but unproductive");
        crop.setPlant(plant("stickreed")); crop.setGrowthStage(3);
        h.assertTrue(crop.getPlant().canBeHarvested(crop) && crop.getPlant().getGains(crop)[0].is(Items.SUGAR_CANE), "Early reed harvest is unreachable");
        crop.setPlant(plant("diareed")); crop.setGrowthStage(4);
        for (int i = 0; i < 30; i++) h.assertTrue(crop.getPlant().getGains(crop)[0].is(item("resource/item_diamond_dust_small").getItem()), "Diareed base gain is not tiny dust");
        crop.setPlant(plant("redwheat")); crop.setGrowthStage(7); crop.updateState();
        h.assertTrue(((BlockEntity)crop).getBlockState().getSignal(h.getLevel(), crop.getPlanterPos(), Direction.UP) == 15
            && ((BlockEntity)crop).getBlockState().getLightEmission(h.getLevel(), crop.getPlanterPos()) == 7, "Mature redwheat did not emit redstone and light");
        crop.setPlant(plant("venomilia")); crop.setGrowthStage(5); crop.setGrowthSpeed(8);
        h.assertTrue(crop.getPlant().isWeed(crop) && crop.getPlant().getGains(crop)[0].is(item("resource/item_grin_dust").getItem()), "Venomilia stage-five policy missing");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void legacySaveMigratesOnlyProgressAndNewSaveRoundTrips(GameTestHelper h) {
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 4);
        crop.setProgress(19000); crop.setGrowthSpeed(7); crop.setYield(9); crop.setResilience(11); crop.setScanLevel(3); crop.setWater(83);
        BlockEntity entity = (BlockEntity) crop;
        var saved = entity.saveWithFullMetadata(h.getLevel().registryAccess()); saved.remove("CropCycleVersion");
        BlockEntity migrated = BlockEntity.loadStatic(crop.getPlanterPos(), entity.getBlockState(), saved, h.getLevel().registryAccess());
        IPlanter result = (IPlanter) migrated;
        h.assertTrue(result.getProgress() == 0 && result.getGrowthStage() == 4 && result.getGrowthSpeed() == 7
            && result.getYield() == 9 && result.getResilience() == 11 && result.getScanLevel() == 3 && result.getWater() == 83, "Legacy migration altered crop identity or traits");
        result.setProgress(123);
        var current = migrated.saveWithFullMetadata(h.getLevel().registryAccess());
        IPlanter restored = (IPlanter) BlockEntity.loadStatic(crop.getPlanterPos(), entity.getBlockState(), current, h.getLevel().registryAccess());
        h.assertTrue(restored.getProgress() == 123, "New-format reload reset valid growth points");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void matronScansOnePositionAndPaysForEachSupply(GameTestHelper h) {
        BlockPos machinePos = new BlockPos(6, 3, 6);
        h.setBlock(machinePos, block("producer/block_matron_elc"));
        var machine = (AbstractEnergyBlockEntity) h.getBlockEntity(machinePos);
        IPlanter crop = crop(h, new BlockPos(3, 2, 2), "netherwart", 1);
        crop.setNutrients(99);
        var port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(machinePos), Direction.UP);
        h.assertTrue(port != null && port.getTanks() == 2 && port.getTankCapacity(0) == 2000 && port.getTankCapacity(1) == 2000, "Cropmatron tank capacities incorrect");
        port.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        port.fill(new FluidStack(BuiltInRegistries.FLUID.get(id("weed_ex")), 1000), IFluidHandler.FluidAction.EXECUTE);
        var inv = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(machinePos), Direction.UP);
        inv.insertItem(10, item("resource/item_fertilizer"), false);
        machine.getEnergyStorageInternal().setEnergy(1000);
        invoke(machine, "processCrops", new Class<?>[0]);
        h.assertTrue(crop.getNutrients() == 189 && crop.getWater() == 200 && crop.getWeedControl() == 150, "Matron did not apply all three independent supplies");
        h.assertTrue(machine.getEnergyStorageInternal().getAmount() == 969 && machine.getEnergyStorageInternal().getCapacity() == 10000, "Matron should consume exactly 1+10+10+10 EU");
        h.assertTrue(port.getFluidInTank(0).getAmount() == 800 && port.getFluidInTank(1).getAmount() == 850, "Supply transfer violated fluid conservation");
        invoke(machine, "processCrops", new Class<?>[0]);
        h.assertTrue(machine.getEnergyStorageInternal().getAmount() == 968, "Second scan revisited the first crop instead of advancing");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void weedExCapabilitySimulatesAndConservesEveryDose(GameTestHelper h) {
        ItemStack can = item("normal/item_weedex");
        var fluid = can.getCapability(Capabilities.FluidHandler.ITEM);
        h.assertTrue(fluid != null && fluid.getTankCapacity(0) == 3200, "Weed-EX item lacks original 3200 mB capacity");
        h.assertTrue(fluid.drain(49, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Can must not provide partial 50 mB doses");
        h.assertTrue(fluid.drain(100, IFluidHandler.FluidAction.SIMULATE).getAmount() == 50 && can.getDamageValue() == 0, "Simulation mutated can");
        int drained = 0;
        for (int i = 0; i < 65; i++) drained += fluid.drain(2000, IFluidHandler.FluidAction.EXECUTE).getAmount();
        h.assertTrue(drained == 3200 && fluid.getContainer().isEmpty(), "Weed-EX can duplicated or lost doses");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void analyzerStopsAtOutputAndChargesEveryStageExactlyOnce(GameTestHelper h) throws Exception {
        ItemStack analyzer = item("normal/item_crop_analyzer");
        var battery = (com.miophas.singularity_iteration.core.api.item.IBatteryItem) analyzer.getItem();
        battery.setEnergy(analyzer, 10000);
        h.assertTrue(battery.getMaxEnergy() == 100000 && battery.getChargeRate(analyzer) == 128, "Analyzer energy parameters incorrect");
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 7);
        ItemStack seed = crop.makeSeeds(crop.getPlant(), 1, 2, 3, 4);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        String menuClass = analyzer.getItem().getClass().getPackageName().replace("item.tools", "menu.tool") + ".CropAnalyzerMenu";
        var menu = (net.minecraft.world.inventory.AbstractContainerMenu) Class.forName(menuClass)
            .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, ItemStack.class)
            .newInstance(1, player.getInventory(), analyzer);
        int[] costs = {10, 90, 900, 9000}; long remaining = 10000;
        for (int step = 0; step < 4; step++) {
            menu.getSlot(0).set(seed); menu.getSlot(1).set(ItemStack.EMPTY);
            menu.broadcastChanges(); remaining -= costs[step];
            seed = menu.getSlot(1).getItem();
            h.assertTrue(!seed.isEmpty() && ((ICropSeedItem) seed.getItem()).cropScanLevel(seed) == step + 1
                && battery.getEnergy(analyzer) == remaining, "Analyzer stage or cost incorrect");
            menu.broadcastChanges();
            h.assertTrue(menu.getSlot(0).getItem().isEmpty() && battery.getEnergy(analyzer) == remaining,
                "Analyzer automatically recycled output or charged twice");
        }
        h.assertTrue(remaining == 0, "Complete analysis must cost 10000 EU");
        ItemStack addonSeed = new ItemStack(CoreExampleMod.FOREIGN_CROP_SEED.get());
        battery.setEnergy(analyzer, 10);
        menu.getSlot(0).set(addonSeed); menu.getSlot(1).set(ItemStack.EMPTY); menu.broadcastChanges();
        h.assertTrue(((ICropSeedItem) menu.getSlot(1).getItem().getItem()).cropScanLevel(menu.getSlot(1).getItem()) == 1
            && battery.getEnergy(analyzer) == 0, "Analyzer ignored the addon seed contract");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void harvesterUsesOptimalStageAndPaysPerReturnedStack(GameTestHelper h) {
        BlockPos machinePos = new BlockPos(6, 3, 6);
        h.setBlock(machinePos, block("producer/block_harvest_elc"));
        var machine = (AbstractEnergyBlockEntity) h.getBlockEntity(machinePos);
        IPlanter crop = crop(h, new BlockPos(3, 2, 2), "stickreed", 4);
        crop.setYield(31); machine.getEnergyStorageInternal().setEnergy(1000);
        double chance = Math.pow(0.95, 4) * Math.pow(1.03, 31);
        long seed = 0; int expected;
        do { seed++; expected = Math.max(0, (int) Math.round(RandomSource.create(seed).nextGaussian() * chance * 0.6827 + chance)); } while (expected < 2);
        h.getLevel().random.setSeed(seed);
        invoke(machine, "scanAndHarvest", new Class<?>[]{Level.class, BlockPos.class}, h.getLevel(), h.absolutePos(machinePos));
        h.assertTrue(machine.getEnergyStorageInternal().getAmount() == 1000 - 20 * expected, "Harvester must charge per returned stack");
        h.assertTrue(crop.getGrowthStage() >= 1 && crop.getGrowthStage() <= 3, "Stickreed did not regrow from a random size 1..3");
        var inventory = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(machinePos), Direction.WEST);
        int stored = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) stored += inventory.getStackInSlot(slot).getCount();
        h.assertTrue(stored == expected, "Harvester lost or duplicated product");
        var tag = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(tag.getInt("CropScanIndex") == 1, "Harvester cursor is not persisted");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void venomEatingAndTrowelHaveOriginalEffects(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 3, 3);
        IPlanter crop = crop(h, pos, "venomilia", 5);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        crop.getPlant().onCollision(crop, player);
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON) && crop.getGrowthStage() == 4,
            "Venomilia collision must poison and return to size four");
        crop.setPlant(plant("eatingplant")); crop.setGrowthStage(3);
        h.setBlock(pos.below(2), Blocks.LAVA);
        h.assertTrue(crop.getPlant().canGrow(crop), "Eating plant did not recognize deep lava");
        var pig = h.spawn(net.minecraft.world.entity.EntityType.PIG, pos.above());
        float health = pig.getHealth();
        crop.getPlant().tick(crop);
        h.assertTrue(pig.getHealth() < health && pig.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
            && crop.getCustomData().getBoolean("eaten"), "Eating plant did not attack, slow and store eaten state");
        crop.setPlant(plant("weed")); crop.setGrowthStage(4);
        ItemStack trowel = item("normal/item_weeding_trowel");
        player.setItemInHand(InteractionHand.MAIN_HAND, trowel);
        var hit = new BlockHitResult(Vec3.atCenterOf(crop.getPlanterPos()), Direction.UP, crop.getPlanterPos(), false);
        var result = trowel.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(result.consumesAction() && crop.getPlant() == null && !trowel.isDamageableItem(), "Weeding trowel should clear weeds without durability");
        int weeds = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(crop.getPlanterPos()).inflate(1)).stream()
            .filter(entity -> entity.getItem().is(item("normal/item_weed").getItem())).mapToInt(entity -> entity.getItem().getCount()).sum();
        h.assertTrue(weeds == 4, "Trowel must return weed count equal to size");
        h.succeed();
    }

    @GameTest(template="reactor_loop", batch="agriculture", timeoutTicks=100)
    public static void breakingAPlantedStickCannotRestoreRemovedBlock(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        IPlanter crop = crop(h, pos, "wheat", 7);
        crop.setGrowthSpeed(12); crop.setResilience(31);
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        h.assertTrue(h.getBlockState(pos).isAir() && h.getLevel().getBlockEntity(h.absolutePos(pos)) == null,
            "Dropping crop seeds restored the block during onRemove");
        h.succeed();
    }
}
