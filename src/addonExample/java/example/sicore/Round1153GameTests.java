package example.sicore;

import com.miophas.singularity_iteration.core.api.event.MachineWorkCompleteEvent;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
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

    // ------------------------------------------------------------------ B3 (work cycles, 0.1.11.2 API)

    private static final java.util.Map<BlockPos, Integer> COMPLETES = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean listening;

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((MachineWorkCompleteEvent e) -> COMPLETES.merge(e.getPos().immutable(), 1, Integer::sum));
    }

    private static CompoundTag cycleTag(String key, Object value) {
        CompoundTag tag = new CompoundTag();
        if (value instanceof Boolean b) tag.putBoolean(key, b);
        else tag.putFloat(key, (Float) value);
        return tag;
    }

    @SuppressWarnings("deprecation")
    private static AbstractProcessingMachineBlockEntity macerator(GameTestHelper h, BlockPos at, ItemStack upgrade) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_powder_elc")));
        var m = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        h.assertTrue(m.getItemHandler().insertItem(0, new ItemStack(Items.STONE, 64), false).isEmpty(), "stone was refused");
        if (!upgrade.isEmpty()) h.assertTrue(m.getItemHandler().insertItem(3, upgrade, false).isEmpty(), "the upgrade was refused");
        m.getEnergyStorageInternal().setStored(600);
        return m;
    }

    /** A-1: a machine that ends its operation without finishWork() (here the molecular transformer) now ends its cycles. */
    @GameTest(template = "empty", timeoutTicks = 100)
    @SuppressWarnings("deprecation")
    public static void transformerEndsItsWorkCycles(GameTestHelper h) {
        listen();
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_molecular_transformer")));
        var m = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        BlockPos abs = h.absolutePos(at);
        COMPLETES.remove(abs);
        h.assertTrue(m.getItemHandler().insertItem(0, new ItemStack(Items.DIRT, 3), false).isEmpty(), "dirt was refused");
        h.onEachTick(() -> {
            m.getEnergyStorageInternal().setStored(Math.min(m.getEnergyStorageInternal().getCapacity(), 200_000));
            int clay = m.getItemHandler().getStackInSlot(1).getCount();
            if (clay >= 2) {
                int events = COMPLETES.getOrDefault(abs, 0);
                h.assertTrue(events >= clay, "MachineWorkCompleteEvent came " + events + " times for " + clay + " clay (0.1.11.2: never)");
                h.succeed();
            }
        });
    }

    /** A-5: a forced stop ends the open cycle; the next run starts a new one (it kept the old factors). */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void forcedStopEndsTheCycle(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        var m = macerator(h, at, ExampleCycleUpgrade.configured(cycleTag("duration", 2.0F)));
        h.runAfterDelay(10, () -> {
            h.assertTrue(m.isWorkCycleOpen() && m.getCycleDurationFactor() == 2F && m.getMaxProgress() == 600,
                "the cycle runs with its factor: " + m.getMaxProgress());
            m.forceStopWork();
            h.assertTrue(!m.isWorkCycleOpen() && m.getCycleDurationFactor() == 1F && m.getMaxProgress() == 300,
                "a forced stop ends the cycle: open " + m.isWorkCycleOpen() + ", ticks " + m.getMaxProgress());
            h.succeed();
        });
    }

    /** A-5: a forced start during a hold respects the hold. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void forcedStartRespectsAHold(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        var m = macerator(h, at, ExampleCycleUpgrade.configured(cycleTag("hold", true)));
        h.runAfterDelay(5, () -> {
            m.forceStartWork();
            h.assertTrue(m.getProgress() == 0 && !m.isWorkCycleOpen(), "a forced start skipped the hold: progress " + m.getProgress());
            h.succeed();
        });
    }

    /** A-4: after a hold, a machine whose input is taken out goes back to idle sleep. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void heldMachineSleepsWhenTheInputGoes(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        var m = macerator(h, at, ExampleCycleUpgrade.configured(cycleTag("hold", true)));
        h.runAfterDelay(5, () -> {
            h.assertTrue(!m.isIdleAsleep(), "the held machine is awake");
            m.getItemHandler().extractItem(0, 64, false);
            h.runAfterDelay(50, () -> {
                h.assertTrue(m.isIdleAsleep(), "without input the machine sleeps again (0.1.11.2: awake for good after a hold)");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ B4 (gameplay correctness)

    private static Object enumConstant(String type, String name) {
        for (Object c : type(type).getEnumConstants()) if (((Enum<?>) c).name().equals(name)) return c;
        throw new AssertionError(type + "." + name);
    }

    /** S-3: leggings removed any Speed (potions, beacons); now only their own hidden effect. */
    @GameTest(template = "empty")
    public static void leggingsRemoveOnlyTheirOwnSpeed(GameTestHelper h) {
        ServerPlayer p = player(h);
        Class<?> quantum = type(COMMON + "item.armor.mio_icif_leggings_quantum");
        var speed = net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED;
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(speed, 3600, 1));   // a potion: icon, particles
        call(quantum, "removeOwnSpeed", p);
        h.assertTrue(p.hasEffect(speed), "A Speed potion is kept");
        p.removeEffect(speed);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(speed, 100, 1, false, false, false));
        call(quantum, "removeOwnSpeed", p);
        h.assertTrue(!p.hasEffect(speed), "The leggings' own Speed is removed");
        p.discard();
        h.succeed();
    }

    /** S-6: the treetap's +2 reach is an item attribute modifier of the main hand. */
    @GameTest(template = "empty")
    public static void treetapReachIsAMainHandModifier(GameTestHelper h) {
        ItemStack tap = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_tool_treetap_elc")));
        h.assertTrue(!tap.isEmpty(), "treetap item id");
        var modifiers = tap.getItem().getDefaultAttributeModifiers(tap);
        boolean found = modifiers.modifiers().stream().anyMatch(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE)
            && e.slot() == net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND && e.modifier().amount() == 2.0);
        h.assertTrue(found, "+2 block interaction range in the main hand, got " + modifiers.modifiers());
        h.succeed();
    }

    /** S-7: the quantum helmet eats only food without effects. */
    @GameTest(template = "empty")
    public static void quantumHelmetEatsPlainFoodOnly(GameTestHelper h) {
        ServerPlayer p = player(h);
        Class<?> helmet = type(COMMON + "item.armor.mio_icif_helmet_quantum");
        java.util.function.Predicate<net.minecraft.world.item.Item> plain = i -> (boolean) call(helmet, "plainFood", new ItemStack(i), p);
        h.assertTrue(plain.test(Items.BREAD) && plain.test(Items.COOKED_BEEF) && plain.test(Items.MUSHROOM_STEW), "Plain food is eaten");
        h.assertTrue(!plain.test(Items.GOLDEN_APPLE) && !plain.test(Items.ENCHANTED_GOLDEN_APPLE), "Golden apples are kept");
        h.assertTrue(!plain.test(Items.ROTTEN_FLESH) && !plain.test(Items.SPIDER_EYE) && !plain.test(Items.PUFFERFISH), "Harmful food is not eaten");
        h.assertTrue(!plain.test(Items.CHORUS_FRUIT) && !plain.test(Items.STICK), "Chorus fruit and non-food are not eaten");
        p.discard();
        h.succeed();
    }

    /** S-8: painting keeps the candles of a block. */
    @GameTest(template = "empty")
    public static void painterKeepsCandleCount(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        BlockState four = Blocks.WHITE_CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 4);
        h.setBlock(at, four);
        Object painter = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "build/item_painter_red"));
        h.assertTrue(type(COMMON + "item.build.PainterItem").isInstance(painter), "red painter item id");
        call(painter, "copyBlockStateProperties", four, Blocks.RED_CANDLE.defaultBlockState(), h.getLevel(), h.absolutePos(at));
        BlockState now = h.getBlockState(at);
        h.assertTrue(now.is(Blocks.RED_CANDLE) && now.getValue(BlockStateProperties.CANDLES) == 4, "Four red candles, got " + now);
        h.succeed();
    }

    /** S-5: a rocket keeps its shot settings through a save. */
    @GameTest(template = "empty")
    public static void rocketKeepsItsSettingsThroughASave(GameTestHelper h) {
        ServerPlayer p = player(h);
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "rocket_entity"));
        net.minecraft.world.entity.Entity rocket = type.create(h.getLevel());
        h.assertTrue(rocket != null && rocket.getClass().getSimpleName().equals("mio_icif_rocket_entity"), "rocket entity id, got " + rocket);
        call(rocket, "setTerrainDamage", false);
        call(rocket, "setExplosionPower", 2.0F);
        CompoundTag saved = rocket.saveWithoutId(new CompoundTag());
        net.minecraft.world.entity.Entity loaded = type.create(h.getLevel());
        loaded.load(saved);
        h.assertTrue(!(boolean) ArmoryServiceGameTests.field(loaded, "terrainDamage"), "Terrain damage stays off after a load");
        h.assertTrue((float) ArmoryServiceGameTests.field(loaded, "explosionPower") == 2.0F, "Explosion power is kept");
        p.discard();
        h.succeed();
    }

    /** X-1: a market that changes its currency refunds the resting orders in the old one. */
    @GameTest(template = "empty")
    @SuppressWarnings("unchecked")
    public static void exchangeRefundsInTheOldCurrency(GameTestHelper h) {
        String dataType = COMMON + "exchange.ExchangeData";
        Object d = call(type(dataType), "get", h.getLevel().getServer());
        String id = "si_core_example:currency_switch_test";
        java.util.UUID owner = java.util.UUID.randomUUID();
        Object uu = enumConstant(dataType + "$Currency", "UU"), coin = enumConstant(dataType + "$Currency", "COIN");
        Object book = call(d, "book", id, uu);
        Object orderBook = ArmoryServiceGameTests.field(book, "book");
        Object order;
        try {
            var ctor = type(COMMON + "exchange.OrderBook$Order").getDeclaredConstructors()[0];
            order = ctor.newInstance(9_000_000L, owner.toString(), "", true, 10L, 5, 0L);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("order", e);
        }
        call(orderBook, "rest", order);
        Object box = call(d, "mailbox", owner);
        long uuBefore = (long) ArmoryServiceGameTests.field(box, "uu"), coinsBefore = (long) ArmoryServiceGameTests.field(box, "coins");
        call(d, "book", id, coin);
        long uuAfter = (long) ArmoryServiceGameTests.field(box, "uu"), coinsAfter = (long) ArmoryServiceGameTests.field(box, "coins");
        ((java.util.Map<String, ?>) ArmoryServiceGameTests.field(d, "books")).remove(id);
        ((java.util.Map<java.util.UUID, ?>) ArmoryServiceGameTests.field(d, "mail")).remove(owner);
        h.assertTrue(uuAfter - uuBefore == 50 && coinsAfter == coinsBefore,
            "The bid of 5 at 10 comes back as 50 UU, not coins: uu +" + (uuAfter - uuBefore) + ", coins +" + (coinsAfter - coinsBefore));
        h.assertTrue((int) call(orderBook, "size") == 0, "The resting order is gone");
        h.succeed();
    }

    /** W-2: the futures menu pages with the server's value. */
    @GameTest(template = "empty")
    public static void futuresMenuPagesWithTheServerValue(GameTestHelper h) {
        ServerPlayer p = player(h);
        String be = COMMON + "blockentity.producer.mio_icif_future_elc";
        int count = (int) staticField(be, "DATA_COUNT"), index = (int) staticField(be, "DATA_ITEMS_PER_PAGE");
        var data = new SimpleContainerData(count);
        data.set(index, 7);
        Object menu;
        try {
            menu = type(COMMON + "menu.producer.FutureElcMenu").getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class,
                type(be), net.neoforged.neoforge.items.IItemHandler.class, net.minecraft.world.inventory.ContainerData.class)
                .newInstance(1, p.getInventory(), null, null, data);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("futures menu", e);
        }
        h.assertTrue((int) call(menu, "getItemsPerPage") == 7, "The synced page size wins over the local config");
        p.discard();
        h.succeed();
    }
}
