package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_od_scanner_menu;
import com.miophas.singularity_iteration.core.api.item.IScannerItem;
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
import com.miophas.singularity_iteration.common.network.ODScannerResultPacket;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * OV scanner.
 * Scans a 25x25x13 area around the player for ores.
 * Maximum energy: 1,000,000 EU.
 * Energy per scan: 96 EU.
 */
@SuppressWarnings("null")
public class mio_icif_ov_scanner extends mio_icif_tool_elc implements IScannerItem {

    // Maximum energy of the OV scanner, in EU.
    public static final int OV_SCANNER_MAX_ENERGY = 1000000;

    // Energy that one scan uses, in EU.
    public static final int OV_SCANNER_ENERGY_PER_USE = 96;

    // Horizontal scan radius on the XZ plane, centered on the player.
    public static final int SCAN_RADIUS = 12; // A 25x25 area gives a radius of 12.

    // Vertical scan radius on the Y axis.
    public static final int Y_RADIUS = 6; // 6 blocks above and 6 blocks below, 13 blocks in total.

    /**
     * Creates an OV scanner with no energy.
     * @param properties the item properties
     */
    public mio_icif_ov_scanner(Properties properties) {
        super(properties, OV_SCANNER_MAX_ENERGY, OV_SCANNER_MAX_ENERGY, "ov_scanner", OV_SCANNER_MAX_ENERGY, OV_SCANNER_ENERGY_PER_USE, 1);
    }

    /**
     * Creates an OV scanner with a given initial energy.
     * @param properties the item properties
     * @param initialEnergy the initial energy, in EU
     */
    public mio_icif_ov_scanner(Properties properties, int initialEnergy) {
        super(properties, OV_SCANNER_MAX_ENERGY, OV_SCANNER_MAX_ENERGY - initialEnergy, "ov_scanner", OV_SCANNER_MAX_ENERGY, OV_SCANNER_ENERGY_PER_USE, 1);
    }

    /**
     * Runs a scan when the player uses the item.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Run the scan only on the server.
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {

            // Check that the scanner has enough energy.
            // Cost per scan: datapack file si_tuning/ov_scanner.json.
            final long cost = com.miophas.singularity_iteration.common.tuning.SiTuningValues.OV_ENERGY_PER_USE.get();
            if (!hasEnoughEnergy(stack, cost)) {
                return InteractionResultHolder.fail(stack);
            }

            // Consume energy. Creative players use no energy.
            if (!player.isCreative()) {
                boolean consumed = consumeEnergy(stack, cost);
                if (!consumed) {
                    // If the energy consumption fails, do not scan.
                    return InteractionResultHolder.fail(stack);
                }
            }

            // Run the scan.
            Map<String, Integer> scanResults = performScan(level, player);

            // Open the GUI and show the results.
            openScannerGUI(serverPlayer, scanResults);

            // Put the changed ItemStack back into the player hand.
            player.setItemInHand(hand, stack);

            return InteractionResultHolder.success(stack);
        }

        // On the client, return the side-dependent result.
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Runs the scan.
     * The scan area is 6 blocks above and below the player Y level, and 25x25 on the XZ plane.
     * The total scan volume is 25x13x25 blocks.
     * @return a map from the ore translation key to the ore count
     */
    private Map<String, Integer> performScan(Level level, Player player) {
        Map<String, Integer> oreCounts = new HashMap<>();

        // Get the player position.
        BlockPos playerPos = player.blockPosition();

        // Scan the 25x13x25 area centered on the player.
        // Live values: datapack file si_tuning/ov_scanner.json (defaults SCAN_RADIUS, Y_RADIUS).
        final int radius = com.miophas.singularity_iteration.common.tuning.SiTuningValues.OV_SCAN_RADIUS.get();
        final int yRadius = com.miophas.singularity_iteration.common.tuning.SiTuningValues.OV_Y_RADIUS.get();
        // Build height: min is inclusive, max is exclusive (0.1.7.36: before, y == max was read).
        final int minY = Math.max(level.getMinBuildHeight(), playerPos.getY() - yRadius);
        final int maxY = Math.min(level.getMaxBuildHeight() - 1, playerPos.getY() + yRadius);
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    scanPos.set(playerPos.getX() + x, y, playerPos.getZ() + z);

                    BlockState state = level.getBlockState(scanPos);
                    Block block = state.getBlock();

                    // Count the block if it is an ore.
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
     * Checks if the block is an ore.
     */
    private boolean isOre(BlockState state) {
        // Check the vanilla ore tags.
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

        // Check the common NeoForge/Forge ore tag.
        if (state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) {
            return true;
        }

        // Check if the registry name contains "ore".
        Block block = state.getBlock();
        // cached per block: the scan asks this for thousands of positions per use
        return ORE_BY_NAME.computeIfAbsent(block, k -> BuiltInRegistries.BLOCK.getKey(k).getPath().contains("ore"));
    }

    private static final java.util.Map<Block, Boolean> ORE_BY_NAME = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Returns the translation key of the ore display name.
     */
    private String getOreName(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).toString();

        // Find the ore type from the registry name.
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

        // By default, return the block description ID.
        return block.getDescriptionId();   // translated on the client
    }

    /**
     * Opens the scanner GUI.
     */
    private void openScannerGUI(ServerPlayer player, Map<String, Integer> scanResults) {
        // Open the GUI through a MenuProvider.
        mio_icif_od_scanner_menu.Provider provider = new mio_icif_od_scanner_menu.Provider(scanResults);
        // the client-side menu factory reads the results from the open packet: always send them
        player.openMenu(provider, provider::writeExtraData);

        // Send the scan results to the client.
        PacketDistributor.sendToPlayer(player, new ODScannerResultPacket(scanResults));
    }

    /**
     * Prevents damage to the OV scanner.
     */
    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // The OV scanner uses energy, not durability. damageItem always returns 0.
        return 0;
    }

    /**
     * Makes the item bar show the OV scanner charge.
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        // Return true when the energy is not full. The item bar then shows the charge level.
        return getEnergy(stack) < getMaxEnergy();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

}

