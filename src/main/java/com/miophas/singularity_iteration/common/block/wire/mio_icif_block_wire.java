package com.miophas.singularity_iteration.common.block.wire;

import com.miophas.singularity_iteration.core.api.energy.ICableBlock;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.common.block.build.mio_icif_block_foam;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import com.miophas.singularity_iteration.common.block.mio_icif_entity_block;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.WaterEntityQueryCache;
import com.miophas.singularity_iteration.core.api.energy.grid.*;
import com.miophas.singularity_iteration.core.runtime.energy.grid.*;
import com.miophas.singularity_iteration.common.integration.ae2.AE2Compat;
import com.miophas.singularity_iteration.common.integration.mi.MICompat;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Queue;
import org.jetbrains.annotations.Nullable;

/**
 * 电线方块
 * 用于传输能量，可以向所有方向传�?
 * 支持含水
 * 含水电线会对接触的实体造成电击伤害
 * 支持不同电压等级（LV, MV, HV, EV, IVIV）
 */
@SuppressWarnings("null")
public class mio_icif_block_wire extends mio_icif_entity_block implements SimpleWaterloggedBlock, ICableBlock {

    private static final Direction[] WATER_DIRECTIONS = Direction.values();

    // 导电范围（方块半径）
    @SuppressWarnings("unused")
    private static final int CONDUCTIVITY_RANGE = 32;

    /**
     * 电线连接调试日志开关，默认关闭。
     * 之前每次 neighborChanged 都会对 6 个方向各打一条 WARN，导致日志被同一坐标刷屏。
     * 需要排查时用 JVM 参数打开：-Dsingularity_iteration.wireConnectDebug=true
     */
    private static final boolean WIRE_CONNECT_DEBUG =
        Boolean.parseBoolean(System.getProperty("singularity_iteration.wireConnectDebug", "false"));

    /** 调试日志去重缓存（仅在开关打开时写入），避免同一个坐标+方向反复刷屏 */
    private static final java.util.Set<String> WIRE_CONNECT_DEBUG_ONCE =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private static void logWireConnect(String reason, BlockPos pos, @Nullable Direction side,
                                       @Nullable BlockState state, @Nullable BlockEntity blockEntity) {
        if (!WIRE_CONNECT_DEBUG) return;
        String key = reason + "|" + pos + "|" + side;
        if (WIRE_CONNECT_DEBUG_ONCE.size() > 512) {
            WIRE_CONNECT_DEBUG_ONCE.clear();
        }
        if (!WIRE_CONNECT_DEBUG_ONCE.add(key)) return;
        Singularity_Iteration.LOGGER.warn(
            "[WireConnect-DEBUG] 电线连接受理 {} (side={}) | 原因: {} | 方块={} BE={}",
            pos, side, reason,
            state != null ? state.getBlock() : "null",
            blockEntity != null ? blockEntity.getClass().getName() : "null"
        );
    }

    /**
     * 获取电击伤害值，根据电缆等级递增
     * LV: 4点（2颗心）
     * MV: 8点（4颗心）
     * HV: 18点（9颗心）
     * EV: 40点（20颗心）
     * IV: 0点（无伤害）- IV电线是玻璃电缆，绝缘性好
     * LuV: 0点（无伤害）- 超导合金电缆，完全绝缘
     * @return 伤害值
     */
    public float getElectricDamage() {
        return cableTier.getElectricDamage();
    }
    
    public static final MapCodec<mio_icif_block_wire> CODEC = simpleCodec(mio_icif_block_wire::new);
    
    // 连接状态属性?
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    // 含水属性?
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    // 含泡沫属性?
    public static final BooleanProperty FOAMLOGGED = BooleanProperty.create("foamlogged");
    public static final BooleanProperty FOAM_REINFORCED = BooleanProperty.create("foam_reinforced");
    public static final BooleanProperty DISGUISED = BooleanProperty.create("disguised");
    /**
     * Foam around the cable has set: reinforced foam becomes reinforced stone (blast proof), plain
     * foam a CF wall - with the cable still inside and conducting. FOAM_REINFORCED without
     * FOAMLOGGED marks a cable laid through an iron scaffold, which foams into reinforced foam.
     */
    public static final BooleanProperty FOAM_HARDENED = BooleanProperty.create("foam_hardened");
    
    // 基础中心�?4x4x4
    protected static final VoxelShape CENTER_SHAPE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    
    // 各方向的连接形状 (延伸6个像�?
    protected static final VoxelShape NORTH_SHAPE = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 6.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(6.0, 6.0, 10.0, 10.0, 10.0, 16.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(10.0, 6.0, 6.0, 16.0, 10.0, 10.0);
    protected static final VoxelShape WEST_SHAPE = Block.box(0.0, 6.0, 6.0, 6.0, 10.0, 10.0);
    protected static final VoxelShape UP_SHAPE = Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0);
    protected static final VoxelShape DOWN_SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);

    private static final int NORTH_SHAPE_BIT = 1;
    private static final int SOUTH_SHAPE_BIT = 1 << 1;
    private static final int EAST_SHAPE_BIT = 1 << 2;
    private static final int WEST_SHAPE_BIT = 1 << 3;
    private static final int UP_SHAPE_BIT = 1 << 4;
    private static final int DOWN_SHAPE_BIT = 1 << 5;
    private static final VoxelShape[] CONNECTION_SHAPES = createConnectionShapes();
    
    // 电缆等级
    protected final CableTier cableTier;
    /** 是否为绝缘电线 */
    protected final boolean insulated;
    private final int insulationLayers;
    private static final java.util.Map<CableTier, java.util.Map<Integer, mio_icif_block_wire>> LAYERS = new java.util.HashMap<>();
    private mio_icif_block_wire bareCounterpart;
    private mio_icif_block_wire insulatedCounterpart;
    
    /**
     * 默认构造函数（LV等级�?
     */
    public mio_icif_block_wire(Properties properties) {
        this(properties, CableTier.LV, false);
    }
    
    /**
     * 带电压等级的构造函数?
     * @param properties 方块属性?
     * @param cableTier 电缆等级
     */
    public mio_icif_block_wire(Properties properties, CableTier cableTier) {
        this(properties, cableTier, false);
    }

    /**
     * 带电压等级和绝缘标记的构造函数
     * @param properties 方块属性
     * @param cableTier 电缆等级
     * @param insulated 是否为绝缘电线（绝缘电线无触电伤害）
     */
    public mio_icif_block_wire(Properties properties, CableTier cableTier, boolean insulated) {
        this(properties, cableTier, insulated ? 1 : 0);
    }

    public mio_icif_block_wire(Properties properties, CableTier cableTier, int layers) {
        super(properties);
        this.cableTier = cableTier;
        this.insulated = layers > 0;
        this.insulationLayers = layers;
        LAYERS.computeIfAbsent(cableTier, ignored -> new java.util.HashMap<>()).put(layers, this);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(WATERLOGGED, false)
            .setValue(FOAMLOGGED, false)
            .setValue(FOAM_REINFORCED, false)
            .setValue(FOAM_HARDENED, false)
            .setValue(DISGUISED, false));
    }

    /**
     * 获取电缆等级
     */
    public CableTier getCableTier() {
        return cableTier;
    }

    /**
     * 是否为绝缘电线
     */
    public boolean isInsulated() {
        return insulated;
    }
    public int getInsulationLayers() { return insulationLayers; }

    public mio_icif_block_wire getBareCounterpart() {
        if (!insulated) return null;
        return LAYERS.getOrDefault(cableTier, java.util.Map.of()).getOrDefault(insulationLayers - 1, bareCounterpart);
    }

    public mio_icif_block_wire getInsulatedCounterpart() {
        return LAYERS.getOrDefault(cableTier, java.util.Map.of()).getOrDefault(insulationLayers + 1, insulatedCounterpart);
    }

    public void setCounterparts(mio_icif_block_wire bare, mio_icif_block_wire insulated) {
        this.bareCounterpart = bare;
        this.insulatedCounterpart = insulated;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED, FOAMLOGGED, FOAM_REINFORCED, FOAM_HARDENED, DISGUISED);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // foam, set foam and the scaffold around the cable fill the block
        if (state.getValue(FOAMLOGGED) || state.getValue(FOAM_REINFORCED)) return Shapes.block();
        return getCombinedShape(state);
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // wet foam has no collision (IC2); set foam and the scaffold are solid
        if (state.getValue(FOAM_HARDENED) || (state.getValue(FOAM_REINFORCED) && !state.getValue(FOAMLOGGED))) return Shapes.block();
        return getCombinedShape(state);
    }

    /** Iron-class scaffold (IC2: sprays into reinforced foam): a cable may be laid through it. */
    public static boolean isFoamScaffold(BlockState state) {
        return state.getBlock() instanceof com.miophas.singularity_iteration.common.block.build.mio_icif_block_scaffold scaffold
            && scaffold.getStrength() >= 3;
    }

    /** A cable is embedded in a set wall (reinforced stone or CF wall). */
    public static boolean isEmbedded(BlockState state) {
        return state.getBlock() instanceof mio_icif_block_wire && state.getValue(FOAMLOGGED) && state.getValue(FOAM_HARDENED);
    }

    private static BlockState wallOf(BlockState state) {
        return state.getValue(FOAM_REINFORCED)
            ? com.miophas.singularity_iteration.common.registry.mio_icif_blocks.CONSTRUCTION_WALL.get().defaultBlockState()
            : com.miophas.singularity_iteration.common.registry.mio_icif_blocks.CONSTRUCTION_FOAM_WALL.get().defaultBlockState();
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (isEmbedded(state)) return wallOf(state).getBlock().getExplosionResistance();
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // an embedded cable is mined like the wall around it
        if (isEmbedded(state)) return wallOf(state).getDestroyProgress(player, level, pos);
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(FOAMLOGGED) && !state.getValue(FOAM_HARDENED);
    }

    /** Foam around a cable sets like a free foam block (IC2 harden time and light formula). */
    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(FOAMLOGGED) || state.getValue(FOAM_HARDENED)) return;
        int tickSpeed = level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING);
        if (tickSpeed <= 0) return;
        int hardenTime = state.getValue(FOAM_REINFORCED) ? mio_icif_block_foam.HARDEN_TIME_REINFORCED : mio_icif_block_foam.HARDEN_TIME_NORMAL;
        float chance = mio_icif_block_foam.getHardenChance(level, pos, state, hardenTime) * 4096.0F / tickSpeed;
        if (random.nextFloat() < chance) hardenFoam(level, pos, state);
    }

    public static void hardenFoam(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof mio_icif_block_wire) || !state.getValue(FOAMLOGGED) || state.getValue(FOAM_HARDENED)) return;
        level.setBlockAndUpdate(pos, state.setValue(FOAM_HARDENED, true));
        level.playSound(null, pos, SoundEvents.SAND_HIT, SoundSource.BLOCKS, 0.5F, 0.5F);
    }
    
    private static VoxelShape getCombinedShape(BlockState state) {
        int connectionMask = 0;
        if (state.getValue(NORTH)) connectionMask |= NORTH_SHAPE_BIT;
        if (state.getValue(SOUTH)) connectionMask |= SOUTH_SHAPE_BIT;
        if (state.getValue(EAST)) connectionMask |= EAST_SHAPE_BIT;
        if (state.getValue(WEST)) connectionMask |= WEST_SHAPE_BIT;
        if (state.getValue(UP)) connectionMask |= UP_SHAPE_BIT;
        if (state.getValue(DOWN)) connectionMask |= DOWN_SHAPE_BIT;
        return CONNECTION_SHAPES[connectionMask];
    }

    private static VoxelShape[] createConnectionShapes() {
        VoxelShape[] shapes = new VoxelShape[1 << 6];
        for (int connectionMask = 0; connectionMask < shapes.length; connectionMask++) {
            VoxelShape shape = CENTER_SHAPE;
            if ((connectionMask & NORTH_SHAPE_BIT) != 0) shape = Shapes.or(shape, NORTH_SHAPE);
            if ((connectionMask & SOUTH_SHAPE_BIT) != 0) shape = Shapes.or(shape, SOUTH_SHAPE);
            if ((connectionMask & EAST_SHAPE_BIT) != 0) shape = Shapes.or(shape, EAST_SHAPE);
            if ((connectionMask & WEST_SHAPE_BIT) != 0) shape = Shapes.or(shape, WEST_SHAPE);
            if ((connectionMask & UP_SHAPE_BIT) != 0) shape = Shapes.or(shape, UP_SHAPE);
            if ((connectionMask & DOWN_SHAPE_BIT) != 0) shape = Shapes.or(shape, DOWN_SHAPE);
            shapes[connectionMask] = shape;
        }
        return shapes;
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        boolean waterlogged = fluidState.getType() == Fluids.WATER;

        BlockState replacedState = level.getBlockState(pos);
        boolean foamlogged = false;
        boolean foamReinforced = false;
        if (replacedState.getBlock() instanceof mio_icif_block_foam) {
            foamlogged = true;
            foamReinforced = replacedState.getValue(mio_icif_block_foam.REINFORCED);
        } else if (isFoamScaffold(replacedState)) {
            foamReinforced = true;             // laid through an iron scaffold: sprays into reinforced foam
        }
        
        return this.defaultBlockState()
            .setValue(NORTH, canConnectTo(level, pos.north(), pos))
            .setValue(SOUTH, canConnectTo(level, pos.south(), pos))
            .setValue(EAST, canConnectTo(level, pos.east(), pos))
            .setValue(WEST, canConnectTo(level, pos.west(), pos))
            .setValue(UP, canConnectTo(level, pos.above(), pos))
            .setValue(DOWN, canConnectTo(level, pos.below(), pos))
            .setValue(WATERLOGGED, waterlogged)
            .setValue(FOAMLOGGED, foamlogged)
            .setValue(FOAM_REINFORCED, foamReinforced);
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED) && direction == Direction.DOWN) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof mio_icif_wire wire && wire.isDirectionBlocked(direction)) {
            return switch (direction) {
                case NORTH -> state.setValue(NORTH, false);
                case SOUTH -> state.setValue(SOUTH, false);
                case EAST -> state.setValue(EAST, false);
                case WEST -> state.setValue(WEST, false);
                case UP -> state.setValue(UP, false);
                case DOWN -> state.setValue(DOWN, false);
            };
        }

        boolean canConnect = canConnectTo(level, neighborPos, pos);
        return switch (direction) {
            case NORTH -> state.setValue(NORTH, canConnect);
            case SOUTH -> state.setValue(SOUTH, canConnect);
            case EAST -> state.setValue(EAST, canConnect);
            case WEST -> state.setValue(WEST, canConnect);
            case UP -> state.setValue(UP, canConnect);
            case DOWN -> state.setValue(DOWN, canConnect);
        };
    }
    
    @SuppressWarnings("unused")
    private boolean canConnectTo(BlockGetter level, BlockPos pos) {
        return canConnectTo(level, pos, null);
    }

    /**
     * 检查是否可以连接到指定位置的方法?
     * @param level 方块获取得?
     * @param pos 目标位置
     * @param fromPos 当前电线位置（用于计算方向）
     * @return 是否可以连接
     */
    private boolean canConnectTo(BlockGetter level, BlockPos pos, @Nullable BlockPos fromPos) {
        if (fromPos != null) {
            Direction side = Direction.fromDelta(pos.getX() - fromPos.getX(), pos.getY() - fromPos.getY(), pos.getZ() - fromPos.getZ());
            if (side != null && !com.miophas.singularity_iteration.core.api.energy.IColoredEnergyTile.connects(
                    level.getBlockEntity(fromPos), level.getBlockEntity(pos), side)) return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof mio_icif_block_wire) {
            if (fromPos != null) {
                Direction dirToTarget = Direction.fromDelta(
                    pos.getX() - fromPos.getX(),
                    pos.getY() - fromPos.getY(),
                    pos.getZ() - fromPos.getZ()
                );
                if (dirToTarget != null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof mio_icif_wire targetWire && targetWire.isDirectionBlocked(dirToTarget.getOpposite())) {
                        return false;
                    }
                }
            }
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        // Independent transformers participate directly in the native grid and
        // deliberately expose no legacy EU capability; they still connect visually.
        if (blockEntity instanceof com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity) {
            return true;
        }
        if (blockEntity instanceof AbstractEnergyBlockEntity || blockEntity instanceof mio_icif_wire) {
            return true;
        }
        if (blockEntity instanceof IEnergyTile) {
            return true;
        }
        if (blockEntity instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity) {
            return true;
        }

        if (level instanceof net.minecraft.world.level.Level realLevel) {
            try {
                Direction side = null;
                if (fromPos != null) {
                    side = Direction.fromDelta(
                        fromPos.getX() - pos.getX(),
                        fromPos.getY() - pos.getY(),
                        fromPos.getZ() - pos.getZ()
                    );
                }

                IEUEnergyStorage euSided = side != null ? realLevel.getCapability(EUApi.SIDED, pos, side) : null;
                IEUEnergyStorage euNull = realLevel.getCapability(EUApi.SIDED, pos, null);

                if (euSided != null || euNull != null) {
                    logWireConnect("EUApi.SIDED 非空", pos, side, state, blockEntity);
                    return true;
                }

                if (MICompat.isMILoaded()) {
                    if (side != null && MICompat.getMIStorage(realLevel, pos, side) != null) {
                        logWireConnect("MI能力 非空", pos, side, state, blockEntity);
                        return true;
                    }
                    if (MICompat.getMIStorage(realLevel, pos, null) != null) {
                        logWireConnect("MI能力(null) 非空", pos, null, state, blockEntity);
                        return true;
                    }
                }

                if (side != null && com.miophas.singularity_iteration.common.integration.gt.GTCompat.findSink(realLevel, pos, side) != null) {
                    logWireConnect("GT能力 非空", pos, side, state, blockEntity);
                    return true;
                }

                // 检查 NeoForge 原生 FE 能力
                net.neoforged.neoforge.energy.IEnergyStorage feSided = side != null
                    ? realLevel.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, side) : null;
                net.neoforged.neoforge.energy.IEnergyStorage feNull = realLevel.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, null);

                if ((feSided != null && feSided.canReceive()) || (feNull != null && feNull.canReceive())) {
                    return true;
                }

                if (AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(realLevel, pos)) {
                    return true;
                }

            } catch (Exception e) {
                if (WIRE_CONNECT_DEBUG) {
                    Singularity_Iteration.LOGGER.error("[WireConnect-DEBUG] 电线连接检查异常 at {}: {}", pos, e.getMessage());
                }
                com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures.report(
                    "wire-connect", "Cable connection check against " + pos, e);
                return false;
            }
        }

        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            boolean northBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.NORTH);
            boolean southBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.SOUTH);
            boolean eastBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.EAST);
            boolean westBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.WEST);
            boolean upBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.UP);
            boolean downBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.DOWN);

            BlockState newState = state
                .setValue(NORTH, !northBlocked && canConnectTo(level, pos.north(), pos))
                .setValue(SOUTH, !southBlocked && canConnectTo(level, pos.south(), pos))
                .setValue(EAST, !eastBlocked && canConnectTo(level, pos.east(), pos))
                .setValue(WEST, !westBlocked && canConnectTo(level, pos.west(), pos))
                .setValue(UP, !upBlocked && canConnectTo(level, pos.above(), pos))
                .setValue(DOWN, !downBlocked && canConnectTo(level, pos.below(), pos));
            if (newState != state) {
                level.setBlock(pos, newState, 3);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            // Grid removal is handled by the BlockEntity's setRemoved() which fires EnergyTileUnloadEvent.
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (stack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof mio_icif_block_foam) {
                return !state.getValue(FOAMLOGGED);
            }
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(net.minecraft.world.item.Items.SAND) && state.getValue(FOAMLOGGED) && !state.getValue(FOAM_HARDENED)) {
            if (!level.isClientSide) {
                hardenFoam(level, pos, state);               // IC2: sand sets foam at once
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() && stack.getItem() instanceof com.miophas.singularity_iteration.core.api.item.ICutterItem
                && getBareCounterpart() != null && level.getBlockEntity(pos) instanceof mio_icif_wire wire) {
            if (!level.isClientSide) {
                wire.changeInsulation(getBareCounterpart());
                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    Block.popResource(level, pos, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.parse("mio_icif:resource/item_rubber"))));
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof net.minecraft.world.item.DyeItem dye
                && level.getBlockEntity(pos) instanceof mio_icif_wire wire && wire.canColor(dye.getDyeColor())) {
            if (!level.isClientSide) {
                wire.setCableColor(dye.getDyeColor());
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("mio_icif:resource/item_rubber")
                && getInsulatedCounterpart() != null && level.getBlockEntity(pos) instanceof mio_icif_wire wire) {
            if (!level.isClientSide) {
                wire.changeInsulation(getInsulatedCounterpart());
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof mio_icif_block_foam foamBlock) {
            if (state.getValue(FOAMLOGGED)) {
                return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                boolean reinforced = foamBlock.defaultBlockState().getValue(mio_icif_block_foam.REINFORCED)
                    || state.getValue(FOAM_REINFORCED);       // a scaffolded cable stays reinforced
                BlockState newState = state
                    .setValue(FOAMLOGGED, true)
                    .setValue(FOAM_REINFORCED, reinforced);
                level.setBlockAndUpdate(pos, newState);
                level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // 对齐 IC2：破坏被建筑泡沫包裹的线缆时，泡沫会被破坏且不可回收，
    // 线缆本体按战利品表正常掉落。此处无需掉落泡沫物品。

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        var drops = new java.util.ArrayList<>(super.getDrops(state, context));
        // the cable's surroundings: the iron scaffold it was laid through (also under wet reinforced
        // foam, as a foam block drops it), or the set wall it is embedded in
        if (state.getValue(FOAM_HARDENED) && state.getValue(FOAMLOGGED)) drops.add(new ItemStack(wallOf(state).getBlock().asItem()));
        else if (state.getValue(FOAM_REINFORCED))
            drops.add(new ItemStack(com.miophas.singularity_iteration.common.registry.mio_icif_blocks.SCAFFOLD_IRON.get().asItem()));
        if (context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof mio_icif_wire wire && wire.getCableColor() != net.minecraft.world.item.DyeColor.BLACK) {
            var data = new net.minecraft.nbt.CompoundTag();
            data.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(wire.getType()).toString());
            data.putInt("CableColor", wire.getCableColor().getId());
            for (var drop : drops) if (drop.is(asItem()))
                drop.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(data));
        }
        return drops;
    }

    public static void tickFoamHardening(Level level, BlockPos pos, BlockState state) {
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(DISGUISED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.controls(state)) {
            super.stepOn(level, pos, state, entity); return;
        }
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            if (insulated) return;
            if (level.getBlockEntity(pos) instanceof mio_icif_wire wireEntity && wireEntity.isPowered()) {
                float damage = getElectricDamage();
                if (damage > 0) {
                    Holder<DamageType> electricDamageType = level.registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
                    DamageSource electricSource = new DamageSource(electricDamageType);
                    livingEntity.hurt(electricSource, damage);
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.controls(state)) {
            super.entityInside(state, level, pos, entity); return;
        }
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            if (insulated) return;
            if (level.getBlockEntity(pos) instanceof mio_icif_wire wireEntity && wireEntity.isPowered()) {
                float damage = getElectricDamage();
                if (damage > 0) {
                    Holder<DamageType> electricDamageType = level.registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
                    DamageSource electricSource = new DamageSource(electricDamageType);
                    if (state.getValue(WATERLOGGED)) {
                        // 含水电线：对连接该水体的所有生物造成伤害
                        damageEntitiesInConnectedWater(level, pos, electricSource);
                    } else {
                        livingEntity.hurt(electricSource, damage);
                    }
                }
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    /**
     * 对连接该水体的所有生物造成触电伤害
     * 使用洪水填充算法找到所有连接的水方块，然后对这些水体中的生物造成伤害
     * @param level 世界
     * @param pos 电线位置
     * @param electricSource 电击伤害�?
     */
    private void damageEntitiesInConnectedWater(Level level, BlockPos pos, DamageSource electricSource) {
        if (!(level.getBlockEntity(pos) instanceof mio_icif_wire wire)
                || !wire.claimWaterDamageTick(level.getGameTime())) {
            return;
        }

        // 仅在同一 Tick 内复用水体拓扑快照，下一 Tick 重新查询，
        // 避免同 Tick 的 entityInside 回调重复遍历最多 1000 个方块。
        java.util.Set<BlockPos> connectedWaterBlocks = wire.getWaterConnectivityCache(level.getGameTime());
        if (connectedWaterBlocks == null) {
            connectedWaterBlocks = findConnectedWaterBlocks(level, pos);
            wire.cacheWaterConnectivity(level.getGameTime(), connectedWaterBlocks);
        }
        
        // 获取半径16格内的所有实�?
        AABB rangeBox = new AABB(
            pos.getX() - 16, pos.getY() - 16, pos.getZ() - 16,
            pos.getX() + 16, pos.getY() + 16, pos.getZ() + 16
        );
        List<LivingEntity> entitiesInRange = WaterEntityQueryCache.query(level, rangeBox);
        
        float damage = getElectricDamage();
        for (LivingEntity targetLiving : entitiesInRange) {
            // 检查生物是否在与电线相同的水体�?
            if (isEntityInConnectedWater(targetLiving, connectedWaterBlocks)) {
                targetLiving.hurt(electricSource, damage);
            }
        }
    }

    /**
     * 使用洪水填充算法找到所有与电线连接的水方块
     * 限制搜索范围在半�?6格内，避免性能问题
     * @param level 世界
     * @param startPos 起始位置（电线位置）
     * @return 连接的水方块集合
     */
    private java.util.Set<BlockPos> findConnectedWaterBlocks(Level level, BlockPos startPos) {
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);
        visited.add(startPos);
        
        int maxSearchRange = 16;
        int maxBlocks = 1000; // 限制最大搜索方块数，避免性能问题
        
        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();
            
            // 检查六个方向的相邻方块
            for (Direction direction : WATER_DIRECTIONS) {
                BlockPos neighbor = current.relative(direction);
                
                // 检查是否在搜索范围�?
                if (Math.abs(neighbor.getX() - startPos.getX()) > maxSearchRange ||
                    Math.abs(neighbor.getY() - startPos.getY()) > maxSearchRange ||
                    Math.abs(neighbor.getZ() - startPos.getZ()) > maxSearchRange) {
                    continue;
                }
                
                // 已经访问�?
                if (visited.contains(neighbor)) {
                    continue;
                }
                
                // 检查是否是水方法?
                if (isWaterBlock(level, neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        return visited;
    }

    /**
     * 检查指定位置是否是水方法?
     * 包括水源、水流、含水方块等
     * @param level 世界
     * @param pos 位置
     * @return 是否是水方块
     */
    private boolean isWaterBlock(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType() == Fluids.WATER) {
            return true;
        }
        
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof SimpleWaterloggedBlock) {
            return blockState.getValue(BlockStateProperties.WATERLOGGED);
        }
        
        return false;
    }

    /**
     * 检查生物是否在与电线连接的水体�?
     * 检查生物所在位置及其周围是否是连接的水方块
     * @param entity 生物
     * @param connectedWaterBlocks 连接的水方块集合
     * @return 是否在连接的水体�?
     */
    private boolean isEntityInConnectedWater(LivingEntity entity, java.util.Set<BlockPos> connectedWaterBlocks) {
        // 获取生物所在位置?
        BlockPos entityPos = entity.blockPosition();
        
        // 检查生物所在位置及其周围是否是连接的水方块
        // 生物可能占据多个方块位置，所以检查一个范围?
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = entityPos.offset(dx, dy, dz);
                    if (connectedWaterBlocks.contains(checkPos)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

@Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_wire(pos, state, cableTier, insulated);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
            (l, p, s, blockEntity) -> {
                if (blockEntity instanceof mio_icif_wire wire) {
                    mio_icif_wire.tick(l, p, s, wire);
                }
            };
    }

    /**
     * 触发电线融毁（当低等级电线连接高等级电线时）
     * @param level 世界
     * @param pos 电线位置
     */
    @SuppressWarnings("unused")
    private void triggerWireBurnout(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof com.miophas.singularity_iteration.core.api.energy.tile.IExplosionPowerOverride override && !override.shouldExplode()) {
            level.destroyBlock(pos, false);
            return;
        }

        float explosionPower = 0.5F;
        if (be instanceof com.miophas.singularity_iteration.core.api.energy.tile.IExplosionPowerOverride override) {
            explosionPower = override.getExplosionPower(0, explosionPower);
        }

        level.destroyBlock(pos, false);

        if (explosionPower > 0.0F) {
            level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                explosionPower,
                Level.ExplosionInteraction.NONE
            );
        }
    }

    public void refreshConnections(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof mio_icif_block_wire)) return;

        BlockEntity be = level.getBlockEntity(pos);
        boolean northBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.NORTH);
        boolean southBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.SOUTH);
        boolean eastBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.EAST);
        boolean westBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.WEST);
        boolean upBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.UP);
        boolean downBlocked = be instanceof mio_icif_wire w && w.isDirectionBlocked(Direction.DOWN);

        BlockState newState = state
            .setValue(NORTH, !northBlocked && canConnectTo(level, pos.north(), pos))
            .setValue(SOUTH, !southBlocked && canConnectTo(level, pos.south(), pos))
            .setValue(EAST, !eastBlocked && canConnectTo(level, pos.east(), pos))
            .setValue(WEST, !westBlocked && canConnectTo(level, pos.west(), pos))
            .setValue(UP, !upBlocked && canConnectTo(level, pos.above(), pos))
            .setValue(DOWN, !downBlocked && canConnectTo(level, pos.below(), pos));
        if (newState != state) {
            level.setBlock(pos, newState, 3);
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            level.updateNeighborsAt(neighborPos, this);
        }
    }

    public static Direction getHitDirection(net.minecraft.world.phys.Vec3 hitVec, BlockPos pos, BlockState state) {
        double lx = hitVec.x - pos.getX();
        double ly = hitVec.y - pos.getY();
        double lz = hitVec.z - pos.getZ();

        boolean inCenterX = lx >= 0.375 && lx <= 0.625;
        boolean inCenterY = ly >= 0.375 && ly <= 0.625;
        boolean inCenterZ = lz >= 0.375 && lz <= 0.625;

        if (inCenterX && inCenterY && inCenterZ) {
            return null;
        }

        if (inCenterX && inCenterY) {
            if (lz < 0.375 && state.getValue(NORTH)) return Direction.NORTH;
            if (lz > 0.625 && state.getValue(SOUTH)) return Direction.SOUTH;
        }
        if (inCenterY && inCenterZ) {
            if (lx < 0.375 && state.getValue(WEST)) return Direction.WEST;
            if (lx > 0.625 && state.getValue(EAST)) return Direction.EAST;
        }
        if (inCenterX && inCenterZ) {
            if (ly < 0.375 && state.getValue(DOWN)) return Direction.DOWN;
            if (ly > 0.625 && state.getValue(UP)) return Direction.UP;
        }

        if (lz < 0.375) return Direction.NORTH;
        if (lz > 0.625) return Direction.SOUTH;
        if (lx < 0.375) return Direction.WEST;
        if (lx > 0.625) return Direction.EAST;
        if (ly < 0.375) return Direction.DOWN;
        if (ly > 0.625) return Direction.UP;

        return null;
    }

    public static boolean isHitInCenter(net.minecraft.world.phys.Vec3 hitVec, BlockPos pos) {
        double lx = hitVec.x - pos.getX();
        double ly = hitVec.y - pos.getY();
        double lz = hitVec.z - pos.getZ();
        return lx >= 0.375 && lx <= 0.625 && ly >= 0.375 && ly <= 0.625 && lz >= 0.375 && lz <= 0.625;
    }
}
