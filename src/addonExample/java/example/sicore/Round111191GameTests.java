package example.sicore;

import static example.sicore.Round33GameTests.call;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.191: the TESU output port's output is set in EU/t (its screen, after the Creative Generator's): any value up to one MAX
 * packet, the voltage the lowest that holds it, the buffer two packets of it; the screen's keys set a voltage's full packet,
 * x10, /10 and the switch; a port saved before (a voltage only) keeps its voltage's full packet. Classes by name.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111191GameTests {
    private Round111191GameTests() {}

    private static long capacity(BlockEntity be) { return (Long) call(call(be, "getEnergyStorageInternal"), "getCapacity"); }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void outputPortTakesAnyOutput(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_tesu_output")));
        BlockEntity port = h.getBlockEntity(at);
        List<String> wrong = new ArrayList<>();
        // output -> voltage index (IV 8,192, LuV 32,768, ZPM 131,072, ...), buffer two packets of the output
        long[][] cases = {{5_000, 0}, {8_192, 0}, {10_000, 1}, {131_072, 2}, {131_073, 3}, {2_147_483_647L, 9}, {0, 0}};
        for (long[] c : cases) {
            call(port, "configure", c[0], true);
            long rate = (Long) call(port, "rate");
            int tier = (Integer) call(port, "tierIndex");
            if (rate != c[0] || tier != c[1] || capacity(port) != Math.max(1, c[0]) * 2)
                wrong.add(c[0] + " EU/t gave " + rate + " at voltage " + tier + ", buffer " + capacity(port));
        }
        call(port, "configure", 5_000_000_000L, true);
        if ((Long) call(port, "rate") != 2_147_483_647L) wrong.add("over MAX gave " + call(port, "rate"));
        h.assertTrue(wrong.isEmpty(), "outputs: " + wrong);

        // the screen's keys (server side): a voltage's full packet, x10, /10, the switch
        ServerPlayer player = ArmoryServiceGameTests.player(h);
        try {
            AbstractContainerMenu menu = (AbstractContainerMenu) call(port, "createMenu", 1, player.getInventory(), player);
            menu.clickMenuButton(player, 1);   // LuV
            h.assertTrue((Long) call(port, "rate") == 32_768L && (Long) call(menu, "rate") == 32_768L, "the LuV key gave " + call(port, "rate"));
            menu.clickMenuButton(player, 11);  // /10
            h.assertTrue((Long) call(port, "rate") == 3_276L && (Integer) call(port, "tierIndex") == 0, "/10 gave " + call(port, "rate"));
            menu.clickMenuButton(player, 10);  // x10
            h.assertTrue((Long) call(port, "rate") == 32_760L && (Integer) call(port, "tierIndex") == 1, "x10 gave " + call(port, "rate"));
            menu.clickMenuButton(player, 12);  // switch
            h.assertTrue(!(Boolean) call(port, "enabled") && !(Boolean) call(menu, "enabled"), "the switch did not turn it off");
            call(menu, "applyRate", 777L);
            h.assertTrue((Long) call(port, "rate") == 777L && !(Boolean) call(port, "enabled"), "a typed output changed the switch or did not apply");
            h.assertTrue((Long) call(Round33GameTests.common("menu.wiring.TesuOutputMenu"), "parseRate", "2.5M") == 2_500_000L
                && (Long) call(Round33GameTests.common("menu.wiring.TesuOutputMenu"), "parseRate", "3G") == -1L, "the field reads numbers wrong");
        } finally {
            player.discard();
        }

        // a port saved before 0.1.11.191 (a voltage, no output) sends its voltage's full packet, switched on
        CompoundTag old = new CompoundTag();
        old.putString("TesuTier", "uv");
        call(port, "loadAdditional", old, h.getLevel().registryAccess());
        h.assertTrue((Long) call(port, "rate") == 524_288L && (Integer) call(port, "tierIndex") == 3 && (Boolean) call(port, "enabled"),
            "an old UV port has " + call(port, "rate") + " at voltage " + call(port, "tierIndex"));
        h.succeed();
    }
}
