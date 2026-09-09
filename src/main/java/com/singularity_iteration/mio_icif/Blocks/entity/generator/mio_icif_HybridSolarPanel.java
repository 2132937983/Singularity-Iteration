package com.singularity_iteration.mio_icif.Blocks.entity.generator;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Menu.Generator.HybridSolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 混合太阳能发电机 (Hybrid Solar Panel)
 *
 * 配置参数参考 ASP:
 * - 白天发电: 64 EU/t
 * - 夜晚发电: 8 EU/t
 * - 能量存储: 100,000 EU
 * - 电压等级: MV (2)
 */
@SuppressWarnings("null")
public class mio_icif_HybridSolarPanel extends mio_icif_AdvancedSolarGenerator {

    public static final int DAY_POWER = 64;
    public static final int NIGHT_POWER = 8;
    public static final long CAPACITY = 100000L;
    public static final int TIER = 2; // MV
    public static final int SLOT_COUNT = 4; // 4个电池槽

    public mio_icif_HybridSolarPanel(BlockPos pos, BlockState state) {
        super(pos, state, DAY_POWER, NIGHT_POWER, CAPACITY, TIER, mio_icif_block_entities.HYBRID_SOLAR_PANEL_ENTITY_TYPE.get());
        // 重新初始化ItemHandler以支持4个槽位
        this.slotLayout = SlotLayout.builder().extra(4).build();
        this.itemHandler = createItemHandler(slotLayout);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new HybridSolarPanelMenu(id, playerInventory, this);
    }
}