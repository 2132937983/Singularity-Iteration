package com.singularity_iteration.mio_icif.Items.Tools;

import com.singularity_iteration.mio_icif.Blocks.Wire.mio_icif_block_wire;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_item;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water;
import com.singularity_iteration.mio_icif.Blocks.Pipe.mio_icif_block_pipe_water_extract;
import com.singularity_iteration.mio_icif.Blocks.entity.HUEntity.mio_icif_HeatU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.KUEntity.mio_icif_KineticU_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_Energy_Block;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.singularity_iteration.mio_icif.Blocks.entity.pipe.mio_icif_pipe_default;
import com.singularity_iteration.mio_icif.Blocks.entity.reactor.mio_icif_reactor_chamber;
import com.singularity_iteration.mio_icif.Blocks.entity.transformer.mio_icif_transformer;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
public class mio_icif_wrench extends Item implements IWrenchItem {

    public static final int WRENCH_DURABILITY = 120;

    public static final TagKey<Block> WRENCH_CAN_DAMAGED = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("c", "wrench_can_damaged")
    );

    public mio_icif_wrench(Properties properties) {
        super(properties.durability(WRENCH_DURABILITY));
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof mio_icif_wrench)) {
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
                    dismantleBlock(level, pos, state, player, stack, hand);
                    return;
                }
            }
            if (isWire) {
                handleWireInteraction(level, pos, state, player, stack, hitVec, clickedFace, hand);
            } else {
                handlePipeInteraction(level, pos, state, player, stack, hitVec, clickedFace, hand);
            }
            return;
        }

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // 特殊处理：储物箱直接右键拆除（不判断朝向）
        if (blockEntity instanceof mio_icif_storage_box_entity) {
            dismantleBlock(level, pos, state, player, stack, hand);
            return;
        }

        Direction currentFacing = getCurrentFacing(state);

        // 如果方块没有正面属性（如核反应仓），直接扳手右键即可拆除
        if (currentFacing == null) {
            dismantleBlock(level, pos, state, player, stack, hand);
            return;
        }

        // 有正面属性的方块
        if (player.isShiftKeyDown()) {
            rotateBlock(level, pos, state, player, stack, clickedFace.getOpposite());
            return;
        }

        if (currentFacing == clickedFace) {
            dismantleBlock(level, pos, state, player, stack, hand);
            return;
        }

        rotateBlock(level, pos, state, player, stack, clickedFace);
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
                damageWrench(stack, player, hand);
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
                damageWrench(stack, player, hand);
            }
        }
    }

    private void handlePipeInteraction(Level level, BlockPos pos, BlockState state,
                                       Player player, ItemStack stack, Vec3 hitVec, Direction clickedFace, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof mio_icif_pipe_default pipe)) {
            return;
        }

        boolean inCenter = isHitInCenter(hitVec, pos);

        if (inCenter) {
            if (pipe.isDirectionBlocked(clickedFace)) {
                pipe.unblockDirection(clickedFace);
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.mio_icif.pipe.unblock", clickedFace.getName()),
                        true
                    );
                }
                damageWrench(stack, player, hand);
            }
        } else {
            Direction armDir = getHitDirection(hitVec, pos, state);
            if (armDir != null && !pipe.isDirectionBlocked(armDir)) {
                pipe.blockDirection(armDir);
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("message.mio_icif.pipe.block", armDir.getName()),
                        true
                    );
                }
                damageWrench(stack, player, hand);
            }
        }
    }

    private void damageWrench(ItemStack stack, Player player, InteractionHand hand) {
        if (player == null || player.isCreative()) return;
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, slot);
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
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            wrenchStack.hurtAndBreak(10, player, slot);
        }
    }

    private InteractionResult rotateBlock(Level level, BlockPos pos, BlockState state, Player player, ItemStack wrenchStack, Direction targetFace) {
        BlockState newState = getRotatedState(state, targetFace);

        if (newState != null && newState != state) {
            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.5F, 1.0F);
            if (!player.isCreative()) {
                wrenchStack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
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

    /**
     * 检查方块实体是否是工业设备
     */
    @SuppressWarnings("unused")
    private boolean isIndustrialDevice(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }

        if (blockEntity instanceof mio_icif_Energy_Block) return true;
        if (blockEntity instanceof mio_icif_HeatU_Block) return true;
        if (blockEntity instanceof mio_icif_KineticU_Block) return true;
        if (blockEntity instanceof mio_icif_transformer) return true;
        if (blockEntity instanceof mio_icif_reactor_chamber) return true;
        if (blockEntity instanceof com.singularity_iteration.mio_icif.Blocks.entity.build.mio_icif_storage_box_entity) return true;

        return false;
    }

    /**
     * 判断点击位置是否在管道中心区域（4-12像素范围）
     */
    public static boolean isHitInCenter(Vec3 hitVec, BlockPos pos) {
        double lx = hitVec.x - pos.getX();
        double ly = hitVec.y - pos.getY();
        double lz = hitVec.z - pos.getZ();
        return lx >= 0.25 && lx <= 0.75 && ly >= 0.25 && ly <= 0.75 && lz >= 0.25 && lz <= 0.75;
    }

    /**
     * 根据点击位置判断是哪个方向被点击
     */
    public static Direction getHitDirection(Vec3 hitVec, BlockPos pos, BlockState state) {
        double lx = hitVec.x - pos.getX();
        double ly = hitVec.y - pos.getY();
        double lz = hitVec.z - pos.getZ();

        boolean inCenterX = lx >= 0.25 && lx <= 0.75;
        boolean inCenterY = ly >= 0.25 && ly <= 0.75;
        boolean inCenterZ = lz >= 0.25 && lz <= 0.75;

        if (inCenterX && inCenterY && inCenterZ) {
            return null;
        }

        if (inCenterX && inCenterY) {
            if (lz < 0.25) return Direction.NORTH;
            if (lz > 0.75) return Direction.SOUTH;
        }
        if (inCenterY && inCenterZ) {
            if (lx < 0.25) return Direction.WEST;
            if (lx > 0.75) return Direction.EAST;
        }
        if (inCenterX && inCenterZ) {
            if (ly < 0.25) return Direction.DOWN;
            if (ly > 0.75) return Direction.UP;
        }

        if (lz < 0.25) return Direction.NORTH;
        if (lz > 0.75) return Direction.SOUTH;
        if (lx < 0.25) return Direction.WEST;
        if (lx > 0.75) return Direction.EAST;
        if (ly < 0.25) return Direction.DOWN;
        if (ly > 0.75) return Direction.UP;

        return null;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }
}