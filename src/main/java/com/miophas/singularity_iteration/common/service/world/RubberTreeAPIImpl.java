package com.miophas.singularity_iteration.common.service.world;

import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.item.ITreeTapItem;
import com.miophas.singularity_iteration.core.api.world.IRubberTreeAPI;
import com.miophas.singularity_iteration.core.api.world.RubberHarvestResult;
import com.miophas.singularity_iteration.core.runtime.world.RubberTreeSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 橡胶树体系 API 实现。
 *
 * <p>所有规则委托给内容无关的 {@link RubberTreeSystem}，本类仅负责绑定
 * mio_icif 的树脂物品（{@code resource/item_harz}）。这样附属模组只需依赖
 * {@code core.api}，实现细节可自由重构。
 */
@SuppressWarnings("null")
public class RubberTreeAPIImpl implements IRubberTreeAPI {

    @Override
    public Item getResinItem() {
        return mio_icif_resources.HARZ.get();
    }

    @Override
    public ItemStack createResinStack(int count) {
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(getResinItem(), count);
    }

    @Override
    public boolean isTreeTap(ItemStack stack) {
        return RubberTreeSystem.isTreeTap(stack);
    }

    @Override
    public boolean isElectricTreeTap(ItemStack stack) {
        return RubberTreeSystem.isElectricTreeTap(stack);
    }

    @Override
    public boolean isRubberWood(BlockState state) {
        return RubberTreeSystem.isRubberWood(state);
    }

    @Override
    public boolean hasResin(BlockState state) {
        return RubberTreeSystem.hasResin(state);
    }

    @Override
    public boolean isTappable(BlockState state) {
        return RubberTreeSystem.isTappable(state);
    }

    @Override
    public boolean canHarvest(BlockState state) {
        return RubberTreeSystem.canHarvest(state);
    }

    @Override
    public boolean canUseTapOn(BlockState state, ItemStack tool) {
        return RubberTreeSystem.canUseTapOn(state, tool);
    }

    @Override
    public RubberHarvestResult tryHarvest(Level level, BlockPos pos, BlockState state,
                                          Player player, InteractionHand hand) {
        InteractionHand usedHand = hand == null ? InteractionHand.MAIN_HAND : hand;
        ItemStack tool = player == null ? ItemStack.EMPTY : player.getItemInHand(usedHand);
        return RubberTreeSystem.harvest(level, pos, state, player, usedHand, tool, getResinItem());
    }

    @Override
    public int rollResinCount(ItemStack tool, RandomSource random) {
        return tool != null && tool.getItem() instanceof ITreeTapItem tap
            ? RubberTreeSystem.rollResinCount(tap, random)
            : 0;
    }

    @Override
    public void dropResin(Level level, BlockPos pos, Player player, int count) {
        RubberTreeSystem.dropResin(level, pos, player, getResinItem(), count);
    }

    @Override
    public boolean tryRegrowResin(Level level, BlockPos pos, BlockState state, RandomSource random) {
        return RubberTreeSystem.tryRegrowResin(level, pos, state, random);
    }

    @Override
    public float getResinRegrowChance() {
        return RubberTreeSystem.RESIN_REGROW_CHANCE;
    }
}
