package com.miophas.singularity_iteration.common.item.cell;

import com.miophas.singularity_iteration.core.api.item.IFluidCellItem;
import com.miophas.singularity_iteration.core.prefab.fluid.FluidContainerInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;

@SuppressWarnings("null")
public class mio_icif_cell extends Item implements IFluidCellItem {

    private final Fluid content;
    private static final int CAPACITY = 1000; // 1000mb = 1 bucket
    private static final String FLUID_KEY = "fluid";
    private static final String AMOUNT_KEY = "amount";
    /**
     * 显式“已排空”标记。
     *
     * <p>静态单元在无 NBT 时会被视为构造时绑定的满态（新造物品 / 旧存档兼容），
     * 因此排空后必须写入该标记，否则 {@link #readFluidFromNBT} 会回退成满态，
     * 导致 drain 永远排不空 —— 右键储罐 / 发射器 / 流体能力调用都会因此复制流体。
     */
    private static final String EMPTY_KEY = "empty";

    public mio_icif_cell(Item.Properties properties, Fluid content) {
        super(properties);
        this.content = content;
    }

    public Fluid getContent() {
        return this.content;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, this.getFluid(itemStack).isEmpty() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemStack);
        } else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        } else {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getDirection();
            BlockPos blockPos2 = blockPos.relative(direction);
            
            if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos2, direction, itemStack)) {
                return InteractionResultHolder.fail(itemStack);
            } else if (this.getFluid(itemStack).isEmpty()) {
                // 空单元：尝试从世界中吸取流体
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BucketPickup bucketPickup) {
                    Fluid fluidType = blockState.getFluidState().getType();
                    Item filledCellItem = mio_icif_cells.getFilledCellForFluid(fluidType);
                    if (filledCellItem != null) {
                        ItemStack pickedUp = bucketPickup.pickupBlock(player, level, blockPos, blockState);
                        if (!pickedUp.isEmpty()) {
                            ItemStack resultStack = new ItemStack(filledCellItem);
                            if (!resultStack.isEmpty()) {
                                player.awardStat(Stats.ITEM_USED.get(this));
                                bucketPickup.getPickupSound(blockState).ifPresent((soundEvent) -> {
                                    level.playSound(player, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
                                });
                                level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);

                                ItemStack returnStack = ItemUtils.createFilledResult(itemStack, player, resultStack, false);
                                return InteractionResultHolder.sidedSuccess(returnStack, level.isClientSide());
                            }
                        }
                    }
                }
                return InteractionResultHolder.fail(itemStack);
            } else {
                // 有液体的单元：尝试放置液体
                // 蹲下时不放置液体，让方块的 useItemOn 处理（如罐机填充）
                if (player.isShiftKeyDown()) {
                    return InteractionResultHolder.pass(itemStack);
                }

                BlockState blockState = level.getBlockState(blockPos);
                BlockPos placePos = blockState.getBlock() instanceof LiquidBlockContainer ? blockPos : blockPos2;
                FluidStack fluid = getFluid(itemStack);
                
                if (this.emptyContents(player, level, placePos, blockHitResult, fluid)) {
                    this.checkExtraContent(player, level, itemStack, placePos);
                    player.awardStat(Stats.ITEM_USED.get(this));
                    level.playSound(player, placePos, this.getEmptySound(fluid.getFluid()), SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(player, GameEvent.FLUID_PLACE, placePos);
                    
                    ItemStack emptyCell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                    ItemStack returnStack;
                    if (itemStack.getCount() == 1) {
                        returnStack = emptyCell;
                    } else {
                        itemStack.shrink(1);
                        if (!player.getInventory().add(emptyCell)) {
                            player.drop(emptyCell, false);
                        }
                        returnStack = itemStack;
                    }
                    return InteractionResultHolder.sidedSuccess(returnStack, level.isClientSide());
                } else {
                    return InteractionResultHolder.fail(itemStack);
                }
            }
        }
    }

    /**
     * 右键点击方块时的交互（用于与储罐交互）。
     *
     * <p>语义对齐 IC2 {@code ItemFluidCell#interactWithTank}：统一交给
     * {@link FluidContainerInteraction} —— 先"单元 → 储罐"，不行再"储罐 → 单元"，
     * 支持整叠单元，<b>没有蹲下开关</b>；方向完全由双方容器自身能否接受决定。</p>
     */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();

        IFluidHandler tankHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);

        if (tankHandler != null && player != null
                && FluidContainerInteraction.interact(player, context.getHand(), tankHandler)) {
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }


    @SuppressWarnings("deprecation")
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, FluidStack fluidStack) {
        Fluid fluid = fluidStack.getFluid();
        if (!(fluid instanceof net.minecraft.world.level.material.FlowingFluid)) {
            return false;
        } else {
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
                if (liquidBlockContainer.canPlaceLiquid(player, level, pos, blockState, fluid)) {
                    liquidBlockContainer.placeLiquid(level, pos, blockState, fluid.defaultFluidState());
                    this.playEmptySound(player, level, pos, fluid);
                    return true;
                }
            }

            if (!level.isClientSide && blockState.canBeReplaced(fluid) && !blockState.liquid()) {
                level.destroyBlock(pos, true);
            }

            if (!level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 11) && !blockState.getFluidState().isSource()) {
                return false;
            } else {
                this.playEmptySound(player, level, pos, fluid);
                return true;
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void playEmptySound(@Nullable Player player, Level level, BlockPos pos, Fluid fluid) {
        SoundEvent soundEvent = fluid.getFluidType().getSound(player, level, pos, net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
        if (soundEvent == null) {
            soundEvent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        }
        level.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @SuppressWarnings("deprecation")
    protected SoundEvent getEmptySound(Fluid fluid) {
        return fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
    }

    protected void checkExtraContent(@Nullable Player player, Level level, ItemStack containerStack, BlockPos pos) {
        // 单元不需要额外处理（比如桶里的鱼）
    }

    @Override
    public String getDescriptionId() {
        if (this.content == null || this.content == Fluids.EMPTY) {
            return "item.mio_icif.cell_empty";
        }
        String descriptionId = mio_icif_cells.getDescriptionIdForFluid(this.content);
        return descriptionId != null ? descriptionId : super.getDescriptionId();
    }

    // ==================== NBT 存储方法 ====================

    /**
     * 从物品堆的 NBT 中读取流体信息
     */
    public FluidStack readFluidFromNBT(ItemStack stack) {
        CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(FLUID_KEY)) {
                String fluidId = tag.getString(FLUID_KEY);
                int amount = tag.getInt(AMOUNT_KEY);
                Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                    net.minecraft.resources.ResourceLocation.tryParse(fluidId));
                if (fluid != null && fluid != Fluids.EMPTY && amount > 0) {
                    return new FluidStack(fluid, amount);
                }
                // 已声明流体键但内容无效：按已排空处理，绝不回退成满态
                return FluidStack.EMPTY;
            }
            // 显式空标记：排空后保持为空，否则会被下方回退重新当成满态（流体复制）
            if (tag.getBoolean(EMPTY_KEY)) {
                return FluidStack.EMPTY;
            }
        }
        // 无任何 NBT 数据：视为构造时指定的满态（新造物品 / 旧存档兼容）
        if (content != null && content != Fluids.EMPTY) {
            return new FluidStack(content, CAPACITY);
        }
        return FluidStack.EMPTY;
    }

    /**
     * 将流体信息写入物品堆的 NBT
     */
    public void writeFluidToNBT(ItemStack stack, FluidStack fluidStack) {
        CompoundTag tag = new CompoundTag();
        CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        
        if (fluidStack.isEmpty()) {
            tag.remove(FLUID_KEY);
            tag.remove(AMOUNT_KEY);
            // 显式空标记：没有它就会回退成“构造时的满态”，导致排空失效
            tag.putBoolean(EMPTY_KEY, true);
        } else {
            tag.remove(EMPTY_KEY);
            String fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()).toString();
            tag.putString(FLUID_KEY, fluidId);
            tag.putInt(AMOUNT_KEY, fluidStack.getAmount());
        }
        
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ==================== IFluidCellItem API ====================

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public FluidStack getFluid(ItemStack stack) {
        return readFluidFromNBT(stack);
    }

    @Override
    public boolean canHoldFluid(Fluid fluid) {
        // 静态单元与流体一一对应：只容纳自身绑定的流体
        return fluid != null && fluid != Fluids.EMPTY && fluid == this.content;
    }

    @Override
    public int fill(ItemStack stack, FluidStack fluidStack, boolean simulate) {
        if (fluidStack.isEmpty() || !canHoldFluid(fluidStack.getFluid())) return 0;
        
        FluidStack current = getFluid(stack);
        if (!current.isEmpty() && current.getFluid() != fluidStack.getFluid()) return 0;
        
        int currentAmount = current.getAmount();
        int fillAmount = Math.min(CAPACITY - currentAmount, fluidStack.getAmount());
        
        if (fillAmount <= 0) return 0;
        
        if (!simulate) {
            FluidStack newFluid = new FluidStack(fluidStack.getFluid(), currentAmount + fillAmount);
            writeFluidToNBT(stack, newFluid);
        }
        
        return fillAmount;
    }

    @Override
    public FluidStack drain(ItemStack stack, int amount, boolean simulate) {
        FluidStack current = getFluid(stack);
        if (current.isEmpty()) return FluidStack.EMPTY;
        
        int drainAmount = Math.min(current.getAmount(), amount);
        if (drainAmount <= 0) return FluidStack.EMPTY;
        
        FluidStack drained = new FluidStack(current.getFluid(), drainAmount);
        
        if (!simulate) {
            int remaining = current.getAmount() - drainAmount;
            if (remaining <= 0) {
                writeFluidToNBT(stack, FluidStack.EMPTY);
            } else {
                FluidStack newFluid = new FluidStack(current.getFluid(), remaining);
                writeFluidToNBT(stack, newFluid);
            }
        }
        
        return drained;
    }

    @Override
    public ItemStack getEmptyContainer(ItemStack stack) {
        return new ItemStack(mio_icif_cells.CELL_EMPTY.get());
    }

    @Override
    public ItemStack getFilledContainer(ItemStack stack, Fluid fluid) {
        Item filledCell = mio_icif_cells.getFilledCellForFluid(fluid);
        return filledCell != null ? new ItemStack(filledCell) : ItemStack.EMPTY;
    }

    // ==================== Forge Fluid Capability ====================

    /**
     * 创建此物品的流体处理器，用于 Forge Capability 系统。
     * 这使得单元可以与其他模组的流体系统交互。
     */
    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new CellFluidHandler(stack, this);
    }

    /**
     * 单元专用的流体处理器，实现 IFluidHandlerItem 接口。
     * 支持部分填充和排空，兼容其他模组的流体系统。
     */
    public static class CellFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final mio_icif_cell cell;

        public CellFluidHandler(ItemStack container, mio_icif_cell cell) {
            this.container = container.copy();
            this.cell = cell;
        }

        @Override
        public ItemStack getContainer() {
            return container.copy();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) return FluidStack.EMPTY;
            return cell.getFluid(container);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? cell.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && cell.canHoldFluid(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !cell.canHoldFluid(resource.getFluid())) return 0;
            
            FluidStack current = cell.getFluid(container);
            if (!current.isEmpty() && current.getFluid() != resource.getFluid()) return 0;
            
            int capacity = cell.getCapacity();
            int currentAmount = current.getAmount();
            int fillAmount = Math.min(capacity - currentAmount, resource.getAmount());
            
            if (fillAmount <= 0) return 0;
            
            if (action.execute()) {
                FluidStack newFluid = new FluidStack(resource.getFluid(), currentAmount + fillAmount);
                cell.writeFluidToNBT(container, newFluid);
            }
            
            return fillAmount;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            FluidStack current = cell.getFluid(container);
            if (current.isEmpty() || current.getFluid() != resource.getFluid()) return FluidStack.EMPTY;
            
            int drainAmount = Math.min(current.getAmount(), resource.getAmount());
            if (drainAmount <= 0) return FluidStack.EMPTY;
            
            FluidStack drained = new FluidStack(current.getFluid(), drainAmount);
            
            if (action.execute()) {
                int remaining = current.getAmount() - drainAmount;
                if (remaining <= 0) {
                    cell.writeFluidToNBT(container, FluidStack.EMPTY);
                } else {
                    FluidStack newFluid = new FluidStack(current.getFluid(), remaining);
                    cell.writeFluidToNBT(container, newFluid);
                }
            }
            
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            FluidStack current = cell.getFluid(container);
            if (current.isEmpty()) return FluidStack.EMPTY;
            
            int drainAmount = Math.min(current.getAmount(), maxDrain);
            if (drainAmount <= 0) return FluidStack.EMPTY;
            
            FluidStack drained = new FluidStack(current.getFluid(), drainAmount);
            
            if (action.execute()) {
                int remaining = current.getAmount() - drainAmount;
                if (remaining <= 0) {
                    cell.writeFluidToNBT(container, FluidStack.EMPTY);
                } else {
                    FluidStack newFluid = new FluidStack(current.getFluid(), remaining);
                    cell.writeFluidToNBT(container, newFluid);
                }
            }
            
            return drained;
        }
    }

    // ==================== Dispenser Support ====================

    /**
     * 注册所有流体单元的发射器行为。
     * 使发射器可以发射流体单元中的流体。
     */
    public static void registerDispenserBehaviors() {
        // 注册满单元的发射行为（放置流体）
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_WATER.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_LAVA.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_BIOGAS.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_HOTWATER.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_BIOMASS.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_CONSTRUCTIONFOAM.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_COOLANT.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_DISTILLEDWATER.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_HOTCOOLANT.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_PAHOEHOELAVA.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_STEAM.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_SUPERHEATEDSTEAM.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_UUMATTER.get());
        registerFilledCellDispenserBehavior(mio_icif_cells.CELL_AIR.get());

        registerDynamicCellDispenserBehavior();
    }

    private static void registerDynamicCellDispenserBehavior() {
        mio_icif_dynamic_cell dynamicCell = mio_icif_cells.CELL_EMPTY.get();

        DispenserBlock.registerBehavior(dynamicCell, new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();

            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                Direction direction = source.state().getValue(DispenserBlock.FACING);
                BlockPos pos = source.pos().relative(direction);

                FluidStack fluid = dynamicCell.getFluid(stack);

                if (!fluid.isEmpty()) {
                    if (dynamicCell.emptyContents(null, level, pos, null, fluid)) {
                        dynamicCell.drain(stack, fluid.getAmount(), false);
                        if (dynamicCell.getFluid(stack).isEmpty()) {
                            return new ItemStack(dynamicCell);
                        }
                        return stack;
                    }
                } else {
                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.getBlock() instanceof BucketPickup bucketPickup) {
                        Fluid fluidType = blockState.getFluidState().getType();
                        if (fluidType != Fluids.EMPTY) {
                            ItemStack pickupResult = bucketPickup.pickupBlock(null, level, pos, blockState);
                            if (!pickupResult.isEmpty()) {
                                int filled = dynamicCell.fill(stack, new FluidStack(fluidType, 1000), false);
                                if (filled > 0) {
                                    bucketPickup.getPickupSound(blockState).ifPresent(soundEvent -> {
                                        level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
                                    });
                                    level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                                    return stack;
                                }
                            }
                        }
                    }
                }

                return defaultBehavior.dispense(source, stack);
            }
        });
    }

    /**
     * 注册满单元的发射器行为（放置流体）
     */
    private static void registerFilledCellDispenserBehavior(mio_icif_cell cell) {
        DispenserBlock.registerBehavior(cell, new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();

            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                Direction direction = source.state().getValue(DispenserBlock.FACING);
                BlockPos pos = source.pos().relative(direction);

                // 尝试放置流体
                FluidStack fluid = cell.getFluid(stack);
                if (!fluid.isEmpty() && cell.emptyContents(null, level, pos, null, fluid)) {
                    // 排空流体（修改 NBT）
                    cell.drain(stack, fluid.getAmount(), false);
                    // 检查是否为空
                    if (cell.getFluid(stack).isEmpty()) {
                        return new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                    }
                    return stack;
                }

                return defaultBehavior.dispense(source, stack);
            }
        });
    }
}