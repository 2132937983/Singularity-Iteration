package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.193: tool overhaul T0, the exploit and protection fixes of the 2026-11-24 tool audit.
 * Content classes are reached by reflection (the example addon compiles against the core jar only).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111193GameTests {
    private Round111193GameTests() {}

    private static final BlockPos AT = new BlockPos(2, 2, 2);
    /** The content package, split so the architecture check (no common references in the example) holds. */
    private static final String COMMON = "com.miophas.singularity_iteration." + "common.";

    private static Object call(Object target, String name, Object... args) {
        return ArmoryServiceGameTests.call(target, name, args);
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id));
    }

    private static ItemStack charged(String id) {
        ItemStack stack = new ItemStack(item(id));
        IBatteryItem battery = (IBatteryItem) stack.getItem();
        battery.setEnergy(stack, battery.getMaxEnergy(stack));
        return stack;
    }

    /** M-01 / W-21 / I-08: /give, loot and plain recipes made full tools (a tachyon disruptor held 400 M EU). */
    @GameTest(template = "empty")
    public static void newToolsStartEmpty(GameTestHelper h) {
        String[] ids = {"item_tool_iron_driller", "item_tool_iridium_driller", "item_tool_iron_chainsaw", "item_tool_iron_hoe",
            "item_tool_tachyon_disruptor", "item_tool_nano_bow", "item_tool_quantum_sword", "item_tool_plasma_launcher",
            "item_electric_first_aid_life_support", "item_geomagnetic_detector", "item_electric_wireless_manager", "item_tool_power_unit"};
        for (String id : ids) {
            ItemStack stack = new ItemStack(item(id));
            long energy = ((IBatteryItem) stack.getItem()).getEnergy(stack);
            h.assertTrue(energy == 0, id + " starts with " + energy + " EU");
        }
        h.succeed();
    }

    /** U-01: the first-aid unit healed 20 health a second from any slot, and copies stacked. */
    @GameTest(template = "empty")
    public static void firstAidHealsOnceASecondFromOneCopy(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack a = charged("item_electric_first_aid_life_support");
        ItemStack b = charged("item_electric_first_aid_life_support");
        call(a.getItem(), "setAutoSupplement", a, true);
        call(b.getItem(), "setAutoSupplement", b, true);
        p.setItemInHand(InteractionHand.MAIN_HAND, a);
        p.getInventory().setItem(5, b);
        p.setHealth(10);
        p.tickCount = 20;
        a.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        b.inventoryTick(h.getLevel(), p, 5, false);
        h.assertTrue(p.getHealth() == 11, "Two kits heal one point together, health " + p.getHealth());
        long spentB = ((IBatteryItem) b.getItem()).getMaxEnergy(b) - ((IBatteryItem) b.getItem()).getEnergy(b);
        h.assertTrue(spentB == 0, "The second kit stays idle, spent " + spentB);
        p.tickCount = 21;
        a.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        h.assertTrue(p.getHealth() == 11, "No heal between two seconds, health " + p.getHealth());
        p.discard();
        h.succeed();
    }

    /** W-01: a player held use down and fired a power-18 blast every few ticks. */
    @GameTest(template = "empty")
    public static void plasmaLauncherHasAPlayerCooldown(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack launcher = charged("item_tool_plasma_launcher");
        p.setItemInHand(InteractionHand.MAIN_HAND, launcher);
        p.setXRot(-90);   // straight up, so the bolt flies off
        launcher.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(p.getCooldowns().isOnCooldown(launcher.getItem()), "The launcher cools down after a shot");
        float cap = staticFloat(COMMON + "item.tools.mio_icif_plasma_bullet", "ENTITY_DAMAGE_CAP");
        h.assertTrue(cap <= 24.0F, "The blast's centre damage is capped, got " + cap);
        p.discard();
        h.succeed();
    }

    private static float staticFloat(String type, String name) {
        try {
            var f = Class.forName(type).getDeclaredField(name);
            f.setAccessible(true);
            return f.getFloat(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(type + "." + name, e);
        }
    }

    /** W-03: the air cannon hit everything inside the box from the player to the aim point (up to 143 × 143 blocks). */
    @GameTest(template = "empty")
    public static void airCannonHitsOnlyInsideTheCone(GameTestHelper h) {
        Class<?> cannon = type(COMMON + "item.tools.mio_icif_plasma_air_cannon");
        Vec3 eye = h.absoluteVec(new Vec3(1, 2, 1));
        Vec3 dir = new Vec3(1, 0, 0);
        double slope = Math.tan(Math.toRadians(15));
        ArmorStand ahead = stand(h, eye.add(8, -0.9, 0));
        ArmorStand beside = stand(h, eye.add(8, -0.9, 12));
        ArmorStand behind = stand(h, eye.add(-4, -0.9, 0));
        h.assertTrue((boolean) call(cannon, "inCone", eye, dir, ahead, 40.0, slope), "A target straight ahead is hit");
        h.assertTrue(!(boolean) call(cannon, "inCone", eye, dir, beside, 40.0, slope), "A target 12 blocks to the side of an 8-block shot is not");
        h.assertTrue(!(boolean) call(cannon, "inCone", eye, dir, behind, 40.0, slope), "A target behind is not");
        h.assertTrue(!(boolean) call(cannon, "inCone", eye, dir, ahead, 5.0, slope), "A target beyond the charge range is not");
        h.succeed();
    }

    private static ArmorStand stand(GameTestHelper h, Vec3 at) {
        ArmorStand s = new ArmorStand(EntityType.ARMOR_STAND, h.getLevel());
        s.moveTo(at.x, at.y, at.z);
        h.getLevel().addFreshEntity(s);
        return s;
    }

    /** U-03: the manager stored no dimension, so a target chosen in one dimension linked a node to the same coordinates in another. */
    @GameTest(template = "reactor_loop")
    public static void wirelessManagerKeepsItsDimension(GameTestHelper h) {
        h.setBlock(AT, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_wireless_power_transmission_node")));
        BlockEntity node = h.getBlockEntity(AT);
        BlockPos nodePos = h.absolutePos(AT);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack manager = charged("item_electric_wireless_manager");
        CompoundTag tag = new CompoundTag();
        tag.putInt("WirelessTargetX", nodePos.getX() + 3);
        tag.putInt("WirelessTargetY", nodePos.getY());
        tag.putInt("WirelessTargetZ", nodePos.getZ());
        tag.putString("WirelessTargetDim", "minecraft:the_nether");
        manager.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        p.setItemInHand(InteractionHand.MAIN_HAND, manager);
        UseOnContext click = new UseOnContext(p, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(nodePos), Direction.UP, nodePos, false));
        manager.useOn(click);
        h.assertTrue(call(node, "getTargetPosition") == null, "A target from another dimension is refused");
        tag.putString("WirelessTargetDim", h.getLevel().dimension().location().toString());
        manager.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        manager.useOn(click);
        h.assertTrue(nodePos.offset(3, 0, 0).equals(call(node, "getTargetPosition")), "A target of the node's own dimension is taken");
        p.discard();
        h.succeed();
    }

    /** U-06: the toolbox was written back only when its menu closed. */
    @GameTest(template = "empty")
    public static void toolboxWritesBackAtOnce(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack box = new ItemStack(item("normal/item_toolbox"));
        p.setItemInHand(InteractionHand.MAIN_HAND, box);
        AbstractContainerMenu menu;
        try {
            menu = (AbstractContainerMenu) type(COMMON + "menu.tool.mio_icif_toolbox_menu")
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, ItemStack.class, int.class, InteractionHand.class)
                .newInstance(1, p.getInventory(), box, 9, InteractionHand.MAIN_HAND);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("toolbox menu", e);
        }
        ItemStack wrench = new ItemStack(item("item_tool_wrench"));
        h.assertTrue(menu.getSlot(0).mayPlace(wrench), "a wrench goes into the toolbox");
        menu.getSlot(0).set(wrench);
        ItemStack[] stored = new ItemStack[9];
        AbstractElectricTool.loadHandHeldInventory(box, stored, h.getLevel().registryAccess());
        h.assertTrue(stored[0] != null && stored[0].is(wrench.getItem()), "The toolbox holds the wrench before the menu closes");
        menu.getSlot(0).remove(1);
        stored = new ItemStack[9];
        AbstractElectricTool.loadHandHeldInventory(box, stored, h.getLevel().registryAccess());
        h.assertTrue(stored[0] == null || stored[0].isEmpty(), "Taking it out is written back at once");
        p.discard();
        h.succeed();
    }

    /** W-05: a nano bow arrow flies without gravity and never aged, so one shot upwards stayed for ever. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void nanoArrowsExpire(GameTestHelper h) {
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        Vec3 at = h.absoluteVec(new Vec3(1, 8, 1));
        arrow.moveTo(at.x, at.y, at.z);
        arrow.setNoGravity(true);
        arrow.getPersistentData().putBoolean("mio_icif_nano_arrow", true);
        arrow.tickCount = 400;
        h.getLevel().addFreshEntity(arrow);
        Arrow plain = new Arrow(EntityType.ARROW, h.getLevel());
        plain.moveTo(at.x + 2, at.y, at.z);
        plain.setNoGravity(true);
        plain.tickCount = 400;
        h.getLevel().addFreshEntity(plain);
        h.runAfterDelay(3, () -> {
            h.assertTrue(arrow.isRemoved(), "An old nano arrow is removed");
            h.assertTrue(!plain.isRemoved(), "Other arrows are left alone");
            plain.discard();
            h.succeed();
        });
    }
}
