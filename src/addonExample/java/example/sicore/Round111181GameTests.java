package example.sicore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.181: the generator screens (the plant mimic screens, the engine, solar, thermal and wind screens, the quantum
 * generator) and the molecular transformer, polymerizer and pump are the vanilla 176 wide and merged as the processing
 * machines: the machine is drawn on the panel (x 8..168, from y 15) with its slots in it. Every machine slot's 18 x 18 frame
 * stays inside its drawing and apart from the others (the docked upgrade slots aside), and the player inventory starts at
 * x 8. (Menus by name.)
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111181GameTests {
    private Round111181GameTests() {}

    /**
     * The menus: the machine slots each shows in its drawing, the drawing's lower edge (the frames stay above it), and the
     * inventory's top row (the hotbar 58 lower).
     */
    private static final Object[][] MENUS = {
        {"generator.AdvancedDropGeneratorMenu", 1, 78, 162}, {"generator.DropGeneratorMenu", 1, 78, 162}, {"generator.ExperienceGeneratorMenu", 0, 78, 162},
        {"generator.GeoGeneratorMenu", 3, 78, 162}, {"generator.GeomagneticGeneratorMenu", 1, 78, 162}, {"generator.KineticGeneratorMenu", 0, 78, 162},
        {"generator.TurboKineticGeneratorMenu", 0, 78, 162}, {"generator.RTGeneratorMenu", 6, 78, 162}, {"huentity.RTHeatGeneratorMenu", 6, 78, 162},
        {"generator.StirlingGeneratorMenu", 0, 78, 162}, {"generator.StirlingKineticGeneratorMenu", 4, 78, 162},
        {"generator.SteamKineticGeneratorMenu", 1, 78, 162}, {"generator.SteamTurbineMenu", 3, 78, 162}, {"generator.DieselGeneratorMenu", 3, 78, 162},
        {"generator.SemifluidEngineMenu", 3, 78, 162}, {"generator.WaterGeneratorMenu", 2, 78, 162}, {"generator.WaterKineticGeneratorMenu", 1, 78, 162},
        {"generator.QuantumGeneratorMenu", 0, 78, 162}, {"producer.SteamGeneratorMenu", 0, 78, 162}, {"producer.CondenserMenu", 7, 78, 162},
        {"producer.SteamRepressurizerMenu", 0, 78, 162},
        // the second group: the solar panels (one or four charge slots), the thermal generator (its drawing is 95 high), the
        // wind generators, the molecular transformer, the polymerizer and the pump
        {"generator.SolarGeneratorMenu", 1, 78, 162}, {"generator.AdvancedSolarPanelMenu", 4, 78, 162}, {"generator.ThermalGeneratorMenu", 2, 110, 162},
        {"generator.WindGeneratorMenu", 1, 78, 162}, {"generator.WindKineticGeneratorMenu", 1, 78, 162},
        {"producer.MolecularTransformerMenu", 2, 36, 122}, {"producer.NeutronPolymerizerMenu", 3, 60, 160}, {"producer.PumpElcMenu", 3, 112, 160}};

    @GameTest(template = "empty", batch = "round111181_gui")
    public static void generatorSlotsSitInTheirDrawings(GameTestHelper h) throws ReflectiveOperationException {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        List<String> wrong = new ArrayList<>();
        for (Object[] m : MENUS) {
            String name = (String) m[0];
            AbstractContainerMenu menu = (AbstractContainerMenu) Round33GameTests.common("menu." + name)
                .getConstructor(int.class, Inventory.class).newInstance(1, player.getInventory());
            List<int[]> frames = new ArrayList<>();
            boolean inventory = false, hotbar = false;
            for (Slot s : menu.slots) {
                if (s.container instanceof Inventory) {
                    inventory |= s.x == 8 && s.y == (int) m[3];
                    hotbar |= s.x == 8 && s.y == (int) m[3] + 58;
                    continue;
                }
                if (s.x < 0 || s.x > 176) continue;   // a docked upgrade slot
                int[] f = {s.x - 1, s.y - 1, s.x + 17, s.y + 17};
                if (f[0] < 8 || f[2] > 168 || f[1] < 15 || f[3] > (int) m[2]) wrong.add(name + " slot " + s.index + " leaves the drawing at " + s.x + "," + s.y);
                for (int[] o : frames)
                    if (f[0] < o[2] && o[0] < f[2] && f[1] < o[3] && o[1] < f[3]) wrong.add(name + " slot " + s.index + " overlaps another at " + s.x + "," + s.y);
                frames.add(f);
            }
            if (frames.size() != (int) m[1]) wrong.add(name + " shows " + frames.size() + " machine slots, expected " + m[1]);
            if (!inventory || !hotbar) wrong.add(name + " has its inventory elsewhere (expected x 8, y " + m[3] + ")");
        }
        h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
        h.succeed();
    }
}
