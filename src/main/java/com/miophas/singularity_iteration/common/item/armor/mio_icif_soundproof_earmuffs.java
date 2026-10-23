package com.miophas.singularity_iteration.common.item.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 0.1.10.14: Soundproof Earmuffs. Worn in the helmet slot, or in the Curios head slot when Curios is
 * installed, they mute the machines for the wearer (MachineSoundManager): the working loops and the
 * job-finished chime. Alarms, explosions and tool sounds still play. No armor, no durability.
 */
@SuppressWarnings("null")
public class mio_icif_soundproof_earmuffs extends Item implements Equipable {

    public mio_icif_soundproof_earmuffs(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** True when {@code player} wears the earmuffs in the helmet slot or the Curios head slot. */
    public static boolean isWorn(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof mio_icif_soundproof_earmuffs) return true;
        return com.miophas.singularity_iteration.common.integration.CuriosIntegration.isCuriosLoaded()
            && com.miophas.singularity_iteration.common.integration.curios.EarmuffsCurios.isWorn(player);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.soundproof_earmuffs.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mio_icif.soundproof_earmuffs.slot").withStyle(ChatFormatting.DARK_GRAY));
    }
}
