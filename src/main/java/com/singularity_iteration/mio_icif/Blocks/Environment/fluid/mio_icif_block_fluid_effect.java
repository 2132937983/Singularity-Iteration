package com.singularity_iteration.mio_icif.Blocks.Environment.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class mio_icif_block_fluid_effect extends LiquidBlock {

    private final FluidEffect effect;

    public mio_icif_block_fluid_effect(FlowingFluid fluid, Properties properties, FluidEffect effect) {
        super(fluid, properties);
        this.effect = effect;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;

        if (effect == null) return;
        effect.apply(living);
    }

    public interface FluidEffect {
        void apply(LivingEntity entity);
    }

    public static final FluidEffect CONSTRUCTION_FOAM = living -> {
        if (!living.hasEffect(MobEffects.HUNGER)) {
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 2, true, true));
        }
    };

    public static final FluidEffect UU_MATTER = living -> {
        if (!living.hasEffect(MobEffects.REGENERATION)) {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, true));
        }
    };

    public static final FluidEffect STEAM = living -> {
        if (!living.hasEffect(MobEffects.NIGHT_VISION)) {
            living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, true));
        }
    };

    public static final FluidEffect HOT_WATER = living -> {
        if (living.isOnFire()) {
            if (!living.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, true));
            }
        } else {
            if (!living.hasEffect(MobEffects.REGENERATION)) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100,
                        living.level().random.nextInt(2), true, true));
            }
        }
    };

    public static final FluidEffect PAHOEHOE_LAVA = living -> {
        living.setRemainingFireTicks(200);
    };

    public static final FluidEffect HOT_COOLANT = living -> {
        living.setRemainingFireTicks(600);
    };
}