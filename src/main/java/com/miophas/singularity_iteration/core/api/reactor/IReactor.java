// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** SI compatibility surface reconstructed from its public ABI, without predecessor implementation. */
public interface IReactor {
    Level getLevel();
    BlockPos getBlockPos();
    long getHeat();
    void setHeat(long heat);
    long addHeat(long amount);
    long getMaxHeat();
    void setMaxHeat(long capacity);
    void addEmitHeat(long amount);
    float getHeatEffectModifier();
    void setHeatEffectModifier(float modifier);
    long getReactorEnergyOutput();
    ItemStack getItemAt(int column,int row);
    void setItemAt(int column,int row,ItemStack stack);
    void explode();
    int getTickRate();
    boolean produceEnergy();
    boolean isFluidCooled();
}
