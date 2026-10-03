package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.List;

/**
 * 0.1.7.28: advanced-miner filters survive save/load and client updates, wrench pick-up keeps a
 * machine's full settings (and its inventory) and restores them on placement, block-state
 * settings follow, and filled storage boxes cannot be nested.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round28GameTests {
    private Round28GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }
    private static Object field(Object target, String name) { return ArmoryServiceGameTests.field(target, name); }

    private static ItemStack filter(BlockEntity miner, int i) { return (ItemStack) call(miner, "getFilterStack", i); }

    @GameTest(batch = "round28_miner", template = "empty", timeoutTicks = 40)
    public static void minerFiltersSurviveSaveLoadAndClientUpdates(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 1), c = new BlockPos(5, 1, 1);
        for (BlockPos p : List.of(a, b, c)) h.setBlock(p, block("producer/block_advanced_miner_elc"));
        BlockEntity miner = h.getBlockEntity(a);
        call(miner, "setFilterStack", 0, new ItemStack(Items.DIRT));
        call(miner, "setFilterStack", 7, new ItemStack(Items.DIAMOND_ORE));
        call(miner, "setWhitelistMode", true);
        var registries = h.getLevel().registryAccess();
        // disk round trip
        CompoundTag saved = miner.saveWithFullMetadata(registries);
        h.getBlockEntity(b).loadWithComponents(saved, registries);
        h.assertTrue(filter(h.getBlockEntity(b), 0).is(Items.DIRT) && filter(h.getBlockEntity(b), 7).is(Items.DIAMOND_ORE),
            "Filters survive a save/load (were stored as bare {Slot:n})");
        h.assertTrue((boolean) call(h.getBlockEntity(b), "isWhitelistMode"), "Whitelist mode survives");
        // client update packet path: onDataPacket -> loadWithComponents(getUpdateTag())
        h.getBlockEntity(c).loadWithComponents(miner.getUpdateTag(registries), registries);
        h.assertTrue(filter(h.getBlockEntity(c), 0).is(Items.DIRT), "A client update keeps the ghost filter icons");
        h.succeed();
    }

    private static List<ItemEntity> drops(GameTestHelper h, BlockPos at) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(at)).inflate(2));
    }

    private static void wrench(GameTestHelper h, BlockPos at, net.minecraft.server.level.ServerPlayer player) {
        ItemStack wrench = item("item_tool_wrench");
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        Object wrenchItem = wrench.getItem();
        call(wrenchItem, "dismantleBlock", h.getLevel(), h.absolutePos(at), h.getLevel().getBlockState(h.absolutePos(at)), player, wrench, InteractionHand.MAIN_HAND);
    }

    private static ItemStack pickUp(GameTestHelper h, BlockPos at, Block expected) {
        for (ItemEntity e : drops(h, at)) if (e.getItem().is(expected.asItem())) { ItemStack s = e.getItem().copy(); e.discard(); return s; }
        throw new AssertionError("No machine item dropped");
    }

    private static void place(GameTestHelper h, net.minecraft.server.level.ServerPlayer player, ItemStack stack, BlockPos at) {
        h.setBlock(at.below(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos below = h.absolutePos(at.below());
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
    }

    @GameTest(batch = "round28_wrench", template = "reactor_loop", timeoutTicks = 80)
    public static void wrenchPickUpKeepsTheAdvancedMinersSettingsAndContents(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2), again = new BlockPos(6, 2, 2);
        Block minerBlock = block("producer/block_advanced_miner_elc");
        h.setBlock(at, minerBlock);
        BlockEntity miner = h.getBlockEntity(at);
        call(miner, "setFilterStack", 0, new ItemStack(Items.DIRT));
        call(miner, "setFilterStack", 3, new ItemStack(Items.IRON_ORE));
        call(miner, "setWhitelistMode", true);
        call(miner, "setSilkTouchMode", true);
        var items = (IItemHandlerModifiable) field(miner, "itemHandler");
        items.setStackInSlot(1, item("item_tool_od_scanner"));
        items.setStackInSlot(2, new ItemStack(BuiltInRegistries.ITEM.get(id("upgrade/overclocker_upgrade")), 2));
        ((AbstractEnergyBlockEntity) miner).getEnergyStorageInternal().setStored(4321);
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(h.absolutePos(at.north(2))));
        wrench(h, at, player);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(at)).isAir(), "The miner was picked up");
        ItemStack machine = pickUp(h, at, minerBlock);
        h.assertTrue(drops(h, at).stream().noneMatch(e -> e.getItem().is(BuiltInRegistries.ITEM.get(id("upgrade/overclocker_upgrade")))),
            "Upgrades travel inside the machine item instead of spilling");
        CompoundTag data = machine.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        h.assertTrue(!data.contains("tipPos") && !data.contains("progress"), "World-bound runtime state is stripped: " + data.getAllKeys());
        place(h, player, machine, again);
        BlockEntity placed = h.getBlockEntity(again);
        h.assertTrue(filter(placed, 0).is(Items.DIRT) && filter(placed, 3).is(Items.IRON_ORE), "Filters restored");
        h.assertTrue((boolean) call(placed, "isWhitelistMode") && (boolean) call(placed, "isSilkTouchMode"), "Modes restored");
        var placedItems = (IItemHandlerModifiable) field(placed, "itemHandler");
        h.assertTrue(!placedItems.getStackInSlot(1).isEmpty(), "Scanner restored");
        h.assertTrue(placedItems.getStackInSlot(2).getCount() == 2, "Upgrades restored");
        h.assertTrue(((AbstractEnergyBlockEntity) placed).getEnergyStorage().getAmount() == 4321, "Energy restored, got "
            + ((AbstractEnergyBlockEntity) placed).getEnergyStorage().getAmount());
        h.succeed();
    }

    @GameTest(batch = "round28_wrench", template = "reactor_loop", timeoutTicks = 80)
    public static void wrenchPickUpKeepsModesOfOtherMachines(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // metal former: mode is also shown by the block state
        BlockPos mf = new BlockPos(2, 2, 2), mf2 = new BlockPos(6, 2, 2);
        Block former = block("producer/block_metal_former");
        h.setBlock(mf, former);
        BlockEntity f = h.getBlockEntity(mf);
        call(f, "setMode", Enum.valueOf(modeEnum(f), "CUTTING"));
        player.setPos(Vec3.atCenterOf(h.absolutePos(mf.north(2))));
        wrench(h, mf, player);
        place(h, player, pickUp(h, mf, former), mf2);
        BlockEntity f2 = h.getBlockEntity(mf2);
        h.assertTrue(String.valueOf(call(f2, "getMode")).equals("CUTTING"), "Metal former mode restored, got " + call(f2, "getMode"));
        String stateMode = h.getLevel().getBlockState(h.absolutePos(mf2)).getValues().entrySet().stream()
            .filter(e -> e.getKey().getName().equals("mode")).map(e -> String.valueOf(e.getValue())).findFirst().orElse("?");
        h.assertTrue(stateMode.equalsIgnoreCase("cutting"), "Block state shows the restored mode, got " + stateMode);
        // MFSU: energy and redstone mode
        BlockPos box = new BlockPos(2, 2, 5), box2 = new BlockPos(6, 2, 5);
        Block mfsu = block("wiring/block_mfsu");
        h.setBlock(box, mfsu);
        var storage = (AbstractEnergyStorageBlockEntity) h.getBlockEntity(box);
        storage.getEnergyStorageInternal().setStored(1_234_567);
        storage.setRedstoneMode((byte) 3);
        player.setPos(Vec3.atCenterOf(h.absolutePos(box.north(2))));
        wrench(h, box, player);
        place(h, player, pickUp(h, box, mfsu), box2);
        var storage2 = (AbstractEnergyStorageBlockEntity) h.getBlockEntity(box2);
        h.assertTrue(storage2.getEnergyStorage().getAmount() == 1_234_567, "MFSU energy restored, got " + storage2.getEnergyStorage().getAmount());
        h.assertTrue(storage2.getRedstoneMode() == 3, "MFSU redstone mode restored, got " + storage2.getRedstoneMode());
        h.succeed();
    }

    @GameTest(batch = "round28_wrench", template = "reactor_loop", timeoutTicks = 80)
    public static void wrenchPickUpKeepsTheTransformerMode(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = new BlockPos(2, 2, 2), again = new BlockPos(6, 2, 2);
        Block xfmr = block("wiring/transformer_lv_mv");
        h.setBlock(at, xfmr);
        BlockEntity t = h.getBlockEntity(at);
        boolean engine = t.getClass().getSimpleName().equals("IndependentTransformerBlockEntity");
        if (engine) call(t, "setSavedMode", 2);
        player.setPos(Vec3.atCenterOf(h.absolutePos(at.north(2))));
        wrench(h, at, player);
        ItemStack picked = pickUp(h, at, xfmr);
        place(h, player, picked, again);
        BlockEntity t2 = h.getBlockEntity(again);
        if (engine) h.assertTrue((int) call(t2, "savedMode") == 2, "Transformer step mode restored, got " + call(t2, "savedMode"));
        h.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<Enum> modeEnum(BlockEntity former) {
        for (var m : former.getClass().getMethods()) if (m.getName().equals("setMode") && m.getParameterCount() == 1) return (Class<Enum>) m.getParameterTypes()[0];
        throw new AssertionError("no setMode");
    }

    @GameTest(batch = "round28_storage", template = "empty")
    public static void filledStorageBoxesCannotBeNested(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, block("build/block_iron_storage"));
        var be = h.getBlockEntity(at);
        var handler = (net.neoforged.neoforge.items.IItemHandler) call(be, "getItemHandler");
        ItemStack filled = item("build/block_iron_storage");
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString());
        filled.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        h.assertTrue(!handler.isItemValid(0, filled), "A filled storage box is refused");
        h.assertTrue(handler.isItemValid(0, item("build/block_iron_storage")), "An empty one is fine");
        h.succeed();
    }
}
