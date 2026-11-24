package example.sicore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of 0.1.11.200: tool overhaul T5c, the multimeter (EU meter with energy, wind and geomagnetic probes). */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111200GameTests {
    private Round111200GameTests() {}

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
    }

    /** D7: G cycles energy, wind, geomagnetic; a wind or geomagnetic reading needs no energy and no block. */
    @GameTest(template = "empty")
    public static void multimeterCyclesItsProbes(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack meter = stack("item_tool_meter");
        p.setItemInHand(InteractionHand.MAIN_HAND, meter);
        var item = meter.getItem();
        h.assertTrue((int) ArmoryServiceGameTests.call(item, "toolModeIndex", meter) == 0, "the meter starts on the energy probe");
        ArmoryServiceGameTests.call(item, "toggleActive", meter, p);
        h.assertTrue((int) ArmoryServiceGameTests.call(item, "toolModeIndex", meter) == 1, "G switches to the wind probe");
        h.assertTrue(item.use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "a wind reading in the air");
        ArmoryServiceGameTests.call(item, "toggleActive", meter, p);
        h.assertTrue((int) ArmoryServiceGameTests.call(item, "toolModeIndex", meter) == 2, "then the geomagnetic probe");
        h.assertTrue(item.use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "a geomagnetic reading");
        ArmoryServiceGameTests.call(item, "toggleActive", meter, p);
        h.assertTrue((int) ArmoryServiceGameTests.call(item, "toolModeIndex", meter) == 0, "and back to energy");
        p.discard();
        h.succeed();
    }

    /** D7: the old wind meter and geomagnetic detector craft into the multimeter. */
    @GameTest(template = "empty")
    public static void oldMetersConvertIntoTheMultimeter(GameTestHelper h) {
        for (String id : new String[] {"item_tool_windmeter", "item_geomagnetic_detector"}) {
            var input = CraftingInput.of(1, 1, java.util.List.of(stack(id)));
            var made = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input, h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
            h.assertTrue(made.is(stack("item_tool_meter").getItem()), id + " crafts into the multimeter, made " + made);
        }
        h.succeed();
    }
}
