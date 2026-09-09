package com.singularity_iteration.mio_icif.Blocks.entity.oilrig;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Menu.OilRig.OilRigPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_oil_rig_panel_entity extends BlockEntity implements MenuProvider {

    private BlockPos drillCoord = BlockPos.ZERO;
    private int coreState = 1;

    public mio_icif_oil_rig_panel_entity(BlockPos pos, BlockState state) {
        super(mio_icif_block_entities.OIL_RIG_PANEL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_oil_rig_panel_entity blockEntity) {
        if (level.isClientSide()) return;
    }

    public void updateInfo(BlockPos drillCoord, int coreState) {
        this.drillCoord = drillCoord;
        this.coreState = coreState;
        setChanged();
    }

    public BlockPos getDrillCoord() {
        return drillCoord;
    }

    public int getCoreState() {
        return coreState;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.oil_rig_panel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OilRigPanelMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("drillX", drillCoord.getX());
        tag.putLong("drillY", drillCoord.getY());
        tag.putLong("drillZ", drillCoord.getZ());
        tag.putInt("coreState", coreState);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("drillX")) {
            drillCoord = new BlockPos(
                tag.getInt("drillX"),
                tag.getInt("drillY"),
                tag.getInt("drillZ")
            );
        }
        if (tag.contains("coreState")) {
            coreState = tag.getInt("coreState");
        }
    }
}