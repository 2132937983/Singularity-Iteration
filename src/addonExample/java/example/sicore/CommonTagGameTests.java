package example.sicore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Convention (c:) tags that other industrial mods query, as loaded by the game. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CommonTagGameTests {
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path))); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }
    private static TagKey<net.minecraft.world.item.Item> itemTag(String path) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path)); }
    private static TagKey<Block> blockTag(String path) { return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path)); }

    @GameTest(template = "reactor_loop", batch = "tags", timeoutTicks = 20)
    public static void materialTagsFollowConventions(GameTestHelper h) {
        h.assertTrue(item("resource/item_ingot_tin").is(itemTag("ingots/tin")) && item("resource/item_ingot_tin").is(itemTag("ingots")), "Tin ingot must be c:ingots/tin and c:ingots");
        h.assertTrue(item("resource/item_tin_plate").is(itemTag("plates")), "Plates must be in the c:plates parent");
        h.assertTrue(item("resource/item_tin_dust_small").is(itemTag("tiny_dusts/tin")), "Small dust must be c:tiny_dusts/tin");
        h.assertFalse(item("resource/item_tin_dust_small").is(itemTag("dusts")), "A small dust must not count as a full c:dusts");
        h.assertTrue(item("resource/item_tin_dust").is(itemTag("dusts")), "Full dust must stay in c:dusts");
        h.assertTrue(item("resource/item_niobium_titanium_ingot").is(itemTag("ingots/niobium_titanium")), "Alloy ingot tag missing");
        h.assertTrue(item("block_bronze").is(itemTag("storage_blocks/bronze")), "Bronze block item tag missing");
        h.assertTrue(block("block_bronze").defaultBlockState().is(blockTag("storage_blocks/bronze")), "Bronze block tag missing");
        h.assertTrue(block("block_ore_tin").defaultBlockState().is(blockTag("ores/tin")) && block("block_ore_tin_in_deep").defaultBlockState().is(blockTag("ores")), "Ore block tags missing");
        h.assertTrue(item("item_tool_wrench").is(itemTag("tools/wrench")), "Wrench tag missing");
        h.assertTrue(item("item_tool_tactical_laser_rifle").is(itemTag("tools/ranged_weapon")) && item("item_tool_laser_miner").is(itemTag("tools/mining_tool")), "Tool tags missing");
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "tags", timeoutTicks = 20)
    public static void refinedIronIsInterchangeableWithSteel(GameTestHelper h) {
        for (String[] form : new String[][]{{"ingots", "resource/item_adviron_ingot"}, {"plates", "resource/item_adviron_plate"},
                {"dense_plates", "resource/item_adviron_denseplate"}, {"casings", "resource/item_adviron_casing"},
                {"storage_blocks", "block_adviron"}}) {
            ItemStack stack = item(form[1]);
            for (String alias : new String[]{"steel", "adviron", "refined_iron"})
                h.assertTrue(stack.is(itemTag(form[0] + "/" + alias)), form[1] + " must be c:" + form[0] + "/" + alias);
        }
        h.assertTrue(block("block_adviron").defaultBlockState().is(blockTag("storage_blocks/steel"))
            && block("block_adviron").defaultBlockState().is(blockTag("storage_blocks/refined_iron")), "Refined iron block must be a steel storage block");
        h.succeed();
    }
}
