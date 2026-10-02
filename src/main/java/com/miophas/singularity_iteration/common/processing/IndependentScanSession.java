// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;

import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Owned scan progress and result; the machine remains responsible for its input inventory. */
public final class IndependentScanSession {
    public record ScanReceipt(String itemKey, double uuBuckets, long energyCost) { }
    public enum State { IDLE, SCANNING, WAITING_ENERGY, COMPLETED, FAILED, CANCELLED }
    public enum CompletionKind { FINITE, KNOWN_DENIED }
    public interface EnergyPort {
        long available();
        /** Must report the amount actually consumed. Invalid replies are held, never replayed. */
        long consume(long amount);
    }
    private static final Set<String> FIELDS = Set.of("scex_scan_version", "item_key", "item", "uu_buckets",
        "energy_cost", "progress", "last_tick", "state", "total_ticks", "energy_per_tick", "paid_tick", "pending_payment", "item_backed", "completion_kind");
    private final BooleanSupplier serverThread;
    private final Runnable changed;
    private final int totalTicks;
    private final long energyPerTick;
    /**
     * The fixed cost of an explicit denial scan.  Keep this value checked at
     * construction time so a malformed/overlarge profile can never make a
     * later begin or persistence path throw an arithmetic exception.
     */
    private final long totalScanEnergy;
    private String sourceKey = "";
    private ItemStack sourceStack = ItemStack.EMPTY;
    private double buckets;
    private CompletionKind completionKind = CompletionKind.FINITE;
    private long energyCost;
    private int progress;
    private long lastTick = Long.MIN_VALUE;
    private long paidTick;
    private long pendingPayment;
    private State state = State.IDLE;
    private boolean busy;
    private CompoundTag heldRaw;

    public IndependentScanSession(BooleanSupplier serverThread, int totalTicks, long energyPerTick) {
        this(serverThread, totalTicks, energyPerTick, () -> {});
    }
    public IndependentScanSession(BooleanSupplier serverThread, int totalTicks, long energyPerTick, Runnable changed) {
        this.serverThread = Objects.requireNonNull(serverThread, "server thread");
        this.changed = Objects.requireNonNull(changed, "change notification");
        if (totalTicks <= 0 || energyPerTick <= 0) throw new IllegalArgumentException("scan profile");
        if (energyPerTick > Long.MAX_VALUE / totalTicks) {
            throw new IllegalArgumentException("scan profile energy overflow");
        }
        this.totalTicks = totalTicks; this.energyPerTick = energyPerTick;
        this.totalScanEnergy = (long) totalTicks * energyPerTick;
    }
    private boolean mayBegin(double uuBuckets, long totalEnergy) {
        return serverThread.getAsBoolean() && !busy && heldRaw == null && pendingPayment == 0
            && (state == State.IDLE || state == State.CANCELLED)
            && StoredPattern.validCosts(uuBuckets, totalEnergy);
    }
    /** Symbol-only caller contract; machine adapters must use the ItemStack overload. */
    public boolean begin(String itemKey, double uuBuckets, long totalEnergy) {
        if (itemKey == null || itemKey.isBlank() || !mayBegin(uuBuckets, totalEnergy)) return false;
        start(itemKey, ItemStack.EMPTY, uuBuckets, totalEnergy, CompletionKind.FINITE); return true;
    }
    public boolean begin(ItemStack item, double uuBuckets, long totalEnergy) {
        if (item == null || item.isEmpty() || item.getCount() != 1 || !mayBegin(uuBuckets, totalEnergy)) return false;
        start(IndependentUuValueIndex.keyOf(item).itemId(), item.copy(), uuBuckets, totalEnergy, CompletionKind.FINITE); return true;
    }
    /** The owning adapter must establish both KNOWN_DENIED and explicit scan eligibility first. */
    public boolean beginDenied(ItemStack item) {
        if (item == null || item.isEmpty() || item.getCount() != 1 || !serverThread.getAsBoolean() || busy
                || heldRaw != null || pendingPayment != 0 || (state != State.IDLE && state != State.CANCELLED)) return false;
        start(IndependentUuValueIndex.keyOf(item).itemId(), item.copy(), 0,
                totalScanEnergy, CompletionKind.KNOWN_DENIED);
        return true;
    }
    public CompletionKind completionKind() { return completionKind; }
    private void start(String key, ItemStack item, double cost, long eu, CompletionKind kind) {
        sourceKey = key; sourceStack = item; buckets = cost; energyCost = eu; completionKind = kind;
        progress = 0; paidTick = 0; lastTick = Long.MIN_VALUE; state = State.SCANNING; changed.run();
    }
    /** Each observed tick admits at most one debit, including partial replies and clock rewinds. */
    public long tick(long gameTick, EnergyPort energy) {
        if (!serverThread.getAsBoolean() || busy || (state != State.SCANNING && state != State.WAITING_ENERGY)
                || gameTick == lastTick || pendingPayment != 0 || heldRaw != null) return 0;
        Objects.requireNonNull(energy, "energy");
        busy = true;
        try {
            lastTick = gameTick;
            long due = energyPerTick - paidTick;
            long available;
            try { available = energy.available(); }
            catch (RuntimeException failure) { state = State.FAILED; changed.run(); return 0; }
            if (available < due) { state = State.WAITING_ENERGY; changed.run(); return 0; }
            // The owner can save this intent before an external debit callback is entered.
            pendingPayment = due; changed.run();
            long paid;
            try { paid = energy.consume(due); }
            catch (RuntimeException failure) { state = State.FAILED; changed.run(); return 0; }
            if (paid < 0 || paid > due) { state = State.FAILED; changed.run(); return 0; }
            pendingPayment = 0;
            paidTick += paid;
            if (paidTick == energyPerTick) {
                paidTick = 0; progress++;
                state = progress == totalTicks ? State.COMPLETED : State.SCANNING;
            } else state = State.WAITING_ENERGY;
            changed.run(); return paid;
        } finally { busy = false; }
    }
    public void cancel() {
        if (serverThread.getAsBoolean() && !busy && (state == State.SCANNING || state == State.WAITING_ENERGY)) {
            state = State.CANCELLED; changed.run();
        }
    }
    public State state() { return state; }
    public int progress() { return progress; }
    public int totalTicks() { return totalTicks; }
    public long paidTick() { return paidTick; }
    public long pendingPayment() { return pendingPayment; }
    public String sourceKey() { return sourceKey; }
    public ItemStack sourceStack() { return sourceStack.copy(); }
    public ScanReceipt takeResult() {
        if (!serverThread.getAsBoolean() || busy || completionKind != CompletionKind.FINITE || state != State.COMPLETED || !sourceStack.isEmpty()) return null;
        var value = new ScanReceipt(sourceKey, buckets, energyCost); clear(); changed.run(); return value;
    }
    public StoredPattern takeStoredPattern() {
        if (!serverThread.getAsBoolean() || busy || completionKind != CompletionKind.FINITE || state != State.COMPLETED || sourceStack.isEmpty()) return null;
        var value = new StoredPattern(sourceStack, buckets, energyCost); clear(); changed.run(); return value;
    }
    /** Symbol-only compatibility entry. Components require an explicit registry provider. */
    public CompoundTag save() {
        if (heldRaw != null) return heldRaw.copy();
        if (!sourceStack.isEmpty()) throw new IllegalStateException("Item-backed scan requires registry-aware save");
        return snapshot();
    }
    public CompoundTag save(HolderLookup.Provider registries) {
        if (heldRaw != null) return heldRaw.copy();
        var tag = snapshot();
        if (!sourceStack.isEmpty()) tag.put("item", sourceStack.save(registries));
        return tag;
    }
    private CompoundTag snapshot() {
        var tag = new CompoundTag();
        tag.putInt("scex_scan_version", 3); tag.putString("item_key", sourceKey);
        tag.putString("completion_kind", completionKind.name());
        if (completionKind == CompletionKind.FINITE) tag.putDouble("uu_buckets", buckets);
        tag.putLong("energy_cost", energyCost);
        tag.putInt("progress", progress); tag.putLong("last_tick", lastTick); tag.putInt("state", state.ordinal());
        tag.putInt("total_ticks", totalTicks); tag.putLong("energy_per_tick", energyPerTick);
        tag.putLong("paid_tick", paidTick); tag.putLong("pending_payment", pendingPayment);
        tag.putBoolean("item_backed", !sourceStack.isEmpty());
        return tag;
    }
    public void load(CompoundTag tag) { load(tag, null); }
    /** Unknown, legacy or incompatible data is retained byte-for-value and cannot resume or be overwritten. */
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (busy || !serverThread.getAsBoolean()) return;
        clear();
        if (tag == null) { state = State.FAILED; return; }
        heldRaw = tag.copy(); state = State.FAILED;
        try {
            int version = tag.getInt("scex_scan_version");
            if (version != 2 && version != 3 || version == 2 && tag.contains("completion_kind")) return;
            CompletionKind kind = version == 2 ? CompletionKind.FINITE
                    : tag.contains("completion_kind", Tag.TAG_STRING) ? CompletionKind.valueOf(tag.getString("completion_kind")) : null;
            if (kind == null || (kind == CompletionKind.FINITE ? !tag.contains("uu_buckets", Tag.TAG_DOUBLE) : tag.contains("uu_buckets"))) return;
            if (!FIELDS.containsAll(tag.getAllKeys()) || !tag.contains("scex_scan_version", Tag.TAG_INT)
                    || !tag.contains("item_key", Tag.TAG_STRING) || !tag.contains("energy_cost", Tag.TAG_LONG)
                    || !tag.contains("progress", Tag.TAG_INT) || !tag.contains("last_tick", Tag.TAG_LONG)
                    || !tag.contains("state", Tag.TAG_INT) || !tag.contains("total_ticks", Tag.TAG_INT)
                    || !tag.contains("energy_per_tick", Tag.TAG_LONG) || !tag.contains("paid_tick", Tag.TAG_LONG)
                    || !tag.contains("pending_payment", Tag.TAG_LONG)
                    || !tag.contains("item_backed", Tag.TAG_BYTE) || tag.getByte("item_backed") < 0 || tag.getByte("item_backed") > 1
                    || tag.getBoolean("item_backed") != tag.contains("item")
                    || tag.getInt("total_ticks") != totalTicks || tag.getLong("energy_per_tick") != energyPerTick) return;
            int ordinal = tag.getInt("state");
            if (ordinal < 0 || ordinal >= State.values().length) return;
            State loaded = State.values()[ordinal];
            String key = tag.getString("item_key");
            double cost = tag.getDouble("uu_buckets"); long eu = tag.getLong("energy_cost");
            int ticks = tag.getInt("progress"); long partial = tag.getLong("paid_tick"), pending = tag.getLong("pending_payment");
            if (ticks < 0 || ticks > totalTicks || partial < 0 || partial >= energyPerTick
                    || pending < 0 || pending > energyPerTick - partial) return;
            ItemStack item = ItemStack.EMPTY;
            if (tag.contains("item")) {
                if (registries == null || !tag.contains("item", Tag.TAG_COMPOUND)) return;
                item = ItemStack.parse(registries, tag.getCompound("item")).orElse(ItemStack.EMPTY);
                if (item.isEmpty() || item.getCount() != 1 || !IndependentUuValueIndex.keyOf(item).itemId().equals(key)) return;
            }
            if (loaded == State.IDLE) {
                if (!key.isEmpty() || !item.isEmpty() || cost != 0 || eu != 0 || ticks != 0 || partial != 0 || pending != 0) return;
                if (kind != CompletionKind.FINITE) return;
            } else if (key.isBlank() || (kind == CompletionKind.FINITE ? !StoredPattern.validCosts(cost, eu)
                    : item.isEmpty() || eu != totalScanEnergy)) return;
            if (loaded == State.COMPLETED && (ticks != totalTicks || partial != 0 || pending != 0)) return;
            if (loaded != State.COMPLETED && ticks == totalTicks) return;
            if (pending != 0 && loaded != State.SCANNING && loaded != State.WAITING_ENERGY && loaded != State.FAILED) return;
            sourceKey = key; sourceStack = item.copy(); buckets = cost; energyCost = eu; completionKind = kind;
            progress = ticks; paidTick = partial; pendingPayment = pending; lastTick = tag.getLong("last_tick");
            state = pending == 0 ? loaded : State.FAILED;
            heldRaw = null;
        } catch (RuntimeException invalid) { /* Keep the original payload for later recovery. */ }
    }
    private void clear() {
        completionKind = CompletionKind.FINITE;
        sourceKey = ""; sourceStack = ItemStack.EMPTY; buckets = 0; energyCost = 0; progress = 0;
        lastTick = Long.MIN_VALUE; paidTick = 0; pendingPayment = 0; state = State.IDLE; heldRaw = null;
    }
}
