package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of 0.1.8.4: generator output config, weapon hook for AI shooters, build orders, SlashBlade bridge. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round184GameTests {

    /** Every entry of the GeneratorOutput config is a registered EU generator block entity, 28 in all. */
    @GameTest(template = "empty", batch = "round184_data")
    public static void generatorOutputCoversEveryEuGenerator(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Class<?> output = Round33GameTests.common("energy.GeneratorOutput");
        List<?> entries;
        try {
            entries = (List<?>) output.getField("GENERATORS").get(null);
        } catch (ReflectiveOperationException e) {
            h.fail("GeneratorOutput.GENERATORS: " + e);
            return;
        }
        for (Object entry : entries) {
            String id = (String) Round33GameTests.call(entry, "id");
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("mio_icif", id))) problems.add(id);
        }
        h.assertTrue(entries.size() == 28, "expected 28 generators, got " + entries.size());
        h.assertTrue(problems.isEmpty(), "not registered: " + problems);
        h.succeed();
    }

    /** An energy rifle fires for a non-player shooter: one bolt, paid from the gun; an empty gun refuses. */
    @GameTest(template = "empty", batch = "round184_combat")
    public static void energyRifleFiresForAiShooter(GameTestHelper h) {
        var shooter = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        ItemStack rifle = Round33GameTests.item("item_tool_electric_rifle");
        if (!(rifle.getItem() instanceof IWeaponItem gun)) { h.fail("electric rifle is not an IWeaponItem"); return; }
        gun.setEnergy(rifle, gun.getMaxEnergy(rifle));
        long before = gun.getEnergy(rifle);
        int cooldown = gun.fireFrom(rifle, shooter, new Vec3(1, 0, 0), true);
        h.assertTrue(cooldown >= 0, "rifle did not fire");
        h.assertTrue(gun.getEnergy(rifle) < before, "shot was free");
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_bullet"));
        AABB around = new AABB(h.absolutePos(new BlockPos(1, 2, 1))).inflate(6);
        h.assertTrue(!h.getLevel().getEntities(type, around, e -> true).isEmpty(), "no energy bolt spawned");
        ItemStack empty = Round33GameTests.item("item_tool_electric_rifle");
        gun.setEnergy(empty, 0);
        h.assertTrue(gun.fireFrom(empty, shooter, new Vec3(1, 0, 0), true) < 0, "an empty rifle fired");
        h.succeed();
    }

    /** Without SlashBlade: Resharped the bridge is off and a plain sword is not a blade. */
    @GameTest(template = "empty", batch = "round184_combat")
    public static void slashBladeBridgeIgnoresPlainSwords(GameTestHelper h) {
        Class<?> bridge = Round33GameTests.common("armory.SlashBladeCombat");
        h.assertTrue(!(Boolean) Round33GameTests.call(bridge, "isBlade", new ItemStack(Items.DIAMOND_SWORD)), "a diamond sword counted as a SlashBlade");
        h.succeed();
    }

    /** A replicator without the pattern refuses a build order and keeps no order. */
    @GameTest(template = "empty", batch = "round184_data")
    public static void replicatorRefusesOrdersWithoutPattern(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_replicator_elc")));
        var be = h.getLevel().getBlockEntity(h.absolutePos(at));
        if (be == null || !Round33GameTests.common("blockentity.producer.mio_icif_replicator_elc").isInstance(be)) {
            h.fail("replicator block entity missing: " + be);
            return;
        }
        ItemStack stone = new ItemStack(Items.STONE);
        h.assertTrue(!(Boolean) Round33GameTests.call(be, "canTakeOrder", stone), "accepted an order without a pattern");
        h.assertTrue(!(Boolean) Round33GameTests.call(be, "placeOrder", stone, 8), "placed an order without a pattern");
        h.assertTrue((Integer) Round33GameTests.call(be, "orderLeft", Items.STONE) == 0, "an order was kept");
        h.succeed();
    }
}
