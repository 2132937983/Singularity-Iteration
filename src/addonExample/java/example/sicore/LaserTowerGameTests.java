package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Laser defence towers: multi-target volleys capped per tier, exact EU cost per target,
 * hostile-only targeting and the adjustable scan box.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class LaserTowerGameTests {
    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    private static List<Zombie> zombies(GameTestHelper h, int count) {
        List<Zombie> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(4 + (i % 4) * 2, 2, 5 + (i / 4) * 2));
            z.setNoAi(true);
            list.add(z);
        }
        return list;
    }

    private static long hurt(List<Zombie> list) {
        return list.stream().filter(z -> !z.isAlive() || z.getHealth() < z.getMaxHealth()).count();
    }

    private static AbstractEnergyBlockEntity place(GameTestHelper h, String id, long energy) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, block(id));
        var tower = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        tower.getEnergyStorageInternal().setEnergy(energy);
        return tower;
    }

    /** Sets the scan box through the saved-data path (clamped on load); returns the applied {h, v}. */
    private static int[] setRange(GameTestHelper h, AbstractEnergyBlockEntity tower, int horizontal, int vertical) {
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = tower.saveWithoutMetadata(registries);
        tag.putInt("TowerRangeH", horizontal);
        tag.putInt("TowerRangeV", vertical);
        tower.loadWithComponents(tag, registries);
        CompoundTag saved = tower.saveWithoutMetadata(registries);
        return new int[]{saved.getInt("TowerRangeH"), saved.getInt("TowerRangeV")};
    }

    @GameTest(template = "reactor_loop", batch = "tower_ground", timeoutTicks = 120)
    public static void groundTowerHitsFiveTargetsPerVolley(GameTestHelper h) {
        var tower = place(h, "producer/block_laser_defense_tower", 100_000);
        h.assertTrue(tower.getEnergyStorageInternal().getCapacity() == 250_000, "Laser Defense Tower stores 250k EU");
        var targets = zombies(h, 7);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 4.5)));
        float playerHealth = player.getHealth();
        h.succeedWhen(() -> {
            long spent = 100_000 - tower.getEnergyStorageInternal().getAmount();
            h.assertTrue(spent > 0, "Tower has not fired yet");
            h.assertTrue(hurt(targets) == 5, "First volley must hit exactly 5 of 7 hostiles, hit " + hurt(targets));
            h.assertTrue(spent == 5 * 1_250, "Volley must cost 1250 EU per target, spent " + spent);
            h.assertTrue(player.getHealth() == playerHealth, "Tower must never target players");
        });
    }

    @GameTest(template = "reactor_loop", batch = "tower_sky", timeoutTicks = 120)
    public static void skyPatrolHitsTenTargetsPerVolley(GameTestHelper h) {
        var tower = place(h, "producer/block_sky_patrol_laser_tower", 400_000);
        h.assertTrue(tower.getEnergyStorageInternal().getCapacity() == 500_000, "Sky Patrol station stores 500k EU");
        int[] range = setRange(h, tower, 8, 4);   // keep the scan inside this test's structure
        h.assertTrue(range[0] == 8 && range[1] == 4, "Range adjustment must apply");
        var targets = zombies(h, 12);
        h.succeedWhen(() -> {
            long spent = 400_000 - tower.getEnergyStorageInternal().getAmount();
            h.assertTrue(spent > 0, "Tower has not fired yet");
            h.assertTrue(hurt(targets) == 10, "First volley must hit exactly 10 of 12 hostiles, hit " + hurt(targets));
            h.assertTrue(spent == 10 * 2_500, "Volley must cost 2500 EU per target, spent " + spent);
        });
    }

    @GameTest(template = "reactor_loop", batch = "tower_range", timeoutTicks = 120)
    public static void towerRespectsAdjustedRangeAndEnergy(GameTestHelper h) {
        var tower = place(h, "producer/block_laser_defense_tower", 1_250);   // enough for one target only
        int[] clamped = setRange(h, tower, -100, 1000);
        h.assertTrue(clamped[0] == 1 && clamped[1] == 32, "Range must clamp to 1..max, got " + clamped[0] + "/" + clamped[1]);
        setRange(h, tower, 3, 2);
        var near = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        near.setNoAi(true);
        var near2 = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 4));
        near2.setNoAi(true);
        var far = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 2));
        far.setNoAi(true);
        h.succeedWhen(() -> {
            int hit = (near.getHealth() < near.getMaxHealth() ? 1 : 0) + (near2.getHealth() < near2.getMaxHealth() ? 1 : 0);
            h.assertTrue(hit == 1, "Exactly one target is affordable with 1250 EU, hit " + hit);
            h.assertTrue(tower.getEnergyStorageInternal().getAmount() == 0, "The single shot must spend the buffer");
            h.assertTrue(far.getHealth() == far.getMaxHealth(), "Zombie outside the scan box must be ignored");
        });
    }
}
