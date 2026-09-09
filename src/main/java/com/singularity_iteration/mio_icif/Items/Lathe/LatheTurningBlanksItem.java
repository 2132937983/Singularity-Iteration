package com.singularity_iteration.mio_icif.Items.Lathe;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.api.item.ILatheItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 车床加工件物�?
 * 对应 IC2 �?ItemLatheDefault
 * 
 * 材质枚举�?
 * - WOOD: 木质加工�?(宽度3, 硬度0, 产出木棍, 概率0.1)
 * - IRON: 铁质加工�?(宽度5, 硬度2, 产出小撮铁粉, 概率0.5)
 */
@SuppressWarnings("null")
public class LatheTurningBlanksItem extends Item implements ILatheItem {

    private final LatheMaterial material;

    public LatheTurningBlanksItem(LatheMaterial material) {
        super(new Item.Properties().stacksTo(1));
        this.material = material;
    }

    @Override
    public int getWidth(ItemStack stack) {
        return material.width;
    }

    @Override
    public int[] getCurrentState(ItemStack stack) {
        int[] ret = new int[5];
        CompoundTag stackTag = getCustomDataTag(stack);
        if (stackTag != null && stackTag.contains("state")) {
            CompoundTag tag = stackTag.getCompound("state");
            for (int i = 0; i < 5; i++) {
                if (tag.contains("l" + i)) {
                    ret[i] = tag.getInt("l" + i);
                } else {
                    ret[i] = getWidth(stack);
                }
            }
        } else {
            for (int i = 0; i < 5; i++) {
                ret[i] = getWidth(stack);
            }
        }
        return ret;
    }

    @Override
    public void setState(ItemStack stack, int position, int value) {
        if (stack.isEmpty()) return;
        CompoundTag stackTag = getCustomDataTag(stack);
        if (stackTag == null) {
            stackTag = new CompoundTag();
        }
        if (!stackTag.contains("state")) {
            stackTag.put("state", new CompoundTag());
        }
        stackTag.getCompound("state").putInt("l" + position,
            Math.max(1, Math.min(value, getWidth(stack))));
        setCustomDataTag(stack, stackTag);
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

    @Override
    public ResourceLocation getTexture(ItemStack stack) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/items/turnables/" + material.name + ".png");
    }

    @Override
    public int getHardness(ItemStack stack) {
        return material.harvestLevel;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        int[] state = getCurrentState(stack);
        
        // 显示各段状态?
        int max = getWidth(stack);
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 5; j++) {
            sb.append(Component.translatable("ic2.Lathe.gui.info", state[j], max).getString());
            if (j < 4) sb.append("   ");
        }
        tooltip.add(Component.literal(sb.toString()));
    }

    /**
     * 获取自定义数据标�?
     */
    @Nullable
    private CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        return null;
    }

    /**
     * 设置自定义数据标�?
     */
    private void setCustomDataTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 获取数值化的状态?
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
     * 从数值化状态恢复数据?
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
     * 根据状态数组获取新的物品栈
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
     * 车床加工件材质枚举?
     */
    @SuppressWarnings("null")
public enum LatheMaterial {
        WOOD("wood", 0.1F, 3, 0),
        IRON("iron", 0.5F, 5, 2);

        public final String name;
        public final float chance;
        public final int width;
        public final int harvestLevel;

        LatheMaterial(String name, float chance, int width, int harvestLevel) {
            this.name = name;
            this.chance = chance;
            this.width = width;
            this.harvestLevel = harvestLevel;
        }
    }
}

