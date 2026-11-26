package example.sicore;

import static example.sicore.Round33GameTests.call;
import static example.sicore.Round33GameTests.common;

import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.219: the replicator pays 0.1 mB of UU and its EU per step (an item dearer than the tank is copied bit by bit), and
 * a paid copy is never lost: the pattern away (the crystal taken out, a wrenched machine placed without it) pauses it; a
 * crystal with another pattern, a second Stop or an item that lost its price end it with the paid UU back in the balance; a
 * new price keeps what was paid (it used to hold the machine); UU piped in wakes a replicator that slept waiting for it.
 * The copies here are diamonds (24.1 mB, 241 steps of one tick).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111219GameTests {
    private Round111219GameTests() {}

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static int status(String name) {
        try {
            return common("blockentity.producer.mio_icif_replicator_elc").getField(name).getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private static ItemStack crystal(GameTestHelper h, ItemStack item) {
        var quote = UuQuoteBook.quote(h.getLevel().getServer(), item);
        h.assertTrue(quote != null, item + " has no UU price");
        ItemStack crystal = new ItemStack(BuiltInRegistries.ITEM.get(si("resource/item_crystal_memory")));
        h.assertTrue((Boolean) call(crystal.getItem(), "tryStoreData", crystal, item, quote.buckets(), 844_800L), "the crystal refused " + item);
        return crystal;
    }

    private static IItemHandlerModifiable slots(BlockEntity replicator) { return (IItemHandlerModifiable) call(replicator, "getItemHandler"); }
    private static FluidTank tank(BlockEntity replicator) { return (FluidTank) call(replicator, "getUuMatterTank"); }
    private static double processed(BlockEntity replicator) { return (Double) call(replicator, "getProcessedUuBuckets"); }
    private static double credit(BlockEntity replicator) { return (Double) call(replicator, "getUuCreditBuckets"); }
    private static double price(BlockEntity replicator) { return (Double) call(replicator, "getCurrentUuCostBuckets"); }
    private static long completed(BlockEntity replicator) { return (Long) call(replicator, "getTotalProcessed"); }
    private static int statusOf(BlockEntity replicator) { return (Integer) call(replicator, "statusCode"); }
    /** All the UU of the machine: balance, tank and the paid part of the copy (a step moves UU from the first two to the last). */
    private static double uu(BlockEntity replicator) { return credit(replicator) + tank(replicator).getFluidAmount() / 1000.0 + processed(replicator); }

    private static void fill(BlockEntity replicator, int mb) {
        tank(replicator).fill(new FluidStack(BuiltInRegistries.FLUID.get(si("uumatter")), mb), IFluidHandler.FluidAction.EXECUTE);
    }

    /** A replicator with energy, {@code uuMb} of UU-matter and a crystal of {@code item}, set to copy once. */
    private static BlockEntity replicator(GameTestHelper h, BlockPos pos, ItemStack item, int uuMb) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(si("producer/block_replicator_elc")));
        BlockEntity replicator = h.getBlockEntity(pos);
        call(call(replicator, "getEnergyStorageInternal"), "setStored", 2_000_000L);
        fill(replicator, uuMb);
        slots(replicator).setStackInSlot(2, crystal(h, item));
        call(replicator, "generateOnce");
        return replicator;
    }

    @GameTest(template = "empty", batch = "round111219_replicator", timeoutTicks = 600)
    public static void aCopyDearerThanTheTankIsPaidStepByStep(GameTestHelper h) {
        // 5 mB in the tank for a 24.1 mB diamond: it copies until the UU runs out, waits, and finishes with more
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1), new ItemStack(Items.DIAMOND), 5);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(statusOf(replicator) == status("STATUS_NO_UU"), "not waiting for UU: " + statusOf(replicator)))
            .thenExecute(() -> {
                h.assertTrue(processed(replicator) > 0.0045 && processed(replicator) < 0.0052 && completed(replicator) == 0,
                    "the 5 mB were not paid in steps: " + processed(replicator));
                fill(replicator, 25);
            })
            .thenWaitUntil(() -> h.assertTrue(completed(replicator) == 1, "the diamond was not finished with more UU"))
            .thenSucceed();
    }

    @GameTest(template = "empty", batch = "round111219_replicator", timeoutTicks = 200)
    public static void anotherCrystalEndsTheCopyWithoutLosingUu(GameTestHelper h) {
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1), new ItemStack(Items.DIAMOND), 32_000);
        double[] before = {0};
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) >= 0.002, "the diamond copy did not get going"))
            .thenExecute(() -> {
                before[0] = uu(replicator);
                slots(replicator).setStackInSlot(2, crystal(h, new ItemStack(Items.GOLD_INGOT)));
            })
            .thenIdle(3)
            .thenExecute(() -> {
                double gold = UuQuoteBook.quote(h.getLevel().getServer(), new ItemStack(Items.GOLD_INGOT)).buckets();
                h.assertTrue(price(replicator) == gold, "the gold copy did not start: price " + price(replicator));
                h.assertTrue(Math.abs(uu(replicator) - before[0]) < 1e-9, "UU lost by the swap: " + before[0] + " -> " + uu(replicator));
            })
            .thenSucceed();
    }

    @GameTest(template = "empty", batch = "round111219_replicator", timeoutTicks = 200)
    public static void stopPausesAndSingleGoesOn(GameTestHelper h) {
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1), new ItemStack(Items.DIAMOND), 32_000);
        double[] paid = {0};
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) >= 0.002, "the diamond copy did not get going"))
            .thenExecute(() -> {
                call(replicator, "stopGeneration");
                paid[0] = processed(replicator);
            })
            .thenIdle(5)
            .thenExecute(() -> {
                h.assertTrue(processed(replicator) == paid[0] && statusOf(replicator) == status("STATUS_PAUSED"),
                    "Stop did not pause: " + paid[0] + " -> " + processed(replicator) + ", status " + statusOf(replicator));
                call(replicator, "generateOnce");
            })
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) > paid[0] + 0.0005, "Single did not go on with the paused copy"))
            .thenSucceed();
    }

    @GameTest(template = "empty", batch = "round111219_replicator", timeoutTicks = 200)
    public static void aNewPriceKeepsWhatWasPaid(GameTestHelper h) {
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1), new ItemStack(Items.DIAMOND), 32_000);
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) >= 0.002, "the diamond copy did not get going"))
            .thenExecute(() -> {
                call(replicator, "stopGeneration");   // paused: no step runs between the calls below
                double paid = processed(replicator), price = price(replicator);
                long generation = UuQuoteBook.generation(h.getLevel().getServer());
                // dearer: the copy goes on from what it paid (until 0.1.11.218 any change held the machine)
                call(replicator, "applyQuote", new UuQuoteBook.Quote(price * 2, generation));
                h.assertTrue(processed(replicator) == paid && price(replicator) == price * 2 && !(Boolean) call(replicator, "hasHeldReplicationData"),
                    "a dearer price lost the paid UU or held the machine");
                // cheaper than what was paid: the copy is paid in full, the excess goes back to the balance
                double balance = credit(replicator);
                call(replicator, "applyQuote", new UuQuoteBook.Quote(paid / 2, generation));
                h.assertTrue(processed(replicator) == paid / 2 && Math.abs(credit(replicator) - balance - paid / 2) < 1e-12,
                    "a cheaper price did not refund the excess: " + processed(replicator) + ", balance " + balance + " -> " + credit(replicator));
                // no price any more: the copy ends, the paid UU goes back
                double balance2 = credit(replicator);
                call(replicator, "applyQuote", (Object) null);
                h.assertTrue(processed(replicator) == 0 && price(replicator) == 0 && Math.abs(credit(replicator) - balance2 - paid / 2) < 1e-12,
                    "an item without a price did not end the copy with a refund");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty", batch = "round111219_replicator", timeoutTicks = 300)
    public static void aWrenchedReplicatorKeepsItsCopyAndWaitsForItsCrystal(GameTestHelper h) {
        BlockEntity replicator = replicator(h, new BlockPos(1, 1, 1), new ItemStack(Items.DIAMOND), 32_000);
        BlockEntity[] placed = {null};
        double[] paid = {0};
        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(processed(replicator) >= 0.002, "the diamond copy did not get going"))
            .thenExecute(() -> {
                var registries = h.getLevel().registryAccess();
                call(replicator, "stopGeneration");
                call(replicator, "generateOnce");   // still a running Single copy, now steady
                paid[0] = processed(replicator);
                // the item a wrench makes (the inventory spilled), placed again elsewhere
                CompoundTag data = (CompoundTag) call(common("block.MachineItemData"), "capture", replicator, registries, false);
                h.setBlock(new BlockPos(3, 1, 1), BuiltInRegistries.BLOCK.get(si("producer/block_replicator_elc")));
                placed[0] = h.getBlockEntity(new BlockPos(3, 1, 1));
                placed[0].loadCustomOnly(data, registries);
                call(call(placed[0], "getEnergyStorageInternal"), "setStored", 2_000_000L);
            })
            .thenIdle(5)
            .thenExecute(() -> {
                // until 0.1.11.218 a copy loaded without its crystal went on copying
                h.assertTrue(processed(placed[0]) >= paid[0] - 1e-12 && processed(placed[0]) <= paid[0] + 0.0001 + 1e-12
                    && statusOf(placed[0]) == status("STATUS_PATTERN_PAUSED") && !(Boolean) call(placed[0], "isReplicating"),
                    "the placed copy: paid " + processed(placed[0]) + " (was " + paid[0] + "), status " + statusOf(placed[0]));
                slots(placed[0]).setStackInSlot(2, crystal(h, new ItemStack(Items.DIAMOND)));
            })
            .thenWaitUntil(() -> h.assertTrue(processed(placed[0]) > paid[0] + 0.0005, "the copy did not go on with its crystal"))
            .thenSucceed();
    }

    @GameTest(template = "empty", batch = "round111219_replicator", timeoutTicks = 40)
    public static void uuPipedInWakesASleepingReplicator(GameTestHelper h) throws ReflectiveOperationException {
        h.setBlock(new BlockPos(1, 1, 1), BuiltInRegistries.BLOCK.get(si("producer/block_replicator_elc")));
        BlockEntity replicator = h.getBlockEntity(new BlockPos(1, 1, 1));
        var asleep = Class.forName("com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity").getDeclaredField("asleep");
        asleep.setAccessible(true);
        asleep.setBoolean(replicator, true);
        var port = (IFluidHandler) call(replicator, "getFluidHandlerCapability", (Object) null);
        int filled = port.fill(new FluidStack(BuiltInRegistries.FLUID.get(si("uumatter")), 1000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 1000 && !asleep.getBoolean(replicator), "UU piped in did not wake the replicator (filled " + filled + ")");
        h.succeed();
    }
}
