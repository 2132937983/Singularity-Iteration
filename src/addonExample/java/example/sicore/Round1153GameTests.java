package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.item.AbstractBattery;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.3: the duplication, exploit, crash and leak fixes of the 2026-11-16 audit (B1 and B2).
 * Content classes are reached by reflection (the example addon compiles against the core jar only).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1153GameTests {
    private Round1153GameTests() {}

    private static final BlockPos AT = new BlockPos(2, 2, 2);
    /** The content package, split so the architecture check (no common references in the example) holds. */
    private static final String COMMON = "com.miophas.singularity_iteration." + "common.";

    private static Object call(Object target, String name, Object... args) {
        return ArmoryServiceGameTests.call(target, name, args);
    }

    private static void setField(Object target, String name, Object value) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new AssertionError(name, e);
            }
        }
        throw new AssertionError("no field " + name);
    }

    private static Object staticField(String type, String name) {
        try {
            Field f = Class.forName(type).getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(type + "." + name, e);
        }
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static BlockEntity place(GameTestHelper h, String block) {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", block)));
        return h.getBlockEntity(AT);
    }

    private static ServerPlayer player(GameTestHelper h) {
        return ArmoryServiceGameTests.player(h);
    }

    // ------------------------------------------------------------------ B1

    /** E-1: a node linked to its own position gained what it sent each tick. */
    @GameTest(template = "reactor_loop", timeoutTicks = 60)
    public static void wirelessNodeNeverFeedsItself(GameTestHelper h) {
        BlockEntity node = place(h, "wiring/block_wireless_power_transmission_node");
        BlockPos own = h.absolutePos(AT);
        call(node, "setTargetPosition", own);
        h.assertTrue(call(node, "getTargetPosition") == null, "A link to the node's own position is refused");
        setField(node, "targetPosition", own);   // a link saved by an older version
        ((AbstractEnergyBlockEntity) node).getEnergyStorageInternal().setEnergy(1000);
        h.runAfterDelay(20, () -> {
            long now = ((AbstractEnergyBlockEntity) node).getEnergyStorageInternal().getAmount();
            h.assertTrue(now <= 1000, "A node linked to itself must not gain energy, has " + now + " EU (was 1000)");
            h.succeed();
        });
    }

    /** M-1: the Blueprint Builder charged one item per block whatever the block held. */
    @GameTest(template = "empty")
    public static void builderChargesWhatABlockHolds(GameTestHelper h) {
        Class<?> builder = type(COMMON + "builder.BlueprintBuilderBlockEntity");
        java.util.function.ToIntFunction<BlockState> amount = s -> (int) call(builder, "amount", s);
        java.util.function.UnaryOperator<BlockState> target = s -> (BlockState) call(builder, "target", s);
        h.assertTrue(amount.applyAsInt(Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 4)) == 4, "Four candles cost four");
        h.assertTrue(amount.applyAsInt(Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, 3)) == 3, "Three pickles cost three");
        h.assertTrue(amount.applyAsInt(Blocks.TURTLE_EGG.defaultBlockState().setValue(BlockStateProperties.EGGS, 4)) == 4, "Four eggs cost four");
        h.assertTrue(amount.applyAsInt(Blocks.PINK_PETALS.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, 4)) == 4, "Four petals cost four");
        h.assertTrue(amount.applyAsInt(Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 6)) == 6, "Six layers cost six");
        h.assertTrue(amount.applyAsInt(Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE,
            net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)) == 2, "A double slab still costs two");
        h.assertTrue(amount.applyAsInt(Blocks.STONE.defaultBlockState()) == 1, "A plain block costs one");

        h.assertTrue(target.apply(Blocks.COMPOSTER.defaultBlockState().setValue(BlockStateProperties.LEVEL_COMPOSTER, 8))
            .getValue(BlockStateProperties.LEVEL_COMPOSTER) == 0, "A full composter is placed empty");
        h.assertTrue(target.apply(Blocks.BEEHIVE.defaultBlockState().setValue(BlockStateProperties.LEVEL_HONEY, 5))
            .getValue(BlockStateProperties.LEVEL_HONEY) == 0, "A beehive is placed without honey");
        h.assertTrue(target.apply(Blocks.LAVA_CAULDRON.defaultBlockState()).is(Blocks.CAULDRON), "A lava cauldron is placed empty");
        h.assertTrue(target.apply(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)).is(Blocks.CAULDRON),
            "A water cauldron is placed empty");
        h.assertTrue(target.apply(Blocks.POTTED_DANDELION.defaultBlockState()).is(Blocks.FLOWER_POT), "A potted plant is placed as a pot");
        h.assertTrue(target.apply(Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7))
            .getValue(BlockStateProperties.AGE_7) == 0, "A ripe crop is placed unripe");
        h.assertTrue(!target.apply(Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.BERRIES, true))
            .getValue(BlockStateProperties.BERRIES), "Cave vines are placed without berries");
        h.assertTrue(!target.apply(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true))
            .getValue(BlockStateProperties.WATERLOGGED), "Waterlogged blocks are still placed dry");
        h.assertTrue(target.apply(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST))
            .getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, "Other properties are kept");
        h.succeed();
    }

    /** N-1: the held toolbox could be picked up while its menu was open. */
    @GameTest(template = "empty")
    public static void toolboxMenuLocksTheHeldSlot(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().selected = 3;
        ItemStack held = new ItemStack(Items.STICK);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        AbstractContainerMenu menu;
        try {
            menu = (AbstractContainerMenu) type(COMMON + "menu.tool.mio_icif_toolbox_menu")
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, ItemStack.class, int.class, InteractionHand.class)
                .newInstance(1, p.getInventory(), held, 9, InteractionHand.MAIN_HAND);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("toolbox menu", e);
        }
        Slot heldSlot = null, other = null;
        for (Slot s : menu.slots) {
            if (s.container != p.getInventory()) continue;
            if (s.getContainerSlot() == 3) heldSlot = s;
            if (s.getContainerSlot() == 4) other = s;
        }
        h.assertTrue(heldSlot != null && other != null, "The menu shows the hotbar");
        h.assertTrue(!heldSlot.mayPickup(p) && !heldSlot.mayPlace(new ItemStack(Items.DIRT)), "The held slot is locked");
        h.assertTrue(other.mayPickup(p), "Other slots stay free");
        p.getInventory().setItem(4, new ItemStack(Items.DIRT));
        menu.clicked(other.index, 3, ClickType.SWAP, p);
        h.assertTrue(p.getInventory().getItem(3) == held, "The number key of the held slot does not move the toolbox");
        h.assertTrue(!menu.stillValid(p), "A menu whose stack is not a toolbox in that hand is not valid (was always true)");
        p.discard();
        h.succeed();
    }

    /** N-2: the Armory remote reached showcases of other players. */
    @GameTest(template = "reactor_loop", timeoutTicks = 40)
    public static void showcaseAnswersOnlyItsOwnerFromAfar(GameTestHelper h) {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armor_showcase")));
        BlockEntity showcase = h.getBlockEntity(AT);
        ServerPlayer owner = player(h);
        ServerPlayer stranger = player(h);   // at the same spot, so the showcase is in range of the stranger's remote
        h.assertTrue(!(boolean) call(showcase, "remoteUsableBy", owner), "A showcase nobody placed or used answers nobody");
        BlockPos at = h.absolutePos(AT);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.getLevel().getBlockState(at).useWithoutItem(h.getLevel(), owner, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        h.assertTrue((boolean) call(showcase, "remoteUsableBy", owner), "The first player to use it by hand owns it");
        h.getLevel().getBlockState(at).useWithoutItem(h.getLevel(), stranger, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        h.assertTrue(!(boolean) call(showcase, "remoteUsableBy", stranger), "A second player does not take it over");
        java.util.List<?> seen = (java.util.List<?>) call(type(COMMON + "armory.ArmoryRemoteService"),
            "showcases", stranger, null);
        h.assertTrue(!seen.contains(showcase), "The stranger's remote does not list it");
        CompoundTag saved = showcase.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.hasUUID("Owner") && saved.getUUID("Owner").equals(owner.getUUID()), "The owner is saved");
        owner.discard();
        stranger.discard();
        h.succeed();
    }

    /** N-3: the harvester menu never became invalid. */
    @GameTest(template = "reactor_loop", timeoutTicks = 40)
    public static void harvesterMenuClosesWhenFarOrGone(GameTestHelper h) {
        BlockEntity harvester = place(h, "producer/block_harvest_elc");
        ServerPlayer p = player(h);
        AbstractContainerMenu menu;
        try {
            Class<?> beType = type(COMMON + "blockentity.producer.mio_icif_harvest_elc");
            menu = (AbstractContainerMenu) type(COMMON + "menu.producer.HarvestElcMenu")
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, beType, net.minecraft.world.inventory.ContainerData.class)
                .newInstance(1, p.getInventory(), harvester, new SimpleContainerData(3));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("harvester menu", e);
        }
        h.assertTrue(menu.stillValid(p), "Valid next to the harvester");
        Vec3 near = p.position();
        p.setPos(near.x + 20, near.y, near.z);
        h.assertTrue(!menu.stillValid(p), "Not valid 20 blocks away (was always true)");
        p.setPos(near.x, near.y, near.z);
        h.setBlock(AT, Blocks.AIR);
        h.assertTrue(!menu.stillValid(p), "Not valid once the harvester is gone");
        p.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ B2

    /** S-1: the auto kit put a dead player back to one health point, so the respawn button did nothing. */
    @GameTest(template = "empty")
    public static void firstAidKitLeavesTheDeadAlone(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack kit = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_electric_first_aid_life_support")));
        AbstractBattery item = (AbstractBattery) kit.getItem();
        item.setEnergy(kit, item.getMaxEnergy(kit));
        call(item.getClass(), "setAutoSupplement", kit, true);
        p.setHealth(10);
        kit.inventoryTick(h.getLevel(), p, 5, false);
        h.assertTrue(p.getHealth() == 11, "The kit heals a living player (health " + p.getHealth() + ")");
        p.setHealth(0);
        kit.inventoryTick(h.getLevel(), p, 5, false);
        h.assertTrue(p.getHealth() == 0, "The kit must not heal a dead player (health " + p.getHealth() + ")");
        p.discard();
        h.succeed();
    }

    /** E-2: the charging map held the player strongly, so it kept a logged-out player and the level. */
    @GameTest(template = "empty")
    @SuppressWarnings("unchecked")
    public static void chargingHoldersAreWeak(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack tool = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_electric_first_aid_life_support")));
        p.setItemInHand(InteractionHand.MAIN_HAND, tool);
        String type = "com.miophas.singularity_iteration.core.runtime.energy.EnergyPackCharging";
        call(type(type), "trackHolder", tool, p);
        Object value = ((Map<ItemStack, ?>) staticField(type, "HOLDERS")).get(tool);
        h.assertTrue(value instanceof WeakReference<?> ref && ref.get() == p, "The holder is kept through a weak reference, got " + value);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        call(type(type), "trackHolder", tool, p);
        h.assertTrue(((Map<ItemStack, ?>) staticField(type, "HOLDERS")).get(tool) == null, "Leaving the hand still forgets it");
        p.discard();
        h.succeed();
    }

    /** E-3: an output whose foreign receipt never came latched the grid-tie interface for good. */
    @GameTest(template = "reactor_loop", timeoutTicks = 40)
    public static void gridTieWritesOffAStaleUncertainOutput(GameTestHelper h) {
        BlockEntity tie = place(h, "energy_converter/energy_converter");
        Object bridge = call(tie, "scexFeBridge");
        call(bridge, "loadUncertainOutput", 100);
        h.assertTrue((int) call(bridge, "uncertainOutput") == 100, "The saved latch loads");
        call(tie, "tick");
        h.assertTrue((int) call(bridge, "uncertainOutput") == 0, "The first tick closes a latch from the save (it never cleared)");
        h.succeed();
    }

    /** M-3: a pump with an uncertain removal never ticked again, across reloads. */
    @GameTest(template = "reactor_loop", timeoutTicks = 40)
    public static void pumpClosesAnUncertainRemoval(GameTestHelper h) {
        BlockEntity pump = place(h, "producer/block_pump_elc");
        CompoundTag open = new CompoundTag();
        open.putLong("Position", h.absolutePos(AT.below()).asLong());
        open.putString("Fluid", "minecraft:water");
        setField(pump, "uncertainRemoval", open);
        h.assertTrue((boolean) call(pump, "hasUncertainRemoval"), "The open removal is set");
        h.runAfterDelay(3, () -> {
            h.assertTrue(!(boolean) call(pump, "hasUncertainRemoval"), "The pump closes the open removal and goes on");
            h.succeed();
        });
    }
}
