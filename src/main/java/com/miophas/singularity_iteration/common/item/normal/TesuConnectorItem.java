package com.miophas.singularity_iteration.common.item.normal;

import com.miophas.singularity_iteration.common.blockentity.batbox.TesuRelayBlockEntity;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWireless;
import com.miophas.singularity_iteration.common.blockentity.batbox.TesuWirelessPortBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.158, reworked 0.1.11.159 on the wireless power transmission node's remote (the electric wireless manager):
 * <ul>
 *   <li>sneak + right-click a TESU wireless output port or a TESU relay: it becomes the target (kept, with its dimension);</li>
 *   <li>right-click a relay: the relay is linked to the target, if the target is still a port or relay, in the same dimension,
 *       not the relay itself, the relay stands inside the target's cube (±{@link TesuWireless#RANGE}) and the link makes no loop.
 *       The target stays, so several relays can be linked to one port;</li>
 *   <li>right-click anything else (or the air): the target is told; sneak + right-click the air forgets it.</li>
 * </ul>
 * Every answer goes to the chat. The TESU blocks pass the connector's click on to it ({@code TesuWirelessBlock#useItemOn}); in
 * 0.1.11.158 the relay's status message took the click and the connector never linked anything. 0.1.11.167: a click on the
 * shaft or the head of a relay pylon is a click on the relay (its base).
 */
@SuppressWarnings("null")
public class TesuConnectorItem extends Item {
    private static final String KEY = "TesuTarget", DIM = "TesuDim";

    public TesuConnectorItem(Properties properties) { super(properties.stacksTo(1)); }

    /** The stored target, whatever its dimension, or {@code null}. */
    @Nullable
    public static BlockPos target(ItemStack stack) {
        int[] p = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntArray(KEY);
        return p.length == 3 ? new BlockPos(p[0], p[1], p[2]) : null;
    }

    private static boolean sameDimension(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return level.dimension().location().toString().equals(tag.getString(DIM));
    }

    private static void setTarget(ItemStack stack, Level level, @Nullable BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (pos == null) { tag.remove(KEY); tag.remove(DIM); }
            else { tag.putIntArray(KEY, new int[]{pos.getX(), pos.getY(), pos.getZ()}); tag.putString(DIM, level.dimension().location().toString()); }
        });
    }

    private static void say(@Nullable Player player, String key, Object... args) {
        if (player != null) player.sendSystemMessage(Component.translatable("message.mio_icif.tesu.connector." + key, args));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos relayBase = com.miophas.singularity_iteration.common.block.energycontainer.TesuRelayColumnBlock.base(level, pos);
        if (relayBase != null) pos = relayBase;   // 0.1.11.167: the pylon's shaft and head stand for its base
        BlockEntity be = level.getBlockEntity(pos);

        if (player != null && player.isShiftKeyDown()) {   // set the target
            if (!TesuWireless.isNode(be)) {
                say(player, "not_node");
                return InteractionResult.FAIL;
            }
            setTarget(stack, level, pos);
            say(player, "target_set", pos.getX(), pos.getY(), pos.getZ());
            return InteractionResult.SUCCESS;
        }

        BlockPos target = target(stack);
        if (be instanceof TesuRelayBlockEntity relay) {   // link it to the target
            if (target == null) {
                say(player, "no_target");
                return InteractionResult.FAIL;
            }
            if (!sameDimension(stack, level)) {
                say(player, "other_dim");
                return InteractionResult.FAIL;
            }
            if (target.equals(pos)) {
                say(player, "self");
                return InteractionResult.FAIL;
            }
            if (!level.isLoaded(target) || !TesuWireless.isNode(level.getBlockEntity(target))) {
                say(player, "gone", target.getX(), target.getY(), target.getZ());
                return InteractionResult.FAIL;
            }
            if (!TesuWireless.covers(target, pos)) {
                say(player, "too_far", target.getX(), target.getY(), target.getZ(), TesuWireless.RANGE);
                return InteractionResult.FAIL;
            }
            if (TesuWireless.leadsTo(level, target, pos)) {
                say(player, "loop");
                return InteractionResult.FAIL;
            }
            relay.link(target);
            say(player, "linked", pos.getX(), pos.getY(), pos.getZ(), target.getX(), target.getY(), target.getZ());
            return InteractionResult.SUCCESS;
        }

        if (be instanceof TesuWirelessPortBlockEntity) say(player, "port_hint");
        else tell(player, stack, level);
        return InteractionResult.SUCCESS;
    }

    private static void tell(@Nullable Player player, ItemStack stack, Level level) {
        BlockPos target = target(stack);
        if (target == null) say(player, "no_target");
        else if (!sameDimension(stack, level)) say(player, "other_dim");
        else say(player, "target_info", target.getX(), target.getY(), target.getZ());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                setTarget(stack, level, null);
                say(player, "cleared");
            } else tell(player, stack, level);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BlockPos target = target(stack);
        if (target != null) tooltip.add(Component.translatable("tooltip.mio_icif.tesu.connector.target", target.getX(), target.getY(), target.getZ())
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mio_icif.tesu.connector.use", TesuWireless.RANGE).withStyle(ChatFormatting.GRAY));
    }
}
