package com.singularity_iteration.mio_icif.Blocks.entity;

import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.*;
import com.singularity_iteration.mio_icif.integration.ae2.AE2Compat;
import com.singularity_iteration.mio_icif.integration.ae2.Ae2EnergySink;
import com.singularity_iteration.mio_icif.integration.mi.EnergyBridge;
import com.singularity_iteration.mio_icif.mio_icif_sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire block entity implementing IC2-style IEnergyConductor.
 * Registers to the energy grid via EnergyTileLoadEvent/EnergyTileUnloadEvent.
 * Uses EnergyBridge from the integration layer to push energy to adjacent
 * non-IEnergyTile storages (e.g. Modern Industrialization machines).
 *
 * <h3>电线参数说明</h3>
 * <p>所有电气参数均从 {@link CableTier} 读取，在构造时一次性确定并存储在 final 字段中。</p>
 * <p><b>修改电线参数只需修改 {@link CableTier} 中的实例定义即可。</b></p>
 * <ul>
 *   <li>绝缘电线：将 {@code insulated} 设为 {@code true}，触电伤害自动为 0</li>
 *   <li>自定义参数：使用全参数构造函数直接传入自定义值</li>
 * </ul>
 */
@SuppressWarnings("null")
public class mio_icif_wire extends mio_icif_Energy_Block implements IEnergyConductor {

    // ============ 字段 ============

    private final CableTier cableTier;
    /** 是否为绝缘电线。绝缘电线本质上就是没有触电伤害的电线 */
    protected final boolean insulated;

    /** 导体熔毁能量阈值（超过此值导体本身被摧毁） */
    private final double conductorBreakdownEnergy;
    /** 绝缘层熔毁能量阈值（超过此值绝缘层被剥离） */
    private final double insulationBreakdownEnergy;
    /** 绝缘层能量吸收阈值（低于此值绝缘层完全吸收，高于此值产生触电伤害） */
    private final double insulationEnergyAbsorption;
    /** 传导损耗（每包能量传输损失的比例） */
    private final double conductionLoss;
    /** 触电伤害值（绝缘电线为 0） */
    private final float electricDamage;

    // ============ Tick 相关常量 ============

    private static final int CONDUCTIVITY_RANGE = 32;
    private static final int ELECTRIC_CHECK_INTERVAL = 10;
    private static final int POWERED_DURATION = 20;
    private static final int FE_COMPAT_SCAN_INTERVAL = 20; // 每20tick扫描一次FE兼容机器

    private int ticksUntilElectricCheck = 0;
    private int ticksUntilFECompatScan = 0;
    protected int poweredTicksRemaining = 0;

    private final EnergyBridge energyBridge = new EnergyBridge();
    private final EnumSet<Direction> blockedDirections = EnumSet.noneOf(Direction.class);

    private static final String TAG_DISGUISED_BLOCK = "DisguisedBlockId";
    private ResourceLocation disguisedBlockId = null;

    public ResourceLocation getDisguisedBlockId() {
        return disguisedBlockId;
    }

    public void setDisguisedBlockId(ResourceLocation blockId) {
        this.disguisedBlockId = blockId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearDisguise() {
        this.disguisedBlockId = null;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasDisguise() {
        return disguisedBlockId != null;
    }

    public Block getDisguisedBlock() {
        if (disguisedBlockId == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(disguisedBlockId);
        if (block == Blocks.AIR) {
            return null;
        }
        return block;
    }

    /** 相邻FE机器的电网代理，key为电线指向邻居的方向 */
    private final Map<Direction, FECompatTile> feCompatTiles = new HashMap<>();
    /** 相邻AE2能源接收器的电网代理，key为电线指向邻居的方向 */
    private final Map<Direction, Ae2EnergySink> ae2Sinks = new HashMap<>();

    // ============ 类型注册表 ============

    /** 普通电线注册表：CableTier → BlockEntityType */
    private static final Map<CableTier, BlockEntityType<?>> TYPE_REGISTRY = new HashMap<>();
    /** 绝缘电线注册表（与普通电线分开，避免键冲突） */
    private static final Map<CableTier, BlockEntityType<?>> ISOLATION_TYPE_REGISTRY = new HashMap<>();

    // ============ 注册方法 ============

    /**
     * 注册电线方块实体类型（在 BlockEntityType 构建时由注册代码调用）
     */
    public static void registerWireType(CableTier tier, BlockEntityType<?> type) {
        TYPE_REGISTRY.put(tier, type);
    }

    /**
     * 注册绝缘电线方块实体类型（在 BlockEntityType 构建时由注册代码调用）
     */
    public static void registerIsolationWireType(CableTier tier, BlockEntityType<?> type) {
        ISOLATION_TYPE_REGISTRY.put(tier, type);
    }

    /** 内部查找 type：优先查普通表，再查绝缘表 */
    protected static BlockEntityType<?> lookupWireType(CableTier tier) {
        BlockEntityType<?> type = TYPE_REGISTRY.get(tier);
        if (type != null) return type;
        return ISOLATION_TYPE_REGISTRY.get(tier);
    }

    /** 绝缘电线查找 type：优先查绝缘表，再查普通表 */
    protected static BlockEntityType<?> lookupIsolationWireType(CableTier tier) {
        BlockEntityType<?> type = ISOLATION_TYPE_REGISTRY.get(tier);
        if (type != null) return type;
        return TYPE_REGISTRY.get(tier);
    }

    // ============ 构造函数 ============

    /**
     * 便捷构造器：创建普通（非绝缘）电线，使用默认电气参数
     */
    public mio_icif_wire(BlockPos pos, BlockState state, CableTier cableTier) {
        this(pos, state, lookupWireType(cableTier), cableTier, false);
    }

    /**
     * 带明确 type 的内部构造器（供子类及方块类使用）
     */
    public mio_icif_wire(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier) {
        this(pos, state, type, cableTier, false);
    }

    /**
     * 便捷构造器：根据电压等级与绝缘标记创建电线，自动查找对应的 BlockEntityType
     *
     * @param insulated 是否为绝缘电线（绝缘电线触电伤害为 0，绝缘吸收阈值更高，传导损耗更低）
     */
    public mio_icif_wire(BlockPos pos, BlockState state, CableTier cableTier, boolean insulated) {
        this(pos, state, insulated ? lookupIsolationWireType(cableTier) : lookupWireType(cableTier), cableTier, insulated);
    }

    /**
     * 标准构造器：根据电压等级与绝缘标记自动计算所有电气参数
     *
     * @param insulated 是否为绝缘电线（绝缘电线触电伤害为 0，绝缘吸收阈值更高，传导损耗更低）
     */
    protected mio_icif_wire(BlockPos pos, BlockState state, BlockEntityType<?> type, CableTier cableTier, boolean insulated) {
        this(pos, state, type, cableTier, insulated,
             -1, -1, -1, -1, -1);
    }

    /**
     * 全参数构造器：允许子类完全自定义所有电气参数
     *
     * <p>传入 {@code -1} 表示使用基于电压等级的默认值。子类可以传入任意正值来自定义。</p>
     *
     * @param pos                    方块位置
     * @param state                  方块状态
     * @param type                   方块实体类型
     * @param cableTier              电压等级
     * @param insulated              是否绝缘（绝缘电线触电伤害自动为 0）
     * @param conductorBreakdownEnergy   导体熔毁能量（-1 = 从 CableTier 读取）
     * @param insulationBreakdownEnergy  绝缘层熔毁能量（-1 = 从 CableTier 读取）
     * @param insulationEnergyAbsorption 绝缘吸收阈值（-1 = 从 CableTier 读取）
     * @param conductionLoss         传导损耗（-1 = 从 CableTier 读取）
     * @param electricDamage         触电伤害（-1 = 根据 insulated 自动决定）
     */
    protected mio_icif_wire(BlockPos pos, BlockState state, BlockEntityType<?> type,
                           CableTier cableTier, boolean insulated,
                           double conductorBreakdownEnergy,
                           double insulationBreakdownEnergy,
                           double insulationEnergyAbsorption,
                           double conductionLoss,
                           float electricDamage) {
        super(pos, state, type, 0, 0, 0, cableTier);
        this.cableTier = cableTier;
        this.insulated = insulated;

        // 使用传入值或从 CableTier 读取
        this.conductorBreakdownEnergy = conductorBreakdownEnergy >= 0
            ? conductorBreakdownEnergy
            : cableTier.conductorBreakdownEnergy;
        this.insulationBreakdownEnergy = insulationBreakdownEnergy >= 0
            ? insulationBreakdownEnergy
            : cableTier.insulationBreakdownEnergy;
        this.insulationEnergyAbsorption = insulationEnergyAbsorption >= 0
            ? insulationEnergyAbsorption
            : (insulated ? cableTier.insulatedInsulationEnergyAbsorption : cableTier.insulationEnergyAbsorption);
        this.conductionLoss = conductionLoss >= 0
            ? conductionLoss
            : (insulated ? cableTier.insulatedConductionLoss : cableTier.conductionLoss);
        this.electricDamage = electricDamage >= 0
            ? electricDamage
            : (insulated ? 0.0f : cableTier.electricDamage);
    }

    // ============ IEnergyConductor 接口实现 ============

    @Override
    public double getConductionLoss() {
        return conductionLoss;
    }

    @Override
    public double getInsulationEnergyAbsorption() {
        return insulationEnergyAbsorption;
    }

    @Override
    public double getInsulationBreakdownEnergy() {
        return insulationBreakdownEnergy;
    }

    @Override
    public double getConductorBreakdownEnergy() {
        return conductorBreakdownEnergy;
    }

    /**
     * 获取触电伤害值。绝缘电线始终返回 0。
     */
    protected float getElectricDamage() {
        return electricDamage;
    }

    // ============ 过载处理 ============

    @Override
    public void removeInsulation() {
        if (level != null && !level.isClientSide) {
            spawnBurnoutEffects(level, worldPosition);
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    public void removeConductor() {
        if (level != null && !level.isClientSide) {
            spawnBurnoutEffects(level, worldPosition);
            level.destroyBlock(worldPosition, false);
        }
    }

    private void spawnBurnoutEffects(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 30, 0.1, 0.1, 0.1, 0.05);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 15, 0.15, 0.15, 0.15, 0.03);
        }

        level.playSound(null, x, y, z, mio_icif_sounds.CABLE_BREAK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ============ IEnergyEmitter / IEnergyAcceptor ============

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return !blockedDirections.contains(direction);
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return !blockedDirections.contains(direction);
    }

    // ============ IEnergySink override for bridge ============

    @Override
    public double getDemandedEnergy() {
        if (!energyBridge.hasAdjacentCompatSinks()) return 0.0D;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        if (spaceAvailable <= 0) return 0.0D;
        return spaceAvailable;
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (!energyBridge.hasAdjacentCompatSinks()) return amount;
        long spaceAvailable = getEffectiveCapacity() - energyStorage.getAmount();
        long accepted = Math.min((long) amount, spaceAvailable);
        energyStorage.setEnergy(energyStorage.getAmount() + accepted);
        return amount - accepted;
    }

    // ============ Server Tick ============

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wire blockEntity) {
        if (level.isClientSide) return;

        if (blockEntity.poweredTicksRemaining > 0) {
            blockEntity.poweredTicksRemaining--;
        }

        if (state.getBlock() instanceof mio_icif_block_wire) {
            blockEntity.ticksUntilElectricCheck--;
            if (blockEntity.ticksUntilElectricCheck <= 0) {
                blockEntity.ticksUntilElectricCheck = ELECTRIC_CHECK_INTERVAL;
                blockEntity.checkElectricDamage(level, pos, state);
            }
        }

        blockEntity.energyBridge.tick(level, pos, blockEntity.cableTier, blockEntity.energyStorage);

        blockEntity.ticksUntilFECompatScan--;
        if (blockEntity.ticksUntilFECompatScan <= 0) {
            blockEntity.ticksUntilFECompatScan = FE_COMPAT_SCAN_INTERVAL;
            blockEntity.updateFECompatTiles(level, pos);
        }
    }

    public void setPowered() {
        this.poweredTicksRemaining = POWERED_DURATION;
    }

    @Override
    public void onEnergyPass() {
        setPowered();
    }

    public boolean isPowered() {
        return this.poweredTicksRemaining > 0;
    }

    protected void checkElectricDamage(Level level, BlockPos pos, BlockState state) {
        if (!isPowered()) return;

        float damage = getElectricDamage();
        if (damage <= 0) return;

        if (state.getValue(mio_icif_block_wire.WATERLOGGED)) {
            Holder<DamageType> electricDamageType = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
            DamageSource electricSource = new DamageSource(electricDamageType);

            AABB rangeBox = new AABB(
                pos.getX() - CONDUCTIVITY_RANGE, pos.getY() - CONDUCTIVITY_RANGE, pos.getZ() - CONDUCTIVITY_RANGE,
                pos.getX() + CONDUCTIVITY_RANGE, pos.getY() + CONDUCTIVITY_RANGE, pos.getZ() + CONDUCTIVITY_RANGE
            );
            List<Entity> entitiesInRange = level.getEntities(null, rangeBox);

            for (Entity targetEntity : entitiesInRange) {
                if (targetEntity instanceof LivingEntity livingEntity && livingEntity.isInWater()) {
                    livingEntity.hurt(electricSource, damage);
                }
            }
        }
    }

    // ============ FE兼容代理管理 ============

    /**
     * 扫描相邻方块，管理FE机器的电网代理tile。
     * 每tick调用，但只在有变化时才注册/注销。
     */
    private void updateFECompatTiles(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (blockedDirections.contains(dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            if (level.getBlockEntity(neighborPos) instanceof IEnergyTile) continue;

            if (AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(level, neighborPos)) {
                Ae2EnergySink ae2Sink = ae2Sinks.get(dir);
                if (ae2Sink == null) {
                    ae2Sink = new Ae2EnergySink(level, neighborPos);
                    ae2Sinks.put(dir, ae2Sink);
                    EnergyNetGlobal.addTile(ae2Sink, level, neighborPos);
                    ae2Sink.setRegistered(true);
                } else if (!ae2Sink.isValid()) {
                    ae2Sinks.remove(dir);
                    if (ae2Sink.isRegistered()) {
                        EnergyNetGlobal.removeTile(ae2Sink);
                        ae2Sink.setRegistered(false);
                    }
                }
                feCompatTiles.remove(dir);
                continue;
            }

            ae2Sinks.remove(dir);

            boolean isFEMachine = EnergyBridge.hasCompatEnergyStorage(level, neighborPos, dir);
            if (isFEMachine) {
                FECompatTile compat = feCompatTiles.get(dir);
                if (compat == null) {
                    compat = new FECompatTile(level, neighborPos, dir);
                    feCompatTiles.put(dir, compat);
                    EnergyNetGlobal.addTile(compat, level, neighborPos);
                } else if (!compat.isValid()) {
                    feCompatTiles.remove(dir);
                    if (compat.isRegistered()) {
                        EnergyNetGlobal.removeTile(compat);
                    }
                }
            }
        }

        feCompatTiles.entrySet().removeIf(entry -> {
            FECompatTile compat = entry.getValue();
            if (!compat.isValid()) {
                if (compat.isRegistered()) {
                    EnergyNetGlobal.removeTile(compat);
                }
                return true;
            }
            return false;
        });

        ae2Sinks.entrySet().removeIf(entry -> {
            Ae2EnergySink sink = entry.getValue();
            if (!sink.isValid()) {
                if (sink.isRegistered()) {
                    EnergyNetGlobal.removeTile(sink);
                    sink.setRegistered(false);
                }
                return true;
            }
            return false;
        });
    }

    // ============ 方向控制 ============

    public CableTier getCableTier() {
        return cableTier;
    }

    public boolean isDirectionBlocked(Direction dir) {
        return blockedDirections.contains(dir);
    }

    public void blockDirection(Direction dir) {
        if (blockedDirections.add(dir)) {
            setChanged();
            refreshRegistration();
        }
    }

    public void unblockDirection(Direction dir) {
        if (blockedDirections.remove(dir)) {
            setChanged();
            refreshRegistration();
        }
    }

    // ============ NBT ============

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        blockedDirections.clear();
        if (tag.contains("BlockedDirections", Tag.TAG_INT)) {
            int bits = tag.getInt("BlockedDirections");
            for (Direction dir : Direction.values()) {
                if ((bits & (1 << dir.get3DDataValue())) != 0) {
                    blockedDirections.add(dir);
                }
            }
        }
        if (tag.contains(TAG_DISGUISED_BLOCK)) {
            String idStr = tag.getString(TAG_DISGUISED_BLOCK);
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            this.disguisedBlockId = id;
        } else {
            this.disguisedBlockId = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int bits = 0;
        for (Direction dir : blockedDirections) {
            bits |= (1 << dir.get3DDataValue());
        }
        tag.putInt("BlockedDirections", bits);
        if (disguisedBlockId != null) {
            tag.putString(TAG_DISGUISED_BLOCK, disguisedBlockId.toString());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (disguisedBlockId != null) {
            tag.putString(TAG_DISGUISED_BLOCK, disguisedBlockId.toString());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains(TAG_DISGUISED_BLOCK)) {
            String idStr = tag.getString(TAG_DISGUISED_BLOCK);
            this.disguisedBlockId = ResourceLocation.tryParse(idStr);
        } else {
            this.disguisedBlockId = null;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}