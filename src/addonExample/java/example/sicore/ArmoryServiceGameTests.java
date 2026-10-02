package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Armory service features: storage-bus access modes, FE input, stored-gear repair and
 * recharge with a time estimate and completion notice, the purge -> latch suit-up order,
 * and the Armor Showcase (own outfit, outfit swap, Armory-linked mirror, drops).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ArmoryServiceGameTests {
    private static final BlockPos ARMORY = new BlockPos(2, 2, 2);
    private static final int HEAD_0 = 1, CHEST_0 = 2, MAIN_0 = 5;

    static Object call(Object target, String name, Object... args) {
        Class<?> type = target instanceof Class<?> c ? c : target.getClass();
        for (Class<?> k = type; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
                try {
                    m.setAccessible(true);
                    return m.invoke(target instanceof Class<?> ? null : target, args);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(name, e.getCause() != null ? e.getCause() : e);
                }
            }
        }
        throw new AssertionError("no method " + name + "/" + args.length + " on " + type);
    }

    static Object field(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new AssertionError(name, e);
            }
        }
        throw new AssertionError("no field " + name);
    }

    static ItemStack si(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    static AbstractProcessingMachineBlockEntity armory(GameTestHelper h, long energy) {
        h.setBlock(ARMORY, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        var armory = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(ARMORY);
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(energy);
        return armory;
    }

    static long energy(Object armory) {
        return ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().getAmount();
    }

    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(5, 2, 2));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90, 0);
        return p;
    }

    static String automation(Object armory) {
        return ((Enum<?>) call(armory, "automation")).name();
    }

    // ------------------------------------------------------------------ AE2 / pipes
    @GameTest(template = "reactor_loop", batch = "armory_service", timeoutTicks = 40)
    public static void storageBusAccessFollowsAutomationMode(GameTestHelper h) {
        var armory = armory(h, 50_000);
        ServerPlayer p = player(h);
        call(armory, "tryBind", p);
        armory.getItemHandler().insertItem(HEAD_0, new ItemStack(Items.IRON_HELMET), false);
        var bus = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ARMORY), Direction.UP);
        h.assertTrue(bus != null, "Armory exposes an item handler to storage buses");

        h.assertTrue(automation(armory).equals("LOCKED"), "New Armories start sealed");
        h.assertTrue(bus.extractItem(HEAD_0, 1, true).isEmpty(), "Locked: suits cannot be pulled out");
        h.assertTrue(!bus.insertItem(CHEST_0, new ItemStack(Items.IRON_CHESTPLATE), true).isEmpty(), "Locked: nothing can be pushed in");

        call(armory, "cycleAutomation", p);
        bus = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ARMORY), Direction.UP);
        h.assertTrue(automation(armory).equals("DEPOSIT"), "Owner cycles to deposit mode");
        h.assertTrue(bus.insertItem(CHEST_0, new ItemStack(Items.IRON_CHESTPLATE), false).isEmpty(), "Deposit: a chestplate goes into the chest column");
        h.assertTrue(!bus.insertItem(HEAD_0 + 6, new ItemStack(Items.IRON_BOOTS), true).isEmpty(), "Deposit still checks the column");
        h.assertTrue(bus.extractItem(HEAD_0, 1, true).isEmpty(), "Deposit: nothing comes out");

        call(armory, "cycleAutomation", p);
        bus = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ARMORY), Direction.UP);
        h.assertTrue(automation(armory).equals("OPEN"), "Owner cycles to open mode");
        h.assertTrue(bus.extractItem(HEAD_0, 1, false).is(Items.IRON_HELMET), "Open: a storage bus can take stored gear");

        CompoundTag tag = armory.saveWithFullMetadata(h.getLevel().registryAccess());
        BlockEntity copy = BlockEntity.loadStatic(h.absolutePos(ARMORY), armory.getBlockState(), tag, h.getLevel().registryAccess());
        h.assertTrue(copy != null && automation(copy).equals("OPEN"), "Automation mode survives a reload");

        ServerPlayer stranger = h.makeMockServerPlayerInLevel();
        call(armory, "cycleAutomation", stranger);
        h.assertTrue(automation(armory).equals("OPEN") || stranger.hasPermissions(2), "Only the owner changes the mode");
        p.discard();
        stranger.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ energy input
    @GameTest(template = "reactor_loop", batch = "armory_service", timeoutTicks = 40)
    public static void armoryTakesForgeEnergy(GameTestHelper h) {
        var armory = armory(h, 0);
        var fe = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(ARMORY), Direction.WEST);
        h.assertTrue(fe != null && fe.canReceive(), "Armory exposes an FE input");
        int moved = fe.receiveEnergy(2000, false);
        h.assertTrue(moved == 2000 && energy(armory) == 500, "2000 FE must become 500 EU, moved " + moved + " stored " + energy(armory));
        h.succeed();
    }

    // ------------------------------------------------------------------ maintenance
    @GameTest(template = "reactor_loop", batch = "armory_service", timeoutTicks = 260)
    public static void storedGearIsRepairedAndChargedWithEstimate(GameTestHelper h) {
        var armory = armory(h, 200_000);
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.setDamageValue(60);
        armory.getItemHandler().insertItem(CHEST_0, chest, false);
        ItemStack battery = si("normal/item_lithium_battery");
        armory.getItemHandler().insertItem(MAIN_0, battery, false);
        Object service = call(armory, "maintenance");
        long start = energy(armory);
        int[] tick = {0};
        int[] firstEta = {-2};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now == 12) {
                firstEta[0] = (int) call(service, "etaTicks");
                h.assertTrue((int) call(service, "pendingPieces") == 2, "Two pieces need service, got " + call(service, "pendingPieces"));
                h.assertTrue(firstEta[0] > 20 && firstEta[0] < 400, "ETA must be a positive time, got " + firstEta[0]);
            }
            if (now == 240) {
                ItemStack repaired = armory.getItemHandler().getStackInSlot(CHEST_0);
                h.assertTrue(repaired.getDamageValue() == 0, "Chestplate still damaged: " + repaired.getDamageValue());
                long spent = start - energy(armory);
                h.assertTrue(spent >= 60 * 40, "Repair of 60 points must cost >= 2400 EU, spent " + spent);
                h.assertTrue((int) call(service, "pendingPieces") == 0 || (int) call(service, "etaTicks") >= 0, "Service state inconsistent");
                h.assertTrue((int) call(service, "completedSessions") >= 0, "Session counter readable");
                h.succeed();
            }
        });
    }

    @GameTest(template = "reactor_loop", batch = "armory_service", timeoutTicks = 200)
    public static void maintenanceFinishesAndRaisesNotice(GameTestHelper h) {
        var armory = armory(h, 100_000);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(30);
        armory.getItemHandler().insertItem(MAIN_0, sword, false);
        Object service = call(armory, "maintenance");
        h.succeedWhen(() -> {
            h.assertTrue(armory.getItemHandler().getStackInSlot(MAIN_0).getDamageValue() == 0, "Sword not repaired yet");
            h.assertTrue((int) call(service, "completedSessions") == 1, "A finished session must raise exactly one notice");
            h.assertTrue((int) call(service, "state") == 0, "Finished service reads as ready");
        });
    }

    @GameTest(template = "reactor_loop", batch = "armory_service", timeoutTicks = 80)
    public static void maintenanceNeverEatsTheSummonReserve(GameTestHelper h) {
        Object probe = call(armory(h, 0), "maintenance");
        long reserve;
        try {
            reserve = probe.getClass().getField("SUMMON_RESERVE").getLong(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        var armory = armory(h, reserve + 100);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(100);
        armory.getItemHandler().insertItem(MAIN_0, sword, false);
        Object service = call(armory, "maintenance");
        h.runAfterDelay(60, () -> {
            h.assertTrue(energy(armory) >= reserve, "Maintenance dipped into the summon reserve: " + energy(armory) + " < " + reserve);
            h.assertTrue((int) call(service, "state") == 2, "With no income it must report 'needs EU', got " + call(service, "state"));
            h.assertTrue((int) call(service, "etaTicks") == -1, "No ETA without income");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ suit-up order
    @GameTest(template = "reactor_loop", batch = "armory_service", timeoutTicks = 200)
    public static void oldPieceIsPurgedBeforeTheNewOneLocks(GameTestHelper h) {
        var armory = armory(h, 200_000);
        ServerPlayer p = player(h);
        call(armory, "tryBind", p);
        armory.getItemHandler().insertItem(HEAD_0, new ItemStack(Items.IRON_HELMET), false);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        h.assertTrue((boolean) call(call(armory, "summon", p, 0), "ok"), "Summon must succeed");
        boolean[] sawGap = {false};
        h.onEachTick(() -> {
            ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
            if (head.isEmpty()) {
                sawGap[0] = true;
                h.assertTrue(armory.getItemHandler().getStackInSlot(HEAD_0).is(Items.LEATHER_HELMET),
                    "A purged piece must already be home");
                long echoes = h.getLevel().getEntities((Entity) null, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(64),
                    e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("armory_piece")
                        && (boolean) call(e, "isReturn")).size();
                h.assertTrue(echoes >= 1, "The purged piece must fly home visibly");
            }
            if (head.is(Items.IRON_HELMET)) {
                h.assertTrue(sawGap[0], "The old helmet must be purged before the new one locks on");
                p.discard();
                h.succeed();
            }
        });
    }

    /** Even next to the Armory a summon is a ~3 s ritual: warning, then one piece every 0.4 s, boots to helmet. */
    @GameTest(template = "reactor_loop", batch = "armory_sequence", timeoutTicks = 260)
    public static void summonIsStaggeredBootsToHelmetAndTakesItsTime(GameTestHelper h) {
        var armory = armory(h, 300_000);
        ServerPlayer p = player(h);
        p.moveTo(h.absolutePos(ARMORY).getX() + 2.5, h.absolutePos(ARMORY).getY(), h.absolutePos(ARMORY).getZ() + 0.5, 90, 0);
        call(armory, "tryBind", p);
        var inv = armory.getItemHandler();
        inv.insertItem(1, new ItemStack(Items.IRON_HELMET), false);
        inv.insertItem(2, new ItemStack(Items.IRON_CHESTPLATE), false);
        inv.insertItem(3, new ItemStack(Items.IRON_LEGGINGS), false);
        inv.insertItem(4, new ItemStack(Items.IRON_BOOTS), false);
        inv.insertItem(5, new ItemStack(Items.IRON_SWORD), false);
        inv.insertItem(6, new ItemStack(Items.SHIELD), false);
        long start = h.getLevel().getGameTime();
        h.assertTrue((boolean) call(call(armory, "summon", p, 0), "ok"), "Summon must succeed");
        List<Entity> pieces = h.getLevel().getEntities((Entity) null, new AABB(h.absolutePos(ARMORY)).inflate(16),
            e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("armory_piece") && !(boolean) call(e, "isReturn"));
        h.assertTrue(pieces.size() == 6, "Six pieces, got " + pieces.size());
        pieces.sort(java.util.Comparator.comparingLong(e -> (long) call(e, "launchTime")));
        String[] order = {"FEET", "LEGS", "CHEST", "OFFHAND", "MAINHAND", "HEAD"};
        for (int i = 0; i < 6; i++) {
            Entity e = pieces.get(i);
            h.assertTrue(((Enum<?>) call(e, "piece")).name().equals(order[i]), "Launch " + i + " is " + call(e, "piece") + ", expected " + order[i]);
            long launch = (long) call(e, "launchTime");
            if (i == 0) h.assertTrue(launch - start >= 10, "A warning must precede the first launch");
            else {
                long gap = launch - (long) call(pieces.get(i - 1), "launchTime");
                h.assertTrue(gap >= 6 && gap <= 10, "Pieces must launch 0.3-0.5 s apart, gap " + gap);
            }
        }
        long[] firstWorn = {-1};
        h.onEachTick(() -> {
            long now = h.getLevel().getGameTime();
            if (firstWorn[0] < 0 && p.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS)) firstWorn[0] = now - start;
            if (p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)) {
                long total = now - start;
                h.assertTrue(firstWorn[0] >= 50, "Even point-blank the first piece needs >= 2.5 s, took " + firstWorn[0] + " ticks");
                h.assertTrue(total >= 60 && total <= 200, "Whole suit-up took " + total + " ticks");
                h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                    && p.getMainHandItem().is(Items.IRON_SWORD) && p.getOffhandItem().is(Items.SHIELD), "Helmet is last: everything else is on");
                p.discard();
                h.succeed();
            }
        });
    }

    // ------------------------------------------------------------------ showcase
    static final BlockPos SHOWCASE = new BlockPos(4, 2, 4);

    static BlockEntity showcase(GameTestHelper h) {
        h.setBlock(SHOWCASE, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armor_showcase")));
        return h.getBlockEntity(SHOWCASE);
    }

    static void use(GameTestHelper h, ServerPlayer p, ItemStack held) {
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos at = h.absolutePos(SHOWCASE);
        var hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        h.getLevel().getBlockState(at).useItemOn(held, h.getLevel(), p, InteractionHand.MAIN_HAND, hit);
        if (held.isEmpty()) h.getLevel().getBlockState(at).useWithoutItem(h.getLevel(), p, hit);
    }

    @SuppressWarnings("unchecked")
    static List<ItemStack> display(BlockEntity showcase) {
        return (List<ItemStack>) call(showcase, "display");
    }

    @GameTest(template = "reactor_loop", batch = "armory_showcase", timeoutTicks = 60)
    public static void showcaseDressesSwapsAndDrops(GameTestHelper h) {
        BlockEntity showcase = showcase(h);
        ServerPlayer p = player(h);
        use(h, p, new ItemStack(Items.DIAMOND_CHESTPLATE));
        use(h, p, new ItemStack(Items.SHIELD));
        use(h, p, new ItemStack(Items.NETHERITE_SWORD));
        List<ItemStack> shown = display(showcase);
        h.assertTrue(shown.get(1).is(Items.DIAMOND_CHESTPLATE), "Chestplate goes on the torso");
        h.assertTrue(shown.get(5).is(Items.SHIELD), "Shield goes in the off hand");
        h.assertTrue(shown.get(4).is(Items.NETHERITE_SWORD), "Sword goes in the main hand");

        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setShiftKeyDown(true);
        use(h, p, ItemStack.EMPTY);
        p.setShiftKeyDown(false);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "Sneak-use swaps the outfit: player wears the diamond");
        h.assertTrue(display(showcase).get(1).is(Items.IRON_CHESTPLATE), "...and the mannequin gets the iron chestplate");
        h.assertTrue(p.getMainHandItem().is(Items.NETHERITE_SWORD) && p.getOffhandItem().is(Items.SHIELD), "Hands are swapped too");
        use(h, p, new ItemStack(Items.GOLDEN_BOOTS));

        int before = (int) call(showcase, "rotation");
        use(h, p, ItemStack.EMPTY);
        h.assertTrue((int) call(showcase, "rotation") == (before + 45) % 360, "Empty-hand use turns the mannequin");

        h.setBlock(SHOWCASE, net.minecraft.world.level.block.Blocks.AIR);
        long drops = h.getLevel().getEntities((Entity) null, new AABB(h.absolutePos(SHOWCASE)).inflate(2),
            e -> e instanceof net.minecraft.world.entity.item.ItemEntity).size();
        h.assertTrue(drops == 2, "Breaking the showcase drops its outfit (chestplate + boots), got " + drops + " item entities");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "armory_showcase", timeoutTicks = 80)
    public static void showcaseMirrorsALinkedArmorySuit(GameTestHelper h) {
        var armory = armory(h, 50_000);
        ServerPlayer p = player(h);
        call(armory, "tryBind", p);
        armory.getItemHandler().insertItem(HEAD_0 + 6, new ItemStack(Items.GOLDEN_HELMET), false);   // suit 1
        call(armory, "rename", p, 1, "Gold Parade");
        BlockEntity showcase = showcase(h);
        ItemStack remote = si("normal/item_armory_remote");
        ResourceLocation targetKey = ResourceLocation.fromNamespaceAndPath("mio_icif", "armory_target");
        @SuppressWarnings("unchecked")
        var type = (net.minecraft.core.component.DataComponentType<net.minecraft.core.GlobalPos>)
            BuiltInRegistries.DATA_COMPONENT_TYPE.get(targetKey);
        h.assertTrue(type != null, "armory_target component registered");
        remote.set(type, net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(ARMORY)));
        use(h, p, remote);                 // link: suit 0 (empty)
        use(h, p, remote);                 // next: suit 1
        h.assertTrue((boolean) call(showcase, "linked"), "Remote links the showcase");
        h.assertTrue(display(showcase).get(0).is(Items.GOLDEN_HELMET), "Linked showcase shows suit 1's helmet");
        h.assertTrue("Gold Parade".equals(call(showcase, "label")), "Linked showcase is labelled with the suit name");
        armory.getItemHandler().extractItem(HEAD_0 + 6, 1, false);
        h.runAfterDelay(25, () -> {
            h.assertTrue(display(showcase).get(0).isEmpty(), "Mirror follows the Armory within a second");
            p.setShiftKeyDown(true);
            use(h, p, remote);
            h.assertTrue(!(boolean) call(showcase, "linked"), "Sneak-use with the remote unlinks");
            p.discard();
            h.succeed();
        });
    }
}
