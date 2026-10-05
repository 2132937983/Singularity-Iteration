package com.miophas.singularity_iteration.common.item.build;

import com.miophas.singularity_iteration.common.block.build.mio_icif_block_foam;
import com.miophas.singularity_iteration.common.block.build.mio_icif_block_scaffold;
import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.item.ICFSprayerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

/**
 * CF 喷枪 - 对齐 IC2 1.12.2 原版 {@code ic2.core.item.tool.ItemSprayer}。
 *
 * <p>对齐要点：
 * <ul>
 *     <li><b>流体储量</b>：容量 8000 mB，每喷洒 1 个方块消耗 100 mB 建筑泡沫流体。</li>
 *     <li><b>两种模式</b>：普通模式最多铺 10 个方块（BFS 泛洪），单方块模式仅 1 个。</li>
 *     <li><b>目标识别</b>：优先识别脚手架 / 线缆，否则在点击面相邻位置喷洒。</li>
 *     <li><b>强化泡沫</b>：喷洒在金属类（铁、钢、碳纤维）脚手架上生成强化泡沫，
 *         木质脚手架生成普通泡沫；金属类脚手架会被消耗并掉落 1 个铁栅栏。</li>
 *     <li><b>补充</b>：可在灌装机中注入建筑泡沫流体补充。</li>
 * </ul>
 */
@SuppressWarnings("null")
public class CFSprayerItem extends Item implements ICFSprayerItem {

    /** 流体容量（对齐 IC2：8000 mB，即 8 桶）。 */
    public static final int CAPACITY = 8000;

    /** 每个方块消耗的流体量（对齐 IC2：100 mB）。 */
    public static final int FLUID_PER_FOAM = 100;

    /** 普通模式一次最多铺设的方块数（对齐 IC2）。 */
    public static final int MAX_BLOCKS_NORMAL = 10;

    /** 单方块模式一次最多铺设的方块数。 */
    public static final int MAX_BLOCKS_SINGLE = 1;

    private static final String TAG_FLUID_ID = "FluidId";
    private static final String TAG_FLUID_AMOUNT = "FluidAmount";
    private static final String TAG_MODE = "PlacementMode";

    /** 放置模式（对齐 IC2：mode 0 = 普通，mode 1 = 单方块）。 */
    public enum PlacementMode {
        NORMAL("tooltip.mio_icif.cf_sprayer.mode.normal"),
        SINGLE("tooltip.mio_icif.cf_sprayer.mode.single");

        public final String translationKey;

        PlacementMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public PlacementMode next() {
            return this == NORMAL ? SINGLE : NORMAL;
        }
    }

    /** 喷洒目标类型（对齐 IC2 ItemSprayer.Target）。 */
    private enum Target {
        ANY, SCAFFOLD, CABLE
    }

    public CFSprayerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ==================== 流体储存 ====================

    public FluidStack getFluid(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return FluidStack.EMPTY;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_FLUID_ID)) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_FLUID_ID));
        if (id == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        int amount = tag.getInt(TAG_FLUID_AMOUNT);
        if (amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount);
    }

    public void setFluid(ItemStack stack, FluidStack fluid) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        if (fluid.isEmpty()) {
            tag.remove(TAG_FLUID_ID);
            tag.remove(TAG_FLUID_AMOUNT);
        } else {
            tag.putString(TAG_FLUID_ID, BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            tag.putInt(TAG_FLUID_AMOUNT, fluid.getAmount());
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** 是否只接受建筑泡沫流体。 */
    public boolean isAcceptedFluid(Fluid fluid) {
        return fluid == mio_icif_fluids.CONSTRUCTIONFOAM.get();
    }

    /** 排空指定数量流体，返回实际排出的量。 */
    public int drainFluid(ItemStack stack, int amount) {
        FluidStack current = getFluid(stack);
        if (current.isEmpty() || amount <= 0) {
            return 0;
        }
        int drained = Math.min(current.getAmount(), amount);
        setFluid(stack, new FluidStack(current.getFluid(), current.getAmount() - drained));
        return drained;
    }

    /** 注入流体，返回实际接受的量。 */
    public int fillFluid(ItemStack stack, FluidStack resource) {
        if (resource.isEmpty() || !isAcceptedFluid(resource.getFluid())) {
            return 0;
        }
        FluidStack current = getFluid(stack);
        if (!current.isEmpty() && current.getFluid() != resource.getFluid()) {
            return 0;
        }
        int currentAmount = current.isEmpty() ? 0 : current.getAmount();
        int accepted = Math.min(CAPACITY - currentAmount, resource.getAmount());
        if (accepted <= 0) {
            return 0;
        }
        setFluid(stack, new FluidStack(resource.getFluid(), currentAmount + accepted));
        return accepted;
    }

    public boolean isEmpty(ItemStack stack) {
        return getFluid(stack).isEmpty();
    }

    public boolean isFull(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return !fluid.isEmpty() && fluid.getAmount() >= CAPACITY;
    }

    // ==================== 模式 ====================

    public PlacementMode getPlacementMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            int ordinal = customData.copyTag().getInt(TAG_MODE);
            PlacementMode[] values = PlacementMode.values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
        }
        return PlacementMode.NORMAL;
    }

    public void setPlacementMode(ItemStack stack, PlacementMode mode) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt(TAG_MODE, mode.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private int getMaxFoamBlocks(ItemStack stack) {
        return getPlacementMode(stack) == PlacementMode.NORMAL ? MAX_BLOCKS_NORMAL : MAX_BLOCKS_SINGLE;
    }

    // ==================== 耐久条 / 提示 ====================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !isEmpty(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return Math.round(13.0F * fluid.getAmount() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ChatFormatting.GOLD.getColor();
    }

    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        int amount = fluid.isEmpty() ? 0 : fluid.getAmount();
        PlacementMode mode = getPlacementMode(stack);

        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.foam", amount, CAPACITY)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.current_mode",
                        Component.translatable(mode.translationKey))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.refill")
                .withStyle(ChatFormatting.GRAY));
    }

    // ==================== 使用 ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                PlacementMode newMode = getPlacementMode(itemStack).next();
                setPlacementMode(itemStack, newMode);
                player.displayClientMessage(
                        Component.translatable("tooltip.mio_icif.cf_sprayer.mode_switch",
                                Component.translatable(newMode.translationKey)), true);
            }
            return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
        }
        return InteractionResultHolder.pass(itemStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        // Shift+右键：切换模式（对齐 IC2 的模式切换键）
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                PlacementMode newMode = getPlacementMode(stack).next();
                setPlacementMode(stack, newMode);
                player.displayClientMessage(
                        Component.translatable("tooltip.mio_icif.cf_sprayer.mode_switch",
                                Component.translatable(newMode.translationKey)), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty() || !isAcceptedFluid(fluid.getFluid())) {
            player.displayClientMessage(Component.translatable("tooltip.mio_icif.cf_sprayer.empty"), true);
            return InteractionResult.FAIL;
        }

        int maxFoamBlocks = Math.min(fluid.getAmount() / FLUID_PER_FOAM, getMaxFoamBlocks(stack));
        if (maxFoamBlocks <= 0) {
            player.displayClientMessage(Component.translatable("tooltip.mio_icif.cf_sprayer.empty"), true);
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Target target;
        if (canPlaceFoam(level, pos, Target.SCAFFOLD)) {
            target = Target.SCAFFOLD;
        } else if (canPlaceFoam(level, pos, Target.CABLE)) {
            target = Target.CABLE;
        } else {
            pos = pos.relative(side);
            target = Target.ANY;
        }

        // 排除玩家视线方向，避免泡沫向玩家一侧倒灌（对齐 IC2）
        Vec3 view = player.getViewVector(1.0F);
        Direction viewFacing = Direction.getNearest(view.x, view.y, view.z);
        Direction excludedDir = viewFacing.getOpposite();

        int placed = sprayFoam(level, pos, excludedDir, target, maxFoamBlocks);
        if (placed <= 0) {
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            drainFluid(stack, placed * FLUID_PER_FOAM);
        }
        level.playSound(null, context.getClickedPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
        return InteractionResult.sidedSuccess(false);
    }

    private BlockState foamState(boolean reinforced) {
        return mio_icif_blocks.CONSTRUCTION_FOAM.get().defaultBlockState()
                .setValue(mio_icif_block_foam.REINFORCED, reinforced);
    }

    private boolean canPlaceFoam(Level level, BlockPos pos, Target target) {
        BlockState state = level.getBlockState(pos);
        return switch (target) {
            case ANY -> (state.isAir() || state.canBeReplaced())
                    && !(state.getBlock() instanceof mio_icif_block_foam);
            case SCAFFOLD -> state.getBlock() instanceof mio_icif_block_scaffold
                    // cables laid through the scaffold are foamed with it (into reinforced foam)
                    || state.getBlock() instanceof mio_icif_block_wire && !state.getValue(mio_icif_block_wire.FOAMLOGGED)
                       && state.getValue(mio_icif_block_wire.FOAM_REINFORCED);
            case CABLE -> state.getBlock() instanceof mio_icif_block_wire
                    && !state.getValue(mio_icif_block_wire.FOAMLOGGED);
        };
    }

    /**
     * 对齐 IC2 {@code ItemSprayer.sprayFoam}：从起始位置 BFS 泛洪收集可喷洒位置，再逐一替换。
     *
     * @return 实际成功放置的方块数
     */
    public int sprayFoam(Level level, BlockPos pos, Direction excludedDir, Target target, int maxFoamBlocks) {
        if (!canPlaceFoam(level, pos, target)) {
            return 0;
        }

        Queue<BlockPos> toCheck = new ArrayDeque<>();
        Set<BlockPos> positions = new LinkedHashSet<>();
        toCheck.add(pos);
        BlockPos current;
        while ((current = toCheck.poll()) != null && positions.size() < maxFoamBlocks) {
            if (!canPlaceFoam(level, current, target)) {
                continue;
            }
            if (positions.add(current)) {
                for (Direction dir : Direction.values()) {
                    if (dir != excludedDir) {
                        toCheck.add(current.relative(dir));
                    }
                }
            }
        }

        int failedPlacements = 0;
        for (BlockPos targetPos : positions) {
            BlockState state = level.getBlockState(targetPos);

            if (state.getBlock() instanceof mio_icif_block_scaffold scaffold) {
                boolean ironClass = scaffold.getStrength() >= 3;
                if (ironClass) {
                    // IC2：金属类脚手架 → 掉落 1 个铁栅栏，并生成强化泡沫
                    level.removeBlock(targetPos, false);
                    Block.popResource(level, targetPos, new ItemStack(mio_icif_blocks.BLOCK_FENCE_IRON.get().asItem()));
                    level.setBlockAndUpdate(targetPos, foamState(true));
                } else {
                    // 木质脚手架 → 正常掉落脚手架本体，并生成普通泡沫
                    level.destroyBlock(targetPos, false);
                    level.setBlockAndUpdate(targetPos, foamState(false));
                }
                continue;
            }

            if (state.getBlock() instanceof mio_icif_block_wire) {
                if (state.getValue(mio_icif_block_wire.FOAMLOGGED)) {
                    failedPlacements++;
                } else {
                    level.setBlockAndUpdate(targetPos, state
                            .setValue(mio_icif_block_wire.FOAMLOGGED, true)
                            .setValue(mio_icif_block_wire.FOAM_REINFORCED, state.getValue(mio_icif_block_wire.FOAM_REINFORCED)));
                }
                continue;
            }

            if (!level.setBlockAndUpdate(targetPos, foamState(false))) {
                failedPlacements++;
            }
        }

        return positions.size() - failedPlacements;
    }

    // ==================== 物品流体能力 ====================

    /** 供 NeoForge 能力系统使用（在 {@code mio_icif_capacities} 中注册）。 */
    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new SprayerFluidHandler(stack, this);
    }

    /** CF 喷枪专用的流体处理器，只接受建筑泡沫流体。 */
    public static class SprayerFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final CFSprayerItem sprayer;

        public SprayerFluidHandler(ItemStack container, CFSprayerItem sprayer) {
            this.container = container.copy();
            this.sprayer = sprayer;
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
            return tank == 0 ? sprayer.getFluid(container) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && sprayer.isAcceptedFluid(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (action.simulate()) {
                FluidStack current = sprayer.getFluid(container);
                if (resource.isEmpty() || !sprayer.isAcceptedFluid(resource.getFluid())) {
                    return 0;
                }
                if (!current.isEmpty() && current.getFluid() != resource.getFluid()) {
                    return 0;
                }
                int currentAmount = current.isEmpty() ? 0 : current.getAmount();
                return Math.max(0, Math.min(CAPACITY - currentAmount, resource.getAmount()));
            }
            return sprayer.fillFluid(container, resource);
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            FluidStack current = sprayer.getFluid(container);
            if (current.isEmpty() || resource.isEmpty() || current.getFluid() != resource.getFluid()) {
                return FluidStack.EMPTY;
            }
            int drained = Math.min(current.getAmount(), resource.getAmount());
            if (drained <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                sprayer.drainFluid(container, drained);
            }
            return new FluidStack(current.getFluid(), drained);
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            FluidStack current = sprayer.getFluid(container);
            if (current.isEmpty() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            int drained = Math.min(current.getAmount(), maxDrain);
            if (action.execute()) {
                sprayer.drainFluid(container, drained);
            }
            return new FluidStack(current.getFluid(), drained);
        }
    }
}
