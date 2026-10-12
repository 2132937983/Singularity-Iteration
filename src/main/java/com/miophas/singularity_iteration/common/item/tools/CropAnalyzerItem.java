// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.menu.tool.CropAnalyzerMenu;
import com.miophas.singularity_iteration.common.menu.tool.CropScannerState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * <p><b>EN:</b> Advanced Crop Scanner (0.1.8.2; the item id stays {@code crop_analyzer}, so old items
 * keep working). Tier 4 (EV), 4,000,000 EU, charge rate 2,048 EU/t.
 * <ul>
 *   <li>Right-click: opens the scanner menu (analysis grid, breeding research, gene editing; see
 *       {@link CropAnalyzerMenu}).</li>
 *   <li>Held in either hand with energy: the crop HUD shows the crop stick under the crosshair
 *       (see {@code CropScannerHud}). Each HUD reading costs {@link #HUD_EU_PER_READING} EU.</li>
 *   <li>Right-click a crop stick (not sneaking): a full diagnosis in the chat.</li>
 * </ul>
 * <p><b>ZH:</b> 次世代作物扫描仪（0.1.8.2；物品 ID 仍为 {@code crop_analyzer}，旧物品继续可用）。等级 4（EV），4,000,000 EU，充电速率 2,048 EU/t。
 * <ul>
 *   <li>右键：打开扫描仪菜单（分析网格、交配研究、基因编辑；见 {@link CropAnalyzerMenu}）。</li>
 *   <li>任一只手持有且有电：作物 HUD 显示准星下的作物架（见 {@code CropScannerHud}）。每次 HUD 读数消耗 {@link #HUD_EU_PER_READING} EU。</li>
 *   <li>右键作物架（不潜行）：在聊天栏输出完整诊断。</li>
 * </ul>
 * <p><b>JA:</b> 次世代型作物スキャナー（0.1.8.2。アイテム ID は {@code crop_analyzer} のままなので、旧アイテムもそのまま使える）。ティア 4（EV）、4,000,000 EU、充電速度 2,048 EU/t。
 * <ul>
 *   <li>右クリック：スキャナーのメニューを開く（分析グリッド、交配研究、遺伝子改変。{@link CropAnalyzerMenu} 参照）。</li>
 *   <li>どちらかの手に持ち、電力があるとき：作物 HUD が照準の先の作物棒を表示する（{@code CropScannerHud} 参照）。HUD の読み取り 1 回に {@link #HUD_EU_PER_READING} EU を使う。</li>
 *   <li>作物棒を右クリック（スニークなし）：チャットに完全な診断を出す。</li>
 * </ul>
 */
public class CropAnalyzerItem extends mio_icif_tool_elc {
    public static final long MAX_ENERGY = 4_000_000L;
    public static final long CHARGE_RATE = 2_048L;
    public static final int TIER = 4;
    /** EN: EU of one HUD reading (about two readings per second). ZH: 一次 HUD 读数的 EU（约每秒两次）。JA: HUD の読み取り 1 回の EU（約毎秒 2 回）。 */
    public static final long HUD_EU_PER_READING = 8L;
    /** EN: EU of one chat diagnosis. ZH: 一次聊天栏诊断的 EU。JA: チャット診断 1 回の EU。 */
    public static final long DIAGNOSIS_EU = 200L;

    public CropAnalyzerItem(Properties properties) {
        super(properties, MAX_ENERGY, 0L, CHARGE_RATE, HUD_EU_PER_READING, TIER);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown() && context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof com.miophas.singularity_iteration.core.api.crop.IPlanter crop) {
            if (!context.getLevel().isClientSide && consumeEnergy(context.getItemInHand(), DIAGNOSIS_EU)) {
                var plant = crop.getPlant();
                player.sendSystemMessage(Component.translatable("message.mio_icif.crop.diagnostic",
                    plant == null ? Component.translatable("message.mio_icif.crop_stick_empty")
                        : Component.translatable(plant.getTranslationKey()),
                    crop.getGrowthStage(), crop.getNutrients(), crop.getWater(), crop.getWeedControl(), crop.getProgress()));
                if (plant != null) player.sendSystemMessage(Component.translatable("message.mio_icif.crop.diagnostic_traits",
                    crop.getGrowthSpeed(), crop.getYield(), crop.getResilience(), crop.getHumidity(), crop.getSoilNutrients(),
                    crop.getAirQuality(), crop.getLightLevel()));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (!context.getLevel().isClientSide && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, context.getItemInHand(), context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) open(serverPlayer, stack, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void open(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        player.openMenu(new CropAnalyzerMenu.Provider(stack, hand), buffer -> buffer.writeEnum(hand));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.crop_scanner.hud").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.crop_scanner.menu").withStyle(ChatFormatting.GRAY));
        int unlocks = CropScannerState.unlocksOf(stack);
        tooltip.add(Component.translatable((unlocks & CropScannerState.UNLOCK_BREED) != 0
            ? "tooltip.mio_icif.crop_scanner.breed_on" : "tooltip.mio_icif.crop_scanner.breed_off")
            .withStyle((unlocks & CropScannerState.UNLOCK_BREED) != 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable((unlocks & CropScannerState.UNLOCK_GENE) != 0
            ? "tooltip.mio_icif.crop_scanner.gene_on" : "tooltip.mio_icif.crop_scanner.gene_off")
            .withStyle((unlocks & CropScannerState.UNLOCK_GENE) != 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
    }

    /**
     * <p><b>EN:</b> EU of one scan level in the 0.1.8.1 analyzer. Kept for addons; the 0.1.8.2 scanner uses {@code ScannerMath}.
     * <p><b>ZH:</b> 0.1.8.1 分析仪每个扫描等级的 EU。为附属保留；0.1.8.2 扫描仪使用 {@code ScannerMath}。
     * <p><b>JA:</b> 0.1.8.1 の分析機のスキャン段階ごとの EU。アドオン向けに残す。0.1.8.2 のスキャナーは {@code ScannerMath} を使う。
     */
    @Deprecated
    public static int energyForLevel(int level) {
        return switch (level) {
            case 1 -> 90;
            case 2 -> 900;
            case 3 -> 9000;
            default -> 10;
        };
    }
}
