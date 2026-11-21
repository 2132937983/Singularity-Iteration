package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.97: the fusion reactor's fuel injection port and disruption mitigation port have slots of their own and a screen:
 * pipes and hoppers put deuterium and tritium fuel rods (fuel port) or coolant cells (mitigation port) only into their own
 * slots and take only the empty fuel rods out; the formed controller takes one rod or cell whenever its slot is empty and hands
 * its empty fuel rods back to the fuel port. (The mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11597GameTests {
    private Round11597GameTests() {}

    private static final String CONTROLLER = "blockentity.fusion.FusionControllerBlockEntity";
    private static final String D = "normal/item_deuterium_cell", T = "normal/item_tritium_cell", ROD = "normal/item_fuel_rod",
        C10 = "reactor/item_reactor_collant_simple", C30 = "reactor/item_reactor_collant_triple", C60 = "reactor/item_reactor_coolant_six";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(id(path)); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack stack(String path, int count) { return new ItemStack(item(path), count); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static int controllerSlot(String field) {
        try {
            return Round33GameTests.common(CONTROLLER).getField(field).getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static IItemHandler handler(GameTestHelper h, BlockPos absolute) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, absolute, null);
    }

    private static int count(IItemHandler items, String path) {
        int n = 0;
        for (int i = 0; i < items.getSlots(); i++) if (items.getStackInSlot(i).is(item(path))) n += items.getStackInSlot(i).getCount();
        return n;
    }

    @GameTest(template = "empty", batch = "round11597_ports", timeoutTicks = 20)
    public static void portsTakeOnlyTheirOwnItems(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        BlockPos fuelAt = new BlockPos(1, 2, 1), coolAt = new BlockPos(3, 2, 1);
        h.setBlock(fuelAt, block("fusion/block_fusion_fuel_port"));
        h.setBlock(coolAt, block("fusion/block_fusion_mitigation_port"));
        IItemHandler fuel = handler(h, h.absolutePos(fuelAt)), cool = handler(h, h.absolutePos(coolAt));
        if (fuel == null || cool == null) {
            h.fail("a port has no item handler: fuel " + fuel + ", mitigation " + cool);
            return;
        }
        if (fuel.getSlots() != 6 || cool.getSlots() != 4) wrong.add("slots: fuel " + fuel.getSlots() + ", mitigation " + cool.getSlots());
        // the fuel port: deuterium in 0-1, tritium in 2-3, nothing into the outputs 4-5
        if (!fuel.insertItem(0, stack(D, 4), false).isEmpty()) wrong.add("the deuterium slot refuses deuterium rods");
        if (!fuel.insertItem(2, stack(T, 4), false).isEmpty()) wrong.add("the tritium slot refuses tritium rods");
        if (fuel.insertItem(1, stack(T, 1), true).isEmpty()) wrong.add("the deuterium slot takes a tritium rod");
        if (fuel.insertItem(3, stack(D, 1), true).isEmpty()) wrong.add("the tritium slot takes a deuterium rod");
        for (int s = 0; s < fuel.getSlots(); s++) {
            if (fuel.insertItem(s, stack(C60, 1), true).isEmpty()) wrong.add("fuel port slot " + s + " takes a coolant cell");
            if (fuel.insertItem(s, stack("reactor/item_reactor_uranium_simple", 1), true).isEmpty()) wrong.add("fuel port slot " + s + " takes a uranium rod");
        }
        if (fuel.insertItem(4, stack(ROD, 1), true).isEmpty() || fuel.insertItem(5, stack(D, 1), true).isEmpty())
            wrong.add("an empty-rod output takes items");
        if (!fuel.extractItem(0, 1, true).isEmpty() || !fuel.extractItem(2, 1, true).isEmpty()) wrong.add("the fuel inputs give items out");
        // the mitigation port: the three coolant cells, nothing else, nothing out
        if (!cool.insertItem(0, stack(C10, 1), false).isEmpty() || !cool.insertItem(1, stack(C30, 1), false).isEmpty()
                || !cool.insertItem(2, stack(C60, 2), false).isEmpty()) wrong.add("the mitigation port refuses a coolant cell");
        if (cool.insertItem(3, stack(D, 1), true).isEmpty() || cool.insertItem(3, stack(T, 1), true).isEmpty())
            wrong.add("the mitigation port takes a fuel rod");
        if (!cool.extractItem(0, 1, true).isEmpty()) wrong.add("the mitigation port gives items out");
        // not part of a reactor: not linked
        BlockEntity port = h.getBlockEntity(fuelAt);
        int[] values = (int[]) call(port, "screenValues");
        if (values[0] != 0) wrong.add("a lone port says it is linked");
        // the screen: the port's slots and the player's; a shift-click puts tritium rods into the tritium slots
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = ((MenuProvider) port).createMenu(0, player.getInventory(), player);
        if (menu == null || menu.slots.size() != 6 + 36) wrong.add("the fuel port's menu has " + (menu == null ? "no" : menu.slots.size()) + " slots");
        else {
            player.getInventory().setItem(9, stack(T, 5));
            menu.quickMoveStack(player, 6);
            var items = (IItemHandlerModifiable) call(port, "getItems");
            if (items.getStackInSlot(2).getCount() + items.getStackInSlot(3).getCount() != 9) wrong.add("a shift-click did not put the tritium rods in");
            if (!player.getInventory().getItem(9).isEmpty()) wrong.add("the tritium rods stayed in the inventory");
        }
        AbstractContainerMenu coolMenu = ((MenuProvider) h.getBlockEntity(coolAt)).createMenu(1, player.getInventory(), player);
        if (coolMenu == null || coolMenu.slots.size() != 4 + 36) wrong.add("the mitigation port's menu is wrong");
        // broken, a port drops what it holds
        h.getLevel().destroyBlock(h.absolutePos(fuelAt), false);
        int dropped = 0;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(fuelAt)).inflate(2)))
            if (e.getItem().is(item(D))) dropped += e.getItem().getCount();
        if (dropped != 4) wrong.add("the broken fuel port dropped " + dropped + " deuterium rods, not 4");
        h.assertTrue(wrong.isEmpty(), "ports: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11597_fusion_ports", timeoutTicks = 100)
    public static void formedControllerTakesFromThePortsAndGivesTheEmptyRodsBack(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 6, 2));
        call(Round33GameTests.common("fusion.FusionDemo"), "placeStructure", h.getLevel(), at);
        h.runAfterDelay(3, () -> {
            List<String> wrong = new ArrayList<>();
            BlockEntity c = h.getLevel().getBlockEntity(at);
            call(c, "validate");
            if (!(Boolean) call(c, "isFormed")) {
                h.fail("the tokamak as laid out did not form");
                return;
            }
            BlockPos fuelAt = null, coolAt = null;
            for (Object o : (List<?>) call(c, "ports")) {
                BlockPos p = (BlockPos) o;
                if (h.getLevel().getBlockState(p).is(block("fusion/block_fusion_fuel_port"))) fuelAt = p;
                if (h.getLevel().getBlockState(p).is(block("fusion/block_fusion_mitigation_port"))) coolAt = p;
            }
            if (fuelAt == null || coolAt == null) {
                h.fail("the laid-out tokamak has no fuel port or no mitigation port");
                return;
            }
            IItemHandler fuel = handler(h, fuelAt), cool = handler(h, coolAt);
            var items = (IItemHandlerModifiable) call(c, "getItems");
            int dSlot = controllerSlot("SLOT_DEUTERIUM"), tSlot = controllerSlot("SLOT_TRITIUM"), mitSlot = controllerSlot("SLOT_MITIGATION");
            int out1 = controllerSlot("SLOT_EMPTY_CELL"), out2 = controllerSlot("SLOT_EMPTY_ROD");
            for (int s : new int[] {dSlot, tSlot, mitSlot, out1, out2}) items.setStackInSlot(s, ItemStack.EMPTY);
            fuel.insertItem(0, stack(D, 3), false);
            fuel.insertItem(2, stack(T, 3), false);
            cool.insertItem(0, stack(C60, 2), false);
            // what the controller does every tick, ten times
            for (int i = 0; i < 10; i++) {
                call(c, "servePorts");
                call(c, "takeFuel");
            }
            long[] v = (long[]) call(c, "screenValues");
            if (v[8] != 30_000 || v[9] != 30_000) wrong.add("the tanks hold " + v[8] + " deuterium and " + v[9] + " tritium, not 30,000 each");
            if (count(fuel, D) != 0 || count(fuel, T) != 0) wrong.add("rods are left in the fuel port");
            int emptyRods = fuel.getStackInSlot(4).getCount() + fuel.getStackInSlot(5).getCount();
            if (emptyRods != 6 || !fuel.getStackInSlot(4).is(item(ROD))) wrong.add(emptyRods + " empty fuel rods in the fuel port, not 6");
            if (!items.getStackInSlot(out1).isEmpty() || !items.getStackInSlot(out2).isEmpty()) wrong.add("empty fuel rods stayed in the controller");
            if (fuel.extractItem(4, 64, false).getCount() != emptyRods) wrong.add("a hopper cannot take the empty fuel rods out");
            // one coolant cell in the controller, the other waits in the port
            if (items.getStackInSlot(mitSlot).getCount() != 1 || !items.getStackInSlot(mitSlot).is(item(C60))) wrong.add("the mitigation slot holds " + items.getStackInSlot(mitSlot));
            if (count(cool, C60) != 1) wrong.add(count(cool, C60) + " coolant cells left in the mitigation port, not 1");
            // after a mitigated disruption the next cell moves in
            call(c, "disrupt", true, 5.984e8);   // 0.1.11.137: the reference plasma's thermal energy (J), was 15 keV
            call(c, "servePorts");
            if (items.getStackInSlot(mitSlot).getCount() != 1 || count(cool, C60) != 0) wrong.add("the next coolant cell did not move in after a disruption");
            int[] fuelValues = (int[]) call(h.getLevel().getBlockEntity(fuelAt), "screenValues");
            int[] coolValues = (int[]) call(h.getLevel().getBlockEntity(coolAt), "screenValues");
            if (fuelValues[0] != 1 || fuelValues[1] != 30_000 || fuelValues[2] != 30_000) wrong.add("the fuel port's screen shows " + java.util.Arrays.toString(fuelValues));
            if (coolValues[0] != 1 || coolValues[1] != 1) wrong.add("the mitigation port's screen shows " + java.util.Arrays.toString(coolValues));
            // leave the reactor empty (it stays in the world)
            for (int s : new int[] {dSlot, tSlot, mitSlot, out1, out2}) items.setStackInSlot(s, ItemStack.EMPTY);
            h.assertTrue(wrong.isEmpty(), "fusion ports: " + wrong);
            h.succeed();
        });
    }
}
