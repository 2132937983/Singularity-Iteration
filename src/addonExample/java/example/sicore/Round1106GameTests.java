package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.6 (patch): ejector routing and the ping-pong guard, the overclock throttle and
 * the displayed progress, Milk Wart, the Weed-EX fluid name, the Crop Manager and other fluid
 * machines taking every fluid container, the UU tank of the Next-Gen Crop Analyzer.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1106GameTests {
    private Round1106GameTests() {}

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static ItemStack craft(GameTestHelper h, String[] grid) {
        List<ItemStack> items = new ArrayList<>();
        for (String id : grid) items.add(id == null ? ItemStack.EMPTY
            : new ItemStack(BuiltInRegistries.ITEM.get(id.contains(":") ? ResourceLocation.parse(id) : rl(id))));
        CraftingInput input = CraftingInput.of(3, 3, items);
        var level = h.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
            .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ 1. ejector routing

    @GameTest(batch = "round1106_routing", template = "empty", timeoutTicks = 20)
    public static void storageComesBeforeMachines(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(rl("producer/block_powder_elc")));
        h.setBlock(at.east(), Blocks.CHEST);
        h.setBlock(at.west(), BuiltInRegistries.BLOCK.get(rl("producer/block_furnace_elc")));
        h.setBlock(at.north(), Blocks.STONE);
        var macerator = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        Direction east = h.getTestRotation().rotate(Direction.EAST), west = h.getTestRotation().rotate(Direction.WEST),
            north = h.getTestRotation().rotate(Direction.NORTH);
        h.assertTrue((Integer) Round33GameTests.call(macerator, "neighbourKind", east) == 0, "a chest is not storage");
        h.assertTrue((Integer) Round33GameTests.call(macerator, "neighbourKind", west) == 2, "a machine is not a processor");
        h.assertTrue((Integer) Round33GameTests.call(macerator, "neighbourKind", north) == 1, "stone is not 'other'");
        Direction[] order = (Direction[]) Round33GameTests.call(macerator, "byTargetPriority", (Iterable<Direction>) List.of(west, north, east));
        h.assertTrue(order[0] == east && order[1] == north && order[2] == west, "wrong order: " + java.util.Arrays.toString(order));
        h.assertTrue(BuiltInRegistries.BLOCK.get(rl("build/block_wood_storage")).defaultBlockState()
            .is(TagKey.create(Registries.BLOCK, rl("automation/storage"))), "the wooden crate is not in mio_icif:automation/storage");
        h.succeed();
    }

    @GameTest(batch = "round1106_routing", template = "empty", timeoutTicks = 20)
    public static void noPingPongBetweenMachines(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(rl("producer/block_powder_elc")));
        h.setBlock(at.east(), BuiltInRegistries.BLOCK.get(rl("producer/block_furnace_elc")));
        h.setBlock(at.west(), Blocks.CHEST);
        var macerator = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        Direction east = h.getTestRotation().rotate(Direction.EAST), west = h.getTestRotation().rotate(Direction.WEST);
        h.assertTrue(!(Boolean) Round33GameTests.call(macerator, "pingPong", east, true), "a fresh machine is blocked");
        Round33GameTests.call(macerator, "noteTransfer", east, false);   // pulled from the furnace
        h.assertTrue((Boolean) Round33GameTests.call(macerator, "pingPong", east, true), "ejects back into the machine it pulled from");
        Round33GameTests.call(macerator, "noteTransfer", west, false);   // pulled from the chest
        h.assertTrue(!(Boolean) Round33GameTests.call(macerator, "pingPong", west, true), "storage is limited");
        h.succeed();
    }

    // ------------------------------------------------------------------ 2./3. overclock throttle

    @GameTest(batch = "round1106_overclock", template = "empty", timeoutTicks = 20)
    public static void throttleRoundsAndSaves(GameTestHelper h) {
        h.assertTrue(AbstractProcessingMachineBlockEntity.activeOverclockers(3, 50) == 2
            && AbstractProcessingMachineBlockEntity.activeOverclockers(3, 0) == 0
            && AbstractProcessingMachineBlockEntity.activeOverclockers(3, 100) == 3
            && AbstractProcessingMachineBlockEntity.activeOverclockers(4, 30) == 1, "activeOverclockers rounds wrong");
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(rl("producer/block_furnace_elc")));
        var furnace = (AbstractProcessingMachineBlockEntity) h.getBlockEntity(at);
        furnace.setOverclockThrottle(140);
        h.assertTrue(furnace.getOverclockThrottle() == 100, "the throttle is not clamped");
        furnace.setOverclockThrottle(40);
        var tag = furnace.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(tag.getInt("scex_oc_throttle") == 40, "the throttle is not saved");
        furnace.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(furnace.getOverclockThrottle() == 40, "the throttle is not loaded");
        h.assertTrue(furnace.getDisplayProgress() == furnace.getProgress(), "an idle machine shows a progress");
        h.succeed();
    }

    // ------------------------------------------------------------------ 8./9. Milk Wart and Weed-EX

    @GameTest(batch = "round1106_food", template = "empty", timeoutTicks = 20)
    public static void milkWartCuresAndCountsAsMilk(GameTestHelper h) {
        Item wart = BuiltInRegistries.ITEM.get(rl("resource/item_milk_wart"));
        ItemStack stack = new ItemStack(wart, 2);
        for (String tag : new String[]{"c:buckets/milk", "c:drinks/milk", "c:milks", "forge:milk"})
            h.assertTrue(stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))), "milk wart is not in " + tag);
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        p.getFoodData().setFoodLevel(10);
        ItemStack left = stack.finishUsingItem(h.getLevel(), p);
        h.assertTrue(!p.hasEffect(MobEffects.POISON), "milk wart did not cure poison");
        h.assertTrue(p.getFoodData().getFoodLevel() == 12, "milk wart gave " + (p.getFoodData().getFoodLevel() - 10) + " food");
        h.assertTrue(left.getCount() == 1, "eating used " + (2 - left.getCount()) + " warts");
        Round33GameTests.remove(p);
        String w = "resource/item_milk_wart";
        ItemStack cake = craft(h, new String[]{w, "minecraft:milk_bucket", w, "minecraft:sugar", "minecraft:egg", "minecraft:sugar",
            "minecraft:wheat", "minecraft:wheat", "minecraft:wheat"});
        h.assertTrue(cake.is(Items.CAKE), "milk warts do not make a cake");
        h.succeed();
    }

    @GameTest(batch = "round1106_food", template = "empty", timeoutTicks = 20)
    public static void weedExFluidHasItsName(GameTestHelper h) {
        var fluid = BuiltInRegistries.FLUID.get(rl("weed_ex"));
        h.assertTrue("fluid_type.mio_icif.weed_ex".equals(fluid.getFluidType().getDescriptionId()), "Weed-EX key: " + fluid.getFluidType().getDescriptionId());
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            h.assertTrue(json != null && json.contains("\"fluid_type.mio_icif.weed_ex\""), "no Weed-EX name in " + lang);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ 10. Crop Manager and fluid containers

    @GameTest(batch = "round1106_fluids", template = "empty", timeoutTicks = 20)
    public static void cropManagerTakesContainersFromTheSide(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(rl("producer/block_matron_elc")));
        BlockPos abs = h.absolutePos(at);
        var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.NORTH);
        h.assertTrue(items != null, "no item handler on the side");
        ItemStack weedBucket = new ItemStack(BuiltInRegistries.ITEM.get(rl("weed_ex_bucket")));
        ItemStack rest = items.insertItem(10, weedBucket.copy(), false);
        h.assertTrue(rest.isEmpty(), "the side refused a Weed-EX bucket");
        h.assertTrue(items.insertItem(1, new ItemStack(Items.WATER_BUCKET), false).isEmpty(), "the side refused a water bucket");
        var be = h.getBlockEntity(at);
        Round33GameTests.call(be, "processWeedExCell");
        Round33GameTests.call(be, "processWaterCell");
        IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, Direction.EAST);
        h.assertTrue(fluids != null && fluids.getFluidInTank(0).getAmount() == 1000 && fluids.getFluidInTank(1).getAmount() == 1000,
            "tanks after the buckets: " + (fluids == null ? "none" : fluids.getFluidInTank(0).getAmount() + "/" + fluids.getFluidInTank(1).getAmount()));
        h.assertTrue(items.getStackInSlot(2).is(Items.BUCKET) && items.getStackInSlot(2).getCount() == 2,
            "the empty buckets were not kept: " + items.getStackInSlot(2));
        int filled = fluids.fill(new FluidStack(BuiltInRegistries.FLUID.get(rl("weed_ex")), 500), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 500, "a pipe could not fill Weed-EX: " + filled);
        h.succeed();
    }

    @GameTest(batch = "round1106_fluids", template = "empty", timeoutTicks = 20)
    public static void heatExchangerTakesHotCoolantBuckets(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(rl("hugenerator/block_heat_source_fluid")));
        var be = h.getBlockEntity(at);
        ItemStack bucket = new ItemStack(BuiltInRegistries.ITEM.get(rl("hotcoolant_bucket")));
        h.assertTrue((Boolean) Round33GameTests.call(be, "isItemValidForSlot", 0, bucket), "a hot coolant bucket is refused");
        h.succeed();
    }
}
