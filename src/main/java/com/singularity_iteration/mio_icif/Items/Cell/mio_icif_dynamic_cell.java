package com.singularity_iteration.mio_icif.Items.Cell;

import com.singularity_iteration.mio_icif.api.item.IFluidCellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
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
import java.util.List;

/**
 * 动态流体单元 - 使用 NBT 存储流体类型和数量。
 * 
 * <p>与 {@link mio_icif_cell} 不同，此单元不依赖构造时指定的流体类型，
 * 而是通过 NBT 动态存储任意支持的流体。这允许：</p>
 * <ul>
 *   <li>单个物品处理多种流体（通过 NBT 区分）</li>
 *   <li>真正的部分填充支持（500mB、250mB 等任意数量）</li>
 *   <li>简化流体单元注册（无需为每种流体注册单独物品）</li>
 * </ul>
 * 
 * <p><b>使用方式：</b></p>
 * <pre>
 * // 创建空动态单元
 * ItemStack emptyCell = new ItemStack(DYNAMIC_CELL.get());
 * 
 * // 填充水 500mB
 * cell.fill(emptyCell, new FluidStack(Fluids.WATER, 500), false);
 * 
 * // 现在 emptyCell 的 NBT 中存储了 {fluid:"minecraft:water", amount:500}
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_dynamic_cell extends Item implements IFluidCellItem {

    private static final int CAPACITY = 1000; // 1000mb = 1 bucket
    private static final String FLUID_KEY = "fluid";
    private static final String AMOUNT_KEY = "amount";

    public mio_icif_dynamic_cell(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 64;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return !getFluid(stack).isEmpty();
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (!hasCraftingRemainingItem(stack)) return super.getCraftingRemainingItem(stack);
        return new ItemStack(this);
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) {
            return Component.translatable("item.mio_icif.cell_empty");
        }
        Item staticCell = mio_icif_cells.getFilledCellForFluid(fluid.getFluid());
        if (staticCell != null) {
            return staticCell.getDescription().copy();
        }
        return Component.translatable("item.mio_icif.cell_dynamic_named", fluid.getFluid().getFluidType().getDescription());
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        FluidStack fluid = getFluid(itemStack);
        BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, fluid.isEmpty() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        
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
            } else if (fluid.isEmpty()) {
                // 空单元：尝试从世界中吸取流体
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BucketPickup bucketPickup) {
                    Fluid fluidType = blockState.getFluidState().getType();
                    if (fluidType != Fluids.EMPTY) {
                        ItemStack filledCell = bucketPickup.pickupBlock(player, level, blockPos, blockState);
                        if (!filledCell.isEmpty()) {
                            // 创建填充后的动态单元
                            ItemStack resultStack = new ItemStack(this);
                            FluidStack newFluid = new FluidStack(fluidType, CAPACITY);
                            writeFluidToNBT(resultStack, newFluid);
                            
                            player.awardStat(Stats.ITEM_USED.get(this));
                            bucketPickup.getPickupSound(blockState).ifPresent((soundEvent) -> {
                                level.playSound(player, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
                            });
                            level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);

                            ItemStack returnStack;
                            if (itemStack.getCount() == 1) {
                                returnStack = resultStack;
                            } else {
                                itemStack.shrink(1);
                                if (!player.getInventory().add(resultStack)) {
                                    player.drop(resultStack, false);
                                }
                                returnStack = itemStack;
                            }
                            return InteractionResultHolder.sidedSuccess(returnStack, level.isClientSide());
                        }
                    }
                }
                return InteractionResultHolder.fail(itemStack);
            } else {
                // 有液体的单元：尝试放置液体（仅满量单元可放置源方块）
                if (player.isShiftKeyDown() || fluid.getAmount() < CAPACITY) {
                    return InteractionResultHolder.pass(itemStack);
                }

                BlockState blockState = level.getBlockState(blockPos);
                BlockPos placePos = blockState.getBlock() instanceof LiquidBlockContainer ? blockPos : blockPos2;
                
                if (this.emptyContents(player, level, placePos, blockHitResult, fluid)) {
                    player.awardStat(Stats.ITEM_USED.get(this));
                    level.playSound(player, placePos, this.getEmptySound(fluid.getFluid()), SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(player, GameEvent.FLUID_PLACE, placePos);
                    
                    ItemStack emptyCell = new ItemStack(this);
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
     * 右键点击方块时的交互（用于与储罐交互）
     */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        
        net.neoforged.neoforge.fluids.capability.IFluidHandler tankHandler = level.getCapability(
            Capabilities.FluidHandler.BLOCK, pos, side);
        
        if (tankHandler != null && player != null) {
            if (interactWithTank(player, context.getHand(), level, pos, side, tankHandler)) {
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        
        return super.useOn(context);
    }

    /**
     * 与储罐交互，支持堆叠单元批量传输（对齐 IC2 的 interactWithTank）。
     * <p>当玩家手持多个单元时，循环传输直到无法继续。</p>
     */
    private boolean interactWithTank(Player player, InteractionHand hand, Level level, BlockPos pos, Direction side, net.neoforged.neoforge.fluids.capability.IFluidHandler tankHandler) {
        ItemStack stack = player.getItemInHand(hand);
        boolean single = stack.getCount() == 1;
        boolean drainFromTank = player.isShiftKeyDown();
        boolean changeMade = false;

        while (true) {
            ItemStack singleStack = single ? stack : stack.copyWithCount(1);
            IFluidHandlerItem itemHandler = singleStack.getCapability(Capabilities.FluidHandler.ITEM);
            if (itemHandler == null) break;

            boolean success;
            if (drainFromTank) {
                success = tryDrainFromTank(itemHandler, tankHandler);
            } else {
                success = tryFillTank(itemHandler, tankHandler);
            }

            if (success) {
                ItemStack resultContainer = itemHandler.getContainer();
                if (single) {
                    player.setItemInHand(hand, resultContainer);
                    return true;
                }
                stack.shrink(1);
                if (!resultContainer.isEmpty()) {
                    if (!player.getInventory().add(resultContainer)) {
                        player.drop(resultContainer, false);
                    }
                }
                changeMade = true;
                if (stack.isEmpty()) break;
            } else {
                break;
            }
        }

        return changeMade;
    }

    private boolean tryDrainFromTank(IFluidHandlerItem itemHandler, net.neoforged.neoforge.fluids.capability.IFluidHandler tankHandler) {
        FluidStack available = tankHandler.drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        if (!available.isEmpty()) {
            int filled = itemHandler.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (filled > 0) {
                FluidStack drained = tankHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                itemHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }

    private boolean tryFillTank(IFluidHandlerItem itemHandler, net.neoforged.neoforge.fluids.capability.IFluidHandler tankHandler) {
        FluidStack available = itemHandler.drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        if (!available.isEmpty()) {
            int filled = tankHandler.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (filled > 0) {
                FluidStack drained = itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                tankHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.fluid_content", 
                fluid.getAmount(), CAPACITY, fluid.getFluid().getFluidType().getDescription()).withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.empty_cell").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
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
            }
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
            if (tag.isEmpty()) {
                stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                return;
            }
        } else {
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
        // 动态单元可以存储任何流体（包括模组流体）
        return fluid != null && fluid != Fluids.EMPTY;
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
        return new ItemStack(this);
    }

    @Override
    public ItemStack getFilledContainer(ItemStack stack, Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(this);
        writeFluidToNBT(result, new FluidStack(fluid, CAPACITY));
        return result;
    }

    // ==================== Forge Fluid Capability ====================

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new DynamicCellFluidHandler(stack, this);
    }

    public static class DynamicCellFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final mio_icif_dynamic_cell cell;

        public DynamicCellFluidHandler(ItemStack container, mio_icif_dynamic_cell cell) {
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
}