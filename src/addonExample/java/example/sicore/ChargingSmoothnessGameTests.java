package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Charge pads: charging a player's battery must be monotonic (never step back) and
 * lossless (what leaves the pad arrives in the item), tick after tick.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ChargingSmoothnessGameTests {

    @GameTest(template = "reactor_loop", batch = "charging", timeoutTicks = 120)
    public static void chargePadChargesSmoothlyWithoutLoss(GameTestHelper h) {
        BlockPos pad = new BlockPos(2, 2, 2);
        h.setBlock(pad, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/block_batbox_charger")));
        AbstractEnergyBlockEntity charger = h.getBlockEntity(pad);
        charger.getEnergyStorageInternal().setStored(30_000);
        var api = MioIcifAPI.instance().getItemAPI();
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(pad);
        player.moveTo(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
        ItemStack battery = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:normal/item_lithium_battery")));
        api.dischargeBattery(battery, Long.MAX_VALUE / 4, false);
        player.getInventory().setItem(9, battery);
        long padStart = charger.getEnergyStorageInternal().getAmount();
        long[] last = {api.getBatteryStored(player.getInventory().getItem(9))};
        long itemStart = last[0];
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            long stored = api.getBatteryStored(player.getInventory().getItem(9));
            h.assertTrue(stored >= last[0], "Battery charge stepped back at tick " + now + ": " + last[0] + " -> " + stored);
            last[0] = stored;
            if (now == 80) {
                long gained = stored - itemStart;
                long spent = padStart - charger.getEnergyStorageInternal().getAmount();
                h.assertTrue(gained > 0, "Charge pad did not charge the battery");
                h.assertTrue(gained == spent, "Charging lost energy: pad spent " + spent + " EU, battery gained " + gained + " EU");
                player.discard();
                h.succeed();
            }
        });
    }
}
