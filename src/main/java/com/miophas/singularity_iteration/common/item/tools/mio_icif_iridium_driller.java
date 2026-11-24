package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

/**
 * Iridium drill (IC2: 1,000,000 EU, 2,000 EU/t, 800 EU a block, speed 25) with its own Fortune III or Silk Touch.
 * 0.1.11.196 (tool overhaul T3): on {@link AbstractDrill}; drops from every pickaxe and shovel block (netherite tier),
 * areas 1×1, 3×3, 5×5 (G), Fortune / Silk Touch on sneak + G, attack 10 that costs a use and needs energy (it was 21 for
 * nothing, above the nanosaber of its tier).
 */
public class mio_icif_iridium_driller extends AbstractDrill implements IEquipmentHudProvider {
    public static final int IRIDIUM_DRILLER_MAX_ENERGY = 1000000;
    public static final int IRIDIUM_DRILLER_TRANSFER_LIMIT = 2000;
    public static final int IRIDIUM_DRILLER_ENERGY_PER_USE = 800;
    /** Added to the wielder's base damage: a player hits for 10. */
    public static final float IRIDIUM_DRILLER_ATTACK_DAMAGE = 9.0F;
    public static final float IRIDIUM_DRILLER_EFFICIENCY = 25.0F;

    /** Custom-data key of the loot mode; the miner reads it too. */
    private static final String MODE_KEY = "iridium_driller_mode";
    private static final int MODE_FORTUNE = 0;
    private static final int MODE_SILK_TOUCH = 1;

    public mio_icif_iridium_driller(Properties properties) {
        super(properties, IRIDIUM_DRILLER_MAX_ENERGY, IRIDIUM_DRILLER_TRANSFER_LIMIT, IRIDIUM_DRILLER_ENERGY_PER_USE, 3,
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, IRIDIUM_DRILLER_EFFICIENCY, IRIDIUM_DRILLER_ATTACK_DAMAGE, 2, Tiers.DIAMOND.getEnchantmentValue());
    }

    private static int getMode(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getInt(MODE_KEY) == MODE_SILK_TOUCH ? MODE_SILK_TOUCH : MODE_FORTUNE;
    }

    private static Component modeName(int mode) {
        return Component.translatable(mode == MODE_FORTUNE ? "hud.mio_icif.driller.mode_fortune" : "hud.mio_icif.driller.mode_silk");
    }

    /** G cycles the area; sneak + G switches Fortune III and Silk Touch (0.1.11.196: action bar, not chat). */
    @Override
    public void toggleActive(ItemStack stack, Player player) {
        if (!player.isShiftKeyDown()) {
            super.toggleActive(stack, player);
            return;
        }
        int next = getMode(stack) == MODE_FORTUNE ? MODE_SILK_TOUCH : MODE_FORTUNE;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, next));
        player.displayClientMessage(Component.translatable("hud.mio_icif.driller.switch_mode", modeName(next)), true);
    }

    /** The drill's own Fortune III or Silk Touch, on top of its enchantments (NeoForge enchantment level event). */
    public static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        ItemStack stack = event.getStack();
        if (!(stack.getItem() instanceof mio_icif_iridium_driller)) return;
        ItemEnchantments.Mutable enchantments = event.getEnchantments();
        HolderLookup.RegistryLookup<Enchantment> lookup = event.getLookup();
        if (getMode(stack) == MODE_FORTUNE) {
            lookup.get(Enchantments.FORTUNE).ifPresent(holder -> {
                if (event.isTargetting(holder)) enchantments.set(holder, Math.max(3, enchantments.getLevel(holder)));
            });
        } else {
            lookup.get(Enchantments.SILK_TOUCH).ifPresent(holder -> {
                if (event.isTargetting(holder)) enchantments.set(holder, 1);
            });
        }
    }

    @Override
    public Component getEquipmentHudText(ItemStack stack) {
        int side = getRadius(stack) * 2 + 1;
        return Component.translatable("hud.mio_icif.driller.display",
            Component.translatable("tooltip.mio_icif.drill.area", side, side).copy().append(" · ").append(modeName(getMode(stack))),
            formatEu(getEnergy(stack)), formatEu(getMaxEnergy(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("hud.mio_icif.driller.switch_mode", modeName(getMode(stack))).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
