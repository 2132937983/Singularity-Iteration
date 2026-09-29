package com.miophas.singularity_iteration.core.api.machine.builder;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;

public interface IEnergyContainerBuilder {

    IEnergyContainerBuilder setName(String name);

    IEnergyContainerBuilder setTranslationKey(String key);

    IEnergyContainerBuilder setEnergyCapacity(long capacity);

    IEnergyContainerBuilder setMaxReceive(long maxReceive);

    IEnergyContainerBuilder setMaxExtract(long maxExtract);

    IEnergyContainerBuilder setCableTier(ICableTier tier);

    IEnergyContainerBuilder setChargepad(boolean chargepad);

    IEnergyContainerBuilder setMaxTransferPerItem(long maxTransfer);

    String getName();

    long getEnergyCapacity();

    long getMaxReceive();

    long getMaxExtract();

    ICableTier getCableTier();

    boolean isChargepad();

    long getMaxTransferPerItem();

    EnergyContainerConfiguration build();

    EnergyContainerConfiguration buildAndRegister(String modId);

    interface EnergyContainerConfiguration {
        String getName();
        String getTranslationKey();
        long getEnergyCapacity();
        long getMaxReceive();
        long getMaxExtract();
        ICableTier getCableTier();
        boolean isChargepad();
        long getMaxTransferPerItem();
    }
}