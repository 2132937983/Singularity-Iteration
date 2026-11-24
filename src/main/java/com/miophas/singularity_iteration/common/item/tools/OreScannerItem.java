// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.menu.tool.mio_icif_od_scanner_menu;
import com.miophas.singularity_iteration.common.network.ODScannerResultPacket;
import com.miophas.singularity_iteration.common.tuning.SiTuning;
import com.miophas.singularity_iteration.core.api.item.IScannerItem;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The ore scanners, 0.1.11.199 (tool overhaul T5b, decision D7). The OD scanner (13×13×13) and the OV scanner (25×13×25)
 * were two near copies; they are now two tiers of this one scanner, and the OV is still crafted from an OD (the upgrade,
 * the charge carried over). Both stay items because the miner takes either as its scanner tier.
 *
 * <p>What changed for the player:
 * <ul>
 *   <li>Only blocks in {@code c:ores} count (U-07). A name containing "ore" counted before: the GESU core, the oil rig core,
 *       a spore blossom, a heavy core.</li>
 *   <li>Ores are grouped by their {@code c:ores/<kind>} tag (U-07): tin and platinum were told apart by the text "tin" in the
 *       block name, so platinum ore showed as tin.</li>
 *   <li>Each kind shows where its nearest block is: a compass direction, up or down, and the distance in blocks.</li>
 * </ul>
 */
public abstract class OreScannerItem extends mio_icif_tool_elc implements IScannerItem {

    /** Kinds with their own name in the language files; another kind shows the name of its nearest block. */
    private static final Set<String> NAMED_KINDS = Set.of("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald",
        "tin", "lead", "silver", "uranium", "aluminum", "nickel", "platinum", "zinc", "ruby", "sapphire", "quartz");
    private static final Map<Block, String> KIND = new java.util.concurrent.ConcurrentHashMap<>();

    private final SiTuning.IntValue radius;
    private final SiTuning.IntValue yRadius;
    private final SiTuning.IntValue cost;

    protected OreScannerItem(Properties properties, long maxEnergy, long energyPerUse, String name,
                             SiTuning.IntValue radius, SiTuning.IntValue yRadius, SiTuning.IntValue cost) {
        super(properties, maxEnergy, 0L, name, standardChargeRate(1), energyPerUse, 1);
        this.radius = radius;
        this.yRadius = yRadius;
        this.cost = cost;
    }

    @Override
    public long getEnergyPerUse() {
        return cost.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        long price = cost.get();
        if (!player.isCreative() && !consumeEnergy(stack, price)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mio_icif.tool.no_energy"), true);
            return InteractionResultHolder.fail(stack);
        }
        Map<String, Integer> results = scan(level, player.blockPosition(), radius.get(), yRadius.get());
        var provider = new mio_icif_od_scanner_menu.Provider(results);
        serverPlayer.openMenu(provider, provider::writeExtraData);
        PacketDistributor.sendToPlayer(serverPlayer, new ODScannerResultPacket(results));
        return InteractionResultHolder.success(stack);
    }

    /**
     * Counts the ores in the box round {@code centre}, by kind. Each result's key is the kind's name key, then {@code @} and
     * the offset of its nearest block from {@code centre} ({@code dx,dy,dz}); the screen turns that into a direction.
     */
    public static Map<String, Integer> scan(Level level, BlockPos centre, int radius, int yRadius) {
        Map<String, int[]> found = new HashMap<>();   // kind -> {count, dx, dy, dz, distance²}
        Map<String, String> names = new HashMap<>();
        int minY = Math.max(level.getMinBuildHeight(), centre.getY() - yRadius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, centre.getY() + yRadius);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    at.set(centre.getX() + x, y, centre.getZ() + z);
                    BlockState state = level.getBlockState(at);
                    if (!state.is(Tags.Blocks.ORES)) continue;
                    String kind = kind(state.getBlock());
                    int dy = y - centre.getY();
                    int d2 = x * x + dy * dy + z * z;
                    int[] entry = found.get(kind);
                    if (entry == null) {
                        found.put(kind, new int[] {1, x, dy, z, d2});
                        names.put(kind, nameKey(kind, state.getBlock()));
                    } else {
                        entry[0]++;
                        if (d2 < entry[4]) { entry[1] = x; entry[2] = dy; entry[3] = z; entry[4] = d2; }
                    }
                }
            }
        }
        Map<String, Integer> results = new LinkedHashMap<>();
        found.forEach((kind, e) -> results.put(names.get(kind) + "@" + e[1] + "," + e[2] + "," + e[3], e[0]));
        return results;
    }

    /** The ore kind of {@code block}: the {@code <kind>} of its {@code c:ores/<kind>} tag, or its id when it has none. */
    static String kind(Block block) {
        return KIND.computeIfAbsent(block, b -> b.builtInRegistryHolder().tags()
            .map(t -> t.location())
            .filter(l -> l.getNamespace().equals("c") && l.getPath().startsWith("ores/"))
            .map(l -> l.getPath().substring("ores/".length()))
            .sorted()
            .findFirst()
            .orElse(BuiltInRegistries.BLOCK.getKey(b).toString()));
    }

    private static String nameKey(String kind, Block nearest) {
        return NAMED_KINDS.contains(kind) ? "tooltip.mio_icif.ore." + kind : nearest.getDescriptionId();
    }

    /** Tags change on a data reload: the cached kinds are dropped then. */
    public static void clearKinds() {
        KIND.clear();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }
}
