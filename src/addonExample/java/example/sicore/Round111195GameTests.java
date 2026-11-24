package example.sicore;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.11.195: tool overhaul T2, tags and enchantments (decision D2: vanilla enchantments by category, Energy
 * Saving kept, never Unbreaking or Mending on electric tools; bronze tools are vanilla tools).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111195GameTests {
    private Round111195GameTests() {}

    private static final String COMMON = "com.miophas.singularity_iteration." + "common.";

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", id)));
    }

    private static boolean takes(GameTestHelper h, ItemStack stack, ResourceKey<Enchantment> key) {
        Holder<Enchantment> e = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
        return e.value().canEnchant(stack);
    }

    private static boolean takesEnergySaving(GameTestHelper h, ItemStack stack) {
        return takes(h, stack, ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_saving")));
    }

    /** M-03: bronze tools were in no tag, so they took no enchantment at all and other mods did not see them as tools. */
    @GameTest(template = "empty")
    public static void bronzeToolsAreVanillaTools(GameTestHelper h) {
        h.assertTrue(stack("item_tool_bronze_pickaxe").is(ItemTags.PICKAXES), "bronze pickaxe is a pickaxe");
        h.assertTrue(stack("item_tool_bronze_axe").is(ItemTags.AXES), "bronze axe is an axe");
        h.assertTrue(stack("item_tool_bronze_shovel").is(ItemTags.SHOVELS), "bronze shovel is a shovel");
        h.assertTrue(stack("item_tool_bronze_hoe").is(ItemTags.HOES), "bronze hoe is a hoe");
        h.assertTrue(stack("item_tool_bronze_sword").is(ItemTags.SWORDS), "bronze sword is a sword");
        ItemStack pick = stack("item_tool_bronze_pickaxe");
        h.assertTrue(takes(h, pick, Enchantments.EFFICIENCY) && takes(h, pick, Enchantments.UNBREAKING) && takes(h, pick, Enchantments.MENDING),
            "a bronze pickaxe takes Efficiency, Unbreaking and Mending");
        h.succeed();
    }

    /** D2: drills and the chainsaw take the mining enchantments, never Unbreaking or Mending. */
    @GameTest(template = "empty")
    public static void miningToolsTakeMiningEnchantments(GameTestHelper h) {
        for (String id : new String[] {"item_tool_iron_driller", "item_tool_diamond_driller", "item_tool_iridium_driller", "item_tool_iron_chainsaw"}) {
            ItemStack s = stack(id);
            h.assertTrue(takes(h, s, Enchantments.EFFICIENCY) && takes(h, s, Enchantments.FORTUNE) && takes(h, s, Enchantments.SILK_TOUCH),
                id + " takes Efficiency, Fortune and Silk Touch");
            h.assertTrue(!takes(h, s, Enchantments.UNBREAKING) && !takes(h, s, Enchantments.MENDING), id + " takes no Unbreaking or Mending");
            h.assertTrue(takesEnergySaving(h, s), id + " keeps Energy Saving");
            h.assertTrue(s.isEnchantable(), id + " can go on an enchanting table");
        }
        h.succeed();
    }

    /** D2: the nanosaber and the quantum sword take the sword enchantments; the nano bow and the fishing rod their own. */
    @GameTest(template = "empty")
    public static void weaponsTakeTheirEnchantments(GameTestHelper h) {
        for (String id : new String[] {"item_tool_nanosaber", "item_tool_quantum_sword"}) {
            ItemStack s = stack(id);
            h.assertTrue(takes(h, s, Enchantments.SHARPNESS) && takes(h, s, Enchantments.LOOTING) && takes(h, s, Enchantments.SMITE)
                && takes(h, s, Enchantments.SWEEPING_EDGE) && takes(h, s, Enchantments.FIRE_ASPECT), id + " takes the sword enchantments");
            h.assertTrue(!takes(h, s, Enchantments.UNBREAKING) && !takes(h, s, Enchantments.MENDING), id + " takes no Unbreaking or Mending");
            h.assertTrue(s.is(Tags.Items.MELEE_WEAPON_TOOLS), id + " is a melee weapon");
        }
        ItemStack bow = stack("item_tool_nano_bow");
        h.assertTrue(takes(h, bow, Enchantments.POWER) && takes(h, bow, Enchantments.INFINITY) && bow.isEnchantable(), "the nano bow takes bow enchantments");
        h.assertTrue(bow.is(Tags.Items.TOOLS_BOW) && bow.is(Tags.Items.RANGED_WEAPON_TOOLS), "the nano bow is a bow and a ranged weapon");
        ItemStack rod = stack("item_electric_fishing_rod");
        h.assertTrue(takes(h, rod, Enchantments.LURE) && takes(h, rod, Enchantments.LUCK_OF_THE_SEA), "the electric rod takes Lure and Luck of the Sea");
        h.assertTrue(stack("item_tool_steel_shield").is(Tags.Items.TOOLS_SHIELD), "the steel shield is a shield");
        ItemStack rifle = stack("item_tool_tactical_laser_rifle");
        h.assertTrue(!takes(h, rifle, Enchantments.SHARPNESS) && takesEnergySaving(h, rifle), "rifles keep Energy Saving only");
        h.succeed();
    }

    /** W-06: the nano bow took an arrow and marked it so it could never be picked up. */
    @GameTest(template = "empty")
    public static void nanoBowArrowsCanBePickedUp(GameTestHelper h) {
        ServerPlayer p = ArmoryServiceGameTests.player(h);
        ItemStack bow = stack("item_tool_nano_bow");
        ItemStack arrows = new ItemStack(Items.ARROW, 5);
        Class<?> type;
        try {
            type = Class.forName(COMMON + "item.tools.mio_icif_nano_bow");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
        ItemStack shot = (ItemStack) ArmoryServiceGameTests.call(type, "useAmmo", bow, arrows, p, false);
        h.assertTrue(arrows.getCount() == 4, "one arrow is taken, left " + arrows.getCount());
        h.assertTrue(shot.getCount() == 1 && !shot.has(DataComponents.INTANGIBLE_PROJECTILE), "the shot arrow can be picked up");
        p.discard();
        h.succeed();
    }
}
