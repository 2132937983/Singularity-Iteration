package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.runtime.crop.ScannerMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.174, from a player report:
 * <ul>
 *   <li>a machine screen said "not enough power" with full power: the status fell back to it whenever the machine was not
 *       working at that instant, as five machines (the metal former, the advanced metal former, the block cutter, the oil
 *       refinery, the recycler) stood still for a tick after each operation (the advanced metal former seemed to switch off
 *       once per item). Those go on at once now, and "not enough power" shows only when the energy is short;</li>
 *   <li>the crop scanner's jobs (analysis, breeding, gene editing) ran only with its screen open; they go on in the player's
 *       inventory now.</li>
 * </ul>
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111174GameTests {
    private Round111174GameTests() {}

    private static final BlockPos AT = new BlockPos(2, 1, 2);
    private static final int RUNNING = 0, NO_POWER = 4;

    private static AbstractProcessingMachineBlockEntity machine(GameTestHelper h, String id) {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(AT);
    }

    private static void energy(AbstractProcessingMachineBlockEntity m, boolean full) {
        Object storage = m.getEnergyStorageInternal();
        Round33GameTests.call(storage, "setEnergy", full ? (Long) Round33GameTests.call(storage, "getCapacity") : 0L);
    }

    private static int status(Object machine) { return (Integer) Round33GameTests.call(machine, "processStatus"); }

    /** With input and a recipe but not running yet: "not enough power" without energy, running (about to start) with it. */
    @GameTest(template = "empty", batch = "round111174_machines")
    public static void noPowerOnlyWhenTheEnergyIsShort(GameTestHelper h) {
        for (String id : new String[]{"producer/block_metal_former", "producer/block_compressor_elc"}) {
            AbstractProcessingMachineBlockEntity m = machine(h, id);
            m.getItemHandler().insertItem(0, new ItemStack(id.contains("metal_former") ? Items.IRON_INGOT : Items.SNOWBALL, 16), false);
            energy(m, false);
            h.assertTrue(status(m) == NO_POWER, id + " without energy shows status " + status(m));
            energy(m, true);
            h.assertTrue(status(m) == RUNNING, id + " with full energy, before it starts, shows status " + status(m));
        }
        h.succeed();
    }

    /** In the tick its first plate comes out, the metal former is still working (it stood still for that tick). */
    @GameTest(template = "empty", batch = "round111174_machines", timeoutTicks = 900)
    public static void theMetalFormerGoesOnAfterEachItem(GameTestHelper h) {
        AbstractProcessingMachineBlockEntity m = machine(h, "producer/block_metal_former");
        m.getItemHandler().insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        boolean[] checked = {false};
        h.onEachTick(() -> {
            energy(m, true);
            if (!checked[0] && !m.getItemHandler().getStackInSlot(2).isEmpty()) {
                checked[0] = true;
                if (!m.isWorking() || status(m) != RUNNING)
                    h.fail("after its first plate the metal former stands: working " + m.isWorking() + ", status " + status(m));
            }
        });
        h.succeedWhen(() -> h.assertTrue(checked[0], "no plate yet"));
    }

    /** A seed put into the crop scanner is analyzed with the screen closed, the scanner in the player's inventory. */
    @GameTest(template = "empty", batch = "round111174_scanner", timeoutTicks = 400)
    public static void theCropScannerWorksWithItsScreenClosed(GameTestHelper h) throws ReflectiveOperationException {
        ItemStack scanner = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_crop_analyzer")));
        ((IBatteryItem) scanner.getItem()).setEnergy(scanner, 1_000_000L);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, scanner);
        var open = Round33GameTests.common("menu.tool.CropAnalyzerMenu")
            .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, ItemStack.class);
        AbstractContainerMenu menu = (AbstractContainerMenu) open.newInstance(1, player.getInventory(), scanner);
        menu.getSlot(0).set(new ItemStack(CoreExampleMod.FOREIGN_CROP_SEED.get()));
        menu.removed(player);   // the screen closes at once
        int need = ScannerMath.analysisTicks(0) + 2;
        int[] ticks = {0};
        // the player's inventory ticks the scanner once a game tick (the mock player is not ticked by the level)
        h.onEachTick(() -> { scanner.getItem().inventoryTick(scanner, h.getLevel(), player, 0, true); ticks[0]++; });
        h.succeedWhen(() -> {
            h.assertTrue(ticks[0] >= need, "waiting " + ticks[0] + " / " + need + " ticks");
            try {
                AbstractContainerMenu again = (AbstractContainerMenu) open.newInstance(1, player.getInventory(), scanner);
                ItemStack seed = again.getSlot(0).getItem();
                h.assertTrue(((ICropSeedItem) seed.getItem()).cropScanLevel(seed) == 4,
                    "with the screen closed the seed was scanned to level " + ((ICropSeedItem) seed.getItem()).cropScanLevel(seed));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
    }
}
