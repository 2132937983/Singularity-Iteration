package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.110 (processing overhaul M3): what the instrument screens of the two macerators are fed. The status code says why
 * a machine runs or stands; the menus send the status, the energy per operation and the work index from the server, lay
 * the slots out for the 248 x 204 screen, and the hold speed button switches the cyclone macerator. (The mod's common
 * classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111110GameTests {
    private Round111110GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static int status(Object be) { return (int) call(be, "processStatus"); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at, String id) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    /** No input 1, no recipe 2, no power 4, running 0, output full 3, off by redstone 5. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void maceratorStatusCodes(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        var m = place(h, at, "producer/block_powder_elc");
        IItemHandlerModifiable items = (IItemHandlerModifiable) call(m, "getItemHandler");
        h.runAfterDelay(2, () -> {
            h.assertTrue(status(m) == 1, "empty: " + status(m));
            items.setStackInSlot(0, new ItemStack(Items.STICK));
            h.assertTrue(status(m) == 2, "a stick: " + status(m));
            items.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 8));
            h.assertTrue(status(m) == 4, "iron ore without energy: " + status(m));
            m.getEnergyStorageInternal().setStored(1800);
            h.runAfterDelay(5, () -> {
                h.assertTrue(status(m) == 0, "iron ore with energy: " + status(m));
                items.setStackInSlot(2, new ItemStack(Items.DIAMOND, 64));
                h.assertTrue(status(m) == 3, "output blocked: " + status(m));
                items.setStackInSlot(2, ItemStack.EMPTY);
                h.setBlock(at.east(), Blocks.REDSTONE_BLOCK);
                h.runAfterDelay(3, () -> {
                    h.assertTrue(status(m) == 5, "redstone: " + status(m));
                    h.succeed();
                });
            });
        });
    }

    /** The menus send status, energy per operation and work index; the slots sit where the screen draws them. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void maceratorMenusFeedTheScreen(GameTestHelper h) {
        var basic = place(h, new BlockPos(1, 2, 1), "producer/block_powder_elc");
        var cyclone = place(h, new BlockPos(3, 2, 1), "producer/block_powder_advanced_elc");
        ((IItemHandlerModifiable) call(basic, "getItemHandler")).setStackInSlot(0, new ItemStack(Items.GRAVEL, 4));
        ((IItemHandlerModifiable) call(cyclone, "getItemHandler")).setStackInSlot(0, new ItemStack(Items.IRON_ORE, 4));
        ServerPlayer player = Round33GameTests.survivalPlayer(h);
        h.runAfterDelay(3, () -> {
            AbstractContainerMenu a = (AbstractContainerMenu) call(basic, "createMenu", 1, player.getInventory(), player);
            AbstractContainerMenu b = (AbstractContainerMenu) call(cyclone, "createMenu", 2, player.getInventory(), player);
            a.broadcastChanges();
            b.broadcastChanges();
            ContainerData da = (ContainerData) call(a, "getData"), db = (ContainerData) call(b, "getData");
            h.assertTrue(da.get(5) == 4 && da.get(6) == 1191 && da.get(7) == 252, "macerator data: status " + da.get(5) + ", EU " + da.get(6) + ", Wi " + da.get(7));
            h.assertTrue(db.get(8) == 4 && db.get(9) == 480 && db.get(10) == 127, "cyclone data: status " + db.get(8) + ", EU " + db.get(9) + ", Wi " + db.get(10));
            int[][] hoppers = {{73, 19}, {71, 19}};   // 0.1.11.180: in the mill's hopper (MergedSlots.Mill, .Cyclone), 176 wide; 0.1.11.176 had 35,39
            for (int m = 0; m < 2; m++) {
                AbstractContainerMenu menu = m == 0 ? a : b;
                var input = menu.getSlot(0);
                h.assertTrue(input.x == hoppers[m][0] && input.y == hoppers[m][1], "input slot at " + input.x + "," + input.y);
                boolean inventoryAt = false;
                for (var s : menu.slots) if (s.container == player.getInventory() && s.getContainerSlot() == 9) inventoryAt = s.x == 8 && s.y == 160;   // 0.1.11.120: under the readout window; 0.1.11.180: the vanilla x 8
                h.assertTrue(inventoryAt, "the player inventory does not start at 8,160");
            }
            h.assertTrue(!(boolean) call(cyclone, "isHoldingSpeed"), "hold starts off");
            h.assertTrue(b.clickMenuButton(player, 0) && (boolean) call(cyclone, "isHoldingSpeed"), "the hold button switches hold speed on");
            b.broadcastChanges();
            h.assertTrue(db.get(6) == 1, "hold on is synced");
            Round33GameTests.remove(player);
            h.succeed();
        });
    }
}
