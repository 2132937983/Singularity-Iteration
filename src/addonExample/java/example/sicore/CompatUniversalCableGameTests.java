package example.sicore;

import com.miophas.singularity_iteration.core.runtime.energy.ForeignEnergyCables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 0.1.10.39: universal cables (FE) against the real Mekanism and AE2 jars on the compat GameTest server. Only registry ids,
 * NeoForge capabilities and reflection are used (the test sources may not link against the other mods). Each test registers
 * only when its mods are loaded. Energy: SI EU from the block entity's storage; Mekanism through its FE capability; AE2 through
 * the energy cell's {@code getAECurrentPower}. Mekanism converts J and FE itself (2.5 J per FE by default); SI is 4 FE per EU.
 */
@GameTestHolder(CompatUniversalCableGameTests.NAMESPACE)
public final class CompatUniversalCableGameTests {
    /** Its own GameTest namespace: the universalGameTestServer run (with Mekanism) runs only these tests. */
    static final String NAMESPACE = "si_universal";
    private CompatUniversalCableGameTests() {}

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CompatUniversalCableGameTests.class);

    static final String MEK = "mekanism", MEK_GEN = "mekanismgenerators", AE2 = "ae2";

    @GameTestGenerator
    public static Collection<TestFunction> universalCableTests() {
        List<TestFunction> tests = new ArrayList<>();
        tests.add(test("probe", "probeEnergyFacesOfTheOtherMods", 40, CompatUniversalCableGameTests::probeEnergyFacesOfTheOtherMods));
        if (ModList.get().isLoaded(MEK)) {
            tests.add(test("mek_fe_in", "mekanismEnergyCubePushesFeIntoSiAtVoltageZero", 160, CompatUniversalCableGameTests::mekanismEnergyCubePushesFeIntoSiAtVoltageZero));
            tests.add(test("mek_fe_out", "siNetworkRunsAMekanismEnrichmentChamber", 400, CompatUniversalCableGameTests::siNetworkRunsAMekanismEnrichmentChamber));
            tests.add(test("mek_loop", "siCableRefusesAMekanismUniversalCable", 120, CompatUniversalCableGameTests::siCableRefusesAMekanismUniversalCable));
        }
        if (ModList.get().isLoaded(MEK_GEN))
            tests.add(test("mek_gen_in", "mekanismHeatGeneratorFeedsSiCables", 400, CompatUniversalCableGameTests::mekanismHeatGeneratorFeedsSiCables));
        if (ModList.get().isLoaded(AE2)) {
            tests.add(test("ae2_fe_out", "siNetworkChargesAnAe2Network", 160, CompatUniversalCableGameTests::siNetworkChargesAnAe2Network));
            tests.add(test("ae2_pad", "chargePadChargesAnAe2Terminal", 40, CompatUniversalCableGameTests::chargePadChargesAnAe2Terminal));
            tests.add(test("ae2_battery", "chargingBatteryChargesAnAe2Terminal", 40, CompatUniversalCableGameTests::chargingBatteryChargesAnAe2Terminal));
        }
        return tests;
    }

    private static TestFunction test(String batch, String name, int ticks, Consumer<GameTestHelper> body) {
        return new TestFunction("universal_" + batch, "compatuniversalcablegametests." + name.toLowerCase(java.util.Locale.ROOT),
            NAMESPACE + ":empty", ticks, 0, true, body);   // the same empty 5 x 4 x 5 template, in this namespace
    }

    // ------------------------------------------------------------------ helpers

    private static Block block(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (block == null || BuiltInRegistries.BLOCK.getKey(block).getPath().equals("air")) throw new IllegalStateException("no block " + id);
        return block;
    }

    private static BlockState facing(String id, Direction side) {
        BlockState state = block(id).defaultBlockState();
        return state.getBlock().getStateDefinition().getProperty("facing") instanceof DirectionProperty facing
            && facing.getPossibleValues().contains(side) ? state.setValue(facing, side) : state;
    }

    /**
     * Places a block as an item would: setBlock, then the block's setPlacedBy (without a placer). Mekanism may set up its side
     * configuration there; placed by setBlock alone its machines offered energy and items on no face.
     */
    private static void place(GameTestHelper h, BlockPos at, BlockState state) {
        h.setBlock(at, state);
        BlockPos abs = h.absolutePos(at);
        BlockState placed = h.getLevel().getBlockState(abs);
        ItemStack item = new ItemStack(placed.getBlock().asItem());
        // like BlockItem placement: the item's data components go into the block entity (Mekanism keeps its default side
        // configuration there; without them every energy and item face was NONE)
        var entity = h.getLevel().getBlockEntity(abs);
        if (entity != null) { entity.applyComponentsFromItemStack(item); entity.setChanged(); }
        // no placer: Mekanism looks a placing player up in the server's profile cache, which the GameTest server lacks
        placed.getBlock().setPlacedBy(h.getLevel(), abs, placed, null, item);
        h.getLevel().invalidateCapabilities(abs);
    }

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /** SI EU stored in the block entity at {@code at}. */
    private static long siEu(GameTestHelper h, BlockPos at) {
        return (Long) call(call(h.getBlockEntity(at), "getEnergyStorageInternal"), "getAmount");
    }

    private static void setSiEu(GameTestHelper h, BlockPos at, long eu) {
        call(call(h.getBlockEntity(at), "getEnergyStorageInternal"), "setStored", eu);
    }

    private static IEnergyStorage fe(GameTestHelper h, BlockPos at, Direction side) {
        return h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(at), side);
    }

    /** FE stored, read through any face that offers the FE capability. */
    private static int feStored(GameTestHelper h, BlockPos at) {
        for (Direction side : Direction.values()) {
            IEnergyStorage storage = fe(h, at, side);
            if (storage != null) return storage.getEnergyStored();
        }
        IEnergyStorage any = fe(h, at, null);
        return any == null ? -1 : any.getEnergyStored();
    }

    private static final String MFSU = "mio_icif:wiring/block_mfsu", LV_CABLE = "mio_icif:wiring/cable/block_tin_cable_1",
        SC_CABLE = "mio_icif:wiring/cable/block_superconducting_cable";

    /** "N[r e] E[-] ..." for each face (and null): FE capability present, canReceive, canExtract, stored. */
    private static String describe(GameTestHelper h, BlockPos at) {
        StringBuilder out = new StringBuilder(BuiltInRegistries.BLOCK.getKey(h.getBlockState(at).getBlock()) + " " + h.getBlockState(at).getValues() + ":");
        for (Direction side : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN, null}) {
            IEnergyStorage storage = fe(h, at, side);
            out.append(' ').append(side == null ? "null" : side.getName().substring(0, 1).toUpperCase())
                .append(storage == null ? "[-]" : "[" + (storage.canReceive() ? "r" : "") + (storage.canExtract() ? "e" : "") + " " + storage.getEnergyStored() + "]");
        }
        return out.toString();
    }

    /** The first face whose FE capability takes energy, or null. */
    private static Direction inputFace(GameTestHelper h, BlockPos at) {
        for (Direction side : Direction.values()) { IEnergyStorage s = fe(h, at, side); if (s != null && s.canReceive()) return side; }
        return null;
    }

    /** Logs the energy faces of the other mods' blocks and the energy capabilities registered (diagnostics; always passes). */
    private static void probeEnergyFacesOfTheOtherMods(GameTestHelper h) {
        StringBuilder caps = new StringBuilder();
        for (var cap : net.neoforged.neoforge.capabilities.BlockCapability.getAll())
            if (cap.name().getPath().contains("energy") || cap.name().getNamespace().equals(MEK)) caps.append(cap.name()).append(' ');
        LOG.info("[universal] energy block capabilities: {}", caps);
        List<String> ids = new ArrayList<>();
        if (ModList.get().isLoaded(MEK)) ids.addAll(List.of("mekanism:basic_energy_cube", "mekanism:enrichment_chamber", "mekanism:basic_universal_cable"));
        if (ModList.get().isLoaded(MEK_GEN)) ids.add("mekanismgenerators:heat_generator");
        if (ModList.get().isLoaded(AE2)) ids.addAll(List.of("ae2:energy_acceptor", "ae2:energy_cell"));
        int x = 0;
        for (String id : ids) { place(h, new BlockPos(x % 5, 1, 2 * (x / 5)), facing(id, Direction.EAST)); x += 2; }
        h.runAfterDelay(10, () -> {
            int i = 0;
            for (String id : ids) { LOG.info("[universal] {}", describe(h, new BlockPos(i % 5, 1, 2 * (i / 5)))); i += 2; }
            if (ModList.get().isLoaded(MEK)) for (int at : new int[]{0, 2}) LOG.info("[universal] {}", mekanismConfig(h, new BlockPos(at, 1, 0)));
            h.succeed();
        });
    }

    /** Mekanism's side configuration of the energy transmission at {@code at}: data type per relative side, disabled faces. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String mekanismConfig(GameTestHelper h, BlockPos at) {
        try {
            Object be = h.getBlockEntity(at);
            ClassLoader loader = be.getClass().getClassLoader();
            Object config = call(be, "getConfig");
            Object energy = Enum.valueOf((Class) Class.forName("mekanism.common.lib.transmitter.TransmissionType", true, loader), "ENERGY");
            StringBuilder out = new StringBuilder(be.getClass().getSimpleName() + " energy:");
            for (Object side : Class.forName("mekanism.api.RelativeSide", true, loader).getEnumConstants())
                out.append(' ').append(side).append('=').append(call(config, "getDataType", energy, side));
            out.append(" | FE disabled on:");
            for (Direction d : Direction.values())
                if ((Boolean) call(config, "isCapabilityDisabled", Capabilities.EnergyStorage.BLOCK, d)) out.append(' ').append(d.getName());
            return out.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "config of " + at + " not readable: " + e;
        }
    }

    // ------------------------------------------------------------------ 1. Mekanism -> SI (FE in)

    /**
     * A Mekanism basic energy cube, filled through its FE capability, pushes FE out of its front into an SI low-voltage cable (32
     * EU). The FE enters at voltage 0, so the cable does not melt although the cube sends far more than 32 EU a tick, and the MFSU
     * behind it gains 1 EU per 4 FE the cube lost (less the cable loss).
     */
    private static void mekanismEnergyCubePushesFeIntoSiAtVoltageZero(GameTestHelper h) {
        BlockPos cube = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), storage = new BlockPos(3, 1, 2);
        place(h, cube, facing("mekanism:basic_energy_cube", Direction.EAST));   // its front (output) into the cable
        h.setBlock(cable, block(LV_CABLE));
        h.setBlock(storage, facing(MFSU, Direction.EAST));                        // output away from the cable
        h.runAfterDelay(10, () -> {
            Direction in = inputFace(h, cube);
            h.assertTrue(in != null, "the energy cube takes FE on no face: " + describe(h, cube));
            int filled = fe(h, cube, in).receiveEnergy(400_000, false);
            int cubeStart = feStored(h, cube);
            long siStart = siEu(h, storage);
            h.assertTrue(filled > 0, "the energy cube took no FE");
            h.runAfterDelay(100, () -> {
                int lostFe = cubeStart - feStored(h, cube);
                long gainedEu = siEu(h, storage) - siStart;
                h.assertTrue(h.getBlockState(cable).is(block(LV_CABLE)), "the low-voltage SI cable melted under Mekanism's FE");
                h.assertTrue(lostFe > 0 && gainedEu > 0, "no FE reached SI: cube lost " + lostFe + " FE, MFSU gained " + gainedEu + " EU; "
                    + describe(h, cube) + "; cable " + describe(h, cable));
                double ratio = lostFe / (double) gainedEu;
                h.assertTrue(ratio >= 4.0 && ratio <= 4.4, "FE per EU " + ratio + " (cube lost " + lostFe + " FE, MFSU gained " + gainedEu + " EU)");
                LOG.info("[universal] Mekanism energy cube -> SI: {} FE out, {} EU in, {} FE per EU", lostFe, gainedEu, ratio);
                h.succeed();
            });
        });
    }

    /**
     * A Mekanism Heat Generator burning lava from its tank pushes FE into SI cables on all its faces; the MFSU they lead to fills.
     */
    private static void mekanismHeatGeneratorFeedsSiCables(GameTestHelper h) {
        BlockPos generator = new BlockPos(2, 1, 2), storage = new BlockPos(4, 1, 2);
        place(h, generator, facing("mekanismgenerators:heat_generator", Direction.NORTH));
        // a ring of LV cables around its four sides and over it, all joined, into the MFSU
        for (BlockPos at : List.of(new BlockPos(2, 1, 1), new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(1, 1, 3), new BlockPos(2, 1, 3),
                new BlockPos(3, 1, 3), new BlockPos(3, 1, 2), new BlockPos(2, 2, 2), new BlockPos(3, 2, 2)))
            h.setBlock(at, block(LV_CABLE));
        h.setBlock(storage, facing(MFSU, Direction.EAST));
        h.runAfterDelay(10, () -> {
            int lava = 0;
            for (Direction side : Direction.values()) {
                IFluidHandler tank = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(generator), side);
                if (tank != null && (lava = tank.fill(new FluidStack(Fluids.LAVA, 24_000), IFluidHandler.FluidAction.EXECUTE)) > 0) break;
            }
            h.assertTrue(lava > 0, "the heat generator took no lava");
            final int lavaIn = lava;
            long siStart = siEu(h, storage);
            var engine = com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.current(h.getLevel().getServer());
            // the generator's FE reaches SI two ways: it pushes into the cables (inbox) and SI pulls from its output face
            var imported = com.miophas.singularity_iteration.core.runtime.PerfCounters.Kind.FE_IMPORTED;
            long feStart = engine.feInputTotal(), importStart = com.miophas.singularity_iteration.core.runtime.PerfCounters.total(imported);
            h.runAfterDelay(300, () -> {
                long gained = siEu(h, storage) - siStart, pushed = engine.feInputTotal() - feStart;
                long pulled = com.miophas.singularity_iteration.core.runtime.PerfCounters.total(imported) - importStart, feIn = pushed + pulled;
                h.assertTrue(gained > 0, "the heat generator's FE never reached SI (MFSU +" + gained + " EU, " + feIn + " FE in)");
                double ratio = feIn / (double) gained;
                h.assertTrue(ratio >= 3.95 && ratio <= 4.4, "FE per EU " + ratio + " (pushed " + pushed + " + pulled " + pulled + " FE, MFSU gained " + gained + " EU)");
                LOG.info("[universal] Mekanism heat generator -> SI: pushed {} FE + pulled {} FE, MFSU gained {} EU in 300 ticks, {} FE per EU ({} mB lava)",
                    pushed, pulled, gained, ratio, lavaIn);
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ 2. SI -> Mekanism (FE out)

    /** An SI MFSU powers a Mekanism Enrichment Chamber through an SI cable: the chamber fills with FE and enriches iron ore. */
    private static void siNetworkRunsAMekanismEnrichmentChamber(GameTestHelper h) {
        BlockPos storage = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), chamber = new BlockPos(3, 1, 2);
        h.setBlock(storage, facing(MFSU, Direction.EAST));   // output into the cable
        h.setBlock(cable, block(SC_CABLE));
        place(h, chamber, block("mekanism:enrichment_chamber").defaultBlockState());
        setSiEu(h, storage, 1_000_000L);
        h.runAfterDelay(10, () -> {
            boolean inserted = false;
            for (Direction side : Direction.values()) {
                IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(chamber), side);
                if (items == null) continue;
                for (int slot = 0; slot < items.getSlots() && !inserted; slot++)
                    inserted = items.insertItem(slot, new ItemStack(Items.IRON_ORE, 4), false).isEmpty();
                if (inserted) break;
            }
            h.assertTrue(inserted, "the enrichment chamber took no iron ore");
            long siStart = siEu(h, storage);
            h.runAfterDelay(40, () -> {
                int chamberFe = feStored(h, chamber);
                h.assertTrue(chamberFe > 0, "the enrichment chamber got no FE from the SI network (" + chamberFe + " FE)");
                h.runAfterDelay(280, () -> {
                    int dust = 0;
                    for (Direction side : Direction.values()) {
                        IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(chamber), side);
                        if (items == null) continue;
                        int here = 0;
                        for (int slot = 0; slot < items.getSlots(); slot++) {
                            ItemStack stack = items.getStackInSlot(slot);
                            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("mekanism:dust_iron")) here += stack.getCount();
                        }
                        dust = Math.max(dust, here);
                    }
                    long usedEu = siStart - siEu(h, storage);
                    h.assertTrue(dust > 0, "the enrichment chamber made no iron dust in 320 ticks (FE " + feStored(h, chamber) + ", SI paid " + usedEu + " EU)");
                    LOG.info("[universal] SI -> Mekanism enrichment chamber: {} iron dust, SI paid {} EU, chamber holds {} FE",
                        dust, usedEu, feStored(h, chamber));
                    h.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------ 3. SI -> AE2

    /** An SI MFSU charges an AE2 network (energy acceptor + energy cell) through an SI cable at 2 AE per EU (= 4 FE). */
    private static void siNetworkChargesAnAe2Network(GameTestHelper h) {
        BlockPos storage = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), acceptor = new BlockPos(3, 1, 2), cell = new BlockPos(4, 1, 2);
        h.setBlock(storage, facing(MFSU, Direction.EAST));
        h.setBlock(cable, block(SC_CABLE));
        place(h, acceptor, block("ae2:energy_acceptor").defaultBlockState());
        place(h, cell, block("ae2:energy_cell").defaultBlockState());
        setSiEu(h, storage, 1_000_000L);
        h.runAfterDelay(20, () -> {
            double aeStart = (Double) call(h.getBlockEntity(cell), "getAECurrentPower");
            long siStart = siEu(h, storage);
            h.runAfterDelay(100, () -> {
                double aeGained = (Double) call(h.getBlockEntity(cell), "getAECurrentPower") - aeStart;
                long euPaid = siStart - siEu(h, storage);
                h.assertTrue(aeGained > 0 && euPaid > 0, "the AE2 network got no energy: +" + aeGained + " AE, SI paid " + euPaid + " EU");
                double ratio = aeGained / euPaid;
                h.assertTrue(ratio > 1.7 && ratio <= 2.0 + 1e-6, "AE per EU " + ratio + " (+" + aeGained + " AE for " + euPaid + " EU)");
                LOG.info("[universal] SI -> AE2: {} EU paid, {} AE stored, {} AE per EU", euPaid, aeGained, ratio);
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ 4. anti-loop

    /**
     * An SI cable next to a Mekanism Universal Cable does not connect: it offers that cable no FE input, sends no FE into it (an
     * energy cube behind the Mekanism cable stays empty), and takes none from it (an MFSU behind an SI cable stays empty while a
     * full energy cube pushes into a Mekanism cable that touches it).
     */
    /** An AE2 wireless terminal (an FE item) with its FE capability, or a failed assertion. */
    private static ItemStack ae2Terminal(GameTestHelper h) {
        ItemStack terminal = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:wireless_terminal")));
        IEnergyStorage fe = terminal.getCapability(Capabilities.EnergyStorage.ITEM);
        h.assertTrue(!terminal.isEmpty() && fe != null && fe.canReceive(), "the AE2 wireless terminal has no FE item capability: " + terminal);
        return terminal;
    }

    private static int terminalFe(ItemStack terminal) {
        return terminal.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored();
    }

    /**
     * Follow-up: a BatBox Charger (a charge pad) charges an AE2 wireless terminal in the player's inventory, at 4 FE per EU, as the
     * MFSU's charge slot already did. Its own per-player step runs here with a mock player (not put into the world).
     */
    private static void chargePadChargesAnAe2Terminal(GameTestHelper h) {
        BlockPos pad = new BlockPos(2, 1, 2);
        place(h, pad, block("mio_icif:wiring/block_batbox_charger").defaultBlockState());
        setSiEu(h, pad, 10_000);
        ItemStack terminal = ae2Terminal(h);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(9, terminal);
        long before = siEu(h, pad);
        boolean charged = (Boolean) call(com.miophas.singularity_iteration.core.runtime.machine.ChargepadHelper.class, "chargePlayerEquipment",
            h.getBlockEntity(pad), player, 32L);
        long spent = before - siEu(h, pad);
        int got = terminalFe(terminal);
        LOG.info("charge pad -> AE2 terminal: {} EU spent, {} FE received", spent, got);
        h.assertTrue(charged && got > 0, "the charge pad did not charge the AE2 terminal (" + got + " FE)");
        h.assertTrue(got == spent * 4, "the terminal got " + got + " FE for " + spent + " EU, not 4 FE per EU");
        h.succeed();
    }

    /** Follow-up: a Charging RE-Battery in auto-charge charges an AE2 wireless terminal in the inventory, at 4 FE per EU. */
    private static void chargingBatteryChargesAnAe2Terminal(GameTestHelper h) {
        ItemStack battery = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:normal/item_chargebat_lev0")));
        var cell = (com.miophas.singularity_iteration.core.api.item.IBatteryItem) battery.getItem();
        cell.addEnergy(battery, cell.getMaxEnergy(battery));
        ItemStack terminal = ae2Terminal(h);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(9, terminal);
        long before = cell.getEnergy(battery);
        ((com.miophas.singularity_iteration.core.api.item.IEnergyDistributable) battery.getItem()).distributeEnergyToItems(player, battery, 1_000);
        long spent = before - cell.getEnergy(battery);
        int got = terminalFe(terminal);
        LOG.info("charging battery -> AE2 terminal: {} EU spent, {} FE received", spent, got);
        h.assertTrue(got > 0, "the charging battery did not charge the AE2 terminal");
        h.assertTrue(got <= spent * 4 && got > (spent - 1) * 4, "the terminal got " + got + " FE for " + spent + " EU, not 4 FE per EU");
        h.succeed();
    }

    private static void siCableRefusesAMekanismUniversalCable(GameTestHelper h) {
        // row z = 1: SI MFSU -> SI cable | Mekanism cable -> energy cube
        BlockPos siSource = new BlockPos(0, 1, 1), siCable = new BlockPos(1, 1, 1), mekCable = new BlockPos(2, 1, 1), mekCube = new BlockPos(3, 1, 1);
        h.setBlock(siSource, facing(MFSU, Direction.EAST));
        h.setBlock(siCable, block(SC_CABLE));
        place(h, mekCable, block("mekanism:basic_universal_cable").defaultBlockState());
        place(h, mekCube, facing("mekanism:basic_energy_cube", Direction.EAST));   // front away: its back takes input
        setSiEu(h, siSource, 1_000_000L);
        // row z = 3: full energy cube -> Mekanism cable | SI cable -> SI MFSU
        BlockPos fullCube = new BlockPos(0, 1, 3), mekCable2 = new BlockPos(1, 1, 3), siCable2 = new BlockPos(2, 1, 3), siSink = new BlockPos(3, 1, 3);
        place(h, fullCube, facing("mekanism:basic_energy_cube", Direction.EAST));   // its front (output) into the Mekanism cable
        place(h, mekCable2, block("mekanism:basic_universal_cable").defaultBlockState());
        h.setBlock(siCable2, block(SC_CABLE));
        h.setBlock(siSink, facing(MFSU, Direction.EAST));
        h.assertTrue(ForeignEnergyCables.is(h.getLevel(), h.absolutePos(mekCable)), "the Mekanism Universal Cable is not seen as another mod's cable");
        h.assertTrue(fe(h, siCable, Direction.EAST) == null, "the SI cable offers FE input toward the Mekanism cable");
        h.runAfterDelay(10, () -> {
            Direction in = inputFace(h, fullCube);
            h.assertTrue(in != null && fe(h, fullCube, in).receiveEnergy(400_000, false) > 0, "could not fill the energy cube: " + describe(h, fullCube));
            int cubeBefore = feStored(h, mekCube);
            long sinkBefore = siEu(h, siSink);
            h.runAfterDelay(80, () -> {
                int cubeGot = feStored(h, mekCube) - cubeBefore;
                long sinkGot = siEu(h, siSink) - sinkBefore;
                h.assertTrue(cubeGot == 0, "FE crossed from the SI cable into the Mekanism cable: the cube got " + cubeGot + " FE");
                h.assertTrue(sinkGot == 0, "FE crossed from the Mekanism cable into the SI cable: the MFSU got " + sinkGot + " EU");
                LOG.info("[universal] SI cable | Mekanism cable: no connection (cube +{} FE, MFSU +{} EU)", cubeGot, sinkGot);
                h.succeed();
            });
        });
    }
}
