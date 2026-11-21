package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 0.1.11.137 (fusion overhaul F5): disruption mitigation by shattered pellet injection (frozen neon pellets; coolant cells kept
 * for this version), disruption heat in proportion to the plasma's thermal energy (no first-wall loss from a weak plasma), the
 * meltdown as the magnets' stored energy and contamination instead of a nuclear blast (the old blast behind a config, off), and
 * the new item, recipe and texts. (The meltdown itself is not run here: its contaminated zone would outlive the test.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111137GameTests {
    private Round111137GameTests() {}

    private static final String CONTROLLER = "blockentity.fusion.FusionControllerBlockEntity";
    private static final double REFERENCE_J = 5.984e8;

    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static double num(String field) {
        try {
            var f = cls(CONTROLLER).getDeclaredField(field);
            f.setAccessible(true);
            return ((Number) f.get(null)).doubleValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(field, e);
        }
    }

    private static BlockEntity controller(GameTestHelper h, BlockPos at) {
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "fusion/block_fusion_controller"))
            .defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        return h.getBlockEntity(at);
    }

    @GameTest(template = "empty", batch = "round111137_mitigation", timeoutTicks = 20)
    public static void neonPelletMitigatesInProportion(GameTestHelper h) throws Exception {
        List<String> wrong = new ArrayList<>();
        Item pellet = item("normal/item_frozen_neon_pellet");
        if (pellet == Items.AIR) wrong.add("no frozen neon pellet");
        double share = ((Number) cls(CONTROLLER).getMethod("energyShare", double.class).invoke(null, REFERENCE_J)).doubleValue();
        if (Math.abs(share - 1) > 0.01) wrong.add("reference energy share " + share);
        BlockEntity c = controller(h, new BlockPos(2, 2, 2));
        var items = (IItemHandlerModifiable) call(c, "getItems");
        int slot = (int) num("SLOT_MITIGATION");
        ItemStack vent = new ItemStack(item("reactor/item_reactor_iridium_oc_vent"));
        // a neon pellet at the reference burn: the components stay, one pellet is used, 5% of the maximum heat
        items.setStackInSlot(0, vent.copy());
        items.setStackInSlot(slot, new ItemStack(pellet, 3));
        long before = ((Number) call(c, "hull")).longValue(), max = ((Number) call(c, "maxHull")).longValue();
        call(c, "disrupt", true, REFERENCE_J);
        long heat = ((Number) call(c, "hull")).longValue() - before;
        if (items.getStackInSlot(0).isEmpty()) wrong.add("the pellet did not spare the first column");
        if (items.getStackInSlot(slot).getCount() != 2) wrong.add("not one pellet used: " + items.getStackInSlot(slot));
        if (Math.abs(heat - 0.05 * max) > 2) wrong.add("pellet heat " + heat + " of " + max);
        // unmitigated but weak (10% of the reference energy): the first column survives, 5% of the maximum heat
        items.setStackInSlot(slot, ItemStack.EMPTY);
        before = ((Number) call(c, "hull")).longValue();
        call(c, "disrupt", true, 0.1 * REFERENCE_J);
        heat = ((Number) call(c, "hull")).longValue() - before;
        if (items.getStackInSlot(0).isEmpty()) wrong.add("a weak plasma destroyed the first column");
        if (Math.abs(heat - 0.05 * max) > 2) wrong.add("weak unmitigated heat " + heat);
        // the ports and the controller take the pellet
        if (!(Boolean) cls(CONTROLLER).getMethod("isCoolant", ItemStack.class).invoke(null, new ItemStack(pellet))) wrong.add("the mitigation refuses the pellet");
        h.assertTrue(wrong.isEmpty(), "mitigation: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "round111137_mitigation", timeoutTicks = 20)
    public static void meltdownIsNotNuclearByDefault(GameTestHelper h) throws Exception {
        List<String> wrong = new ArrayList<>();
        Class<?> config = cls("Singularity_Iteration_Config");
        Object value = config.getField("FUSION_NUCLEAR_MELTDOWN").get(null);
        if ((Boolean) value.getClass().getMethod("getDefault").invoke(value)) wrong.add("the nuclear meltdown is on by default");
        if (num("MAGNET_BLAST_POWER") != 16 || num("CONTAMINATION_RADIUS") != 48) wrong.add("blast " + num("MAGNET_BLAST_POWER") + " zone " + num("CONTAMINATION_RADIUS"));
        String recipe = Round33GameTests.resource("/data/mio_icif/recipe/compressor/air_cell_to_frozen_neon_pellet.json");
        if (recipe == null || !recipe.contains("mio_icif:air") || !recipe.contains("item_frozen_neon_pellet")) wrong.add("no compressor recipe");
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            if (json == null || !json.contains("\"item.mio_icif.normal.item_frozen_neon_pellet\"") || !json.contains("\"message.mio_icif.fusion.meltdown_magnet\""))
                wrong.add(lang + " lacks the pellet or the magnet meltdown");
        }
        for (String p : new String[]{"", "_zh_cn/", "_ja_jp/"}) {
            String md = Round33GameTests.resource("/assets/mio_icif/guides/mio_icif/manual/" + p + "machines/fusion_block_fusion_controller.md");
            if (md == null || !md.contains("fusionNuclearMeltdown") || !md.contains("51 GJ")) wrong.add(p + " manual lacks the new meltdown");
        }
        h.assertTrue(wrong.isEmpty(), "meltdown: " + wrong);
        h.succeed();
    }
}
