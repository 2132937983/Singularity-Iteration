// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.reactor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Public SI metadata. Concrete items own their durability and thermal data. */
public interface IBaseReactorComponent {
    default boolean canBePlacedIn(ItemStack stack,IReactor reactor){return !stack.isEmpty()&&stack.getItem()==this&&reactor!=null;}
    default int getNumberOfCells(){return isFuelRod()?1:0;}
    ReactorComponentType getComponentType();
    default int getNeutronPulseOutput(){return 0;}
    default int getHeatOutput(){return 0;}
    default int getMaxHeatStorage(){return 0;}
    default int getHeatTransferEfficiency(){return 0;}
    default boolean isMoxFuel(){return false;}
    default boolean isFuelRod(){return getComponentType()==ReactorComponentType.FUEL_ROD;}
    default boolean isDepleted(ItemStack stack){return stack.isEmpty();}
    default Item getDepletedItem(){return null;}
}
