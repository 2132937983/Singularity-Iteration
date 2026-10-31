package example.sicore;

import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of the 0.1.8.0 combat, suit energy and data fixes. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round180GameTests {
    private static final String[] QUANTUM = {"armor/item_armor_quantum_helmet", "armor/item_armor_quantum_chestplate",
        "armor/item_armor_quantum_leggings", "armor/item_armor_quantum_boots"};
    private static final String[] NANO = {"armor/item_armor_nano_helmet", "armor/item_armor_nano_chestplate",
        "armor/item_armor_nano_leggings", "armor/item_armor_nano_boots"};

    /**
     * A full quantum set stops a whole magic hit (bypasses_armor) and a whole
     * physical hit. The four pieces pay together: no piece pays much more.
     */
    @GameTest(template = "empty", batch = "round180_suit")
    public static void quantumSetAbsorbsAllAndSharesEnergy(GameTestHelper h) {
        // a survival player: the mock server player is creative and has join protection, so it takes no damage
        ServerPlayer player = Round33GameTests.survivalPlayer(h);
        ItemStack[] pieces = equip(player, QUANTUM);
        long[] before = energies(pieces);
        player.hurt(h.getLevel().damageSources().magic(), 10.0F);
        h.assertTrue(player.getHealth() >= player.getMaxHealth() - 0.01F, "Quantum set let magic damage through: " + player.getHealth());
        player.invulnerableTime = 0;
        player.hurt(h.getLevel().damageSources().generic(), 14.0F);
        h.assertTrue(player.getHealth() >= player.getMaxHealth() - 0.01F, "Quantum set let a heavy physical hit through: " + player.getHealth());
        long[] after = energies(pieces);
        double minRatio = Double.MAX_VALUE, maxRatio = 0;
        for (int i = 0; i < 4; i++) {
            long spent = before[i] - after[i];
            h.assertTrue(spent > 0, "A quantum piece did not share the cost");
            // Each piece pays in proportion to its stored energy: equal fractions, within rounding.
            double ratio = (spent - 2.0) / before[i], upper = (spent + 2.0) / before[i];
            minRatio = Math.min(minRatio, upper); maxRatio = Math.max(maxRatio, ratio);
        }
        h.assertTrue(maxRatio <= minRatio, "The pool did not split the cost by stored energy");
        Round33GameTests.remove(player);
        h.succeed();
    }

    /** A full nano set absorbs 90 % of a hit, not 99.99 % (the old chained product) and not 40 %. */
    @GameTest(template = "empty", batch = "round180_suit")
    public static void nanoSetAbsorbsNinetyPercent(GameTestHelper h) {
        // a survival player: the mock server player is creative and has join protection, so it takes no damage
        ServerPlayer player = Round33GameTests.survivalPlayer(h);
        equip(player, NANO);
        float start = player.getHealth();
        player.hurt(h.getLevel().damageSources().magic(), 10.0F);
        float lost = start - player.getHealth();
        // 1.0 without Resistance; 0.8 when the nano chestplate already gave Resistance I.
        h.assertTrue(lost >= 0.7F && lost <= 1.05F, "Full nano set did not absorb 90 %: lost " + lost);
        Round33GameTests.remove(player);
        h.succeed();
    }

    /** With unequal charges, the fuller pieces pay more: each piece pays in proportion to its stored energy. */
    @GameTest(template = "empty", batch = "round180_suit")
    public static void poolPaysInProportionToStoredEnergy(GameTestHelper h) {
        // a survival player: the mock server player is creative and has join protection, so it takes no damage
        ServerPlayer player = Round33GameTests.survivalPlayer(h);
        ItemStack[] pieces = equip(player, QUANTUM);
        long[] stored = {1_000_000, 3_000_000, 2_000_000, 4_000_000};
        for (int i = 0; i < 4; i++) ((AbstractElectricArmor) pieces[i].getItem()).setEnergy(pieces[i], stored[i]);
        player.hurt(h.getLevel().damageSources().magic(), 10.0F);
        long total = 0;
        long[] spent = new long[4];
        for (int i = 0; i < 4; i++) { spent[i] = stored[i] - ((AbstractElectricArmor) pieces[i].getItem()).getEnergy(pieces[i]); total += spent[i]; }
        h.assertTrue(total > 0, "The suit paid nothing");
        for (int i = 0; i < 4; i++) {
            double expected = (double) total * stored[i] / 10_000_000;
            h.assertTrue(Math.abs(spent[i] - expected) <= 2, "Piece " + i + " paid " + spent[i] + ", expected " + expected);
        }
        Round33GameTests.remove(player);
        h.succeed();
    }

    /** The goggles hold the full charge of the advanced RE-battery of their recipe. */
    @GameTest(template = "empty", batch = "round180_data")
    public static void nightVisionGogglesHoldFourHundredThousand(GameTestHelper h) {
        var goggles = new ItemStack(item("armor/item_armor_nightvision_goggles"));
        long max = ((AbstractElectricArmor) goggles.getItem()).getMaxEnergy(goggles);
        h.assertTrue(max == 400_000L, "Night vision goggles capacity is " + max);
        h.succeed();
    }

    /** Four charcoal dusts make one carbon fibre, as four coal dusts do. */
    @GameTest(template = "empty", batch = "round180_data")
    public static void charcoalDustMakesCarbonFibre(GameTestHelper h) {
        var dust = new ItemStack(item("resource/item_charcoal_dust"));
        var input = CraftingInput.of(2, 2, List.of(dust.copy(), dust.copy(), dust.copy(), dust.copy()));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "No crafting recipe accepts four charcoal dusts");
        var result = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(item("resource/item_coal_fabre")), "Charcoal dust did not make carbon fibre: " + result);
        h.succeed();
    }

    /** Energy Saving supports every energy consumer that was missing in 0.1.7.x. */
    @GameTest(template = "empty", batch = "round180_data")
    public static void energySavingCoversAccessories(GameTestHelper h) {
        TagKey<Item> electric = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("mio_icif", "enchantable/electric"));
        h.assertTrue(new ItemStack(item("item_geomagnetic_detector")).is(electric), "Geomagnetic detector is not enchantable");
        for (String trinket : new String[]{"item_trinket_flight_ring", "item_trinket_life_support_ring", "item_trinket_fire_proof_necklace"}) {
            var id = ResourceLocation.fromNamespaceAndPath("mio_icif", trinket);
            if (!BuiltInRegistries.ITEM.containsKey(id)) continue; // Curios is not installed
            h.assertTrue(new ItemStack(BuiltInRegistries.ITEM.get(id)).is(electric), trinket + " is not enchantable");
        }
        h.succeed();
    }

    /**
     * The quantum suit ships baked Blockbench models: each piece has its geometry and texture, the
     * chestplate has hand cover parts, the boots face forward, the items draw the 3D model and the
     * armor layer uses the baked texture.
     */
    @GameTest(template = "empty", batch = "round180_data")
    public static void quantumSuitUsesBakedModels(GameTestHelper h) {
        List<String> problems = new java.util.ArrayList<>();
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        String[][] parts = {{"head"}, {"body", "right_arm", "left_arm", "right_hand", "left_hand"},
            {"body", "right_leg", "left_leg"}, {"right_leg", "left_leg"}};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            String json = Round33GameTests.resource("/assets/mio_icif/armor_models/quantum_" + pieces[i] + ".json");
            if (json == null) { problems.add("model missing: " + pieces[i]); continue; }
            var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("parts");
            for (String part : parts[i]) if (!root.has(part) || root.getAsJsonArray(part).size() < 6) problems.add(pieces[i] + " lacks " + part);
            // 0.1.10.11: the 3D piece everywhere, the inventory included (builtin/entity)
            String problem = Round183GameTests.bakedArmorItemModel("armor/item_armor_quantum_" + pieces[i]);
            if (problem != null) problems.add(pieces[i] + " " + problem);
            var stack = new ItemStack(item("armor/item_armor_quantum_" + pieces[i]));
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
                var layer = armor.getMaterial().value().layers().get(0);
                var texture = stack.getItem().getArmorTexture(stack, null, slots[i], layer, false);
                if (texture == null || !texture.getPath().equals("textures/models/armor/quantum_" + pieces[i] + ".png"))
                    problems.add(pieces[i] + " armor texture " + texture);
                if (!modResourceExists("assets/mio_icif/textures/models/armor/quantum_" + pieces[i] + ".png"))
                    problems.add("texture missing: " + pieces[i]);
            } else problems.add(pieces[i] + " is not armor");
        }
        String boots = Round33GameTests.resource("/assets/mio_icif/armor_models/quantum_boots.json");
        if (boots != null) {
            var root = com.google.gson.JsonParser.parseString(boots).getAsJsonObject().getAsJsonObject("parts");
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            for (String part : new String[]{"right_leg", "left_leg"}) for (var q : root.getAsJsonArray(part)) {
                var v = q.getAsJsonObject().getAsJsonArray("v");
                for (int k = 0; k < 4; k++) { double z = v.get(k * 5 + 2).getAsDouble(); minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z); }
            }
            if (-minZ <= maxZ) problems.add("quantum boots face backwards: " + minZ + " .. " + maxZ);
        }
        h.assertTrue(problems.isEmpty(), "quantum: " + problems);
        h.succeed();
    }

    /** True when the mio_icif mod file contains {@code path} (binary resources included). */
    static boolean modResourceExists(String path) {
        var file = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile();
        return java.nio.file.Files.exists(file.findResource(path));
    }

    private static ItemStack[] equip(ServerPlayer player, String[] ids) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        ItemStack[] pieces = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            var stack = new ItemStack(item(ids[i]));
            var armor = (AbstractElectricArmor) stack.getItem();
            armor.setEnergy(stack, armor.getMaxEnergy(stack));
            player.setItemSlot(slots[i], stack);
            pieces[i] = player.getItemBySlot(slots[i]);
        }
        return pieces;
    }

    private static long[] energies(ItemStack[] pieces) {
        long[] result = new long[pieces.length];
        for (int i = 0; i < pieces.length; i++) result[i] = ((AbstractElectricArmor) pieces[i].getItem()).getEnergy(pieces[i]);
        return result;
    }

    private static Item item(String path) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
}
