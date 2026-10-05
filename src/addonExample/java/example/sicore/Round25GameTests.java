package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.EnergyNodeRegistry;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentSpecialCableBlockEntity;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.7.25: energy terminal global mode and recognition self-check, the voltage detector reading
 * a cable network, the induction furnace overclocker, storage output while charging items, and
 * the physical (window-less) fronts of storage / kinetic / heat machines.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round25GameTests {
    private Round25GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }
    private static Object field(Object target, String name) { return ArmoryServiceGameTests.field(target, name); }

    private static Class<?> walker() {
        try { return Class.forName(COMMON + "blockentity.wiring.terminal.NetworkWalker"); }
        catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }

    private static boolean walkerSays(String method, BlockEntity be) {
        try {
            Method m = walker().getMethod(method, BlockEntity.class);
            Object r = m.invoke(null, be);
            return r instanceof Boolean b ? b : r != null;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    /** Every block entity the EU engine treats as a node must be known to the terminal / detector walker. */
    @GameTest(batch = "round25_selfcheck", template = "empty", timeoutTicks = 40)
    public static void everyEnergyBlockIsRecognisedByTheTerminal(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        List<String> missed = new ArrayList<>();
        int checked = 0, cables = 0, transformers = 0;
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation key = entry.getKey().location();
            if (!key.getNamespace().equals("mio_icif") || !(entry.getValue() instanceof EntityBlock)) continue;
            String path = key.getPath();
            if (path.contains("nuke") || path.contains("bomb") || path.contains("tnt") || path.contains("explosive")) continue;
            BlockEntity be;
            try {
                h.setBlock(at, entry.getValue());
                be = h.getLevel().getBlockEntity(h.absolutePos(at));
            } catch (RuntimeException e) {
                continue;
            }
            if (be == null) { h.setBlock(at, Blocks.AIR); continue; }
            boolean conductor = be instanceof ICableEnergyNode || be instanceof IndependentSpecialCableBlockEntity;
            boolean transformer = be instanceof IndependentTransformerBlockEntity || path.startsWith("wiring/transformer_");
            boolean energy = conductor || transformer || be instanceof AbstractEnergyBlockEntity || EnergyNodeRegistry.profile(key) != null;
            if (energy && !path.equals("wiring/block_energy_terminal")) {
                checked++;
                boolean ok;
                if (conductor) { ok = walkerSays("isConductor", be); cables++; }
                else if (transformer) { ok = walkerSays("isTransformer", be); transformers++; }
                else ok = walkerSays("categorize", be) || walkerSays("isConductor", be);
                if (!ok) missed.add(path + " (" + be.getClass().getSimpleName() + ")");
            }
            h.setBlock(at, Blocks.AIR);
        }
        h.assertTrue(cables >= 14, "All cable kinds (incl. superconducting, detector, splitter) checked, got " + cables);
        h.assertTrue(transformers >= 6, "Every transformer tier up to LuV->ZPMV checked, got " + transformers);
        h.assertTrue(missed.isEmpty(), "Terminal does not recognise " + missed.size() + "/" + checked + ": " + missed);
        h.succeed();
    }

    /** source - cable - LV/MV transformer - cable - sink; the terminal sits on the source side. */
    @GameTest(batch = "round25_global", template = "reactor_loop", timeoutTicks = 200)
    public static void globalModeCrossesTransformers(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), xfmr = new BlockPos(4, 2, 1), sink = new BlockPos(7, 2, 1), terminal = new BlockPos(2, 2, 2);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x : new int[]{2, 3, 5, 6}) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_cable"));
        h.setBlock(xfmr, block("wiring/transformer_lv_mv"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        h.setBlock(terminal, block("wiring/block_energy_terminal"));
        BlockEntity term = h.getBlockEntity(terminal);
        BlockEntity transformerBe = h.getBlockEntity(xfmr);
        h.assertTrue(walkerSays("isTransformer", transformerBe), "Transformer recognised: " + transformerBe.getClass().getSimpleName());
        call(term, "setGlobalMode", false);     // 0.1.7.27: new terminals start in GLOBAL; check LOCAL first
        int[] phase = {0};
        h.onEachTick(() -> {
            Object s = call(term, "snapshot");
            List<?> devices = (List<?>) field(s, "devices");
            boolean global = (boolean) field(s, "global");
            if (phase[0] == 0 && !global && devices.size() == 2) {
                // local scope: the source and the transformer, nothing behind it
                h.assertTrue((int) field(s, "conductors") == 2, "Local scope counts 2 cables, got " + field(s, "conductors"));
                call(term, "setGlobalMode", true);
                phase[0] = 1;
            } else if (phase[0] == 1 && global) {
                List<?> subnets = (List<?>) field(s, "subnets");
                h.assertTrue(subnets.size() == 2, "Two sub-networks across the transformer, got " + subnets.size());
                h.assertTrue(devices.size() == 3, "Global scope sees source, transformer and sink, got " + devices.size());
                h.assertTrue((int) field(s, "conductors") == 4, "Global scope counts all 4 cables, got " + field(s, "conductors"));
                phase[0] = 2;
            }
        });
        h.succeedWhen(() -> h.assertTrue(phase[0] == 2, "waiting for global scan, phase " + phase[0]));
    }

    /** The detector on a bare cable keeps the GUI open and samples the network instead of closing. */
    @GameTest(batch = "round25_meter", template = "reactor_loop", timeoutTicks = 120)
    public static void meterReadsTheNetworkThroughACable(GameTestHelper h) {
        BlockPos source = new BlockPos(1, 2, 1), cable = new BlockPos(3, 2, 1), sink = new BlockPos(5, 2, 1);
        h.setBlock(source, CoreExampleMod.SOURCE.get());
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 1), block("wiring/cable/block_cable"));
        h.setBlock(sink, CoreExampleMod.SINK.get());
        InheritedSource src = h.getBlockEntity(source);
        var player = h.makeMockServerPlayerInLevel();
        AbstractContainerMenu meter;
        try {
            meter = (AbstractContainerMenu) Class.forName(COMMON + "menu.tool.mio_icif_meter_menu")
                .getConstructor(int.class, Inventory.class, BlockPos.class, int.class)
                .newInstance(7, player.getInventory(), h.absolutePos(cable), 1);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        player.containerMenu = meter;
        int[] ticks = {0};
        h.onEachTick(() -> {
            src.generate(32);
            if (player.containerMenu == meter) call(meter, "sampleCompletedTick", player);
            ticks[0]++;
        });
        h.succeedWhen(() -> {
            h.assertTrue(player.containerMenu == meter, "The meter must stay open on a cable");
            h.assertTrue((int) call(meter, "getResultCount") > 20, "samples: " + call(meter, "getResultCount"));
            h.assertTrue((double) call(meter, "getResultMax") > 0, "Energy out of the network measured, max " + call(meter, "getResultMax"));
        });
    }

    private static long work(BlockEntity furnace) {
        IItemHandlerModifiable items = (IItemHandlerModifiable) field(furnace, "itemHandler");
        int done = items.getStackInSlot(3).getCount() + items.getStackInSlot(4).getCount();
        return done * ((Number) field(furnace, "PROGRESS_TARGET")).longValue() + ((Number) field(furnace, "progress")).longValue();
    }

    /** Two overclockers make the induction furnace heat up and smelt faster than a bare one. */
    @GameTest(batch = "round25_induction", template = "empty", timeoutTicks = 400)
    public static void inductionFurnaceOverclockerSpeedsItUp(GameTestHelper h) {
        BlockPos plainPos = new BlockPos(1, 1, 1), fastPos = new BlockPos(3, 1, 1);
        h.setBlock(plainPos, block("producer/block_induction_elc"));
        h.setBlock(fastPos, block("producer/block_induction_elc"));
        AbstractEnergyBlockEntity plain = (AbstractEnergyBlockEntity) h.getBlockEntity(plainPos);
        AbstractEnergyBlockEntity fast = (AbstractEnergyBlockEntity) h.getBlockEntity(fastPos);
        for (AbstractEnergyBlockEntity f : List.of(plain, fast)) {
            IItemHandlerModifiable items = (IItemHandlerModifiable) field(f, "itemHandler");
            items.setStackInSlot(0, new ItemStack(Items.RAW_IRON, 64));
            items.setStackInSlot(1, new ItemStack(Items.RAW_IRON, 64));
        }
        IItemHandlerModifiable fastItems = (IItemHandlerModifiable) field(fast, "itemHandler");
        fastItems.setStackInSlot(5, new ItemStack(BuiltInRegistries.ITEM.get(id("upgrade/overclocker_upgrade")), 2));
        h.onEachTick(() -> {
            for (AbstractEnergyBlockEntity f : List.of(plain, fast)) f.getEnergyStorageInternal().setStored(f.getEnergyStorageInternal().getCapacity());
        });
        h.runAfterDelay(300, () -> {
            long a = work(plain), b = work(fast);
            h.assertTrue(a > 0, "The bare furnace works too, got " + a);
            h.assertTrue(b > a * 1.3, "Overclocked furnace must be clearly faster: bare " + a + ", overclocked " + b);
            h.succeed();
        });
    }

    /** A full MFSU keeps feeding the grid while it charges a battery: charging does not lock output. */
    @GameTest(batch = "round25_storage", template = "reactor_loop", timeoutTicks = 120)
    public static void storageOutputsWhileChargingItems(GameTestHelper h) {
        BlockPos box = new BlockPos(3, 2, 3);
        h.setBlock(box, block("wiring/block_mfsu"));
        AbstractEnergyStorageBlockEntity mfsu = (AbstractEnergyStorageBlockEntity) h.getBlockEntity(box);
        Direction out = null;
        for (Direction d : Direction.values()) if (mfsu.canProvidePowerFromSide(d)) { out = d; break; }
        h.assertTrue(out != null, "The MFSU has an output face");
        BlockPos sinkPos = box.relative(out);
        h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        InheritedSink sink = h.getBlockEntity(sinkPos);
        int chargeSlot = ((Number) field(mfsu, "CHARGE_SLOT")).intValue();
        ItemStack battery = new ItemStack(BuiltInRegistries.ITEM.get(id("normal/item_lithium_battery")));
        mfsu.getItemHandler().insertItem(chargeSlot, battery, false);
        var api = MioIcifAPI.instance().getItemAPI();
        h.onEachTick(() -> mfsu.getEnergyStorageInternal().setStored(Math.max(mfsu.getEnergyStorageInternal().getAmount(),
            mfsu.getEnergyStorageInternal().getCapacity() / 2)));
        h.succeedWhen(() -> {
            ItemStack charging = mfsu.getItemHandler().getStackInSlot(chargeSlot);
            h.assertTrue(!charging.isEmpty() && api.getBatteryStored(charging) > 0, "The battery in the charge slot is being charged");
            h.assertTrue(sink.stored() > 0, "...and the grid side still receives energy");
        });
    }

    private static int screenPixels(String path) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        try (var in = java.nio.file.Files.newInputStream(file.findResource(("resourcepacks/si_experimental/assets/mio_icif/textures/block/refined/" + path + ".png").split("/")))) {
            var img = javax.imageio.ImageIO.read(in);
            int n = 0;
            for (int y = 4; y < 30; y++) for (int x = 4; x < 28; x++) {
                int rgb = img.getRGB(x, y), r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                if (b > r + 25 && (r * 299 + g * 587 + b * 114) / 1000 < 90) n++;
            }
            return n;
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    /** Storage boxes show an output socket, kinetic machines a shaft coupling, heat machines a copper port - no screens. */
    @GameTest(batch = "round25_fronts", template = "empty")
    public static void physicalFrontsHaveNoScreen(GameTestHelper h) {
        h.assertTrue(screenPixels("producer/block_compressor_elc") > 50, "Sanity: a processing machine still has its display window");
        for (String p : List.of("wiring/block_bat_box", "wiring/block_cesu", "wiring/block_mfe", "wiring/block_mfsu",
                "kugenerator/block_kinetic_generator_elc", "kugenerator/block_stirling_kinetic_generator",
                "hugenerator/block_solid_heat_generator", "hugenerator/block_fluid_heat_generator")) {
            h.assertTrue(screenPixels(p) < 8, p + " must not carry a display window, " + screenPixels(p) + " screen pixels");
        }
        h.succeed();
    }

    /** 0.1.7.26: the dedicated suit models are sealed - Nano / Quantum render with the classic 64x32 armour again. */
    @GameTest(batch = "round25_suits", template = "empty")
    public static void suitsUseClassicArmourAndClassicPackShips(GameTestHelper h) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        h.assertTrue(!java.nio.file.Files.exists(file.findResource("assets", "mio_icif", "textures", "armor", "suit", "quantum.png")),
            "The sealed suit atlases are no longer shipped");
        h.assertTrue(java.nio.file.Files.exists(file.findResource("resourcepacks", "si_experimental", "pack.mcmeta")), "Experimental pack ships");
        h.assertTrue(java.nio.file.Files.exists(file.findResource("resourcepacks", "si_experimental", "assets", "mio_icif", "models", "block", "producer", "block_compressor_elc.json")),
            "The experimental pack carries the refined machine models");
        // 0.1.7.34: the nano suit now has its own Blockbench model and per-piece textures (Round34GameTests)
        for (String suit : List.of("quantum")) {
            for (var slot : List.of(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS)) {
                String piece = slot == net.minecraft.world.entity.EquipmentSlot.CHEST ? "chestplate" : "leggings";
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id("armor/item_armor_" + suit + "_" + piece)));
                var item = (net.minecraft.world.item.ArmorItem) stack.getItem();
                Object tex = item.getArmorTexture(stack, null, slot, item.getMaterial().value().layers().get(0), slot == net.minecraft.world.entity.EquipmentSlot.LEGS);
                String want = "textures/armor/" + suit + (slot == net.minecraft.world.entity.EquipmentSlot.LEGS ? "_2" : "_1") + ".png";
                h.assertTrue(String.valueOf(tex).endsWith(want), suit + " " + piece + " uses the classic texture " + want + ", got " + tex);
            }
        }
        h.succeed();
    }
}
