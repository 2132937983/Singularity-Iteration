package com.singularity_iteration.mio_icif.Blocks.Environment;

import com.singularity_iteration.mio_icif.effect.mio_icif_effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 辐射方块基类
 * 被核爆炸污染的方块，会对周围生物造成辐射伤害
 */
@SuppressWarnings("null")
public class mio_icif_radioactive_block extends Block {
    
    private final int radiationLevel; // 辐射等级，影响伤害和范围
    
    public mio_icif_radioactive_block(Properties properties, int radiationLevel) {
        super(properties);
        this.radiationLevel = radiationLevel;
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 随机对周围生物造成辐射效果
        if (random.nextInt(20) == 0) { // 5% 概率
            applyRadiationEffect(level, pos);
        }
    }
    
    /**
     * 对周围生物应用辐射效果?     */
    private void applyRadiationEffect(ServerLevel level, BlockPos pos) {
        int range = radiationLevel * 2;
        var entities = level.getEntities(null, 
            new net.minecraft.world.phys.AABB(
                pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                pos.getX() + range, pos.getY() + range, pos.getZ() + range
            ));
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                double distance = Math.sqrt(entity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
                if (distance <= range) {
                    int effectDuration = (int) ((1.0 - distance / range) * 200 * radiationLevel);
                    int effectLevel = radiationLevel - 1;
                    
                    if (effectDuration > 20) {
                        living.addEffect(new MobEffectInstance(mio_icif_effects.RADIATION, effectDuration, effectLevel));
                        if (radiationLevel >= 2) {
                            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration, effectLevel));
                        }
                        if (radiationLevel >= 3) {
                            living.addEffect(new MobEffectInstance(MobEffects.WITHER, effectDuration / 2, 0));
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }
    
    public int getRadiationLevel() {
        return radiationLevel;
    }
}

