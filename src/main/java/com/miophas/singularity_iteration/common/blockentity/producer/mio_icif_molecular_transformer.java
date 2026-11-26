package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class mio_icif_molecular_transformer extends AbstractProcessingMachineBlockEntity {
    // Per-call-site recipe memo: canWork() runs every tick, getRecipeFor is a linear scan.
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput, com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe> recipeCache1 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput, com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe> recipeCache2 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput, com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe> recipeCache3 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE);
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipeInput, com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe> recipeCache4 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE);

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .input(1)
        .output(1)
        .build();

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int TOTAL_SLOTS = 2;

    // 参考ASP源代码：分子转换仪接受IV等级（最大8192 EU/t）
    // 能量容量需要足够大以支持高EU消耗（最大配方需要约20,000,000 EU）
    public static final long DEFAULT_CAPACITY = 120_000_000L;  // 最大能量容量（EU）
    /** No input ceiling (0.1.7.32): any voltage, any amount up to the free buffer per tick. */
    public static final long DEFAULT_MAX_RECEIVE = Long.MAX_VALUE;
    /** Recipes finished per tick at most when the buffer holds enough for several (parallel run). */
    public static final int MAX_OPERATIONS_PER_TICK = 64;
    public static final long DEFAULT_MAX_EXTRACT = 0L;         // 不输出能量
    public static final int DEFAULT_MAX_PROGRESS = 100;        // 最大进度（实际使用EU消耗）
    public static final long DEFAULT_ENERGY_PER_TICK = 32768L; // 每tick消耗32768 EU

    // 累计消耗的 EU，用于追踪配方进度值
    private long consumedEU = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                // 返回 0-100 的进度百分比，用于 UI 显示
                case 0 -> currentRecipeEU != null && currentRecipeEU > 0
                    ? (int) Math.min((consumedEU * 100) / currentRecipeEU, 100)
                    : 0;
                case 1 -> 100; // maxProgress 固定为100（百分比）
                case 2 -> isWorking ? 1 : 0;
                case 3 -> (int) Math.min(energyStorage.getAmount(), Integer.MAX_VALUE);
                case 4 -> (int) Math.min(energyStorage.getCapacity(), Integer.MAX_VALUE);
                case 5 -> currentRecipeEU != null ? (int) Math.min(currentRecipeEU, Integer.MAX_VALUE) : 0;
                case 6 -> lastEnergyPerTick != null ? (int) Math.min(lastEnergyPerTick, Integer.MAX_VALUE) : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端不能直接设置这些值
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    private Long currentRecipeEU = null;
    private Long lastEnergyPerTick = null;

    public mio_icif_molecular_transformer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.MOLECULAR_TRANSFORMER_ENTITY_TYPE.get());
    }

    public mio_icif_molecular_transformer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_MAX_PROGRESS,
            LAYOUT,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.UNLIMITED);
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        return new int[]{INPUT_SLOT, OUTPUT_SLOT};
    }

    @Override
    protected boolean canInsertItem(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == INPUT_SLOT) {
            return itemHandler.isItemValid(slot, stack);
        }
        return false;
    }

    @Override
    protected boolean canExtractItem(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            if (level == null || stack.isEmpty()) return false;
            var recipeManager = level.getRecipeManager();
            var input = new mio_icif_MolecularTransformerRecipeInput(stack);
            return recipeCache1.find(level, input).isPresent();
        }
        return false;
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    /**
     * 判断当前输入是否有有效的分子转换配方
     * 用于判断当前物品是否更该设置进度
     */
    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || level == null) return false;
        var recipeManager = level.getRecipeManager();
        var recipeInput = new mio_icif_MolecularTransformerRecipeInput(input);
        return recipeCache2.find(level, recipeInput).isPresent();
    }

    @Override
    protected boolean canWork() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;

        if (level == null) return false;
        var recipeManager = level.getRecipeManager();
        var recipeInput = new mio_icif_MolecularTransformerRecipeInput(input);
        var recipeOpt = recipeCache3.find(level, recipeInput);
        if (recipeOpt.isEmpty()) return false;

        var recipe = recipeOpt.get().value();

        long recipeEUCost = recipe.getEuCost();
        if (recipeEUCost <= 0) { resetWork(); return false; }
        if (currentRecipeEU == null || currentRecipeEU != recipeEUCost) {
            currentRecipeEU = recipeEUCost;
            consumedEU = 0;
        }

        if (consumedEU >= currentRecipeEU) {
            return true;
        }

        if (!canAddItem(OUTPUT_SLOT, recipe.getResult())) return false;

        // any stored energy advances the recipe: a weak line no longer has to fill a 32768 EU tick first
        return energyStorage.getAmount() > 0;
    }

    /** Never overvolted: every packet tier is accepted. */
    @Override
    public int getSinkTier() {
        return Integer.MAX_VALUE;
    }

    /**
     * Spends everything the buffer holds (no per-tick ceiling), so the speed follows the power
     * supplied; with enough energy several items finish in the same tick (up to
     * {@link #MAX_OPERATIONS_PER_TICK}).
     */
    @Override
    protected void doWork() {
        if (currentRecipeEU == null) return;
        long spent = 0;
        for (int op = 0; op < MAX_OPERATIONS_PER_TICK; op++) {
            if (consumedEU < currentRecipeEU) {
                long take = Math.min(currentRecipeEU - consumedEU, energyStorage.getAmount());
                long extracted = take > 0 ? apiUseEnergy(take, false) : 0;
                if (extracted <= 0) break;
                consumedEU += extracted;
                spent += extracted;
            }
            if (consumedEU < currentRecipeEU) break;
            operate();
            if (currentRecipeEU != null) break;                 // output blocked: operate kept the bill
            if (!canWork() || currentRecipeEU == null) break;   // sets up the next item / recipe
        }
        if (spent <= 0 && currentRecipeEU != null && consumedEU < currentRecipeEU) {
            stopWork();
            return;
        }
        lastEnergyPerTick = spent;
        isWorking = true;
        syncProgressToBase();
    }

    private void syncProgressToBase() {
        if (currentRecipeEU != null && currentRecipeEU > 0) {
            this.progress = (int) com.miophas.singularity_iteration.core.api.util.BoundedUnits.multiplyDivide(Math.max(0, Math.min(consumedEU, currentRecipeEU)), Math.max(0, maxProgress), currentRecipeEU);
        }
    }

    @Override
    protected void updateProgress() {
    }

    @Override
    protected void checkInputChanged() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return;

        if (lastInputStacks == null || lastInputStacks.length != inputSlots.length) {
            lastInputStacks = new ItemStack[inputSlots.length];
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            return;
        }

        boolean changed = false;
        for (int i = 0; i < inputSlots.length; i++) {
            ItemStack current = itemHandler.getStackInSlot(inputSlots[i]);
            ItemStack last = lastInputStacks[i];
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                changed = true;
                break;
            }
        }

        if (changed) {
            progress = 0;
            isWorking = false;
            closeWorkCycle();   // 0.1.11.3: as the default input check does since 0.1.11.2
            consumedEU = 0;
            currentRecipeEU = null;
            lastEnergyPerTick = null;
            for (int i = 0; i < inputSlots.length; i++) {
                lastInputStacks[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
            }
            setChanged();
        }
    }

    @Override
    protected boolean shouldResetProgress() {
        int[] inputSlots = getInputSlots();
        if (inputSlots.length == 0) return false;
        ItemStack input = itemHandler.getStackInSlot(inputSlots[0]);
        if (input.isEmpty()) {
            consumedEU = 0;
            currentRecipeEU = null;
            lastEnergyPerTick = null;
            return true;
        }
        boolean valid = hasValidRecipe();
        if (!valid) {
            consumedEU = 0;
            currentRecipeEU = null;
            lastEnergyPerTick = null;
        }
        return !valid;
    }

    private void operate() {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            resetWork();
            return;
        }

        if (level == null) {
            resetWork();
            return;
        }

        var recipeManager = level.getRecipeManager();
        var recipeInput = new mio_icif_MolecularTransformerRecipeInput(input);
        var recipeOpt = recipeCache4.find(level, recipeInput);

        if (recipeOpt.isPresent()) {
            var recipe = recipeOpt.get().value();
            // 再次验证是否可以添加到输出槽
            if (canAddItem(OUTPUT_SLOT, recipe.getResult())) {
                var plan = com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.prepare(itemHandler, INPUT_SLOT, 1,
                    new int[]{OUTPUT_SLOT}, java.util.List.of(recipe.getResult()));
                if (plan.isEmpty()) { stopWork(); return; }
                long paid = consumedEU;
                var cost = currentRecipeEU;
                var lastRate = lastEnergyPerTick;
                int savedProgress = progress;
                // Publish completed billing before slot callbacks can observe the next input.
                consumedEU = 0; currentRecipeEU = null; lastEnergyPerTick = null; progress = 0; isWorking = false;
                if (plan.get().commit()) { countFinishedOperation(); viewFinished++; resetWork(); }   // 0.1.11.3: closes the work cycle
                else { consumedEU = paid; currentRecipeEU = cost; lastEnergyPerTick = lastRate; progress = savedProgress; stopWork(); }
            } else {
                // 无法添加到输出槽，停止工作状态，不重置进度
                // 等待输出槽位有空位时再继续
                isWorking = false;
                stopWork();
            }
            return;
        }

        // The recipe is no longer valid. Reset the work.
        resetWork();
    }

    private void resetWork() {
        progress = 0;
        maxProgress = DEFAULT_MAX_PROGRESS;
        consumedEU = 0;
        currentRecipeEU = null;
        lastEnergyPerTick = null;
        isWorking = false;
        setChanged();
    }

    private void addItemToSlot(int slot, ItemStack stack) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(slot, stack);
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(stack.getCount());
        }
    }

    private boolean canAddItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        var current = itemHandler.getStackInSlot(slot);
        int limit = Math.min(itemHandler.getSlotLimit(slot), stack.getMaxStackSize());
        return stack.getCount() <= limit && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)
            && current.getCount() <= limit - stack.getCount());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (currentRecipeEU != null) {
            tag.putLong("currentRecipeEU", currentRecipeEU);
        }
        tag.putLong("consumedEU", consumedEU);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        long cost = tag.getLong("currentRecipeEU");
        currentRecipeEU = cost > 0 ? cost : null;
        consumedEU = currentRecipeEU == null ? 0 : com.miophas.singularity_iteration.core.api.util.BoundedUnits.clamp(tag.getLong("consumedEU"), currentRecipeEU);
        syncProgressToBase();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.molecular_transformer");
    }

    public ContainerData getContainerData() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.miophas.singularity_iteration.common.menu.producer.MolecularTransformerMenu(
            containerId, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_molecular_transformer blockEntity) {
        if (level.isClientSide()) return;
        AbstractProcessingMachineBlockEntity.tick(level, pos, state, blockEntity);
        blockEntity.updateView();
    }

    // ------------------------------------------------------------------ 0.1.11.213 (MT1 X1): what the chamber renderer sees
    /*
     * The client draws the chamber from a small view: the input (one item), the recipe's result and process kind, whether the
     * machine works, and the EU paid / needed / spent per tick at a game time. The client runs the progress on from that, so
     * the server sends an update only on events, never every tick: the input or result changes, the machine starts or stops,
     * an item finishes while one item takes STREAM_TICKS or more, or the smoothed draw moves by more than a quarter. Updates
     * go through ClientSyncThrottle (at most one packet per 5 ticks per machine).
     */
    /** Below this many ticks per item the chamber shows a continuous stream instead of item-by-item (2 s, as the screen). */
    public static final int STREAM_TICKS = 40;
    private final com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<mio_icif_MolecularTransformerRecipeInput, com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe> recipeCache5 = new com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache<>(mio_icif_MolecularTransformerRecipes.MOLECULAR_TRANSFORMER_TYPE);
    private int viewFinished;
    private double viewRateAvg;
    // last sent (server) / last received (client)
    private ItemStack viewInput = ItemStack.EMPTY, viewResult = ItemStack.EMPTY;
    private String viewKind = "unknown";
    private boolean viewWorking;
    private long viewPaid, viewCost, viewRate, viewTime;
    private int viewSentFinished;

    private void updateView() {
        if (level == null || level.isClientSide) return;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack result = ItemStack.EMPTY;
        String kind = "unknown";
        if (!input.isEmpty()) {
            var recipe = recipeCache5.find(level, new mio_icif_MolecularTransformerRecipeInput(input));
            if (recipe.isPresent()) {
                result = recipe.get().value().getResult();
                kind = recipe.get().value().getProcess().kind();
            }
        }
        long spent = isWorking && lastEnergyPerTick != null ? lastEnergyPerTick : 0;
        viewRateAvg = isWorking ? (viewRateAvg <= 0 ? spent : viewRateAvg * 0.85 + spent * 0.15) : 0;
        long rate = Math.round(viewRateAvg);
        long cost = currentRecipeEU != null ? currentRecipeEU : 0;
        boolean single = rate > 0 && cost >= rate * (long) STREAM_TICKS;
        boolean changed = !ItemStack.isSameItem(input, viewInput) || !ItemStack.isSameItem(result, viewResult)
            || !kind.equals(viewKind) || isWorking != viewWorking
            || single && viewFinished != viewSentFinished
            || Math.abs(rate - viewRate) * 4 > Math.max(1, viewRate);
        if (!changed) return;
        viewInput = input.copyWithCount(1);
        viewResult = result.copyWithCount(1);
        viewKind = kind;
        viewWorking = isWorking;
        viewPaid = consumedEU;
        viewCost = cost;
        viewRate = rate;
        viewTime = level.getGameTime();
        viewSentFinished = viewFinished;
        com.miophas.singularity_iteration.core.runtime.sync.ClientSyncThrottle.request(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag view = new CompoundTag();
        if (!viewInput.isEmpty()) view.put("in", viewInput.save(registries));
        if (!viewResult.isEmpty()) view.put("out", viewResult.save(registries));
        view.putString("kind", viewKind);
        view.putBoolean("work", viewWorking);
        view.putLong("paid", viewPaid);
        view.putLong("cost", viewCost);
        view.putLong("rate", viewRate);
        view.putLong("time", viewTime);
        tag.put("mt_view", view);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (!tag.contains("mt_view")) return;
        CompoundTag view = tag.getCompound("mt_view");
        viewInput = view.contains("in") ? ItemStack.parseOptional(registries, view.getCompound("in")) : ItemStack.EMPTY;
        viewResult = view.contains("out") ? ItemStack.parseOptional(registries, view.getCompound("out")) : ItemStack.EMPTY;
        viewKind = view.getString("kind");
        viewWorking = view.getBoolean("work");
        viewPaid = view.getLong("paid");
        viewCost = view.getLong("cost");
        viewRate = view.getLong("rate");
        viewTime = view.getLong("time");
    }

    /** Block entity data packets carry the update tag (the default would load it as a save and clear the machine). */
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    /** The chamber view on the client (or the last sent view on the server). */
    public ItemStack viewInput() { return viewInput; }
    public ItemStack viewResult() { return viewResult; }
    /** The recipe's process kind: molecular, lattice, nuclear, exotic or unknown (MolecularProcess). */
    public String viewKind() { return viewKind; }
    public boolean viewWorking() { return viewWorking && !viewInput.isEmpty(); }

    /** Ticks one item takes at the synced draw, or 0 when nothing is drawn. */
    public double viewTicksPerItem() {
        return viewRate > 0 && viewCost > 0 ? (double) viewCost / viewRate : 0;
    }

    /** Whether the chamber shows a continuous stream: one item takes less than {@link #STREAM_TICKS}. */
    public boolean viewStream() {
        double t = viewTicksPerItem();
        return viewWorking() && t > 0 && t < STREAM_TICKS;
    }

    /**
     * Progress of the item in the chamber, 0..1, at {@code gameTime} (partial ticks allowed): the paid EU at the last update
     * plus the synced draw since. Past one item it starts the next (the same recipe runs on until an update says otherwise).
     */
    public double viewProgress(double gameTime) {
        if (!viewWorking() || viewCost <= 0) return 0;
        double p = (viewPaid + viewRate * Math.max(0, gameTime - viewTime)) / (double) viewCost;
        return p - Math.floor(p);
    }
}