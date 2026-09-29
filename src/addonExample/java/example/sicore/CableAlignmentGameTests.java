package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile;
import com.miophas.singularity_iteration.core.runtime.CoreConfig;
import com.miophas.singularity_iteration.core.runtime.energy.engine.*;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class CableAlignmentGameTests {
    @GameTest(template = "empty", batch = "cable_arithmetic")
    public static void shockUsesPathMaximaAndSumsDistinctPaths(GameTestHelper h) {
        var accumulator = new ShockAccumulator<String, Object>();
        Object cow = new Object(), other = new Object();
        accumulator.add("first", cow, 24); accumulator.add("first", cow, 24);
        accumulator.add("first", cow, 96.9); accumulator.add("first", cow, 96);
        accumulator.add("second", cow, 33); // floor(96.9)+33=129, ceil(/64)=3.
        accumulator.add("first", other, 0.99);
        int[] calls = {0};
        accumulator.apply((entity, damage) -> { h.assertTrue(entity == cow && damage == 3, "Shock path accounting differs from IC2"); calls[0]++; });
        h.assertTrue(calls[0] == 1, "Fraction below 1 EU produced shock damage");
        accumulator.apply((entity, damage) -> h.fail("Old exposure replayed next settlement"));
        h.succeed();
    }

    @GameTest(template = "empty", batch = "cable_arithmetic")
    public static void fractionalLossPreservesConservationAndFeGranularity(GameTestHelper h) {
        boolean previous = CoreConfig.getBool("energynet", "roundEnetLoss", true);
        try {
            var sources = List.of(new DomainDistributor.Source(32, 32, true));
            RouteCosts route = new RouteCosts() {
                public boolean reaches(int id) { return true; }
                public long lossMilliTo(int id) { return 200; }
            };
            var domains = List.of(new DomainDistributor.Domain(new int[]{0}, List.of(route), new int[][]{{0}}));
            for (boolean rounded : new boolean[]{true, false}) {
                CoreConfig.overrideBool("energynet", "roundEnetLoss", rounded);
                var result = FractionalDistributor.allocateTraced(sources, List.of(EnergyAmount.of(32)), domains,
                    new int[]{0}, List.of(EnergyAmount.of(1000)), List.of(), new Random(1));
                h.assertTrue(result.debit(0).equals(EnergyAmount.of(32))
                    && result.credit(0).add(result.dissipated()).equals(result.debit(0)), "Fractional loss broke conservation");
                h.assertTrue(Math.abs(result.credit(0).toDouble() - (rounded ? 32 : 31.8)) < 1e-12, "Wrong rounded/fractional loss");
                h.assertTrue(EnergyLoss.feLoss(200) == (rounded ? 0 : 1), "FE loss was rounded down below its actual route cost");
            }
        } finally { CoreConfig.overrideBool("energynet", "roundEnetLoss", previous); }
        h.succeed();
    }

    @GameTest(template = "reactor_loop", batch = "cable_materials", timeoutTicks = 50)
    public static void materialLayersSurviveReloadAndStripOneAtATime(GameTestHelper h) {
        String[] ids = {"block_tin_cable", "block_tin_cable_1", "block_cable_o", "block_cable", "block_gold_cable", "block_gold_cable_1",
            "block_gold_cable_2", "block_iron_cable", "block_iron_cable_1", "block_iron_cable_2", "block_iron_cable_3", "block_glass_cable"};
        double[] absorption = {8,32,32,128,32,128,512,32,128,512,2048,Integer.MAX_VALUE};
        for (int i = 0; i < ids.length; i++) {
            var p = new BlockPos(i, 2, 2); h.setBlock(p, cable(ids[i]));
            ICableEnergyNode wire = (ICableEnergyNode) h.getBlockEntity(p);
            h.assertTrue(wire.getInsulationEnergyAbsorption() == absorption[i], "Wrong insulation: " + ids[i]);
        }
        var p = new BlockPos(10,2,2);
        color(h, p, DyeColor.RED);
        var entity = h.getBlockEntity(p);
        var saved = entity.saveWithoutMetadata(h.getLevel().registryAccess());
        h.setBlock(p, Blocks.AIR); h.setBlock(p, cable("block_iron_cable_3"));
        h.getBlockEntity(p).loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(((IColoredEnergyTile)h.getBlockEntity(p)).getEnergyColor(Direction.UP) == DyeColor.RED, "Cable color lost on reload");
        for (int layer = 2; layer >= 0; layer--) {
            ((ICableEnergyNode)h.getBlockEntity(p)).removeInsulation();
            h.assertBlockPresent(cable("block_iron_cable" + (layer == 0 ? "" : "_" + layer)).getBlock(), p);
            h.assertTrue(((IColoredEnergyTile)h.getBlockEntity(p)).getEnergyColor(Direction.UP) == (layer == 0 ? null : DyeColor.RED), "Strip removed the wrong color/layer");
        }
        ((ICableEnergyNode)h.getBlockEntity(p)).removeInsulation();
        h.assertBlockPresent(cable("block_iron_cable").getBlock(), p);
        for (int i = 0; i < ids.length; i++) h.setBlock(new BlockPos(i,2,2), Blocks.AIR);
        h.runAfterDelay(20, h::succeed);
    }

    @GameTest(template = "reactor", batch = "cable_colors", timeoutTicks = 140)
    public static void colorsDisconnectReconnectAndBlackRemainsWildcard(GameTestHelper h) {
        var sourcePos = new BlockPos(1,2,2); var first = sourcePos.east(); var second = first.east(); var sinkPos = second.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        h.setBlock(first, cable("block_tin_cable_1")); h.setBlock(second, cable("block_tin_cable_1"));
        color(h, first, DyeColor.RED); color(h, second, DyeColor.BLUE);
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now <= 110) source.generate(32);
            if (now == 30) { h.assertTrue(sink.stored() == 0, "Different colors conducted"); color(h, second, DyeColor.RED); }
            if ((now > 50 && now <= 70) || (now > 90 && now <= 110)) h.assertTrue(sink.stored() == 32, "Same/wildcard colors did not resume continuous flow");
            if (now == 70) color(h, second, DyeColor.BLACK);
            if (now <= 110) sink.getEnergyStorageInternal().setStored(0);
            if (now == 110) for (var p : List.of(sourcePos, first, second, sinkPos)) h.setBlock(p, Blocks.AIR);
            if (now == 130) h.succeed();
        });
    }

    @GameTest(template = "reactor_loop", batch = "cable_shock_dry", timeoutTicks = 110)
    public static void adjacentBareWiresShockOnceAndIdleWiresStop(GameTestHelper h) { shock(h, false); }
    @GameTest(template = "reactor_loop", batch = "cable_shock_wet", timeoutTicks = 110)
    public static void wetWireDoesNotElectrifyDistantWater(GameTestHelper h) { shock(h, true); }

    private static void shock(GameTestHelper h, boolean wet) {
        var sourcePos = new BlockPos(1,3,2); var first = sourcePos.east(); var second = first.east(); var sinkPos = second.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        var state = cable("block_tin_cable").setValue(BlockStateProperties.WATERLOGGED, wet);
        h.setBlock(first, state); h.setBlock(second, state);
        InheritedSource source = h.getBlockEntity(sourcePos);
        var cow = h.spawn(EntityType.COW, first); cow.setNoAi(true); cow.setNoGravity(true);
        var distant = h.spawn(EntityType.COW, new BlockPos(8,3,2)); distant.setNoAi(true); distant.setNoGravity(true);
        if (wet) h.setBlock(new BlockPos(8,3,2), Blocks.WATER);
        h.runAfterDelay(40, () -> source.generate(32));
        h.runAfterDelay(45, () -> {
            h.assertTrue(cow.getHealth() == cow.getMaxHealth() - 1, "Two bare segments should cause one point, health=" + cow.getHealth());
            h.assertTrue(distant.getHealth() == distant.getMaxHealth(), "Remote water inherited cable shock");
            cow.setHealth(cow.getMaxHealth()); cow.invulnerableTime = 0;
        });
        h.runAfterDelay(70, () -> {
            h.assertTrue(cow.getHealth() == cow.getMaxHealth() && distant.getHealth() == distant.getMaxHealth(), "Idle powered animation replayed shock");
            cow.discard(); distant.discard();
            for (var p : List.of(sourcePos, first, second, sinkPos, new BlockPos(8,3,2))) h.setBlock(p, Blocks.AIR);
        });
        h.runAfterDelay(90, h::succeed);
    }

    private static void color(GameTestHelper h, BlockPos at, DyeColor color) {
        var entity = h.getBlockEntity(at);
        var tag = entity.saveWithoutMetadata(h.getLevel().registryAccess()); tag.putInt("CableColor", color.getId());
        entity.loadWithComponents(tag, h.getLevel().registryAccess());
    }

    @GameTest(template = "reactor", batch = "cable_color_drop", timeoutTicks = 45)
    public static void coloredCableDropRestoresColorThroughBlockItemPlacement(GameTestHelper h) {
        var p = new BlockPos(2,2,2); h.setBlock(p,cable("block_iron_cable_3")); color(h,p,DyeColor.BLUE);
        var world = h.getLevel(); var absolute = h.absolutePos(p);
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(p),world,absolute,h.getBlockEntity(p));
        h.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1, "Cable did not drop exactly one item");
        var stack = drops.getFirst();
        h.setBlock(p,Blocks.AIR); h.setBlock(p.below(),Blocks.STONE);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(absolute),
            Direction.UP,absolute.below(),false);
        var context = new net.minecraft.world.item.context.BlockPlaceContext(world,player,
            net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit);
        var result = ((net.minecraft.world.item.BlockItem) stack.getItem()).place(context);
        h.assertTrue(result.consumesAction(), "Dropped cable could not be placed");
        h.assertTrue(((IColoredEnergyTile)h.getBlockEntity(p)).getEnergyColor(Direction.UP) == DyeColor.BLUE,
            "Survival placement lost the dropped cable color");
        h.setBlock(p,Blocks.AIR); h.setBlock(p.below(),Blocks.AIR);
        h.runAfterDelay(20,h::succeed);
    }
    private static BlockState cable(String name) {
        var id = ResourceLocation.parse("mio_icif:wiring/cable/" + name);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Missing cable " + id);
        return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }
}
