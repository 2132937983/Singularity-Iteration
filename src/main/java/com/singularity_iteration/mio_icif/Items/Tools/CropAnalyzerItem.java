package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Menu.Tool.CropAnalyzerMenu;
import com.singularity_iteration.mio_icif.api.crop.IPlanter;
import com.singularity_iteration.mio_icif.api.crop.PlantType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


/**
 * 作物分析器
 * 照搬原版IC2逻辑：
 * - 右键作物架：在聊天栏显示作物信息（消耗能量）
 * - 右键空气/其他方块：打开手持GUI进行种子扫描
 */
@SuppressWarnings({"null", "deprecation"})
public class CropAnalyzerItem extends mio_icif_tool_elc {

    private static final int MAX_ENERGY = 100000;
    private static final int ENERGY_PER_USE = 10;

    public CropAnalyzerItem(Properties properties) {
        super(properties, MAX_ENERGY, MAX_ENERGY, "item_crop_analyzer", 128, ENERGY_PER_USE, 2);
    }

    /**
     * 右键方块时的行为
     * 照搬IC2: onItemUseFirst - 如果右键作物架，在聊天栏显示信息
     * 优先级高于收获逻辑
     * 
     * 创造模式特殊功能：蹲下右键杂交架，强制进行杂交（用于测试）
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof IPlanter planter)) return InteractionResult.PASS;

        // 创造模式 + 蹲下 = 强制杂交（测试功能）
        if (player.isCreative() && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                // 检查是否是高级作物架且没有作物
                if (planter.getPlant() != null) {
                    player.sendSystemMessage(Component.literal("§c该位置已有作物，无法杂交"));
                    return InteractionResult.FAIL;
                }
                
                // 检查是否是高级作物架
                BlockState state = level.getBlockState(pos);
                boolean isUpgraded = state.getBlock() instanceof com.singularity_iteration.mio_icif.Blocks.Crop.mio_icif_crop_stick_upgraded;
                if (!isUpgraded) {
                    player.sendSystemMessage(Component.literal("§c需要使用高级作物架才能杂交"));
                    return InteractionResult.FAIL;
                }
                
                // 尝试强制杂交（创造模式跳过随机检查）
                boolean success = com.singularity_iteration.mio_icif.api.internal.crop.PlantHybridization.forceHybridize(planter);
                if (success) {
                    player.sendSystemMessage(Component.literal("§a杂交成功！生成了: " + planter.getPlant().getTypeId()));
                } else {
                    player.sendSystemMessage(Component.literal("§c杂交失败，请确保周围有至少2个可杂交的成熟作物"));
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 普通模式：显示作物信息
        if (!player.isShiftKeyDown()) {
            PlantType plant = planter.getPlant();
            if (plant == null) {
                // 没有作物，返回 SUCCESS 阻止收获逻辑，但不显示信息
                return InteractionResult.SUCCESS;
            }

            if (!level.isClientSide) {
                if (!consumeEnergy(context.getItemInHand(), energyForLevel(2))) {
                    player.sendSystemMessage(Component.translatable("message.mio_icif.crop_analyzer.no_energy"));
                    return InteractionResult.FAIL;
                }

                player.sendSystemMessage(Component.translatable("ic2.crop_analyzer.crop_name", Component.translatable(plant.getTranslationKey())));
                player.sendSystemMessage(Component.translatable("ic2.crop.analyzer.crop_discovered_by", plant.getFoundBy(player.getName().getString())));
                player.sendSystemMessage(Component.translatable("ic2.crop_analyzer.crop_age", planter.getGrowthStage()));
                player.sendSystemMessage(Component.translatable("ic2.crop_analyzer.crop_nutrients", planter.getNutrients()));
                player.sendSystemMessage(Component.translatable("ic2.crop_analyzer.crop_water", planter.getWater()));
                player.sendSystemMessage(Component.translatable("ic2.crop_analyzer.crop_weed_extra", planter.getWeedControl()));
                player.sendSystemMessage(Component.translatable("ic2.crop_analyzer.crop.growth_points", planter.getProgress(), plant.getGrowthTime(planter)));
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
        return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
    }

    /**
     * 右键空气时打开手持GUI
     * 照搬IC2: use - 打开手持容器GUI
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new CropAnalyzerMenu.Provider(stack, hand));
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    /**
     * 物品栏tick：从内部电池槽充电
     * 无论GUI是否打开，只要物品在玩家物品栏中就会持续充电
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player)) return;

        // 从内部电池槽充电
        ItemStack[] inventory = new ItemStack[3];
        mio_icif_tool_elc.loadHandHeldInventory(stack, inventory, level.registryAccess());

        ItemStack battery = inventory[2]; // SLOT_BATTERY = 2
        if (battery == null || battery.isEmpty()) return;
        var api = com.singularity_iteration.mio_icif.api.MioIcifAPI.instance().getItemAPI();
        if (!api.isBattery(battery)) return;

        long currentEnergy = getEnergy(stack);
        long maxEnergy = getMaxEnergy();
        if (currentEnergy >= maxEnergy) return;

        long transferRate = getChargeRate();
        long needed = Math.min(transferRate, maxEnergy - currentEnergy);
        long available = api.getBatteryStored(battery);
        long toTransfer = Math.min(needed, available);

        if (toTransfer > 0) {
            api.dischargeBattery(battery, toTransfer, false);
            addEnergy(stack, toTransfer);
            mio_icif_tool_elc.saveHandHeldInventory(stack, inventory, level.registryAccess());
        }
    }

    /**
     * 照搬IC2: energyForLevel
     * 不同扫描等级需要的能量
     */
    public static int energyForLevel(int level) {
        return switch (level) {
            default -> 10;
            case 1 -> 90;
            case 2 -> 900;
            case 3 -> 9000;
        };
    }
}