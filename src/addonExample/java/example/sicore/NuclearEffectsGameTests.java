package example.sicore;

import com.miophas.singularity_iteration.core.runtime.reactor.BlastInput;
import com.miophas.singularity_iteration.core.runtime.reactor.BlastWorkEstimate;
import com.miophas.singularity_iteration.core.runtime.reactor.NuclearCloudSimulation;
import com.miophas.singularity_iteration.core.runtime.radiation.RadiationState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class NuclearEffectsGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Block nuke() { return BuiltInRegistries.BLOCK.get(id("reactor/block_reactor_nuke")); }
    private static Class<?> implementation(String name) {
        try {
            var entity = ((EntityBlock) nuke()).newBlockEntity(BlockPos.ZERO, nuke().defaultBlockState());
            return Class.forName(entity.getClass().getPackageName() + "." + name);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object task(GameTestHelper h, Vec3 center, float power, int radius, List<Entity> entities) {
        try { return implementation("NukeExplosionTask").getConstructor(ServerLevel.class, Vec3.class,
            float.class, int.class, List.class).newInstance(h.getLevel(), center, power, radius, entities); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static long finish(GameTestHelper h, Object task, int radius, int entities) {
        long limit = BlastWorkEstimate.nuke(radius, entities).totalOperations();
        long steps = 0;
        while (!(boolean) ArmoryServiceGameTests.call(task, "isComplete") && steps <= limit) {
            ArmoryServiceGameTests.call(task, "advance"); steps++;
        }
        h.assertTrue(steps <= limit, "Blast exceeded its bounded work estimate");
        return steps;
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void cloudsStayFiniteThroughTheirEntireLifetimeAndRespectDensityCap(GameTestHelper h) {
        for (int radius : new int[] {24, 2000}) {
            var cloud = new NuclearCloudSimulation(radius, 10, 7);
            h.assertTrue(cloud.parcels().length == NuclearCloudSimulation.MAX_PARCELS, "Density budget not capped");
            for (int tick = 0; tick <= NuclearCloudSimulation.LIFETIME + 20; tick++) {
                cloud.tick();
                if (tick % 20 == 0) for (var p : cloud.parcels()) {
                    h.assertTrue(Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z)
                        && Float.isFinite(p.size) && p.alpha >= 0 && p.alpha <= 1, "Invalid cloud parcel at late age " + tick);
                }
            }
            h.assertTrue(cloud.isComplete(), "Cloud did not expire");
            for (var p : cloud.parcels()) h.assertTrue(p.alpha == 0, "Cloud expired before fading out");
        }
        h.assertTrue(new NuclearCloudSimulation(24, 0, 7).parcels().length == 0, "Zero density still allocates particles");
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void flashPrecedesTheShockAndTheFarthestListenerReceivesItBeforeExpiry(GameTestHelper h) {
        h.assertTrue(NuclearCloudSimulation.flash(0) == 1 && NuclearCloudSimulation.flash(60) == 0, "Flash envelope incorrect");
        h.assertTrue(NuclearCloudSimulation.shockRadius(20) == 45, "Pressure front propagation incorrect");
        h.assertTrue(NuclearCloudSimulation.shockRadius(NuclearCloudSimulation.LIFETIME - 60) > 4096, "Distant listener loses audio before arrival");
        h.assertTrue(NuclearCloudSimulation.shake(-1) == 0 && NuclearCloudSimulation.shake(0) == 1
            && NuclearCloudSimulation.shake(60) == 0, "Shock shake does not decay");
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void shieldedSolidBlocksSurviveButFalloutRemovesVegetation(GameTestHelper h) {
        for (int y = 1; y <= 6; y++) for (int z = 2; z <= 10; z++) h.setBlock(new BlockPos(7, y, z), Blocks.BEDROCK);
        h.setBlock(new BlockPos(5, 3, 6), Blocks.OBSIDIAN);
        h.setBlock(new BlockPos(8, 3, 6), Blocks.BRICKS);
        h.setBlock(new BlockPos(8, 4, 6), Blocks.OAK_LEAVES);
        finish(h, task(h, Vec3.atCenterOf(h.absolutePos(new BlockPos(6, 3, 6))), 400, 4, List.of()), 4, 0);
        h.assertTrue(h.getBlockState(new BlockPos(5, 3, 6)).isAir(), "Exposed obsidian remained an absolute nuclear shield");
        h.assertTrue(h.getBlockState(new BlockPos(7, 3, 6)).is(Blocks.BEDROCK), "Bedrock destroyed");
        h.assertTrue(h.getBlockState(new BlockPos(8, 3, 6)).is(Blocks.BRICKS), "Pressure passed through bedrock");
        h.assertTrue(h.getBlockState(new BlockPos(8, 4, 6)).isAir(), "Fallout failed to remove vegetation inside its footprint");
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void shieldingIsReadBeforeTerrainAndControlsEntityImpulse(GameTestHelper h) {
        for (int y = 1; y <= 6; y++) for (int z = 2; z <= 10; z++) h.setBlock(new BlockPos(7, y, z), Blocks.BEDROCK);
        Vec3 center = Vec3.atCenterOf(h.absolutePos(new BlockPos(6, 3, 6)));
        ItemEntity exposed = new ItemEntity(h.getLevel(), center.x - 2, center.y, center.z, new ItemStack(Items.DIAMOND));
        ItemEntity covered = new ItemEntity(h.getLevel(), center.x + 2, center.y, center.z, new ItemStack(Items.DIAMOND));
        h.getLevel().addFreshEntity(exposed); h.getLevel().addFreshEntity(covered);
        finish(h, task(h, center, 400, 4, List.of(exposed, covered)), 4, 2);
        h.assertTrue(exposed.isRemoved() && !covered.isRemoved(), "Entity damage ignored shielding");
        covered.discard(); h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void negativeCoordinatesUseFloorInsteadOfTruncation(GameTestHelper h) {
        int y = h.absolutePos(new BlockPos(0, 3, 0)).getY();
        BlockPos correct = new BlockPos(-7, y, -7), truncated = new BlockPos(-6, y, -6);
        var a = h.getLevel().getBlockState(correct); var b = h.getLevel().getBlockState(truncated);
        try {
            h.getLevel().setBlock(correct, Blocks.OBSIDIAN.defaultBlockState(), 2);
            h.getLevel().setBlock(truncated, Blocks.OBSIDIAN.defaultBlockState(), 2);
            finish(h, task(h, new Vec3(-6.25, y + .5, -6.25), 400, 1, List.of()), 1, 0);
            h.assertTrue(h.getLevel().getBlockState(correct).isAir(), "Negative center was truncated");
            h.assertTrue(h.getLevel().getBlockState(truncated).is(Blocks.OBSIDIAN), "Blast sampled the wrong origin");
        } finally { h.getLevel().setBlock(correct, a, 2); h.getLevel().setBlock(truncated, b, 2); }
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void blastStopsAtUnloadedChunksWithoutLoadingThem(GameTestHelper h) {
        int x = 1000000, z = 1000000;
        h.assertTrue(h.getLevel().getChunkSource().getChunkNow(x >> 4, z >> 4) == null, "Test chunk already loaded");
        finish(h, task(h, new Vec3(x, h.absolutePos(new BlockPos(0, 3, 0)).getY(), z), 400, 3, List.of()), 3, 0);
        h.assertTrue(h.getLevel().getChunkSource().getChunkNow(x >> 4, z >> 4) == null, "Nuclear scan forced a chunk load");
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void zeroPowerDoesNoTerrainWorkAndSurfaceBoundsAreValidated(GameTestHelper h) {
        BlockPos pos = new BlockPos(6, 3, 6); h.setBlock(pos, Blocks.BRICKS);
        finish(h, task(h, Vec3.atCenterOf(h.absolutePos(pos)), 0, 4, List.of()), 4, 0);
        h.assertTrue(h.getBlockState(pos).is(Blocks.BRICKS), "Zero power destroys terrain");
        boolean rejected = false;
        try { BlastInput.nuke(Integer.MAX_VALUE - 4, 0, 0, 400, 4); }
        catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Fallout coordinate overflow not rejected"); h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void laterRaysStillPayForTheWallRemovedByEarlierRays(GameTestHelper h) {
        for (int x = 7; x <= 8; x++) for (int y = 1; y <= 6; y++) for (int z = 2; z <= 10; z++)
            h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        h.setBlock(new BlockPos(9, 3, 6), Blocks.GLASS);
        finish(h, task(h, Vec3.atCenterOf(h.absolutePos(new BlockPos(6, 3, 6))), 1, 5, List.of()), 5, 0);
        h.assertTrue(h.getBlockState(new BlockPos(7, 3, 6)).isAir(), "Weak front did not remove the first wall");
        h.assertTrue(!h.getBlockState(new BlockPos(8, 3, 6)).isAir(), "Later rays bypassed original wall resistance");
        h.assertTrue(h.getBlockState(new BlockPos(9, 3, 6)).is(Blocks.GLASS), "Blast tunnelled through two resistant layers");
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void farUnloadedFalloutRegionsAreSkippedByChunkRatherThanCubicScan(GameTestHelper h) {
        Object task = task(h, new Vec3(1000000, 0, 1000000), 1000000, 2000, List.of());
        long steps = finish(h, task, 2000, 0);
        h.assertTrue(steps < 200000, "Unloaded radius-2000 blast wastes work on empty columns or dead rays: " + steps);
        h.succeed();
    }

    private static Entity primed(GameTestHelper h) {
        try {
            Entity example = BuiltInRegistries.ENTITY_TYPE.get(id("nuke_primed")).create(h.getLevel());
            Vec3 center = Vec3.atCenterOf(h.absolutePos(new BlockPos(6, 3, 6)));
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(id("reactor/block_ic_tnt")));
            Entity entity = (Entity) example.getClass().getConstructor(Level.class, double.class, double.class,
                double.class, LivingEntity.class, List.class).newInstance(h.getLevel(), center.x, center.y, center.z, null, List.of(stack));
            entity.setNoGravity(true); entity.setDeltaMovement(Vec3.ZERO);
            h.getLevel().addFreshEntity(entity); return entity;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Class<?> scheduler() {
        try { return Class.forName(implementation("NukeExplosionTask").getPackageName().replace("blockentity.reactor", "reactor") + ".ExplosionWorkScheduler"); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void primedNukeInAirAdmitsTerrainWorkBeforeDiscard(GameTestHelper h) {
        var zonesBefore = new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet());
        Class<?> facade = implementation("NukeExplosionScheduler");
        int before = (int) ArmoryServiceGameTests.call(facade, "getActiveExplosionCount");
        Entity entity = primed(h);
        try {
            ArmoryServiceGameTests.call(entity, "setFuse", 1); entity.tick();
            h.assertTrue(entity.isRemoved(), "Primed nuke not discarded after successful admission");
            h.assertTrue((int) ArmoryServiceGameTests.call(facade, "getActiveExplosionCount") == before + 1,
                "Air-to-air removal cancelled the actual blast");
        } finally {
            ArmoryServiceGameTests.call(facade, "clear", h.getLevel()); entity.discard();
            for (var zone : new HashSet<>(RadiationState.get(h.getLevel()).zones.keySet())) if (!zonesBefore.contains(zone))
                ArmoryServiceGameTests.call(implementation("NukeRadiationZoneManager"), "removeRadiationZone", zone);
        }
        h.succeed();
    }
    @GameTest(template="reactor_loop", batch="nuclear_effects", timeoutTicks=40)
    public static void fullQueueKeepsPrimedPayloadAndRetriesInsteadOfLosingIt(GameTestHelper h) {
        Entity entity = primed(h); Class<?> scheduler = scheduler(); Object category = new Object();
        try {
            while ((boolean) ArmoryServiceGameTests.call(scheduler, "hasCapacity", h.getLevel()))
                h.assertTrue((boolean) ArmoryServiceGameTests.call(scheduler, "trySubmit", h.getLevel(), entity.blockPosition(),
                    category, new Object(), (BooleanSupplier) () -> false), "Could not fill test queue");
            ArmoryServiceGameTests.call(entity, "setFuse", 1); entity.tick();
            h.assertTrue(!entity.isRemoved() && (int) ArmoryServiceGameTests.call(entity, "getFuse") == 20,
                "Full explosion queue discarded the primed source");
            @SuppressWarnings("unchecked") List<ItemStack> items = (List<ItemStack>) ArmoryServiceGameTests.call(entity, "getContainedItems");
            h.assertTrue(items.get(0).getCount() == 1, "Retry lost explosive payload");
        } finally { ArmoryServiceGameTests.call(scheduler, "clear", h.getLevel(), category); entity.discard(); }
        h.succeed();
    }
}
