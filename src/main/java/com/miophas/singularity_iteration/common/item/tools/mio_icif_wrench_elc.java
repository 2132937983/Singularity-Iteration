package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;

import com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire;
import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_item;
import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_water;
import com.miophas.singularity_iteration.common.block.pipe.mio_icif_block_pipe_water_extract;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.mio_icif_wire;
import com.miophas.singularity_iteration.common.blockentity.pipe.mio_icif_pipe_default;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
@SuppressWarnings("null")
public class mio_icif_wrench_elc extends mio_icif_tool_elc implements IWrenchItem, com.miophas.singularity_iteration.core.api.item.IFeatureToolItem {

    public static final int WRENCH_ELC_MAX_ENERGY = 50000;
    public static final int WRENCH_ELC_ENERGY_PER_USE = 100;
    public static final int WRENCH_ELC_ROTATE_ENERGY = 5;
    public static final int WRENCH_ELC_PIPE_WIRE_ENERGY = 2;

    // 0.1.11.172: c:wrench_can_damaged is gone; what a wrench takes apart is mio_icif:wrenchable (WrenchRules.dismantlable)

    public mio_icif_wrench_elc(Properties properties) {
        super(properties, WRENCH_ELC_MAX_ENERGY, 0, "wrench_elc", standardChargeRate(1), WRENCH_ELC_ENERGY_PER_USE, 1);
    }

    public mio_icif_wrench_elc(Properties properties, int initialEnergy) {
        super(properties, WRENCH_ELC_MAX_ENERGY, initialEnergy, "wrench_elc", standardChargeRate(1), WRENCH_ELC_ENERGY_PER_USE, 1);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    /**
     * 右键入口：静态订阅（{@link EventBusSubscriber}）保证只注册一次。
     * 电力扳手原先有 2 个构造器、各自 addListener，只要两个构造器都被用到就会
     * 重复注册（一次右键处理两遍：连转两次、连扣两份电）。语义同普通扳手，
     * 对齐 IC2 ItemToolWrenchNew#onItemUseFirst：命中即吃掉点击，未命中交还方块。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof mio_icif_wrench_elc wrench) {
            wrench.handleInteraction(event);
        }
    }

    private void handleInteraction(PlayerInteractEvent.RightClickBlock event) {
        // 只处理主手，避免主副手重复触发
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

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
        // 0.1.11.171: machines, casings and ports, as every SI wrench sees them (WrenchRules)
        boolean isMachine = WrenchRules.dismantlable(state);
        
        if (!isWire && !isPipe && !isMachine) {
            return;
        }

        // 取消事件，阻止 GUI 打开。
        // 必须同时给出 cancellationResult：NeoForge 在事件被取消时返回的是 cancellationResult，
        // 默认值 PASS 会让原版继续调用方块的 useWithoutItem（表现：旋转/拆解的同时弹出机器 GUI）。
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (level.isClientSide) {
            return;
        }
        // spectators, adventure mode, spawn protection and claims: same checks as a vanilla edit
        if (!com.miophas.singularity_iteration.common.util.BlockEditPermission.canEdit(player, level, pos,
                event.getFace(), stack)) {
            return;
        }

        Direction clickedFace = event.getHitVec().getDirection();
        Vec3 hitVec = event.getHitVec().getLocation();
        InteractionHand hand = event.getHand();

        if (isWire || isPipe) {
            if (player.isShiftKeyDown()) {
                boolean canBeDismantled = WrenchRules.dismantlable(state);
                if (canBeDismantled) {
                    if (!ready(stack, player, getEnergyPerUse())) {
                        player.setItemInHand(hand, stack);
                        return;
                    }
                    dismantleBlock(level, pos, state, player, stack, hand);
                    player.setItemInHand(hand, stack);
                    return;
                }
            }
            if (!ready(stack, player, WRENCH_ELC_PIPE_WIRE_ENERGY)) {
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

        if (!isMachine) {
            player.setItemInHand(hand, stack);
            return;
        }

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // 特殊处理：储物箱直接右键拆除（不判断朝向，不消耗旋转电量）
        if (blockEntity instanceof mio_icif_storage_box_entity) {
            if (!ready(stack, player, getEnergyPerUse())) {
                player.setItemInHand(hand, stack);
                return;
            }
            dismantleBlock(level, pos, state, player, stack, hand);
            player.setItemInHand(hand, stack);
            return;
        }

        // 0.1.11.171: the rules every SI wrench shares (WrenchRules). This wrench never dismantled a block without a facing (it
        // tried to turn it), and with front dismantle off it dismantled nothing; off now means: only a sneaking click dismantles.
        boolean frontDismantle = com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(stack, FEATURE_FRONT_DISMANTLE);
        switch (WrenchRules.consult(level, pos, state, player, WrenchRules.action(state, clickedFace, player.isShiftKeyDown(), frontDismantle), clickedFace)) {
            case DISMANTLE -> {
                if (ready(stack, player, getEnergyPerUse())) dismantleBlock(level, pos, state, player, stack, hand);
            }
            case ROTATE -> rotateBlock(level, pos, state, player, stack, clickedFace);
            case ROTATE_OPPOSITE -> rotateBlock(level, pos, state, player, stack, clickedFace.getOpposite());
            case NONE -> { }
        }
        player.setItemInHand(hand, stack);
    }

    private void handleWireInteraction(Level level, BlockPos pos, BlockState state,
                                       Player player, ItemStack stack, Vec3 hitVec, Direction clickedFace, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof mio_icif_wire wireEntity)) {
            return;
        }

        // 0.1.11.29: a cable inside foam or a wall is hit on the block's outer face: the clicked face toggles (the arms
        // cannot be told apart there, so a blocked face could not be opened again)
        if (mio_icif_block_wire.foamlogged(state)) {
            boolean blocked = wireEntity.isDirectionBlocked(clickedFace);
            if (blocked) wireEntity.unblockDirection(clickedFace); else wireEntity.blockDirection(clickedFace);
            ((mio_icif_block_wire) state.getBlock()).refreshConnections(level, pos);
            if (player != null) player.displayClientMessage(
                Component.translatable(blocked ? "message.mio_icif.cable.unblock" : "message.mio_icif.cable.block", clickedFace.getName()), true);
            consumeEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY);
            return;
        }
        boolean inCenter = mio_icif_block_wire.isHitInCenter(hitVec, pos, state);   // 0.1.10.3: the cable's own thickness

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

        if (pipe instanceof com.miophas.singularity_iteration.common.blockentity.pipe.FluidPipeBlockEntity fluidPipe) {
            if (!ready(stack, player, WRENCH_ELC_PIPE_WIRE_ENERGY)) return;
            fluidPipe.toggleConnection(com.miophas.singularity_iteration.common.block.pipe.FluidPipeBlock.hitSide(state, pos, hitVec, clickedFace));
            consumeEnergy(stack, WRENCH_ELC_PIPE_WIRE_ENERGY);
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

    /**
     * 0.1.11.198 (T5, U-12): may the wrench act for {@code amount} EU? Creative needs no charge; a flat wrench says so on the
     * action bar (it did nothing without a word, the click was swallowed).
     */
    private boolean ready(ItemStack stack, Player player, long amount) {
        if (player.isCreative() || hasEnoughEnergy(stack, amount)) return true;
        if (!player.level().isClientSide) player.displayClientMessage(Component.translatable("message.mio_icif.tool.no_energy"), true);
        return false;
    }

    private InteractionResult rotateBlock(Level level, BlockPos pos, BlockState state, Player player, ItemStack wrenchStack, Direction targetFace) {
        if (!ready(wrenchStack, player, WRENCH_ELC_ROTATE_ENERGY)) {
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

    private void dismantleBlock(Level level, BlockPos pos, BlockState state, Player player, ItemStack wrenchStack, InteractionHand hand) {
        mio_icif_wrench.dismantle(level, pos, state, player, wrenchStack,
            com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures.isEnabled(wrenchStack, FEATURE_KEEP_CONTENTS));
        if (!player.isCreative()) {
            consumeEnergy(wrenchStack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        com.miophas.singularity_iteration.core.prefab.item.ArmorFeatureTooltip.appendAll(tooltip, stack, getFeatures(stack));
    }

    // ---- equipment console traits
    /** Machines keep their inventory inside the dropped item (off: contents spill as with a pickaxe). */
    public static final String FEATURE_KEEP_CONTENTS = "keep_contents";
    /** Clicking a machine's front face dismantles it (off: only sneak-dismantle, a safety against misclicks). */
    public static final String FEATURE_FRONT_DISMANTLE = "front_dismantle";

    @Override
    public java.util.List<com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return java.util.List.of(
            new com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo(net.minecraft.world.entity.EquipmentSlot.MAINHAND, FEATURE_KEEP_CONTENTS, "tooltip.mio_icif.tool.feature_keep_contents"),
            new com.miophas.singularity_iteration.core.api.item.ArmorFeatureInfo(net.minecraft.world.entity.EquipmentSlot.MAINHAND, FEATURE_FRONT_DISMANTLE, "tooltip.mio_icif.tool.feature_front_dismantle"));
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
