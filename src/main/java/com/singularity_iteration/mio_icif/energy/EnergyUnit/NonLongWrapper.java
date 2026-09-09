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
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record NonLongWrapper(IEnergyStorage storage) implements ILongEnergyStorage {
    @Override
    public long receive(long maxReceive, boolean simulate) {
        int feToReceive = Ints.saturatedCast(maxReceive * FE_PER_EU);
        int feReceived = storage.receiveEnergy(feToReceive, simulate);
        return feReceived / FE_PER_EU;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        int feToExtract = Ints.saturatedCast(maxExtract * FE_PER_EU);
        int feExtracted = storage.extractEnergy(feToExtract, simulate);
        return feExtracted / FE_PER_EU;
    }

    @Override
    public long getAmount() {
        return storage.getEnergyStored() / FE_PER_EU;
    }

    @Override
    public long getCapacity() {
        return storage.getMaxEnergyStored() / FE_PER_EU;
    }

    @Override
    public boolean canExtract() {
        return storage.canExtract();
    }

    @Override
    public boolean canReceive() {
        return storage.canReceive();
    }

    @Override
    public String toString() {
        return "NonLongWrapper[" + storage.toString() + "]";
    }
}

