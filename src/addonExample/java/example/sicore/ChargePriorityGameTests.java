package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.ChargePriority;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Energy allocation: charge pads skip items set to OFF and still charge the others. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class ChargePriorityGameTests {
    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    @GameTest(template = "reactor_loop", batch = "charge_priority", timeoutTicks = 100)
    public static void chargePadSkipsItemsWithAllocationOff(GameTestHelper h) {
        BlockPos pad = new BlockPos(2, 1, 2);
        h.setBlock(pad, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wiring/block_batbox_charger")));
        AbstractEnergyBlockEntity storage = h.getBlockEntity(pad);
        storage.getEnergyStorageInternal().setEnergy(storage.getEnergyStorageInternal().getCapacity());

        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(pad.above())));
        ItemStack helmet = item("armor/item_armor_nano_helmet");
        ItemStack boots = item("armor/item_armor_nano_boots");
        ChargePriority.set(helmet, ChargePriority.OFF);
        ChargePriority.set(boots, ChargePriority.HIGH);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        h.assertTrue(ChargePriority.of(player.getItemBySlot(EquipmentSlot.HEAD)) == ChargePriority.OFF, "Priority must persist on the stack");

        var api = MioIcifAPI.instance().getItemAPI();
        h.succeedWhen(() -> {
            long head = api.getElectricArmorStored(player.getItemBySlot(EquipmentSlot.HEAD));
            long feet = api.getElectricArmorStored(player.getItemBySlot(EquipmentSlot.FEET));
            h.assertTrue(feet > 0, "High-priority boots must charge");
            h.assertTrue(head == 0, "Helmet with allocation OFF must not charge, got " + head);
        });
    }
}
