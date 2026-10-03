package example.sicore;

import com.miophas.singularity_iteration.core.api.machine.MachineStatus;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 0.1.7.24 (Phase 1): machines report a status-lamp state (dark / green / blinking amber / red)
 * that the server tracks and syncs, the lamp models and block list ship with the mod, and every
 * refined machine front carries the shared panel layout with its own mechanism window.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round24GameTests {
    private Round24GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    @GameTest(batch = "round24", template = "empty", timeoutTicks = 120)
    public static void processingMachineLampFollowsItsState(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("producer/block_furnace_elc"));
        var furnace = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        furnace.getEnergyStorageInternal().setStored(0);
        h.assertTrue(furnace.machineStatus() == MachineStatus.OFF, "Empty and unpowered: dark, got " + furnace.machineStatus());
        var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(items.insertItem(0, new ItemStack(Items.CACTUS, 16), false).isEmpty(), "Cactus accepted");
        h.assertTrue(furnace.machineStatus() == MachineStatus.NO_POWER, "Input but no energy: amber, got " + furnace.machineStatus());
        h.onEachTick(() -> furnace.getEnergyStorageInternal().setStored(furnace.getEnergyStorageInternal().getCapacity()));
        h.succeedWhen(() -> {
            h.assertTrue(furnace.machineStatus() == MachineStatus.RUNNING, "Powered with input: green, got " + furnace.machineStatus());
            h.assertTrue(com.miophas.singularity_iteration.core.api.machine.MachineStatus.RUNNING == trackerStatus(furnace),
                "The tracker synced green, got " + trackerStatus(furnace));
        });
    }

    private static Object trackerStatus(AbstractEnergyBlockEntity machine) {
        try {
            Class<?> tracker = Class.forName("com.miophas.singularity_iteration" + ".common.machine.MachineStatusTracker");
            return call(tracker, "sentStatus", machine);
        } catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }

    @GameTest(batch = "round24", template = "empty", timeoutTicks = 40)
    public static void starvedMachineIsRedAndIdleGeneratorAmber(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1), gen = new BlockPos(3, 1, 1);
        h.setBlock(pos, block("producer/block_compressor_elc"));
        h.setBlock(gen, block("generator/block_geo_generator"));
        var compressor = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        var geo = (AbstractEnergyBlockEntity) h.getBlockEntity(gen);
        h.runAfterDelay(2, () -> {
            compressor.getEnergyStorageInternal().setStored(compressor.getEnergyStorageInternal().getCapacity());
            h.assertTrue(compressor.machineStatus() == MachineStatus.BLOCKED, "Powered but no input: red, got " + compressor.machineStatus());
            h.assertTrue(geo.machineStatus() == MachineStatus.NO_POWER, "Fuel-less generator: amber standby, got " + geo.machineStatus());
            geo.getEnergyStorageInternal().setStored(geo.getEnergyStorageInternal().getCapacity());
            h.assertTrue(geo.machineStatus() == MachineStatus.BLOCKED, "Generator with a full buffer: red (output blocked), got " + geo.machineStatus());
            h.succeed();
        });
    }

    private static String read(String... path) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        try { return java.nio.file.Files.readString(file.findResource(path)); } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    @GameTest(batch = "round24", template = "empty")
    public static void lampModelsAndBlockListShip(GameTestHelper h) {
        String list = read("resourcepacks", "si_experimental", "assets", "mio_icif", "si_status_lamps.json");
        for (String b : List.of("producer/block_compressor_elc", "producer/block_furnace_elc", "generator/block_geo_generator", "wiring/block_mfsu"))
            h.assertTrue(list.contains("mio_icif:" + b), b + " gets a status lamp");
        for (String lamp : List.of("off", "run", "nopower", "blocked")) {
            String model = read("assets", "mio_icif", "models", "block", "status", "lamp_" + lamp + ".json");
            h.assertTrue(model.contains("status_slit"), "lamp_" + lamp);
            h.assertTrue(model.contains("block_light") == !lamp.equals("off"), "lamp_" + lamp + ": only lit lamps glow");
        }
        h.assertTrue(read("assets", "mio_icif", "textures", "block", "status", "lamp_nopower.png.mcmeta").contains("animation"),
            "The amber lamp blinks (texture animation)");
        String compressor = read("resourcepacks", "si_experimental", "assets", "mio_icif", "models", "block", "producer", "block_compressor_elc.json");
        h.assertTrue(!compressor.contains("dsp_status_slit"), "The static slit is replaced by the lamp");
        h.succeed();
    }

    @GameTest(batch = "round24", template = "empty")
    public static void refinedFrontsShareTheLayoutAndDiffer(GameTestHelper h) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        int[][] fronts = new int[3][];
        String[] ids = {"block_compressor_elc", "block_powder_elc", "block_furnace_elc"};
        for (int i = 0; i < ids.length; i++) {
            try (var in = java.nio.file.Files.newInputStream(file.findResource("resourcepacks", "si_experimental", "assets", "mio_icif", "textures", "block", "refined", "producer", ids[i] + ".png"))) {
                var img = javax.imageio.ImageIO.read(in);
                // shared layout: dark recessed window frame at (6,9) and a header bar at (10,6)
                int frame = img.getRGB(6, 12), header = img.getRGB(10, 6);
                h.assertTrue(lum(frame) < 80 && lum(header) < 110, ids[i] + ": window frame / header present");
                fronts[i] = img.getRGB(7, 10, 18, 14, null, 0, 18);
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        h.assertTrue(!java.util.Arrays.equals(fronts[0], fronts[1]) && !java.util.Arrays.equals(fronts[1], fronts[2]),
            "Each machine has its own mechanism window");
        h.succeed();
    }

    private static int lum(int rgb) {
        return (((rgb >> 16) & 255) * 299 + ((rgb >> 8) & 255) * 587 + (rgb & 255) * 114) / 1000;
    }
}
