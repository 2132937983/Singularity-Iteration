package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.energy.ICableEnergyNode;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreSounds;
import com.miophas.singularity_iteration.core.runtime.energy.WaterEntityQueryCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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

    /**
     * 0.1.10.39 (universal cables): every face takes FE pushed by the block beyond it (machines, generators, batteries of other
     * mods) into the grid, at voltage 0 ({@code IndependentSiEnergy.offerFe}). Receive only; no FE toward another mod's cable.
     */
    @Override
    public net.neoforged.neoforge.energy.IEnergyStorage scexFeCapability(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (side == null || !(level instanceof ServerLevel server) || isRemoved()) return null;
        if (com.miophas.singularity_iteration.core.runtime.energy.ForeignEnergyCables.is(server, worldPosition.relative(side))) return null;
        BlockPos cable = worldPosition;
        return new net.neoforged.neoforge.energy.IEnergyStorage() {
            private com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy engine() {
                return com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.current(server.getServer());
            }
            @Override public int receiveEnergy(int amount, boolean simulate) {
                var engine = engine();
                return engine == null || isRemoved() ? 0 : engine.offerFe(server, cable, side, amount, simulate);
            }
            @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return 0; }
            @Override public int getMaxEnergyStored() { return com.miophas.singularity_iteration.core.runtime.energy.grid.FECompatTile.INBOX_FE; }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() { return engine() != null && !isRemoved(); }
        };
    }

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

    /**
     * 0.1.10.7: game time until which the wire counts as powered. A wire of the independent energy
     * network does not tick any more (its tick only counted {@code poweredTicksRemaining} down), so
     * the powered state is a time comparison. {@code poweredTicksRemaining} is still set and counted
     * down by wires that tick (the legacy path), for subclasses that read it.
     */
    private long poweredUntil = Long.MIN_VALUE;

    @Override public void onEnergyPass() { setPowered(); }
    public void setPowered() {
        this.poweredTicksRemaining = POWERED_DURATION;
        if (level != null) this.poweredUntil = level.getGameTime() + POWERED_DURATION;
    }
    public boolean isPowered() {
        if (level != null && poweredUntil != Long.MIN_VALUE) return level.getGameTime() < poweredUntil;
        return this.poweredTicksRemaining > 0;
    }

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
        // the level caches its damage sources: no registry lookup or new object per check
        DamageSource electricSource = level.damageSources().lightningBolt();
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