package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.7.21: pattern scanner never locks itself, electric furnace XP goes straight to the player,
 * generators keep only a charge slot, and the segmented energy gauge is green at the full end.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round21GameTests {
    private Round21GameTests() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(id(path)); }
    private static Object call(Object target, String name, Object... args) { return ArmoryServiceGameTests.call(target, name, args); }

    @GameTest(batch = "round21", template = "empty")
    public static void scannerPlacedFromItemDataIsNotPaused(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("producer/block_scanner_elc"));
        var be = h.getBlockEntity(pos);
        // what a wrench-dismantled scanner item carries (id + energy, no scan record)
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "mio_icif:scanner_elc");
        tag.putLong("energy", 1000);
        be.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(!(boolean) call(be, "hasHeldScanData"), "Scanner must not pause on item/legacy block-entity data");
        // an unreadable saved record also recovers instead of locking the machine
        CompoundTag broken = be.saveWithoutMetadata(h.getLevel().registryAccess());
        broken.getCompound("scex_scanner_v1").put("held", new CompoundTag());
        be.loadWithComponents(broken, h.getLevel().registryAccess());
        h.assertTrue(!(boolean) call(be, "hasHeldScanData"), "A held record is discarded on load");
        h.succeed();
    }

    @GameTest(batch = "round21", template = "empty", timeoutTicks = 300)
    public static void electricFurnaceXpGoesStraightToThePlayer(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("producer/block_furnace_elc"));
        var furnace = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        var items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), null);
        h.assertTrue(items.insertItem(0, new ItemStack(Items.CACTUS, 2), false).isEmpty(), "Cactus accepted");
        h.onEachTick(() -> furnace.getEnergyStorageInternal().setStored(furnace.getEnergyStorageInternal().getCapacity()));
        h.succeedWhen(() -> {
            float stored = (float) call(furnace, "getStoredExperience");
            h.assertTrue(stored >= 2.0F, "Two cactus smelted (1 XP each), stored " + stored);
            var player = h.makeMockServerPlayerInLevel();
            int before = player.totalExperience;
            int given = (int) call(furnace, "collectExperience", player);
            h.assertTrue(given == 2 && player.totalExperience - before == 2, "XP added to the player directly: " + given);
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(8)).isEmpty(), "No experience orbs are dropped");
        });
    }

    @GameTest(batch = "round21", template = "empty", timeoutTicks = 80)
    public static void solarGeneratorEjectsItemsFromRemovedDischargeSlot(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, block("generator/block_photon_resonance_solar_generator"));
        var be = h.getBlockEntity(pos);
        var handler = (net.neoforged.neoforge.items.IItemHandlerModifiable) call(be, "getItemHandler");
        handler.setStackInSlot(1, new ItemStack(BuiltInRegistries.ITEM.get(id("normal/item_advbat_lev0"))));
        h.succeedWhen(() -> {
            h.assertTrue(handler.getStackInSlot(1).isEmpty(), "Hidden slot emptied");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3)).isEmpty(),
                "Battery dropped, not lost");
        });
    }

    @GameTest(batch = "round21", template = "empty")
    public static void energyGaugeIsGreenWhenFull(GameTestHelper h) {
        var path = net.neoforged.fml.ModList.get().getModFileById("mio_icif").getFile().findResource("assets", "mio_icif", "textures", "gui", "gui_components_atlas.png");
        try (var in = java.nio.file.Files.newInputStream(path)) {
            var image = javax.imageio.ImageIO.read(in);
            for (int seg = 13; seg < 19; seg++) {
                int rgb = image.getRGB(4 + seg * 5 + 1, 315);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                h.assertTrue(g > r && g > b + 60, "Segment " + seg + " of the full gauge must be green, got " + Integer.toHexString(rgb));
            }
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        h.succeed();
    }
}
