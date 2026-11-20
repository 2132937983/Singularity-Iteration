package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.109 (processing overhaul M2): the cyclone macerator is a closed-circuit mill. One operation costs 80 % of the
 * macerator's for the material (iron ore 480 EU, gravel 952 EU) at 12 EU/t; the rotor speeds up 10 % a second while it
 * works and coasts down 5 % a second; below full speed it runs that share of the ticks, so the energy per operation
 * stays the same; hold speed keeps the rotor turning for 1 EU/t; the rotor state is saved. (The mod's common classes by
 * reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111109GameTests {
    private Round111109GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static double speed(BlockEntity be) { return (double) call(be, "getRotorSpeed"); }

    private static AbstractProcessingMachineBlockEntity place(GameTestHelper h, BlockPos at) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_powder_advanced_elc")));
        return (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
    }

    /** Closed circuit: 80 % of the macerator's energy, 12 EU/t; the buffer does not change with the input. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void closedCircuitCost(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1));
        IItemHandlerModifiable items = (IItemHandlerModifiable) call(m, "getItemHandler");
        items.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 4));
        h.runAfterDelay(3, () -> {
            Object iron = call(m, "processCost");
            long capIron = m.getEffectiveCapacity();
            h.assertTrue((long) call(iron, "energy") == 480 && (long) call(iron, "power") == 12 && m.getMaxProgress() == 40,
                "iron ore " + iron + ", " + m.getMaxProgress() + " t");
            items.setStackInSlot(0, new ItemStack(Items.GRAVEL, 4));
            h.runAfterDelay(3, () -> {
                Object gravel = call(m, "processCost");
                h.assertTrue((long) call(gravel, "energy") == 952 && m.getMaxProgress() == 80, "gravel " + gravel + ", " + m.getMaxProgress() + " t");
                h.assertTrue(m.getEffectiveCapacity() == capIron && capIron == 750 + 960, "buffer " + capIron + " / " + m.getEffectiveCapacity());
                h.succeed();
            });
        });
    }

    /**
     * Spin-up from rest to full speed in 200 ticks (half speed after 100), then at full speed one iron ore every 40
     * ticks for 480 EU each; with no input the rotor coasts down 5 % a second.
     */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void rotorSpinUpRunAndCoast(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1));
        IItemHandlerModifiable items = (IItemHandlerModifiable) call(m, "getItemHandler");
        items.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 64));
        long full = m.getEffectiveCapacity();
        long[] used = {0};
        h.onEachTick(() -> {   // keep the buffer full and count what the mill takes
            long stored = m.getEnergyStorageInternal().getAmount();
            long cap = m.getEffectiveCapacity();
            if (stored < cap) { used[0] += cap - stored; m.getEnergyStorageInternal().setStored(cap); }
        });
        m.getEnergyStorageInternal().setStored(full);
        h.runAfterDelay(101, () -> {
            double half = speed(m);
            h.assertTrue(Math.abs(half - 0.5) < 0.03, "speed after 100 ticks " + half);
            h.runAfterDelay(110, () -> {
                h.assertTrue(speed(m) == 1.0, "speed after 210 ticks " + speed(m));
                int before = items.getStackInSlot(1).getCount();
                long usedBefore = used[0];
                h.runAfterDelay(400, () -> {
                    int ops = (items.getStackInSlot(1).getCount() - before) / 2;
                    long eu = used[0] - usedBefore;
                    h.assertTrue(ops >= 9 && ops <= 11, "operations in 400 ticks at full speed: " + ops);
                    h.assertTrue(Math.abs(eu - ops * 480L) <= 480, "energy " + eu + " EU for " + ops + " operations (480 each)");
                    items.setStackInSlot(0, ItemStack.EMPTY);
                    h.runAfterDelay(101, () -> {
                        double coasting = speed(m);
                        h.assertTrue(Math.abs(coasting - 0.75) < 0.03, "speed 100 ticks after the input ran out " + coasting);
                        h.succeed();
                    });
                });
            });
        });
    }

    /** Hold speed keeps the rotor at speed while idle for 1 EU/t; the rotor state is saved. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void holdSpeedAndSave(GameTestHelper h) {
        var m = place(h, new BlockPos(1, 2, 1));
        CompoundTag tag = new CompoundTag();
        tag.putDouble("cyclone_speed", 0.6);
        tag.putBoolean("cyclone_hold", true);
        m.loadAdditional(tag, h.getLevel().registryAccess());
        h.assertTrue((boolean) call(m, "isHoldingSpeed") && Math.abs(speed(m) - 0.6) < 1e-9, "loaded rotor state");
        m.getEnergyStorageInternal().setStored(500);
        h.runAfterDelay(100, () -> {
            long left = m.getEnergyStorageInternal().getAmount();
            h.assertTrue(Math.abs(speed(m) - 0.6) < 1e-9, "held speed " + speed(m));
            h.assertTrue(left >= 395 && left <= 405, "hold used " + (500 - left) + " EU in 100 ticks");
            CompoundTag saved = m.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(Math.abs(saved.getDouble("cyclone_speed") - 0.6) < 1e-9 && saved.getBoolean("cyclone_hold"), "saved " + saved);
            call(m, "toggleHoldSpeed");
            h.runAfterDelay(41, () -> {
                h.assertTrue(Math.abs(speed(m) - 0.5) < 0.01, "coasting without hold " + speed(m));
                h.succeed();
            });
        });
    }
}
