package example.sicore;

import com.miophas.singularity_iteration.core.api.reactor.IReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.38. Phase A: depleted rods stay in their slot (a reactor walled in by chambers lost the depleted quad rod
 * it threw out at its own position); a reactor chamber is a fluid port of its reactor. Internals are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1138GameTests {
    private Round1138GameTests() {}

    private static final BlockPos REACTOR = new BlockPos(2, 2, 2);

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    /** A fuel rod with {@code remaining} of {@code max} cycles left. */
    private static ItemStack rod(GameTestHelper h, String path, int remaining, int max) {
        CompoundTag tag = (CompoundTag) item(path).save(h.getLevel().registryAccess());
        CompoundTag components = tag.getCompound("components");
        CompoundTag durability = new CompoundTag();
        durability.putInt("remaining_uses", remaining); durability.putInt("max_uses", max); durability.putInt("tick_counter", 0);
        components.put("mio_icif:fuel_rod_durability", durability); tag.put("components", components);
        return ItemStack.parse(h.getLevel().registryAccess(), tag).orElseThrow();
    }

    // ------------------------------------------------------------------ A2: depleted rods stay in their slot

    /**
     * A quad uranium rod one cycle from empty in a reactor with a chamber on all six faces (the usual quad build): the depleted
     * quad rod stays in its slot. Before, it was thrown out at the reactor's own position, inside the walled-in reactor block.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 80)
    public static void depletedQuadRodStaysInAReactorWalledInByChambers(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        for (Direction side : Direction.values())
            h.setBlock(REACTOR.relative(side), BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_chamber")));
        h.setBlock(REACTOR.east(2), Blocks.REDSTONE_BLOCK);   // powers the east chamber, which enables the reactor
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        h.assertTrue(reactor.getItemHandler().insertItem(0, rod(h, "reactor/item_reactor_uranium_quad", 1, 20000), false).isEmpty(), "Fuel rejected");
        var depleted = BuiltInRegistries.ITEM.get(id("reactor/item_reactor_uranium_quad_deplete"));
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getAvailableColumns() == 9, "six chambers give 9 columns: " + reactor.getAvailableColumns());
            h.assertTrue(reactor.getItemHandler().getStackInSlot(0).is(depleted), "The quad rod did not burn out: " + reactor.getItemHandler().getStackInSlot(0));
            h.runAfterDelay(20, () -> {
                h.assertTrue(reactor.getItemHandler().getStackInSlot(0).is(depleted), "The depleted quad rod must stay in its slot");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ A5: reactor chamber fluid port

    /** Water pumped into a reactor chamber reaches its reactor's liquid cooling (the chamber had no fluid port before). */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void reactorChamberIsAFluidPortOfItsReactor(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        h.setBlock(REACTOR.north(), BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_chamber")));
        h.runAfterDelay(2, () -> {
            IFluidHandler chamber = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(REACTOR.north()), Direction.NORTH);
            h.assertTrue(chamber != null, "The reactor chamber has no fluid handler");
            h.assertTrue(chamber.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "The chamber refused coolant");
            IFluidHandler core = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(REACTOR), null);
            h.assertTrue(core != null && core.getFluidInTank(0).getAmount() == 1000, "The coolant did not reach the reactor");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ A3: reactor output above 8192 EU/t, reload, wireless

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Object field(Object target, String name) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
                try { var f = k.getDeclaredField(name); f.setAccessible(true); f.set(target, value); return; }
                catch (NoSuchFieldException next) { /* superclass */ }
            }
            throw new AssertionError("no field " + name);
        } catch (IllegalAccessException e) { throw new AssertionError(name, e); }
    }

    private static long stored(Object block) { return (Long) call(call(block, "getEnergyStorageInternal"), "getAmount"); }

    /**
     * A reactor producing 20,000 EU/t delivers more than 8192 EU in one tick to a sink that takes it (a GESU input module): it
     * sends several IV packets per tick (up to 64) instead of one, so the output above 8192 EU/t is no longer lost.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 80)
    public static void reactorDeliversMoreThan8192EuPerTick(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        h.setBlock(REACTOR.east(), BuiltInRegistries.BLOCK.get(id("wiring/block_gesu_input_iv")));
        h.setBlock(REACTOR.above(), Blocks.REDSTONE_BLOCK);
        IReactorController reactor = (IReactorController) h.getBlockEntity(REACTOR);
        h.assertTrue((Integer) call(reactor, "getPacketCount") == 64, "the reactor must send up to 64 packets");
        h.assertTrue(reactor.getItemHandler().insertItem(10, item("reactor/item_reactor_uranium_simple"), false).isEmpty(), "Fuel rejected");
        var sink = h.getBlockEntity(REACTOR.east());
        h.runAfterDelay(25, () -> {
            h.assertTrue(reactor.getCurrentEnergyGeneration() == 5, "the reactor did not run a cycle: " + reactor.getCurrentEnergyGeneration());
            Object rate = field(reactor, "rate");
            try {
                setField(reactor, "rate", rate.getClass().getConstructor(long.class, long.class).newInstance(20_000L, 0L));
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            long before = stored(sink);
            h.runAfterDelay(2, () -> {
                long perTick = (stored(sink) - before) / 2;
                h.assertTrue(perTick > 8192, "the reactor delivered only " + perTick + " EU/t");
                h.succeed();
            });
        });
    }

    /**
     * A saved rate above 8192 EU/t loads (before, the reactor was put on hold for good after a reload), and a reactor already held
     * for that reason in an older save is freed.
     */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void reactorReloadsAFastRateAndFreesAHeldReactor(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("generator/block_nuclear_reactor_generator")));
        var reactor = h.getBlockEntity(REACTOR);
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = reactor.saveWithoutMetadata(registries);
        CompoundTag own = tag.getCompound("scex_reactor");
        h.assertTrue(own.contains("rate"), "no saved reactor state: " + tag.getAllKeys());
        own.putLong("rate", 20_000L);
        reactor.loadWithComponents(tag, registries);
        h.assertTrue(((CompoundTag) field(reactor, "hold")).isEmpty(), "a 20,000 EU/t reactor was held after a reload: " + field(reactor, "hold"));
        h.assertTrue((Long) call(field(reactor, "rate"), "whole") == 20_000L, "the saved rate was not kept");

        CompoundTag held = own.copy();   // an older save: the hold written by the 8192 check
        CompoundTag hold = new CompoundTag();
        hold.put("invalid_rate", held.copy());
        own.put("hold", hold);
        reactor.loadWithComponents(tag, registries);
        h.assertTrue(((CompoundTag) field(reactor, "hold")).isEmpty(), "a reactor held by the old check was not freed: " + field(reactor, "hold"));
        h.succeed();
    }

    /** The wireless node takes the configured limit (default 64 IV packets per tick, it was 32768). */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void wirelessNodeUsesTheConfiguredLimit(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("wiring/block_wireless_power_transmission_node")));
        var node = h.getBlockEntity(REACTOR);
        long limit = (Long) field(node, "transferSpeed");
        h.assertTrue(limit == 524_288L, "default wireless limit: " + limit);
        h.assertTrue((Long) call(call(node, "getEnergyStorageInternal"), "getCapacity") == limit * 6, "the buffer holds 6 ticks");
        h.succeed();
    }

    // ------------------------------------------------------------------ A1: Energy Converter at EV

    /** The Energy Converter takes EV packets (2048 EU) without exploding: its tier was HV (512 EU) while it took 2048 EU/t. */
    @GameTest(batch = "round1138_reactor", template = "empty", timeoutTicks = 20)
    public static void energyConverterIsEv(GameTestHelper h) {
        h.setBlock(REACTOR, BuiltInRegistries.BLOCK.get(id("energy_converter/energy_converter")));
        var converter = h.getBlockEntity(REACTOR);
        long rating = (Long) call(call(converter, "getEffectiveCableTier"), "getPowerRating");
        h.assertTrue(rating == 2048, "the converter's voltage limit is " + rating);
        h.succeed();
    }
}
