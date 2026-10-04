package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.30: the canner takes any edible item (other mods' food), filled tin cans are quick to eat,
 * restore IC2 amounts and give the empty can back, and an item in a storage box's charge slot no
 * longer stops the box from sending power.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round30GameTests {
    private Round30GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static BlockState block(String path) { return BuiltInRegistries.BLOCK.get(id(path)).defaultBlockState(); }
    private static BlockState facing(String path, Direction side) {
        BlockState s = block(path);
        var p = s.getBlock().getStateDefinition().getProperty("facing");
        return p instanceof DirectionProperty d && d.getPossibleValues().contains(side) ? s.setValue(d, side) : s;
    }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    private static Object dynamicCanningRecipe(GameTestHelper h) {
        for (var holder : h.getLevel().getRecipeManager().getRecipes()) {
            if (holder.value().getClass().getSimpleName().equals("DynamicCanningRecipe")) return holder.value();
        }
        throw new AssertionError("no dynamic canning recipe loaded");
    }

    @GameTest(batch = "round30_canner", template = "empty", timeoutTicks = 20)
    public static void cannerAcceptsAnyEdibleItem(GameTestHelper h) {
        Object recipe = dynamicCanningRecipe(h);
        // a "mod food": an item outside the cannable_foods tag that only carries a food component
        ItemStack modFood = new ItemStack(Items.STICK);
        modFood.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(5).saturationModifier(0.3F).build());
        h.assertTrue((boolean) call(recipe, "acceptsFood", modFood), "food from other mods (food component only) is refused");
        h.assertTrue((boolean) call(recipe, "acceptsFood", new ItemStack(Items.SWEET_BERRIES)), "sweet berries refused");
        h.assertTrue(!(boolean) call(recipe, "acceptsFood", new ItemStack(Items.STICK)), "a non-food item was accepted");
        h.assertTrue(!(boolean) call(recipe, "acceptsFood", item("normal/item_tin_filled_can")), "a filled can was accepted as food");
        h.succeed();
    }

    @GameTest(batch = "round30_cans", template = "empty", timeoutTicks = 20)
    public static void tinCansAreFastFillingAndReturnTheCan(GameTestHelper h) {
        ItemStack cans = item("normal/item_tin_filled_can").copyWithCount(3);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(cans.getUseDuration(player) <= 16, "canned food still takes " + cans.getUseDuration(player) + " ticks to eat");
        FoodProperties food = cans.get(DataComponents.FOOD);
        h.assertTrue(food != null && food.nutrition() >= 2 && food.saturation() >= 3.0F, "canned food restores too little: " + food);

        player.getFoodData().setFoodLevel(4);
        player.getFoodData().setSaturation(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, cans);
        ItemStack rest = cans.finishUsingItem(h.getLevel(), player);
        h.assertTrue(rest.getCount() == 2, "one can should be eaten, " + rest.getCount() + " left");
        h.assertTrue(player.getFoodData().getFoodLevel() == 6, "hunger after one can: " + player.getFoodData().getFoodLevel());
        h.assertTrue(player.getInventory().countItem(BuiltInRegistries.ITEM.get(id("normal/item_tin_empty_can"))) == 1,
            "the empty tin can was not given back");

        ItemStack last = item("normal/item_tin_filled_can");
        ItemStack after = last.finishUsingItem(h.getLevel(), player);
        h.assertTrue(after.is(BuiltInRegistries.ITEM.get(id("normal/item_tin_empty_can"))), "the last can must leave an empty can in hand, got " + after);
        h.succeed();
    }

    @GameTest(batch = "round30_storage", template = "reactor_loop", timeoutTicks = 120)
    public static void chargingItemDoesNotStopStorageOutput(GameTestHelper h) {
        BlockPos chargingBox = new BlockPos(2, 2, 2), plainBox = new BlockPos(2, 2, 6);
        h.setBlock(chargingBox, facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(plainBox, facing("wiring/block_mfsu", Direction.EAST));
        h.setBlock(chargingBox.east(), facing("wiring/block_eesu", Direction.EAST));
        h.setBlock(plainBox.east(), facing("wiring/block_eesu", Direction.EAST));
        long[] start = new long[2];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            AbstractEnergyStorageBlockEntity a = h.getBlockEntity(chargingBox), b = h.getBlockEntity(plainBox);
            AbstractEnergyStorageBlockEntity sa = h.getBlockEntity(chargingBox.east()), sb = h.getBlockEntity(plainBox.east());
            if (now == 5) {
                a.getEnergyStorageInternal().setStored(a.getEnergyStorageInternal().getCapacity());
                b.getEnergyStorageInternal().setStored(b.getEnergyStorageInternal().getCapacity());
                // a crystal accepts more per tick than the MFSU's tier rate: it used to eat the whole output allowance
                ItemStack crystal = item("normal/item_super_lapotron_crystal");
                com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI().setBatteryEnergy(crystal, 0);   // spawns full
                a.setItem(0, crystal);
                start[0] = sa.getEnergyStorageInternal().getAmount();
                start[1] = sb.getEnergyStorageInternal().getAmount();
            }
            if (now == 85) {
                long viaCharging = sa.getEnergyStorageInternal().getAmount() - start[0];
                long viaPlain = sb.getEnergyStorageInternal().getAmount() - start[1];
                var api = com.miophas.singularity_iteration.core.api.MioIcifAPI.instance().getItemAPI();
                h.assertTrue(api.getBatteryStored(a.getItem(0)) > 0, "the crystal in the charge slot was not charged");
                h.assertTrue(viaPlain > 0, "reference MFSU sent nothing");
                h.assertTrue(viaCharging * 2 >= viaPlain,
                    "charging an item throttled the box's output: " + viaCharging + " EU vs " + viaPlain + " EU without an item");
                h.succeed();
            }
        });
    }
}
