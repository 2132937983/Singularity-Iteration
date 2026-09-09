package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Menu.Generator.QuantumSolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 量子太阳能发电机 (Quantum Solar Panel)
 *
 * 配置参数:
 * - 白天发电: 4096 EU/t
 * - 夜晚发电: 2048 EU/t
 * - 能量存储: 10,000,000 EU
 * - 电压等级: IV (8192 EU/t)
 */
@SuppressWarnings("null")
public class mio_icif_QuantumSolarPanel extends mio_icif_AdvancedSolarGenerator {

    public static final int DAY_POWER = 4096;
    public static final int NIGHT_POWER = 2048;
    public static final long CAPACITY = 10000000L;
    public static final int TIER = 5; // IV
    public static final int SLOT_COUNT = 4; // 4个电池槽

    public mio_icif_QuantumSolarPanel(BlockPos pos, BlockState state) {
        super(pos, state, DAY_POWER, NIGHT_POWER, CAPACITY, TIER, mio_icif_block_entities.QUANTUM_SOLAR_PANEL_ENTITY_TYPE.get());
        // 重新初始化ItemHandler以支持4个槽位
        this.slotLayout = SlotLayout.builder().extra(4).build();
        this.itemHandler = createItemHandler(slotLayout);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new QuantumSolarPanelMenu(id, playerInventory, this);
    }
}