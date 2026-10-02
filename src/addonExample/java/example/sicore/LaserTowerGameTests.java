package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
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

    private static List<Husk> zombies(GameTestHelper h, int count) {
        List<Husk> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Husk z = h.spawn(EntityType.HUSK, new BlockPos(4 + (i % 4) * 2, 2, 5 + (i / 4) * 2));
            z.setNoAi(true);
            list.add(z);
        }
        return list;
    }

    private static long hurt(List<Husk> list) {
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
        var near = h.spawn(EntityType.HUSK, new BlockPos(4, 2, 2));
        near.setNoAi(true);
        var near2 = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 4));
        near2.setNoAi(true);
        var far = h.spawn(EntityType.HUSK, new BlockPos(8, 2, 2));
        far.setNoAi(true);
        h.succeedWhen(() -> {
            int hit = (near.getHealth() < near.getMaxHealth() ? 1 : 0) + (near2.getHealth() < near2.getMaxHealth() ? 1 : 0);
            h.assertTrue(hit == 1, "Exactly one target is affordable with 1250 EU, hit " + hit);
            h.assertTrue(tower.getEnergyStorageInternal().getAmount() == 0, "The single shot must spend the buffer");
            h.assertTrue(far.getHealth() == far.getMaxHealth(), "Zombie outside the scan box must be ignored");
        });
    }

    private static void setFilter(GameTestHelper h, AbstractEnergyBlockEntity tower, String mode, String... entries) {
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = tower.saveWithoutMetadata(registries);
        CompoundTag filter = new CompoundTag();
        filter.putString("Mode", mode);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (String e : entries) list.add(net.minecraft.nbt.StringTag.valueOf(e));
        filter.put("Entries", list);
        tag.put("TowerFilter", filter);
        tower.loadWithComponents(tag, registries);
    }

    @GameTest(template = "reactor_loop", batch = "tower_whitelist", timeoutTicks = 120)
    public static void whitelistAttacksOnlyListedEntities(GameTestHelper h) {
        var tower = place(h, "producer/block_laser_defense_tower", 100_000);
        setFilter(h, tower, "WHITELIST", "minecraft:cow");
        var cow = h.spawn(EntityType.COW, new BlockPos(5, 2, 2));
        cow.setNoAi(true);
        var spider = h.spawn(EntityType.SPIDER, new BlockPos(2, 2, 5));   // hostile, does not burn in daylight
        spider.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(cow.getHealth() < cow.getMaxHealth() || !cow.isAlive(), "Whitelisted cow must be attacked");
            h.assertTrue(spider.getHealth() == spider.getMaxHealth(), "Unlisted spider must be ignored in whitelist mode");
        });
    }

    @GameTest(template = "reactor_loop", batch = "tower_blacklist", timeoutTicks = 120)
    public static void blacklistSparesListedHostilesAndRecordsStats(GameTestHelper h) {
        var tower = place(h, "producer/block_laser_defense_tower", 100_000);
        setFilter(h, tower, "BLACKLIST", "creeper");   // bare vanilla name resolves to minecraft:creeper
        var creeper = h.spawn(EntityType.CREEPER, new BlockPos(5, 2, 2));
        creeper.setNoAi(true);
        var zombie = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 5));
        zombie.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "Hostile zombie must be attacked");
            h.assertTrue(creeper.getHealth() == creeper.getMaxHealth(), "Blacklisted creeper must be spared");
            CompoundTag stats = tower.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("TowerStats");
            long hits = stats.getLong("Hits");
            h.assertTrue(hits >= 1 && stats.getCompound("HitsByType").getLong("minecraft:husk") == hits,
                "Stats must record the zombie hits, got " + stats);
            h.assertTrue(stats.getLong("Energy") == hits * 1_250 && stats.getLong("Volleys") >= 1, "Stats energy/volleys mismatch " + stats);
        });
    }

    private static net.minecraft.world.item.ItemStack item(String path, int count) {
        return new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)), count);
    }

    /** Upgrade bay: overclockers speed volleys up and raise the per-target cost (IC2 x0.7 / x1.6), transformer raises the tier. */
    @GameTest(template = "reactor_loop", batch = "tower_upgrades", timeoutTicks = 120)
    public static void towerUpgradeBayAcceptsOverclockerAndTransformer(GameTestHelper h) {
        var tower = (com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity)
            place(h, "producer/block_laser_defense_tower", 250_000);
        var inv = tower.getItemHandler();
        h.assertTrue(inv.getSlots() == 5, "Tower must expose battery + 4 upgrade slots, got " + inv.getSlots());
        h.assertTrue(tower.getUpgradeSlotStart() == 1 && tower.getUpgradeSlotCount() == 4, "Upgrade range must be slots 1..4");
        h.assertTrue(!inv.isItemValid(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)), "Dirt is not an upgrade");
        h.assertTrue(!inv.isItemValid(1, item("upgrade/ejector_upgrade", 1)), "Ejector upgrade means nothing to a tower");
        h.assertTrue(inv.isItemValid(1, item("upgrade/overclocker_upgrade", 1)), "Overclocker must fit the bay");
        h.assertTrue(inv.isItemValid(2, item("upgrade/transformer_upgrade", 1)), "Transformer upgrade must fit the bay");
        int baseTier = tower.getEffectiveCableTier().getTier();
        long baseReceive = tower.getEffectiveMaxReceive();
        inv.insertItem(1, item("upgrade/overclocker_upgrade", 2), false);
        inv.insertItem(2, item("upgrade/transformer_upgrade", 1), false);
        List<Husk> targets = zombies(h, 7);
        long start = tower.getEnergyStorageInternal().getAmount();
        h.succeedWhen(() -> {
            long spent = start - tower.getEnergyStorageInternal().getAmount();
            h.assertTrue(spent > 0, "Tower has not fired yet");
            if (spent != 5 * 3_200L) h.fail("Two overclockers must cost ceil(1250*1.6^2)=3200 EU per target, spent " + spent);
            if (tower.getEffectiveCableTier().getTier() != baseTier + 1) h.fail("Transformer upgrade must raise the input tier: " + baseTier + " -> " + tower.getEffectiveCableTier().getTier());
            if (tower.getEffectiveMaxReceive() <= baseReceive) h.fail("Transformer upgrade must raise the accepted packet size");
            h.assertTrue(hurt(targets) == 5, "Overclocked tower still hits 5 per volley");
            CompoundTag saved = tower.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(saved.toString().contains("overclocker_upgrade"), "Upgrades must persist with the tower");
        });
    }
}
