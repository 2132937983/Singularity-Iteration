package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;
import com.miophas.singularity_iteration.core.api.tool.ToggleableElectricTool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.UseAnim;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_quantum_sword extends mio_icif_tool_elc implements com.miophas.singularity_iteration.core.api.tool.IToolModeProvider, IEquipmentHudProvider {
    public static final int SWORD_MAX_ENERGY = 10000000;
    public static final int SWORD_CHARGE_RATE = 2048;
    public static final int SWORD_TIER = 4;
    public static final int ENERGY_PER_USE = 100;

    public static final int ATTACK_COST = 800;
    public static final int ACTIVATE_COST = 100;
    public static final float DAMAGE_NORMAL = 25.0F;
    public static final float DAMAGE_HYPER = 37.5F;

    private static final String TAG_HYPER = "QuantumSwordHyper";

    public mio_icif_quantum_sword(Properties properties) {
        super(properties.attributes(attributes()), SWORD_MAX_ENERGY, SWORD_MAX_ENERGY, "quantum_sword", SWORD_CHARGE_RATE, ENERGY_PER_USE, SWORD_TIER);
    }

    /**
     * Base damage and speed as real attribute modifiers, so the tooltip shows "Attack Damage" /
     * "Attack Speed" like a sword (player base 1 + 24 = 25). Hyper mode adds its extra on hit.
     * Sweeping ratio gives the quantum blade a strong sweep (50% of the hit).
     */
    private static net.minecraft.world.item.component.ItemAttributeModifiers attributes() {
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID, DAMAGE_NORMAL - 1.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.SWEEPING_DAMAGE_RATIO,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "quantum_sweep"), 0.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .build();
    }

    /** Vanilla sword abilities: sweeping attack (and sword digging of cobwebs etc.). */
    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) {
        return ability == net.neoforged.neoforge.common.ItemAbilities.SWORD_SWEEP
            || ability == net.neoforged.neoforge.common.ItemAbilities.SWORD_DIG
            || super.canPerformAction(stack, ability);
    }

    public static boolean isHyperState(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag().getBoolean(TAG_HYPER);
        }
        return false;
    }

    public static void setHyperState(ItemStack stack, boolean active) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean(TAG_HYPER, active);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean current = isHyperState(stack);
        if (current) {
            setHyperState(stack, false);
            player.sendSystemMessage(Component.translatable("message.mio_icif.quantum_sword.deactivated"));
        } else {
            if (hasEnoughEnergy(stack, ACTIVATE_COST)) {
                setHyperState(stack, true);
                player.sendSystemMessage(Component.translatable("message.mio_icif.quantum_sword.activated"));
            } else {
                player.sendSystemMessage(Component.translatable("message.mio_icif.quantum_sword.no_energy"));
            }
        }
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity instanceof LivingEntity living) {
            ItemStack mainHand = living.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.getItem() == this) {
                // the base 25 comes from the attribute modifier; only hyper mode adds on top
                return isHyperState(mainHand) ? DAMAGE_HYPER - DAMAGE_NORMAL : 0F;
            }
        }
        return 0F;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && isHyperState(stack)) {
            if (!consumeEnergy(stack, ATTACK_COST)) {
                setHyperState(stack, false);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isHyperState(stack)) {
            tooltip.add(Component.translatable("tooltip.mio_icif.quantum_sword.hyper"));
        }
    }

    @Override
    public Component getEquipmentHudText(ItemStack stack) {
        return Component.translatable("hud.mio_icif.quantum_sword.display",
            isHyperState(stack)
                ? Component.translatable("hud.mio_icif.quantum_sword.mode_hyper")
                : Component.translatable("hud.mio_icif.quantum_sword.mode_normal"),
            getEnergy(stack), getMaxEnergy());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || isHyperState(oldStack) != isHyperState(newStack);
    }

    // ---- equipment console (IToolModeProvider)
    @Override public java.util.List<net.minecraft.network.chat.Component> toolModes(ItemStack stack) {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.off"), net.minecraft.network.chat.Component.translatable("tool_mode.mio_icif.hyper"));
    }
    @Override public int toolModeIndex(ItemStack stack) { return isHyperState(stack) ? 1 : 0; }
}
