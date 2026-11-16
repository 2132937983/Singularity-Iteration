package example.sicore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GameTests of 0.1.11.4: the item buffer's upgrades act on all slots, radiation protection of the quantum and nano suits,
 * and the cable materials (tin, iron, copper, gold). Content classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1154GameTests {
    private Round1154GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration." + "common.";

    private static ItemStack si(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)));
    }

    private static Class<?> type(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name, e);
        }
    }

    /** An ejector in the second upgrade slot now empties the left half too (what a pulling upgrade fills). */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void bufferEjectsWhatThePullingSideFilled(GameTestHelper h) {
        BlockPos buffer = new BlockPos(1, 1, 1), chest = new BlockPos(2, 1, 1);
        h.setBlock(buffer, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_item_buffer_elc")));
        h.setBlock(chest, Blocks.CHEST);
        BlockEntity be = h.getBlockEntity(buffer);
        ItemStackHandler slots = (ItemStackHandler) ArmoryServiceGameTests.call(be, "getItemHandler");
        slots.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));   // left half, where slot 1's pulling upgrade puts items
        slots.setStackInSlot(49, si("upgrade/ejector_upgrade"));        // the right-hand (second) upgrade slot
        h.runAfterDelay(30, () -> {
            IItemHandler target = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(chest), null);
            int moved = 0;
            for (int i = 0; i < target.getSlots(); i++) if (target.getStackInSlot(i).is(Items.COBBLESTONE)) moved += target.getStackInSlot(i).getCount();
            h.assertTrue(moved > 0, "The second slot's ejector empties the left half (0.1.11.3: never), chest has " + moved);
            h.succeed();
        });
    }

    /** Issue #31: a full quantum suit is radiation protected everywhere; a nano suit only with a working toxin filter. */
    @GameTest(template = "empty")
    public static void suitsCountAsRadiationProtection(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        Class<?> util = type(COMMON + "util.RadiationProtectionUtil");
        h.assertTrue(!(boolean) ArmoryServiceGameTests.call(util, "isRadiationProtected", p), "No suit, no protection");
        p.setItemSlot(EquipmentSlot.HEAD, si("armor/item_armor_quantum_helmet"));
        p.setItemSlot(EquipmentSlot.CHEST, si("armor/item_armor_quantum_chestplate"));
        p.setItemSlot(EquipmentSlot.LEGS, si("armor/item_armor_quantum_leggings"));
        p.setItemSlot(EquipmentSlot.FEET, si("armor/item_armor_quantum_boots"));
        h.assertTrue((boolean) ArmoryServiceGameTests.call(util, "isRadiationProtected", p), "A full quantum suit protects");
        p.setItemSlot(EquipmentSlot.HEAD, si("armor/item_armor_nano_helmet"));
        p.setItemSlot(EquipmentSlot.CHEST, si("armor/item_armor_nano_chestplate"));
        p.setItemSlot(EquipmentSlot.LEGS, si("armor/item_armor_nano_leggings"));
        p.setItemSlot(EquipmentSlot.FEET, si("armor/item_armor_nano_boots"));
        h.assertTrue(!(boolean) ArmoryServiceGameTests.call(util, "isWearingFilteredNanoSet", p), "A nano suit without the filter unit does not");
        p.discard();
        h.succeed();
    }

    /** The cables are made of tin, iron, copper and gold (low to extra-high voltage). */
    @GameTest(template = "empty")
    public static void cablesAreTinIronCopperGold(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        String[][] expected = {
            {"metal_former/extruding/tin_ingot_to_cable", "wiring/cable/block_tin_cable"},
            {"metal_former/extruding/iron_ingot_to_cable", "wiring/cable/block_cable_o"},
            {"metal_former/extruding/copper_ingot_to_cable", "wiring/cable/block_gold_cable"},
            {"metal_former/extruding/gold_ingot_to_cable", "wiring/cable/block_iron_cable"},
            {"shapeless_178_uninsulated_copper_cable", "wiring/cable/block_cable_o"},
            {"shapeless_181_gold_cable", "wiring/cable/block_gold_cable"},
        };
        for (String[] e : expected) {
            var recipe = recipes.byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", e[0])).orElseThrow(() -> new AssertionError("recipe " + e[0]));
            ItemStack out = recipe.value().getResultItem(h.getLevel().registryAccess());
            h.assertTrue(out.is(si(e[1]).getItem()), e[0] + " makes " + out + ", expected " + e[1]);
        }
        var iron = recipes.byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "shapeless_178_uninsulated_copper_cable")).orElseThrow();
        boolean takesIron = iron.value().getIngredients().stream().anyMatch(i -> i.test(si("resource/item_plate_iron")) || java.util.Arrays.stream(i.getItems())
            .anyMatch(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("iron")));
        h.assertTrue(takesIron, "The hand-cut medium-voltage cable takes an iron plate");
        h.succeed();
    }
}
