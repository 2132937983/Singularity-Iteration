package example.sicore;

import static example.sicore.Round33GameTests.call;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.11.204: the machines that took the wireless upgrade but drew nothing from the TESU wireless field are charged now: the
 * UU-Matter Fabricator (its own upgrade count and tick skipped it), the Oil Refinery (its own tick skipped it) and the Chunk
 * Loader (not a processing machine; nothing drew for it). A formed TESU with a wireless output port and the three machines,
 * each with a wireless upgrade, inside the port's field.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111204GameTests {
    private Round111204GameTests() {}

    private static final String[] MACHINES = {"producer/block_matter_elc", "producer/block_oil_refinery_elc", "producer/block_chunk_loader"};

    private static long stored(BlockEntity be) { return (Long) call(call(be, "getEnergyStorageInternal"), "getAmount"); }

    /** Puts a wireless upgrade in the first slot of {@code be} that takes it. */
    private static void upgrade(BlockEntity be) {
        IItemHandler items = (IItemHandler) ArmoryServiceGameTests.field(be, "itemHandler");
        ItemStack wireless = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "upgrade/wireless_upgrade")));
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.isItemValid(i, wireless) || !items.insertItem(i, wireless.copy(), true).isEmpty()) continue;
            items.insertItem(i, wireless.copy(), false);
            return;
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("no slot of " + be + " takes the wireless upgrade");
    }

    @GameTest(template = "reactor_loop", batch = "round111204_wireless", timeoutTicks = 300)
    public static void wirelessUpgradeChargesTheMachinesItSkipped(GameTestHelper h) {
        BlockPos controllerAt = Round111157GameTests.controller(h);
        Round111157GameTests.build(h, controllerAt, Direction.NORTH, 0, 0, 1);
        BlockEntity controller = Round111157GameTests.be(h, controllerAt);
        BlockPos corner = Round111157GameTests.corner(h);
        BlockEntity[] machines = new BlockEntity[MACHINES.length];
        for (int i = 0; i < MACHINES.length; i++) {
            BlockPos at = corner.offset(11, 1 + i, 1);
            h.getLevel().setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", MACHINES[i])).defaultBlockState(), 3);
            machines[i] = Round111157GameTests.be(h, at);
            upgrade(machines[i]);
        }
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue((Boolean) call(controller, "isStructureComplete") && (Integer) call(controller, "wirelessCount") == 1,
                "the TESU with a wireless port did not form"))
            .thenExecute(() -> call(call(controller, "getEnergyStorageInternal"), "setEnergy", 1_000_000_000L))
            .thenWaitUntil(() -> {
                // the fabricator spends what it gets in the same tick: a full buffer (1,000,000 EU) is 1 mB of UU-Matter at once,
                // so its storage and its work are back at 0 after the tick; the matter it made shows the charge
                h.assertTrue((Integer) call(machines[0], "getUuMatterAmount") > 0, "the UU-Matter Fabricator got nothing from the wireless field");
                h.assertTrue(stored(machines[1]) > 0, "the Oil Refinery got nothing from the wireless field");
                h.assertTrue(stored(machines[2]) > 0, "the Chunk Loader got nothing from the wireless field");
            })
            .thenExecute(() -> h.assertTrue((Long) call(call(controller, "getEnergyStorageInternal"), "getAmount") < 1_000_000_000L,
                "the TESU did not pay for the charge"))
            .thenSucceed();
    }
}
