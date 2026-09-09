package com.singularity_iteration.mio_icif.world.feature;

import com.singularity_iteration.mio_icif.Blocks.Environment.BlockRubberWood;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.List;

@SuppressWarnings("null")
public class RubberWoodDecorator extends TreeDecorator {
    public static final MapCodec<RubberWoodDecorator> CODEC = Codec.INT
        .fieldOf("min_count")
        .xmap(RubberWoodDecorator::new, d -> d.minCount);

    private final int minCount;

    public RubberWoodDecorator(int minCount) {
        this.minCount = minCount;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return mio_icif_tree_decorators.RUBBER_WOOD.get();
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        List<BlockPos> logs = context.logs();

        if (logs.isEmpty()) {
            return;
        }

        int minY = logs.get(0).getY() + 2;
        int maxY = logs.get(logs.size() - 1).getY() - 1;

        if (minY >= maxY) {
            return;
        }

        int range = Math.max(1, 4 - minCount);
        int targetCount = minCount + random.nextInt(range);
        int placed = 0;

        List<BlockPos> shuffledLogs = new java.util.ArrayList<>(logs);
        java.util.Collections.shuffle(shuffledLogs, new java.util.Random(random.nextLong()));

        for (BlockPos logPos : shuffledLogs) {
            if (placed >= targetCount) {
                break;
            }

            int y = logPos.getY();
            if (y < minY || y > maxY) {
                continue;
            }

            Direction[] directions = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
            Direction direction = directions[random.nextInt(directions.length)];

            context.setBlock(
                logPos,
                mio_icif_blocks.BLOCK_RUBBER_TREE.get()
                    .defaultBlockState()
                    .setValue(BlockRubberWood.HAS_SPOT, true)
                    .setValue(BlockRubberWood.HAS_HARZ, true)
                    .setValue(BlockRubberWood.FACING, direction)
            );
            placed++;
        }
    }
}