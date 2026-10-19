package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.13: GitHub issue #18 (a recipe of another mod with a null result crashed the
 * machine drop when an explosion broke a pattern scanner) and the Wingman arm jitter (no swing restart,
 * no swing per shot, one ranged test for the combat and the pose, a bounded pose for every item).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1113GameTests {
    private Round1113GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration" + ".common.";

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static Class<?> common(String name) {
        try {
            return Class.forName(COMMON + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    /** A recipe of another mod that breaks the rules: a null result, and optionally failing ingredients. */
    private static final class BrokenRecipe implements Recipe<CraftingInput> {
        private final boolean throwingIngredients;
        BrokenRecipe(boolean throwingIngredients) { this.throwingIngredients = throwingIngredients; }
        @Override public boolean matches(CraftingInput input, Level level) { return false; }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
        @Override public boolean canCraftInDimensions(int width, int height) { return false; }
        @SuppressWarnings("DataFlowIssue")
        @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return null; }
        @Override public NonNullList<Ingredient> getIngredients() {
            if (throwingIngredients) throw new IllegalStateException("broken ingredients");
            return NonNullList.create();
        }
        @Override public RecipeSerializer<?> getSerializer() { return RecipeSerializer.SHAPELESS_RECIPE; }
        @Override public RecipeType<?> getType() { return RecipeType.CRAFTING; }
    }

    // ------------------------------------------------------------------ GitHub issue #18

    /**
     * The salvage drop of the pattern scanner skips a recipe with a null result (and one whose
     * ingredients fail) and still finds the hull of the scanner's real recipe.
     */
    @GameTest(batch = "round1113_salvage", template = "empty", timeoutTicks = 20)
    public static void salvageSkipsRecipesWithANullResult(GameTestHelper h) {
        Item scanner = BuiltInRegistries.ITEM.get(rl("producer/block_scanner_elc"));
        Item hull = BuiltInRegistries.ITEM.get(rl("producer/block_machine_hull_basic"));
        h.assertTrue(scanner != Items.AIR && hull != Items.AIR, "scanner or hull not registered");
        var registries = h.getLevel().registryAccess();
        RecipeManager manager = new RecipeManager(registries);
        manager.replaceRecipes(List.of(
            new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("other_mod", "null_result"), new BrokenRecipe(false)),
            new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("other_mod", "broken_ingredients"), new BrokenRecipe(true)),
            new RecipeHolder<>(rl("test/scanner"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(scanner),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(hull), Ingredient.of(Items.REDSTONE))))));
        Object drop = call(common("block.MachineSalvage"), "compute", h.getLevel(), manager, scanner);
        h.assertTrue(drop == hull, "salvage of the scanner: " + drop);
        Class<?> results = common("util.RecipeResults");
        h.assertTrue(((ItemStack) call(results, "of", new BrokenRecipe(false), registries)).isEmpty(), "a null result is not empty");
        h.assertTrue(((ItemStack) call(results, "of", (Object) null, registries)).isEmpty(), "no recipe is not empty");
        h.succeed();
    }

    // ------------------------------------------------------------------ Wingman arm jitter

    private static LivingEntity wingman(GameTestHelper h) {
        var w = BuiltInRegistries.ENTITY_TYPE.get(rl("wingman")).create(h.getLevel());
        h.assertTrue(w instanceof LivingEntity, "no wingman entity type");
        return (LivingEntity) w;
    }

    /** A running swing is not restarted (the arm snapped back mid-cut); after its end the next one starts. */
    @GameTest(batch = "round1113_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanSwingIsNotRestarted(GameTestHelper h) {
        LivingEntity w = wingman(h);
        w.swing(InteractionHand.MAIN_HAND);
        h.assertTrue(w.swinging && w.swingTime == -1, "the first swing did not start");
        for (int i = 0; i < 4; i++) call(w, "updateSwingTime");
        int mid = w.swingTime;
        w.swing(InteractionHand.MAIN_HAND);
        h.assertTrue(w.swinging && w.swingTime == mid, "a running swing was restarted (" + mid + " -> " + w.swingTime + ")");
        for (int i = 0; i < 10 && w.swinging; i++) call(w, "updateSwingTime");
        h.assertFalse(w.swinging, "the swing never ended");
        w.swing(InteractionHand.MAIN_HAND);
        h.assertTrue(w.swinging && w.swingTime == -1, "the next swing did not start");
        w.discard();
        h.succeed();
    }

    /** An entangled shot counts a shot (muzzle flash, recoil) but no longer swings the arm. */
    @GameTest(batch = "round1113_wingman", template = "empty", timeoutTicks = 20)
    public static void wingmanShotDoesNotSwing(GameTestHelper h) {
        LivingEntity w = wingman(h);
        w.moveTo(h.absoluteVec(new Vec3(2, 2, 2)));
        int before = (Integer) call(w, "shotCount");
        call(w, "entangledShot", h.absoluteVec(new Vec3(4, 2, 2)));
        h.assertTrue((Integer) call(w, "shotCount") == before + 1, "the shot was not counted");
        h.assertFalse(w.swinging, "the shot swung the arm");
        w.discard();
        h.succeed();
    }

    /**
     * Every registered item (this mod, vanilla, other mods): the combat kind and the arm pose agree on
     * ranged weapons, the SI guns are ranged and the melee tools are not, and the pose of every hold kind
     * is finite and bounded for any state.
     */
    @GameTest(batch = "round1113_wingman", template = "empty", timeoutTicks = 40)
    public static void everyItemHasABoundedPose(GameTestHelper h) {
        Class<?> pose = common("armory.WingmanPose"), entity = common("armory.WingmanEntity");
        List<String> problems = new ArrayList<>();
        int items = 0, ranged = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            boolean isRanged = (Boolean) call(pose, "ranged", stack);
            String kind = String.valueOf(call(entity, "kindOf", stack));
            String hold = String.valueOf(call(pose, "holdOf", stack, false));
            if (isRanged != kind.equals("RANGED") && !kind.equals("BLADE")) problems.add(BuiltInRegistries.ITEM.getKey(item) + " " + kind + "/" + isRanged);
            if (!stack.isEmpty() && hold.equals("RANGED") != isRanged) problems.add(BuiltInRegistries.ITEM.getKey(item) + " hold " + hold);
            items++;
            if (isRanged) ranged++;
        }
        for (String id : new String[]{"item_tool_electric_rifle", "item_tool_advanced_electric_rifle", "item_tool_tactical_laser_rifle",
                "item_tool_electric_plasma_gun", "item_tool_tachyon_disruptor", "item_tool_nano_bow", "item_tool_rocket_launcher",
                "item_tool_plasma_launcher", "item_tool_laser_miner"})
            if (!(Boolean) call(pose, "ranged", Round33GameTests.item(id))) problems.add(id + " is not ranged");
        for (String id : new String[]{"item_tool_nanosaber", "item_tool_quantum_sword", "item_tool_iron_driller", "item_tool_iron_chainsaw"})
            if ((Boolean) call(pose, "ranged", Round33GameTests.item(id))) problems.add(id + " is ranged");
        float limit;
        try {
            limit = pose.getField("LIMIT").getFloat(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("LIMIT", e);
        }
        for (Object hold : common("armory.WingmanPose$Hold").getEnumConstants())
            for (boolean left : new boolean[]{false, true})
                for (float f : new float[]{0, 0.5F, 1, Float.NaN}) for (float c : new float[]{0, 1, 7}) for (float pitch : new float[]{-3, 0, 3}) {
                    float[] p = (float[]) call(pose, "base", hold, left, f, 1.0F, c, 0.05F, pitch, 2.5F, 1.0F);
                    for (float v : p) if (!Float.isFinite(v) || Math.abs(v) > limit) { problems.add(hold + " pose " + v); break; }
                }
        h.assertTrue(problems.isEmpty(), "problems: " + problems);
        h.assertTrue(ranged >= 9 && items > ranged, "items " + items + ", ranged " + ranged);
        h.succeed();
    }
}
