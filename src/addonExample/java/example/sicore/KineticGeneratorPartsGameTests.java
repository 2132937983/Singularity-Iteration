package example.sicore;

import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Every kinetic generator but the Electric one needs its lathe-shaped iron blank (IC2: Water, Stirling;
 * added: Manual, Wind, Steam). The Electric Kinetic Generator needs none and drives the first lathe,
 * and its GUI data reports the KU it hands over.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class KineticGeneratorPartsGameTests {
    private KineticGeneratorPartsGameTests() {}

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(rl(path))); }
    /** 0.1.10.38: a lathe-shaped part (mio_icif:lathe_state); electric and machine inputs (mio_icif:electric) are custom too. */
    private static boolean lathePart(Ingredient i) {
        return i.isCustom() && rl("lathe_state").equals(net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES.getKey(i.getCustomIngredient().getType()));
    }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static ItemStack blank(int[] state) {
        ItemStack stack = item("normal/item_iron_turning_blanks");
        for (int j = 0; j < 5; j++) if (state[j] < 5) ((ILatheItem) stack.getItem()).setState(stack, j, state[j]);
        return stack;
    }

    @GameTest(batch = "kinetic_parts", template = "empty", timeoutTicks = 20)
    public static void kineticGeneratorsTakeTheirParts(GameTestHelper h) {
        Object[][] cases = {
            {"block_manual_kinetic_generator", new int[]{5, 5, 5, 2, 2}},
            {"shaped_34_wind_kinetic_generator", new int[]{5, 3, 3, 3, 5}},
            {"shaped_324_steam_kinetic_generator", new int[]{1, 3, 5, 3, 1}},
            {"shaped_323_water_kinetic_generator", new int[]{4, 3, 4, 3, 4}},
            {"shaped_322_stirling_kinetic_generator", new int[]{3, 2, 3, 2, 3}}};
        for (Object[] c : cases) {
            var holder = h.getLevel().getRecipeManager().byKey(rl((String) c[0]));
            h.assertTrue(holder.isPresent(), "no recipe " + c[0]);
            Ingredient part = null;
            for (Ingredient i : holder.get().value().getIngredients()) if (lathePart(i)) part = i;
            h.assertTrue(part != null, c[0] + " has no lathe-shaped part");
            h.assertTrue(part.test(blank((int[]) c[1])), c[0] + " does not take its part");
            h.assertTrue(!part.test(item("normal/item_iron_turning_blanks")), c[0] + " takes an uncut blank");
        }
        h.assertTrue(h.getLevel().getRecipeManager().byKey(rl("shapeless_209_manual_kinetic_generator")).isEmpty(),
            "the old manual kinetic generator recipe without a part is still there");

        // the Electric Kinetic Generator needs no lathe part: it powers the first lathe
        for (RecipeHolder<?> r : h.getLevel().getRecipeManager().getRecipes()) {
            if (!BuiltInRegistries.ITEM.getKey(r.value().getResultItem(h.getLevel().registryAccess()).getItem())
                    .getPath().equals("kugenerator/block_kinetic_generator_elc")) continue;
            for (Ingredient i : r.value().getIngredients()) h.assertTrue(!lathePart(i), r.id() + " needs a lathe part");
        }
        h.succeed();
    }

    /** Bootstrap: Electric Kinetic Generator (3 motors, EU) facing a Lathe fills the Lathe's KU buffer; the GUI data shows it. */
    @GameTest(batch = "kinetic_parts", template = "empty", timeoutTicks = 100)
    public static void electricKineticGeneratorDrivesTheLathe(GameTestHelper h) {
        BlockPos lathePos = new BlockPos(1, 1, 1), genPos = new BlockPos(2, 1, 1);
        h.setBlock(lathePos, BuiltInRegistries.BLOCK.get(rl("producer/block_lathe")));
        h.setBlock(genPos, BuiltInRegistries.BLOCK.get(rl("kugenerator/block_kinetic_generator_elc")).defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.WEST));
        BlockEntity lathe = h.getBlockEntity(lathePos);
        BlockEntity gen = h.getBlockEntity(genPos);
        IItemHandler motors = (IItemHandler) call(gen, "getItemHandler");
        for (int slot = 1; slot <= 3; slot++) motors.insertItem(slot, item("resource/item_motor"), false);
        ((AbstractEnergyBlockEntity) gen).getEnergyStorageInternal().setEnergy(40_000);

        var player = h.makeMockServerPlayerInLevel();
        Object menu = call(gen, "createMenu", 0, player.getInventory(), player);
        h.succeedWhen(() -> {
            int ku;
            try { ku = lathe.getClass().getField("kUBuffer").getInt(lathe); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            h.assertTrue(ku > 0, "the lathe got no KU");
            int shown = (Integer) call(menu, "getKineticGeneration");
            h.assertTrue(shown > 0 && shown <= 300, "GUI shows " + shown + " KU/t");
            h.assertTrue((Integer) call(menu, "getMaxKineticGeneration") == 300, "3 motors allow 300 KU/t");
            // the client gets every value as a short (ClientboundContainerSetDataPacket): 40000 EU must survive
            long energy = ((AbstractEnergyBlockEntity) gen).getEnergyStorageInternal().getAmount();
            var client = clientCopy(menu, player.getInventory());
            h.assertTrue((Integer) call(client, "getEnergy") == energy, "GUI energy " + call(client, "getEnergy") + " != " + energy);
            h.assertTrue((Integer) call(client, "getMaxEnergy") >= 40_000, "GUI capacity " + call(client, "getMaxEnergy"));
            h.assertTrue((Integer) call(client, "getKineticGeneration") == shown, "client KU/t differs");
        });
    }

    /**
     * A client-side menu that received every data slot of the server menu the way the sync packet
     * (ClientboundContainerSetDataPacket) carries it: slot id and the value as a short.
     */
    @SuppressWarnings("unchecked")
    private static net.minecraft.world.inventory.AbstractContainerMenu clientCopy(Object serverMenu, net.minecraft.world.entity.player.Inventory inventory) {
        try {
            var field = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("dataSlots");
            field.setAccessible(true);
            var slots = (java.util.List<net.minecraft.world.inventory.DataSlot>) field.get(serverMenu);
            var client = (net.minecraft.world.inventory.AbstractContainerMenu) serverMenu.getClass()
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class).newInstance(1, inventory);
            for (int i = 0; i < slots.size(); i++) client.setData(i, (short) slots.get(i).get());
            return client;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
