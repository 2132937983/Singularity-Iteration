package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.machine.IStorageMember;
import com.miophas.singularity_iteration.core.prefab.blockentity.MultiblockEnergyCore;
import com.miophas.singularity_iteration.core.runtime.energy.DirectItemCharging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.29: armor equip sounds are never null (Presence Footsteps crash), storage GUIs are titled
 * with their own block, the GESU charge / discharge slots work, the energy terminal walker sees a
 * GESU as one store, and repeated recipe lookups are memoised.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round29GameTests {
    private Round29GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    private static Class<?> walker() {
        try {
            return Class.forName(COMMON + "blockentity.wiring.terminal.NetworkWalker");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @GameTest(batch = "round29_sound", template = "empty", timeoutTicks = 20)
    public static void armorEquipSoundIsNeverNull(GameTestHelper h) {
        boolean mixed = false;
        for (var m : ArmorItem.class.getDeclaredMethods()) mixed |= m.getName().contains("neverNull");
        h.assertTrue(mixed, "ArmorEquipSoundMixin is not applied to ArmorItem");
        int armor = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ArmorItem a)) continue;
            armor++;
            h.assertTrue(a.getEquipSound() != null, "null equip sound on " + BuiltInRegistries.ITEM.getKey(item));
        }
        h.assertTrue(armor > 0, "no armor items registered");
        h.succeed();
    }

    @GameTest(batch = "round29_title", template = "empty", timeoutTicks = 20)
    public static void storageGuisUseTheirOwnBlockName(GameTestHelper h) {
        String[] ids = {"wiring/block_cesu", "wiring/block_batbox_charger", "wiring/block_mfe_charger",
            "wiring/block_mfsu_charger", "wiring/block_lesu_charger", "wiring/block_eesu_charger", "wiring/block_bat_box"};
        for (int i = 0; i < ids.length; i++) {
            BlockPos at = new BlockPos(1 + i, 1, 1);
            h.setBlock(at, block(ids[i]));
            BlockEntity be = h.getBlockEntity(at);
            h.assertTrue(be instanceof MenuProvider, ids[i] + " has no menu");
            String shown = ((MenuProvider) be).getDisplayName().getString();
            String expected = block(ids[i]).getName().getString();
            h.assertTrue(shown.equals(expected), ids[i] + " GUI title '" + shown + "' instead of '" + expected + "'");
        }
        h.succeed();
    }

    private static BlockPos buildGesu(GameTestHelper h) {
        BlockPos core = new BlockPos(3, 2, 3);
        for (Direction d : Direction.values()) {
            h.setBlock(core.relative(d), block(d == Direction.UP ? "wiring/block_gesu_output_iv" : "wiring/block_gesu_input_iv"));
        }
        h.setBlock(core, block("wiring/block_gesu_core"));
        return core;
    }

    @GameTest(batch = "round29_gesu_slots", template = "reactor_loop", timeoutTicks = 120)
    public static void gesuChargeAndDischargeSlotsWork(GameTestHelper h) {
        BlockPos core = buildGesu(h);
        var api = MioIcifAPI.instance().getItemAPI();
        int[] tick = {0};
        long[] before = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            MultiblockEnergyCore gesu = h.getBlockEntity(core);
            Container slots = (Container) gesu;
            if (now == 5) {
                h.assertTrue(slots.getContainerSize() == 2, "GESU core has no charge / discharge slots");
                gesu.getEnergyStorageInternal().setStored(1_000_000);
                ItemStack empty = item("normal/item_lithium_battery");
                h.assertTrue(DirectItemCharging.canCharge(empty), "a lithium battery must be accepted in the charge slot");
                slots.setItem(0, empty);
                before[0] = gesu.getStoredEnergy();
            }
            if (now == 25) {
                long charged = api.getBatteryStored(slots.getItem(0));
                long spent = before[0] - gesu.getStoredEnergy();
                h.assertTrue(charged > 0, "the battery in the GESU charge slot was not charged");
                h.assertTrue(spent >= charged, "energy appeared from nowhere: core spent " + spent + ", battery got " + charged);
                // discharge slot: a full battery empties into the core
                ItemStack full = item("normal/item_lithium_battery");
                api.setBatteryEnergy(full, api.getBatteryCapacity(full));
                slots.setItem(1, full);
                before[0] = gesu.getStoredEnergy();
            }
            if (now == 45) {
                h.assertTrue(gesu.getStoredEnergy() > before[0] - 200_000, "discharge slot did not feed the core");
                h.assertTrue(api.getBatteryStored(slots.getItem(1)) < api.getBatteryCapacity(slots.getItem(1)),
                    "the battery in the discharge slot kept its charge");
                h.succeed();
            }
        });
    }

    @GameTest(batch = "round29_gesu_walker", template = "reactor_loop", timeoutTicks = 60)
    public static void terminalWalkerTreatsGesuAsOneStore(GameTestHelper h) {
        BlockPos core = buildGesu(h);
        h.runAfterDelay(10, () -> {
            BlockEntity coreBe = h.getBlockEntity(core);
            BlockEntity input = h.getBlockEntity(core.below());
            BlockEntity output = h.getBlockEntity(core.above());
            h.assertTrue(input instanceof IStorageMember && output instanceof IStorageMember, "GESU ports are not storage members");
            for (BlockEntity be : new BlockEntity[]{coreBe, input, output}) {
                Object cat = call(walker(), "categorize", be);
                h.assertTrue(cat != null && cat.toString().equals("STORAGE"), be.getType() + " categorised as " + cat);
                h.assertTrue((boolean) call(walker(), "isBuffer", be), be.getType() + " is not a voltage buffer");
            }
            h.assertTrue(call(walker(), "storageOwner", input) == coreBe, "the input port must report the core's store");
            h.assertTrue(call(walker(), "storageOwner", output) == coreBe, "the output port must report the core's store");
            h.succeed();
        });
    }

    @GameTest(batch = "round29_recipe", template = "empty", timeoutTicks = 20)
    public static void repeatedRecipeLookupsAreStable(GameTestHelper h) {
        var recipes = MioIcifAPI.instance().getRecipeAPI();
        var level = h.getLevel();
        for (ItemStack in : new ItemStack[]{new ItemStack(Items.COBBLESTONE), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.DIRT)}) {
            var first = recipes.findCompressorRecipe(in, level);
            var second = recipes.findCompressorRecipe(in.copyWithCount(17), level);
            h.assertTrue(first.isPresent() == second.isPresent()
                    && (first.isEmpty() || first.get() == second.get()),
                "memoised lookup differs for " + in);
        }
        h.succeed();
    }
}
