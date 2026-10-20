package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * IC2 1.12 {@code TileEntityAbstractRCI}: the Redstone (RSH) and Lapis (LZH) Coolant Injectors refill the
 * condensators of the reactor (or reactor chamber) behind their front, one block and 1,000 EU per
 * condensator more than 85% full; the 9-slot layout keeps the coolant of injectors saved before.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CoolantInjectorGameTests {
    private CoolantInjectorGameTests() {}

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(si(path)); }

    private static final String RSH = "producer/block_redstone_reactor_coolant_injector";
    private static final String LZH = "producer/block_lapis_reactor_coolant_injector";
    private static final String RSH_CELL = "reactor/item_reactor_condensator";
    private static final String LZH_CELL = "reactor/item_reactor_condensator_lap";

    private static ItemStack condensator(String path, int heat) {
        ItemStack stack = new ItemStack(item(path));
        call(stack.getItem(), "setStoredHeat", stack, heat);
        return stack;
    }

    private static int heat(ItemStack stack) {
        return (Integer) call(stack.getItem(), "getStoredHeat", stack);
    }

    private static BlockEntity place(GameTestHelper h, BlockPos pos, String block, Direction front) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(si(block)).defaultBlockState().setValue(DirectionalBlock.FACING, front));
        return h.getBlockEntity(pos);
    }

    private static IItemHandlerModifiable reactor(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(si("generator/block_nuclear_reactor_generator")));
        return (IItemHandlerModifiable) call(h.getBlockEntity(pos), "getReactorItemHandler");
    }

    private static void power(BlockEntity injector, long eu) {
        call(call(injector, "getEnergyStorageInternal"), "setStored", eu);
    }

    private static IItemHandler items(BlockEntity be) { return (IItemHandler) call(be, "getItemHandler"); }

    /** Removes the test's reactor parts: a live reactor marks itself changed every tick and would weigh on later tests. */
    private static void clear(GameTestHelper h, BlockPos... positions) {
        for (BlockPos pos : positions) h.setBlock(pos, Blocks.AIR);
    }

    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 60)
    public static void rshRefillsCondensatorsOverEightyFivePercent(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        IItemHandlerModifiable parts = reactor(h, core);
        parts.setStackInSlot(0, condensator(RSH_CELL, 18_000));   // 90%: refilled
        parts.setStackInSlot(1, condensator(RSH_CELL, 17_000));   // exactly 85%: not yet
        parts.setStackInSlot(2, condensator(LZH_CELL, 95_000));   // another kind: not this injector's
        // front east: the reactor is behind it, to the west
        BlockEntity injector = place(h, core.east(), RSH, Direction.EAST);
        power(injector, 48_000L);
        h.assertTrue(items(injector).insertItem(0, new ItemStack(Items.REDSTONE_BLOCK, 2), false).isEmpty(), "redstone blocks refused");
        h.assertTrue(!items(injector).insertItem(1, new ItemStack(Items.LAPIS_BLOCK), true).isEmpty(), "RSH takes lapis blocks");
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(heat(parts.getStackInSlot(0)) == 0, "90% condensator not refilled: " + heat(parts.getStackInSlot(0)));
            h.assertTrue(heat(parts.getStackInSlot(1)) == 17_000 && heat(parts.getStackInSlot(2)) == 95_000, "refilled the wrong condensators");
            h.assertTrue((Integer) call(injector, "getCoolantCount") == 1, "coolant left " + call(injector, "getCoolantCount"));
            long eu = (Long) call(call(injector, "getEnergyStorage"), "getAmount");
            h.assertTrue(eu == 47_000L, "EU left " + eu);
        }).thenExecute(() -> clear(h, core, core.east())).thenSucceed();
    }

    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 60)
    public static void lzhRefillsLapisCondensatorsThroughAChamber(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        IItemHandlerModifiable parts = reactor(h, core);
        h.setBlock(core.south(), BuiltInRegistries.BLOCK.get(si("reactor/block_reactor_chamber")));
        parts.setStackInSlot(0, condensator(LZH_CELL, 90_000));
        // behind the chamber, front south
        BlockEntity injector = place(h, core.south(2), LZH, Direction.SOUTH);
        power(injector, 48_000L);
        items(injector).insertItem(0, new ItemStack(Items.LAPIS_BLOCK, 3), false);
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(heat(parts.getStackInSlot(0)) == 0, "lapis condensator not refilled: " + heat(parts.getStackInSlot(0)));
            h.assertTrue((Integer) call(injector, "getCoolantCount") == 2, "coolant left " + call(injector, "getCoolantCount"));
        }).thenExecute(() -> clear(h, core, core.south(), core.south(2))).thenSucceed();
    }

    /** Remaining uses of a fuel rod, read from the component itself (a fresh rod saves no component patch). */
    private static int fuelUses(GameTestHelper h, ItemStack rod) {
        var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(si("fuel_rod_durability"));
        return (Integer) call(rod.get(type), "remainingUses");
    }

    /**
     * The reported setup: an LZH with its front against a reactor chamber, a reactor at feed rate 16 (16
     * cycles in one operation) and a condensator just under 85%. It is refilled between the feeds of
     * that operation: right after it the condensator holds only the heat of the feeds after the refill.
     * Without that it would be over 85% (or already emptied by the next tick) at that moment.
     */
    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 80)
    public static void lzhFrontAgainstAChamberRefillsBetweenFeeds(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        IItemHandlerModifiable parts = reactor(h, core);
        BlockEntity reactor = h.getBlockEntity(core);
        h.setBlock(core.south(), BuiltInRegistries.BLOCK.get(si("reactor/block_reactor_chamber")));
        call(reactor, "setFeedRate", 16);
        parts.setStackInSlot(10, new ItemStack(item("reactor/item_reactor_uranium_simple")));
        parts.setStackInSlot(11, condensator(LZH_CELL, 84_999));
        BlockEntity injector = place(h, core.south(2), LZH, Direction.NORTH);   // front against the chamber
        power(injector, 48_000L);
        items(injector).insertItem(0, new ItemStack(Items.LAPIS_BLOCK, 10), false);
        h.setBlock(core.above(), Blocks.REDSTONE_BLOCK);
        int uses = fuelUses(h, parts.getStackInSlot(10));
        h.assertTrue(((java.util.List<?>) call(reactor, "coolantInjectors")).contains(injector), "the reactor does not see the injector");
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(uses - fuelUses(h, parts.getStackInSlot(10)) == 16, "waiting for the first operation: used "
                + (uses - fuelUses(h, parts.getStackInSlot(10))) + " of " + uses + ", condensator " + heat(parts.getStackInSlot(11))
                + ", slot 10 " + parts.getStackInSlot(10) + ", EU/t " + call(reactor, "getCurrentEnergyGeneration")
                + ", heat " + call(reactor, "getCurrentHeat"));
            int heat = heat(parts.getStackInSlot(11));
            h.assertTrue(heat > 0 && heat < 85_000, "condensator after the operation: " + heat + " (not refilled between feeds)");
            h.assertTrue((Integer) call(injector, "getCoolantCount") == 9, "lapis left " + call(injector, "getCoolantCount"));
        }).thenExecute(() -> clear(h, core.above(), core, core.south(), core.south(2))).thenSucceed();
    }

    /** A side against the reactor works too (IC2 needs the back). */
    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 40)
    public static void aSideAgainstTheReactorWorks(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        IItemHandlerModifiable parts = reactor(h, core);
        parts.setStackInSlot(0, condensator(RSH_CELL, 19_000));
        BlockEntity injector = place(h, core.east(), RSH, Direction.UP);   // the reactor is on its west side
        power(injector, 48_000L);
        items(injector).insertItem(0, new ItemStack(Items.REDSTONE_BLOCK, 2), false);
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(heat(parts.getStackInSlot(0)) == 0, "not refilled from the side: " + heat(parts.getStackInSlot(0)));
            h.assertTrue((Integer) call(injector, "getCoolantCount") == 1, "coolant left " + call(injector, "getCoolantCount"));
        }).thenExecute(() -> clear(h, core, core.east())).thenSucceed();
    }

    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 20)
    public static void energyFacesAndUpgradesFollowIc2(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        reactor(h, core);
        BlockEntity injector = place(h, core.east(), RSH, Direction.EAST);
        h.assertTrue(!(Boolean) call(injector, "canConnect", Direction.WEST), "connects to the reactor");
        h.assertTrue((Boolean) call(injector, "canConnect", Direction.EAST) && (Boolean) call(injector, "canConnect", Direction.UP),
            "free faces closed");
        h.assertTrue((Long) call(call(injector, "getEnergyStorage"), "getCapacity") == 48_000L, "capacity");
        IItemHandler items = items(injector);
        h.assertTrue(items.getSlots() == 13, "slots " + items.getSlots());
        h.assertTrue(items.isItemValid(9, new ItemStack(item("upgrade/pulling_upgrade"))), "pulling upgrade refused");
        h.assertTrue(!items.isItemValid(9, new ItemStack(item("upgrade/overclocker_upgrade"))), "overclocker accepted");
        clear(h, core, core.east());
        h.succeed();
    }

    /** An injector saved with 53 coolant slots and a battery slot: 9 stacks in the slots, the rest in the reserve (used first). */
    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 60)
    public static void injectorsSavedBeforeKeepTheirCoolant(GameTestHelper h) {
        BlockPos core = new BlockPos(2, 2, 2);
        IItemHandlerModifiable parts = reactor(h, core);
        parts.setStackInSlot(0, condensator(RSH_CELL, 20_000));
        BlockEntity injector = place(h, core.east(), RSH, Direction.EAST);
        var registries = h.getLevel().registryAccess();
        ListTag list = new ListTag();
        for (int slot = 0; slot < 12; slot++) list.add(entry(slot, new ItemStack(Items.REDSTONE_BLOCK, 64), registries));
        list.add(entry(53, new ItemStack(Items.STICK), registries));
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Size", 54);
        inventory.put("Items", list);
        CompoundTag tag = injector.saveWithoutMetadata(registries);
        tag.put("inventory", inventory);
        injector.loadWithComponents(tag, registries);
        h.assertTrue((Integer) call(injector, "getCoolantCount") == 576, "slots hold " + call(injector, "getCoolantCount"));
        h.assertTrue((Integer) call(injector, "getReserveCoolantCount") == 192, "reserve holds " + call(injector, "getReserveCoolantCount"));
        power(injector, 48_000L);
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(heat(parts.getStackInSlot(0)) == 0, "not refilled");
            h.assertTrue((Integer) call(injector, "getReserveCoolantCount") == 191 && (Integer) call(injector, "getCoolantCount") == 576,
                "the reserve was not used first");
        }).thenExecute(() -> clear(h, core, core.east())).thenSucceed();
    }

    @GameTest(batch = "coolant_injector", template = "empty", timeoutTicks = 20)
    public static void breakingDropsTheReserve(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        BlockEntity injector = place(h, at, RSH, Direction.EAST);
        var registries = h.getLevel().registryAccess();
        ListTag list = new ListTag();
        for (int slot = 0; slot < 10; slot++) list.add(entry(slot, new ItemStack(Items.REDSTONE_BLOCK, 64), registries));
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Size", 54);
        inventory.put("Items", list);
        CompoundTag tag = injector.saveWithoutMetadata(registries);
        tag.put("inventory", inventory);
        injector.loadWithComponents(tag, registries);
        h.getLevel().destroyBlock(h.absolutePos(at), false);
        int dropped = 0;
        for (ItemEntity drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(at)).inflate(3))) {
            if (drop.getItem().is(Items.REDSTONE_BLOCK)) dropped += drop.getItem().getCount();
        }
        h.assertTrue(dropped == 640, "dropped " + dropped + " of 640 redstone blocks");
        h.succeed();
    }

    private static CompoundTag entry(int slot, ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Slot", slot);
        tag.put("Item", stack.save(registries));
        return tag;
    }
}
