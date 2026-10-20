package com.miophas.singularity_iteration.common.item.lathe;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_resources;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

/**
 * Lathe turning blank, aligned with IC2 ItemLatheDefault.
 * The state is the thickness of each of the 5 positions (1 to the width), stored as
 * {@code state: {l0..l4}} in the custom data; a missing position is at full width.
 *
 * Materials:
 * - WOOD: wooden blank (width 3, hardness 0, gives sticks, chance 0.1)
 * - IRON: iron blank (width 5, hardness 2, gives small piles of iron dust, chance 0.5)
 */
@SuppressWarnings("null")
public class LatheTurningBlanksItem extends Item implements ILatheItem {

    private final LatheMaterial material;

    public LatheTurningBlanksItem(LatheMaterial material) {
        super(new Item.Properties().stacksTo(1));
        this.material = material;
    }

    public LatheMaterial getMaterial() {
        return material;
    }

    @Override
    public int getWidth(ItemStack stack) {
        return material.width;
    }

    @Override
    public int[] getCurrentState(ItemStack stack) {
        if (stack.isEmpty()) return new int[0];
        int[] ret = new int[5];
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null && customData.contains("state")
            ? customData.copyTag().getCompound("state") : null;
        for (int i = 0; i < ret.length; i++) {
            ret[i] = tag != null && tag.contains("l" + i) ? tag.getInt("l" + i) : getWidth(stack);
        }
        return ret;
    }

    @Override
    public void setState(ItemStack stack, int position, int value) {
        if (stack.isEmpty()) return;
        int clamped = Math.max(Math.min(value, getWidth(stack)), 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (!tag.contains("state")) tag.put("state", new CompoundTag());
            tag.getCompound("state").putInt("l" + position, clamped);
        });
    }

    @Override
    public ItemStack getOutputItem(ItemStack stack, int position) {
        return switch (material) {
            case WOOD -> new ItemStack(net.minecraft.world.item.Items.STICK);
            case IRON -> new ItemStack(mio_icif_resources.IRON_DUST_SMALL.get());
        };
    }

    @Override
    public float getOutputChance(ItemStack stack, int position) {
        return material.chance;
    }

    /** The 256 x 256 profile sheet the lathe GUI draws from (IC2 textures/items/turnables). */
    @Override
    public ResourceLocation getTexture(ItemStack stack) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/turnables/" + material.name + ".png");
    }

    @Override
    public int getHardness(ItemStack stack) {
        return material.harvestLevel;
    }

    /** Cuts made so far: the thickness removed from all positions. */
    public int getCutsMade(ItemStack stack) {
        int width = getWidth(stack), cuts = 0;
        for (int s : getCurrentState(stack)) cuts += width - s;
        return cuts;
    }

    /** Cuts a fresh blank takes until every position is at the minimum thickness of 1. */
    public int getMaxCuts(ItemStack stack) {
        return 5 * (getWidth(stack) - 1);
    }

    /**
     * The name of the shape and the item it is a part for ({@link LatheShapes}; IC2 key
     * {@code ic2.<item>.tooltip.<numeric state in hex>}), the processing stage, the hardness, the
     * by-product and, with Shift, the named shapes it can still be cut to. The profile with the
     * thickness of every position is the tooltip image.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        var named = LatheShapes.of(stack);
        if (named.isPresent()) {
            tooltip.add(named.get().name().copy().withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.mio_icif.turning_blanks.used_in",
                named.get().usedInItem().getDescription()).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            // IC2: any other shape with a name in the language file
            String shape = "ic2." + material.internalName + ".tooltip." + Integer.toHexString(getNumericState(getCurrentState(stack)));
            if (Language.getInstance().has(shape)) tooltip.add(Component.translatable(shape).withStyle(ChatFormatting.AQUA));
        }

        int cuts = getCutsMade(stack), max = getMaxCuts(stack);
        if (cuts <= 0) {
            tooltip.add(Component.translatable("item.mio_icif.turning_blanks.stage.blank").withStyle(ChatFormatting.GRAY));
        } else if (cuts < max) {
            tooltip.add(Component.translatable("item.mio_icif.turning_blanks.stage.turned", cuts, max).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item.mio_icif.turning_blanks.stage.finished").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("item.mio_icif.turning_blanks.hardness", getHardness(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mio_icif.turning_blanks.byproduct",
            getOutputItem(stack, 0).getHoverName(), Math.round(getOutputChance(stack, 0) * 100)).withStyle(ChatFormatting.GRAY));

        List<LatheShapes.Shape> targets = LatheShapes.reachable(stack);
        if (targets.isEmpty()) return;
        if (!flag.hasShiftDown()) {
            tooltip.add(Component.translatable("item.mio_icif.turning_blanks.targets.hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.mio_icif.turning_blanks.targets").withStyle(ChatFormatting.GRAY));
        for (LatheShapes.Shape target : targets) {
            tooltip.add(Component.translatable("item.mio_icif.turning_blanks.target", target.name(),
                target.profile(), LatheShapes.cutsTo(stack, target)).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new LatheProfileTooltip(stack));
    }

    /** Like a durability bar: the material left to cut, shown once the blank has been cut. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCutsMade(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = getMaxCuts(stack);
        return max <= 0 ? 0 : Math.round(13.0F * (max - getCutsMade(stack)) / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int max = getMaxCuts(stack);
        float left = max <= 0 ? 0.0F : (float) (max - getCutsMade(stack)) / max;
        return Mth.hsvToRgb(left / 3.0F, 1.0F, 1.0F);
    }

    /**
     * Gets the state as one numeric value.
     */
    public static int getNumericState(int[] state) {
        if (state == null || state.length != 5) return -1;
        int i = 0;
        for (int j = 0; j < 5; j++) {
            if (state[j] < 16) {
                i += state[j];
                if (j != 4) i <<= 4;
            }
        }
        return i;
    }

    /**
     * Restores the state array from the numeric value.
     */
    public static int[] getStateFromNumeric(int state) {
        int[] ret = new int[5];
        if (state == -1) return ret;
        for (int j = 4; j >= 0; j--) {
            ret[j] = state & 0xF;
            if (j != 0) state >>= 4;
        }
        return ret;
    }

    /**
     * Gets a new item stack for the state array.
     */
    public static ItemStack getStackForState(ItemStack stack, int[] state) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (state == null || state.length != 5 || !(stack.getItem() instanceof ILatheItem)) return stack.copy();

        ItemStack result = stack.copy();
        ILatheItem l = (ILatheItem) result.getItem();
        for (int i = 0; i < 5; i++) {
            l.setState(result, i, state[i]);
        }
        return result;
    }

    public static ItemStack getStackForState(ItemStack stack, int state) {
        return getStackForState(stack, getStateFromNumeric(state));
    }

    /**
     * Material enum for lathe workpieces (IC2 ItemLatheDefault.LatheMaterial).
     */
    @SuppressWarnings("null")
public enum LatheMaterial {
        WOOD("wood", "itemTurningBlanksWood", 0.1F, 3, 0),
        IRON("iron", "itemTurningBlanks", 0.5F, 5, 2);

        public final String name;
        /** IC2 InternalName, the prefix of the shape tooltip keys. */
        public final String internalName;
        public final float chance;
        public final int width;
        public final int harvestLevel;

        LatheMaterial(String name, String internalName, float chance, int width, int harvestLevel) {
            this.name = name;
            this.internalName = internalName;
            this.chance = chance;
            this.width = width;
            this.harvestLevel = harvestLevel;
        }
    }
}
