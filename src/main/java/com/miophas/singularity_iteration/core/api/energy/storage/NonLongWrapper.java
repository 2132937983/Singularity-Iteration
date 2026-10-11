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
package com.miophas.singularity_iteration.core.api.energy.storage;

import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record NonLongWrapper(IEnergyStorage storage) implements ILongEnergyStorage {
    /**
     * EU view of an FE storage. Contract: the EU returned is exactly the energy moved, in whole EU.
     * Both directions quote first and then move a multiple of {@link #FE_PER_EU} FE, so a foreign
     * storage that takes 1..3 FE neither makes free EU (receive) nor loses FE (extract). The FE
     * request is capped before the multiplication, so Long.MAX_VALUE ("as much as possible")
     * cannot overflow to a negative request.
     */
    @Override
    public long receive(long maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;
        int request = (int) Math.min(maxReceive, Integer.MAX_VALUE / FE_PER_EU) * FE_PER_EU;
        int whole = storage.receiveEnergy(request, true) / FE_PER_EU * FE_PER_EU;
        if (whole <= 0) return 0;
        return (simulate ? whole : storage.receiveEnergy(whole, false)) / FE_PER_EU;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;
        int request = (int) Math.min(maxExtract, Integer.MAX_VALUE / FE_PER_EU) * FE_PER_EU;
        int whole = storage.extractEnergy(request, true) / FE_PER_EU * FE_PER_EU;
        if (whole <= 0) return 0;
        return (simulate ? whole : storage.extractEnergy(whole, false)) / FE_PER_EU;
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

