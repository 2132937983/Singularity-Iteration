package com.miophas.singularity_iteration.common.client.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * <p><b>EN:</b> Client-side view of a machine's running state, read from the block state that the
 * server already syncs ({@code active}, {@code lit}, {@code working} or {@code running}). The
 * property lookup runs once per block and is cached, so a check is one map lookup and one state
 * read: cheap enough for each frame and each tick.
 * <p><b>ZH:</b> 客户端对机器运行状态的视图，直接读取服务器已同步的方块状态（{@code active}、{@code lit}、{@code working} 或 {@code running}）。
 * 属性查找每个方块只做一次并缓存，因此一次检查只是一次映射查找加一次状态读取，足以每帧、每 tick 使用。
 * <p><b>JA:</b> 機械の稼働状態のクライアント側ビュー。サーバーが同期済みのブロックステート（{@code active}、{@code lit}、{@code working}、{@code running}）を読む。
 * プロパティの検索はブロックごとに 1 回でキャッシュするので、1 回の判定はマップ参照 1 回と状態読み取り 1 回で済み、毎フレーム・毎 tick 使える。
 */
@OnlyIn(Dist.CLIENT)
public final class MachineRunState {
    private static final Map<Block, BooleanProperty> RUNNING = new IdentityHashMap<>();
    private static final Map<Block, DirectionProperty> FACING = new IdentityHashMap<>();
    private static final BooleanProperty NONE = BooleanProperty.create("si_none");
    private static final DirectionProperty NO_FACING = DirectionProperty.create("si_none_facing");

    /** Animation and sound cycle length shared by parts, lamps and loops (2 s). */
    public static final int CYCLE_TICKS = 40;

    private MachineRunState() {}

    public static boolean isRunning(BlockState state) {
        BooleanProperty p = RUNNING.computeIfAbsent(state.getBlock(), MachineRunState::findRunning);
        return p != NONE && state.getValue(p);
    }

    public static boolean hasRunState(BlockState state) {
        return RUNNING.computeIfAbsent(state.getBlock(), MachineRunState::findRunning) != NONE;
    }

    /** Horizontal facing (north when the block has none). */
    public static Direction facing(BlockState state) {
        DirectionProperty p = FACING.computeIfAbsent(state.getBlock(), MachineRunState::findFacing);
        if (p == NO_FACING) return Direction.NORTH;
        Direction d = state.getValue(p);
        return d.getAxis().isHorizontal() ? d : Direction.NORTH;
    }

    /** Per-machine phase offset in ticks, so neighbouring machines do not move in lock-step. */
    public static int phaseOffset(BlockPos pos) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 29);
        return (int) Math.floorMod(h, CYCLE_TICKS);
    }

    /**
     * <p><b>EN:</b> Run-state property: "active" first, then "lit", "working" or "running" (0.1.8.1
     * adds the last two; the Crop Harvester and the Cropmatron use "working"). The same names
     * as {@code AbstractEnergyBlockEntity#runFlag()}.
     * <p><b>ZH:</b> 运行状态属性：优先 "active"，其次 "lit"、"working" 或 "running"（0.1.8.1 新增后两者；作物收割机与作物监护机使用 "working"）。
     * 与 {@code AbstractEnergyBlockEntity#runFlag()} 使用相同的名称。
     * <p><b>JA:</b> 稼働状態プロパティ：まず "active"、次に "lit"・"working"・"running"（後の 2 つは 0.1.8.1 で追加。
     * 作物収穫機と作物管理機は "working" を使う）。{@code AbstractEnergyBlockEntity#runFlag()} と同じ名前。
     */
    private static BooleanProperty findRunning(Block block) {
        BooleanProperty other = null;
        for (Property<?> p : block.getStateDefinition().getProperties()) {
            if (p instanceof BooleanProperty bp) {
                String name = p.getName();
                if (name.equals("active")) return bp;
                if (other == null && (name.equals("lit") || name.equals("working") || name.equals("running"))) other = bp;
            }
        }
        return other != null ? other : NONE;
    }

    private static DirectionProperty findFacing(Block block) {
        for (Property<?> p : block.getStateDefinition().getProperties()) {
            if (p instanceof DirectionProperty dp && p.getName().equals("facing")) return dp;
        }
        return NO_FACING;
    }
}
