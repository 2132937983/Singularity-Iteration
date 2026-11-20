package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.common.menu.generator.QuantumSolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 量子太阳能发电机 (Quantum Solar Panel)
 *
 * 0.1.11.101（太阳能大修 S1）：A 线聚光光热，电池恒为 25 °C。
 * - 白天发电: 7168 EU/t（0.1.11.106：原 6144；0.1.11.100 前为 4096），只吃直射光，满功率 850 W/m²
 * - 夜晚发电: 0（原 1024）
 * - 能量存储: 10,000,000 EU
 * - 电压等级: IV (8192 EU/t)
 */
@SuppressWarnings("null")
public class mio_icif_QuantumSolarPanel extends mio_icif_AdvancedSolarGenerator {

    /** 0.1.11.106: 7168 (was 6144): the balance test found it only 8 % above the ultimate photon resonance panel on a clear day. */
    public static final int DAY_POWER = 7168;
    public static final int NIGHT_POWER = 0;   // 0.1.11.101: route A makes nothing at night (was 1024)
    public static final long CAPACITY = 10000000L;
    public static final int TIER = 5; // IV
    public static final int SLOT_COUNT = 4; // 4个电池槽
    /** 0.1.11.101: route A, concentrator (see SolarRoutes). */
    public static final com.miophas.singularity_iteration.common.energy.SolarProfile PROFILE =
        com.miophas.singularity_iteration.common.energy.SolarRoutes.QUANTUM;

    public mio_icif_QuantumSolarPanel(BlockPos pos, BlockState state) {
        super(pos, state, DAY_POWER, NIGHT_POWER, CAPACITY, TIER, mio_icif_block_entities.QUANTUM_SOLAR_PANEL_ENTITY_TYPE.get(), PROFILE);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new QuantumSolarPanelMenu(id, playerInventory, this);
    }
}