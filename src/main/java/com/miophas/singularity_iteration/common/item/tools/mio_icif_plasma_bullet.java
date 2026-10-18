// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 等离子弹 - 对齐 IC2 1.7.10 的 {@code EntityParticle}（{@code PlasmaLauncher} 的发射物）。
 *
 * <p>沿视线直线无重力高速飞行，飞行路径上会：
 * <ul>
 *   <li>熔炼沿途方块（仅当烧炼产物仍是方块时替换，如圆石→石头、沙子→玻璃）；</li>
 *   <li>清除水与岩浆；</li>
 *   <li>点燃易燃方块。</li>
 * </ul>
 * 命中方块或实体时产生 Heat 爆炸（原版威力 18）：按原版 Heat 吸收公式判定，只能破坏抗性较低的"软"方块。
 */
@SuppressWarnings("null")
public class mio_icif_plasma_bullet extends IndependentElectricProjectile {

    /** 原版 {@code EntityParticle} 的 speed 参数（格/tick）。 */
    public static final double PLASMA_SPEED = 8.0D;
    /** 原版 {@code ExplosionIC2} 的威力。 */
    public static final float EXPLOSION_POWER = 18.0F;
    /** 原版 coreSize（弹体核心半径）。 */
    public static final double CORE_SIZE = 1.0D;
    /** 原版 influenceSize（影响半径，决定熔炼圆柱的半径）。 */
    public static final double INFLUENCE_SIZE = 2.0D;

    private static final double MAX_RANGE = 256.0D;

    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> type, Level level) {
        super(type, level);
        setRange(MAX_RANGE);
    }

    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> type, LivingEntity owner, Level level) {
        super(type, owner, level);
        setRange(MAX_RANGE);
    }

    @Override
    public void tick() {
        // 0.1.10.4: only a player who may build melts the path (a Wingman's bolt leaves the terrain)
        if (!level().isClientSide && !isRemoved() && terrainAllowed()) {
            Vec3 start = position();
            Vec3 end = start.add(getDeltaMovement());
            meltAlongPath(start, end);
        }
        if (level().isClientSide && tickCount > 0) trailParticles();
        super.tick();
    }

    /** 0.1.10.4 client: sparks and violet embers shed along the path of the last tick. */
    private void trailParticles() {
        Vec3 v = getDeltaMovement(), p = position(), from = origin();
        for (int i = 0; i < 5; i++) {
            Vec3 at = p.subtract(v.scale(random.nextDouble()));
            if (at.distanceToSqr(from) < 9) continue;            // keep the shooter's view clear
            level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, at.x + (random.nextDouble() - 0.5) * 0.3,
                at.y + (random.nextDouble() - 0.5) * 0.3, at.z + (random.nextDouble() - 0.5) * 0.3,
                (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12);
        }
        for (int i = 0; i < 2; i++) {
            Vec3 at = p.subtract(v.scale(random.nextDouble()));
            if (at.distanceToSqr(from) < 9) continue;
            level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.72F, 0.38F, 1.0F), 1.3F),
                at.x, at.y, at.z, 0, 0.02, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        // 0.1.10.4: a bolt that may not blast (Wingman, or no build right) burns the target instead
        if (!detonate(hit.getLocation())) {
            damage(hit.getEntity(), mio_icif_plasma_launcher.AI_HIT_DAMAGE);
            hit.getEntity().igniteForSeconds(5.0F);
        }
        impact(hit.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) return;
        detonate(hit.getLocation());
        impact(hit.getLocation());
        discard();
    }

    /** 0.1.10.4: impact flash: a white core flash, a ring of violet sparks and rising embers. */
    private void impact(Vec3 at) {
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 36, 0.35, 0.35, 0.35, 0.45);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x, at.y, at.z, 14, 0.2, 0.2, 0.2, 0.12);
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.72F, 0.38F, 1.0F), 1.6F),
            at.x, at.y, at.z, 24, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, at.x, at.y, at.z, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ENERGY_HIT.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F + random.nextFloat() * 0.15F);
    }

    /**
     * 触发 IC2 风格的 Heat 爆炸：只破坏抗性足够低（"软"）的方块。
     *
     * <p>原版 {@code ExplosionIC2.getAbsorption} 在 Heat 类型下对方块抗性的换算为
     * {@code 0.5 + (抗性 + 4) * 0.3 * 6}，当该值大于剩余威力时射线停止、方块不破坏。
     * 这里通过自定义 {@link ExplosionDamageCalculator} 复刻该判定：抗性超过威力的方块返回超高抗性，
     * 既使其不被破坏，也耗尽射线从而保留遮挡关系（硬方块后方的软方块同样不会被波及）。
     */
    /** Returns true when the heat explosion happened (0.1.10.4: the callers burn the target otherwise). */
    private boolean detonate(Vec3 center) {
        if (!(level() instanceof ServerLevel level)) return false;
        if (!(getOwner() instanceof ServerPlayer player) || !player.mayBuild()) return false;

        BlockPos pos = BlockPos.containing(center);
        int radius = (int) Math.ceil(EXPLOSION_POWER) + 1;
        if (!level.isLoaded(pos.offset(-radius, -radius, -radius))
                || !level.isLoaded(pos.offset(radius, radius, radius))
                || !level.mayInteract(player, pos)) {
            return false;
        }

        level.explode(this, level.damageSources().explosion(this, player), new HeatExplosionCalculator(),
            center.x, center.y, center.z, EXPLOSION_POWER, false, Level.ExplosionInteraction.TNT);
        return true;
    }

    /**
     * 对直线飞行段施加等离子影响：扫描以该段为中轴、半径 {@link #INFLUENCE_SIZE} 的圆柱体。
     */
    private void meltAlongPath(Vec3 start, Vec3 end) {
        if (!(level() instanceof ServerLevel level)) return;
        if (start.distanceToSqr(end) <= 1.0E-6D) return;

        BlockPos min = BlockPos.containing(
            Math.min(start.x, end.x) - INFLUENCE_SIZE,
            Math.min(start.y, end.y) - INFLUENCE_SIZE,
            Math.min(start.z, end.z) - INFLUENCE_SIZE);
        BlockPos max = BlockPos.containing(
            Math.max(start.x, end.x) + INFLUENCE_SIZE,
            Math.max(start.y, end.y) + INFLUENCE_SIZE,
            Math.max(start.z, end.z) + INFLUENCE_SIZE);

        double radiusSqr = INFLUENCE_SIZE * INFLUENCE_SIZE;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) continue;
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 closest = closestPointOnSegment(center, start, end);
            if (closest.distanceToSqr(center) > radiusSqr) continue;
            applyInfluence(level, pos, center, closest);
        }
    }

    private void applyInfluence(ServerLevel level, BlockPos pos, Vec3 center, Vec3 closest) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        // 清除水与岩浆
        if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        // 熔炼：仅当烧炼产物仍是方块时替换
        ItemStack input = new ItemStack(state.getBlock());
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        if (recipe.isPresent()) {
            ItemStack output = recipe.get().value().assemble(new SingleRecipeInput(input), level.registryAccess());
            if (!output.isEmpty() && output.getItem() instanceof BlockItem blockItem) {
                level.setBlock(pos, blockItem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }

        // 点燃易燃方块：在背向弹道的一侧生火
        if (state.isFlammable(level, pos, Direction.UP)) {
            Vec3 outward = center.subtract(closest);
            Direction dir = outward.lengthSqr() < 1.0E-6D
                ? Direction.UP
                : Direction.getNearest(outward.x, outward.y, outward.z);
            BlockPos firePos = pos.relative(dir);
            if (level.isLoaded(firePos) && level.getBlockState(firePos).isAir()) {
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Vec3 closestPointOnSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double abLenSqr = ab.lengthSqr();
        if (abLenSqr <= 1.0E-8D) return a;
        double t = point.subtract(a).dot(ab) / abLenSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return a.add(ab.scale(t));
    }

    /** 复刻 IC2 {@code ExplosionIC2.getAbsorption} 的 Heat 抗性判定。 */
    private static final class HeatExplosionCalculator extends ExplosionDamageCalculator {
        /** 足以耗尽射线、使方块无法被破坏的抗性值。 */
        private static final float PROTECTED_RESISTANCE = 1000.0F;
        /** 原版 Heat 吸收公式系数：0.5 + (抗性 + 4) * 0.3 * 6。 */
        private static final double HEAT_ABSORPTION_FACTOR = 0.3D * 6.0D;

        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos,
                                                           BlockState state, FluidState fluid) {
            Optional<Float> vanilla = super.getBlockExplosionResistance(explosion, level, pos, state, fluid);
            if (vanilla.isEmpty()) return Optional.empty();

            double absorption = 0.5D + (vanilla.get() + 4.0D) * HEAT_ABSORPTION_FACTOR;
            if (absorption > EXPLOSION_POWER) {
                return Optional.of(PROTECTED_RESISTANCE);
            }
            return Optional.of((float) absorption);
        }
    }
}
