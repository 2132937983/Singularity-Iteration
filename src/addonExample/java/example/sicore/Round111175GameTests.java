package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.175: the canner's screen is the first merged one: its slots sit in the machine's drawing above the readouts. Its four
 * machine slots stay inside the drawing's area (0.1.11.180, 176 wide: x 8..168, y 17..95), apart from one another and off the
 * two gauge tanks (input x 32..48, output x 150..166, both y 18..92).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111175GameTests {
    private Round111175GameTests() {}

    @GameTest(template = "empty", batch = "round111175_gui")
    public static void cannerSlotsSitInItsDrawing(GameTestHelper h) throws ReflectiveOperationException {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = (AbstractContainerMenu) Round33GameTests.common("menu.producer.CannerElcMenu")
            .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class).newInstance(1, player.getInventory());
        List<int[]> frames = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Slot s = menu.getSlot(i);
            int[] f = {s.x - 1, s.y - 1, s.x + 17, s.y + 17};   // the 18 x 18 frame
            h.assertTrue(f[0] >= 8 && f[2] <= 168 && f[1] >= 17 && f[3] <= 95, "canner slot " + i + " leaves the drawing: " + s.x + "," + s.y);
            for (int[] tank : new int[][]{{32, 18, 48, 92}, {150, 18, 166, 92}})
                h.assertTrue(!overlap(f, tank), "canner slot " + i + " lies on a tank: " + s.x + "," + s.y);
            for (int[] other : frames) h.assertTrue(!overlap(f, other), "canner slot " + i + " overlaps another slot");
            frames.add(f);
        }
        h.succeed();
    }

    /**
     * 0.1.11.178 (was the 0.1.11.176 template's test): the other thirteen processing machines have their own drawings, one
     * layout of MergedSlots each (the input where the machine takes it, the outputs in the tray or rack at the outlet). Their
     * machine slots stay in the drawing's area (0.1.11.180, 176 wide: x 8..168), off the icon buttons at the upper right of the
     * machines that have them (x 152..166, y 18..45), apart from one another; the first slot is the layout's input; the
     * battery is at the lower left under its bolt.
     */
    @GameTest(template = "empty", batch = "round111175_gui")
    public static void machineSlotsSitInTheirOwnDrawings(GameTestHelper h) throws ReflectiveOperationException {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        String[][] menus = {{"BlockCutterMenu", "Cutter"}, {"CentrifugeElcMenu", "Centrifuge"}, {"CompressorAdvancedElcMenu", "Press"},
            {"CompressorElcMenu", "Press"}, {"ExtractorElcMenu", "Screw"}, {"FurnaceElcMenu", "Oven"}, {"InductionElcMenu", "Induction"},
            {"MetalFormerMenu", "Former"}, {"MetalFormerAdvancedMenu", "Former"}, {"PowderAdvancedElcMenu", "Cyclone"},
            {"PowderElcMenu", "Mill"}, {"RecyclerElcMenu", "Shredder"}, {"WasherElcMenu", "Washer"}};
        int[] buttons = {152, 18, 166, 45};
        java.util.Set<String> withButtons = java.util.Set.of("FurnaceElcMenu", "InductionElcMenu", "MetalFormerMenu", "MetalFormerAdvancedMenu",
            "PowderAdvancedElcMenu");
        for (String[] m : menus) {
            String name = m[0];
            AbstractContainerMenu menu = (AbstractContainerMenu) Round33GameTests.common("menu.producer." + name)
                .getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class).newInstance(1, player.getInventory());
            Class<?> layout = Round33GameTests.common("menu.MergedSlots$" + m[1]);
            int inX = (m[1].equals("Induction") ? layout.getField("IN_A_X") : layout.getField("IN_X")).getInt(null);
            int inY = layout.getField("IN_Y").getInt(null);
            Slot first = menu.getSlot(0);
            h.assertTrue(first.x == inX && first.y == inY, name + " input at " + first.x + "," + first.y + ", its drawing has it at " + inX + "," + inY);
            List<int[]> frames = new ArrayList<>();
            boolean battery = false;
            for (Slot s : menu.slots) {
                if (s.container instanceof net.minecraft.world.entity.player.Inventory || s.x < 0 || s.x > 248) continue;   // the player's, or docked
                int[] f = {s.x - 1, s.y - 1, s.x + 17, s.y + 17};
                h.assertTrue(f[0] >= 8 && f[2] <= 168 && f[1] >= 17 && f[3] <= 95, name + " slot " + s.index + " leaves the drawing: " + s.x + "," + s.y);
                h.assertTrue(!withButtons.contains(name) || !overlap(f, buttons), name + " slot " + s.index + " lies under the icon buttons: " + s.x + "," + s.y);
                for (int[] other : frames) h.assertTrue(!overlap(f, other), name + " slot " + s.index + " overlaps another slot");
                frames.add(f);
                battery |= s.x == 11 && s.y == 74;
            }
            h.assertTrue(battery, name + " has no battery slot at the lower left");
            h.assertTrue(frames.size() >= 3, name + " shows only " + frames.size() + " machine slots");
        }
        h.succeed();
    }

    private static boolean overlap(int[] a, int[] b) {
        return a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3];
    }
}
