package com.miophas.singularity_iteration.common.blockentity.reactor;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;

import com.miophas.singularity_iteration.core.api.item.IFluidPort;
import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;

import com.miophas.singularity_iteration.core.api.upgrade.tile.IUpgradeItem;
import com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty;
import com.miophas.singularity_iteration.core.prefab.component.FluidAutomationComponent;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.upgrade.MachineUpgradeStats;
import com.miophas.singularity_iteration.common.menu.generator.ReactorFluidPortMenu;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 流体核反应堆流体端口方块实体
 *
 * 功能
 * - 用于流体核反应堆多方块结构中作为流体访问接口
 * - 提供 GUI 用于管理流体输入输出
 * - 插件槽可放置流体弹出/流体抽入升级，自动输出热冷却液或输入冷却液
 * - 与反应堆核心进行交互，处理冷却剂和热冷却剂的输入输出
 * - 实现 IFluidHandler 接口，允许外部管道连
 */
@SuppressWarnings("null")
public class mio_icif_reactor_fluid_port extends BlockEntity implements MenuProvider, IFluidHandler, IFluidPort {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(1)
        .build();

    /** 流体端口对外的升级属性集合（对齐 IC2 TileEntityReactorFluidPort#getUpgradableProperties）。 */
    private static final Set<UpgradableProperty> FLUID_PORT_PROPERTIES =
        Set.of(UpgradableProperty.FLUID_CONSUMING, UpgradableProperty.FLUID_PRODUCING);

    // 插件槽位（放置流体弹出/抽入升级插件）
    private final MachineItemHandler itemHandler = new MachineItemHandler(LAYOUT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // 每 tick 依据插件槽重算一次的升级统计（缓存以避免组件内重复解析）
    private MachineUpgradeStats upgradeStats = MachineUpgradeStats.empty();

    // 流体弹出/抽入升级的通用自动化组件（弹出热冷却液 / 抽入冷却液）
    private final FluidAutomationComponent fluidAutomation = new FluidAutomationComponent(
        new FluidAutomationComponent.Host() {
            @Override public Level level() { return level; }
            @Override public BlockPos worldPosition() { return worldPosition; }
            @Override public IFluidHandler ownFluidHandler() { return getFluidHandler(); }
            @Override public IFluidHandler adjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
                return getAdjacentFluidHandler(pos, side);
            }
            @Override public IMachineUpgradeStats upgradeStats() { return upgradeStats; }
            @Override public void markUnsaved() {
                ContainerToTank.markUnsaved(mio_icif_reactor_fluid_port.this);
            }
            @Override public void markNeighborUnsaved(BlockPos pos) {
                FluidAutomationComponent.markNeighborUnsaved(level, pos);
            }
        });

    // 缓存的所连接反应堆位置（由 tick 周期性刷新；多方块可能在端口放置之后才成型）
    @Nullable
    private BlockPos connectedReactorPos;
    private long lastReactorScanTick = -1000L;   // game time is never negative: the first scan runs at once (MIN_VALUE overflowed the age check)

    public mio_icif_reactor_fluid_port(BlockPos pos, BlockState state) {
        super(com.miophas.singularity_iteration.common.registry.mio_icif_block_entities.REACTOR_FLUID_PORT_ENTITY_TYPE.get(), pos, state);
        // 对齐 IC2 TileEntityReactorFluidPort：插件槽只接受声明了流体属性的升级
        itemHandler.setValidator((slot, stack, slotType) -> isSuitablePlugin(stack));
    }

    /** 与 IC2 InvSlotUpgrade#accepts 一致：插件必须声明适用于“流体消耗/流体产出”。 */
    public static boolean isSuitablePlugin(ItemStack stack) {
        return stack.getItem() instanceof IUpgradeItem upgrade && upgrade.isSuitableFor(stack, FLUID_PORT_PROPERTIES);
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
     * tick 更新逻辑
     */
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_reactor_fluid_port fluidPort) {
        if (level.isClientSide()) {
            return;
        }

        // 周期性重新扫描所连接的反应堆（多方块结构可能在端口放置之后才成型）
        fluidPort.refreshConnection(level);
        // 结构有效时确保反应堆处于流体模式，否则管道会连上但传输恒为 0
        fluidPort.ensureFluidMode(level);
        // 刷新插件槽的升级统计；只有装了流体弹出/抽入升级才驱动自动化，避免空槽时的无谓查询
        fluidPort.upgradeStats = MachineUpgradeStats.fromInventory(fluidPort.itemHandler, 0, 1);
        if (fluidPort.upgradeStats.getFluidEjectorCount() > 0 || fluidPort.upgradeStats.getFluidPullingCount() > 0) {
            fluidPort.fluidAutomation.runAutomation();
        }
    }

    /** 周期性刷新缓存的反应堆位置，避免每次 capability 查询都做一次全量 5x5x5 扫描。 */
    private void refreshConnection(Level level) {
        long now = level.getGameTime();
        // no reactor nearby: the 125-block search runs once a second, not every tick
        if (connectedReactorPos == null && now - lastReactorScanTick < 20) return;
        if (now - lastReactorScanTick < 20 && connectedReactorPos != null
                && level.getBlockEntity(connectedReactorPos) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator cached
                && isFluidCooled(cached)) {
            lastReactorScanTick = now;
            return;
        }
        lastReactorScanTick = now;
        connectedReactorPos = findReactor(level);
    }

    /**
     * 在 5x5x5 范围内寻找流体模式的反应堆 —— 严格对齐 IC2 {@code FluidReactorLookup#updateReactor}：
     * 只要求「Chebyshev 距离 ≤ 2」+「反应堆处于流体模式（{@code isFluidCooled()}）」，
     * <b>不要求本端口已被多方块结构登记为成员</b>。
     * <p>
     * 原版每个端口都是把反应堆的 input/output 两个罐（{@code addUnmanagedTankHook}）挂到自己身上，
     * 因此贴在结构外侧、或结构成型之后才补放的端口，同样能读/写同一对共享罐。
     * 此前这里额外要求 {@code structure.isValid() && structure.isPartOfStructure(worldPosition)}，
     * 会让"结构没认下的那部分端口"永远拿不到罐（表现为多个接口里只有一部分能输出热冷却液）。
     */
    @Nullable
    private BlockPos findReactor(Level level) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    if(!level.getChunkSource().hasChunk(checkPos.getX()>>4,checkPos.getZ()>>4))continue;
                    if (level.getBlockEntity(checkPos) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator reactor
                            && isFluidCooled(reactor)) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    /** IC2 {@code TileEntityNuclearReactorElectric#isFluidCooled()} 的等价判定：反应堆处于流体模式。 */
    private static boolean isFluidCooled(com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator reactor) {
        return reactor.getReactorMode() == com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_reactor_mode.FLUID;
    }

    /**
     * 结构有效时确保反应堆处于流体模式。
     * 与此前只有访问接口才做的 ensureFluidMode 保持一致；否则反应堆停在 GENERATOR 模式时，
     * 端口 getTanks()/getTankCapacity() 仍报告有罐，但 fill/drain 会恒返回 0/EMPTY。
     */
    private void ensureFluidMode(Level level) {
        if (connectedReactorPos == null) return;
        if (level.getBlockEntity(connectedReactorPos) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator reactor) {
            var structure = reactor.getFluidReactorMultiblock();
            if (structure != null && structure.isValid()
                    && reactor.getReactorMode() != com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_reactor_mode.FLUID) {
                reactor.setReactorMode(com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_reactor_mode.FLUID);
            }
        }
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
    public com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator getReactor() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)||!server.getServer().isSameThread()||isRemoved())return null;
        var ownChunk=server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        if(ownChunk==null||ownChunk.getBlockEntity(worldPosition,net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK)!=this)return null;

        // 优先使用 tick 缓存的连接；缓存失效（方块没了或不再是流体模式）时才重新扫描
        com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator cached = null;
        if (connectedReactorPos != null
                && level.getBlockEntity(connectedReactorPos) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator reactor) {
            cached = reactor;
        }
        // a stale link is rescanned at most once a second (pipes query several times per tick)
        if ((cached == null || !isFluidCooled(cached)) && level.getGameTime() - lastReactorScanTick >= 20) {
            lastReactorScanTick = level.getGameTime();
            connectedReactorPos = findReactor(level);
        } else if (cached == null || !isFluidCooled(cached)) {
            return null;
        }
        if (connectedReactorPos == null) return null;
        return level.getBlockEntity(connectedReactorPos) instanceof com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_nuclear_reactor_generator reactor ? reactor : null;
    }

    /**
     * 获取反应堆的流体处理
 */
    @Nullable
    public com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_fluid_reactor_handler getFluidHandler() {
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

    /**
     * 获取相邻位置的流体处理器。
     * 跳过本结构内的其它流体端口，避免在同一反应堆内部来回搬运流体。
     */
    @Nullable
    private IFluidHandler getAdjacentFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null || target instanceof mio_icif_reactor_fluid_port) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        fluidAutomation.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        fluidAutomation.load(tag, registries);
    }
}
