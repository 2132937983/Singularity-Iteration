package com.miophas.singularity_iteration.common.client;

import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.prefab.item.ArmorFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public class TooltipInteractionHandler {

    private static int frozenX = -1;
    private static int frozenY = -1;
    private static boolean wasAltDown = false;
    private static boolean isManualRendering = false;

    private static int lastRenderedX = 0;
    private static int lastRenderedY = 0;
    private static int lastTooltipWidth = 0;
    private static int lastTooltipHeight = 0;

    private static ItemStack frozenItem = ItemStack.EMPTY;
    private static List<Component> frozenLines = new ArrayList<>();

    private static boolean isAltDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    private static void refreshFrozenLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || frozenItem.isEmpty()) return;

        Item.TooltipContext context = Item.TooltipContext.of(mc.level);
        TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        frozenLines = new ArrayList<>(frozenItem.getTooltipLines(context, mc.player, flag));
    }

    private static void applyLocalToggle(String slotName, String featureKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 槽位标识定位；若该槽位物品未声明此特性（例如背槽物品声明 CHEST），回退到全槽位查找
        ItemStack clientStack = ArmorFeatureSlots.stackById(mc.player, slotName);
        if (ArmorFeatures.find(clientStack, featureKey) == null) {
            clientStack = ArmorFeatureSlots.findDeclaring(mc.player, featureKey);
        }
        if (clientStack.isEmpty()) return;

        // 乐观更新（开关型取反 / 模式型循环），服务端由 /mio_icif toggle 命令执行同一步骤
        ArmorFeatures.toggleOrCycle(clientStack, null, featureKey);
        frozenItem = clientStack.copy();
    }

    private static void renderFrozenTooltip(GuiGraphics guiGraphics) {
        if (frozenItem.isEmpty()) return;
        if (frozenLines.isEmpty()) return;
        if (frozenX < 0 || frozenY < 0) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        isManualRendering = true;
        guiGraphics.renderComponentTooltip(font, frozenLines, frozenX, frozenY, frozenItem);
        isManualRendering = false;
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null) return;

        boolean altDown = isAltDown();

        if (altDown && !frozenItem.isEmpty()) {
            return;
        }

        frozenLines = new ArrayList<>(event.getToolTip());
        frozenItem = event.getItemStack().copy();
    }

    @SubscribeEvent
    public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        if (isManualRendering) return;

        boolean altDown = isAltDown();

        if (!altDown) {
            frozenX = -1;
            frozenY = -1;
            wasAltDown = false;
            frozenItem = ItemStack.EMPTY;
            frozenLines = new ArrayList<>();
            return;
        }

        if (!wasAltDown) {
            frozenX = event.getX();
            frozenY = event.getY();
            wasAltDown = true;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderTooltipColor(RenderTooltipEvent.Color event) {
        lastRenderedX = event.getX();
        lastRenderedY = event.getY();

        Font font = event.getFont();
        int totalHeight = 0;
        int maxWidth = 0;
        for (var ctc : event.getComponents()) {
            totalHeight += ctc.getHeight();
            int width = ctc.getWidth(font);
            if (width > maxWidth) maxWidth = width;
        }

        lastTooltipWidth = maxWidth;
        lastTooltipHeight = totalHeight;
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (isAltDown()) {
            renderFrozenTooltip(event.getGuiGraphics());
        } else {
            frozenX = -1;
            frozenY = -1;
            wasAltDown = false;
            frozenItem = ItemStack.EMPTY;
            frozenLines = new ArrayList<>();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (isAltDown()) {
            renderFrozenTooltip(event.getGuiGraphics());
        } else {
            frozenX = -1;
            frozenY = -1;
            wasAltDown = false;
            frozenItem = ItemStack.EMPTY;
            frozenLines = new ArrayList<>();
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isAltDown()) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (frozenItem.isEmpty()) return;
        if (frozenLines.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        int tooltipLeft = lastRenderedX;
        int tooltipTop = lastRenderedY;
        int tooltipRight = tooltipLeft + lastTooltipWidth + 4;
        int tooltipBottom = tooltipTop + lastTooltipHeight + 4;

        if (mouseX < tooltipLeft || mouseX > tooltipRight || mouseY < tooltipTop || mouseY > tooltipBottom) {
            return;
        }

        int lineY = tooltipTop;
        for (int i = 0; i < frozenLines.size(); i++) {
            Component line = frozenLines.get(i);
            int lineHeight = (i == 0) ? 12 : 10;

            if (mouseY >= lineY && mouseY < lineY + lineHeight) {
                ClickEvent clickEvent = findClickEvent(line);
                if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    executeCommand(mc, clickEvent.getValue());
                    event.setCanceled(true);
                    return;
                }
            }
            lineY += lineHeight;
        }
    }

    private static ClickEvent findClickEvent(Component component) {
        ClickEvent direct = component.getStyle().getClickEvent();
        if (direct != null) return direct;

        for (Component sibling : component.getSiblings()) {
            ClickEvent siblingClick = findClickEvent(sibling);
            if (siblingClick != null) return siblingClick;
        }

        return null;
    }

    private static void executeCommand(Minecraft mc, String command) {
        if (mc.player == null) return;

        String cmd = command.startsWith("/") ? command.substring(1) : command;
        mc.player.connection.sendCommand(cmd);

        String[] parts = cmd.split(" ");
        if (parts.length >= 4 && parts[0].equals("mio_icif") && parts[1].equals("toggle")) {
            String slotName = parts[2];
            String featureKey = parts[3];
            applyLocalToggle(slotName, featureKey);
        }

        refreshFrozenLines();
    }
}