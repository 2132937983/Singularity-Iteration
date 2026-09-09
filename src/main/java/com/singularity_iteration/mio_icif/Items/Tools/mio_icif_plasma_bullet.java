package com.singularity_iteration.mio_icif.Items.Tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * 等离子子弹实体类 - 还原IC2原版EntityParticle行为
 *
 * IC2原版EntityParticle核心机制造?
 * - 自定义移动：直接修改坐标，不依赖ThrowableProjectile的move()
 * - 射线追踪：每tick从旧位置到新位置做射线追踪，精确检测碰�?
 * - 沿路径影响：飞行路径上每一步都施加influenceSize范围的影�?
 * - radialTestVectors：围绕飞行轴的径向测试向量，形成圆柱形影响区
 *
 * 核心参数据?
 * - speed=8.0, coreSize=1.0, influenceSize=4.0(原版2.0的两�?
 * - lifeTime=6000, 无重力，直线飞行
 */
@SuppressWarnings("null")
public class mio_icif_plasma_bullet extends ThrowableProjectile {

    public static final double PLASMA_SPEED = 8.0;
    public static final int MAX_LIFE = 6000;
    private static final double INFLUENCE_SIZE = 2.0;
    private static final double HEAT_EXPLOSION_RADIUS = 4.0;
    private static final float HEAT_EXPLOSION_DAMAGE = 60.0F;
    private static final float HARD_BLOCK_RESISTANCE = 6.0F;

    private int life = 0;

    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> entityType, LivingEntity owner, Level level) {
        super(entityType, owner, level);
        this.setNoGravity(true);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> entityType, double x, double y, double z, Level level) {
        super(entityType, x, y, z, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    /**
     * 还原IC2原版EntityParticle的tick逻辑
     *
     * IC2原版流程�?
     * 1. 保存上一tick位置(prevPos)
     * 2. 直接将motion加到位置上得到新位置
     * 3. 从旧位置到新位置做射线追踪?clip)，检测方块碰�?
     * 4. 检测路径上的实体碰�?核心coreSize命中 vs 影响influenceSize范围)
     * 5. 对影响范围内的实体调用onInfluence
     * 6. 对飞行路径周围用radialTestVectors检测方块并调用onInfluence
     * 7. 如果有核心命�?方块或实�?，调用onImpact并销毁?
     * 8. 否则递减lifeTime
     */
    @Override
    public void tick() {
        this.baseTick();
        this.life++;
        if (this.life > MAX_LIFE) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            Vec3 start = this.position();
            Vec3 velocity = this.getDeltaMovement();
            Vec3 end = start.add(velocity);

            BlockHitResult blockHit = this.level().clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
            );

            AABB pathBox = this.getBoundingBox().expandTowards(velocity).inflate(INFLUENCE_SIZE);
            List<Entity> entitiesInPath = this.level().getEntities(this, pathBox,
                entity -> entity != this.getOwner() && entity.isPickable()
            );

            Entity closestHitEntity = null;
            double closestEntityDist = start.distanceTo(end);
            @SuppressWarnings("unused")
            Entity closestInfluenceEntity = null;
            double maxInfluenceDist = closestEntityDist + INFLUENCE_SIZE;

            for (Entity entity : entitiesInPath) {
                AABB entityBox = entity.getBoundingBox().inflate(INFLUENCE_SIZE);
                Optional<Vec3> influenceHit = entityBox.clip(start, end);
                if (influenceHit.isPresent()) {
                    double dist = start.distanceTo(influenceHit.get());
                    if (dist <= maxInfluenceDist) {
                        closestInfluenceEntity = entity;
                    }

                    AABB coreBox = entity.getBoundingBox();
                    Optional<Vec3> coreHit = coreBox.clip(start, end);
                    if (coreHit.isPresent()) {
                        double coreDist = start.distanceTo(coreHit.get());
                        if (coreDist < closestEntityDist) {
                            closestHitEntity = entity;
                            closestEntityDist = coreDist;
                        }
                    }
                }
            }

            Vec3 hitPos = end;
            boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
            boolean hitEntity = closestHitEntity != null;

            if (hitBlock) {
                double blockDist = start.distanceTo(blockHit.getLocation());
                if (blockDist < closestEntityDist) {
                    hitPos = blockHit.getLocation();
                } else {
                    hitPos = start.add(velocity.normalize().scale(closestEntityDist));
                }
            } else if (hitEntity) {
                hitPos = start.add(velocity.normalize().scale(closestEntityDist));
            }

            applyInfluenceAlongPath(start, hitPos);

            this.setPos(hitPos);

            if (hitBlock || hitEntity) {
                onImpact();
                this.discard();
                return;
            }
        }

        if (this.level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getY() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                    0, 0, 0
                );
            }
        }
    }

    /**
     * 还原IC2原版沿飞行路径施加影�?
     * IC2使用radialTestVectors围绕飞行轴形成圆柱形影响�?
     * 简化实现：沿路径每2格为一步，每步检查influenceSize半径内的方块
     */
    private void applyInfluenceAlongPath(Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.1) return;

        Vec3 step = direction.normalize();
        int steps = Math.max(1, (int) Math.ceil(length / 2.0));

        for (int i = 0; i <= steps; i++) {
            Vec3 pathPos = start.add(step.scale(Math.min(i * 2.0, length)));
            applyInfluenceAtPos(pathPos);
        }
    }

    private void applyInfluenceAtPos(Vec3 pos) {
        BlockPos center = BlockPos.containing(pos);
        int r = (int) Math.ceil(INFLUENCE_SIZE);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > INFLUENCE_SIZE * INFLUENCE_SIZE) continue;
                    BlockPos blockPos = center.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(blockPos);
                    Block block = state.getBlock();

                    if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                        this.level().setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    if (state.isAir()) continue;

                    onInfluenceBlock(blockPos, state, block);
                }
            }
        }
    }

    private void onInfluenceBlock(BlockPos pos, BlockState state, Block block) {
        ItemStack inputStack = new ItemStack(block);
        net.minecraft.world.item.crafting.SingleRecipeInput recipeInput =
            new net.minecraft.world.item.crafting.SingleRecipeInput(inputStack);

        Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<SmeltingRecipe>> recipeOptional =
            this.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, this.level());

        if (recipeOptional.isPresent()) {
            ItemStack result = recipeOptional.get().value().getResultItem(this.level().registryAccess());
            if (!result.isEmpty()) {
                Block resultBlock = Block.byItem(result.getItem());
                if (resultBlock != Blocks.AIR) {
                    this.level().setBlock(pos, resultBlock.defaultBlockState(), 3);
                } else {
                    this.level().removeBlock(pos, false);
                    Block.popResource(this.level(), pos, result.copy());
                }
                return;
            }
        }

        // 点燃方块功能已移除
    }

    /**
     * 还原IC2原版onImpact - Heat类型热力爆炸
     *
     * Heat爆炸特点（参考ExplosionIC2.Type.Heat）：
     * - 方块爆炸抗性乘�?倍（getAbsorption�?extra * 6.0D�?
     * - 硬方块（石头等）抗性远超爆炸威�?8.0，不受影�?
     * - 软方块（泥土、沙子等）抗性低于爆炸威力，被烧�?移除
     * - 蒸发水和岩浆
     * - 对实体造成伤害并点�?
     */
    @SuppressWarnings("deprecation")
    private void onImpact() {
        Level level = this.level();
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        Vec3 center = this.position();

        int r = (int) Math.ceil(HEAT_EXPLOSION_RADIUS);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > HEAT_EXPLOSION_RADIUS * HEAT_EXPLOSION_RADIUS) continue;

                    BlockPos pos = BlockPos.containing(center.x + dx, center.y + dy, center.z + dz);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();

                    if (state.isAir()) continue;

                    if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    float resistance = block.getExplosionResistance();
                    if (resistance >= HARD_BLOCK_RESISTANCE) continue;

                    ItemStack inputStack = new ItemStack(block);
                    net.minecraft.world.item.crafting.SingleRecipeInput recipeInput =
                        new net.minecraft.world.item.crafting.SingleRecipeInput(inputStack);

                    Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<SmeltingRecipe>> recipeOptional =
                        level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);

                    if (recipeOptional.isPresent()) {
                        ItemStack result = recipeOptional.get().value().getResultItem(level.registryAccess());
                        if (!result.isEmpty()) {
                            Block resultBlock = Block.byItem(result.getItem());
                            if (resultBlock != Blocks.AIR) {
                                level.setBlock(pos, resultBlock.defaultBlockState(), 3);
                            } else {
                                level.removeBlock(pos, false);
                                Block.popResource(level, pos, result.copy());
                            }
                            continue;
                        }
                    }

                    level.removeBlock(pos, false);
                }
            }
        }

        AABB damageArea = AABB.ofSize(center, HEAT_EXPLOSION_RADIUS * 2, HEAT_EXPLOSION_RADIUS * 2, HEAT_EXPLOSION_RADIUS * 2);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, damageArea,
            e -> e != owner && e.distanceTo(this) <= HEAT_EXPLOSION_RADIUS
        );
        for (LivingEntity entity : entities) {
            double distance = entity.distanceTo(this);
            float damageFactor = (float) (1.0 - distance / HEAT_EXPLOSION_RADIUS);
            float damage = HEAT_EXPLOSION_DAMAGE * damageFactor;
            if (damage > 0) {
                entity.hurt(this.damageSources().mobProjectile(this, owner), damage);
                // 实体着火效果已移除
            }
        }

        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        // 爆炸火焰粒子效果已移除
    }

    @Override
    protected void onHit(HitResult result) {
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0;
        if (Double.isNaN(d0)) {
            d0 = 1.0;
        }
        d0 *= 64.0 * getViewScale();
        return distance < d0 * d0;
    }
}