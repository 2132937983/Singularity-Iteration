package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.prefab.item.AbstractBattery;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import com.miophas.singularity_iteration.core.prefab.item.BatteryElectricAdapter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.194: tool overhaul T1, the shared tool framework (one debit path, tool base for every tool,
 * charge rates, tooltip stats line, IC2-style adapter flags, per-stack capacity).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111194GameTests {
    private Round111194GameTests() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id));
    }

    /** I-10: the geomagnetic detector and the wireless manager were batteries with a hand-copied tool behaviour. */
    @GameTest(template = "empty")
    public static void everyElectricToolIsOnTheToolBase(GameTestHelper h) {
        h.assertTrue(item("item_geomagnetic_detector") instanceof AbstractElectricTool, "The geomagnetic detector is a tool");
        h.assertTrue(item("item_electric_wireless_manager") instanceof AbstractElectricTool, "The wireless manager is a tool");
        h.succeed();
    }

    /** I-03: six tools used their capacity as the charge rate, so any source filled them in one tick. */
    @GameTest(template = "empty")
    public static void toolsChargeAtTheirTierRate(GameTestHelper h) {
        String[] tierOne = {"item_tool_od_scanner", "item_tool_ov_scanner", "item_tool_power_unit", "item_tool_power_unit_small",
            "item_tool_treetap_elc", "item_tool_wrench_elc"};
        for (String id : tierOne) {
            AbstractBattery battery = (AbstractBattery) item(id);
            h.assertTrue(battery.getChargeRate() == AbstractElectricTool.standardChargeRate(1), id + " charges at " + battery.getChargeRate());
        }
        AbstractBattery bow = (AbstractBattery) item("item_tool_nano_bow");
        h.assertTrue(bow.getChargeRate() == 512, "The nano bow charges at " + bow.getChargeRate());
        h.succeed();
    }

    /** T1: a tool shows its tier, the cost per use and the uses left, and the energy has grouped digits. */
    @GameTest(template = "empty")
    public static void toolTooltipShowsTheStats(GameTestHelper h) {
        Item drill = item("item_tool_iridium_driller");
        ItemStack stack = new ItemStack(drill);
        ((IBatteryItem) drill).setEnergy(stack, 1_000_000L);
        List<Component> lines = new ArrayList<>();
        drill.appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        Object[] stats = null, energy = null;
        for (Component line : lines) {
            if (line.getContents() instanceof TranslatableContents t) {
                if (t.getKey().equals("tooltip.mio_icif.tool.stats")) stats = t.getArgs();
                if (t.getKey().equals("tooltip.mio_icif.energy")) energy = t.getArgs();
            }
        }
        h.assertTrue(stats != null, "The stats line is shown: " + lines);
        h.assertTrue("1,250".equals(String.valueOf(stats[2])), "1,000,000 EU at 800 EU a use leaves 1,250 uses, shown " + stats[2]);
        h.assertTrue(energy != null && "1,000,000".equals(String.valueOf(energy[0])), "Energy has grouped digits");
        h.succeed();
    }

    /** I-05: the adapter rate-limited only a simulated charge, and a real external drain ran as a simulation. */
    @GameTest(template = "empty")
    public static void electricItemAdapterReadsItsFlagsOneWay(GameTestHelper h) {
        Item drill = item("item_tool_iron_driller");
        var adapter = new BatteryElectricAdapter.ElectricItemWrapper((IBatteryItem) drill);
        ItemStack stack = new ItemStack(drill);
        long rate = ((AbstractBattery) drill).getChargeRate();
        long charged = adapter.charge(stack, 10_000L, 1, false, false);
        h.assertTrue(charged == rate, "A real charge within the transfer limit moves " + rate + ", moved " + charged);
        h.assertTrue(adapter.getCharge(stack) == rate, "and the stack holds it");
        long simulated = adapter.charge(stack, 10_000L, 1, true, true);
        h.assertTrue(simulated > rate && adapter.getCharge(stack) == rate, "A simulation without the limit changes nothing");
        h.assertTrue(adapter.discharge(stack, 50L, 1, true, true, false) == 0, "A tool gives nothing to an external drain");
        h.assertTrue(adapter.discharge(stack, 50L, 1, true, false, true) == 50 && adapter.getCharge(stack) == rate,
            "An internal simulation takes nothing");
        h.assertTrue(adapter.discharge(stack, 50L, 1, true, false, false) == 50 && adapter.getCharge(stack) == rate - 50,
            "An internal discharge takes the energy");
        h.assertTrue(adapter.getTier(stack) == 1 && new BatteryElectricAdapter.ElectricItemWrapper((IBatteryItem) item("item_tool_iridium_driller"))
            .getTier(stack) == 3, "The adapter reports the tool's tier");
        h.succeed();
    }
}
