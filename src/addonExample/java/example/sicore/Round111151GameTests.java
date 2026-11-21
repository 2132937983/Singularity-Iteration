package example.sicore;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.151: slot silhouettes (as vanilla's armour slots show armour): every silhouette is a 16 x 16 GUI sprite, every item the
 * table names exists, cells / batteries / rotors / cutting blades share theirs, the typed slots name an item that has one, and a
 * machine menu tells a slot's type (the condenser's battery slot). (The mod's common classes by reflection: addon examples do
 * not compile against them.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111151GameTests {
    private Round111151GameTests() {}

    private static final String SILHOUETTES = "client.screen.SlotSilhouettes";

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }
    private static Class<?> cls(String name) { return Round33GameTests.common(name); }
    private static ItemStack stack(String id) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))); }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void silhouettesAreSpritesOfRealItems(GameTestHelper h) throws java.io.IOException {
        List<String> wrong = new ArrayList<>();
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        List<?> names;
        try {
            names = (List<?>) cls(SILHOUETTES).getField("NAMES").get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        for (Object name : names) {
            var path = file.findResource("assets/mio_icif/textures/gui/sprites/slot/" + name + ".png");
            if (!Files.exists(path)) { wrong.add("no sprite for " + name); continue; }
            try (var in = Files.newInputStream(path)) {
                var image = javax.imageio.ImageIO.read(in);
                if (image == null || image.getWidth() != 16 || image.getHeight() != 16) wrong.add(name + " is not 16 x 16");
            }
        }
        for (Object id : (java.util.Set<?>) call(cls(SILHOUETTES), "itemIds"))
            if (BuiltInRegistries.ITEM.get(ResourceLocation.parse((String) id)) == Items.AIR) wrong.add("no item " + id);
        // shared silhouettes
        String[][] shared = {{"mio_icif:cell/item_cell_water", "cell"}, {"mio_icif:cell/item_cell_empty", "cell"},
            {"mio_icif:normal/item_chargebat_lev0", "battery"}, {"mio_icif:rotors/item_bronze_rotor", "rotor"},
            {"mio_icif:resource/item_diamond_cut_blade", "cut_blade"}, {"mio_icif:reactor/item_reactor_collant_simple", "coolant_cell"}};
        for (String[] s : shared) {
            Object name = call(cls(SILHOUETTES), "name", stack(s[0]));
            if (!s[1].equals(name)) wrong.add(s[0] + " -> " + name + ", expected " + s[1]);
        }
        if (call(cls(SILHOUETTES), "name", new ItemStack(Items.BREAD)) != null) wrong.add("a vanilla item has a silhouette");
        // every typed slot that names an item names one with a silhouette
        Class<?> slotType;
        try {
            slotType = Class.forName("com.miophas.singularity_iteration.core.prefab.inventory.SlotType");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
        int typed = 0;
        for (Object type : slotType.getEnumConstants()) {
            ItemStack item = (ItemStack) call(cls(SILHOUETTES), "forType", type);
            if (item.isEmpty()) continue;
            typed++;
            if (call(cls(SILHOUETTES), "name", item) == null) wrong.add(type + " names " + item + ", which has no silhouette");
        }
        if (typed < 10) wrong.add("only " + typed + " slot types name an item");
        h.assertTrue(wrong.isEmpty(), "silhouettes: " + wrong);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void machineMenuTellsTheSlotType(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_condenser")));
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        AbstractContainerMenu menu = ((MenuProvider) h.getBlockEntity(at)).createMenu(0, player.getInventory(), player);
        h.assertTrue(menu != null, "the condenser has no menu");
        Object battery = call(menu, "machineSlotType", menu.getSlot(6));
        Object vent = call(menu, "machineSlotType", menu.getSlot(0));
        Object playerSlot = call(menu, "machineSlotType", menu.getSlot(menu.slots.size() - 1));
        h.assertTrue("BATTERY".equals(String.valueOf(battery)), "the condenser's battery slot is " + battery);
        h.assertTrue("EXTRA".equals(String.valueOf(vent)), "the condenser's vent slot is " + vent);
        h.assertTrue(playerSlot == null, "a player slot has the type " + playerSlot);
        h.succeed();
    }
}
