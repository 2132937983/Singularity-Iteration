package example.sicore;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.138 (fusion overhaul F6): the fusion console's vanilla-frame screen, 344 x 252: the blanket grid at (8, 18), the fuel
 * and mitigation slots at y 131, the player inventory at (91, 170) and the hotbar at y 228; the readout and figure texts in
 * every language. (Classes by name; the screen itself is client-only.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111138GameTests {
    private Round111138GameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fusionConsoleLayout(GameTestHelper h) throws Exception {
        Class<?> menuClass = Round33GameTests.common("menu.fusion.FusionControllerMenu");
        h.assertTrue(menuClass.getField("GUI_WIDTH").getInt(null) == 344 && menuClass.getField("GUI_HEIGHT").getInt(null) == 252, "console size");
        Inventory inv = h.makeMockPlayer(GameType.SURVIVAL).getInventory();
        AbstractContainerMenu menu = (AbstractContainerMenu) menuClass.getConstructor(int.class, Inventory.class).newInstance(0, inv);
        h.assertTrue(menu.slots.size() == 54 + 5 + 36, "slots " + menu.slots.size());
        h.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 18, "blanket at " + menu.slots.get(0).x + ", " + menu.slots.get(0).y);
        h.assertTrue(menu.slots.get(54).y == 131 && menu.slots.get(58).x == 116, "fuel row at " + menu.slots.get(54).y);
        h.assertTrue(menu.slots.get(59).x == 91 && menu.slots.get(59).y == 170, "inventory at " + menu.slots.get(59).x + ", " + menu.slots.get(59).y);
        h.assertTrue(menu.slots.get(86).y == 228, "hotbar at " + menu.slots.get(86).y);
        // nothing may reach past the frame
        for (var slot : menu.slots) h.assertTrue(slot.x + 16 <= 344 - 7 && slot.y + 16 <= 252 - 7, "slot outside the frame at " + slot.x + ", " + slot.y);
        String[] keys = {"mode.h", "mode.l", "line_temp", "line_temp_none", "line_power", "line_fuel", "tip.heat", "tip.input"};
        String[] parts = {"solenoid", "plasma", "blanket", "vessel", "coil", "poloidal", "divertor", "nbi", "ecrh", "cryostat"};
        for (String lang : new String[]{"en_us", "zh_cn", "ja_jp", "ru_ru"}) {
            String json = Round33GameTests.resource("/assets/mio_icif/lang/" + lang + ".json");
            for (String k : keys) h.assertTrue(json != null && json.contains("\"gui.mio_icif.fusion." + k + "\""), lang + " lacks " + k);
            for (String p : parts)
                h.assertTrue(json.contains("\"gui.mio_icif.fusion.tip." + p + "\"") && json.contains("\"gui.mio_icif.fusion.tip." + p + ".title\""), lang + " lacks tip " + p);
        }
        h.succeed();
    }
}
