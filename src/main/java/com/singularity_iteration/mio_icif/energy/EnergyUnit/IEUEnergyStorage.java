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

/**
 * EU Energy Storage interface for Industrial Craft In Future.
 * Ported from Modern Industrialization's MIEnergyStorage.
 * 
 * <p>This interface extends GrandPower's ILongEnergyStorage to provide
 * EU (Energy Unit) based energy storage with cable tier support.
 */
public interface IEUEnergyStorage extends ILongEnergyStorage {
    
    /**
     * Checks if this energy storage can connect to a cable of the given tier.
     * 
     * @param cableTier The tier of the cable trying to connect
     * @return true if connection is allowed
     */
    boolean canConnect(CableTier cableTier);

    /**
     * Overload of {@link #canConnect(CableTier)} that's easier to access by reflection.
     * 
     * @param cableTier The name of the cable tier (e.g., "lv", "mv", "hv", "ev", "iv")
     * @return true if connection is allowed
     */
    default boolean canConnect(String cableTier) {
        return switch (cableTier) {
            case "lv" -> canConnect(CableTier.LV);
            case "mv" -> canConnect(CableTier.MV);
            case "hv" -> canConnect(CableTier.HV);
            case "ev" -> canConnect(CableTier.EV);
            case "iv" -> canConnect(CableTier.IV);
            case "luv" -> canConnect(CableTier.LuV);
            case "zpmv" -> canConnect(CableTier.ZPMV);
            case "uv" -> canConnect(CableTier.UV);
            case "uhv" -> canConnect(CableTier.UHV);
            case "uev" -> canConnect(CableTier.UEV);
            case "uiv" -> canConnect(CableTier.UIV);
            case "uxv" -> canConnect(CableTier.UXV);
            case "opv" -> canConnect(CableTier.OpV);
            case "max" -> canConnect(CableTier.MAX);
            default -> false;
        };
    }

    default long generateEnergy(long amount, boolean simulate) {
        long toGenerate = Math.min(amount, getCapacity() - getAmount());
        if (!simulate && toGenerate > 0) {
            setStored(getAmount() + toGenerate);
        }
        return toGenerate;
    }

    default long useEnergy(long amount, boolean simulate) {
        long toUse = Math.min(amount, getAmount());
        if (!simulate && toUse > 0) {
            setStored(getAmount() - toUse);
        }
        return toUse;
    }

    default void setStored(long amount) {
        throw new UnsupportedOperationException("setStored not supported");
    }

    /**
     * Interface for energy storages that cannot extract energy.
     */
    interface NoExtract extends IEUEnergyStorage {
        @Override
        default boolean canExtract() {
            return false;
        }

        @Override
        default long extract(long maxExtract, boolean simulate) {
            return 0;
        }
    }

    /**
     * Interface for energy storages that cannot receive energy.
     */
    interface NoInsert extends IEUEnergyStorage {
        @Override
        default boolean canReceive() {
            return false;
        }

        @Override
        default long receive(long amount, boolean simulate) {
            return 0;
        }
    }
}