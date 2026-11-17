package example.sicore;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IEnergyDistributable;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.35 (player reports): a charging battery charges the geomagnetic detector, which a machine no longer drains; the
 * advanced miner shows the power it really uses (it showed 1 EU/t whatever the overclockers).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11535GameTests {
    private Round11535GameTests() {}

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path))); }

    /** An RE charging battery feeds an empty geomagnetic detector in the inventory; the detector is not an energy source. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void chargingBatteryChargesTheDetector(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack battery = item("normal/item_chargebat_lev0"), detector = item("item_geomagnetic_detector");
        var source = (IEnergyDistributable) battery.getItem();
        var target = (IBatteryItem) detector.getItem();
        target.extractEnergy(detector, target.getEnergy(detector));
        h.assertTrue(target.getEnergy(detector) == 0 && source.getEnergy(battery) > 0, "fixture: empty detector, charged battery");
        player.getInventory().setItem(0, battery);
        player.getInventory().setItem(1, detector);
        source.distributeEnergyToItems(player, battery, 1000);
        h.assertTrue(target.getEnergy(player.getInventory().getItem(1)) > 0, "the charging battery skipped the detector");
        h.assertTrue(!BatteryTransfer.canProvideExternally(player.getInventory().getItem(1)), "a machine may drain the detector");
        h.assertTrue(!BatteryTransfer.canProvideExternally(item("item_electric_wireless_manager")), "a machine may drain the wireless manager");
        h.succeed();
    }

    /** A working miner shows more than 1 EU/t, and more with overclockers (each adds five blocks a cycle at 512 EU). */
    @GameTest(template = "empty", batch = "round11535_miner", timeoutTicks = 180)
    public static void minerShowsItsRealPower(GameTestHelper h) {
        // the miner's 9 x 9 area, deep enough that a cycle never runs out of blocks in its layer
        BlockPos at = new BlockPos(4, 8, 4);
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) for (int y = 1; y <= 7; y++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_advanced_miner_elc")));
        var miner = h.getBlockEntity(at);
        ((AbstractEnergyBlockEntity) miner).getEnergyStorageInternal().setStored(4_000_000);
        var items = (IItemHandlerModifiable) ArmoryServiceGameTests.field(miner, "itemHandler");
        ItemStack scanner = item("item_tool_od_scanner");
        if (scanner.getItem() instanceof IBatteryItem b) b.setEnergy(scanner, b.getMaxEnergy(scanner));
        items.setStackInSlot(1, scanner);
        var producer = (IProducerBlock) miner;
        var finalItems = items;
        long[] slow = {-1};
        int[] tick = {0};
        h.onEachTick(() -> {
            ((AbstractEnergyBlockEntity) miner).getEnergyStorageInternal().setStored(4_000_000);
            int now = ++tick[0];
            if (now == 80) {
                slow[0] = producer.getDisplayedEnergyPerTick();
                h.assertTrue(slow[0] > 1, "a working miner shows " + slow[0] + " EU/t");
                finalItems.setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "upgrade/overclocker_upgrade")), 3));
            }
            if (now == 160) {
                long fast = producer.getDisplayedEnergyPerTick();
                h.assertTrue(fast > slow[0], "overclockers did not raise the shown power: " + slow[0] + " -> " + fast);
                h.succeed();
            }
        });
    }
}
