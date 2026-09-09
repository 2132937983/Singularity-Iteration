package com.singularity_iteration.mio_icif.Screen;

import com.singularity_iteration.mio_icif.Singularity_Iteration_Config;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.future.CommodityCategory;
import com.singularity_iteration.mio_icif.future.FutureCommodity;
import com.singularity_iteration.mio_icif.future.FutureCommodityManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.singularity_iteration.mio_icif.Menu.Producer.FutureElcMenu;
import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_future_elc;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 期货的GUI - 表现层
 * 
 * 职责：
 * 1. 渲染界面和所有视觉元素
 * 2. 处理用户输入（按钮点击等）
 * 3. 从 ContainerData 读取数据并显示
 * 4. 将用户操作转发给 BlockEntity 处理
 * 
 * 注意：
 * - GUI 只负责显示，不处理业务逻辑
 * - 所有数据修改通过发送按钮事件给服务端
 * - 保持原有样式不变
 */
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_gui_future_elc extends mio_icif_screen<com.singularity_iteration.mio_icif.Menu.Producer.FutureElcMenu> {

    private static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.parse("mio_icif:textures/gui/gui_future.png");

    // GUI 纹理尺寸
    private static final int TEXTURE_WIDTH = 500;
    private static final int TEXTURE_HEIGHT = 300;

    // 按钮尺寸
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TAB_BUTTON_WIDTH = 50;
    private static final int TAB_BUTTON_HEIGHT = 18;

    // 按钮ID
    private static final int BUTTON_COMMODITY_BASE = 0;
    private static final int BUTTON_DECREASE = 100;
    private static final int BUTTON_INCREASE = 101;
    private static final int BUTTON_BUY = 102;
    private static final int BUTTON_SELL = 103;
    private static final int BUTTON_PREV_PAGE = 104;
    private static final int BUTTON_NEXT_PAGE = 105;
    private static final int BUTTON_CATEGORY_BASE = 200;

    // 组件引用
    private List<CommodityButton> commodityButtons = new ArrayList<>();
    private List<Button> categoryTabButtons = new ArrayList<>();
    private Button buyButton;
    private Button sellButton;
    private Button increaseButton;
    private Button decreaseButton;
    private Button prevPageButton;
    private Button nextPageButton;
    
    // 缂撳瓨鍊肩敤浜庢��娴嬪彉鍖�
    private int lastCategory = -1;
    private int lastPage = -1;

    public mio_icif_gui_future_elc(FutureElcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = this.width;
        this.imageHeight = this.height;
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        recreateAllButtons();
    }

    /**
     * 重新创建所有按钮
     */
    private void recreateAllButtons() {
        // 清除旧按钮
        commodityButtons.forEach(this::removeWidget);
        categoryTabButtons.forEach(this::removeWidget);
        if (buyButton != null) removeWidget(buyButton);
        if (sellButton != null) removeWidget(sellButton);
        if (increaseButton != null) removeWidget(increaseButton);
        if (decreaseButton != null) removeWidget(decreaseButton);
        if (prevPageButton != null) removeWidget(prevPageButton);
        if (nextPageButton != null) removeWidget(nextPageButton);
        
        commodityButtons.clear();
        categoryTabButtons.clear();

        createCategoryTabs();
        createCommodityButtons();
        createQuantityButtons();
        createTradeButtons();
    }

    /**
     * 创建种类标签页按钮
     */
    private void createCategoryTabs() {
        List<CommodityCategory> categories = FutureCommodityManager.getCategoriesWithCommodities();
        if (categories.isEmpty()) return;

        int tabY = (int)(this.height * 0.08f);
        int totalWidth = categories.size() * TAB_BUTTON_WIDTH + (categories.size() - 1) * 5;
        int startX = (this.width - totalWidth) / 2;

        for (int i = 0; i < categories.size(); i++) {
            final int buttonId = BUTTON_CATEGORY_BASE + i;
            CommodityCategory category = categories.get(i);
            int tabX = startX + i * (TAB_BUTTON_WIDTH + 5);

            Button tabButton = Button.builder(category.getDisplayName(), b -> sendButtonClick(buttonId))
                .pos(tabX, tabY)
                .size(TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT)
                .build();
            this.categoryTabButtons.add(tabButton);
            this.addRenderableWidget(tabButton);
        }
    }

    /**
     * 创建货品按钮和翻页按钮
     */
    private void createCommodityButtons() {
        int commodityListX = 20;
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonSpacing = 4;

        // 翻页按钮
        int pageButtonWidth = buttonWidth / 2 - 2;
        int pageButtonHeight = buttonHeight;
        int pageNavY = (int)(this.height * 0.20f) - buttonHeight - 4;

        prevPageButton = Button.builder(Component.literal("<"), b -> sendButtonClick(BUTTON_PREV_PAGE))
            .pos(commodityListX, pageNavY)
            .size(pageButtonWidth, pageButtonHeight)
            .build();
        this.addRenderableWidget(prevPageButton);

        nextPageButton = Button.builder(Component.literal(">"), b -> sendButtonClick(BUTTON_NEXT_PAGE))
            .pos(commodityListX + pageButtonWidth + 4, pageNavY)
            .size(pageButtonWidth, pageButtonHeight)
            .build();
        this.addRenderableWidget(nextPageButton);

        // 货品按钮
        int commodityListY = (int)(this.height * 0.20f);
        List<FutureCommodity> commodities = getMenu().getCurrentPageCommodities();

        for (int i = 0; i < commodities.size(); i++) {
            final int buttonId = BUTTON_COMMODITY_BASE + i;
            int btnX = commodityListX;
            int btnY = commodityListY + i * (buttonHeight + buttonSpacing);

            CommodityButton btn = new CommodityButton(
                btnX, btnY, buttonWidth, buttonHeight,
                commodities.get(i),
                b -> sendButtonClick(buttonId)
            );
            this.commodityButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    /**
     * 创建数量调节按钮
     */
    private void createQuantityButtons() {
        int quantityAdjustX = this.width - BUTTON_WIDTH - 20;
        int quantityAdjustY = this.height - BUTTON_HEIGHT * 2 - 30 - BUTTON_HEIGHT - 5;
        int squareSize = BUTTON_HEIGHT;
        int spacing = BUTTON_WIDTH - squareSize * 2;

        decreaseButton = Button.builder(Component.literal("-"), b -> sendButtonClick(BUTTON_DECREASE))
            .pos(quantityAdjustX, quantityAdjustY)
            .size(squareSize, squareSize)
            .build();
        this.addRenderableWidget(decreaseButton);

        increaseButton = Button.builder(Component.literal("+"), b -> sendButtonClick(BUTTON_INCREASE))
            .pos(quantityAdjustX + squareSize + spacing, quantityAdjustY)
            .size(squareSize, squareSize)
            .build();
        this.addRenderableWidget(increaseButton);
    }

    /**
     * 创建交易按钮
     */
    private void createTradeButtons() {
        int confirmButtonX = this.width - BUTTON_WIDTH - 20;
        int confirmButtonY = this.height - BUTTON_HEIGHT * 2 - 30;

        buyButton = Button.builder(Component.translatable("gui.mio_icif.future.buy"), b -> sendButtonClick(BUTTON_BUY))
            .pos(confirmButtonX, confirmButtonY)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
        buyButton.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.future.buy.tooltip")));
        this.addRenderableWidget(buyButton);

        sellButton = Button.builder(Component.translatable("gui.mio_icif.future.sell"), b -> sendButtonClick(BUTTON_SELL))
            .pos(confirmButtonX, confirmButtonY + BUTTON_HEIGHT + 5)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
        sellButton.setTooltip(Tooltip.create(Component.translatable("gui.mio_icif.future.sell.tooltip")));
        this.addRenderableWidget(sellButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, 0, 0, this.width, this.height, 
            0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不渲染默认标签
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderBg(guiGraphics, partialTick, mouseX, mouseY);
        
        for (var widget : this.renderables) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        FutureElcMenu menu = this.getMenu();
        if (menu == null) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        // 绘制标题
        guiGraphics.drawString(this.font, this.title, 
            (int)(this.width * 0.05f), (int)(this.height * 0.05f), 0x404040, false);

        // 绘制第几天显示
        Component dayText = Component.translatable("gui.mio_icif.future.day", menu.getDay() + 1);
        int dayDisplayX = this.width - BUTTON_WIDTH - 20;
        int dayDisplayY = 20;
        guiGraphics.drawString(this.font, dayText, dayDisplayX, dayDisplayY, 0xFFFFFF, false);

        // 绘制货币显示
        int coinDisplayY = dayDisplayY + 15;
        int coinDisplayX = dayDisplayX;

        ItemStack coinStack = new ItemStack(mio_icif_normal.COIN.get());
        guiGraphics.renderItem(coinStack, coinDisplayX, coinDisplayY);

        Component coinText = Component.literal(String.valueOf(menu.getPlayerCoins()));
        guiGraphics.drawString(this.font, coinText, coinDisplayX + 20, coinDisplayY + 4, 0xFFD700, false);

        // 绘制中央黑色矩形背景
        int commodityButtonWidth = 60;
        int commodityButtonHeight = 20;
        int commodityButtonSpacing = 4;
        int firstCommodityY = (int)(this.height * 0.20f);
        int fixedCommodityCount = 7;
        int lastCommodityY = firstCommodityY + (fixedCommodityCount - 1) * (commodityButtonHeight + commodityButtonSpacing);
        int panelTopY = firstCommodityY;
        int panelBottomY = lastCommodityY + commodityButtonHeight;
        int panelLeftX = 20 + commodityButtonWidth + 15;
        int panelRightX = this.width - BUTTON_WIDTH - 20 - 15;

        guiGraphics.fill(panelLeftX, panelTopY, panelRightX, panelBottomY, 0x80000000);

        // 绘制价格折线图
        drawPriceChart(guiGraphics, panelLeftX, panelTopY, panelRightX, panelBottomY);

        // 绘制交易数量
        int quantityAdjustX = this.width - BUTTON_WIDTH - 20;
        int quantityAdjustY = this.height - BUTTON_HEIGHT * 2 - 30 - BUTTON_HEIGHT - 5;
        int squareSize = BUTTON_HEIGHT;
        int quantitySpacing = BUTTON_WIDTH - squareSize * 2;
        Component quantityText = Component.literal(String.valueOf(menu.getTradeQuantity()));
        guiGraphics.drawCenteredString(this.font, quantityText,
            quantityAdjustX + squareSize + quantitySpacing / 2, quantityAdjustY + 5, 0xFFFFFF);

        // 绘制底部信息
        int bottomY = this.height - 25;
        int centerScreenX = this.width / 2;

        Component tradeText = Component.translatable("gui.mio_icif.future.daily_volume",
            menu.getDailyVolume(), Singularity_Iteration_Config.FUTURE_DAILY_LIMIT.get());
        int tradeTextWidth = this.font.width(tradeText);

        boolean hasEnergy = menu.hasEnoughEnergy();
        Component energyText;
        int energyColor;

        if (hasEnergy) {
            energyText = Component.translatable("gui.mio_icif.future.energy.ok",
                menu.getEnergy(), menu.getMaxEnergy());
            energyColor = 0x00FF00;
        } else {
            energyText = Component.translatable("gui.mio_icif.future.energy.no");
            energyColor = 0xFF0000;
        }
        int energyTextWidth = this.font.width(energyText);

        int spacing = 20;
        int totalWidth = tradeTextWidth + spacing + energyTextWidth;
        int startX = centerScreenX - totalWidth / 2;

        int tradeInfoX = startX;
        int energyDisplayX = startX + tradeTextWidth + spacing;

        guiGraphics.drawString(this.font, tradeText, tradeInfoX, bottomY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, energyText, energyDisplayX, bottomY, energyColor, false);

        updateButtonStates();
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        FutureElcMenu menu = this.getMenu();
        if (menu == null) return;

        int currentCategory = menu.getCurrentCategory();
        for (int i = 0; i < categoryTabButtons.size(); i++) {
            Button btn = categoryTabButtons.get(i);
            if (i == currentCategory) {
                btn.setMessage(btn.getMessage().copy().withStyle(net.minecraft.ChatFormatting.BOLD));
            }
        }

        int selectedIndex = menu.getSelectedCommodityIndex();
        for (int i = 0; i < commodityButtons.size(); i++) {
            commodityButtons.get(i).setSelected(i == selectedIndex);
        }

        if (prevPageButton != null) {
            prevPageButton.active = menu.getCurrentPage() > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = menu.getCurrentPage() < menu.getTotalPages() - 1;
        }
    }

    private void sendButtonClick(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null && this.menu != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        
        FutureElcMenu menu = this.getMenu();
        if (menu != null && menu.data != null) {
            int currentCategory = menu.data.get(mio_icif_future_elc.DATA_CURRENT_CATEGORY);
            int currentPage = menu.data.get(mio_icif_future_elc.DATA_CURRENT_PAGE);

            if (currentCategory != lastCategory || currentPage != lastPage) {
                lastCategory = currentCategory;
                lastPage = currentPage;
                recreateCommodityButtons();
            }
        }
    }
    
    /**
     * 重新创建货品按钮
     */
    private void recreateCommodityButtons() {
        commodityButtons.forEach(this::removeWidget);
        commodityButtons.clear();
        
        if (prevPageButton != null) removeWidget(prevPageButton);
        if (nextPageButton != null) removeWidget(nextPageButton);
        
        int commodityListX = 20;
        int buttonWidth = 60;
        int buttonHeight = 20;
        int pageButtonWidth = buttonWidth / 2 - 2;
        int pageNavY = (int)(this.height * 0.20f) - buttonHeight - 4;

        prevPageButton = Button.builder(Component.literal("<"), b -> sendButtonClick(BUTTON_PREV_PAGE))
            .pos(commodityListX, pageNavY)
            .size(pageButtonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(prevPageButton);

        nextPageButton = Button.builder(Component.literal(">"), b -> sendButtonClick(BUTTON_NEXT_PAGE))
            .pos(commodityListX + pageButtonWidth + 4, pageNavY)
            .size(pageButtonWidth, buttonHeight)
            .build();
        this.addRenderableWidget(nextPageButton);
        
        int commodityListY = (int)(this.height * 0.20f);
        List<FutureCommodity> commodities = getMenu().getCurrentPageCommodities();

        for (int i = 0; i < commodities.size(); i++) {
            final int buttonId = BUTTON_COMMODITY_BASE + i;
            int btnX = commodityListX;
            int btnY = commodityListY + i * (buttonHeight + 4);

            CommodityButton btn = new CommodityButton(
                btnX, btnY, buttonWidth, buttonHeight,
                commodities.get(i),
                b -> sendButtonClick(buttonId)
            );
            this.commodityButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    /**
     * 绘制价格折线图
     */
    private void drawPriceChart(GuiGraphics guiGraphics, int leftX, int topY, int rightX, int bottomY) {
        FutureElcMenu menu = this.getMenu();
        if (menu == null) return;
        
        FutureCommodity selectedCommodity = menu.getSelectedCommodity();
        if (selectedCommodity == null) return;
        
        List<Integer> priceHistory = menu.getPriceHistory();
        if (priceHistory.isEmpty()) return;
        
        int chartLeft = leftX + 30;
        int chartRight = rightX - 10;
        int chartTop = topY + 10;
        int chartBottom = bottomY - 20;
        
        int chartWidth = chartRight - chartLeft;
        int chartHeight = chartBottom - chartTop;
        
        int basePrice = selectedCommodity.getBasePrice();
        
        int maxPrice = 0;
        int minPrice = Integer.MAX_VALUE;
        for (int price : priceHistory) {
            if (price > maxPrice) maxPrice = price;
            if (price < minPrice) minPrice = price;
        }
        
        if (minPrice == maxPrice) {
            maxPrice = basePrice + 100;
            minPrice = Math.max(0, basePrice - 100);
        } else {
            @SuppressWarnings("unused")
            int range = maxPrice - minPrice;
            int centerPrice = (maxPrice + minPrice) / 2;
            int distanceFromBase = Math.abs(centerPrice - basePrice);
            if (distanceFromBase > 0) {
                maxPrice += distanceFromBase;
                minPrice = Math.max(0, minPrice - distanceFromBase);
            }
        }
        
        int priceRange = maxPrice - minPrice;
        if (priceRange == 0) priceRange = 1;
        
        int basePriceY = chartBottom - (int)((basePrice - minPrice) * (float)chartHeight / priceRange);
        
        int axisColor = 0xFFAAAAAA;
        guiGraphics.fill(chartLeft - 1, chartTop, chartLeft, chartBottom, axisColor);
        guiGraphics.fill(chartLeft, chartBottom, chartRight, chartBottom + 1, axisColor);
        
        guiGraphics.drawString(this.font, String.valueOf(maxPrice), chartLeft - 25, chartTop - 4, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, String.valueOf(basePrice), chartLeft - 25, basePriceY - 4, 0xFFFF00, false);
        guiGraphics.drawString(this.font, String.valueOf(minPrice), chartLeft - 25, chartBottom - 4, 0xFFFFFF, false);
        
        Component chartTitle = Component.literal(selectedCommodity.getDisplayName() + " - 20 天价格趋势");
        guiGraphics.drawString(this.font, chartTitle, chartLeft + chartWidth / 2 - this.font.width(chartTitle) / 2, chartTop - 10, 0xFFD700, false);
        
        int risingLineColor = 0xFFFF0000;
        int fallingLineColor = 0xFF00FF00;
        int pointColor = 0xFFD700;
        int textColor = 0xFFD700;
        
        int daysCount = priceHistory.size();
        int[] xPoints = new int[daysCount];
        int[] yPoints = new int[daysCount];
        int pointSpacing = chartWidth / (daysCount - 1);
        
        for (int i = 0; i < daysCount; i++) {
            int price = priceHistory.get(i);
            xPoints[i] = chartLeft + i * pointSpacing;
            yPoints[i] = chartBottom - (int)((price - minPrice) * (float)chartHeight / priceRange);
        }
        
        for (int i = 0; i < daysCount - 1; i++) {
            int lineColor;
            if (yPoints[i] > yPoints[i + 1]) {
                lineColor = risingLineColor;
            } else if (yPoints[i] < yPoints[i + 1]) {
                lineColor = fallingLineColor;
            } else {
                lineColor = 0xFF808080;
            }
            drawLine(guiGraphics, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1], lineColor);
        }
        
        for (int i = 0; i < daysCount; i++) {
            int pointSize = 4;
            guiGraphics.fill(
                xPoints[i] - pointSize / 2,
                yPoints[i] - pointSize / 2,
                xPoints[i] + pointSize / 2,
                yPoints[i] + pointSize / 2,
                pointColor
            );
            
            int price = priceHistory.get(i);
            Component priceText = Component.literal(String.valueOf(price));
            float scale = 0.5f;
            int textX = xPoints[i] - (int)(this.font.width(priceText) * scale / 2);
            int textY = yPoints[i] - 8;
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textX, textY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(this.font, priceText, 0, 0, textColor, false);
            guiGraphics.pose().popPose();
        }
        
        int[] keyDays = {0, 4, 8, 12, 16, 19};
        String[] keyLabels = {"19天前", "15天前", "11天前", "7天前", "3天前", "今天"};
        for (int j = 0; j < keyDays.length; j++) {
            int i = keyDays[j];
            if (i < daysCount) {
                int labelX = xPoints[i] - this.font.width(keyLabels[j]) / 2;
                guiGraphics.drawString(this.font, keyLabels[j], labelX, chartBottom + 5, 0xFFFFFF, false);
            }
        }
    }
    
    /**
     * 使用 Bresenham 算法绘制直线
     */
    private void drawLine(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            guiGraphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            
            if (x0 == x1 && y0 == y1) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    // ==================== 货品按钮类 ====================

    private class CommodityButton extends Button {
        private final FutureCommodity commodity;
        private boolean selected = false;
        private final OnPress pressCallback;

        public CommodityButton(int x, int y, int width, int height, 
                              FutureCommodity commodity, OnPress onPress) {
            super(x, y, width, height, Component.empty(), (btn) -> {}, DEFAULT_NARRATION);
            this.commodity = commodity;
            this.pressCallback = onPress;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = selected ? 0xFF808080 : (this.isHovered() ? 0xFF606060 : 0xFF404040);
            guiGraphics.fill(this.getX(), this.getY(),
                this.getX() + this.width, this.getY() + this.height, color);

            if (selected) {
                guiGraphics.renderOutline(this.getX(), this.getY(),
                    this.width, this.height, 0xFFFFFF);
            }

            ItemStack stack = new ItemStack(commodity.getItem());
            int iconX = this.getX() + 4;
            int iconY = this.getY() + (this.height - 16) / 2;
            guiGraphics.renderItem(stack, iconX, iconY);
            
            // 获取今天的价格并显示在图标右侧
            FutureElcMenu menu = mio_icif_gui_future_elc.this.getMenu();
            if (menu != null) {
                int currentPrice = menu.getCommodityPrice(commodity);
                int basePrice = commodity.getBasePrice();
                
                // 根据价格涨跌确定颜色：高于基础价格=红色(涨)，低于=绿色(跌)，等于灰色
                int priceColor;
                if (currentPrice > basePrice) {
                    priceColor = 0xFFFF0000; // 红色 - 上涨
                } else if (currentPrice < basePrice) {
                    priceColor = 0xFF00FF00; // 绿色 - 下跌
                } else {
                    priceColor = 0xFF808080; // 灰色 - 持平
                }
                
                String priceText = String.valueOf(currentPrice);
                int textX = iconX + 20;
                int textY = this.getY() + (this.height - 8) / 2;
                guiGraphics.drawString(mio_icif_gui_future_elc.this.font, priceText, textX, textY, priceColor, false);
            }
        }

        @Override
        public void onPress() {
            if (pressCallback != null) {
                pressCallback.onPress(this);
            }
        }
    }


}