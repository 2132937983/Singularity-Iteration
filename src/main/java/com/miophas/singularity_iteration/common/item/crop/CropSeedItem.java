package com.miophas.singularity_iteration.common.item.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.miophas.singularity_iteration.core.runtime.processing.CropSeedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Crop seed item.
 * Players plant it on crop sticks.
 * The plant type is stored in the item NBT data.
 */
@SuppressWarnings("null")
public class CropSeedItem extends Item implements ICropSeedItem {

    public CropSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || player.isSpectator() || stack.isEmpty() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.PASS;

        // 检查是否是种植架方法
   if (!isCropStick(state)) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IPlanter planter)) {
            return InteractionResult.PASS;
        }

        // 种子袋保留物品交互；原版基础种子从作物架的 useItemOn 进入。
        PlantType currentPlant = getPlantType(stack);
        if (currentPlant == null) {
            // Empty bags cannot clone a living crop; left-click uprooting supplies seeds.
            return InteractionResult.PASS;
        }
        return tryPlant(level, planter, stack, player, null, this);
    }

    /** Called by both crop-stick blocks after the normal vanilla/NeoForge interaction entrypoint. */
    public static ItemInteractionResult tryPlantBaseSeed(ItemStack stack, BlockState state, Level level,
                                                        BlockPos pos, Player player, Direction face) {
        // Seed bags and crop tools retain their own item interaction behavior.
        if (stack.isEmpty() || stack.getItem() instanceof ICropSeedItem || !isCropStick(state))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var baseSeed = PlantRegistry.instance.getBaseSeed(stack);
        if (baseSeed == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player == null || player.isSpectator() || !level.isLoaded(pos)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)
                || level.getBlockState(pos) != state
                || !(level.getBlockEntity(pos) instanceof IPlanter planter)
                || planter.getPlant() != null || planter.isHybridBase() || baseSeed.plantType == null)
            return ItemInteractionResult.FAIL;
        InteractionResult result = tryPlant(level, planter, stack, player, baseSeed, null);
        return result == InteractionResult.SUCCESS ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.FAIL;
    }

    /**
     * <p><b>EN:</b> Plants a seed bag or a registered base seed on an empty crop stick for a machine
     * (no player; since 0.1.8.2, used by the Ender IO Farming Station compatibility). The rules are
     * the same as for a player: the planter must be empty and not a crossing base, and the plant
     * must be able to grow there. It takes the seeds from {@code stack}. Server thread only.
     * <p><b>ZH:</b> 供机器（无玩家）在空作物架上种植种子袋或已注册的基础种子（0.1.8.2 起，Ender IO 农场站兼容使用）。规则与玩家相同：
     * 作物架必须为空且不是杂交基座，植物必须能在此生长。从 {@code stack} 扣除种子。仅限服务器线程。
     * <p><b>JA:</b> 機械（プレイヤーなし）が空の作物棒に種子袋か登録済みの基本種子を植える（0.1.8.2 以降、Ender IO の Farming Station 互換が使う）。
     * 規則はプレイヤーと同じ：作物棒は空で交配台ではなく、植物がそこで成長できること。{@code stack} から種子を取る。サーバースレッド専用。
     */
    public static boolean plantFromMachine(Level level, IPlanter planter, ItemStack stack) {
        if (level == null || level.isClientSide || stack.isEmpty()) return false;
        PlantRegistry.BaseSeed baseSeed = stack.getItem() instanceof ICropSeedItem ? null : PlantRegistry.instance.getBaseSeed(stack);
        if (!(stack.getItem() instanceof ICropSeedItem) && (baseSeed == null || baseSeed.plantType == null)) return false;
        if (planter.getPlant() != null || planter.isHybridBase()) return false;
        PlantType plantType = baseSeed == null ? getPlantType(stack) : baseSeed.plantType;
        int requiredSeeds = baseSeed == null ? 1 : Math.max(1, baseSeed.seed.getCount());
        if (plantType == null || !plantType.canGrow(planter) || stack.getCount() < requiredSeeds) return false;
        var traits = CropSeedData.traits(stack);
        planter.setPlant(plantType);
        planter.setGrowthStage(baseSeed == null ? 1 : Math.max(1, baseSeed.stage));
        planter.setGrowthSpeed(baseSeed == null ? traits.growth() : baseSeed.growthSpeed);
        planter.setYield(baseSeed == null ? traits.yield() : baseSeed.yield);
        planter.setResilience(baseSeed == null ? traits.resilience() : baseSeed.resilience);
        planter.setScanLevel(baseSeed == null ? traits.scan() : 1);
        planter.setProgress(0);
        planter.setHybridBase(false);
        planter.updateState();
        stack.shrink(requiredSeeds);
        return true;
    }

    /**
     * 尝试种植作物
     */
    private static InteractionResult tryPlant(Level level, IPlanter planter, ItemStack stack, Player player,
                                               PlantRegistry.BaseSeed baseSeed, Item emptyBagItem) {
        if (planter.getPlant() != null || planter.isHybridBase()) return InteractionResult.PASS;
        PlantType plantType = baseSeed == null ? getPlantType(stack) : baseSeed.plantType;
        int requiredSeeds = baseSeed == null ? 1 : Math.max(1, baseSeed.seed.getCount());
        if (plantType == null || !plantType.canGrow(planter) || stack.getCount() < requiredSeeds) return InteractionResult.PASS;

        if (!level.isClientSide) {
            var traits = CropSeedData.traits(stack);
            int stage = baseSeed == null ? 1 : Math.max(1, baseSeed.stage);
            int growth = baseSeed == null ? traits.growth() : baseSeed.growthSpeed;
            int yield = baseSeed == null ? traits.yield() : baseSeed.yield;
            int resilience = baseSeed == null ? traits.resilience() : baseSeed.resilience;
            planter.setPlant(plantType);
            planter.setGrowthStage(stage);
            planter.setGrowthSpeed(growth);
            planter.setYield(yield);
            planter.setResilience(resilience);
            planter.setScanLevel(baseSeed == null ? traits.scan() : 1);
            planter.setProgress(0);
            planter.setHybridBase(false);
            planter.updateState();

            // Consume the registered seed amount; seed bags are consumed as in IC2.
            if (!player.getAbilities().instabuild) {
                stack.shrink(requiredSeeds);

            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isCropStick(BlockState state) {
        return state.getBlock() instanceof com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick ||
               state.getBlock() instanceof com.miophas.singularity_iteration.common.block.crop.mio_icif_crop_stick_upgraded;
    }

    /**
     * Gets the plant type from the NBT data of the ItemStack.
     */
    public static PlantType getPlantType(ItemStack stack) {
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            String plantModId = tag.getString("PlantModId");
            String plantId = tag.getString("PlantId");
            if (!plantModId.isEmpty() && !plantId.isEmpty()) {
                return PlantRegistry.instance.getPlant(plantModId, plantId);
            }
        }
        return null;
    }

    @Override
    public Component getName(ItemStack stack) {
        PlantType plant = getPlantType(stack);
        int scanLevel = getScanLevel(stack);
        if (plant != null) {
            if (scanLevel == 0) {
                return Component.translatable("ic2.crop.unknown");
            }
            return Component.translatable("item.mio_icif.crop_seed", Component.translatable(plant.getTranslationKey()));
        }
        return Component.translatable(CropSeedData.isEmptyBag(stack)?"item.mio_icif.empty_seed_bag":"ic2.crop.unknown");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        PlantType plant = getPlantType(stack);
        int scanLevel = getScanLevel(stack);
        if (plant != null) {
            if (scanLevel == 0) {
                tooltipComponents.add(Component.translatable("ic2.crop.unknown").withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("tooltip.mio_icif.crop.scan_level_0"));
            } else {
                if (scanLevel >= 1) {
                    tooltipComponents.add(Component.translatable(plant.getTranslationKey()).withStyle(net.minecraft.ChatFormatting.GREEN));
                }
                if (scanLevel >= 2) {
                    tooltipComponents.add(Component.translatable("tooltip.mio_icif.crop.level", plant.getStats().getLevel()));
                }
                if (scanLevel >= 4) {
                    var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        var traits=CropSeedData.traits(stack);
                        int growthSpeed = traits.growth();
                        int yield = traits.yield();
                        int resilience = traits.resilience();

                        if (growthSpeed > 0 || yield > 0 || resilience > 0) {
                            tooltipComponents.add(Component.translatable("tooltip.mio_icif.crop.growth_speed", growthSpeed));
                            tooltipComponents.add(Component.translatable("tooltip.mio_icif.crop.yield", yield));
                            tooltipComponents.add(Component.translatable("tooltip.mio_icif.crop.resilience", resilience));
                        }
                    }
                }
                tooltipComponents.add(Component.translatable("tooltip.mio_icif.crop.scan_level", scanLevel));
            }
        } else if(CropSeedData.isEmptyBag(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.empty_seed_bag"));
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.empty_seed_bag.usage"));
        } else {
            tooltipComponents.add(Component.translatable("ic2.crop.unknown").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * 创建一个带有属性的种子
     */
    public static ItemStack createSeedStack(Item item, String plantModId, String plantId, int growthSpeed, int yield, int resilience) {
        return CropSeedData.fillOne(new ItemStack(item),plantModId,plantId,growthSpeed,yield,resilience);
    }

    public static int getScanLevel(ItemStack stack) {
        return CropSeedData.traits(stack).scan();
    }

    public static void setScanLevel(ItemStack stack, int level) {
        CropSeedData.setScan(stack,level);
    }

    public static void incrementScanLevel(ItemStack stack) {
        setScanLevel(stack, getScanLevel(stack) + 1);
    }

    public static int getGrowthFromStack(ItemStack stack) {
        return CropSeedData.traits(stack).growth();
    }

    public static int getGainFromStack(ItemStack stack) {
        return CropSeedData.traits(stack).yield();
    }

    public static int getResistanceFromStack(ItemStack stack) {
        return CropSeedData.traits(stack).resilience();
    }
}
