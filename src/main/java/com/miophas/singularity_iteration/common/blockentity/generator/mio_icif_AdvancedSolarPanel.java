package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.menu.generator.AdvancedSolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 高级太阳能发电机 (Advanced Solar Panel)
 *
 * 0.1.11.101（太阳能大修 S1）：A 线聚光光热，无冷却。
 * - 白天发电: 12 EU/t（原 8），只吃直射光，满功率 850 W/m²
 * - 夜晚发电: 0（原 1）
 * - 能量存储: 32,000 EU
 * - 电压等级: LV (1)
 */
@SuppressWarnings("null")
public class mio_icif_AdvancedSolarPanel extends mio_icif_AdvancedSolarGenerator {

    public static final int DAY_POWER = 12;
    public static final int NIGHT_POWER = 0;
    public static final long CAPACITY = 32000L;
    public static final int TIER = 1; // LV
    public static final int SLOT_COUNT = 4; // 4个电池槽
    /** 0.1.11.101: route A, concentrator (see SolarRoutes). */
    public static final com.miophas.singularity_iteration.common.energy.SolarProfile PROFILE =
        com.miophas.singularity_iteration.common.energy.SolarRoutes.ADVANCED;

    public mio_icif_AdvancedSolarPanel(BlockPos pos, BlockState state) {
        super(pos, state, DAY_POWER, NIGHT_POWER, CAPACITY, TIER, mio_icif_block_entities.ADVANCED_SOLAR_PANEL_ENTITY_TYPE.get(), PROFILE);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AdvancedSolarPanelMenu(id, playerInventory, this);
    }
}