package example.sicore;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.42: no two crafting recipes share a grid with different results (the 0.1.10.5 MOX fuel recipe and the old
 * MOX rod recipe had the same one), and heat goes in and out of each heat machine only on the faces it declares (a generic
 * adapter opened every face: a heat generator behind a blast furnace heated it).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1142GameTests {
    private Round1142GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    /** One ingredient as a comparable text: a custom ingredient by itself, a plain one by its sorted item ids. */
    private static String signature(Ingredient ingredient) {
        if (ingredient.isEmpty()) return "-";
        if (ingredient.isCustom()) return ingredient.getCustomIngredient().toString();
        List<String> ids = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        ids.sort(null);
        return String.join("|", ids);
    }

    /** No two crafting recipes of the mod take the same grid and give different items. */
    @GameTest(batch = "round1142_recipes", template = "empty", timeoutTicks = 20)
    public static void noTwoCraftingRecipesShareAGrid(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        Map<String, String> grids = new HashMap<>();
        List<String> clashes = new ArrayList<>();
        int checked = 0;
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!holder.id().getNamespace().equals("mio_icif")) continue;
            var recipe = holder.value();
            List<String> parts = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) parts.add(signature(ingredient));
            String grid;
            if (recipe instanceof ShapedRecipe shaped) grid = "shaped " + shaped.getWidth() + "x" + shaped.getHeight() + " " + parts;
            else { parts.sort(null); grid = "shapeless " + parts; }
            String result = BuiltInRegistries.ITEM.getKey(recipe.getResultItem(registries).getItem()) + " x" + recipe.getResultItem(registries).getCount();
            String earlier = grids.putIfAbsent(grid, holder.id() + " -> " + result);
            if (earlier != null && !earlier.endsWith(" -> " + result)) clashes.add(earlier + " / " + holder.id() + " -> " + result);
            checked++;
        }
        h.assertTrue(checked > 100, "only " + checked + " crafting recipes");
        h.assertTrue(clashes.isEmpty(), "crafting recipes with the same grid and different results: " + clashes);
        h.succeed();
    }

    private static final BiPredicate<Direction, Direction> FRONT_ONLY = (side, front) -> side == front;
    private static final BiPredicate<Direction, Direction> ALL_BUT_FRONT = (side, front) -> side != front;
    private static final BiPredicate<Direction, Direction> ALL = (side, front) -> true;

    @SuppressWarnings("unchecked")
    private static BlockState facing(Block block, Direction front) {
        Property<?> facing = block.getStateDefinition().getProperty("facing");
        if (facing == null) return block.defaultBlockState();
        return block.defaultBlockState().setValue((Property<Direction>) facing, front);
    }

    /**
     * Heat faces of the eleven machines the generic adapter opened: the blast furnaces and the fermenter take heat on the front
     * only, the heat generators give it on the front only, the steam generator, the repressurizer and the Stirling kinetic
     * generator take it everywhere but the front, the advanced Stirling generator and the induction furnace on every face.
     */
    @GameTest(batch = "round1142_heat", template = "empty", timeoutTicks = 20)
    public static void heatMachinesOpenOnlyTheirFaces(GameTestHelper h) {
        Map<String, BiPredicate<Direction, Direction>> rules = new java.util.LinkedHashMap<>();
        for (String path : List.of("producer/block_blast_furnace", "producer/block_blast_furnace_advanced", "producer/block_fermenter_elc",
                "hugenerator/block_solid_heat_generator", "hugenerator/block_fluid_heat_generator", "hugenerator/block_rt_heat_generator"))
            rules.put(path, FRONT_ONLY);
        for (String path : List.of("producer/block_steam_generator", "producer/block_steam_repressurizer", "kugenerator/block_stirling_kinetic_generator"))
            rules.put(path, ALL_BUT_FRONT);
        for (String path : List.of("generator/block_advanced_stirling_generator", "producer/block_induction_elc")) rules.put(path, ALL);
        BlockPos at = new BlockPos(2, 1, 2);
        List<String> problems = new ArrayList<>();
        rules.forEach((path, open) -> {
            Block block = BuiltInRegistries.BLOCK.get(id(path));
            if (block == Blocks.AIR) { problems.add(path + " not registered"); return; }
            h.setBlock(at, facing(block, Direction.EAST));
            for (Direction side : Direction.values()) {
                boolean has = h.getLevel().getCapability(IMioIcifCapabilities.HEAT_STORAGE_BLOCK, h.absolutePos(at), side) != null;
                if (has != open.test(side, Direction.EAST)) problems.add(path + " " + side + (has ? " open" : " closed"));
            }
            h.setBlock(at, Blocks.AIR);
        });
        h.assertTrue(problems.isEmpty(), "heat faces: " + problems);
        h.succeed();
    }

    /** A solid heat generator pointing at the back of a blast furnace finds no receiver; pointing at its front it does. */
    @GameTest(batch = "round1142_heat", template = "empty", timeoutTicks = 20)
    public static void aGeneratorBehindABlastFurnaceDoesNotHeatIt(GameTestHelper h) {
        BlockPos furnace = new BlockPos(2, 1, 2);
        Block generator = BuiltInRegistries.BLOCK.get(id("hugenerator/block_solid_heat_generator"));
        h.setBlock(furnace, facing(BuiltInRegistries.BLOCK.get(id("producer/block_blast_furnace")), Direction.EAST));
        h.setBlock(furnace.west(), facing(generator, Direction.EAST));    // its front on the furnace's back
        h.assertTrue(ThermalOutput.front(h.getBlockEntity(furnace.west())) == null, "the generator heats the furnace through its back");
        h.assertTrue(ThermalOutput.undeclared(h.getLevel(), h.absolutePos(furnace)) == null, "the fallback reaches the furnace past its capability");
        h.setBlock(furnace.west(), Blocks.AIR);
        h.setBlock(furnace.east(), facing(generator, Direction.WEST));    // its front on the furnace's front
        h.assertTrue(ThermalOutput.front(h.getBlockEntity(furnace.east())) != null, "the generator does not reach the furnace's front");
        h.succeed();
    }
}
