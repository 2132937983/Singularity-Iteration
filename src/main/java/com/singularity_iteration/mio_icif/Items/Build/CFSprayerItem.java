package com.singularity_iteration.mio_icif.Items.Build;

import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_foam;
import com.singularity_iteration.mio_icif.Blocks.Build.mio_icif_block_scaffold;
import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.api.item.ICFSprayerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.entity.LivingEntity;

/**
 * CF喷枪 - IC2风格的建筑泡沫喷枪
 * 使用耐久度系统来表示建筑泡沫的储量
 * 耐久度条满时（damageValue = 0）代表泡沫满
 * 耐久度条空时（damageValue = maxFoam）代表泡沫为 0
 * 默认为空（damageValue = maxFoam），需要补充泡沫后才能使用
 *
 * 使用方法：
 * - 右键方块表面：喷洒建筑泡沫（消耗一点泡沫储量）
 * - Shift+右键空气：切换放置模式（单方块/3x3x3区域）
 *
 * 补充方法：
 * - 在灌装机(Canner)的混合模式中，将CF喷枪放入材料槽
 * - 输入液体槽放入建筑泡沫流体，即可自动补充泡沫
 * - 消耗建筑泡沫流体来给喷枪补充泡沫
 */
public class CFSprayerItem extends Item implements ICFSprayerItem {

    // 最大泡沫储量
    public static final int MAX_FOAM = 256;

    // 每次喷洒消耗的泡沫量
    public static final int FOAM_PER_USE = 1;

    // 每个建筑泡沫物品可补充的泡沫量
    public static final int FOAM_PER_REFILL = 64;

    // 放置模式
public enum PlacementMode {
        SINGLE("tooltip.mio_icif.cf_sprayer.mode.single"),
        AREA_3X3X3("tooltip.mio_icif.cf_sprayer.mode.3x3x3"),
        FAN("tooltip.mio_icif.cf_sprayer.mode.fan");

        public final String translationKey;

        PlacementMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public PlacementMode next() {
            return switch (this) {
                case SINGLE -> AREA_3X3X3;
                case AREA_3X3X3 -> FAN;
                case FAN -> SINGLE;
            };
        }
    }

    public CFSprayerItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .durability(MAX_FOAM)
                // 默认为空：damageValue = MAX_FOAM（泡沫量为0）
                .component(DataComponents.DAMAGE, MAX_FOAM)
                // 设为不可破坏，防止物品被消耗掉
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    /**
     * 获取当前泡沫储量
     * 泡沫量 = 最大耐久度 - 当前伤害值
     */
    public int getFoamAmount(ItemStack stack) {
        return MAX_FOAM - stack.getDamageValue();
    }

    /**
     * 设置当前泡沫储量
     */
    public void setFoamAmount(ItemStack stack, int amount) {
        amount = Math.max(0, Math.min(MAX_FOAM, amount));
        stack.setDamageValue(MAX_FOAM - amount);
    }

    /**
     * 添加泡沫（补充）
     * @return 实际添加的泡沫量
     */
    public int addFoam(ItemStack stack, int amount) {
        int current = getFoamAmount(stack);
        int newAmount = Math.min(MAX_FOAM, current + amount);
        int added = newAmount - current;
        setFoamAmount(stack, newAmount);
        return added;
    }

    /**
     * 消耗泡沫
     * @return 实际消耗的泡沫量
     */
    public int consumeFoam(ItemStack stack, int amount) {
        int current = getFoamAmount(stack);
        int newAmount = Math.max(0, current - amount);
        int consumed = current - newAmount;
        setFoamAmount(stack, newAmount);
        return consumed;
    }

    /**
     * 检查是否有足够的泡沫
     */
    public boolean hasEnoughFoam(ItemStack stack, int amount) {
        return getFoamAmount(stack) >= amount;
    }

    /**
     * 泡沫是否为空
     */
    public boolean isEmpty(ItemStack stack) {
        return getFoamAmount(stack) <= 0;
    }

    /**
     * 泡沫是否已满
     */
    public boolean isFull(ItemStack stack) {
        return getFoamAmount(stack) >= MAX_FOAM;
    }

    /**
     * 获取当前放置模式（使用 DataComponents.CUSTOM_DATA）
     */
    public PlacementMode getPlacementMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("PlacementMode")) {
                int ordinal = tag.getInt("PlacementMode");
                if (ordinal >= 0 && ordinal < PlacementMode.values().length) {
                    return PlacementMode.values()[ordinal];
                }
            }
        }
        return PlacementMode.SINGLE;
    }

    /**
     * 设置放置模式（使用 DataComponents.CUSTOM_DATA）
     */
    public void setPlacementMode(ItemStack stack, PlacementMode mode) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = (customData != null) ? customData.copyTag() : new CompoundTag();
        tag.putInt("PlacementMode", mode.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 重写：总是显示泡沫条
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    /**
     * 重写：根据当前泡沫量计算条宽度
     * 泡沫满时条满，泡沫空时条空
     */
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFoamAmount(stack) / MAX_FOAM);
    }

    /**
     * 重写：泡沫条颜色 - 使用橙色（区别于耐久度的绿色）
     */
    @Override
    public int getBarColor(ItemStack stack) {
        return ChatFormatting.GOLD.getColor();
    }

    /**
     * 重写：防止物品被损坏（泡沫耗尽不会损坏物品）
     * 返回0表示不传递任何伤害到vanilla逻辑
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /**
     * 重写：不可被传统方式损坏
     */
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    /**
     * 重写：永远不会被视为"已损坏"
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    /**
     * 右键方块：喷洒建筑泡沫
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (level.isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        // Shift+右键方块：切换放置模式
        if (player != null && player.isShiftKeyDown()) {
            PlacementMode currentMode = getPlacementMode(itemStack);
            PlacementMode newMode = currentMode.next();
            setPlacementMode(itemStack, newMode);
            player.displayClientMessage(
                Component.translatable("tooltip.mio_icif.cf_sprayer.mode_switch",
                    Component.translatable(newMode.translationKey)), true);
            return InteractionResult.sidedSuccess(false);
        }

        // 检查是否有足够的泡沫
        PlacementMode mode = getPlacementMode(itemStack);
        int foamNeeded = switch (mode) {
            case SINGLE -> 1;
            case AREA_3X3X3 -> 27;
            case FAN -> 9;
        };

        if (!hasEnoughFoam(itemStack, foamNeeded)) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("tooltip.mio_icif.cf_sprayer.empty"), true);
            }
            return InteractionResult.FAIL;
        }

        int placedCount = 0;

        // 检查点击的目标是否是脚手架
        BlockState clickedState = level.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();
        boolean isClickedScaffold = clickedBlock instanceof mio_icif_block_scaffold;

        if (mode == PlacementMode.SINGLE) {
            // 如果点击的是脚手架，直接替换脚手架；否则在点击面的相邻位置放置
            BlockPos foamPos = isClickedScaffold ? clickedPos : clickedPos.relative(clickedFace);
            if (placeFoam(level, foamPos)) {
                placedCount = 1;
            }
        } else if (mode == PlacementMode.AREA_3X3X3) {
            // 如果点击的是脚手架，以脚手架位置为中心；否则在点击面的相邻位置
            BlockPos centerPos = isClickedScaffold ? clickedPos : clickedPos.relative(clickedFace);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos foamPos = centerPos.offset(dx, dy, dz);
                        if (placeFoam(level, foamPos)) {
                            placedCount++;
                        }
                    }
                }
            }
        } else if (mode == PlacementMode.FAN) {
            // 扇形模式：在点击面的平面上放置3x3区域
            // 扇形是以点击点为中心，在点击面方向上展开3x3平面
            // 如果点击的是脚手架，以脚手架位置为中心；否则在点击面的相邻位置
            BlockPos centerPos = isClickedScaffold ? clickedPos : clickedPos.relative(clickedFace);
            placedCount = placeFanShape(level, centerPos, clickedFace);
        }

        if (placedCount > 0) {
            if (player != null && !player.getAbilities().instabuild) {
                consumeFoam(itemStack, placedCount);
            }
            level.playSound(null, clickedPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.FAIL;
    }

    /**
     * 在指定位置放置建筑泡沫
     * 对齐原版IC2逻辑：
     * - 如果目标是脚手架，破坏脚手架并在原地放置泡沫
     * - 如果是铁脚手架，放置强化泡沫
     * - 如果是普通脚手架，放置普通泡沫
     * - 如果是空气或可替换方块，检查周围是否有铁脚手架来决定是否放置强化泡沫
     * @return 是否成功放置
     */
    private boolean placeFoam(Level level, BlockPos pos) {
        BlockState existingState = level.getBlockState(pos);
        Block existingBlock = existingState.getBlock();

        if (existingBlock instanceof mio_icif_block_wire) {
            if (existingState.getValue(mio_icif_block_wire.FOAMLOGGED)) {
                return false;
            }
            boolean hasIronScaffoldAdjacent = false;
            for (Direction dir : Direction.values()) {
                BlockState adjacentState = level.getBlockState(pos.relative(dir));
                Block adjacentBlock = adjacentState.getBlock();
                if (adjacentBlock instanceof mio_icif_block_scaffold) {
                    mio_icif_block_scaffold scaffold = (mio_icif_block_scaffold) adjacentBlock;
                    if (scaffold.getStrength() >= 3) {
                        hasIronScaffoldAdjacent = true;
                        break;
                    }
                }
            }
            BlockState newWireState = existingState
                    .setValue(mio_icif_block_wire.FOAMLOGGED, true)
                    .setValue(mio_icif_block_wire.FOAM_REINFORCED, hasIronScaffoldAdjacent);
            level.setBlockAndUpdate(pos, newWireState);
            return true;
        }

        if (existingBlock instanceof mio_icif_block_scaffold) {
            mio_icif_block_scaffold scaffold = (mio_icif_block_scaffold) existingBlock;
            int strength = scaffold.getStrength();

            boolean isIronScaffold = strength >= 3;

            level.removeBlock(pos, false);

            BlockState foamState = mio_icif_blocks.CONSTRUCTION_FOAM.get().defaultBlockState()
                .setValue(mio_icif_block_foam.REINFORCED, isIronScaffold);
            level.setBlockAndUpdate(pos, foamState);
            return true;
        }

        if (!existingState.isAir() && !existingState.canBeReplaced()) {
            return false;
        }

        boolean hasIronScaffoldAdjacent = false;
        for (Direction dir : Direction.values()) {
            BlockState adjacentState = level.getBlockState(pos.relative(dir));
            Block adjacentBlock = adjacentState.getBlock();
            if (adjacentBlock instanceof mio_icif_block_scaffold) {
                mio_icif_block_scaffold scaffold = (mio_icif_block_scaffold) adjacentBlock;
                if (scaffold.getStrength() >= 3) {
                    hasIronScaffoldAdjacent = true;
                    break;
                }
            }
        }

        BlockState foamState = mio_icif_blocks.CONSTRUCTION_FOAM.get().defaultBlockState()
            .setValue(mio_icif_block_foam.REINFORCED, hasIronScaffoldAdjacent);

        level.setBlockAndUpdate(pos, foamState);
        return true;
    }

    /**
     * 扇形模式放置泡沫
     * 在点击面的平面上放置3x3区域（垂直于点击方向的平面展开）
     * 例如：点击北面，在X/Y平面展开3x3
     *       点击上面，在X/Z平面展开3x3
     */
    private int placeFanShape(Level level, BlockPos center, Direction face) {
        int placed = 0;
        for (int d1 = -1; d1 <= 1; d1++) {
            for (int d2 = -1; d2 <= 1; d2++) {
                BlockPos pos = switch (face) {
                    case NORTH, SOUTH -> center.offset(d1, d2, 0); // X/Y平面
                    case EAST, WEST -> center.offset(0, d2, d1);  // Y/Z平面
                    case UP, DOWN -> center.offset(d1, 0, d2);    // X/Z平面
                };
                if (placeFoam(level, pos)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /**
     * Shift+右键空气：切换放置模式
     * 右键空气：无操作
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }

        if (player.isShiftKeyDown()) {
            // 切换模式
            PlacementMode currentMode = getPlacementMode(itemStack);
            PlacementMode newMode = currentMode.next();
            setPlacementMode(itemStack, newMode);
            player.displayClientMessage(
                Component.translatable("tooltip.mio_icif.cf_sprayer.mode_switch",
                    Component.translatable(newMode.translationKey)), true);
            return InteractionResultHolder.sidedSuccess(itemStack, false);
        }

        return InteractionResultHolder.pass(itemStack);
    }

    /**
     * 重写：在物品提示中显示当前泡沫量和模式
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int currentFoam = getFoamAmount(stack);
        PlacementMode mode = getPlacementMode(stack);

        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.foam", currentFoam, MAX_FOAM)
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.current_mode",
                Component.translatable(mode.translationKey))
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.usage")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.mio_icif.cf_sprayer.refill")
                .withStyle(ChatFormatting.GRAY));
    }

}