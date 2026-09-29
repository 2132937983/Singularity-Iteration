package com.miophas.singularity_iteration.common.item.resource;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 记忆水晶物品。
 * 用于存储模式存储机中的扫描结果
 *
 * 可以存储一个扫描结果，包含：
 * - 物品信息
 * - UU物质消耗
 * - EU能量消耗
 */
@SuppressWarnings("null")
public class mio_icif_memory extends Item {
    private static final String CONTAINER_VERSION="scex_pattern_container";
    private static final String[] OWNED_KEYS={"item_id","item_count","item_name","uu_matter_cost_buckets","uu_matter_cost","energy_cost",CONTAINER_VERSION};

    public mio_icif_memory(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 检查记忆水晶是否已存储数据
     */
    public boolean hasData(ItemStack stack) {
        CompoundTag tag=getDataTag(stack);
        return tag!=null&&(tag.contains(CONTAINER_VERSION)||tag.contains("item_id")||tag.contains("item_name"));
    }

    /**
     * 获取存储的物品名称
     */
    public String getStoredItemName(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        if (tag != null && tag.contains("item_name")) {
            return tag.getString("item_name");
        }
        return "";
    }

    /**
     * 获取存储的物品堆
     */
    public ItemStack getStoredItemStack(ItemStack stack) {
        CompoundTag tag=getDataTag(stack);
        return tag!=null&&validCosts(tag)?getStoredItemIdentity(stack):ItemStack.EMPTY;
    }

    /** Identity-only migration boundary; returned identity never authorizes a stored price. */
    public ItemStack getStoredItemIdentity(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        if (tag == null || stack.getItem()!=this || stack.getCount()!=1) return ItemStack.EMPTY;
        if(tag.contains(CONTAINER_VERSION)) {
            if(!tag.contains(CONTAINER_VERSION,net.minecraft.nbt.Tag.TAG_INT)||tag.getInt(CONTAINER_VERSION)!=1)return ItemStack.EMPTY;
            var contents=stack.get(DataComponents.CONTAINER);
            if(contents==null||contents.getSlots()!=1)return ItemStack.EMPTY;
            var item=contents.copyOne();
            return item.getCount()==1?item:ItemStack.EMPTY;
        }
        if (tag.contains("item_id",net.minecraft.nbt.Tag.TAG_STRING)&&tag.contains("item_count",net.minecraft.nbt.Tag.TAG_INT)
                &&!stack.has(DataComponents.CONTAINER)) {
            String itemId = tag.getString("item_id");
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(itemId);
            if (id != null) {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    int count = tag.getInt("item_count");
                    if (count == 1) return new ItemStack(item);
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 获取存储的UU物质消耗
     */
    public double getUuMatterCost(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        return tag!=null&&validCosts(tag)?buckets(tag):0;
    }

    /**
     * 获取存储的EU能量消耗
     */
    public long getEnergyCost(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        if (tag != null && validCosts(tag)) {
            return tag.getLong("energy_cost");
        }
        return 0;
    }

    /**
     * 存储扫描结果到记忆水晶（旧版，使用物品名称字符串）
     */
    public void storeData(ItemStack stack, String itemName, double uuMatterCostBuckets, long energyCost) {
        if(!canReplace(stack)||itemName==null||itemName.isBlank()||!com.miophas.singularity_iteration.common.processing.StoredPattern.validCosts(uuMatterCostBuckets,energyCost))return;
        CompoundTag tag = cleanOwnedTag(stack);
        tag.putString("item_name", itemName);
        tag.putDouble("uu_matter_cost_buckets", uuMatterCostBuckets);
        tag.putLong("energy_cost", energyCost);
        if(ownsContainer(stack))stack.remove(DataComponents.CONTAINER);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 存储扫描结果到记忆水晶（新版，使用物品堆）
     */
    public void storeData(ItemStack stack, ItemStack storedItem, double uuMatterCostBuckets, long energyCost) {
        tryStoreData(stack,storedItem,uuMatterCostBuckets,energyCost);
    }

    public boolean tryStoreData(ItemStack stack,ItemStack storedItem,double uuMatterCostBuckets,long energyCost) {
        if(!canReplace(stack)||!com.miophas.singularity_iteration.common.processing.StoredPattern.valid(storedItem,uuMatterCostBuckets,energyCost))return false;
        var content=net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(storedItem));
        CompoundTag tag = cleanOwnedTag(stack);
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(storedItem.getItem()).toString();
        tag.putString("item_id", itemId);
        tag.putInt("item_count", storedItem.getCount());
        tag.putDouble("uu_matter_cost_buckets", uuMatterCostBuckets);
        tag.putLong("energy_cost", energyCost);
        tag.putString("item_name", storedItem.getHoverName().getString());
        tag.putInt(CONTAINER_VERSION,1);
        stack.set(DataComponents.CONTAINER,content);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    /**
     * 清除记忆水晶中的数据
     */
    public void clearData(ItemStack stack) {
        var existing=getDataTag(stack);
        if(existing==null||existing.contains(CONTAINER_VERSION)&&!ownsContainer(stack))return;
        if(ownsContainer(stack))stack.remove(DataComponents.CONTAINER);
        var tag=cleanOwnedTag(stack);
        if(tag.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    private boolean ownsContainer(ItemStack stack) {
        var tag=getDataTag(stack);return tag!=null&&tag.contains(CONTAINER_VERSION,net.minecraft.nbt.Tag.TAG_INT)&&tag.getInt(CONTAINER_VERSION)==1;
    }
    private boolean canReplace(ItemStack stack) {
        if(stack.getItem()!=this||stack.getCount()!=1)return false;
        var tag=getDataTag(stack);
        if(tag!=null&&tag.contains(CONTAINER_VERSION)&&!ownsContainer(stack))return false;
        return ownsContainer(stack)||!stack.has(DataComponents.CONTAINER);
    }
    private CompoundTag cleanOwnedTag(ItemStack stack) {
        var tag=getDataTag(stack);if(tag==null)tag=new CompoundTag();
        for(var key:OWNED_KEYS)tag.remove(key);return tag;
    }
    private static double buckets(CompoundTag tag) {
        if(tag.contains("uu_matter_cost_buckets",net.minecraft.nbt.Tag.TAG_DOUBLE))return tag.getDouble("uu_matter_cost_buckets");
        if(tag.contains("uu_matter_cost",net.minecraft.nbt.Tag.TAG_LONG))return tag.getLong("uu_matter_cost")/1000.0;
        return Double.NaN;
    }
    private static boolean validCosts(CompoundTag tag) {
        return tag.contains("energy_cost",net.minecraft.nbt.Tag.TAG_LONG)&&com.miophas.singularity_iteration.common.processing.StoredPattern.validCosts(buckets(tag),tag.getLong("energy_cost"));
    }

    /**
     * 获取数据标签
     */
    @Nullable
    private CompoundTag getDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        return null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 如果有数据，显示附魔光效
        return hasData(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (hasData(stack)) {
            // 显示存储的物品名称
            String itemName = getStoredItemName(stack);
            if (!itemName.isEmpty()) {
                tooltipComponents.add(Component.translatable("tooltip.mio_icif.memory.stored_item", itemName));
            }

            // 显示UU物质消耗
            double uuCost = getUuMatterCost(stack);
            String uuText = toSiString(uuCost, 4) + "B";
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.memory.uu_cost", uuText));

            // 显示EU能量消耗
            long euCost = getEnergyCost(stack);
            String euText;
            if (euCost < 1000000) {
                euText = euCost + " EU";
            } else {
                euText = String.format("%.2fM EU", euCost / 1000000.0);
            }
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.memory.eu_cost", euText));
        } else {
            // 显示空状态
            tooltipComponents.add(Component.translatable("tooltip.mio_icif.memory.empty"));
        }
    }

    private static String toSiString(double value, int digits) {
        if (value == 0.0D) return "0 ";
        if (Double.isNaN(value)) return "NaN ";
        String ret = "";
        String si;
        if (value < 0.0D) {
            ret = "-";
            value = -value;
        }
        if (Double.isInfinite(value)) return ret + "\u221E ";
        double log = Math.log10(value);
        double mul;
        if (log >= 0.0D) {
            int reduce = (int) Math.floor(log / 3.0D);
            mul = 1.0D / Math.pow(10.0D, reduce * 3);
            si = switch (reduce) {
                case 0 -> "";
                case 1 -> "k";
                case 2 -> "M";
                case 3 -> "G";
                case 4 -> "T";
                case 5 -> "P";
                case 6 -> "E";
                case 7 -> "Z";
                case 8 -> "Y";
                default -> "E" + (reduce * 3);
            };
        } else {
            int expand = (int) Math.ceil(-log / 3.0D);
            mul = Math.pow(10.0D, expand * 3);
            si = switch (expand) {
                case 0 -> "";
                case 1 -> "m";
                case 2 -> "\u00B5";
                case 3 -> "n";
                case 4 -> "p";
                case 5 -> "f";
                case 6 -> "a";
                case 7 -> "z";
                case 8 -> "y";
                default -> "E-" + (expand * 3);
            };
        }
        value *= mul;
        int iVal = (int) Math.floor(value);
        value -= iVal;
        int iDigits = 1;
        if (iVal > 0) iDigits = (int) (iDigits + Math.floor(Math.log10(iVal)));
        double mul2 = Math.pow(10.0D, digits - iDigits);
        int dVal = (int) Math.round(value * mul2);
        if (dVal >= mul2) {
            iVal++;
            dVal = (int) (dVal - mul2);
            iDigits = 1;
            if (iVal > 0) iDigits = (int) (iDigits + Math.floor(Math.log10(iVal)));
        }
        ret = ret + Integer.toString(iVal);
        if (digits > iDigits && dVal != 0) {
            ret = ret + String.format(".%0" + (digits - iDigits) + "d", dVal);
        }
        ret = ret.replaceFirst("(\\.\\d*?)0+$", "$1");
        return ret + " " + si;
    }
}
