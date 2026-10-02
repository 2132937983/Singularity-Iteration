package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreSounds;
import com.miophas.singularity_iteration.core.runtime.energy.WaterEntityQueryCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

@SuppressWarnings("null")
public abstract class AbstractWireBlockEntity extends AbstractEnergyBlockEntity implements ICableEnergyNode {

    private static final int CONDUCTIVITY_RANGE = 32;
    private static final int ELECTRIC_CHECK_INTERVAL = 10;
    private static final int POWERED_DURATION = 20;

    private final CableTier cableTier;
    protected final boolean insulated;

    private final double conductorBreakdownEnergy;
    private final double insulationBreakdownEnergy;
    private final double insulationEnergyAbsorption;
    private final double conductionLoss;
    private final float electricDamage;

    private int ticksUntilElectricCheck = 0;
    protected int poweredTicksRemaining = 0;

    protected AbstractWireBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                      CableTier cableTier, boolean insulated,
                                      double conductorBreakdownEnergy,
                                      double insulationBreakdownEnergy,
                                      double insulationEnergyAbsorption,
                                      double conductionLoss,
                                      float electricDamage) {
        super(pos, state, type, 0, 0, 0, cableTier);
        this.cableTier = cableTier;
        this.insulated = insulated;
        this.conductorBreakdownEnergy = conductorBreakdownEnergy >= 0
            ? conductorBreakdownEnergy : cableTier.conductorBreakdownEnergy;
        this.insulationBreakdownEnergy = insulationBreakdownEnergy >= 0
            ? insulationBreakdownEnergy : cableTier.insulationBreakdownEnergy;
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

    protected AbstractWireBlockEntity(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                      CableTier cableTier, boolean insulated) {
        this(pos, state, type, cableTier, insulated, -1, -1, -1, -1, -1);
    }

    @Override public double getConductionLoss() { return conductionLoss; }
    @Override public double getInsulationEnergyAbsorption() { return insulationEnergyAbsorption; }
    @Override public double getInsulationBreakdownEnergy() { return insulationBreakdownEnergy; }
    @Override public double getConductorBreakdownEnergy() { return conductorBreakdownEnergy; }
    protected float getElectricDamage() { return electricDamage; }
    public CableTier getCableTier() { return cableTier; }

    @Override public void onEnergyPass() { setPowered(); }
    public void setPowered() { this.poweredTicksRemaining = POWERED_DURATION; }
    public boolean isPowered() { return this.poweredTicksRemaining > 0; }

    protected void tickPoweredState() {
        if (poweredTicksRemaining > 0) poweredTicksRemaining--;
    }

    protected void tickElectricCheck(Level level, BlockPos pos, BlockState state) {
        ticksUntilElectricCheck--;
        if (ticksUntilElectricCheck <= 0) {
            ticksUntilElectricCheck = ELECTRIC_CHECK_INTERVAL;
            checkElectricDamage(level, pos, state);
        }
    }

    protected void checkElectricDamage(Level level, BlockPos pos, BlockState state) {
        if (!isPowered()) return;
        float damage = getElectricDamage();
        if (damage <= 0) return;
        Holder<DamageType> electricDamageType = level.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT);
        DamageSource electricSource = new DamageSource(electricDamageType);
        if (isWaterlogged(state)) {
            AABB rangeBox = new AABB(
                pos.getX() - CONDUCTIVITY_RANGE, pos.getY() - CONDUCTIVITY_RANGE, pos.getZ() - CONDUCTIVITY_RANGE,
                pos.getX() + CONDUCTIVITY_RANGE, pos.getY() + CONDUCTIVITY_RANGE, pos.getZ() + CONDUCTIVITY_RANGE);
            List<LivingEntity> entitiesInRange = WaterEntityQueryCache.query(level, rangeBox);
            for (LivingEntity livingEntity : entitiesInRange) {
                if (livingEntity.isInWater()) livingEntity.hurt(electricSource, damage);
            }
        } else {
            AABB wireBox = new AABB(pos);
            List<LivingEntity> entitiesOnWire = level.getEntitiesOfClass(LivingEntity.class, wireBox,
                entity -> entity.isAlive() && !entity.isSpectator());
            for (LivingEntity livingEntity : entitiesOnWire) livingEntity.hurt(electricSource, damage);
        }
    }

    protected abstract boolean isWaterlogged(BlockState state);

    protected void spawnBurnoutEffects(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 30, 0.1, 0.1, 0.1, 0.05);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 15, 0.15, 0.15, 0.15, 0.03);
        }
        level.playSound(null, x, y, z, CoreSounds.CABLE_BREAK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}