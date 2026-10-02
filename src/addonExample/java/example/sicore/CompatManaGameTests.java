package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Botania mana for the Armory: Mana Spreaders fill it through the ManaReceiver capability,
 * stored Botania gear is repaired with mana (no EU), and mana items are topped up.
 * Present only when Botania (or its API stand-in on the compat server) is loaded.
 */
@GameTestHolder(CoreExampleMod.ID)
public final class CompatManaGameTests {
    private CompatManaGameTests() {}

    private static final BlockPos ARMORY = new BlockPos(2, 2, 2);
    private static final int HEAD_0 = 1, MAIN_0 = 5;

    @GameTestGenerator
    public static Collection<TestFunction> manaTests() {
        List<TestFunction> tests = new ArrayList<>();
        if (!ModList.get().isLoaded("botania")) return tests;
        tests.add(new TestFunction("mana_receiver", "compatmanagametests.armoryreceivesmanabursts", CoreExampleMod.ID + ":empty",
            40, 0, true, CompatManaGameTests::armoryReceivesManaBursts));
        tests.add(new TestFunction("mana_repair", "compatmanagametests.botaniagearrepairswithmana", CoreExampleMod.ID + ":empty",
            120, 0, true, CompatManaGameTests::botaniaGearRepairsWithMana));
        return tests;
    }

    static Object armory(GameTestHelper h, long energy) {
        return ArmoryServiceGameTests.armory(h, energy);
    }

    static int mana(Object armory) {
        return (int) ArmoryServiceGameTests.call(armory, "mana");
    }

    static void armoryReceivesManaBursts(GameTestHelper h) {
        Object armory = armory(h, 0);
        var cap = CompatEnergyGameTests.capability("botania:mana_receiver");
        Object receiver = h.getLevel().getCapability(cap, h.absolutePos(ARMORY), Direction.UP);
        h.assertTrue(receiver != null, "Armory exposes Botania's ManaReceiver");
        h.assertTrue((boolean) CompatEnergyGameTests.call(receiver, "canReceiveManaFromBursts"), "Armory accepts mana bursts");
        CompatEnergyGameTests.call(receiver, "receiveMana", 12_000);
        h.assertTrue(mana(armory) == 12_000, "Mana tank holds the burst, got " + mana(armory));
        h.assertTrue((int) CompatEnergyGameTests.call(receiver, "getCurrentMana") == 12_000, "Receiver reports the tank");
        h.assertTrue(!(boolean) CompatEnergyGameTests.call(receiver, "isFull"), "Tank is not full");
        h.succeed();
    }

    static ItemStack linked(String id, String piece) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        @SuppressWarnings("unchecked")
        var type = (net.minecraft.core.component.DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE
            .get(ResourceLocation.fromNamespaceAndPath("mio_icif", "armory_link"));
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("piece", piece);
        // Built through the component's own codec: no compile-time or by-name link to SI content.
        stack.set(type, type.codecOrThrow().parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow());
        return stack;
    }

    static void botaniaGearRepairsWithMana(GameTestHelper h) {
        Object armory = armory(h, 0);   // no EU at all: mana alone must do the job
        var handler = ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity) armory).getItemHandler();
        ItemStack helmet = linked("botania:manasteel_helmet", "HEAD");
        helmet.setDamageValue(20);
        h.assertTrue(handler.insertItem(HEAD_0, helmet, false).isEmpty(), "Connected Botania helmet is stored");
        ItemStack tablet = linked("botania:mana_tablet", "MAINHAND");
        h.assertTrue(handler.insertItem(MAIN_0, tablet, false).isEmpty(), "Connected mana tablet is stored");
        ArmoryServiceGameTests.call(armory, "receiveMana", 100_000);
        h.runAfterDelay(80, () -> {
            ItemStack after = handler.getStackInSlot(HEAD_0);
            h.assertTrue(after.getDamageValue() == 0, "Botania helmet not repaired by mana: damage " + after.getDamageValue());
            @SuppressWarnings("unchecked")
            ItemCapability<Object, Void> manaItem = null;
            for (var c : ItemCapability.getAll()) if (c.name().toString().equals("botania:mana_item")) {
                @SuppressWarnings("unchecked") var cast = (ItemCapability<Object, Void>) c;
                manaItem = cast;
            }
            Object tabletView = handler.getStackInSlot(MAIN_0).getCapability(manaItem);
            int tabletMana = (int) CompatEnergyGameTests.call(tabletView, "getMana");
            h.assertTrue(tabletMana > 0, "Mana tablet was not topped up");
            int used = 100_000 - mana(armory);
            h.assertTrue(used >= 20 * 70 + tabletMana, "Mana accounting: used " + used + " for 20 points + " + tabletMana + " tablet");
            h.succeed();
        });
    }
}
