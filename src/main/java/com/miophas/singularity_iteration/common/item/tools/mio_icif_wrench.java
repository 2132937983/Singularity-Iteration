package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;

import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_item;
import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_water;
import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_water_extract;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractHeatBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractKineticBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import com.miophas.singularity_iteration.common.blockentity.pipe.mio_icif_pipe_default;
import com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_chamber;
import com.miophas.singularity_iteration.common.blockentity.transformer.mio_icif_transformer;
import com.miophas.singularity_iteration.common.blockentity.build.mio_icif_storage_box_entity;
import com.miophas.singularity_iteration.core.api.item.IWrenchItem;
import com.miophas.singularity_iteration.common.util.mio_icif_tags;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class mio_icif_wrench extends Item implements IWrenchItem {

    public static final int WRENCH_DURABILITY = 120;

    public static final TagKey<Block> WRENCH_CAN_DAMAGED = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("c", "wrench_can_damaged")
    );

    public mio_icif_wrench(Properties properties) {
        super(properties.durability(WRENCH_DURABILITY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    /**
     * 右键入口：静态订阅（{@link EventBusSubscriber}）保证只注册一次。
     * 不要在构造器里 addListener —— 那样每 new 一个扳手实例都会多注册一份，
     * 将来若出现第二个实例（例如再注册一个充能版），一次右键会被处理多遍
     * （连转两次 / 连扣两次耐久）。
     *
     * 语义对齐 IC2 的 ItemToolWrenchNew#onItemUseFirst：命中（机器 / 线缆 / 管道）
     * 就吃掉这次点击并返回 SUCCESS；未命中返回 PASS 交还方块，让 GUI 照常打开。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof mio_icif_wrench wrench) {
            wrench.handleInteraction(event);
        }
    }

    private void handleInteraction(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

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

        // 取消事件，阻止 GUI 打开。
        // 必须同时给出 cancellationResult：NeoForge 在事件被取消时返回的是 cancellationResult，
        // 默认值为 PASS —— 只 setCanceled(true) 的话原版仍会按 PASS 继续调用方块的
        // useWithoutItem，表现就是"扳手旋转/拆解的同时把机器 GUI 打开了"。
        // （写法与 IndependentSiEnergy 里变压器切换模式那段保持一致。）
        event.setCancellationResult(InteractionResult.SUCCESS);
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

        if (pipe instanceof com.miophas.singularity_iteration.common.blockentity.pipe.FluidPipeBlockEntity fluidPipe) {
            fluidPipe.toggleConnection(com.miophas.singularity_iteration.common.block.pipe.FluidPipeBlock.hitSide(state, pos, hitVec, clickedFace));
            damageWrench(stack, player, hand);
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
        dismantle(level, pos, state, player, wrenchStack, true);
        if (!player.isCreative()) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            wrenchStack.hurtAndBreak(10, player, slot);
        }
    }

    /**
     * Shared wrench pick-up (manual and electric wrench): the machine drops as an item that keeps
     * its settings and - with {@code keepContents} - its inventory (MachineItemData). The electric
     * wrench used its own copy without the inventory carry and spilled machine contents.
     */
    public static void dismantle(Level level, BlockPos pos, BlockState state, Player player, ItemStack wrenchStack, boolean keepContents) {
        Block block = state.getBlock();
        level.playSound(null, pos, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.MACHINE_DEMOLISH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            // SI machines without their own drop packing keep their inventory inside the item
            boolean carry = keepContents && block instanceof com.miophas.singularity_iteration.common.block.mio_icif_entity_block
                && state.is(mio_icif_tags.MACHINE) && usesGenericMachineDrops(block);
            if (carry) com.miophas.singularity_iteration.common.block.MachineItemData.carryContents(blockEntity, level.registryAccess());
            try {
                block.playerWillDestroy(level, pos, state, player);
                level.destroyBlock(pos, false);
                Block.dropResources(state, level, pos, blockEntity, player, wrenchStack);
            } finally {
                com.miophas.singularity_iteration.common.block.MachineItemData.endCarry();
            }
        } else {
            level.destroyBlock(pos, true);
        }
    }

    /** True when the block uses the shared machine drop (blocks with their own packing keep theirs). */
    private static boolean usesGenericMachineDrops(Block block) {
        try {
            return block.getClass().getMethod("getDrops", BlockState.class, net.minecraft.world.level.storage.loot.LootParams.Builder.class)
                .getDeclaringClass() == com.miophas.singularity_iteration.common.block.mio_icif_entity_block.class;
        } catch (NoSuchMethodException e) {
            return false;
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
            if (be instanceof AbstractEnergyBlockEntity energyBlock) {
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

        if (blockEntity instanceof AbstractEnergyBlockEntity) return true;
        if (blockEntity instanceof AbstractHeatBlockEntity) return true;
        if (blockEntity instanceof AbstractKineticBlockEntity) return true;
        if (blockEntity instanceof mio_icif_transformer) return true;
        if (blockEntity instanceof com.miophas.singularity_iteration.core.platform.neoforge.energy.IndependentTransformerBlockEntity) return true;
        if (blockEntity instanceof mio_icif_reactor_chamber) return true;
        if (blockEntity instanceof com.miophas.singularity_iteration.common.blockentity.build.mio_icif_storage_box_entity) return true;

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
