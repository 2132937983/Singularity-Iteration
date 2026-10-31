package com.miophas.singularity_iteration.common.block.environment;

import com.miophas.singularity_iteration.common.effect.mio_icif_effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for radioactive blocks.
 * These blocks are contaminated by a nuclear explosion. They cause radiation damage to nearby living entities.
 */
@SuppressWarnings("null")
public class mio_icif_radioactive_block extends Block {
    private static final int MAX_RADIATION_LEVEL = 32;
    private final int radiationLevel; // Radiation level. It controls the damage and the range.
    
    public mio_icif_radioactive_block(Properties properties, int radiationLevel) {
        super(properties);
        this.radiationLevel = Math.clamp(radiationLevel, 0, MAX_RADIATION_LEVEL);
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Apply a radiation effect to nearby living entities at random.
        if (random.nextInt(20) == 0) { // 5% chance
            applyRadiationEffect(level, pos);
        }
    }
    
    /**
     * Applies the radiation effect to nearby living entities.
     */
    private void applyRadiationEffect(ServerLevel level, BlockPos pos) {
        // Keep malformed registry/config values from creating an unbounded
        // entity query. Bundled levels are much smaller than this cap.
        int range = radiationLevel * 2;
        if (range == 0) {
            return;
        }
        double rangeSquared = (double) range * range;
        var entities = level.getEntitiesOfClass(LivingEntity.class, 
            new net.minecraft.world.phys.AABB(
                pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                pos.getX() + range, pos.getY() + range, pos.getZ() + range
            ));
        
        for (LivingEntity living : entities) {
                // 0.1.10.41: a full hazmat suit protects here as in a blast zone (no radiation sickness, weakness or wither)
                if (com.miophas.singularity_iteration.common.util.RadiationProtectionUtil.isWearingFullHazmat(living)) continue;
                double distanceSquared = living.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (distanceSquared <= rangeSquared) {
                    double distance = Math.sqrt(distanceSquared);
                    // 0.1.10.5: lead (and weaker shielding blocks) between this block and the creature
                    double through = com.miophas.singularity_iteration.common.util.RadiationShielding.transmission(level,
                        com.miophas.singularity_iteration.common.util.RadiationShielding.centre(pos), living, pos);
                    if (through <= 0) continue;
                    int effectDuration = (int) ((1.0 - distance / range) * 200 * radiationLevel * through);
                    int effectLevel = through >= 0.5 ? Math.max(0, radiationLevel - 1) : 0;

                    if (effectDuration > 20) {
                        living.addEffect(new MobEffectInstance(mio_icif_effects.RADIATION, effectDuration, effectLevel));
                        if (radiationLevel >= 2 && through >= 0.5) {
                            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration, effectLevel));
                        }
                        if (radiationLevel >= 3 && through >= 0.5) {
                            living.addEffect(new MobEffectInstance(MobEffects.WITHER, effectDuration / 2, 0));
                        }
                    }
                }
        }
    }
    
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return radiationLevel > 0;
    }
    
    public int getRadiationLevel() {
        return radiationLevel;
    }
}

