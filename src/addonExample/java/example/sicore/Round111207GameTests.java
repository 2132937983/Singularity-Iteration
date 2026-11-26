package example.sicore;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.207: the hidden challenge "nixiankaide" (a player's idea: whoever has a Creative Generator "turned it on first"):
 * getting a Creative Generator grants it, and it hangs in the generator branch.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111207GameTests {
    private Round111207GameTests() {}

    @GameTest(template = "empty", batch = "round111207_nixiankaide", timeoutTicks = 20)
    public static void aCreativeGeneratorGrantsNixiankaide(GameTestHelper h) {
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("mio_icif", "nixiankaide"));
        h.assertTrue(holder != null, "no nixiankaide advancement");
        var display = holder.value().display().orElseThrow();
        h.assertTrue(display.isHidden() && display.getType() == AdvancementType.CHALLENGE, "nixiankaide is not a hidden challenge");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        h.assertTrue(!player.getAdvancements().getOrStartProgress(holder).isDone(), "granted before anything happened");
        ItemStack generator = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "generator/block_creative_generator")));
        player.getInventory().add(generator.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), generator);
        h.assertTrue(player.getAdvancements().getOrStartProgress(holder).isDone(), "a Creative Generator did not grant nixiankaide");
        h.succeed();
    }
}
