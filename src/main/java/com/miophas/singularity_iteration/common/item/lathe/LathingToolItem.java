package com.miophas.singularity_iteration.common.item.lathe;

import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.miophas.singularity_iteration.core.api.util.IBoxable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Lathe tool, aligned with IC2 ItemLathingTool: its durability is the vanilla item damage (IC2
 * setMaxDamage(50)), a cut costs 1 point, it only cuts workpieces softer than itself, and it fits
 * into the toolbox.
 */
@SuppressWarnings("null")
public class LathingToolItem extends Item implements ILatheItem.ILatheTool, IBoxable {

    private final ToolMaterial material;

    public LathingToolItem(ToolMaterial material) {
        super(new Item.Properties().durability(material.maxDamage));
        this.material = material;
    }

    @Override
    public int getHardness(ItemStack stack) {
        return material.hardness;
    }

    @Override
    public int getCustomDamage(ItemStack stack) {
        return stack.getDamageValue();
    }

    @Override
    public int getMaxCustomDamage(ItemStack stack) {
        return stack.getMaxDamage();
    }

    @Override
    public void setCustomDamage(ItemStack stack, int damage) {
        stack.setDamageValue(damage);
    }

    /** IC2: stack.damageItem(damage, src), i.e. vanilla damage (unbreaking, creative, breaking). */
    @Override
    public boolean applyCustomDamage(ItemStack stack, int damage, LivingEntity entity) {
        if (entity != null && entity.level() instanceof ServerLevel level) {
            stack.hurtAndBreak(damage, level, entity, item -> {});
        } else {
            setCustomDamage(stack, getCustomDamage(stack) + damage);
        }
        return damage > 0;
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.mio_icif.lathing_tool.tooltip.hardness", material.hardness));
    }

    /**
     * Lathe tool materials.
     */
    @SuppressWarnings("null")
public enum ToolMaterial {
        IRON(3, 50),    // IC2 ItemLathingTool: hardness 3, 50 uses
        DIAMOND(4, 64); // project extension: hardness 4, 64 uses

        public final int hardness;
        public final int maxDamage;

        ToolMaterial(int hardness, int maxDamage) {
            this.hardness = hardness;
            this.maxDamage = maxDamage;
        }
    }
}
