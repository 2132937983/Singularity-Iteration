package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_future_elc;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import com.miophas.singularity_iteration.common.future.CommodityCategory;
import com.miophas.singularity_iteration.common.future.FutureCommodity;
import com.miophas.singularity_iteration.common.future.FutureCommodityManager;
import com.miophas.singularity_iteration.common.future.FutureMarketData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"null"}) public class FutureElcMenu extends mio_icif_machine_menu {
    public static final int BUTTON_COMMODITY_BASE = 0;
    public static final int BUTTON_DECREASE = 100;
    public static final int BUTTON_INCREASE = 101;
    public static final int BUTTON_BUY = 102;
    public static final int BUTTON_SELL = 103;
    public static final int BUTTON_PREV_PAGE = 104;
    public static final int BUTTON_NEXT_PAGE = 105;
    public static final int BUTTON_CATEGORY_BASE = 200;

    public FutureElcMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, null);
    }

    public FutureElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_future_elc blockEntity) {
        this(containerId, playerInventory, blockEntity, null, null);
    }

    public FutureElcMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_future_elc blockEntity, @Nullable IItemHandler itemHandler, @Nullable ContainerData data) {
        super(mio_icif_menus.FUTURE_ELC_MENU_TYPE.get(), containerId, 1, playerInventory, itemHandler, data, mio_icif_future_elc.DATA_COUNT, blockEntity);
    }

    @Override
    public mio_icif_future_elc getBlockEntity() {
        return this.blockEntity instanceof mio_icif_future_elc be ? be : null;
    }

    @Override
    public boolean stillValid(Player player) {
        var future = getBlockEntity();
        if (future == null) return player.level().isClientSide;
        if (!(player instanceof ServerPlayer) || !(future.getLevel() instanceof ServerLevel world)
                || !world.getServer().isSameThread() || player.level() != world || !player.isAlive()
                || player.isSpectator() || future.isRemoved()) return false;
        var pos = future.getBlockPos();
        var chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockEntities().get(pos) == future
            && chunk.getBlockState(pos).getBlock() == future.getBlockState().getBlock()
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    protected boolean shouldAddPlayerInventory() { return false; }

    @Override
    protected void addMachineSlots() {
        this.addSlot(new Slot(new SimpleContainer(1), 0, -1000, -1000) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
            @Override
            public boolean mayPickup(Player player) { return false; }
        });
    }

    public int getEnergy() { return data.get(mio_icif_future_elc.DATA_ENERGY); }
    public int getMaxEnergy() { return data.get(mio_icif_future_elc.DATA_MAX_ENERGY); }
    public int getDailyVolume() { return data.get(mio_icif_future_elc.DATA_DAILY_VOLUME); }
    public int getDay() { return data.get(mio_icif_future_elc.DATA_CURRENT_DAY); }
    public int getSelectedCommodityIndex() { return data.get(mio_icif_future_elc.DATA_SELECTED_COMMODITY); }
    public int getTradeQuantity() { return data.get(mio_icif_future_elc.DATA_TRADE_QUANTITY); }
    public int getCurrentCategory() { return data.get(mio_icif_future_elc.DATA_CURRENT_CATEGORY); }
    public int getCurrentPage() { return data.get(mio_icif_future_elc.DATA_CURRENT_PAGE); }

    public int getPlayerCoins() {
        int low = data.get(mio_icif_future_elc.DATA_PLAYER_COINS_LOW);
        int high = data.get(mio_icif_future_elc.DATA_PLAYER_COINS_HIGH);
        long coins = ((long) high << 16) | (low & 0xFFFF);
        return coins > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) coins;
    }

    public boolean hasEnoughEnergy() {
        return getEnergy() >= Singularity_Iteration_Config.FUTURE_ENERGY_PER_TRADE.get();
    }

    public void updatePlayerCoins(Player player) {
        var future = getBlockEntity();
        if (future != null && player != null && player.containerMenu == this && stillValid(player)) {
            future.updatePlayerCoinsCache(player);
        }
    }

    public int getTotalPages() {
        List<FutureCommodity> commodities = getCurrentCategoryCommodities();
        int itemsPerPage = Singularity_Iteration_Config.FUTURE_ITEMS_PER_PAGE.get();
        return (commodities.size() + itemsPerPage - 1) / itemsPerPage;
    }

    private CommodityCategory getCurrentCategoryEnum() {
        int categoryIndex = getCurrentCategory();
        List<CommodityCategory> categories = FutureCommodityManager.getCategoriesWithCommodities();
        if (categoryIndex >= 0 && categoryIndex < categories.size()) {
            return categories.get(categoryIndex);
        }
        return categories.isEmpty() ? CommodityCategory.OTHER : categories.get(0);
    }

    public List<FutureCommodity> getCurrentCategoryCommodities() {
        return FutureCommodityManager.getCommoditiesByCategory(getCurrentCategoryEnum());
    }

    public List<FutureCommodity> getCurrentPageCommodities() {
        List<FutureCommodity> all = getCurrentCategoryCommodities();
        int currentPage = getCurrentPage();
        int itemsPerPage = Singularity_Iteration_Config.FUTURE_ITEMS_PER_PAGE.get();
        long start = (long) currentPage * itemsPerPage;
        if (start < 0 || start >= all.size()) return List.of();
        int end = (int) Math.min(start + itemsPerPage, all.size());
        return all.subList((int) start, end);
    }

    public FutureCommodity getSelectedCommodity() {
        List<FutureCommodity> commodities = getCurrentPageCommodities();
        int selectedIndex = getSelectedCommodityIndex();
        if (selectedIndex >= 0 && selectedIndex < commodities.size()) {
            return commodities.get(selectedIndex);
        }
        return null;
    }

    public List<Integer> getPriceHistory() {
        List<Integer> history = new ArrayList<>();
        for (int i = 0; i < FutureMarketData.getHistoryDays(); i++) {
            int highIndex = mio_icif_future_elc.DATA_HISTORY_START + i * 2;
            int lowIndex = mio_icif_future_elc.DATA_HISTORY_START + i * 2 + 1;
            int high = data.get(highIndex);
            int low = data.get(lowIndex);
            int price = (high << 16) | (low & 0xFFFF);
            if (price == 0) {
                FutureCommodity commodity = getSelectedCommodity();
                if (commodity != null) {
                    price = commodity.getBasePrice();
                }
            }
            history.add(price);
        }
        return history;
    }

    public int getCommodityPrice(FutureCommodity commodity) {
        List<FutureCommodity> all = FutureCommodityManager.getCommodities();
        int index = all.indexOf(commodity);
        if (index >= 0 && index < 70) {
            int priceIndex = mio_icif_future_elc.DATA_PRICES_START + index;
            return data.get(priceIndex);
        }
        return commodity.getBasePrice();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        var future = getBlockEntity();
        if (future == null || player.containerMenu != this || !stillValid(player)) return false;

        if (id >= BUTTON_CATEGORY_BASE) {
            int categoryIndex = id - BUTTON_CATEGORY_BASE;
            List<CommodityCategory> categories = FutureCommodityManager.getCategoriesWithCommodities();
            if (categoryIndex >= 0 && categoryIndex < categories.size()) {
                future.setCategory(categories.get(categoryIndex));
            }
            return true;
        }

        if (id >= BUTTON_COMMODITY_BASE && id < BUTTON_DECREASE) {
            int index = id - BUTTON_COMMODITY_BASE;
            if (index >= future.getCurrentPageCommodities().size()) return false;
            future.setSelectedCommodity(index);
            return true;
        }

        switch (id) {
            case BUTTON_PREV_PAGE:
                future.previousPage();
                return true;
            case BUTTON_NEXT_PAGE:
                future.nextPage();
                return true;
            case BUTTON_DECREASE:
                future.decreaseTradeQuantity();
                return true;
            case BUTTON_INCREASE:
                future.increaseTradeQuantity();
                return true;
            case BUTTON_BUY:
                boolean buyResult = future.executeBuy(player);
                return buyResult;
            case BUTTON_SELL:
                boolean sellResult = future.executeSell(player);
                return sellResult;
            default:
                return false;
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        var future = getBlockEntity();
        if (future != null && future.getLevel() instanceof ServerLevel world && world.getServer().isSameThread()) {
            for (Player player : world.players()) {
                if (player.containerMenu == this && stillValid(player)) {
                    future.updatePlayerCoinsCache(player);
                    break;
                }
            }
        }
    }
}

