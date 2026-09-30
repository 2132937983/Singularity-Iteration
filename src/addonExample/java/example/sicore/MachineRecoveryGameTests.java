package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Wrench recovery of machines: the machine drops itself and keeps its stored energy. */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class MachineRecoveryGameTests {
    private static final TagKey<Block> MACHINE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "machine"));

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", path)); }

    @GameTest(template = "reactor_loop", batch = "recovery", timeoutTicks = 100)
    public static void particleAggregationGeneratorKeepsEnergyWhenWrenched(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 3, 3);
        Block polymerizer = block("producer/block_neutron_polymerizer");
        h.assertTrue(polymerizer.defaultBlockState().is(MACHINE), "Particle aggregation generator must be a c:machine");
        h.setBlock(pos, polymerizer);
        var machine = (AbstractEnergyBlockEntity) h.getBlockEntity(pos);
        machine.getEnergyStorageInternal().setEnergy(1234);

        ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "item_tool_wrench")));
        h.assertTrue(wrench.is(Tags.Items.TOOLS_WRENCH), "SI wrench must carry c:tools/wrench");
        var state = h.getLevel().getBlockState(h.absolutePos(pos));
        var drops = Block.getDrops(state, h.getLevel(), h.absolutePos(pos), machine, null, wrench);
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(polymerizer.asItem()), "Wrench must return the machine itself, got " + drops);
        var data = drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA);
        h.assertTrue(data != null && data.copyTag().getLong("energy") == 1234, "Wrenched machine lost its stored energy");

        var plain = Block.getDrops(state, h.getLevel(), h.absolutePos(pos), machine, null, new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(plain.size() == 1 && !plain.getFirst().is(polymerizer.asItem()), "Without a wrench a machine yields its hull");
        h.succeed();
    }
}
