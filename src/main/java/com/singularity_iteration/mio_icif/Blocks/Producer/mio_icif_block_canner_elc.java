package com.singularity_iteration.mio_icif.Blocks.Producer;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_canner_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_entity_block;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 装罐机方块类
 * 用于将流体装入容器或从容器中取出流体的机器方?
 */
@SuppressWarnings("null")
public class mio_icif_block_canner_elc extends mio_icif_entity_block {

    // 运行状态属性，用于控制方块的光照和纹理变化
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    // 方块编码器，用于数据生成和序列化
    public static final MapCodec<mio_icif_block_canner_elc> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(propertiesCodec()).apply(instance, mio_icif_block_canner_elc::new));

    public mio_icif_block_canner_elc(Properties properties) {
        super(properties);
        // 注册默认状态：未点?
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        // 添加 LIT 属性到方块状态定?
        builder.add(LIT);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 使用模型渲染
        return RenderShape.MODEL;
    }

    /**
     * 判断点击位置是否在方块的左侧（localX < 0.5）
     * 左侧 = 输入流体槽，右侧 = 输出流体槽
     */
    private boolean isLeftSide(BlockHitResult hitResult, BlockPos pos) {
        Vec3 hitLocation = hitResult.getLocation();
        double localX = hitLocation.x - pos.getX();
        return localX < 0.5;
    }

    /**
     * 检查物品是否是空容器（空单元、或其他空流体容器）
     */
    private boolean isEmptyContainer(ItemStack stack) {
        if (mio_icif_cells.isEmptyCell(stack)) {
            return true;
        }

        var containedOpt = FluidUtil.getFluidContained(stack);
        if (containedOpt.isEmpty() || containedOpt.get().isEmpty()) {
            return FluidUtil.getFluidHandler(stack).isPresent();
        }

        return false;
    }

    /**
     * 玩家手持物品右键点击方块时的处理
     * 蹲下 + 点击左侧(localX<0.5) → 操作输入流体槽
     * 蹲下 + 点击右侧(localX>=0.5) → 操作输出流体槽
     * 不蹲下 → 打开 GUI
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        System.out.println("[CannerDebug] useItemOn called: player=" + player.getName().getString()
                + " shift=" + player.isShiftKeyDown()
                + " stack=" + stack.getItem().getDescriptionId()
                + " isCell=" + mio_icif_cells.isFluidCell(stack));

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof mio_icif_canner_elc canner)) {
            System.out.println("[CannerDebug] Not a canner entity, passing");
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 蹲下 + 手持流体容器(含单元) → 根据点击位置判断左右侧，执行舀出或灌入
        if (player.isShiftKeyDown() && !stack.isEmpty()) {
            System.out.println("[CannerDebug] Player is shifting and holding item");

            // 根据点击位置判断左右侧
            boolean leftSide = isLeftSide(hitResult, pos);
            FluidTank targetTank = leftSide ? canner.getInputFluidTank() : canner.getOutputFluidTank();

            System.out.println("[CannerDebug] Hit side: " + (leftSide ? "LEFT(input)" : "RIGHT(output)"));

            // ========== 舀出逻辑：手持空容器 → 从水槽舀出液体 ==========
            if (isEmptyContainer(stack) && targetTank.getFluidAmount() >= 1000) {
                FluidStack fluidInTank = targetTank.getFluid();
                if (!fluidInTank.isEmpty()) {
                    System.out.println("[CannerDebug] Scooping fluid from " + (leftSide ? "input" : "output") + " tank");

                    // 特殊处理：空单元 → 替换为对应填充单元
                    if (mio_icif_cells.isEmptyCell(stack)) {
                        Fluid fluidType = fluidInTank.getFluid();
                        ItemStack filledCell = mio_icif_cells.getFilledCellForFluidStack(fluidType);
                        if (!filledCell.isEmpty()) {
                            int cellsToFill = Math.min(stack.getCount(), targetTank.getFluidAmount() / 1000);
                            if (cellsToFill > 0) {
                                targetTank.drain(cellsToFill * 1000, IFluidHandler.FluidAction.EXECUTE);
                                canner.setChanged();

                                if (!player.getAbilities().instabuild) {
                                    if (stack.getCount() == 1) {
                                        player.setItemInHand(hand, filledCell);
                                    } else {
                                        stack.shrink(cellsToFill);
                                        ItemStack filledCells = filledCell.copyWithCount(cellsToFill);
                                        if (!player.getInventory().add(filledCells)) {
                                            player.drop(filledCells, false);
                                        }
                                    }
                                }
                                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                                System.out.println("[CannerDebug] Scoop success: " + cellsToFill + " cell(s) filled");
                                return ItemInteractionResult.SUCCESS;
                            }
                        }
                    }

                    // 其他流体容器 → 通过 FluidUtil 填充
                    {
                        FluidStack toDrain = new FluidStack(fluidInTank.getFluid(), 1000);
                        var handlerOpt = FluidUtil.getFluidHandler(stack);
                        if (handlerOpt.isPresent()) {
                            var handler = handlerOpt.get();
                            int filled = handler.fill(toDrain, IFluidHandler.FluidAction.EXECUTE);
                            if (filled >= 1000) {
                                targetTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                                canner.setChanged();

                                if (!player.getAbilities().instabuild) {
                                    if (stack.getCount() == 1) {
                                        player.setItemInHand(hand, handler.getContainer());
                                    } else {
                                        stack.shrink(1);
                                        if (!player.getInventory().add(handler.getContainer())) {
                                            player.drop(handler.getContainer(), false);
                                        }
                                    }
                                }
                                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                                System.out.println("[CannerDebug] Scoop success: container filled");
                                return ItemInteractionResult.SUCCESS;
                            }
                        }
                    }

                    System.out.println("[CannerDebug] Scoop failed: could not fill container");
                }
            }

            // ========== 灌入逻辑：手持有流体的容器 → 注入对应水槽 ==========
            FluidStack fluidToFill = null;
            ItemStack containerAfterFill = ItemStack.EMPTY;

            if (mio_icif_cells.isFluidCell(stack)) {
                FluidStack cellFluid = mio_icif_cells.getCellFluid(stack);
                if (cellFluid != null && !cellFluid.isEmpty()) {
                    fluidToFill = cellFluid.copy();
                    containerAfterFill = mio_icif_cells.getEmptyCellForStack(stack);
                    if (containerAfterFill.isEmpty()) containerAfterFill = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                }
            }

            // 非单元物品，通过NeoForge流体能力系统检测
            if (fluidToFill == null) {
                var fluidOpt = FluidUtil.getFluidContained(stack);
                if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) {
                    fluidToFill = fluidOpt.get();
                    var handlerOpt = FluidUtil.getFluidHandler(stack);
                    if (handlerOpt.isPresent()) {
                        var handler = handlerOpt.get();
                        handler.drain(fluidToFill, IFluidHandler.FluidAction.EXECUTE);
                        containerAfterFill = handler.getContainer();
                    }
                }
            }

            if (fluidToFill != null && !fluidToFill.isEmpty()) {
                int canFill = targetTank.fill(fluidToFill, IFluidHandler.FluidAction.SIMULATE);
                if (canFill > 0) {
                    targetTank.fill(new FluidStack(fluidToFill.getFluid(), canFill), IFluidHandler.FluidAction.EXECUTE);
                    canner.setChanged();

                    if (!player.getAbilities().instabuild) {
                        if (stack.getCount() == 1) {
                            player.setItemInHand(hand, containerAfterFill);
                        } else {
                            stack.shrink(1);
                            if (!player.getInventory().add(containerAfterFill)) {
                                player.drop(containerAfterFill, false);
                            }
                        }
                    }

                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    System.out.println("[CannerDebug] Fill success: " + canFill + "mb into " + (leftSide ? "input" : "output") + " tank");
                    return ItemInteractionResult.SUCCESS;
                }
                System.out.println("[CannerDebug] Fill failed: tank cannot accept fluid or is full");
                return ItemInteractionResult.FAIL;
            }
        }

        // 其他情况打开GUI
        MenuProvider menuProvider = new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.CannerElcMenu(containerId, playerInventory, canner),
            Component.translatable("container.mio_icif.canner_elc")
        );
        player.openMenu(menuProvider);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * 玩家右键点击方块时的处理
     * 打开 GUI 界面
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_canner_elc canner) {
                MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, playerEntity) -> new com.singularity_iteration.mio_icif.Menu.Producer.CannerElcMenu(containerId, playerInventory, canner),
                    Component.translatable("container.mio_icif.canner_elc")
                );
                player.openMenu(menuProvider);
            } else {
                player.sendSystemMessage(Component.literal("This block does not have a GUI!"));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 方块被移除时的处?
     * 掉落方块实体中的物品
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof mio_icif_canner_elc canner) {
                // 掉落物品栏中的所有物?
                for (int i = 0; i < canner.getItemHandler().getSlots(); i++) {
                    ItemStack stack = canner.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    /**
     * 是否有模拟输出信号（用于红石比较器）
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * 获取模拟输出信号强度（用于红石比较器）
     * 根据机器的工作进度输出信号强度
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof mio_icif_canner_elc canner) {
            // 根据流体槽的填充程度输出信号
            int inputAmount = canner.getInputFluidAmount();
            int outputAmount = canner.getOutputFluidAmount();
            int totalFluid = inputAmount + outputAmount;
            int maxFluid = canner.getInputFluidCapacity() + canner.getOutputFluidCapacity();
            return Math.min(15, (totalFluid * 15) / Math.max(1, maxFluid));
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, mio_icif_block_entities.CANNER_ELC_ENTITY_TYPE.get(),
            (world, pos, state1, blockEntity) -> mio_icif_canner_elc.tick(world, pos, state1, blockEntity));
    }
}