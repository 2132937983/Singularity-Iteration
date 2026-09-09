package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.api.item.IFluidPort;

import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.Menu.Generator.ReactorFluidPortMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 流体核反应堆流体端口方块实体
 *
 * 功能
 * - 用于流体核反应堆多方块结构中作为流体访问接口
 * - 提供 GUI 用于管理流体输入输出
 * - 包含一个空闲槽用于放置插件物品（未来扩展）
 * - 与反应堆核心进行交互，处理冷却剂和热冷却剂的输入输出
 * - 实现 IFluidHandler 接口，允许外部管道连
 */
@SuppressWarnings("null")
public class mio_icif_reactor_fluid_port extends BlockEntity implements MenuProvider, IFluidHandler, IFluidPort {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(1)
        .build();

    // 插件槽位（用于未来扩展）
    private final MachineItemHandler itemHandler = new MachineItemHandler(LAYOUT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public mio_icif_reactor_fluid_port(BlockPos pos, BlockState state) {
        super(com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.reactor_fluid_port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorFluidPortMenu(containerId, playerInventory, this);
    }

    /**
     * 
tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_reactor_fluid_port fluidPort) {
        if (level.isClientSide()) {
            return;
        }

        // 流体端口
    }

    /**
     * 获取物品处理
 */
    public MachineItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 检查是否有插件物品
     */
    public boolean hasPlugin() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    /**
     * 获取插件物品
     */
    public ItemStack getPlugin() {
        return itemHandler.getStackInSlot(0);
    }

    /**
     * 获取关联的核反应
 */
    @Nullable
    public com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator getReactor() {
        if (level == null) return null;

        // 
    for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    if (level.getBlockEntity(checkPos) instanceof com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_nuclear_reactor_generator reactor) {
                        return reactor;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取反应堆的流体处理
 */
    @Nullable
    public com.singularity_iteration.mio_icif.Blocks.entity.generator.mio_icif_fluid_reactor_handler getFluidHandler() {
        var reactor = getReactor();
        if (reactor != null) {
            return reactor.getFluidHandler();
        }
        return null;
    }

    // ==================== IFluidHandler 实现 ====================

    @Override
    public int getTanks() {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.getTanks();
        }
        return 0;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.getFluidInTank(tank);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.getTankCapacity(tank);
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.isFluidValid(tank, stack);
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.fill(resource, action);
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.drain(resource, action);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        var handler = getFluidHandler();
        if (handler != null) {
            return handler.drain(maxDrain, action);
        }
        return FluidStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }
}