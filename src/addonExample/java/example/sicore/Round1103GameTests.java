package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IJetpackItem;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.flight.JetpackFlightController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.3 (patch): a riding player never runs the jetpack (no energy, no thrust
 * sample for sound and effects), the cable outline follows the model thickness of every insulation
 * level, and empty energy / lapotron / super lapotron crystals stack while a charged one stays single
 * and a stack is never charged as one.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1103GameTests {
    private Round1103GameTests() {}

    private static final String[] CRYSTALS = {
        "normal/item_crystal_lev0", "normal/item_lapotron_crystal_lev0", "normal/item_super_lapotron_crystal"};

    // ------------------------------------------------------------------ 1. riding

    /** In a boat the hover jetpack used to take the rider for airborne and drain the chest every tick. */
    @GameTest(batch = "round1103_ride", template = "empty", timeoutTicks = 30)
    public static void riderNeverRunsTheJetpack(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        ItemStack chest = Round190GameTests.withCore("armor/item_armor_quantum_chestplate", "propulsion");
        var battery = (IBatteryItem) chest.getItem();
        var jet = (IJetpackItem) chest.getItem();
        battery.setEnergy(chest, 1_000_000);
        jet.setMode(chest, IJetpackItem.JetpackMode.HOVER);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        var boat = EntityType.BOAT.create(h.getLevel());
        h.assertTrue(boat != null, "no boat");
        boat.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 3, 1.5)));
        h.getLevel().addFreshEntity(boat);
        h.assertTrue(player.startRiding(boat, true) && player.isPassenger(), "the player did not board the boat");
        player.setOnGround(false);
        long before = battery.getEnergy(chest);
        for (int i = 0; i < 10; i++) JetpackFlightController.tick(player, chest, jet, 10, 0.3, 0.2, 0.1, true);
        h.assertTrue(battery.getEnergy(chest) == before, "riding drained the jetpack: " + (before - battery.getEnergy(chest)) + " EU");
        h.assertTrue(JetpackFlightController.lastThrust(player) == null, "riding left a thrust sample (sound and flame)");
        // control: the same player off the boat, in the air, hovers and pays
        player.stopRiding();
        player.setOnGround(false);
        JetpackFlightController.tick(player, chest, jet, 10, 0.3, 0.2, 0.1, true);
        h.assertTrue(battery.getEnergy(chest) < before, "the hover test setup does not run the jetpack at all");
        JetpackFlightController.removePlayer(player);
        boat.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ 2. cable outline

    /** Radius in pixels of every cable model (core box from 8 - r to 8 + r). */
    private static final Object[][] CABLES = {
        {"cable/block_tin_cable", 2}, {"cable/block_cable_o", 2}, {"cable/block_gold_cable", 2}, {"cable/block_iron_cable", 2},
        {"cable/block_glass_cable", 2}, {"cable/block_superconducting_cable", 2},
        {"cable/block_tin_cable_1", 3}, {"cable/block_cable", 3}, {"cable/block_gold_cable_1", 3}, {"cable/block_iron_cable_1", 3},
        {"cable/block_gold_cable_2", 4}, {"cable/block_iron_cable_2", 4}, {"cable/block_iron_cable_3", 5},
        {"block_eu_detector_cable", 2}, {"block_eu_splitter_cable", 2}};   // insulated IV, drawn 4 px

    @GameTest(batch = "round1103_cable", template = "empty", timeoutTicks = 20)
    public static void cableOutlineFollowsTheModel(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1), next = pos.east();
        for (Object[] c : CABLES) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/" + c[0]));
            h.assertTrue(block != Blocks.AIR, "missing cable " + c[0]);
            int r = (Integer) c[1];
            h.assertTrue((Integer) Round33GameTests.call(block, "getShapeRadius") == r, c[0] + " radius " + Round33GameTests.call(block, "getShapeRadius") + " != model " + r);
            h.setBlock(pos, block);
            h.setBlock(next, Blocks.AIR);
            AABB core = h.getLevel().getBlockState(h.absolutePos(pos)).getShape(h.getLevel(), h.absolutePos(pos)).bounds();
            double lo = (8 - r) / 16.0, hi = (8 + r) / 16.0;
            h.assertTrue(near(core.minX, lo) && near(core.maxX, hi) && near(core.minY, lo) && near(core.maxY, hi)
                && near(core.minZ, lo) && near(core.maxZ, hi), c[0] + " core outline " + core + " != " + lo + ".." + hi);
            // an arm is as thick as the core and reaches the block face
            h.setBlock(next, block);
            AABB arm = h.getLevel().getBlockState(h.absolutePos(pos)).getShape(h.getLevel(), h.absolutePos(pos)).bounds();
            h.assertTrue(near(arm.maxX, 1.0) && near(arm.minX, lo) && near(arm.minY, lo) && near(arm.maxY, hi)
                && near(arm.minZ, lo) && near(arm.maxZ, hi), c[0] + " arm outline " + arm);
        }
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(next, Blocks.AIR);
        h.succeed();
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    // ------------------------------------------------------------------ 4. crystal stacking

    static ItemStack empty(String path) {
        ItemStack s = Round33GameTests.item(path);
        ((IBatteryItem) s.getItem()).setEnergy(s, 0);   // the super lapotron crystal spawns full
        return s;
    }

    @GameTest(batch = "round1103_stack", template = "empty", timeoutTicks = 20)
    public static void emptyCrystalsStackChargedOnesDoNot(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var registries = h.getLevel().registryAccess();
        for (String path : CRYSTALS) {
            ItemStack a = empty(path), b = empty(path);
            Item item = a.getItem();
            IBatteryItem bat = (IBatteryItem) item;
            h.assertTrue(ItemStack.isSameItemSameComponents(a, b) && a.getMaxStackSize() == 16, path + ": empty crystals do not stack (max " + a.getMaxStackSize() + ")");
            // crafted (onCraftedBy) and creative / JEI (default instance) give the same stack
            ItemStack crafted = empty(path);
            item.onCraftedBy(crafted, h.getLevel(), player);
            h.assertTrue(ItemStack.isSameItemSameComponents(a, crafted), path + ": a crafted crystal differs from an empty one");
            h.assertTrue(ItemStack.isSameItemSameComponents(a, item.getDefaultInstance()), path + ": the creative crystal differs");
            // a charged crystal is single, and a discharged one stacks again
            ItemStack c = empty(path);
            bat.setEnergy(c, 1000);
            h.assertTrue(bat.getEnergy(c) == 1000 && c.getMaxStackSize() == 1, path + ": a charged crystal stacks (max " + c.getMaxStackSize() + ")");
            ItemStack d = c.copy();
            h.assertTrue(ItemStack.isSameItemSameComponents(c, d) && d.getMaxStackSize() == 1, path + ": two equal charged crystals could merge");
            bat.setEnergy(c, 0);
            h.assertTrue(ItemStack.isSameItemSameComponents(a, c) && c.getMaxStackSize() == 16, path + ": a discharged crystal does not stack with an empty one");
            // a stack saved and loaded again keeps stacking
            ItemStack loaded = ItemStack.parseOptional(registries, (CompoundTag) a.copyWithCount(5).save(registries));
            h.assertTrue(loaded.getCount() == 5 && ItemStack.isSameItemSameComponents(a, loaded), path + ": a saved empty stack changed");
            // inventory: empty ones merge into one slot, charged ones keep their own slots
            Inventory inv = player.getInventory();
            inv.clearContent();
            inv.add(a.copyWithCount(10));
            inv.add(b.copyWithCount(6));
            h.assertTrue(inv.countItem(item) == 16 && slots(inv, item) == 1, path + ": 10 + 6 empty crystals use " + slots(inv, item) + " slots");
            inv.clearContent();
            ItemStack e = empty(path), f = empty(path);
            bat.setEnergy(e, 500);
            bat.setEnergy(f, 500);
            inv.add(e);
            inv.add(f);
            h.assertTrue(slots(inv, item) == 2, path + ": two charged crystals merged");
            inv.clearContent();
        }
        // a 0.1.10.2 save wrote an explicit zero charge on crafted and discharged crystals; it loads as a plain empty one
        for (String path : new String[]{"normal/item_crystal_lev0", "normal/item_lapotron_crystal_lev0"}) {
            ItemStack old = Round33GameTests.item(path);
            long max = ((IBatteryItem) old.getItem()).getMaxEnergy(old);
            old.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(0, max));
            ItemStack loaded = ItemStack.parseOptional(registries, (CompoundTag) old.save(registries));
            h.assertTrue(ItemStack.isSameItemSameComponents(loaded, empty(path)), path + ": an old empty crystal still differs: " + loaded.getComponentsPatch());
        }
        // a 0.1.10.2 super lapotron crystal saved full (default component) keeps its charge
        ItemStack full = Round33GameTests.item("normal/item_super_lapotron_crystal");
        IBatteryItem slc = (IBatteryItem) full.getItem();
        ItemStack fullLoaded = ItemStack.parseOptional(registries, (CompoundTag) full.save(registries));
        h.assertTrue(slc.getEnergy(fullLoaded) == slc.getMaxEnergy(fullLoaded) && fullLoaded.getMaxStackSize() == 1, "a full super lapotron crystal lost charge or stacks");
        h.succeed();
    }

    private static int slots(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n++;
        return n;
    }

    /** A stack of empty crystals is never charged as one; a charger slot takes one crystal of a stack. */
    @GameTest(batch = "round1103_stack", template = "empty", timeoutTicks = 40)
    public static void aStackIsNeverChargedAsOne(GameTestHelper h) {
        var api = MioIcifAPI.instance().getItemAPI();
        for (String path : CRYSTALS) {
            ItemStack stack = empty(path).copyWithCount(4);
            IBatteryItem bat = (IBatteryItem) stack.getItem();
            h.assertTrue(bat.addEnergy(stack, 1000) == 0, path + ": addEnergy charged a stack");
            h.assertTrue(api.chargeBattery(stack, 1000, false) == 0, path + ": the item API charged a stack");
            bat.setEnergy(stack, 1000);
            h.assertTrue(bat.getEnergy(stack) == 0, path + ": setEnergy charged a stack of 4");
            h.assertTrue(stack.getCapability(Capabilities.EnergyStorage.ITEM) == null, path + ": a stack exposes FE storage");
        }
        BlockPos box = new BlockPos(1, 1, 1);
        h.setBlock(box, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/block_bat_box")));
        AbstractEnergyStorageBlockEntity be = h.getBlockEntity(box);
        be.getEnergyStorageInternal().setStored(be.getEnergyStorageInternal().getCapacity());
        ItemStack rest = be.getItemHandler().insertItem(0, empty("normal/item_crystal_lev0").copyWithCount(4), false);
        h.assertTrue(rest.getCount() == 3 && be.getItemHandler().getStackInSlot(0).getCount() == 1,
            "the charge slot took " + be.getItemHandler().getStackInSlot(0).getCount() + " crystals of a stack");
        h.runAfterDelay(20, () -> {
            ItemStack charged = be.getItemHandler().getStackInSlot(0);
            h.assertTrue(api.getBatteryStored(charged) > 0, "the crystal in the charge slot was not charged");
            h.assertTrue(charged.getCount() == 1 && charged.getMaxStackSize() == 1, "the charged crystal can stack");
            h.assertTrue(be.getItemHandler().insertItem(0, empty("normal/item_crystal_lev0"), true).getCount() == 1,
                "an empty crystal merged into the charging one");
            h.succeed();
        });
    }
}
