package com.singularity_iteration.mio_icif.Items.Tools;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 镭射枪子弹实体类
 * 继承原版 ThrowableProjectile 投掷物类
 * 击中方块或实体后消失。
 * 支持采矿模式、战斗模式、超级热线模式、爆破模式。 */
@SuppressWarnings("null")
public class mio_icif_laser_bullet extends ThrowableProjectile {

    // 实体数据同步字段 - 是否暴击
    private static final EntityDataAccessor<Boolean> ID_CRIT = SynchedEntityData.defineId(mio_icif_laser_bullet.class, EntityDataSerializers.BOOLEAN);

    // 实体数据同步字段 - 是否为采矿模式（true=采矿模式可以挖矿，false=战斗模式只伤害实体）
    private static final EntityDataAccessor<Boolean> ID_MINING_MODE = SynchedEntityData.defineId(mio_icif_laser_bullet.class, EntityDataSerializers.BOOLEAN);

    // 实体数据同步字段 - 是否为穿透模式（true=穿透方块不消失，false=碰到方块消失）
private static final EntityDataAccessor<Boolean> ID_PENETRATING_MODE = SynchedEntityData.defineId(mio_icif_laser_bullet.class, EntityDataSerializers.BOOLEAN);

    // 实体数据同步字段 - 是否为超级热线模式（true=烧制方块）
private static final EntityDataAccessor<Boolean> ID_SUPER_HEAT_MODE = SynchedEntityData.defineId(mio_icif_laser_bullet.class, EntityDataSerializers.BOOLEAN);

    // 实体数据同步字段 - 是否为爆破模式（true=爆炸效果）
private static final EntityDataAccessor<Boolean> ID_EXPLOSIVE_MODE = SynchedEntityData.defineId(mio_icif_laser_bullet.class, EntityDataSerializers.BOOLEAN);

    // 实体数据同步字段 - 是否为核能模式（true=100倍爆炸威力）
    private static final EntityDataAccessor<Boolean> ID_NUCLEAR_MODE = SynchedEntityData.defineId(mio_icif_laser_bullet.class, EntityDataSerializers.BOOLEAN);

    // 激光子弹飞行速度（IC2原版：1.0 block/tick）
public static final double LASER_SPEED = 1.0;

    // 各模式最大飞行距离（IC2原版用range控制，-1表示无限）
public static final float RANGE_MINING = -1.0f;        // 采矿模式：无限距离
    public static final float RANGE_LOW_FOCUS = 4.0f;      // 低焦模式（近距离模式）
public static final float RANGE_LONG_RANGE = -1.0f;    // 远距模式：无限距离
public static final float RANGE_COMBAT = -1.0f;        // 战斗模式：无限距离
public static final float RANGE_SUPER_HEAT = -1.0f;    // 超级热线模式：无限距离
public static final float RANGE_SCATTER = -1.0f;       // 散射模式：无限距离
public static final float RANGE_EXPLOSIVE = -1.0f;     // 爆破模式：无限距离
public static final float RANGE_3X3 = -1.0f;           // 3x3模式：无限距离?
    // 最大存活tick数（安全上限，防止无限飞行）
    public static final int MAX_LIFE = 200;

    // 激光子弹伤害属性
private float damage = 5.0f;

    // 激光子弹挖掘力（IC2原版：power -= hardness/1.5，power<=0时停止）
    private float power = 5.0f;

    // 激光子弹可破坏方块数（IC2原版：每破坏一个方块blockBreaks--，=0时消失）?=0时消失）
    private int blockBreaks = Integer.MAX_VALUE;

    // 激光子弹剩余飞行距离（IC2原版：每tick range -= 飞行距离，1时消失?1时消失，-1=无限）
private float range = -1.0f;

    // 激光子弹生命周期计数器
private int life = 0;

    // 起始位置（用于计算飞行距离）
    private Vec3 startPos;

 // 本地存的采矿模式（用于客户端）
    private boolean miningMode = true;

 // 本地存的穿透模式（用于客户端）
    private boolean penetratingMode = false;

 // 本地存的超级热线模式（用于客户端）
    private boolean superHeatMode = false;

 // 本地存的爆破模式（用于客户端）
    private boolean explosiveMode = false;

 // 本地存的核能模式（用于客户端）
    private boolean nuclearMode = false;

    /**
     * 默认构造函数（用于实体注册）
 * @param entityType 实体类型
     * @param level 世界
     */
    public mio_icif_laser_bullet(EntityType<? extends mio_icif_laser_bullet> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // 无重力
}

    /**
     * 构造函数（从玩家发射）
     * @param entityType 实体类型
     * @param owner 发射者
 * @param level 世界
     */
    public mio_icif_laser_bullet(EntityType<? extends mio_icif_laser_bullet> entityType, LivingEntity owner, Level level) {
        super(entityType, owner, level);
        this.setNoGravity(true); // 无重力
    this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.startPos = this.position();
    }

    /**
     * 构造函数（指定位置发射）
 * @param entityType 实体类型
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param level 世界
     */
    public mio_icif_laser_bullet(EntityType<? extends mio_icif_laser_bullet> entityType, double x, double y, double z, Level level) {
        super(entityType, x, y, z, level);
        this.setNoGravity(true); // 无重力
    this.startPos = new Vec3(x, y, z);
    }

    /**
     * 设置激光子弹伤害
 * @param damage 伤害值
 */
    public void setDamage(float damage) {
        this.damage = damage;
    }

    /**
     * 获取激光子弹伤害
 * @return 伤害值
 */
    public float getDamage() {
        return damage;
    }

    /**
     * 设置挖掘力（IC2原版：power -= hardness/1.5）
 * @param power 挖掘力
 */
    public void setPower(float power) {
        this.power = power;
    }

    /**
     * 获取挖掘力
 * @return 挖掘力
 */
    public float getPower() {
        return power;
    }

    /**
     * 设置可破坏方块数
     * @param blockBreaks 可破坏方块数
     */
    public void setBlockBreaks(int blockBreaks) {
        this.blockBreaks = blockBreaks;
    }

    /**
     * 获取可破坏方块数
     * @return 可破坏方块数
     */
    public int getBlockBreaks() {
        return blockBreaks;
    }

    /**
     * 设置飞行距离（IC2原版：range -= 飞行距离/tick，1时消失?1时消失，-1=无限）
 * @param range 飞行距离（0表示无限?1表示无限
     */
    public void setRange(float range) {
        this.range = range;
    }

    /**
     * 获取飞行距离
     * @return 飞行距离（-1表示无限?1表示无限
     */
    public float getRange() {
        return range;
    }

    /**
     * 设置是否为暴击
 * @param crit 是否为暴击
 */
    public void setCritArrow(boolean crit) {
        this.entityData.set(ID_CRIT, crit);
    }

    /**
     * 是否为暴击
 * @return 如果是暴击返回true
     */
    public boolean isCritArrow() {
        return this.entityData.get(ID_CRIT);
    }

    /**
     * 设置采矿模式
     * @param miningMode true=采矿模式（可以挖矿），false=战斗模式（只伤害实体）
 */
    public void setMiningMode(boolean miningMode) {
        this.miningMode = miningMode;
        this.entityData.set(ID_MINING_MODE, miningMode);
    }

    /**
     * 是否为采矿模式
 * @return true=采矿模式，false=战斗模式
     */
    public boolean isMiningMode() {
        return this.miningMode;
    }

    /**
     * 设置穿透模式
 * @param penetratingMode true=穿透方块不消失，false=碰到方块消失
     */
    public void setPenetratingMode(boolean penetratingMode) {
        this.penetratingMode = penetratingMode;
        this.entityData.set(ID_PENETRATING_MODE, penetratingMode);
    }

    /**
     * 是否为穿透模式
 * @return true=穿透模式，false=普通模式
 */
    public boolean isPenetratingMode() {
        return this.penetratingMode;
    }

    /**
     * 设置超级热线模式
     * @param superHeatMode true=烧制方块
     */
    public void setSuperHeatMode(boolean superHeatMode) {
        this.superHeatMode = superHeatMode;
        this.entityData.set(ID_SUPER_HEAT_MODE, superHeatMode);
    }

    /**
     * 是否为超级热线模式
 * @return true=超级热线模式
     */
    public boolean isSuperHeatMode() {
        return this.superHeatMode;
    }

    /**
     * 设置爆破模式
     * @param explosiveMode true=爆炸效果
     */
    public void setExplosiveMode(boolean explosiveMode) {
        this.explosiveMode = explosiveMode;
        this.entityData.set(ID_EXPLOSIVE_MODE, explosiveMode);
    }

    /**
     * 是否为爆破模式
 * @return true=爆破模式
     */
    public boolean isExplosiveMode() {
        return this.explosiveMode;
    }

    /**
     * 设置核能模式
     * @param nuclearMode true=核能模式（100倍威力）?00倍威力）
     */
    public void setNuclearMode(boolean nuclearMode) {
        this.nuclearMode = nuclearMode;
        this.entityData.set(ID_NUCLEAR_MODE, nuclearMode);
    }

    /**
     * 是否为核能模式
 * @return true=核能模式
     */
    public boolean isNuclearMode() {
        return this.nuclearMode;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ID_CRIT, false);
        builder.define(ID_MINING_MODE, true); // 默认为采矿模式
    builder.define(ID_PENETRATING_MODE, false); // 默认为非穿透模式
    builder.define(ID_SUPER_HEAT_MODE, false); // 默认为非超级热线模式
        builder.define(ID_EXPLOSIVE_MODE, false); // 默认为非爆破模式
        builder.define(ID_NUCLEAR_MODE, false); // 默认为非核能模式
    }

    /**
     * 每tick更新
     */
    @Override
    public void tick() {
        super.tick();

        // 更新生命周期
        this.life++;
        
        // IC2原版：power<=0或blockBreaks<=0时消失
    if (this.power <= 0.0f || this.blockBreaks <= 0) {
            if (isExplosiveMode()) {
                triggerExplosion();
            }
            this.discard();
            return;
        }

        // IC2原版：range < 1.0 时消失（-1表示无限距离）
    if (this.range >= 0.0f && this.range < 1.0f) {
            if (isExplosiveMode()) {
                triggerExplosion();
            }
            this.discard();
            return;
        }

        // 安全上限：防止无限飞行
    if (this.life > MAX_LIFE) {
            if (isExplosiveMode()) {
                triggerExplosion();
            }
            this.discard();
            return;
        }

        // IC2原版：每tick range -= 移动距离
        if (this.range >= 0.0f) {
            double dx = this.getDeltaMovement().x;
            double dy = this.getDeltaMovement().y;
            double dz = this.getDeltaMovement().z;
            double distanceMoved = Math.sqrt(dx * dx + dy * dy + dz * dz);
            this.range -= distanceMoved;
        }

        // IC2原版：每tick power -= 0.5（即使没有击中任何东西也会衰减）
        this.power -= 0.5f;
    }

    /**
     * 处理碰撞（方块或实体）
 * @param result 碰撞结果
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockResult = (BlockHitResult) result;
            
            // 战斗模式（非采矿、非超级热线、非爆破）：直接消失，不处理方块
            if (!isMiningMode() && !isSuperHeatMode() && !isExplosiveMode()) {
                // 战斗模式碰到方块直接消失，不破坏方块
                this.discard();
                return;
            }
            
            // 爆破模式：在击中点触发爆炸
            if (isExplosiveMode()) {
                // 将子弹移动到精确的击中位置
                Vec3 hitLocation = blockResult.getLocation();
                this.setPos(hitLocation.x, hitLocation.y, hitLocation.z);
                // 触发爆炸
                triggerExplosion();
                // 播放音效并移除
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                this.discard();
                return;
            }
            
            // 其他模式（采矿、超级热线）
            this.onHitBlock(blockResult);
            // 穿透模式下碰到方块不消失，继续飞行
            if (isPenetratingMode()) {
                return; // 不执行后续的音效和移除
            }
        } else if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
            // 爆破模式下击中实体也触发爆炸
            if (isExplosiveMode()) {
                triggerExplosion();
            }
        }

        // 播放音效
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);

        // 移除实体
        this.discard();
    }

    /**
     * 触发爆炸（爆破模式）
     */
    private void triggerExplosion() {
        if (!this.level().isClientSide) {
            if (isNuclearMode()) {
                triggerNuclearExplosion();
            } else {
                // IC2原版：爆破模式爆炸威力?5.0F
                this.level().explode(
                    this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null,
                    this.getX(), this.getY(), this.getZ(),
                    5.0F,
                    Level.ExplosionInteraction.TNT
                );
            }
        }
    }

    /**
     * 触发核能爆炸（完全清除爆炸范围内的所有方块，并对范围内所有实体造成高额伤害）
 */
    private void triggerNuclearExplosion() {
        if (this.level().isClientSide) return;

        // 爆炸中心
        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ();

        // 核能爆炸半径（?00F威力对应的半径）
        int radius = 48; // 48格半径
    int decayStartRadius = 32; // 32格后开始衰退（三分之二半径）
        // 获取爆炸中心实体
    LivingEntity owner = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;

        // 遍历爆炸范围内的所有方块
    for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // 计算距离
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    // 只在球形范围内破坏方块
                if (distance <= radius) {
                        // 核能爆炸：前32格100%破坏，之后逐渐衰减（92%破坏率）
                        float preservationChance;
                        if (distance <= decayStartRadius) {
                             // 32格内：100%破坏，0%保留
                            preservationChance = 0.0f;
                        } else {
                             // 32格之后：100%逐渐衰减到0%，0~8%随机破坏
                            preservationChance = (float)((distance - decayStartRadius) / (radius - decayStartRadius)) * 0.02f;
                        }

                        if (this.random.nextFloat() >= preservationChance) {
                            BlockPos pos = BlockPos.containing(centerX + x, centerY + y, centerZ + z);
                            BlockState state = this.level().getBlockState(pos);

                            // 破坏非空气方块和流体（包括黑曜石、水、岩浆等）
                        if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.BARRIER)) {
                                // 使用 setBlock 替换为空气，可以正确处理流体方块
                                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }

        // 对爆炸范围内的所有实体造成伤害
        // 获取爆炸范围内的所有实体
    var entities = this.level().getEntities(this, 
            new net.minecraft.world.phys.AABB(centerX - radius, centerY - radius, centerZ - radius,
                                               centerX + radius, centerY + radius, centerZ + radius));
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                // 计算实体到爆炸中心的距离
                double distance = Math.sqrt(
                    Math.pow(entity.getX() - centerX, 2) +
                    Math.pow(entity.getY() - centerY, 2) +
                    Math.pow(entity.getZ() - centerZ, 2)
                );
                
                // 只在爆炸半径内造成伤害
                if (distance <= radius) {
                    // 距离衰减：越靠近中心伤害越高，最初500000伤害，最终10000伤害
                    float damageMultiplier = 1.0f - (float)(distance / radius) * 0.9f;
                    float finalDamage = 100000.0f * damageMultiplier;
                    
                    // 使用通用爆炸伤害类型，对末影龙等有效
                // 爆炸伤害可以穿透大部分伤害免疫
                    livingEntity.hurt(this.damageSources().explosion(this, owner), finalDamage);
                }
            }
        }

        // 创建视觉效果（使用原版的爆炸效果）
    this.level().explode(
            owner,
            centerX, centerY, centerZ,
            8.0F, // 较小的威力用于视觉效果
        Level.ExplosionInteraction.NONE // 不破坏方块，只产生视觉效果
    );

        // 播放音效
        this.level().playSound(null, centerX, centerY, centerZ,
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, 0.5F);
    }

    /**
     * 处理方块碰撞
     * @param result 方块碰撞结果
     */
    protected void onHitBlock(BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        BlockState state = this.level().getBlockState(pos);

        // 战斗模式：不破坏任何方块
        if (!isMiningMode() && !isSuperHeatMode() && !isExplosiveMode()) {
            return;
        }

        // 超级热线模式：烧制方块
    if (isSuperHeatMode()) {
            smeltBlock(pos, state);
            this.blockBreaks--;
            return;
        }

        // 采矿模式：破坏方块（IC2原版逻辑）
    if (isMiningMode()) {
            if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
                float hardness = state.getDestroySpeed(this.level(), pos);
                if (hardness < 0.0f) {
                    // 不可破坏方块（如基岩），子弹消失
                    this.discard();
                    return;
                }
                // IC2原版：power -= hardness / 1.5
                this.power -= hardness / 1.5f;
                if (this.power < 0.0f) {
                    // power不足，子弹消失
                return;
                }
                this.level().destroyBlock(pos, true, this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null);
                this.blockBreaks--;
            }
        }
    }

    /**
     * 烧制方块（超级热线模式）
     * @param pos 方块位置
     * @param state 方块状态
 */
    private void smeltBlock(BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // IC2原版：石头材质不掉落（dropBlock=false）
    // 检查是否为石头类材质
    boolean isStoneMaterial = state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE) ||
                                   block == Blocks.COBBLESTONE || block == Blocks.STONE ||
                                   block == Blocks.GRANITE || block == Blocks.DIORITE ||
                                   block == Blocks.ANDESITE || block == Blocks.DEEPSLATE ||
                                   block == Blocks.TUFF || block == Blocks.BASALT ||
                                   block == Blocks.BLACKSTONE || block == Blocks.CALCITE;

        // 尝试获取烧制结果
        ItemStack inputStack = new ItemStack(block);
        
        net.minecraft.world.item.crafting.SingleRecipeInput recipeInput = 
            new net.minecraft.world.item.crafting.SingleRecipeInput(inputStack);
        
        var recipeOptional = this.level().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, recipeInput, this.level());
        
        if (recipeOptional.isPresent()) {
            var recipeHolder = recipeOptional.get();
            ItemStack result = recipeHolder.value().getResultItem(this.level().registryAccess());
            if (!result.isEmpty()) {
                // IC2原版：石头材质不掉落方块
                if (isStoneMaterial) {
                    this.level().removeBlock(pos, false);
                } else {
                    this.level().removeBlock(pos, false);
                    Block resultBlock = Block.byItem(result.getItem());
                    if (resultBlock != Blocks.AIR) {
                        this.level().setBlock(pos, resultBlock.defaultBlockState(), 3);
                    } else {
                        Block.popResource(this.level(), pos, result.copy());
                    }
                }
                // IC2原版：烧制成功后power归零
                this.power = 0.0f;
                return;
            }
        }

        // 如果没有烧制配方，尝试直接破坏方块
    if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
            this.level().destroyBlock(pos, true, this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null);
        }
    }

    /**
     * 处理实体碰撞
     * @param result 实体碰撞结果
     */
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();

        if (entity instanceof LivingEntity livingEntity) {
            // IC2原版：伤害=(int)power?= (int)power
            int damage = (int) this.power;
            if (damage > 0) {
                // 热线模式着火效果已移除
                livingEntity.hurt(this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null), damage);
            }
        }
    }

    /**
     * 检查是否在渲染范围内
 */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) {
            d0 = 1.0;
        }
        d0 *= 64.0 * getViewScale();
        return distance < d0 * d0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putShort("life", (short) this.life);
        compound.putFloat("damage", this.damage);
        compound.putFloat("power", this.power);
        compound.putInt("blockBreaks", this.blockBreaks);
        compound.putFloat("range", this.range);
        compound.putBoolean("miningMode", this.miningMode);
        compound.putBoolean("penetratingMode", this.penetratingMode);
        compound.putBoolean("superHeatMode", this.superHeatMode);
        compound.putBoolean("explosiveMode", this.explosiveMode);
        compound.putBoolean("nuclearMode", this.nuclearMode);
        if (this.startPos != null) {
            compound.putDouble("startX", this.startPos.x);
            compound.putDouble("startY", this.startPos.y);
            compound.putDouble("startZ", this.startPos.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.life = compound.getShort("life");
        if (compound.contains("damage", 99)) {
            this.damage = compound.getFloat("damage");
        }
        if (compound.contains("power", 99)) {
            this.power = compound.getFloat("power");
        }
        if (compound.contains("blockBreaks", 99)) {
            this.blockBreaks = compound.getInt("blockBreaks");
        }
        if (compound.contains("range", 99)) {
            this.range = compound.getFloat("range");
        }
        if (compound.contains("miningMode")) {
            this.miningMode = compound.getBoolean("miningMode");
        }
        if (compound.contains("penetratingMode")) {
            this.penetratingMode = compound.getBoolean("penetratingMode");
        }
        if (compound.contains("superHeatMode")) {
            this.superHeatMode = compound.getBoolean("superHeatMode");
        }
        if (compound.contains("explosiveMode")) {
            this.explosiveMode = compound.getBoolean("explosiveMode");
        }
        if (compound.contains("nuclearMode")) {
            this.nuclearMode = compound.getBoolean("nuclearMode");
        }
        if (compound.contains("startX")) {
            this.startPos = new Vec3(
                compound.getDouble("startX"),
                compound.getDouble("startY"),
                compound.getDouble("startZ")
            );
        }
    }
}