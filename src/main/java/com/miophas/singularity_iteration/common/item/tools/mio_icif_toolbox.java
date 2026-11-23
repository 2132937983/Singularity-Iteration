package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.common.util.mio_icif_tags;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.util.IBoxable;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.Nullable;

/**
 * 工具箱物品。
 * 对齐 IC2 1.12.2 的 ItemToolbox / HandHeldToolbox / SlotBoxable：
 * 右键打开手持容器 GUI。数据保存在物品的 CUSTOM_DATA NBT 中（与 {@link AbstractElectricTool} 的手持物品栏格式一致）。
 *
 * <p>0.1.11.170: the two toolboxes were registered as plain items, so neither opened its GUI; they are toolboxes now: the
 * toolbox with 9 slots (IC2), the advanced toolbox with {@link #ADVANCED_SIZE} (made from a toolbox and carbon plates, its
 * contents kept). Only the lathing tool declared itself boxable, so almost nothing went in: what fits now follows IC2 Classic's
 * rule ({@link #canBeStored}).
 */
public class mio_icif_toolbox extends Item {

    /** 对齐 IC2: ItemToolbox 构造 HandHeldToolbox(player, stack, 9)，共 9 格 */
    public static final int INVENTORY_SIZE = 9;
    /** 0.1.11.170: the advanced toolbox, three rows of nine. */
    public static final int ADVANCED_SIZE = 27;

    private final int slots;

    public mio_icif_toolbox(Properties properties) {
        this(properties, INVENTORY_SIZE);
    }

    public mio_icif_toolbox(Properties properties, int slots) {
        super(properties.stacksTo(1));
        this.slots = slots;
    }

    /** The number of slots of this toolbox. */
    public int slots() { return slots; }

    /**
     * 0.1.11.170: what goes into a toolbox. Never a toolbox, a shulker box or a bundle (no container in a container). An item that
     * declares itself ({@link IBoxable}) decides; then the items of {@code mio_icif:toolbox_storable} (this mod's tools: wrenches,
     * cutters, treetaps, meters, scanners, painters...); then, as IC2 Classic does, any tool or weapon (pickaxes, axes, shovels,
     * hoes, swords, shears, bows, crossbows, tridents, fishing rods, flint and steel) and any electric item.
     */
    public static boolean canBeStored(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof mio_icif_toolbox || item instanceof BundleItem
            || item instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock) return false;
        if (item instanceof IBoxable boxable) return boxable.canBeStoredInToolbox(stack);
        if (stack.is(mio_icif_tags.TOOLBOX_STORABLE)) return true;
        return item instanceof TieredItem || item instanceof ShearsItem || item instanceof BowItem || item instanceof CrossbowItem
            || item instanceof TridentItem || item instanceof FishingRodItem || item instanceof FlintAndSteelItem
            || item instanceof IBatteryItem;
    }

    /** The items stored in a toolbox (empty slots left out). */
    public static List<ItemStack> contents(ItemStack toolbox, net.minecraft.core.HolderLookup.Provider registries) {
        if (!(toolbox.getItem() instanceof mio_icif_toolbox box)) return List.of();
        ItemStack[] stored = new ItemStack[box.slots];
        AbstractElectricTool.loadHandHeldInventory(toolbox, stored, registries);
        java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>();
        for (ItemStack s : stored) if (s != null && !s.isEmpty()) out.add(s);
        return out;
    }

    /** 对齐 IC2: ItemToolbox.use - 打开手持容器 GUI */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new Provider(stack, hand, slots), buf -> { buf.writeVarInt(slots); buf.writeEnum(hand); });
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mio_icif.toolbox", slots).withStyle(ChatFormatting.GRAY));
        if (context.registries() != null) {
            int used = contents(stack, context.registries()).size();
            if (used > 0) tooltip.add(Component.translatable("tooltip.mio_icif.toolbox.used", used, slots).withStyle(ChatFormatting.YELLOW));
        }
    }

    public static class Provider implements MenuProvider {
        private final ItemStack containerStack;
        private final net.minecraft.world.InteractionHand hand;
        private final int slots;

        public Provider(ItemStack containerStack) {
            this(containerStack, net.minecraft.world.InteractionHand.MAIN_HAND);
        }

        public Provider(ItemStack containerStack, net.minecraft.world.InteractionHand hand) {
            this(containerStack, hand, containerStack.getItem() instanceof mio_icif_toolbox box ? box.slots : INVENTORY_SIZE);
        }

        public Provider(ItemStack containerStack, net.minecraft.world.InteractionHand hand, int slots) {
            this.containerStack = containerStack;
            this.hand = hand;
            this.slots = slots;
        }

        @Override
        public Component getDisplayName() {
            return containerStack.getHoverName();
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new com.miophas.singularity_iteration.common.menu.tool.mio_icif_toolbox_menu(
                containerId, playerInventory, containerStack, slots, hand);
        }
    }
}
