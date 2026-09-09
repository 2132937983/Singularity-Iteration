/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 * Copyright (c) 2024 Industrial_Craft_In_Future
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.singularity_iteration.mio_icif.energy.EnergyUnit;

import com.google.common.primitives.Ints;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * {@code long}-based energy storage capability,
 * full interoperable with the {@link IEnergyStorage} capability.
 */
@SuppressWarnings("null")
public interface ILongEnergyStorage extends IEnergyStorage {
    /**
     * Conversion ratio: 1 EU = 4 FE.
     */
    int FE_PER_EU = 4;
    /**
     * The standard block capability for {@link ILongEnergyStorage}.
     * Will seamlessly adapt to and from {@link net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage#BLOCK}.
     */
    BlockCapability<ILongEnergyStorage, @Nullable Direction> BLOCK = BlockCapability.createSided(
            EUApi.id("long_energy_storage"), ILongEnergyStorage.class);

    /**
     * The standard entity capability for {@link ILongEnergyStorage}.
     * Will seamlessly adapt to and from {@link net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage#ENTITY}.
     */
    EntityCapability<ILongEnergyStorage, @Nullable Direction> ENTITY = EntityCapability.createSided(
            EUApi.id("long_energy_storage"), ILongEnergyStorage.class);

    /**
     * The standard item capability for {@link ILongEnergyStorage}.
     * Will seamlessly adapt to and from {@link net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage#ITEM}.
     */
    ItemCapability<ILongEnergyStorage, Void> ITEM = ItemCapability.createVoid(
            EUApi.id("long_energy_storage"), ILongEnergyStorage.class);

    /**
     * Wraps an existing {@link IEnergyStorage} into an {@link ILongEnergyStorage}.
     */
    @Contract("null -> null;!null -> !null")
    static @Nullable ILongEnergyStorage of(@Nullable IEnergyStorage energyStorage) {
        return energyStorage == null ? null : new NonLongWrapper(energyStorage);
    }

    /**
     * Receives some energy.
     *
     * @param maxReceive the maximum amount to receive
     * @param simulate {@code true} to simulate, {@code false} to actually perform the operation
     * @return the amount of energy received
     */
    long receive(long maxReceive, boolean simulate);

    /**
     * Extracts some energy.
     *
     * @param maxExtract the maximum amount to extract
     * @param simulate {@code true} to simulate, {@code false} to actually perform the operation
     * @return the amount of energy extracted
     */
    long extract(long maxExtract, boolean simulate);

    /**
     * Returns the amount of energy that this storage currently holds.
     */
    long getAmount();

    /**
     * Returns the maximum amount of energy that this storage can hold.
     */
    long getCapacity();

    // Default implementations below, do not override!

    @Override
    @ApiStatus.NonExtendable
    @Deprecated
    default int receiveEnergy(int maxReceive, boolean simulate) {
        long euToReceive = maxReceive / FE_PER_EU;
        long euReceived = receive(euToReceive, simulate);
        return Ints.saturatedCast(euReceived * FE_PER_EU);
    }

    @Override
    @ApiStatus.NonExtendable
    @Deprecated
    default int extractEnergy(int maxExtract, boolean simulate) {
        long euToExtract = maxExtract / FE_PER_EU;
        long euExtracted = extract(euToExtract, simulate);
        return Ints.saturatedCast(euExtracted * FE_PER_EU);
    }

    @Override
    @ApiStatus.NonExtendable
    @Deprecated
    default int getEnergyStored() {
        return Ints.saturatedCast(getAmount() * FE_PER_EU);
    }

    @Override
    @ApiStatus.NonExtendable
    @Deprecated
    default int getMaxEnergyStored() {
        return Ints.saturatedCast(getCapacity() * FE_PER_EU);
    }

    @Override
    @ApiStatus.NonExtendable
    @Deprecated // don't call this method on an ILongEnergyStorage
    default boolean canExtract() {
        return true;
    }

    @Override
    @ApiStatus.NonExtendable
    @Deprecated // don't call this method on an ILongEnergyStorage
    default boolean canReceive() {
        return true;
    }
}

