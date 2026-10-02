package com.miophas.singularity_iteration.common.blockentity;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;

import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 火力发电机实体（LV 级）
 * 使用煤炭等燃料发?
 * 使用 EU (Energy Unit) 能量系统
 * LV 级配置：128 EU/tick 输出?4000 EU 容量，发电速率 10 EU/tick
 * 
 * 该发电机通过电网（PowerGrid）分配能量，而非直接向相邻方块输出能量?
 * 电网通过 extractPowerForConsumer 从发电机?energyStorage 提取能量?
 */
@SuppressWarnings("null")
public class mio_icif_Thermal_Generator extends AbstractGeneratorBlockEntity {
    
    // LV 级：对齐 IC2 原版，容纳?4000 EU，发电速率 10 EU/tick，最大输?32 EU/tick
    public mio_icif_Thermal_Generator(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.THERMAL_GENERATOR_ENTITY_TYPE.get(),
            SlotLayout.builder().input(1).battery().build(), 10L, 4000L, 0L, 32L, CableTier.LV);
    }

    public mio_icif_Thermal_Generator(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            SlotLayout.builder().input(1).battery().build(), 10L, 4000L, 0L, 32L, CableTier.LV);
    }
    
    /**
     * 当方块实体加载时注册到电?
     * 注册由基?AbstractEnergyBlockEntity 通过 EnergyTileLoadEvent 自动处理
     */

    /**
     * 火力发电机通过电网分配能量，不直接向相邻方块输出能量?
     * 由电网通过 extractPowerForConsumer 从发电机的存储中提取能量
     */
    @Override
    protected boolean shouldDirectlyDistributeEnergy() {
        return false;
    }
    
    /**
     * 获取燃料的燃烧时间?
 * 像原版熔一样，根据物品是否可燃的标签对其进行燃烧发?
     * @param fuel 燃料物品
     * @return 燃烧时间（tick），0表示不是燃料
     */
    @Override
    public int getFuelBurnTime(ItemStack fuel) {
        return fuelBurnTime(fuel, energyStorage.scexNetworkControlled());
    }

    /** Shared fuel rule for the machine and its client menu prediction. */
    public static int fuelBurnTime(ItemStack fuel, boolean independentNetwork) {
        if (fuel.isEmpty()) {
            return 0;
        }

        if (independentNetwork && fuel.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            return 0;
        }
        int burnTime = fuel.getBurnTime(null);
        // R12 public game observations: coal/charcoal 400, stick 25, planks 75,
        // carpet 16 and slab 37 ticks. New-platform/addon fuels remain extensions.
        if (independentNetwork) burnTime /= 4;
        return burnTime > 0 ? burnTime : 0;
    }
    
    /**
     * 获取发电机的显示名称
     * @return 显示名称
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.thermal_generator");
    }
    
    /**
     * 创建Menu
     * @param id Menu ID
     * @param playerInventory 玩家物品栈?
     * @param player 玩家
     * @return Menu实例
     */
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.generator.ThermalGeneratorMenu(id, playerInventory, this, this.getItemHandler(), null);
    }
}
