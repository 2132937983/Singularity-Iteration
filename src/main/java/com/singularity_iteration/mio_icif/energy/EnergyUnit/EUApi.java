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

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jspecify.annotations.Nullable;

/**
 * EU (Energy Unit) API for Industrial Craft In Future.
 * Ported from Modern Industrialization's EnergyApi.
 * 
 * <p>This API provides EU-based energy storage capabilities compatible with GrandPower.
 */
@SuppressWarnings("null")
public class EUApi {
    
    /**
     * The mod ID for Industrial Craft In Future.
     */
    public static final String MOD_ID = "mio_icif";
    
    /**
     * Creates a ResourceLocation with the ICIF namespace.
     * 
     * @param path The path for the resource location
     * @return A new ResourceLocation
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Sided block capability for EU energy storage.
     */
    public static final BlockCapability<IEUEnergyStorage, @Nullable Direction> SIDED = BlockCapability
            .createSided(id("sided_eu_energy_storage"), IEUEnergyStorage.class);
    
    /**
     * Item capability for EU energy storage (uses GrandPower's ILongEnergyStorage for items).
     */
    public static final ItemCapability<ILongEnergyStorage, Void> ITEM = ItemCapability
            .createVoid(id("eu_energy_storage"), ILongEnergyStorage.class);

    /**
     * Creative energy source with infinite energy.
     */
    public static final IEUEnergyStorage CREATIVE = new IEUEnergyStorage.NoInsert() {
        @Override
        public boolean canConnect(CableTier cableTier) {
            return true;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return maxAmount;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public long getAmount() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getCapacity() {
            return Long.MAX_VALUE;
        }
    };

    /**
     * Empty energy storage with no energy.
     */
    public static final IEUEnergyStorage EMPTY = new EmptyStorage();

    private static class EmptyStorage implements IEUEnergyStorage.NoInsert, IEUEnergyStorage.NoExtract {
        @Override
        public boolean canConnect(CableTier cableTier) {
            return false;
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return 0;
        }
    }

    private EUApi() {
        // Utility class
    }
}

