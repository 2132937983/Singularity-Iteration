package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of 0.1.11.203: the Living Nano Lining goes into the quantum modification station's module slot. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111203GameTests {
    private Round111203GameTests() {}

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
    }

    /** Player report: the lining would not go into the station (its menu slot left it out), so it never reached a quantum piece. */
    @GameTest(template = "empty")
    public static void stationMenuTakesTheLining(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_quantum_modification_station")));
        BlockEntity station = h.getBlockEntity(at);
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        AbstractContainerMenu menu;
        try {
            var type = Class.forName("com.miophas.singularity_iteration." + "common.suit.QuantumModStationMenu");
            var beType = Class.forName("com.miophas.singularity_iteration." + "common.suit.QuantumModStationBlockEntity");
            menu = (AbstractContainerMenu) type.getConstructor(int.class, net.minecraft.world.entity.player.Inventory.class, beType,
                net.minecraft.world.inventory.ContainerData.class).newInstance(1, p.getInventory(), station, null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("station menu", e);
        }
        ItemStack lining = stack("module/item_living_nano_lining");
        boolean placeable = false;
        for (Slot slot : menu.slots) {
            if (slot.container == p.getInventory()) continue;
            if (slot.mayPlace(lining)) placeable = true;
        }
        h.assertTrue(placeable, "a station slot takes the Living Nano Lining");
        for (String piece : new String[] {"armor/item_armor_quantum_helmet", "armor/item_armor_quantum_chestplate",
                "armor/item_armor_quantum_leggings", "armor/item_armor_quantum_boots"}) {
            ItemStack suit = stack(piece);
            Class<?> modules;
            try {
                modules = Class.forName("com.miophas.singularity_iteration." + "common.suit.SuitModules");
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
            h.assertTrue((boolean) ArmoryServiceGameTests.call(modules, "installLivingLining", suit), piece + " takes the lining");
            h.assertTrue((boolean) ArmoryServiceGameTests.call(modules, "hasLivingLining", suit), piece + " has it afterwards");
        }
        p.discard();
        h.succeed();
    }
}
