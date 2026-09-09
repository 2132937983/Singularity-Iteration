package com.singularity_iteration.mio_icif.Items.Cell;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.api.item.IFluidCellItem;
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
                    if (isSupportedFluid(fluidType)) {
                        ItemStack filledCell = bucketPickup.pickupBlock(player, level, blockPos, blockState);
                        if (!filledCell.isEmpty()) {
                            ItemStack resultStack = getFilledCell(filledCell);
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
    private boolean interactWithTank(Player player, net.minecraft.world.InteractionHand hand, Level level, BlockPos pos, Direction side, net.neoforged.neoforge.fluids.capability.IFluidHandler tankHandler) {
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
        if (!available.isEmpty() && canHoldFluid(available.getFluid())) {
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

    private boolean isSupportedFluid(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.LAVA) return true;
        if (fluid == mio_icif_fluids.BIOGAS.get()) return true;
        if (fluid == mio_icif_fluids.HOTWATER.get()) return true;
        if (fluid == mio_icif_fluids.BIOMASS.get()) return true;
        if (fluid == mio_icif_fluids.CONSTRUCTIONFOAM.get()) return true;
        if (fluid == mio_icif_fluids.COOLANT.get()) return true;
        if (fluid == mio_icif_fluids.DISTILLEDWATER.get()) return true;
        if (fluid == mio_icif_fluids.HOTCOOLANT.get()) return true;
        if (fluid == mio_icif_fluids.PAHOEHOELAVA.get()) return true;
        if (fluid == mio_icif_fluids.STEAM.get()) return true;
        if (fluid == mio_icif_fluids.SUPERHEATEDSTEAM.get()) return true;
        if (fluid == mio_icif_fluids.UUMATTER.get()) return true;
        if (fluid == mio_icif_fluids.AIR.get()) return true;
        return false;
    }

    private ItemStack getFilledCell(ItemStack pickupResult) {
        if (pickupResult.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
            return new ItemStack(mio_icif_cells.CELL_WATER.get());
        } else if (pickupResult.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            return new ItemStack(mio_icif_cells.CELL_LAVA.get());
        } else if (pickupResult.is(mio_icif_fluids.BIOGAS_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_BIOGAS.get());
        } else if (pickupResult.is(mio_icif_fluids.HOTWATER_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_HOTWATER.get());
        } else if (pickupResult.is(mio_icif_fluids.BIOMASS_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_BIOMASS.get());
        } else if (pickupResult.is(mio_icif_fluids.CONSTRUCTIONFOAM_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_CONSTRUCTIONFOAM.get());
        } else if (pickupResult.is(mio_icif_fluids.COOLANT_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_COOLANT.get());
        } else if (pickupResult.is(mio_icif_fluids.DISTILLEDWATER_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_DISTILLEDWATER.get());
        } else if (pickupResult.is(mio_icif_fluids.HOTCOOLANT_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_HOTCOOLANT.get());
        } else if (pickupResult.is(mio_icif_fluids.PAHOEHOELAVA_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_PAHOEHOELAVA.get());
        } else if (pickupResult.is(mio_icif_fluids.STEAM_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_STEAM.get());
        } else if (pickupResult.is(mio_icif_fluids.SUPERHEATEDSTEAM_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_SUPERHEATEDSTEAM.get());
        } else if (pickupResult.is(mio_icif_fluids.UUMATTER_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_UUMATTER.get());
        } else if (pickupResult.is(mio_icif_fluids.AIR_BUCKET.get())) {
            return new ItemStack(mio_icif_cells.CELL_AIR.get());
        }
        return ItemStack.EMPTY;
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
        if (this.content == Fluids.EMPTY) {
            return "item.mio_icif.cell_empty";
        } else if (this.content == Fluids.WATER) {
            return "item.mio_icif.cell_water";
        } else if (this.content == Fluids.LAVA) {
            return "item.mio_icif.cell_lava";
        } else if (this.content == mio_icif_fluids.BIOGAS.get()) {
            return "item.mio_icif.cell_biogas";
        } else if (this.content == mio_icif_fluids.HOTWATER.get()) {
            return "item.mio_icif.cell_hotwater";
        } else if (this.content == mio_icif_fluids.BIOMASS.get()) {
            return "item.mio_icif.cell_biomass";
        } else if (this.content == mio_icif_fluids.CONSTRUCTIONFOAM.get()) {
            return "item.mio_icif.cell_constructionfoam";
        } else if (this.content == mio_icif_fluids.COOLANT.get()) {
            return "item.mio_icif.cell_coolant";
        } else if (this.content == mio_icif_fluids.DISTILLEDWATER.get()) {
            return "item.mio_icif.cell_distilledwater";
        } else if (this.content == mio_icif_fluids.HOTCOOLANT.get()) {
            return "item.mio_icif.cell_hotcoolant";
        } else if (this.content == mio_icif_fluids.PAHOEHOELAVA.get()) {
            return "item.mio_icif.cell_pahoehoelava";
        } else if (this.content == mio_icif_fluids.STEAM.get()) {
            return "item.mio_icif.cell_steam";
        } else if (this.content == mio_icif_fluids.SUPERHEATEDSTEAM.get()) {
            return "item.mio_icif.cell_superheatedsteam";
        } else if (this.content == mio_icif_fluids.UUMATTER.get()) {
            return "item.mio_icif.cell_uumatter";
        } else if (this.content == mio_icif_fluids.AIR.get()) {
            return "item.mio_icif.cell_air";
        }
        return super.getDescriptionId();
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
        // 如果没有 NBT 数据，使用构造时指定的流体类型（向后兼容）
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
        return isSupportedFluid(fluid);
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
        if (fluid == Fluids.WATER) return new ItemStack(mio_icif_cells.CELL_WATER.get());
        if (fluid == Fluids.LAVA) return new ItemStack(mio_icif_cells.CELL_LAVA.get());
        if (fluid == mio_icif_fluids.BIOGAS.get()) return new ItemStack(mio_icif_cells.CELL_BIOGAS.get());
        if (fluid == mio_icif_fluids.HOTWATER.get()) return new ItemStack(mio_icif_cells.CELL_HOTWATER.get());
        if (fluid == mio_icif_fluids.BIOMASS.get()) return new ItemStack(mio_icif_cells.CELL_BIOMASS.get());
        if (fluid == mio_icif_fluids.CONSTRUCTIONFOAM.get()) return new ItemStack(mio_icif_cells.CELL_CONSTRUCTIONFOAM.get());
        if (fluid == mio_icif_fluids.COOLANT.get()) return new ItemStack(mio_icif_cells.CELL_COOLANT.get());
        if (fluid == mio_icif_fluids.DISTILLEDWATER.get()) return new ItemStack(mio_icif_cells.CELL_DISTILLEDWATER.get());
        if (fluid == mio_icif_fluids.HOTCOOLANT.get()) return new ItemStack(mio_icif_cells.CELL_HOTCOOLANT.get());
        if (fluid == mio_icif_fluids.PAHOEHOELAVA.get()) return new ItemStack(mio_icif_cells.CELL_PAHOEHOELAVA.get());
        if (fluid == mio_icif_fluids.STEAM.get()) return new ItemStack(mio_icif_cells.CELL_STEAM.get());
        if (fluid == mio_icif_fluids.SUPERHEATEDSTEAM.get()) return new ItemStack(mio_icif_cells.CELL_SUPERHEATEDSTEAM.get());
        if (fluid == mio_icif_fluids.UUMATTER.get()) return new ItemStack(mio_icif_cells.CELL_UUMATTER.get());
        if (fluid == mio_icif_fluids.AIR.get()) return new ItemStack(mio_icif_cells.CELL_AIR.get());
        return ItemStack.EMPTY;
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