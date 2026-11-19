package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.93: the fusion port recipes as the user asked: every port (the electron cyclotron heater and the neutral beam
 * injector among them) has the cryostat casing in the middle instead of a machine casing; the disruption mitigation port
 * takes the reactor coolant injector instead of a dispenser, the cryogenic port the 60k coolant cell instead of ice, the
 * exhaust port the pump instead of a hopper, and the electron cyclotron heater has a coil in each corner.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11593GameTests {
    private Round11593GameTests() {}

    private static final String[] PORTS = {"nbi_port", "ecrh_port", "fuel_port", "exhaust_port", "cryo_port", "power_port", "mitigation_port"};

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack stack(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }

    private static NonNullList<Ingredient> grid(GameTestHelper h, String port, List<String> wrong) {
        var holder = h.getLevel().getRecipeManager().byKey(id("fusion/" + port));
        if (holder.isEmpty() || !(holder.get().value() instanceof ShapedRecipe r) || r.getWidth() != 3 || r.getHeight() != 3) {
            wrong.add(port + ": no 3 x 3 shaped recipe");
            return null;
        }
        return r.getIngredients();
    }

    @GameTest(template = "empty", batch = "round11593_fusion_recipes", timeoutTicks = 20)
    public static void fusionPortRecipesUseTheCryostatAndTheNewParts(GameTestHelper h) {
        List<String> wrong = new ArrayList<>();
        ItemStack cryostat = stack("fusion/block_fusion_cryostat");
        List<ItemStack> hulls = new ArrayList<>();
        for (var key : BuiltInRegistries.ITEM.keySet())
            if (key.getNamespace().equals("mio_icif") && key.getPath().contains("machine_hull")) hulls.add(new ItemStack(BuiltInRegistries.ITEM.get(key)));
        if (hulls.isEmpty()) wrong.add("no machine casing items found to check against");
        for (String port : PORTS) {
            NonNullList<Ingredient> g = grid(h, port, wrong);
            if (g == null) continue;
            if (!g.get(4).test(cryostat)) wrong.add(port + ": no cryostat casing in the middle");
            for (Ingredient i : g)
                for (ItemStack hull : hulls)
                    if (i.test(hull)) wrong.add(port + ": still takes " + BuiltInRegistries.ITEM.getKey(hull.getItem()));
        }
        // the parts that replaced the vanilla blocks (the top middle)
        Object[][] tops = {
            {"mitigation_port", "producer/block_lapis_reactor_coolant_injector", new ItemStack(Items.DISPENSER)},
            {"cryo_port", "reactor/item_reactor_coolant_six", new ItemStack(Items.ICE)},
            {"exhaust_port", "producer/block_pump_elc", new ItemStack(Items.HOPPER)}};
        for (Object[] t : tops) {
            NonNullList<Ingredient> g = grid(h, (String) t[0], wrong);
            if (g == null) continue;
            if (!g.get(1).test(stack((String) t[1]))) wrong.add(t[0] + ": top is not " + t[1]);
            for (Ingredient i : g)
                if (i.test((ItemStack) t[2])) wrong.add(t[0] + ": still takes " + BuiltInRegistries.ITEM.getKey(((ItemStack) t[2]).getItem()));
        }
        // the electron cyclotron heater: a coil in each corner
        NonNullList<Ingredient> ecrh = grid(h, "ecrh_port", wrong);
        if (ecrh != null)
            for (int corner : new int[]{0, 2, 6, 8})
                if (!ecrh.get(corner).test(stack("resource/item_coil"))) wrong.add("ecrh_port: no coil in corner slot " + corner);
        if (wrong.isEmpty()) h.succeed();
        else h.fail(String.join("; ", wrong));
    }
}
