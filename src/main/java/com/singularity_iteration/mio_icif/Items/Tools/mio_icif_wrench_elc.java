package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water_extract;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_default;

import com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity;
import com.singularity_iteration.mio_icif.api.item.IWrenchItem;
import com.singularity_iteration.mio_icif.util.mio_icif_tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@SuppressWarnings("null")
public class mio_icif_wrench_elc extends mio_icif_tool_elc implements IWrenchItem {

    public static final int WRENCH_ELC_MAX_ENERGY = 50000;
    public static final int WRENCH_ELC_ENERGY_PER_USE = 100;
    public static final int WRENCH_ELC_ROTATE_ENERGY = 5;
    public static final int WRENCH_ELC_PIPE_WIRE_ENERGY = 2;

    public static final TagKey<Block> WRENCH_CAN_DAMAGED = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("c", "wrench_can_damaged")
    );

    public mio_icif_wrench_elc(Properties properties) {
        super(properties, WRENCH_ELC_MAX_ENERGY, 0, "wrench_elc", WRENCH_ELC_MAX_ENERGY, WRENCH_ELC_ENERGY_PER_USE, 1);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
    }

    public mio_icif_wrench_elc(Properties properties, int initialEnergy) {
        super(properties, WRENCH_ELC_MAX_ENERGY, initialEnergy, "wrench_elc", WRENCH_ELC_MAX_ENERGY, WRENCH_ELC_ENERGY_PER_USE, 1);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 只处理主手，避免主副手重复触发
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof mio_icif_wrench_elc)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // 检查是否是可处理的方块（机器、线缆、管道）
        boolean isWire = state.getBlock() instanceof mio_icif_block_wire;
        boolean isPipe = state.getBlock() instanceof mio_icif_block_pipe_water
                      || state.getBlock() instanceof mio_icif_block_pipe_water_extract
                      || state.getBlock() instanceof mio_icif_block_pipe_item;
        boolean isMachine = state.is(mio_icif_tags.MACHINE);
        
        if (!isWire && !isPipe && !isMachine) {
            return;
        }

        // 取消事件，阻止 GUI 打开
        event.setCanceled(true);

        if (level.isClientSide) {
            return;
        }

        Direction clickedFace = event.getHitVec().getDirection();
        Vec3 hitVec = event.getHitVec().getLocation();
        InteractionHand hand = event.getHand();

        if (isWire || isPipe) {
            if (player.isShiftKeyDown()) {
                boolean canBeDismantled = state.is(WRENCH_CAN_DAMAGED);
                if (canBeDismantled) {
                    if (!hasEnoughEnergy(stack)) {
                        player.setItemInHand(hand, stack);
                        return;
                    }
                    dismantleBlock(level, pos, state, player, stack, hand);
                    player.setItemInHand(hand, stack);
                    return;
                }
            }
            if (!hasEnoughEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY)) {
                player.setItemInHand(hand, stack);
                return;
            }
            if (isWire) {
                handleWireInteraction(level, pos, state, player, stack, hitVec, clickedFace, hand);
            } else {
                handlePipeInteraction(level, pos, state, player, stack, hitVec, clickedFace, hand);
            }
            player.setItemInHand(hand, stack);
            return;
        }

        if (!state.is(mio_icif_tags.MACHINE)) {
            player.setItemInHand(hand, stack);
            return;
        }

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // 特殊处理：储物箱直接右键拆除（不判断朝向，不消耗旋转电量）
        if (blockEntity instanceof mio_icif_storage_box_entity) {
            if (!hasEnoughEnergy(stack)) {
                player.setItemInHand(hand, stack);
                return;
            }
            dismantleBlock(level, pos, state, player, stack, hand);
            player.setItemInHand(hand, stack);
            return;
        }

        if (player.isShiftKeyDown()) {
            rotateBlock(level, pos, state, player, stack, clickedFace.getOpposite());
            player.setItemInHand(hand, stack);
            return;
        }

        Direction currentFacing = getCurrentFacing(state);
        if (currentFacing != null && currentFacing == clickedFace) {
            if (!hasEnoughEnergy(stack)) {
                player.setItemInHand(hand, stack);
                return;
            }
            dismantleBlock(level, pos, state, player, stack, hand);
            player.setItemInHand(hand, stack);
            return;
        }

        rotateBlock(level, pos, state, player, stack, clickedFace);
        player.setItemInHand(hand, stack);
    }

    private void handleWireInteraction(Level level, BlockPos pos, BlockState state,
                                       Player player, ItemStack stack, Vec3 hitVec, Direction clickedFace, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof mio_icif_wire wireEntity)) {
            return;
        }

        boolean inCenter = mio_icif_block_wire.isHitInCenter(hitVec, pos);

        if (inCenter) {
            if (wireEntity.isDirectionBlocked(clickedFace)) {
                wireEntity.unblockDirection(clickedFace);
                ((mio_icif_block_wire) state.getBlock()).refreshConnections(level, pos);
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.mio_icif.cable.unblock", clickedFace.getName()),
                        true
                    );
                }
                consumeEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY);
            }
        } else {
            Direction armDir = mio_icif_block_wire.getHitDirection(hitVec, pos, state);
            if (armDir != null && !wireEntity.isDirectionBlocked(armDir)) {
                wireEntity.blockDirection(armDir);
                ((mio_icif_block_wire) state.getBlock()).refreshConnections(level, pos);
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.mio_icif.cable.block", armDir.getName()),
                        true
                    );
                }
                consumeEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY);
            }
        }
    }

    private void handlePipeInteraction(Level level, BlockPos pos, BlockState state,
                                       Player player, ItemStack stack, Vec3 hitVec, Direction clickedFace, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof mio_icif_pipe_default pipe)) {
            return;
        }

        boolean inCenter = mio_icif_wrench.isHitInCenter(hitVec, pos);

        if (inCenter) {
            if (pipe.isDirectionBlocked(clickedFace)) {
                pipe.unblockDirection(clickedFace);
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.mio_icif.pipe.unblock", clickedFace.getName()),
                        true
                    );
                }
                consumeEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY);
            }
        } else {
            Direction armDir = mio_icif_wrench.getHitDirection(hitVec, pos, state);
            if (armDir != null && !pipe.isDirectionBlocked(armDir)) {
                pipe.blockDirection(armDir);
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.mio_icif.pipe.block", armDir.getName()),
                        true
                    );
                }
                consumeEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY);
            }
        }
    }

    private InteractionResult rotateBlock(Level level, BlockPos pos, BlockState state, Player player, ItemStack wrenchStack, Direction targetFace) {
        if (!hasEnoughEnergy(wrenchStack, WRENCH_ELC_ROTATE_ENERGY)) {
            return InteractionResult.SUCCESS;
        }

        BlockState newState = getRotatedState(state, targetFace);

        if (newState != null && newState != state) {
            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.5F, 1.0F);
            if (!player.isCreative()) {
                consumeEnergy(wrenchStack, WRENCH_ELC_ROTATE_ENERGY);
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof mio_icif_Energy_Block energyBlock) {
                energyBlock.refreshRegistration();
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    private BlockState getRotatedState(BlockState state, Direction targetFace) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction currentFacing = state.getValue(BlockStateProperties.FACING);
            if (currentFacing == targetFace) return null;
            return state.setValue(BlockStateProperties.FACING, targetFace);
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (targetFace.getAxis().isHorizontal()) {
                Direction currentFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (currentFacing == targetFace) return null;
                return state.setValue(BlockStateProperties.HORIZONTAL_FACING, targetFace);
            }
            return null;
        }

        return null;
    }

    private Direction getCurrentFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return null;
    }

    private void dismantleBlock(Level level, BlockPos pos, BlockState state, Player player, ItemStack wrenchStack, InteractionHand hand) {
        Block block = state.getBlock();

        level.playSound(null, pos, com.singularity_iteration.mio_icif.mio_icif_sounds.MACHINE_DEMOLISH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity != null) {
            block.playerWillDestroy(level, pos, state, player);
            level.destroyBlock(pos, false);
            Block.dropResources(state, level, pos, blockEntity, player, wrenchStack);
        } else {
            level.destroyBlock(pos, true);
        }

        if (!player.isCreative()) {
            consumeEnergy(wrenchStack);
        }
    }

    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
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
}