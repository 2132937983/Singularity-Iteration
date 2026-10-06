// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;
import com.miophas.singularity_iteration.core.api.tool.IToolModeProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick;
import com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick_upgraded;
import com.miophas.singularity_iteration.common.item.resource.MatronFertilizerItem;

/**
 * 电动锄（Electric Hoe，对应 IC2 1.12.2 的 {@code ic2:electric_hoe}）。
 *
 * <p>使用统一 DataComponent 电量驱动（不消耗耐久），IC2 原版数值：
 * maxCharge 10000 EU、transferLimit 100 EU/t、tier 1、挖掘效率 16.0F。
 * 每次操作耗电低于 IC2 原版的 50 EU/次（本工具一次点击要处理整片范围/整片作物）。
 *
 * <h3>功能</h3>
 * <ul>
 *   <li>右键一键锄地：范围由模式决定，1x1（基础，默认）/ 3x3 / 5x5 / 9x9，
 *       按按键集里的“切换工具”键（默认 G）循环切换。</li>
 *   <li>除草：射线打在草木本身上（草方块被草挡住时就是这样），或可锄土壤上方长了草木
 *       （杂草、花、树苗等 {@link BushBlock}）时，先清除草木再锄其下方的土壤——
 *       否则原版 {@code HOE_TILL} 判定会因“上方不是空气”而无法锄地。</li>
 *   <li>连锁收获作物：点击一次成熟作物，<b>不受锄地范围限制</b>，沿连通的同类作物
 *       （原版/本模组 {@link CropBlock} 作物、{@link NetherWartBlock} 地狱疣、{@link IPlanter} 种植架）
 *       整片连锁收获，并自动补种回 0 龄。耗电或数量达到上限即停止。</li>
 *   <li>副手自动种植：副手持有种子标签（{@code c:seeds}）物品时，锄地后会在范围内耕地
 *       （或空作物架）上自动补种，种子从副手消耗。</li>
 *   <li>副手批量施肥：副手持有肥料标签（{@code c:fertilizers}）物品时，按范围给作物施肥——
 *       作物架走方块交互（IC2 肥料加营养值），普通作物走肥料物品自身的 {@code useOn}（原版骨粉等）。</li>
 *   <li>自带时运 3：通过 {@link GetEnchantmentLevelEvent} 动态提供时运等级 3，使收获掉落受时运加成。</li>
 * </ul>
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_electric_hoe extends mio_icif_tool_elc
        implements IToolModeProvider, IEquipmentHudProvider {

    // IC2 原版：maxCharge = 10000 EU
    public static final int HOE_MAX_ENERGY = 10000;

    // IC2 原版：transferLimit = 100 EU/t
    public static final int HOE_TRANSFER_LIMIT = 100;

    // 每次操作（锄地/除草/收获一格/播种一格）消耗的电量。
    // IC2 原版为 50 EU/次，本工具一次点击要处理整片范围，故下调到 10 EU，满电可处理 1000 格。
    public static final int HOE_ENERGY_PER_USE = 10;

    // 连锁收获的工作量上限（防止超大农田一次点击造成卡顿；实际通常先被电量限制）
    private static final int MAX_CHAIN_VISITS = 4096;
    private static final int MAX_CHAIN_HARVESTS = 1024;

    // 连锁收获的扩散方向（含上下，作物架竖直堆叠时也能连成一片）
    private static final Direction[] CHAIN_DIRECTIONS = Direction.values();

    // IC2 原版：ItemToolIC2 的效率值（field_77864_a = 16.0F）
    public static final float HOE_EFFICIENCY = 16.0F;

    // IC2 原版：ItemTool 基础攻击伤害 2.0（玩家基础 1.0 + 1.0 加成）
    public static final float HOE_ATTACK_DAMAGE_BONUS = 1.0F;

    // 自带时运等级
    public static final int BUILT_IN_FORTUNE_LEVEL = 3;

    /** 模式在 CUSTOM_DATA 中的键。 */
    private static final String MODE_KEY = "electric_hoe_range";

    /** 锄地/收获范围模式，序号即为存储值；1x1 为基础范围（默认）。 */
    public enum Range {
        RANGE_1X1(1),
        RANGE_3X3(3),
        RANGE_5X5(5),
        RANGE_9X9(9);

        private final int size;

        Range(int size) {
            this.size = size;
        }

        public int size() {
            return size;
        }

        public String label() {
            return size + "x" + size;
        }

        public Component displayName() {
            return Component.translatable("hud.mio_icif.iron_hoe.mode_" + label());
        }

        public static Range byIndex(int index) {
            return index >= 0 && index < values().length ? values()[index] : RANGE_1X1;
        }
    }

    public mio_icif_electric_hoe(Properties properties) {
        super(properties, HOE_MAX_ENERGY, HOE_MAX_ENERGY, "iron_hoe",
                HOE_TRANSFER_LIMIT, HOE_ENERGY_PER_USE, 1);
    }

    public mio_icif_electric_hoe(Properties properties, int initialEnergy) {
        super(properties, HOE_MAX_ENERGY, HOE_MAX_ENERGY - initialEnergy, "iron_hoe",
                HOE_TRANSFER_LIMIT, HOE_ENERGY_PER_USE, 1);
    }

    // ==================== 模式 ====================

    public static Range getRange(ItemStack stack) {
        return Range.byIndex(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(MODE_KEY));
    }

    private static void setRange(ItemStack stack, Range range) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt(MODE_KEY, range.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    /**
     * 按键集里的“切换工具”键（默认 G）→ {@code ToolTogglePacket} → 本方法：
     * 依次循环 1x1 → 3x3 → 5x5 → 9x9。
     */
    @Override
    public void toggleActive(ItemStack stack, Player player) {
        if (player.level().isClientSide) return;
        Range next = Range.byIndex((getRange(stack).ordinal() + 1) % Range.values().length);
        setRange(stack, next);
        player.displayClientMessage(
                Component.translatable("hud.mio_icif.iron_hoe.switch_range", next.displayName()), true);
    }

    // ==================== 自带时运 3 ====================

    /**
     * 动态提供时运 3，使收获/挖掘掉落受时运加成。
     * 由 {@code Singularity_Iteration#commonSetup} 注册到 NeoForge 事件总线。
     */
    public static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        ItemStack stack = event.getStack();
        if (!(stack.getItem() instanceof mio_icif_electric_hoe)) return;

        HolderLookup.RegistryLookup<Enchantment> lookup = event.getLookup();
        lookup.get(Enchantments.FORTUNE).ifPresent(holder -> {
            if (event.isTargetting(holder)) {
                ItemEnchantments.Mutable enchantments = event.getEnchantments();
                enchantments.set(holder, BUILT_IN_FORTUNE_LEVEL);
            }
        });
    }

    // ==================== 右键：范围锄地 / 连锁收获 ====================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isSpectator()) return InteractionResult.PASS;
        if (!hasEnoughEnergy(stack)) return InteractionResult.PASS;

        BlockPos clicked = context.getClickedPos();
        if (!level.mayInteract(player, clicked)) return InteractionResult.PASS;

        // 连锁收获：点到可收获的作物时不受锄地范围限制，沿连通的同类作物整片收获
        if (canHarvest(level, clicked)) {
            if (!level.isClientSide) {
                chainHarvest(level, clicked, player, stack);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        List<BlockPos> targets = collectTargets(clicked, context.getClickedFace(), getRange(stack).size());

        // 射线打在杂草上（草方块被草挡住时就是这样）时，整片平面都按“杂草层”处理：
        // 先清除杂草，再锄其下方的土壤
        boolean weedLayer = isRemovableVegetation(level.getBlockState(clicked))
                && isTillableSoil(level.getBlockState(soilFor(level, clicked, true)));

        boolean possible = false;
        for (BlockPos target : targets) {
            if (!level.hasChunkAt(target)) continue;
            if (wouldAct(level, context, target, player, weedLayer)) {
                possible = true;
                break;
            }
        }
        if (!possible) return InteractionResult.PASS;

        if (!level.isClientSide) {
            boolean tilled = false;
            for (BlockPos target : targets) {
                if (!hasEnoughEnergy(stack)) break;
                if (!level.hasChunkAt(target)) continue;
                BlockPos soilPos = soilFor(level, target, weedLayer);
                // 除草：原版 HOE_TILL 要求土壤上方为空气，被杂草遮挡时先清除
                for (BlockPos weed : weedsToClear(level, target, soilPos, weedLayer)) {
                    if (!hasEnoughEnergy(stack)) break;
                    if (clearWeeds(level, weed, player)) {
                        consumeEnergy(stack);
                    }
                }
                if (hasEnoughEnergy(stack) && tillOne(level, context, soilPos)) {
                    tilled = true;
                    consumeEnergy(stack);
                }
                // 副手持有种子时，自动在耕地/空作物架上补种
                if (hasEnoughEnergy(stack) && plantOne(level, player, soilPos)) {
                    consumeEnergy(stack);
                }
                // 副手持有肥料时按范围施肥（作物也可能长在平面上一格）
                if (hasEnoughEnergy(stack)) {
                    if (fertilizeOne(level, player, target)
                            || (hasEnoughEnergy(stack) && fertilizeOne(level, player, target.above()))) {
                        consumeEnergy(stack);
                    }
                }
            }
            if (tilled) {
                level.playSound(null, clicked, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 以点击面为基准，取垂直于该面的 size×size 平面（包含点击方块本身），中心优先。 */
    private static List<BlockPos> collectTargets(BlockPos center, Direction face, int size) {
        int radius = (size - 1) / 2;
        Direction.Axis axis = face.getAxis();
        List<BlockPos> targets = new ArrayList<>(size * size);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    switch (axis) {
                        case Y -> {
                            if (dy != 0) continue;
                        }
                        case X -> {
                            if (dx != 0) continue;
                        }
                        case Z -> {
                            if (dz != 0) continue;
                        }
                    }
                    targets.add(center.offset(dx, dy, dz));
                }
            }
        }
        targets.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return targets;
    }

    // ==================== 收获 ====================

    /**
     * 若该方块是已成熟的可收获作物，返回“收获后补种”的方块状态（原版 {@link CropBlock} 作物、
     * 地狱疣），否则返回 {@code null}。
     */
    private static BlockState matureCropReplantState(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? crop.getStateForAge(0) : null;
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE
                    ? state.setValue(NetherWartBlock.AGE, 0)
                    : null;
        }
        return null;
    }

    private static boolean canHarvest(Level level, BlockPos pos) {
        if (matureCropReplantState(level.getBlockState(pos)) != null) return true;
        if (level.getBlockEntity(pos) instanceof IPlanter planter) {
            PlantType plant = planter.getPlant();
            return plant != null && plant.isHarvestable(planter);
        }
        return false;
    }

    /**
     * 连锁收获：从点击处出发，沿“连通的同类作物”做广度优先扩散，
     * <b>不受锄地范围限制</b>（整片农田一次点击收完），成熟的一并收获并自动补种。
     * 每收获一格扣一次电，电量不足或达到上限即停止。
     */
    private void chainHarvest(Level level, BlockPos start, Player player, ItemStack stack) {
        Block referenceBlock = level.getBlockState(start).getBlock();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        int harvested = 0;
        while (!queue.isEmpty() && harvested < MAX_CHAIN_HARVESTS && visited.size() <= MAX_CHAIN_VISITS) {
            if (!hasEnoughEnergy(stack)) break;
            BlockPos pos = queue.poll();
            if (harvestOne(level, pos, player, stack)) {
                consumeEnergy(stack);
                harvested++;
            }
            for (Direction direction : CHAIN_DIRECTIONS) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || !level.hasChunkAt(next)) continue;
                if (!isSameCropFamily(level, next, referenceBlock)) continue;
                visited.add(next);
                queue.add(next);
            }
        }
    }

    /** 同一“作物家族”：同种作物方块；作物架的普通/升级款互相视为一族。 */
    private static boolean isSameCropFamily(Level level, BlockPos pos, Block referenceBlock) {
        BlockState state = level.getBlockState(pos);
        if (isCropStick(referenceBlock)) {
            return isCropStick(state.getBlock());
        }
        return state.is(referenceBlock);
    }

    private boolean harvestOne(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        BlockState replanted = matureCropReplantState(state);
        if (replanted != null) {
            if (level instanceof ServerLevel serverLevel) {
                // Block.getDrops 会查询工具的时运等级，本工具通过 GetEnchantmentLevelEvent 自带时运 3
                List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null, player, stack);
                level.setBlock(pos, replanted, Block.UPDATE_ALL);
                for (ItemStack drop : drops) {
                    Block.popResource(level, pos, drop);
                }
                level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        if (level.getBlockEntity(pos) instanceof IPlanter planter) {
            PlantType plant = planter.getPlant();
            if (plant == null || !plant.isHarvestable(planter)) return false;
            if (!level.isClientSide) {
                // doHarvest 内部会扣除种子并重置生长阶段，等价于“收获后自动补种”
                planter.doManualHarvest();
                level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }

    // ==================== 自动种植（副手种子） ====================

    private static boolean isCropStick(Block block) {
        return block instanceof mio_icif_crop_stick || block instanceof mio_icif_crop_stick_upgraded;
    }

    /**
     * 是否可以在该位置自动种植：副手持有种子标签（{@code c:seeds}）物品，
     * 且目标是上面为空位的耕地，或尚未种植的空作物架。
     */
    private static boolean canPlantAt(Level level, BlockPos pos, Player player) {
        if (!player.getOffhandItem().is(Tags.Items.SEEDS)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FarmBlock) {
            return level.getBlockState(pos.above()).canBeReplaced();
        }
        if (isCropStick(state.getBlock())) {
            return level.getBlockEntity(pos) instanceof IPlanter planter
                    && planter.getPlant() == null && !planter.isHybridBase();
        }
        return false;
    }

    /**
     * 自动种植：调用副手种子物品自身的 {@code useOn}，兼容原版种子（放置作物方块）、
     * 本模组富集作物种子（耕地/作物架两种方式）以及其他模组的种子物品。
     */
    private boolean plantOne(Level level, Player player, BlockPos pos) {
        if (level.isClientSide) return false;
        if (!canPlantAt(level, pos, player)) return false;
        UseOnContext planting = new UseOnContext(player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        return player.getOffhandItem().useOn(planting).consumesAction();
    }

    // ==================== 批量施肥（副手肥料） ====================

    /**
     * 可施肥的作物方块。排除草方块、树苗等地形类的可催熟方块，
     * 避免按范围施肥时种草、长树造成意外。
     */
    private static boolean isFertilizableCrop(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.CROPS) || block instanceof CropBlock || block instanceof NetherWartBlock
                || block instanceof StemBlock || block instanceof SweetBerryBushBlock;
    }

    /**
     * 副手肥料能否对该位置施肥（与 {@link #fertilizeOne} 判定一致，供预判使用）。
     * <ul>
     *   <li>作物架：交给方块交互处理，当前实现认 mio_icif 肥料，空架没有可施肥的作物；</li>
     *   <li>普通作物：交给肥料物品自身的 {@code useOn}（原版骨粉等），mio_icif 肥料只针对作物架。</li>
     * </ul>
     */
    private static boolean canFertilize(Level level, BlockPos pos, Player player) {
        ItemStack fertilizer = player.getOffhandItem();
        if (fertilizer.isEmpty() || !fertilizer.is(Tags.Items.FERTILIZERS)) return false;
        BlockState state = level.getBlockState(pos);
        if (isCropStick(state.getBlock())) {
            // 与 CropInteractions 一致：肥料给作物架加营养值，营养已满时不再消耗肥料
            return MatronFertilizerItem.isFertilizer(fertilizer)
                    && level.getBlockEntity(pos) instanceof IPlanter planter
                    && planter.getPlant() != null
                    && planter.getNutrients() < MatronFertilizerItem.FERTILIZER_VALUE;
        }
        return !MatronFertilizerItem.isFertilizer(fertilizer) && isFertilizableCrop(state);
    }

    /**
     * 批量施肥一格：副手持有肥料标签（{@code c:fertilizers}）物品时，
     * 作物架调用方块自身的交互，普通作物调用肥料物品自身的 {@code useOn}。
     * 两者都按“是否真的消耗了物品/生效”判定，肥料从副手消耗。
     */
    private boolean fertilizeOne(Level level, Player player, BlockPos pos) {
        if (level.isClientSide) return false;
        if (!canFertilize(level, pos, player)) return false;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack fertilizer = player.getOffhandItem();
        if (isCropStick(level.getBlockState(pos).getBlock())) {
            return level.getBlockState(pos)
                    .useItemOn(fertilizer, level, player, InteractionHand.OFF_HAND, hit)
                    .consumesAction();
        }
        return fertilizer.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hit)).consumesAction();
    }

    // ==================== 除草 ====================

    /**
     * 可被锄掉的杂草：草、蕨、高草、枯木、花、树苗、蘑菇等 {@link BushBlock}。
     * 有产出价值的植物不算杂草——作物/瓜茎/浆果丛/地狱疣保持原样
     * （成熟作物走连锁收获，未成熟的不会被锄掉）。
     */
    private static boolean isRemovableVegetation(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.CROPS)) return false;
        Block block = state.getBlock();
        if (block instanceof CropBlock || block instanceof StemBlock || block instanceof SweetBerryBushBlock) {
            return false;
        }
        return block instanceof BushBlock;
    }

    /**
     * 是否为“可锄地的土壤”。原版/NeoForge 的 {@code HOE_TILL} 判定要求上方必须是空气，
     * 所以上方被杂草遮挡时无法锄地；这里用它决定是否值得先除草。
     * 耕地（{@link FarmBlock}）排除在外，避免把耕地上的作物当成杂草清掉。
     */
    private static boolean isTillableSoil(BlockState state) {
        if (state.getBlock() instanceof FarmBlock) return false;
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(BlockTags.DIRT);
    }

    /**
     * 计算该位置对应的“待锄土壤”。
     * 杂草层模式（射线打在杂草上、整片平面都是杂草层）下取下方土壤；
     * 普通模式下就是位置本身。
     */
    private static BlockPos soilFor(Level level, BlockPos target, boolean weedLayer) {
        if (!weedLayer) return target;
        BlockPos soil = target.below();
        for (int i = 0; i < 3 && isRemovableVegetation(level.getBlockState(soil)); i++) {
            soil = soil.below();
        }
        return soil;
    }

    /**
     * 本次需要清除的杂草位置：
     * <ul>
     *   <li>杂草层模式：目标与下方土壤之间（含目标）的全部杂草，含双高草的两段；</li>
     *   <li>普通模式：可锄土壤上方遮挡锄地的杂草（原版 {@code HOE_TILL} 要求上方为空气）。</li>
     * </ul>
     */
    private static List<BlockPos> weedsToClear(Level level, BlockPos target, BlockPos soilPos, boolean weedLayer) {
        if (weedLayer) {
            List<BlockPos> weeds = new ArrayList<>(2);
            for (BlockPos pos = target; !pos.equals(soilPos); pos = pos.below()) {
                if (isRemovableVegetation(level.getBlockState(pos))) {
                    weeds.add(pos);
                }
            }
            return weeds;
        }
        if (isTillableSoil(level.getBlockState(target))
                && isRemovableVegetation(level.getBlockState(target.above()))) {
            return List.of(target.above());
        }
        return List.of();
    }

    /** 预判该位置是否会被处理（锄地/除草/播种/施肥），用于决定右键是否消耗（避免空挥）。 */
    private static boolean wouldAct(Level level, UseOnContext context, BlockPos target, Player player, boolean weedLayer) {
        BlockPos soilPos = soilFor(level, target, weedLayer);
        if (!weedsToClear(level, target, soilPos, weedLayer).isEmpty()) return true;
        return canTill(level, context, soilPos) || canPlantAt(level, soilPos, player)
                || canFertilize(level, target, player) || canFertilize(level, target.above(), player);
    }

    /**
     * 清除杂草：掉落物/粒子/音效与玩家破坏一致（双高植物会被连带清除）。
     * 返回是否执行了清除。
     */
    private static boolean clearWeeds(Level level, BlockPos weedPos, Player player) {
        if (level.isClientSide) return true;
        return level.destroyBlock(weedPos, true, player);
    }

    // ==================== 锄地 ====================

    private static UseOnContext contextAt(UseOnContext source, BlockPos pos) {
        return new UseOnContext(source.getPlayer(), source.getHand(),
                new BlockHitResult(source.getClickLocation(), source.getClickedFace(), pos, source.isInside()));
    }

    private static boolean canTill(Level level, UseOnContext context, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        return state.getToolModifiedState(contextAt(context, pos), ItemAbilities.HOE_TILL, true) != null;
    }

    private static boolean tillOne(Level level, UseOnContext context, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        BlockState tilled = state.getToolModifiedState(contextAt(context, pos), ItemAbilities.HOE_TILL, false);
        if (tilled == null) return false;
        level.setBlock(pos, tilled, Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(context.getPlayer(), tilled));
        return true;
    }

    // ==================== 工具行为 ====================

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!hasEnoughEnergy(stack)) {
            return state.getDestroySpeed(level, pos) > 0 && !state.requiresCorrectToolForDrops();
        }
        return state.getDestroySpeed(level, pos) >= 0;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (!hasEnoughEnergy(stack)) return false;
        if (ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility)) return true;
        return super.canPerformAction(stack, itemAbility);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) return 1.0F;
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE)) return HOE_EFFICIENCY;
        return 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) return false;
        return state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            consumeEnergy(stack);
        }
        return true;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return HOE_ATTACK_DAMAGE_BONUS;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return 0;
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return getEnergy(stack) < getMaxEnergy();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // ==================== 提示 ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("hud.mio_icif.iron_hoe.tooltip_range", getRange(stack).displayName()));
        tooltip.add(Component.translatable("hud.mio_icif.iron_hoe.tooltip_harvest"));
        tooltip.add(Component.translatable("hud.mio_icif.iron_hoe.tooltip_fortune", BUILT_IN_FORTUNE_LEVEL));
        tooltip.add(Component.translatable("hud.mio_icif.iron_hoe.tooltip_weed"));
        tooltip.add(Component.translatable("hud.mio_icif.iron_hoe.tooltip_offhand"));
    }

    @Override
    public Component getEquipmentHudText(ItemStack stack) {
        return Component.translatable("hud.mio_icif.iron_hoe.display",
                getRange(stack).displayName(), getEnergy(stack), getMaxEnergy());
    }

    // ---- equipment console (IToolModeProvider)

    @Override
    public List<Component> toolModes(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        for (Range range : Range.values()) out.add(range.displayName());
        return out;
    }

    @Override
    public int toolModeIndex(ItemStack stack) {
        return getRange(stack).ordinal();
    }

    @Override
    public List<Component> toolModeDetails(ItemStack stack, int index) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("tool_mode.mio_icif.detail.cost", HOE_ENERGY_PER_USE));
        return out;
    }

    @Override
    public void selectToolMode(ItemStack stack, Player player, int index) {
        if (index < 0 || index >= Range.values().length) return;
        setRange(stack, Range.byIndex(index));
    }
}
