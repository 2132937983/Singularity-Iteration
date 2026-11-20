package example.sicore;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.108 (processing overhaul M1): the macerator's energy per operation follows the input's Bond work index at a
 * fixed 2 EU/t (iron ore and stone 600 EU, 300 t as before; clay 335 EU; cobblestone 680 EU; gravel 1,191 EU), a recipe's own
 * time overrides it, the buffer does not change with the input, and the material table reaches clients intact. (The
 * mod's common classes by reflection: addon examples do not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111108GameTests {
    private Round111108GameTests() {}

    private static Class<?> common(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    /** input, expected EU per operation, expected ticks */
    private static final Object[][] INPUTS = {
        {Items.IRON_ORE, 600L, 300}, {Items.CLAY, 335L, 168}, {Items.COBBLESTONE, 680L, 340}, {Items.STONE, 600L, 300},
        {Items.COAL, 539L, 270}, {Items.GRAVEL, 1191L, 596}};

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void maceratorTimesByMaterial(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_powder_elc")));
        BlockEntity be = h.getBlockEntity(at);
        IItemHandlerModifiable items = (IItemHandlerModifiable) call(be, "getItemHandler");
        long[] capacity = new long[INPUTS.length];
        step(h, be, items, capacity, 0);
    }

    private static void step(GameTestHelper h, BlockEntity be, IItemHandlerModifiable items, long[] capacity, int i) {
        if (i == INPUTS.length) {
            for (long c : capacity) h.assertTrue(c == capacity[0], "the buffer changed with the input: " + java.util.Arrays.toString(capacity));
            h.assertTrue(capacity[0] == 1800, "buffer " + capacity[0] + " (600 + the hardest operation, 1,200)");
            h.succeed();
            return;
        }
        Object[] row = INPUTS[i];
        items.setStackInSlot(0, new ItemStack((net.minecraft.world.item.Item) row[0], 4));
        h.runAfterDelay(3, () -> {
            Object cost = call(be, "processCost");
            String what = row[0] + ": " + cost + ", " + call(be, "getMaxProgress") + " t";
            h.assertTrue((long) call(cost, "energy") == (long) row[1] && (long) call(cost, "power") == 2, what);
            h.assertTrue((int) call(be, "getMaxProgress") == (int) row[2], what);
            h.assertTrue((long) call(be, "getEffectiveEnergyPerTick") == 2, what + " at " + call(be, "getEffectiveEnergyPerTick") + " EU/t");
            capacity[i] = (long) call(be, "getEffectiveCapacity");
            step(h, be, items, capacity, i + 1);
        });
    }

    /** A recipe with its own time keeps it (at the machine's 2 EU/t); without one, the material decides. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void recipeTimeOverrides(GameTestHelper h) {
        Class<?> recipe = common("recipe.mio_icif_PowderRecipe");
        Object timed, plain;
        try {
            var ctor = recipe.getConstructor(String.class, Ingredient.class, ItemStack.class, int.class, int.class, int.class);
            timed = ctor.newInstance("", Ingredient.of(Items.GRAVEL), new ItemStack(Items.SAND), 100, 10, 1);
            plain = ctor.newInstance("", Ingredient.of(Items.GRAVEL), new ItemStack(Items.SAND), -1, 10, 2);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        Class<?> c = common("processing.Comminution");
        Object a = call(c, "maceratorCost", new ItemStack(Items.GRAVEL), timed, 2L, 1.0);
        Object b = call(c, "maceratorCost", new ItemStack(Items.GRAVEL), plain, 2L, 1.0);
        h.assertTrue((int) call(a, "ticks") == 100 && (long) call(a, "energy") == 200, "recipe time 100: " + a);
        h.assertTrue((long) call(b, "energy") == 2381, "two gravel by work index: " + b);
        h.assertTrue((int) call(timed, "getProcessingTime") == 100 && (int) call(plain, "getProcessingTime") == 200,
            "the old getter keeps its default (200) for recipes without a time");
        h.succeed();
    }

    /** The material table survives the client sync: encoded and decoded, the entries are the same. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void materialTableSync(GameTestHelper h) {
        Class<?> m = common("processing.SiMaterials");
        List<?> entries = (List<?>) call(m, "entries");
        h.assertTrue(entries.size() >= 18, "entries " + entries.size());
        Class<?> sync = common("processing.SiMaterials$Sync");
        try {
            Object packet = sync.getConstructors()[0].newInstance(entries);
            @SuppressWarnings("unchecked")
            var codec = (net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Object>) sync.getField("CODEC").get(null);
            io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
            codec.encode(buf, packet);
            Object back = codec.decode(buf);
            h.assertTrue(packet.equals(back), "the sync changed the table");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        h.succeed();
    }
}
