package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Menu.Tool.mio_icif_od_scanner_menu;
import com.singularity_iteration.mio_icif.api.item.IScannerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import com.singularity_iteration.mio_icif.network.ODScannerResultPacket;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * OV?��??��??
 * ?��于扫??�玩家周?��25x25x13????��???????��?��
 * ???大�?��?��??1,000,000 EU
 * 毝次?��??��????��??96 EU
 */
@SuppressWarnings("null")
public class mio_icif_ov_scanner extends mio_icif_tool_elc implements IScannerItem {
    private static final Logger LOGGER = LogUtils.getLogger();

    // OV?��??�器???大�?��??
    public static final int OV_SCANNER_MAX_ENERGY = 1000000;

    // 毝次?��??��????��????��??
    public static final int OV_SCANNER_ENERGY_PER_USE = 96;

    // ?��??��???��（XZ平面，以?��家为中�??�?
    public static final int SCAN_RADIUS = 12; // 25x25????���???��??�?12

    // Y轴扫??��?��??
    public static final int Y_RADIUS = 6; // 上方6?��??��?�方6?��，�?��??13???

    /**
     * 默认?????�函?���?空电?��???�?
     * @param properties ??��??属性??
     */
    public mio_icif_ov_scanner(Properties properties) {
        super(properties, OV_SCANNER_MAX_ENERGY, OV_SCANNER_MAX_ENERGY, "ov_scanner", OV_SCANNER_MAX_ENERGY, OV_SCANNER_ENERGY_PER_USE, 1);
    }

    /**
     * 带�?��?��?��?��???????�函???
     * @param properties ??��??属性??
     * @param initialEnergy ??��?��?��??
     */
    public mio_icif_ov_scanner(Properties properties, int initialEnergy) {
        super(properties, OV_SCANNER_MAX_ENERGY, OV_SCANNER_MAX_ENERGY - initialEnergy, "ov_scanner", OV_SCANNER_MAX_ENERGY, OV_SCANNER_ENERGY_PER_USE, 1);
    }

    /**
     * ?��?��使用?��??��??
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ?��?��??�务端�?��?��?��??
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // �?试�?�出当�?��?��??
            LOGGER.info("[OV Scanner] Current energy: " + getEnergy(stack) + "/" + getMaxEnergy());
            LOGGER.info("[OV Scanner] Damage value: " + stack.getDamageValue());
            LOGGER.info("[OV Scanner] Energy per use: " + getEnergyPerUse());

            // �??��?��?��??�足够�?��??
            if (!hasEnoughEnergy(stack)) {
                LOGGER.info("[OV Scanner] Not enough energy!");
                return InteractionResultHolder.fail(stack);
            }

            // �???��?��??
            if (!player.isCreative()) {
                boolean consumed = consumeEnergy(stack);
                LOGGER.info("[OV Scanner] Energy consumed: " + consumed);
                if (!consumed) {
                    // ??��?��????�失败�?��?��?��?�扫???
                    return InteractionResultHolder.fail(stack);
                }
                LOGGER.info("[OV Scanner] Remaining energy: " + getEnergy(stack));
            }

            // ??��?�扫???
            Map<String, Integer> scanResults = performScan(level, player);
            LOGGER.info("[OV Scanner] Scan results: " + scanResults);

            // ??��??GUI并显示�?��??
            openScannerGUI(serverPlayer, scanResults);

            // ??�步修改??��??ItemStack??�玩家�?��??
            player.setItemInHand(hand, stack);

            return InteractionResultHolder.success(stack);
        }

        // 客�?�端?��?��返回?��?��??
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * ??��?�扫???
     * ?��??�玩家Y??��??上方6?��??��?�方6?��，XZ平面25x25????��???????��?��
     * ??��??25x13x25????��??��?????
     */
    private Map<String, Integer> performScan(Level level, Player player) {
        Map<String, Integer> oreCounts = new HashMap<>();

        // ?��??�玩家�???��??��?��??
        BlockPos playerPos = player.blockPosition();

        // ?��???25x13x25????���?以玩家为中�??�?
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                for (int y = -Y_RADIUS; y <= Y_RADIUS; y++) {
                    BlockPos scanPos = playerPos.offset(x, y, z);

                    // �??��Y??��???��?��?��??��??????��???
                    if (scanPos.getY() < level.getMinBuildHeight() || scanPos.getY() > level.getMaxBuildHeight()) {
                        continue;
                    }

                    BlockState state = level.getBlockState(scanPos);
                    Block block = state.getBlock();

                    // �??��?��?��?��?��?��
                    if (isOre(state)) {
                        String oreName = getOreName(block);
                        oreCounts.merge(oreName, 1, Integer::sum);
                    }
                }
            }
        }

        return oreCounts;
    }

    /**
     * �??��?��??�是?��?��?��?��
     */
    private boolean isOre(BlockState state) {
        // �??��?��?��?��??��??????��?��???�?
        if (state.is(BlockTags.COAL_ORES) ||
            state.is(BlockTags.IRON_ORES) ||
            state.is(BlockTags.COPPER_ORES) ||
            state.is(BlockTags.GOLD_ORES) ||
            state.is(BlockTags.REDSTONE_ORES) ||
            state.is(BlockTags.LAPIS_ORES) ||
            state.is(BlockTags.DIAMOND_ORES) ||
            state.is(BlockTags.EMERALD_ORES)) {
            return true;
        }

        // �??��?��?��?��??�用????��?��???签�?�NeoForge/Forge ??????�?
        if (state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) {
            return true;
        }

        // �??��?��??�注?????�是?��????��"ore"
        Block block = state.getBlock();
        String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (blockName.contains("ore") || blockName.contains("_ore")) {
            return true;
        }

        return false;
    }

    /**
     * ?��??�矿?��????��示�?�称�?返回?�翻译按键?
     */
    private String getOreName(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();

        // ??��?�矿?��类�??
        if (blockName.contains("coal")) return "tooltip.mio_icif.ore.coal";
        if (blockName.contains("iron")) return "tooltip.mio_icif.ore.iron";
        if (blockName.contains("copper")) return "tooltip.mio_icif.ore.copper";
        if (blockName.contains("gold")) return "tooltip.mio_icif.ore.gold";
        if (blockName.contains("redstone")) return "tooltip.mio_icif.ore.redstone";
        if (blockName.contains("lapis")) return "tooltip.mio_icif.ore.lapis";
        if (blockName.contains("diamond")) return "tooltip.mio_icif.ore.diamond";
        if (blockName.contains("emerald")) return "tooltip.mio_icif.ore.emerald";
        if (blockName.contains("tin")) return "tooltip.mio_icif.ore.tin";
        if (blockName.contains("lead")) return "tooltip.mio_icif.ore.lead";
        if (blockName.contains("silver")) return "tooltip.mio_icif.ore.silver";
        if (blockName.contains("uranium")) return "tooltip.mio_icif.ore.uranium";
        if (blockName.contains("aluminum") || blockName.contains("bauxite")) return "tooltip.mio_icif.ore.aluminum";
        if (blockName.contains("nickel")) return "tooltip.mio_icif.ore.nickel";
        if (blockName.contains("platinum")) return "tooltip.mio_icif.ore.platinum";
        if (blockName.contains("zinc")) return "tooltip.mio_icif.ore.zinc";
        if (blockName.contains("ruby")) return "tooltip.mio_icif.ore.ruby";
        if (blockName.contains("sapphire")) return "tooltip.mio_icif.ore.sapphire";
        if (blockName.contains("quartz")) return "tooltip.mio_icif.ore.quartz";

        // 默认返回?�方??��?�称
        return block.getName().getString();
    }

    /**
     * ??��???��??�器GUI
     */
    private void openScannerGUI(ServerPlayer player, Map<String, Integer> scanResults) {
        // 使用MenuProvider?��??��??GUI
        player.openMenu(new mio_icif_od_scanner_menu.Provider(scanResults));

        // ??��???��??��?��?��?�客??��??
        PacketDistributor.sendToPlayer(player, new ODScannerResultPacket(scanResults));
    }

    /**
     * ??��?��?�防止OV?��??�器??��??
     */
    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // OV?��??�器丝�?��?�damageItem??��?��?��?�是??��??�???��?��??
        return 0;
    }

    /**
     * ??��?��?�OV?��??�器?��示�?��?�状???
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        // 当�?��?�未满时返回?�true，显示�?��??度信???
        return getEnergy(stack) < getMaxEnergy();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

}

