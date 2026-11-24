package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Armory: power-gated owner binding, suit-slot rules (the gear of every mod, 0.1.11.49),
 * paid summons that fly every piece onto the owner and swap the old gear home, the
 * in-flight safety net and remote pairing.
 *
 * <p>The example source set may not import SI content classes, so Armory methods are
 * reached by reflection; core prefab types are allowed.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ArmoryGameTests {
    private static final BlockPos ARMORY = new BlockPos(2, 2, 2);
    // slot = 1 + set * 6 + column; columns: head, chest, legs, feet, main hand, off hand
    private static final int HEAD_0 = 1, FEET_0 = 4, MAIN_0 = 5, HEAD_1 = 7;

    private static Object call(Object target, String name, Object... args) {
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

    private static ItemStack si(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    private static AbstractProcessingMachineBlockEntity armory(GameTestHelper h, long energy) {
        h.setBlock(ARMORY, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        var armory = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(ARMORY);
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(energy);
        return armory;
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(5, 2, 2));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90, 0);
        return p;
    }

    /** Delivery pieces launched by this test's Armory (tests run side by side). */
    private static List<Entity> pieces(GameTestHelper h) {
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Vec3 home = Vec3.atCenterOf(h.absolutePos(ARMORY));
        return h.getLevel().getEntities((Entity) null, new AABB(o).inflate(96),
            e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("armory_piece")
                && !(boolean) call(e, "isReturn") && ((Vec3) call(e, "start")).distanceTo(home) < 4);
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 40)
    public static void ownerBindingRequiresPower(GameTestHelper h) {
        var armory = armory(h, 0);
        ServerPlayer p = player(h);
        call(armory, "tryBind", p);
        h.assertTrue(call(armory, "owner") == null, "An unpowered Armory must not accept an owner");
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(5_000);
        call(armory, "tryBind", p);
        h.assertTrue(p.getUUID().equals(call(armory, "owner")), "A powered Armory binds the first player");
        ServerPlayer stranger = h.makeMockServerPlayerInLevel();
        h.assertTrue(!(boolean) call(armory, "mayAccess", stranger) || stranger.hasPermissions(2), "Strangers may not open a bound Armory");
        CompoundTagCheck.roundTrip(h, armory, "ArmoryOwner");
        p.discard(); stranger.discard();
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 40)
    public static void suitSlotsAcceptGearOfEveryMod(GameTestHelper h) {
        var armory = armory(h, 5_000);
        var inv = armory.getItemHandler();
        h.assertTrue(inv.getSlots() == 38, "Battery + 6 suits x 6 pieces + mod slot, got " + inv.getSlots());
        h.assertTrue(inv.isItemValid(HEAD_0, new ItemStack(Items.IRON_HELMET)), "Helmet column takes helmets");
        h.assertTrue(!inv.isItemValid(HEAD_0, new ItemStack(Items.IRON_BOOTS)), "Helmet column rejects boots");
        h.assertTrue(inv.isItemValid(FEET_0, new ItemStack(Items.IRON_BOOTS)), "Boots column takes boots");
        h.assertTrue(inv.isItemValid(MAIN_0, new ItemStack(Items.DIAMOND_SWORD)), "Main hand takes weapons");
        h.assertTrue(!inv.isItemValid(MAIN_0, new ItemStack(Items.DIAMOND_CHESTPLATE)), "Hands never take body armour");

        // 0.1.11.49 (F0): the Connector Kit is gone; the gear of every mod is accepted as it is
        ItemStack foreign = new ItemStack(CoreExampleMod.FOREIGN_CROP_SEED.get());
        h.assertTrue(inv.isItemValid(MAIN_0, foreign), "Foreign gear is accepted without a kit");
        h.assertTrue(!inv.isItemValid(HEAD_0, foreign), "Foreign hand gear stays out of armour columns");
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 160)
    public static void summonFliesSuitOntoOwnerAndSendsOldGearHome(GameTestHelper h) {
        var armory = armory(h, 200_000);
        ServerPlayer p = player(h);
        call(armory, "tryBind", p);
        var inv = armory.getItemHandler();
        inv.insertItem(HEAD_0, new ItemStack(Items.IRON_HELMET), false);
        inv.insertItem(MAIN_0, new ItemStack(Items.DIAMOND_SWORD), false);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));

        long before = ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().getAmount();
        double distance = p.position().distanceTo(Vec3.atCenterOf(h.absolutePos(ARMORY)));
        Object result = call(armory, "summon", p, 0);
        h.assertTrue((boolean) call(result, "ok"), "Summon must succeed: " + ((net.minecraft.network.chat.Component) call(result, "message")).getString());
        long expected = (long) call(armory.getClass(), "cost", 2, distance, true);
        long spent = before - ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().getAmount();
        h.assertTrue(spent == expected, "Summon must cost " + expected + " EU, spent " + spent);
        // 0.1.7.22: the worn helmet bursts off at once and takes the new one's place in the suit slot
        h.assertTrue(inv.getStackInSlot(HEAD_0).is(Items.LEATHER_HELMET) && inv.getStackInSlot(MAIN_0).isEmpty(),
            "Pieces leave the Armory at launch; the old helmet is purged home at once");
        long deliveries = pieces(h).stream().filter(e -> !(boolean) call(e, "isReturn")).count();
        h.assertTrue(deliveries == 2, "One flying piece per stored item, got " + deliveries);
        Object busy = call(armory, "summon", p, 0);
        h.assertTrue(!(boolean) call(busy, "ok"), "A second summon while pieces fly is refused");

        h.succeedWhen(() -> {
            h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Helmet has not docked yet");
            h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD), "Sword has not docked yet");
            h.assertTrue(inv.getStackInSlot(HEAD_0).is(Items.LEATHER_HELMET), "Old helmet must go home to the suit slot");
            List<?> log = (List<?>) call(armory, "log");
            h.assertTrue(log.size() == 1, "Summon must be logged");
            h.assertTrue(pieces(h).stream().noneMatch(e -> !((ItemStack) call(e, "item")).isEmpty() && !(boolean) call(e, "isReturn")),
                "No delivery piece may linger");
            p.discard();
        });
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 60)
    public static void destroyedPiecesNeverLoseTheirItem(GameTestHelper h) {
        var armory = armory(h, 200_000);
        ServerPlayer p = player(h);
        call(armory, "tryBind", p);
        var inv = armory.getItemHandler();
        inv.insertItem(HEAD_1, new ItemStack(Items.GOLDEN_HELMET), false);
        h.assertTrue((boolean) call(call(armory, "summon", p, 1), "ok"), "Summon must succeed");
        List<Entity> flying = pieces(h);
        h.assertTrue(flying.size() == 1, "One piece in flight");
        // save/load round trip keeps the carried item
        var tag = flying.get(0).saveWithoutId(new net.minecraft.nbt.CompoundTag());
        h.assertTrue(tag.contains("Item") && tag.getCompound("Item").getString("id").equals("minecraft:golden_helmet"),
            "In-flight piece must persist its item");
        flying.get(0).kill();
        h.assertTrue(inv.getStackInSlot(HEAD_1).is(Items.GOLDEN_HELMET), "A killed piece returns its item to the Armory");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Nothing may be equipped by a destroyed piece");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 40)
    public static void remotePairsOnlyWithOwnersArmory(GameTestHelper h) {
        var armory = armory(h, 5_000);
        ServerPlayer owner = player(h);
        ItemStack remote = si("normal/item_armory_remote");
        owner.setItemInHand(InteractionHand.MAIN_HAND, remote);
        owner.setShiftKeyDown(true);
        BlockPos abs = h.absolutePos(ARMORY);
        remote.useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        h.assertTrue(owner.getUUID().equals(call(armory, "owner")), "Pairing a fresh powered Armory binds it");
        var target = remote.getComponents().keySet().stream()
            .filter(t -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)).equals("mio_icif:armory_target")).findFirst();
        h.assertTrue(target.isPresent(), "Remote must store its Armory position");

        ServerPlayer stranger = h.makeMockServerPlayerInLevel();
        ItemStack other = si("normal/item_armory_remote");
        stranger.setItemInHand(InteractionHand.MAIN_HAND, other);
        stranger.setShiftKeyDown(true);
        other.useOn(new UseOnContext(stranger, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        h.assertTrue(other.getComponents().keySet().stream()
            .noneMatch(t -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)).equals("mio_icif:armory_target")),
            "A stranger's remote must not pair");
        owner.discard(); stranger.discard();
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 40)
    public static void doubleHeightGantrySyncAndCleanup(GameTestHelper h) {
        var armory = armory(h, 5_000);
        ServerPlayer owner = player(h);
        call(armory, "tryBind", owner);
        BlockPos abs = h.absolutePos(ARMORY);
        BlockPos above = abs.above();
        h.runAfterDelay(3, () -> {
            var lower = h.getLevel().getBlockState(abs);
            var upper = h.getLevel().getBlockState(above);
            h.assertTrue(upper.is(lower.getBlock())
                && upper.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER,
                "Upper gantry half must auto-place above a legacy/direct lower Armory");
            h.assertTrue(h.getLevel().getBlockEntity(above) == null, "Upper gantry half must not have a BlockEntity");
            var litProp = (net.minecraft.world.level.block.state.properties.BooleanProperty) lower.getBlock().getStateDefinition().getProperty("lit");
            h.assertTrue(litProp != null && lower.getValue(litProp) && upper.getValue(litProp),
                "Upper gantry half must sync lit state with lower base");

            // Sneak-clicking the upper half with an Armory Remote pairs with the lower base pos
            ItemStack remote = si("normal/item_armory_remote");
            owner.setItemInHand(InteractionHand.MAIN_HAND, remote);
            owner.setShiftKeyDown(true);
            remote.useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(above), Direction.NORTH, above, false)));
            var targetKey = remote.getComponents().keySet().stream()
                .filter(t -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)).equals("mio_icif:armory_target")).findFirst();
            h.assertTrue(targetKey.isPresent() && String.valueOf(remote.get(targetKey.get())).contains(abs.toString()),
                "Remote paired via upper half must point to lower base pos");

            // Breaking lower removes upper cleanly
            h.getLevel().destroyBlock(abs, false);
            h.assertTrue(h.getLevel().getBlockState(above).isAir(), "Breaking lower Armory must clear upper gantry half");
            owner.discard();
            h.succeed();
        });
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 40)
    public static void integratedModificationBayInstallsAndEjects(GameTestHelper h) {
        var armory = armory(h, 10_000);
        ServerPlayer p = player(h);
        var inv = armory.getItemHandler();
        final int MOD_SLOT = 37;

        // Place Quantum Helmet in HEAD_0 and Entity ESP (HEAD) module in MOD_SLOT
        inv.insertItem(HEAD_0, si("armor/item_armor_quantum_helmet"), false);
        inv.insertItem(MOD_SLOT, si("module/item_module_entity_esp"), false);
        long energyBefore = armory.getEnergyStorage().getAmount();
        call(armory, "installMod", p, HEAD_0);
        h.assertTrue(inv.getStackInSlot(MOD_SLOT).isEmpty(), "Mod slot must be consumed after install");
        h.assertTrue(armory.getEnergyStorage().getAmount() == energyBefore - 1_280L, "Install must consume 1,280 EU");
        ItemStack modded = inv.getStackInSlot(HEAD_0);
        h.assertTrue(modded.getComponents().keySet().stream()
                .anyMatch(t -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)).equals("mio_icif:suit_modules")),
            "Helmet must carry installed suit_modules component");

        // Eject the installed module
        h.assertTrue((boolean) call(armory, "removeModUnit", p, HEAD_0, 0), "Armory must eject installed module");
        boolean gotModuleBack = p.getInventory().items.stream().anyMatch(s -> s.is(si("module/item_module_entity_esp").getItem()))
            || inv.getStackInSlot(MOD_SLOT).is(si("module/item_module_entity_esp").getItem());
        h.assertTrue(gotModuleBack, "Ejected module must return to player or mod slot");

        // Install and eject a Frame Core
        inv.extractItem(MOD_SLOT, 64, false);
        inv.insertItem(MOD_SLOT, si("module/item_core_solar_array_1"), false);
        call(armory, "installMod", p, HEAD_0);
        h.assertTrue(inv.getStackInSlot(MOD_SLOT).isEmpty(), "Core must be consumed from mod slot");
        h.assertTrue((boolean) call(armory, "removeModCore", p, HEAD_0), "Armory must eject installed frame core");
        boolean gotCoreBack = p.getInventory().items.stream().anyMatch(s -> s.is(si("module/item_core_solar_array_1").getItem()))
            || inv.getStackInSlot(MOD_SLOT).is(si("module/item_core_solar_array_1").getItem());
        h.assertTrue(gotCoreBack, "Ejected core must return to player or mod slot");

        p.discard();
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "armory", timeoutTicks = 40)
    public static void integratedModificationBayInstallsLivingNanoLining(GameTestHelper h) {
        var armory = armory(h, 10_000);
        ServerPlayer p = player(h);
        var inv = armory.getItemHandler();
        final int CHEST_0 = 2, MOD_SLOT = 37;

        inv.insertItem(CHEST_0, si("armor/item_armor_quantum_chestplate"), false);
        ItemStack lining = si("module/item_living_nano_lining");
        h.assertTrue(inv.isItemValid(MOD_SLOT, lining), "Armory mod slot must accept Living Nano Lining");
        inv.insertItem(MOD_SLOT, lining, false);
        long energyBefore = armory.getEnergyStorage().getAmount();
        call(armory, "installMod", p, CHEST_0);
        h.assertTrue(inv.getStackInSlot(MOD_SLOT).isEmpty(), "Living Nano Lining must be consumed after install");
        h.assertTrue(armory.getEnergyStorage().getAmount() == energyBefore - 1_280L, "Lining install must consume 1,280 EU");
        ItemStack chest = inv.getStackInSlot(CHEST_0);
        h.assertTrue(chest.getComponents().keySet().stream()
                .anyMatch(t -> String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)).equals("mio_icif:suit_living_lining")),
            "Quantum Chestplate must carry mio_icif:suit_living_lining component");

        // Awakened lining must now accept a nano-only chestplate module via the Armory
        inv.insertItem(MOD_SLOT, si("module/item_module_thermoregulator"), false);
        call(armory, "installMod", p, CHEST_0);
        h.assertTrue(inv.getStackInSlot(MOD_SLOT).isEmpty(), "Nano-only module must install into awakened living nano lining");

        p.discard();
        h.succeed();
    }

    /** Owner and suits survive the save/load path used on server restart. */
    private static final class CompoundTagCheck {
        static void roundTrip(GameTestHelper h, AbstractProcessingMachineBlockEntity armory, String key) {
            var reg = h.getLevel().registryAccess();
            var tag = armory.saveWithFullMetadata(reg);
            h.assertTrue(tag.contains(key), "Armory must save " + key);
            var copy = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(armory.getBlockPos(), armory.getBlockState(), tag, reg);
            h.assertTrue(copy != null && String.valueOf(call(copy, "owner")).equals(String.valueOf(call(armory, "owner"))),
                "Owner must survive a reload");
        }
    }
}
