package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 0.1.7.22: machine-specific controls live in a left dock (the sort-button corner stays free),
 * the Armory Remote is an accessory driving the console's Armory section, showcases join the
 * armour network with a two-way exchange, the whole old suit bursts off at once, the remote
 * never flies away, and the refined icons / models carry their badges and status lights.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round22GameTests {
    private Round22GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    private static Class<?> type(String name) {
        try { return Class.forName(COMMON + name); } catch (ClassNotFoundException e) { throw new AssertionError(e); }
    }

    private static AbstractContainerMenu menuOf(GameTestHelper h, String blockPath) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block(blockPath));
        BlockEntity be = h.getBlockEntity(pos);
        var player = h.makeMockServerPlayerInLevel();
        return (AbstractContainerMenu) call(be, "createMenu", 1, player.getInventory(), player);
    }

    @GameTest(batch = "round22", template = "empty")
    public static void upgradeSlotsLiveInTheLeftDock(GameTestHelper h) {
        for (String machine : List.of("producer/block_furnace_elc", "producer/block_compressor_advanced_elc", "producer/block_matron_elc",
                "producer/block_laser_defense_tower", "producer/block_induction_elc")) {
            AbstractContainerMenu menu = menuOf(h, machine);
            int docked = (int) call(menu, "dockedUpgradeSlots");
            h.assertTrue(docked > 0, machine + " has upgrade slots");
            int k = 0;
            for (Slot slot : menu.slots) {
                if (slot.container instanceof Inventory) continue;
                if (slot.x < 0) {
                    h.assertTrue(slot.x == -21 && slot.y == 8 + 18 * k, machine + ": dock slot " + k + " at " + slot.x + "," + slot.y);
                    k++;
                } else {
                    // nothing machine-specific in the sort-button corner (upper right of the panel)
                    h.assertTrue(!(slot.x >= 150 && slot.y < 20), machine + ": slot in the sort-button corner at " + slot.x + "," + slot.y);
                }
            }
            h.assertTrue(k == docked, machine + ": " + k + " dock slots, expected " + docked);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ armory network
    private static ServerPlayer owner(GameTestHelper h, Object armory) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        call(armory, "tryBind", p);
        ItemStack remote = ArmoryServiceGameTests.si("normal/item_armory_remote");
        @SuppressWarnings("unchecked")
        var target = (net.minecraft.core.component.DataComponentType<GlobalPos>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(id("armory_target"));
        h.assertTrue(target != null, "armory_target component");
        remote.set(target, GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(2, 2, 2))));
        p.getInventory().setItem(8, remote);
        return p;
    }

    @GameTest(template = "reactor_loop", batch = "round22_showcase", timeoutTicks = 240)
    public static void showcaseExchangesSuitsBothWays(GameTestHelper h) {
        var armory = ArmoryServiceGameTests.armory(h, 300_000);
        ServerPlayer p = owner(h, armory);
        BlockEntity showcase = ArmoryServiceGameTests.showcase(h);
        ServerPlayer dresser = ArmoryServiceGameTests.player(h);
        ArmoryServiceGameTests.use(h, dresser, new ItemStack(Items.DIAMOND_HELMET));
        ArmoryServiceGameTests.use(h, dresser, new ItemStack(Items.DIAMOND_BOOTS));
        dresser.discard();
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Class<?> service = type("armory.ArmoryRemoteService");
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) call(service, "showcases", p, armory);
        h.assertTrue(list.size() == 1, "The dressed showcase belongs to the network, found " + list.size());
        long before = ArmoryServiceGameTests.energy(armory);
        call(service, "handle", p, 2, 0);   // SHOWCASE exchange
        h.assertTrue(ArmoryServiceGameTests.energy(armory) < before, "The exchange is paid by the Armory");
        // burst purge: the old helmet left the body at once and is already on the mannequin
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Old helmet purged at once");
        h.assertTrue(((ItemStack) call(showcase, "item", headPiece())).is(Items.IRON_HELMET), "Old helmet lands on the showcase");
        h.onEachTick(() -> {
            if (p.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && p.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS)) {
                h.assertTrue(((ItemStack) call(showcase, "item", headPiece())).is(Items.IRON_HELMET), "Two-way exchange");
                p.discard();
                h.succeed();
            }
        });
    }

    private static Object headPiece() {
        Class<?> piece = type("armory.ArmoryPiece");
        for (Object c : piece.getEnumConstants()) if (((Enum<?>) c).name().equals("HEAD")) return c;
        throw new AssertionError("HEAD");
    }

    @GameTest(template = "reactor_loop", batch = "round22_return", timeoutTicks = 60)
    public static void returnSendsTheWornSuitHome(GameTestHelper h) {
        var armory = ArmoryServiceGameTests.armory(h, 300_000);
        ServerPlayer p = owner(h, armory);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        call(type("armory.ArmoryRemoteService"), "handle", p, 3, 0);   // RETURN
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && p.getItemBySlot(EquipmentSlot.LEGS).isEmpty(), "Suit purged");
        boolean chest = false, legs = false;
        for (int s = 0; s < armory.getItemHandler().getSlots(); s++) {
            chest |= armory.getItemHandler().getStackInSlot(s).is(Items.IRON_CHESTPLATE);
            legs |= armory.getItemHandler().getStackInSlot(s).is(Items.IRON_LEGGINGS);
        }
        h.assertTrue(chest && legs, "Returned pieces are stored in the Armory");
        long echoes = h.getLevel().getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(32),
            e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("armory_piece")).size();
        h.assertTrue(echoes == 2, "Both pieces fly home visibly, got " + echoes);
        p.discard();
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "round22_remote", timeoutTicks = 260)
    public static void remoteInHandNeverFliesAway(GameTestHelper h) {
        var armory = ArmoryServiceGameTests.armory(h, 300_000);
        ServerPlayer p = owner(h, armory);
        ItemStack remote = p.getInventory().getItem(8).copy();
        p.getInventory().setItem(8, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND, remote);
        armory.getItemHandler().insertItem(5, new ItemStack(Items.IRON_SWORD), false);   // suit 0 main hand
        call(type("armory.ArmoryRemoteService"), "handle", p, 1, 0);                     // SUMMON suit 0
        h.assertTrue(p.getMainHandItem().getItem() == remote.getItem(), "The remote is not purged");
        h.onEachTick(() -> {
            if (!p.getMainHandItem().is(Items.IRON_SWORD)) return;
            boolean kept = false;
            for (ItemStack s : p.getInventory().items) kept |= s.getItem() == remote.getItem();
            h.assertTrue(kept, "The remote moved to the inventory instead of flying to the Armory");
            for (int s = 0; s < armory.getItemHandler().getSlots(); s++)
                h.assertTrue(armory.getItemHandler().getStackInSlot(s).getItem() != remote.getItem(), "Remote stored in the Armory");
            p.discard();
            h.succeed();
        });
    }

    @GameTest(batch = "round22", template = "empty")
    public static void remoteIsACuriosAccessory(GameTestHelper h) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        for (String slot : List.of("charm", "belt")) {
            var path = file.findResource("data", "curios", "tags", "item", slot + ".json");
            try {
                h.assertTrue(java.nio.file.Files.readString(path).contains("mio_icif:normal/item_armory_remote"), "Remote fits the " + slot + " slot");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        try {
            h.assertTrue(java.nio.file.Files.readString(file.findResource("data", "mio_icif", "curios", "entities", "player.json")).contains("charm"),
                "Players get a charm slot");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.assertTrue(BuiltInRegistries.MENU.get(id("armory_remote_menu")) == null, "The separate remote GUI is gone");
        h.succeed();
    }

    @GameTest(batch = "round22", template = "empty")
    public static void machineIconsAreBadgeFreeAndModelsCarryStatusLights(GameTestHelper h) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        int icons = 0;
        for (String icon : List.of("producer/block_compressor_elc", "generator/block_solar_generator", "wiring/block_mfsu", "reactor/block_reactor_chamber")) {
            try (var in = java.nio.file.Files.newInputStream(file.findResource("assets", "mio_icif", "textures", "item", "icon", icon.split("/")[0], icon.split("/")[1] + ".png"))) {
                var image = javax.imageio.ImageIO.read(in);
                // 0.1.7.23: no corner badge any more - the lower-right corner stays transparent
                int corner = image.getRGB(image.getWidth() - 1, image.getHeight() - 1);
                int inBadge = image.getRGB(image.getWidth() - 7, image.getHeight() - 1);
                h.assertTrue(((corner >> 24) & 255) == 0 && ((inBadge >> 24) & 255) == 0, icon + " must not carry a corner badge");
                icons++;
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        for (String model : List.of("producer/block_compressor_elc", "producer/block_compressor_elc_on")) {
            try {
                String json = java.nio.file.Files.readString(file.findResource("assets", "mio_icif", "models", "block", model.split("/")[0], model.split("/")[1] + ".json"));
                h.assertTrue(json.contains("dsp_indicator"), model + " has its category LED (the status slit is the 0.1.7.24 lamp)");
                h.assertTrue(json.contains("block_light") == model.endsWith("_on"), model + ": only the running model glows");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        h.assertTrue(icons == 4, "icons checked");
        h.succeed();
    }

    @GameTest(batch = "round22_grid", template = "empty", timeoutTicks = 200)
    public static void energyTerminalSeesARowOfMachines(GameTestHelper h) {
        for (int x = 0; x <= 8; x++) h.setBlock(new BlockPos(x, 1, 2), block("wiring/cable/block_cable"));
        h.setBlock(new BlockPos(0, 1, 3), block("wiring/block_energy_terminal"));
        h.setBlock(new BlockPos(1, 1, 1), block("generator/block_geo_generator"));
        h.setBlock(new BlockPos(2, 1, 1), block("generator/block_solar_generator"));
        h.setBlock(new BlockPos(3, 1, 1), block("wiring/block_mfsu"));
        h.setBlock(new BlockPos(4, 1, 1), block("producer/block_furnace_elc"));
        var items = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, h.absolutePos(new BlockPos(4, 1, 1)), null);
        if (items != null) items.insertItem(0, new ItemStack(Items.CACTUS, 64), false);
        var term = h.getBlockEntity(new BlockPos(0, 1, 3));
        h.succeedWhen(() -> {
            Object s = call(term, "snapshot");
            h.assertTrue(s != null, "snapshot");
            int conductors = (int) ArmoryServiceGameTests.field(s, "conductors");
            List<?> devices = (List<?>) ArmoryServiceGameTests.field(s, "devices");
            h.assertTrue(conductors == 9, "9 cables expected, got " + conductors);
            h.assertTrue(devices.size() == 4, "4 devices expected, got " + devices.size());
            float cap = (float) ArmoryServiceGameTests.field(s, "generationCapacity"), demand = (float) ArmoryServiceGameTests.field(s, "demand");
            h.assertTrue(cap > 0, "Generator nameplate capacity is reported, got " + cap);
            h.assertTrue(demand > 0, "Consumer demand is reported, got " + demand);
        });
    }

    @GameTest(batch = "round22", template = "empty", timeoutTicks = 60)
    public static void energyTerminalReportsNameplateCapacityAndDemand(GameTestHelper h) {
        BlockPos terminal = new BlockPos(1, 1, 1), gen = new BlockPos(2, 1, 1), machine = new BlockPos(1, 1, 2);
        h.setBlock(terminal, block("wiring/block_energy_terminal"));
        h.setBlock(gen, block("generator/block_solar_generator"));
        h.setBlock(machine, block("producer/block_furnace_elc"));
        var be = h.getBlockEntity(terminal);
        h.runAfterDelay(30, () -> {
            Object snapshot = call(be, "snapshot");
            h.assertTrue(snapshot != null, "Terminal publishes a snapshot");
            float ratioG = (float) call(snapshot, "generationRatio"), ratioD = (float) call(snapshot, "demandRatio");
            h.assertTrue(ratioG >= 0 && ratioG <= 1.0001F && ratioD >= 0 && ratioD <= 1.0001F, "Ratios are fractions: " + ratioG + " / " + ratioD);
            h.succeed();
        });
    }
}
