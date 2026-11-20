package example.sicore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 0.1.11.98: silver ore (stone and deepslate), the silver block and the raw silver block (the Shining crop's last stage needs
 * silver ore or a silver block under it), and the fusion reactor without its output cap (39.2M EU/t is the reference and the
 * full-power advancement; the power ports go up to UIV). (The mod's common classes by reflection: addon examples do not compile
 * against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11598GameTests {
    private Round11598GameTests() {}

    private static final String ORE = "block_ore_silver", DEEP = "block_ore_silver_in_deep", BLOCK = "block_silver", RAW = "block_raw_silver";
    private static final String PLASMA = "fusion.FusionPlasma", BLANKET = "fusion.FusionBlanket", PORT = "blockentity.fusion.FusionPowerPortBlockEntity";

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(id(path)); }
    private static TagKey<Block> blockTag(String ns, String path) { return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ns, path)); }
    private static TagKey<Item> itemTag(String ns, String path) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path)); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static long lng(Object o) { return ((Number) o).longValue(); }

    private static long num(String owner, String field) {
        try {
            Field f = Round33GameTests.common(owner).getField(field);
            return ((Number) f.get(null)).longValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // the superclass
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(name, e);
            }
        }
        throw new AssertionError("no field " + name);
    }

    @GameTest(template = "empty", batch = "round11598_silver", timeoutTicks = 20)
    public static void silverOreAndBlocksAreTaggedForTheShiningCrop(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        for (String name : new String[] {ORE, DEEP, BLOCK, RAW}) {
            if (block(name) == Blocks.AIR) wrong.add("no block " + name);
            if (item(name) == Items.AIR) wrong.add("no block item " + name);
        }
        BlockState ore = block(ORE).defaultBlockState(), deep = block(DEEP).defaultBlockState(), silver = block(BLOCK).defaultBlockState(),
            raw = block(RAW).defaultBlockState();
        // the Shining crop looks for c:ores/silver or c:storage_blocks/silver below it
        if (!ore.is(blockTag("c", "ores/silver")) || !deep.is(blockTag("c", "ores/silver"))) wrong.add("the silver ores are not c:ores/silver");
        if (!silver.is(blockTag("c", "storage_blocks/silver"))) wrong.add("the silver block is not c:storage_blocks/silver");
        if (!raw.is(blockTag("c", "storage_blocks/raw_silver"))) wrong.add("the raw silver block is not c:storage_blocks/raw_silver");
        if (!ore.is(blockTag("c", "ores")) || !ore.is(blockTag("c", "ores_in_ground/stone")) || !deep.is(blockTag("c", "ores_in_ground/deepslate")))
            wrong.add("the silver ores miss c:ores or c:ores_in_ground");
        if (!new ItemStack(item(ORE)).is(itemTag("c", "ores/silver")) || !new ItemStack(item(BLOCK)).is(itemTag("c", "storage_blocks/silver")))
            wrong.add("the silver item tags are missing");
        // an iron pickaxe, not a stone one
        for (BlockState s : new BlockState[] {ore, deep, silver, raw}) {
            if (!s.is(BlockTags.MINEABLE_WITH_PICKAXE) || !s.is(BlockTags.NEEDS_IRON_TOOL)) wrong.add(s.getBlock() + " is not an iron-pickaxe block");
            if (!new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(s) || new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(s))
                wrong.add(s.getBlock() + ": iron pickaxe / stone pickaxe wrong");
        }
        // the ores drop raw silver, with silk touch themselves
        var level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(1, 2, 1));
        for (BlockState s : new BlockState[] {ore, deep}) {
            List<ItemStack> drops = Block.getDrops(s, level, at, null, null, new ItemStack(Items.IRON_PICKAXE));
            if (drops.isEmpty() || !drops.get(0).is(item("resource/item_raw_silver_ore"))) wrong.add(s.getBlock() + " drops " + drops);
            ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
            silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            List<ItemStack> silkDrops = Block.getDrops(s, level, at, null, null, silk);
            if (silkDrops.isEmpty() || !silkDrops.get(0).is(s.getBlock().asItem())) wrong.add(s.getBlock() + " with silk touch drops " + silkDrops);
        }
        List<ItemStack> blockDrops = Block.getDrops(silver, level, at, null, null, new ItemStack(Items.IRON_PICKAXE));
        if (blockDrops.isEmpty() || !blockDrops.get(0).is(item(BLOCK))) wrong.add("the silver block drops " + blockDrops);
        h.assertTrue(wrong.isEmpty(), "silver blocks: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11598_silver", timeoutTicks = 20)
    public static void silverRecipesAndOreGeneration(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        var recipes = h.getLevel().getRecipeManager();
        var registries = h.getLevel().registryAccess();
        String[][] expected = {
            {"shaped_silver_block", BLOCK}, {"shaped_silver_block_to_ingot", "resource/item_silver_ingot"},
            {"compressor/silver_ingot_to_block", BLOCK}, {"block_raw_silver", RAW},
            {"block_raw_silver_uncraft", "resource/item_raw_silver_ore"}, {"smelting_silver_ore", "resource/item_silver_ingot"},
            {"smelting_silver_ore_deep", "resource/item_silver_ingot"}};
        for (String[] e : expected) {
            var r = recipes.byKey(id(e[0]));
            if (r.isEmpty()) wrong.add("no recipe " + e[0]);
            else {
                Recipe<?> recipe = r.get().value();
                if (!recipe.getResultItem(registries).is(item(e[1]))) wrong.add(e[0] + " gives " + recipe.getResultItem(registries));
            }
        }
        var smelting = recipes.byKey(id("smelting_silver_ore"));
        if (smelting.isPresent() && smelting.get().value().getIngredients().stream().noneMatch(i -> i.test(new ItemStack(item(ORE)))))
            wrong.add("the silver ore does not smelt");
        // the macerator's tag recipe takes both ores
        var powder = recipes.byKey(id("powder/silver_ore_to_silver_ore_crushed"));
        if (powder.isEmpty()) wrong.add("no macerator recipe for silver ore");
        else {
            var in = (net.minecraft.world.item.crafting.Ingredient) call(powder.get().value(), "getIngredient");
            if (!in.test(new ItemStack(item(ORE))) || !in.test(new ItemStack(item(DEEP)))) wrong.add("the macerator does not take the silver ores");
        }
        var placed = registries.registryOrThrow(Registries.PLACED_FEATURE);
        for (String f : new String[] {"silver_ore_placed", "silver_ore_deep_placed", "silver_ore_twilight_forest_placed", "silver_ore_deep_twilight_forest_placed"})
            if (!placed.containsKey(id(f))) wrong.add("no placed feature " + f);
        var modifiers = registries.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        for (String m : new String[] {"silver_ore_overworld", "silver_ore_deep_overworld"})
            if (!modifiers.containsKey(id(m))) wrong.add("no biome modifier " + m);
        h.assertTrue(wrong.isEmpty(), "silver recipes and generation: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round11598_fusion", timeoutTicks = 20)
    public static void fusionOutputHasNoCap(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        long full = num(BLANKET, "FULL_NEUTRONS"), reference = num(PLASMA, "FULL_POWER"), limit = num(PLASMA, "OUTPUT_LIMIT");
        if (reference != 39_200_000L) wrong.add("the reference output is " + reference);
        if (num(PLASMA, "MAGNET_UPKEEP") != 392_000L) wrong.add("the magnet upkeep changed: " + num(PLASMA, "MAGNET_UPKEEP"));
        // a blanket whose rods give 20,000 fission EU/t: 7.84M neutron power + 14,000 x 20,000 = 287,840,000 EU/t
        long strong = lng(call(Round33GameTests.common(BLANKET), "output", full, 0.0, 20_000.0));
        if (strong != full + 14_000L * 20_000L) wrong.add("a strong blanket gives " + strong + ", expected " + (full + 280_000_000L));
        // one power port carries it in UEV packets, and holds a tick of the grid's top voltage
        if (lng(call(Round33GameTests.common(PORT), "tierFor", strong)) != 10) wrong.add("287.84M EU/t on one port is not UEV");
        if (num(PORT, "CAPACITY") < limit) wrong.add("a power port stores only " + num(PORT, "CAPACITY") + " EU");
        // the controller's output store keeps one tick of whatever the output is
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, block("fusion/block_fusion_controller").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        BlockEntity c = h.getBlockEntity(at);
        setField(c, "output", strong);
        setField(c, "powerPorts", 1);
        if (lng(call(c, "portShare")) != strong) wrong.add("one port's share is " + call(c, "portShare") + ", not the whole " + strong);
        call(c, "returnEnergy", strong + 1_000L);
        if (lng(call(c, "buffer")) != strong) wrong.add("the output store holds " + call(c, "buffer") + ", expected one tick, " + strong);
        setField(c, "output", 0L);
        setField(c, "buffer", 0L);
        h.assertTrue(wrong.isEmpty(), "fusion without a cap: " + wrong);
        h.succeed();
    }
}
