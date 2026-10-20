package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Water washes a crop rack with a crop away: the rack is gone and exactly two seed bags of the crop
 * (with its stats) drop, nothing else. Empty racks wash away as before (they drop the rack), and
 * watering a crop by hand only hydrates it.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CropWashingGameTests {
    private CropWashingGameTests() {}

    private static ResourceLocation si(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    /** A rack on farmland at (2, 2, 2); planted with wheat (growth 10, gain 5, resistance 3, scanned) when {@code planted}. */
    private static BlockPos rack(GameTestHelper h, String block, boolean planted) {
        BlockPos rack = new BlockPos(2, 2, 2);
        h.setBlock(rack.below(), Blocks.FARMLAND);
        h.setBlock(rack, BuiltInRegistries.BLOCK.get(si(block)));
        if (planted) {
            IPlanter crop = (IPlanter) h.getBlockEntity(rack);
            crop.setPlant(MioIcifAPI.instance().getCropAPI().getPlant("mio_icif", "wheat"));
            crop.setGrowthStage(3);
            crop.setGrowthSpeed(10);
            crop.setYield(5);
            crop.setResilience(3);
            crop.setScanLevel(4);
            crop.updateState();
        }
        return rack;
    }

    /** A water source east of the rack, walled in on its other sides so that it can only flow into the rack. */
    private static void water(GameTestHelper h, BlockPos rack) {
        BlockPos source = rack.east();
        for (BlockPos stone : new BlockPos[] {source.below(), source.east(), source.north(), source.south()}) h.setBlock(stone, Blocks.STONE);
        h.setBlock(source, Blocks.WATER);
    }

    private static List<ItemStack> drops(GameTestHelper h, BlockPos at) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(at)).inflate(3)).stream()
            .map(ItemEntity::getItem).toList();
    }

    private static void assertTwoWheatBags(GameTestHelper h, List<ItemStack> drops) {
        int bags = 0;
        for (ItemStack drop : drops) {
            h.assertTrue(drop.getItem() instanceof ICropSeedItem, "washing dropped " + drop);
            ICropSeedItem seed = (ICropSeedItem) drop.getItem();
            h.assertTrue(seed.cropId(drop).equals("wheat") && seed.cropGrowth(drop) == 10 && seed.cropGain(drop) == 5
                && seed.cropResistance(drop) == 3, "seed bag stats " + seed.cropData(drop));
            bags += drop.getCount();
        }
        h.assertTrue(bags == 2, bags + " seed bags instead of 2");
    }

    @GameTest(batch = "crop_washing", template = "empty", timeoutTicks = 100)
    public static void flowingWaterWashesAPlantedRackIntoTwoSeedBags(GameTestHelper h) {
        BlockPos rack = rack(h, "crop/stick", true);
        water(h, rack);
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(rack).getFluidState().is(FluidTags.WATER), "the rack is still there");
            assertTwoWheatBags(h, drops(h, rack));
        });
    }

    @GameTest(batch = "crop_washing", template = "empty", timeoutTicks = 40)
    public static void waterEmptiedIntoAnUpgradedRackWashesItToo(GameTestHelper h) {
        BlockPos rack = rack(h, "crop/stick_upgraded", true);
        // a bucket (or dispenser, or cell) emptied into the rack's space
        ((BucketItem) Items.WATER_BUCKET).emptyContents(null, h.getLevel(), h.absolutePos(rack), null);
        h.assertTrue(h.getBlockState(rack).getFluidState().is(FluidTags.WATER), "the rack is still there");
        assertTwoWheatBags(h, drops(h, rack));
        h.succeed();
    }

    @GameTest(batch = "crop_washing", template = "empty", timeoutTicks = 100)
    public static void anEmptyRackWashesAwayAsBefore(GameTestHelper h) {
        BlockPos rack = rack(h, "crop/stick", false);
        water(h, rack);
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(rack).getFluidState().is(FluidTags.WATER), "the rack is still there");
            List<ItemStack> drops = drops(h, rack);
            h.assertTrue(drops.stream().noneMatch(d -> d.getItem() instanceof ICropSeedItem), "an empty rack dropped seed bags");
            h.assertTrue(drops.stream().anyMatch(d -> BuiltInRegistries.ITEM.getKey(d.getItem()).equals(si("crop/stick"))), "no rack dropped: " + drops);
        });
    }

    /** A water cell gives the crop the water it takes (a bucket cannot give part of its 1,000 mB: it pours). */
    @GameTest(batch = "crop_washing", template = "empty", timeoutTicks = 20)
    public static void wateringWithACellOnlyHydrates(GameTestHelper h) {
        BlockPos rack = rack(h, "crop/stick", true);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cell = (ItemStack) Round33GameTests.call(Round33GameTests.common("item.cell.mio_icif_cells"),
            "getFilledCellForFluidStack", net.minecraft.world.level.material.Fluids.WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, cell);
        BlockPos abs = h.absolutePos(rack);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        IPlanter crop = (IPlanter) h.getBlockEntity(rack);
        h.assertTrue(crop.getPlant() != null && crop.getWater() > 0, "the crop was not watered");
        h.assertTrue(drops(h, rack).isEmpty(), "watering dropped items");
        h.succeed();
    }
}
