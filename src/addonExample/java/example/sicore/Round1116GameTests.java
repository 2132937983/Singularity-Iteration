package example.sicore;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.16: stacks above 99 only for the SI coin and only in containers that keep
 * large stacks (vanilla containers and the SI storage box; the global container mixin was removed), a
 * 9,999 coin stack and an old stack with {@code max_stack_size} above 99 still save and load, and the
 * menu data slots carry a full int (the oil rig panel showed wrong drill coordinates beyond ±32,767).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1116GameTests {
    private Round1116GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) {
        try {
            return Class.forName(COMMON + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static ItemStack coin(GameTestHelper h, int count) {
        Item item = BuiltInRegistries.ITEM.get(rl("normal/item_coin"));
        h.assertTrue(item != Items.AIR, "coin not registered");
        return new ItemStack(item, count);
    }

    /** A container with a limit of its own (like a machine slot): the coin keeps that limit. */
    private static final class SmallContainer extends SimpleContainer {
        SmallContainer() { super(1); }
        @Override public int getMaxStackSize() { return 16; }
    }

    // ------------------------------------------------------------------ large stacks

    /** In a plain container and slot the coin stacks to 9,999; other items keep the vanilla limit. */
    @GameTest(batch = "round1116_stacks", template = "empty", timeoutTicks = 20)
    public static void onlyTheCoinStacksAbove99(GameTestHelper h) {
        ItemStack coin = coin(h, 1);
        SimpleContainer plain = new SimpleContainer(1);
        Slot slot = new Slot(plain, 0, 0, 0);
        h.assertTrue(plain.getMaxStackSize(coin) == 9999, "coin in a plain container: " + plain.getMaxStackSize(coin));
        h.assertTrue(slot.getMaxStackSize(coin) == 9999, "coin in a plain slot: " + slot.getMaxStackSize(coin));

        ItemStack stone = new ItemStack(Items.STONE);
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
        h.assertTrue(plain.getMaxStackSize() == 99, "plain container limit: " + plain.getMaxStackSize());
        h.assertTrue(plain.getMaxStackSize(stone) == 64 && slot.getMaxStackSize(stone) == 64, "stone limit changed");
        h.assertTrue(plain.getMaxStackSize(pearl) == 16 && slot.getMaxStackSize(pearl) == 16, "ender pearl limit changed");

        // Up to 0.1.10.15 any item whose max_stack_size was set above 99 got 9,999 in every container.
        ItemStack big = new ItemStack(Items.STONE);
        big.set(DataComponents.MAX_STACK_SIZE, 500);
        h.assertTrue(plain.getMaxStackSize(big) == 99, "max_stack_size 500 in a plain container: " + plain.getMaxStackSize(big));
        h.assertTrue(slot.getMaxStackSize(big) == 99, "max_stack_size 500 in a plain slot: " + slot.getMaxStackSize(big));

        SmallContainer small = new SmallContainer();
        h.assertTrue(small.getMaxStackSize(coin) == 16, "coin in a 16 container: " + small.getMaxStackSize(coin));
        h.assertTrue(new Slot(small, 0, 0, 0).getMaxStackSize(coin) == 16, "coin in a 16 slot");

        // A container of another mod may cut a stack to the plain limit in setItem: the coin keeps 99 there.
        ForeignContainer foreign = new ForeignContainer();
        h.assertTrue(foreign.getMaxStackSize(coin) == 99, "coin in a foreign container: " + foreign.getMaxStackSize(coin));
        h.assertTrue(new Slot(foreign, 0, 0, 0).getMaxStackSize(coin) == 99, "coin in a foreign slot");
        h.succeed();
    }

    /** A container of another mod with the old style of setItem (cuts to the plain limit). */
    private static final class ForeignContainer extends SimpleContainer {
        ForeignContainer() { super(1); }
        @Override public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        }
    }

    /** The SI storage box keeps a 9,999 coin stack put in through its menu slot (it used to cut it to 99). */
    @GameTest(batch = "round1116_stacks", template = "empty", timeoutTicks = 20)
    public static void storageBoxKeepsACoinStack(GameTestHelper h) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(1, 2, 1);
        var block = BuiltInRegistries.BLOCK.get(rl("build/block_wood_storage"));
        h.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR, "wooden storage box not registered");
        h.setBlock(pos, block);
        if (!(h.getBlockEntity(pos) instanceof net.minecraft.world.Container box)) throw new AssertionError("storage box has no container");
        Slot slot = new Slot(box, 0, 0, 0);
        ItemStack coins = coin(h, 9999);
        h.assertTrue(slot.getMaxStackSize(coins) == 9999, "storage box slot limit for coins: " + slot.getMaxStackSize(coins));
        ItemStack left = slot.safeInsert(coins);
        h.assertTrue(left.isEmpty() && box.getItem(0).getCount() == 9999, "storage box holds " + box.getItem(0).getCount() + ", left " + left.getCount());
        ItemStack stone = new ItemStack(Items.STONE, 64);
        box.setItem(1, stone);
        h.assertTrue(box.getItem(1).getCount() == 64, "stone in the storage box: " + box.getItem(1).getCount());
        h.succeed();
    }

    /** The coin takes 9,999 in the player inventory, and a 9,999 stack survives save and load. */
    @GameTest(batch = "round1116_stacks", template = "empty", timeoutTicks = 20)
    public static void coinStackSavesAndLoads(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        h.assertTrue(player.getInventory().add(coin(h, 9000)), "inventory refused 9,000 coins");
        int stacks = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) stacks++;
        }
        h.assertTrue(stacks == 1, "9,000 coins took " + stacks + " slots");

        var registries = h.getLevel().registryAccess();
        ItemStack stack = coin(h, 9999);
        Tag saved = stack.save(registries);
        ItemStack loaded = ItemStack.parse(registries, saved).orElse(ItemStack.EMPTY);
        h.assertTrue(loaded.getItem() == stack.getItem() && loaded.getCount() == 9999, "loaded " + loaded);

        // A stack saved by an earlier version with max_stack_size above 99 still loads (the vanilla
        // component codec allows 1 to 99 and would drop the whole stack).
        ItemStack old = new ItemStack(Items.STONE, 7);
        old.set(DataComponents.MAX_STACK_SIZE, 500);
        ItemStack oldLoaded = ItemStack.parse(registries, old.save(registries)).orElse(ItemStack.EMPTY);
        h.assertTrue(oldLoaded.is(Items.STONE) && oldLoaded.getCount() == 7, "old stack with max_stack_size 500 lost: " + oldLoaded);
        h.succeed();
    }

    // ------------------------------------------------------------------ machine item contents

    /** A wrenched coolant injector used as a material returns its coolant reserve (rci_reserve). */
    @GameTest(batch = "round1116_contents", template = "empty", timeoutTicks = 20)
    public static void injectorReserveIsReturned(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        Item injector = BuiltInRegistries.ITEM.get(rl("producer/block_redstone_reactor_coolant_injector"));
        h.assertTrue(injector != Items.AIR, "coolant injector not registered");
        net.minecraft.nbt.ListTag reserve = new net.minecraft.nbt.ListTag();
        reserve.add(new ItemStack(Items.REDSTONE_BLOCK, 40).save(registries));
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putString("id", "mio_icif:redstone_reactor_coolant_injector");
        data.put("rci_reserve", reserve);
        ItemStack stack = new ItemStack(injector);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
        @SuppressWarnings("unchecked")
        List<ItemStack> contents = (List<ItemStack>) call(common("block.MachineContents"), "of", stack, registries);
        int blocks = contents.stream().filter(s -> s.is(Items.REDSTONE_BLOCK)).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(blocks == 40, "reserve returned: " + blocks + " of 40 redstone blocks");
        h.succeed();
    }

    // ------------------------------------------------------------------ wide data slots

    /** Values beyond the 16-bit range reach the client data unchanged (sent as signed shorts). */
    @GameTest(batch = "round1116_data", template = "empty", timeoutTicks = 20)
    public static void wideDataSlotsCarryAFullInt(GameTestHelper h) {
        int[] values = {0, 1, 32767, 32768, 65535, 65536, -1, -32768, -32769, 30_000_000, -30_000_000,
                Integer.MAX_VALUE, Integer.MIN_VALUE};
        SimpleContainerData server = new SimpleContainerData(values.length);
        SimpleContainerData client = new SimpleContainerData(values.length);
        for (int i = 0; i < values.length; i++) server.set(i, values[i]);
        @SuppressWarnings("unchecked")
        List<DataSlot> out = (List<DataSlot>) call(common("menu.base.WideDataSlots"), "of", (ContainerData) server);
        @SuppressWarnings("unchecked")
        List<DataSlot> in = (List<DataSlot>) call(common("menu.base.WideDataSlots"), "of", (ContainerData) client);
        h.assertTrue(out.size() == 2 * values.length && in.size() == out.size(), "slot count " + out.size());
        // Vanilla writes a data slot as a short; the client receives it sign-extended.
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < out.size(); i++) in.get(i).set((short) out.get(i).get());
        }
        for (int i = 0; i < values.length; i++) {
            h.assertTrue(client.get(i) == values[i], "value " + i + ": sent " + values[i] + ", got " + client.get(i));
        }
        h.succeed();
    }

    /** The oil rig panel (not a base menu) sends its data with wide slots. */
    @GameTest(batch = "round1116_data", template = "empty", timeoutTicks = 20)
    public static void oilRigPanelUsesWideSlots(GameTestHelper h) {
        try {
            var method = common("menu.oilrig.OilRigPanelMenu").getDeclaredMethod("addDataSlots", ContainerData.class);
            h.assertTrue(method.getDeclaringClass().getSimpleName().equals("OilRigPanelMenu"), "not overridden");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("OilRigPanelMenu does not override addDataSlots", e);
        }
        h.succeed();
    }
}
