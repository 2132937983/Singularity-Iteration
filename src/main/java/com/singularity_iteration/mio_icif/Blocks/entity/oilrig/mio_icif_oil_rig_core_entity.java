package com.singularity_iteration.mio_icif.Blocks.entity.oilrig;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.OilRig.mio_icif_block_oil_rig_panel;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.IMultiblockStructure;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyCore;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_multiblock_manager;
import com.singularity_iteration.mio_icif.multiblock.mio_icif_oil_rig_validator;
import com.singularity_iteration.mio_icif.Menu.OilRig.OilRigPanelMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_oil_rig_core_entity extends MultiblockEnergyCore {

    private static final long DEFAULT_CAPACITY = 50000L;
    private static final ICableTier TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("hv");

    private static final long ENERGY_PER_DRILL = 1000L;
    private static final long ENERGY_PER_IDLE = 500L;
    private static final int OIL_PER_DRILL = 100;

    @Nullable
    private IMultiblockStructure multiblockStructure;

    private int tickCounter = 0;
    private boolean needsRevalidation = false;

    private int offsetY = 0;
    private int offsetFlag = 0;
    private boolean isRigFinish = false;
    private boolean haveEnergy = false;

    private BlockPos drillCoord = BlockPos.ZERO;

    public mio_icif_oil_rig_core_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.OIL_RIG_CORE.get(), DEFAULT_CAPACITY, 0, 0, TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_oil_rig_core_entity blockEntity) {
        if (level.isClientSide()) return;

        if (blockEntity.needsRevalidation) {
            blockEntity.needsRevalidation = false;
            blockEntity.revalidateAfterLoad(level, pos);
        }

        blockEntity.tickCounter++;
        if (blockEntity.tickCounter % 25 == 0) {
            blockEntity.checkStructureComplete(level, pos);
        }

        if (blockEntity.tickCounter % 45 == 0 && blockEntity.isStructureComplete()) {
            blockEntity.tryOperate(level, pos);
        }
    }

    private void revalidateAfterLoad(Level level, BlockPos pos) {
        mio_icif_multiblock_manager<?> existing =
            mio_icif_multiblock_manager.getStructureByController(level, pos);
        if (existing != null && existing.isValid()) {
            return;
        }

        mio_icif_oil_rig_validator validator = new mio_icif_oil_rig_validator();
        mio_icif_multiblock_manager<mio_icif_oil_rig_validator> manager =
            new mio_icif_multiblock_manager<>(pos, validator);
        if (manager.tryForm(level)) {
            setChanged();
        } else {
            onMultiblockBroken();
        }
    }

    private void checkStructureComplete(Level level, BlockPos pos) {
        mio_icif_multiblock_manager<?> manager =
            mio_icif_multiblock_manager.getStructureByController(level, pos);
        if (manager != null) {
            if (!manager.validateStructure(level)) {
                onMultiblockBroken();
            }
        } else {
            mio_icif_oil_rig_validator validator = new mio_icif_oil_rig_validator();
            mio_icif_multiblock_manager<mio_icif_oil_rig_validator> newManager =
                new mio_icif_multiblock_manager<>(pos, validator);
            if (newManager.tryForm(level)) {
                setChanged();
            } else {
                onMultiblockBroken();
            }
        }
    }

    private void tryOperate(Level level, BlockPos pos) {
        List<mio_icif_oil_rig_input_entity> inputs = findModules(level, pos, mio_icif_oil_rig_input_entity.class);
        List<mio_icif_oil_rig_output_entity> outputs = findModules(level, pos, mio_icif_oil_rig_output_entity.class);

        if (inputs.isEmpty() || outputs.isEmpty()) {
            setStructureComplete(false);
            updatePanelInfo(level, pos, false);
            return;
        }

        long totalEnergy = 0;
        for (mio_icif_oil_rig_input_entity input : inputs) {
            totalEnergy += input.getStoredEnergy();
        }

        if (totalEnergy >= ENERGY_PER_DRILL) {
            int availableSpace = 0;
            for (mio_icif_oil_rig_output_entity output : outputs) {
                availableSpace += output.fill(new FluidStack(mio_icif_fluids.CRUDEOIL.get(), OIL_PER_DRILL), IFluidHandler.FluidAction.SIMULATE);
            }
            if (availableSpace <= 0) {
                haveEnergy = true;
                updatePanelInfo(level, pos, false);
                return;
            }
            boolean drilled = tryDrill(level, pos);
            if (drilled) {
                int remaining = OIL_PER_DRILL;
                for (mio_icif_oil_rig_output_entity output : outputs) {
                    if (remaining <= 0) break;
                    int amount = output.fill(new FluidStack(mio_icif_fluids.CRUDEOIL.get(), remaining), IFluidHandler.FluidAction.EXECUTE);
                    remaining -= amount;
                }
                consumeEnergyFromInputs(inputs, ENERGY_PER_DRILL);
            } else {
                consumeEnergyFromInputs(inputs, ENERGY_PER_IDLE);
            }
            updatePanelInfo(level, pos, true);
            haveEnergy = true;
        } else {
            haveEnergy = false;
            updatePanelInfo(level, pos, false);
        }
    }

    private void consumeEnergyFromInputs(List<mio_icif_oil_rig_input_entity> inputs, long amount) {
        long remaining = amount;
        for (mio_icif_oil_rig_input_entity input : inputs) {
            if (remaining <= 0) break;
            long consumed = input.consumeEnergy(remaining, false);
            remaining -= consumed;
        }
    }

    private boolean tryDrill(Level level, BlockPos pos) {
        if (offsetFlag >= 45) {
            isRigFinish = true;
            return false;
        }

        int[] offsets = getOffsetCoord(offsetFlag);
        int ox = offsets[0];
        int oz = offsets[1];

        BlockPos targetPos = pos.offset(ox, -2 - offsetY, oz);
        drillCoord = targetPos;

        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.isAir()) {
            offsetY++;
            if (targetState.is(Blocks.BEDROCK)) {
                offsetY = 0;
                offsetFlag++;
                return false;
            }

            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2);

            if (isOreBlock(targetState) || targetState.is(Blocks.LAVA)) {
                return Math.random() < 0.8;
            } else if (targetState.is(Blocks.OBSIDIAN)) {
                return Math.random() < 0.95;
            } else if (targetState.is(Blocks.GRAVEL) || targetState.is(Blocks.SAND)) {
                return Math.random() < 0.1;
            } else {
                return Math.random() < 0.2;
            }
        }
        offsetY++;
        return false;
    }

    private int[] getOffsetCoord(int flag) {
        int scale = flag / 9 + 1;
        int idx = flag % 9;
        int[][] base = {
            {0, 0}, {1, 0}, {-1, 0},
            {0, 1}, {0, -1}, {1, 1},
            {1, -1}, {-1, 1}, {-1, -1}
        };
        return new int[]{base[idx][0] * scale, base[idx][1] * scale};
    }

    private boolean isOreBlock(BlockState state) {
        String path = state.getBlock().getDescriptionId();
        return path.contains("ore") || path.contains("Ore");
    }

    private <T extends BlockEntity> List<T> findModules(Level level, BlockPos pos, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 0}, {0, 2, 0}
        };
        for (int[] offset : offsets) {
            BlockPos checkPos = pos.offset(offset[0], offset[1], offset[2]);
            BlockEntity be = level.getBlockEntity(checkPos);
            if (clazz.isInstance(be)) {
                result.add(clazz.cast(be));
            }
        }
        return result;
    }

    private void updatePanelInfo(Level level, BlockPos pos, boolean isWorking) {
        List<mio_icif_oil_rig_panel_entity> panels = findModules(level, pos, mio_icif_oil_rig_panel_entity.class);
        for (mio_icif_oil_rig_panel_entity panel : panels) {
            panel.updateInfo(drillCoord, getCoreState());
            BlockPos panelPos = panel.getBlockPos();
            BlockState panelState = level.getBlockState(panelPos);
            if (panelState.hasProperty(mio_icif_block_oil_rig_panel.LIT)) {
                boolean currentLit = panelState.getValue(mio_icif_block_oil_rig_panel.LIT);
                if (currentLit != isWorking) {
                    level.setBlock(panelPos, panelState.setValue(mio_icif_block_oil_rig_panel.LIT, isWorking), 3);
                }
            }
        }
    }

    public BlockPos getDrillCoordinate() {
        return drillCoord;
    }

    public int getCoreState() {
        if (!isStructureComplete()) return 1;
        if (!haveEnergy) return -1;
        if (isRigFinish) return 2;
        return 3;
    }

    @Override
    public void onMultiblockFormed(IMultiblockStructure structure) {
        this.multiblockStructure = structure;
        this.setStructureComplete(true);
        if (level != null && !level.isClientSide()) {
            updatePanelInfo(level, worldPosition, false);
        }
        setChanged();
    }

    @Override
    public void onMultiblockBroken() {
        this.multiblockStructure = null;
        this.setStructureComplete(false);
        if (level != null && !level.isClientSide()) {
            updatePanelInfo(level, worldPosition, false);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.oil_rig_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OilRigPanelMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("offsetY", offsetY);
        tag.putInt("offsetFlag", offsetFlag);
        tag.putBoolean("isRigFinish", isRigFinish);
        tag.putBoolean("haveEnergy", haveEnergy);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("offsetY")) offsetY = tag.getInt("offsetY");
        if (tag.contains("offsetFlag")) offsetFlag = tag.getInt("offsetFlag");
        if (tag.contains("isRigFinish")) isRigFinish = tag.getBoolean("isRigFinish");
        if (tag.contains("haveEnergy")) haveEnergy = tag.getBoolean("haveEnergy");
        if (isStructureComplete()) {
            needsRevalidation = true;
        }
    }
}