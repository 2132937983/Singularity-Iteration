package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.miophas.singularity_iteration.core.runtime.crop.CropGrowthMath;
import com.miophas.singularity_iteration.core.runtime.crop.ScannerMath;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * <p><b>EN:</b> 0.1.8.2 agriculture and Advanced Crop Scanner tests. They use public contracts; reflection
 * only reaches the scanner menu (in the common package) and the cross-stick record.
 * <p><b>ZH:</b> 0.1.8.2 农业与次世代作物扫描仪测试。使用公开约定；反射只用于访问扫描仪菜单（common 包）与杂交作物架记录。
 * <p><b>JA:</b> 0.1.8.2 の農業と次世代型作物スキャナーのテスト。公開契約を使う。リフレクションはスキャナーメニュー（common パッケージ）と交配棒の記録にだけ使う。
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CropScanner182GameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Item itemOf(String path) { return BuiltInRegistries.ITEM.get(id(path)); }
    private static ItemStack item(String path, int count) { return new ItemStack(itemOf(path), count); }
    private static PlantType plant(String name) { return MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", name); }

    // EN: Menu constants (mirrors CropAnalyzerMenu). ZH: 菜单常量（与 CropAnalyzerMenu 一致）。JA: メニュー定数（CropAnalyzerMenu と同じ）。
    private static final int SLOT_UPGRADE = 28, SLOT_PARENT_A = 32, SLOT_PARENT_B = 33, SLOT_BREED_OUT = 34, SLOT_GENE = 35, SLOT_CATALYST = 36;
    private static final int BTN_UNLOCK_BREED = 3, BTN_UNLOCK_GENE = 4, BTN_BREED_TOGGLE = 5, BTN_GENE_TOGGLE = 6, BTN_SELECT = 32;

    private static IPlanter crop(GameTestHelper h, BlockPos pos, String species, int stage) {
        h.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(pos, block("crop/stick"));
        IPlanter crop = (IPlanter) h.getBlockEntity(pos);
        if (species != null) { crop.setPlant(plant(species)); crop.setGrowthStage(stage); }
        return crop;
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) {
        try {
            var method = target.getClass().getMethod(name, types);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(name, failure); }
    }

    private static AbstractContainerMenu openScanner(Player player, ItemStack scanner) {
        try {
            String menuClass = scanner.getItem().getClass().getPackageName().replace("item.tools", "menu.tool") + ".CropAnalyzerMenu";
            return (AbstractContainerMenu) Class.forName(menuClass)
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, ItemStack.class)
                .newInstance(1, player.getInventory(), scanner);
        } catch (ReflectiveOperationException failure) { throw new AssertionError("scanner menu", failure); }
    }

    private static void ticks(AbstractContainerMenu menu, int n) {
        for (int i = 0; i < n; i++) call(menu, "processTick", new Class<?>[0]);
    }

    private static ItemStack seed(IPlanter crop, String species, int count, int g, int ga, int r, int scan) {
        ItemStack s = crop.makeSeeds(plant(species), count, g, ga, r);
        ((ICropSeedItem) s.getItem()).setCropScanLevel(s, scan);
        return s;
    }

    private static int scan(ItemStack s) { return ((ICropSeedItem) s.getItem()).cropScanLevel(s); }

    /** EN: Parameters, parallel scan of all grid slots, exact cost, save on close. ZH: 参数、全部网格槽位并行扫描、精确成本、关闭时保存。JA: パラメーター、全グリッドスロットの並行走査、正確なコスト、閉じたときの保存。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 100)
    public static void scannerAnalyzesEveryGridSlotInParallel(GameTestHelper h) {
        ItemStack scanner = item("normal/item_crop_analyzer", 1);
        IBatteryItem battery = (IBatteryItem) scanner.getItem();
        h.assertTrue(battery.getMaxEnergy() == 4_000_000L && battery.getChargeRate(scanner) == 2048L, "Scanner energy parameters incorrect");
        battery.setEnergy(scanner, 1_000_000L);
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 7);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = openScanner(player, scanner);
        menu.getSlot(0).set(seed(crop, "wheat", 8, 1, 2, 3, 0));
        menu.getSlot(5).set(seed(crop, "pumpkin", 1, 1, 1, 1, 0));
        menu.getSlot(26).set(new ItemStack(CoreExampleMod.FOREIGN_CROP_SEED.get()));
        int need = ScannerMath.analysisTicks(0);
        ticks(menu, need - 1);
        h.assertTrue(scan(menu.getSlot(0).getItem()) == 0 && scan(menu.getSlot(5).getItem()) == 0, "A scan finished early");
        ticks(menu, 1);
        h.assertTrue(scan(menu.getSlot(0).getItem()) == 4 && scan(menu.getSlot(5).getItem()) == 4
            && scan(menu.getSlot(26).getItem()) == 4, "The grid slots did not finish together (addon seed contract included)");
        long used = ScannerMath.analysisTotalEu(8, 0) + 2 * ScannerMath.analysisTotalEu(1, 0);
        h.assertTrue(battery.getEnergy(scanner) == 1_000_000L - used, "Analysis cost incorrect: " + battery.getEnergy(scanner));
        h.assertTrue(menu.getSlot(0).getItem().getCount() == 8, "The whole stack must be analyzed at once");
        // EN: Analyzed stacks cost nothing more. ZH: 已分析的堆不再消耗。JA: 分析済みのスタックはもう消費しない。
        long before = battery.getEnergy(scanner);
        ticks(menu, 5);
        h.assertTrue(battery.getEnergy(scanner) == before, "Analyzed stacks still use energy");
        menu.removed(player);
        AbstractContainerMenu again = openScanner(player, scanner);
        h.assertTrue(again.getSlot(0).getItem().getCount() == 8 && scan(again.getSlot(0).getItem()) == 4, "Scanner contents were not saved");
        h.succeed();
    }

    /** EN: Two overclockers: time ×0.7², power ×1.6². ZH: 两张超频：时间 ×0.7²，功率 ×1.6²。JA: オーバークロッカー 2 枚：時間 ×0.7²、電力 ×1.6²。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 100)
    public static void overclockersSpeedUpTheScanAndRaiseThePower(GameTestHelper h) {
        ItemStack scanner = item("normal/item_crop_analyzer", 1);
        IBatteryItem battery = (IBatteryItem) scanner.getItem();
        battery.setEnergy(scanner, 100_000L);
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 7);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = openScanner(player, scanner);
        menu.getSlot(SLOT_UPGRADE).set(item("upgrade/overclocker_upgrade", 2));
        menu.getSlot(1).set(seed(crop, "wheat", 1, 1, 1, 1, 0));
        int need = ScannerMath.analysisTicks(2);
        h.assertTrue(need == 20, "Two overclockers must give 20 ticks, got " + need);
        ticks(menu, need);
        h.assertTrue(scan(menu.getSlot(1).getItem()) == 4, "Overclocked scan did not finish in time");
        h.assertTrue(battery.getEnergy(scanner) == 100_000L - ScannerMath.analysisTotalEu(1, 2), "Overclocked cost incorrect");
        h.succeed();
    }

    /** EN: Breeding: locked, unlock consumes the cost, the result is the selected target with averaged traits. ZH: 交配：锁定、解锁消耗成本、结果为选定目标且属性取平均。JA: 交配：ロック、アンロックでコストを消費、結果は選んだ目標で特性値は平均。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 200)
    public static void breedingResearchUnlocksAndGivesTheSelectedTarget(GameTestHelper h) {
        ItemStack scanner = item("normal/item_crop_analyzer", 1);
        IBatteryItem battery = (IBatteryItem) scanner.getItem();
        battery.setEnergy(scanner, 4_000_000L);
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 7);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = openScanner(player, scanner);
        menu.clickMenuButton(player, 1);
        h.assertTrue(!menu.clickMenuButton(player, BTN_UNLOCK_BREED), "Breeding unlocked without the cost");
        player.getInventory().add(item("resource/item_iridium_plate", 10));
        player.getInventory().add(item("normal/item_living_circuit", 1));
        h.assertTrue(menu.clickMenuButton(player, BTN_UNLOCK_BREED), "Breeding did not unlock with the cost");
        h.assertTrue(player.getInventory().countItem(itemOf("resource/item_iridium_plate")) == 0
            && player.getInventory().countItem(itemOf("normal/item_living_circuit")) == 0, "Unlock cost not consumed");
        menu.getSlot(SLOT_PARENT_A).set(seed(crop, "wheat", 2, 10, 20, 4, 4));
        menu.getSlot(SLOT_PARENT_B).set(seed(crop, "wheat", 2, 13, 3, 9, 4));
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) call(menu, "candidates", new Class<?>[0]);
        h.assertTrue(!list.isEmpty(), "No breeding candidates for wheat + wheat");
        h.assertTrue(menu.clickMenuButton(player, BTN_SELECT), "Candidate selection failed");
        PlantType target = (PlantType) call(list.get(0), "plant", new Class<?>[0]);
        long cost = (Long) call(list.get(0), "cost", new Class<?>[0]);
        h.assertTrue(menu.clickMenuButton(player, BTN_BREED_TOGGLE), "Breeding did not start");
        int need = ScannerMath.researchTicks(cost, ScannerMath.BREED_POWER, 0);
        ticks(menu, need - 1);
        h.assertTrue(menu.getSlot(SLOT_BREED_OUT).getItem().isEmpty(), "Research finished early");
        ticks(menu, 1);
        ItemStack out = menu.getSlot(SLOT_BREED_OUT).getItem();
        ICropSeedItem s = out.getItem() instanceof ICropSeedItem seed ? seed : null;
        h.assertTrue(s != null && s.cropId(out).equals(target.getTypeId()) && s.cropScanLevel(out) == 4, "Wrong research result");
        h.assertTrue(s.cropGrowth(out) == 11 && s.cropGain(out) == 11 && s.cropResistance(out) == 6, "Result traits must be the parent average");
        h.assertTrue(menu.getSlot(SLOT_PARENT_A).getItem().getCount() == 1 && menu.getSlot(SLOT_PARENT_B).getItem().getCount() == 1,
            "Each parent must lose exactly one bag");
        h.assertTrue(battery.getEnergy(scanner) == 4_000_000L - ScannerMath.researchTotalEu(cost, ScannerMath.BREED_POWER, 0), "Breeding cost incorrect");
        h.succeed();
    }

    /** EN: Gene editing: unlock, UU catalyst per three raised points, exact target. ZH: 基因编辑：解锁、每提升三点一个 UU 催化剂、精确目标。JA: 遺伝子改変：アンロック、上げた 3 点ごとに UU 触媒 1 個、目標値どおり。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 200)
    public static void geneEditingSetsTheTargetAndUsesTheCatalyst(GameTestHelper h) {
        ItemStack scanner = item("normal/item_crop_analyzer", 1);
        IBatteryItem battery = (IBatteryItem) scanner.getItem();
        battery.setEnergy(scanner, 4_000_000L);
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 7);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = openScanner(player, scanner);
        player.getInventory().add(item("resource/item_quantum_core", 1));
        player.getInventory().add(item("resource/item_neutron_plate", 4));
        player.getInventory().add(item("normal/item_super_circuit", 2));
        menu.clickMenuButton(player, 2);
        h.assertTrue(menu.clickMenuButton(player, BTN_UNLOCK_GENE), "Gene editing did not unlock with the cost");
        menu.getSlot(SLOT_GENE).set(seed(crop, "wheat", 1, 10, 10, 10, 4));
        ticks(menu, 1);
        call(menu, "setGeneTarget", new Class<?>[]{int.class, int.class}, 0, 13);
        h.assertTrue(!menu.clickMenuButton(player, BTN_GENE_TOGGLE), "Gene editing started without the catalyst");
        menu.getSlot(SLOT_CATALYST).set(item("normal/item_matter", 2));
        h.assertTrue(menu.clickMenuButton(player, BTN_GENE_TOGGLE), "Gene editing did not start");
        long cost = ScannerMath.geneCost(new int[]{10, 10, 10}, new int[]{13, 10, 10});
        int need = ScannerMath.researchTicks(cost, ScannerMath.GENE_POWER, 0);
        ticks(menu, need);
        ItemStack out = menu.getSlot(SLOT_GENE).getItem();
        ICropSeedItem s = (ICropSeedItem) out.getItem();
        h.assertTrue(s.cropGrowth(out) == 13 && s.cropGain(out) == 10 && s.cropResistance(out) == 10, "Gene edit result incorrect");
        h.assertTrue(menu.getSlot(SLOT_CATALYST).getItem().getCount() == 1, "Three raised points must use one UU-Matter");
        h.succeed();
    }

    /** EN: Right-click harvest gives the drops to the player; the plant goes to its after-harvest stage. ZH: 右键收获把掉落物交给玩家；植物回到收获后阶段。JA: 右クリック収穫はドロップをプレイヤーに渡し、植物は収穫後の段階に戻る。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 100)
    public static void manualHarvestGivesTheDropsToThePlayer(GameTestHelper h) {
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 7);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(crop.doManualHarvest(player), "Harvest of a mature crop failed");
        h.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.WHEAT) >= 1, "Harvest drops did not reach the player");
        h.assertTrue(crop.getGrowthStage() == plant("wheat").getStageAfterHarvest(crop), "Plant did not go to its after-harvest stage");
        h.succeed();
    }

    /** EN: Breaking a crossing base gives back both sticks. ZH: 破坏杂交基座返还两根作物架。JA: 交配台を壊すと棒が 2 本とも戻る。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 100)
    public static void brokenCrossingBaseReturnsBothSticks(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        IPlanter crop = crop(h, pos, null, 0);
        crop.setHybridBase(true);
        crop.updateState();
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        Item stick = block("crop/stick").asItem();
        AABB box = new AABB(h.absolutePos(pos)).inflate(2);
        int sticks = h.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream()
            .filter(e -> e.getItem().is(stick)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(sticks == 2, "A broken crossing base must drop two crop sticks, got " + sticks);
        h.succeed();
    }

    /** EN: The Cropmatron tending bonus removes the penalty of a 10/10/10 tier-1 crop in a poor spot. ZH: 作物监护机的照料加成消除了贫瘠环境中 10/10/10 一级作物的惩罚。JA: 作物管理機の管理ボーナスは、悪い場所の 10/10/10 ティア 1 作物のペナルティを消す。 */
    @GameTest(template = "reactor_loop", batch = "agriculture182", timeoutTicks = 100)
    public static void tendedHighTraitCropHasNoPenalty(GameTestHelper h) {
        int required = CropGrowthMath.requiredQuality(1, 20, 20, 0);   // 0.1.10.38: resistance lowers the requirement (SI rules)
        int poor = 10;
        h.assertTrue(CropGrowthMath.penalty(required, poor) > 0, "Test environment must be poor");
        int tended = CropGrowthMath.tendedQuality(poor, required);
        h.assertTrue(CropGrowthMath.penalty(required, tended) == 0 && CropGrowthMath.growthPoints(100, required, tended) >= 150,
            "A tended crop must grow at least 50 % faster than the base");
        IPlanter crop = crop(h, new BlockPos(3, 2, 3), "wheat", 1);
        crop.markTended(h.getLevel().getGameTime() + 100);
        h.assertTrue(crop.isTended(), "markTended did not mark the crop");
        h.succeed();
    }
}
