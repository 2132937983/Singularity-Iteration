package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class LiveCableColorGameTests {
    @GameTest(template = "reactor", batch = "live_cable_dye", timeoutTicks = 180)
    public static void dyeDisconnectsPoweredInsulatedCableAndReconnects(GameTestHelper h) { circuit(h,"block_tin_cable_1"); }
    @GameTest(template = "reactor", batch = "live_glass_dye", timeoutTicks = 180)
    public static void dyeDisconnectsPoweredGlassCableAndReconnects(GameTestHelper h) { circuit(h,"block_glass_cable"); }

    private static void circuit(GameTestHelper h, String cable) {
        var sourcePos = new BlockPos(1,2,2); var first = sourcePos.east(); var second = first.east(); var sinkPos = second.east();
        h.setBlock(sourcePos,CoreExampleMod.SOURCE.get()); h.setBlock(sinkPos,CoreExampleMod.SINK.get());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mio_icif:wiring/cable/" + cable));
        h.setBlock(first,block); h.setBlock(second,block);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        paint(h,player,first,DyeColor.RED); paint(h,player,second,DyeColor.RED);
        InheritedSource source = h.getBlockEntity(sourcePos); InheritedSink sink = h.getBlockEntity(sinkPos);
        int[] tick = {0};
        h.onEachTick(() -> {
            int now = ++tick[0];
            if (now > 20 && now <= 35 || now > 90 && now <= 105 || now > 130 && now <= 150)
                h.assertTrue(sink.stored() == 32,"Same/wildcard cable color stopped delivery at tick " + now);
            if (now == 35) {
                paint(h,player,second,DyeColor.BLUE);
                connections(h,first,second,false);
            }
            if (now > 36 && now <= 70) h.assertTrue(sink.stored() == 0,"Differently colored live wires continued to conduct");
            if (now == 70) { paint(h,player,second,DyeColor.RED); connections(h,first,second,true); }
            if (now == 105) {
                paint(h,player,second,DyeColor.BLUE); connections(h,first,second,false);
                paint(h,player,first,DyeColor.BLACK); connections(h,first,second,true);
            }
            if (now <= 150) { source.generate(32); sink.getEnergyStorageInternal().setStored(0); }
            if (now == 150) for (var p : List.of(sourcePos,first,second,sinkPos)) h.setBlock(p,Blocks.AIR);
            if (now == 170) h.succeed();
        });
    }
    private static void connections(GameTestHelper h, BlockPos first, BlockPos second, boolean expected) {
        var a = h.getBlockState(first); var b = h.getBlockState(second);
        h.assertTrue(a.getValue((BooleanProperty)a.getBlock().getStateDefinition().getProperty("east")) == expected
                && b.getValue((BooleanProperty)b.getBlock().getStateDefinition().getProperty("west")) == expected,
            "Both cable arms must refresh during the paint interaction");
    }
    private static void paint(GameTestHelper h, Player player, BlockPos pos, DyeColor color) {
        var item = switch (color) { case RED -> Items.RED_DYE; case BLUE -> Items.BLUE_DYE; default -> Items.BLACK_DYE; };
        var stack = new ItemStack(item,2); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var absolute = h.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);
        h.getBlockState(pos).useItemOn(stack,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(stack.getCount() == 1,"Dye was not consumed exactly once");
        h.assertTrue(((IColoredEnergyTile)h.getBlockEntity(pos)).getEnergyColor(Direction.UP) == (color == DyeColor.BLACK ? null : color),
            "Paint interaction did not change the electrical color");
        h.assertTrue(h.getBlockEntity(pos).getUpdateTag(h.getLevel().registryAccess()).getInt("CableColor") == color.getId(),
            "Update packet omitted the new color");
    }
}
