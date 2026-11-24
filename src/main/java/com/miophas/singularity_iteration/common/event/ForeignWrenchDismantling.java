package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.util.mio_icif_tags;
import com.miophas.singularity_iteration.core.api.item.IWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets any wrench from another mod ({@code c:tools/wrench}: Mekanism Configurator,
 * Ender IO Yeta Wrench, ...) dismantle SI machines with sneak + right-click.
 *
 * <p>SI's own wrenches ({@link IWrenchItem}) keep their richer handler. Only the
 * sneaking click is claimed, so a foreign wrench's normal right-click (configure,
 * rotate) is left to its own mod. The drop goes through the block's loot path with
 * the wrench as tool, which for {@code c:machine} blocks yields the machine itself
 * with its stored energy; inventory contents spill as on any removal.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
public final class ForeignWrenchDismantling {
    private ForeignWrenchDismantling() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack wrench = event.getItemStack();
        if (!player.isShiftKeyDown() || wrench.isEmpty() || wrench.getItem() instanceof IWrenchItem
                || !wrench.is(Tags.Items.TOOLS_WRENCH)) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        // 0.1.11.171: machines, casings and ports as every SI wrench sees them
        if (!com.miophas.singularity_iteration.common.item.tools.WrenchRules.dismantlable(state)) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (level.isClientSide || !(player instanceof ServerPlayer)) return;
        // 0.1.11.193 (T0, U-02): the same check as SI's own wrenches, including the break event claim mods listen to
        if (!com.miophas.singularity_iteration.common.util.BlockEditPermission.canBreak(player, level, pos, state, event.getFace(), wrench)) return;

        // 0.1.11.198 (T5, D8): a block that refuses removal through IWrenchable keeps its place; one that chooses its drops gives those
        var wrenchable = com.miophas.singularity_iteration.common.item.tools.WrenchRules.wrenchable(level, pos, state);
        if (wrenchable != null && !wrenchable.wrenchCanRemove(level, pos, player)) return;
        var custom = com.miophas.singularity_iteration.common.item.tools.WrenchRules.customDrops(level, pos, state);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        level.playSound(null, pos, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.MACHINE_DEMOLISH.get(),
            SoundSource.BLOCKS, 1.0F, 1.0F);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        if (level.destroyBlock(pos, false, player) && !player.isCreative()) {
            if (custom != null) custom.forEach(drop -> Block.popResource(level, pos, drop));
            else Block.dropResources(state, level, pos, blockEntity, player, wrench);
        }
    }
}
