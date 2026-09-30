// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Debit before delivery; retain uncertain external calls without automatically replaying them. */
public final class FluidTransferBuffer {
    private FluidStack pending = FluidStack.EMPTY;
    private FluidStack uncertain = FluidStack.EMPTY;
    private String phase = "";
    private CompoundTag unrecognizedSave;
    private final Runnable changed;
    private boolean transferring;

    public FluidTransferBuffer(Runnable changed) { this.changed = changed; }
    public FluidStack pending() { return pending.copy(); }
    public FluidStack uncertain() { return uncertain.copy(); }
    public String uncertainPhase() { return phase; }
    public boolean isBlocked() { return unrecognizedSave != null || !phase.isEmpty(); }
    public boolean hasPending() { return !pending.isEmpty(); }

    /** No fresh extraction, including when the pending amount is smaller than the budget. */
    public int deliverPending(IFluidHandler target, int budget, java.util.function.BooleanSupplier current) {
        if (!hasPending()) return 0;
        return move(net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler.INSTANCE, target, budget, current);
    }

    /**
     * Server-thread operation.
     *
     * <p>外部流体处理器是不可信的：真实模组里存在 {@code fill}/ {@code drain} 返回越界数值
     * （负数或超过请求量）的实现。这类违约只按"保守估计"处理并夹到合法区间，
     * 绝不抛异常——否则一个坏掉的相邻容器就能崩掉整个服务器。
     */
    public int move(IFluidHandler source, IFluidHandler target, int budget) {
        return move(source, target, budget, () -> true);
    }

    /** Recheck owner identity after foreign callbacks; confirmed custody is retained on removal. */
    public int move(IFluidHandler source, IFluidHandler target, int budget, java.util.function.BooleanSupplier current) {
        if (transferring || isBlocked() || source == target || budget <= 0 || !current.getAsBoolean()) return 0;
        transferring = true;
        try {
            if (pending.isEmpty()) {
                var available = source.drain(budget, IFluidHandler.FluidAction.SIMULATE);
                if (!current.getAsBoolean() || available.isEmpty()) return 0;
                available = available.copyWithAmount(Math.min(budget, available.getAmount()));
                int quoted = target.fill(available.copy(), IFluidHandler.FluidAction.SIMULATE);
                if (!current.getAsBoolean()) return 0;
                // 填充分额只是预估，不构成任何承诺：夹到 [0, 可用量] 即可。
                // 负数（含违约）视为不可接收而放弃本次传输；超过可用量则按可用量封顶。
                if (quoted <= 0) return 0;
                if (quoted > available.getAmount()) quoted = available.getAmount();
                uncertain = available.copyWithAmount(quoted);
                phase = "drain";
                changed.run();
                // Persisting the intent may synchronously remove the owner.
                // Retain the durable intent without calling a stale source.
                if (!current.getAsBoolean()) return 0;
                var actual = source.drain(uncertain.copy(), IFluidHandler.FluidAction.EXECUTE);
                // 外部来源违约时无法证明其究竟取走了多少：放弃本次意图（宁可损耗也不复制），
                // 且绝不抛出异常崩服务器。下次 tick 会重新从来源读取。
                if (!actual.isEmpty() && (actual.getAmount() > uncertain.getAmount()
                        || !FluidStack.isSameFluidSameComponents(actual, uncertain))) {
                    pending = FluidStack.EMPTY;
                    clearIntent();
                    changed.run();
                    return 0;
                }
                pending = actual.copy();
                clearIntent();
                changed.run();
                if (pending.isEmpty() || !current.getAsBoolean()) return 0;
            }
            var offered = pending.copyWithAmount(Math.min(budget, pending.getAmount()));
            // Reserve the offered portion before calling the external handler. A callback can save
            // this state, and a thrown response must never cause this reservation to be sent again.
            pending.shrink(offered.getAmount());
            if (pending.isEmpty()) pending = FluidStack.EMPTY;
            uncertain = offered.copy();
            phase = "fill";
            changed.run();
            // The persistence callback may synchronously unload/replace the
            // owner.  Do not call a foreign handler with a stale reservation;
            // leave the offered amount in the durable uncertain phase instead.
            if (!current.getAsBoolean()) return 0;
            int accepted = target.fill(offered, IFluidHandler.FluidAction.EXECUTE);
            // 目标物理上最多只能接收 offered，越界返回一律保守地视为"全部接收"：
            // 既不重发（避免复制流体），也不抛异常崩服务器。
            if (accepted < 0 || accepted > uncertain.getAmount()) accepted = uncertain.getAmount();
            int rejected = uncertain.getAmount() - accepted;
            if (rejected > 0) {
                if (pending.isEmpty()) pending = uncertain.copyWithAmount(rejected);
                else pending.grow(rejected);
            }
            clearIntent();
            changed.run();
            return accepted;
        } finally {
            transferring = false;
        }
    }

    private void clearIntent() { uncertain = FluidStack.EMPTY; phase = ""; }

    public Tag save(HolderLookup.Provider registries) {
        if (unrecognizedSave != null) return unrecognizedSave.copy();
        var tag = new CompoundTag();
        tag.putInt("TransferVersion", 2);
        tag.put("Pending", pending.saveOptional(registries));
        tag.put("Uncertain", uncertain.saveOptional(registries));
        tag.putString("Phase", phase);
        return tag;
    }
    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        if (transferring) throw new IllegalStateException("Cannot replace an active fluid transfer");
        pending = FluidStack.EMPTY; clearIntent(); unrecognizedSave = null;
        if (!tag.contains("TransferVersion")) {
            pending = FluidStack.parseOptional(registries, tag);
            if (!tag.isEmpty() && pending.isEmpty()) unrecognizedSave = tag.copy();
            return;
        }
        if (!tag.contains("TransferVersion", Tag.TAG_INT) || tag.getInt("TransferVersion") != 2
                || !tag.contains("Pending", Tag.TAG_COMPOUND) || !tag.contains("Uncertain", Tag.TAG_COMPOUND)
                || !tag.contains("Phase", Tag.TAG_STRING)) {
            unrecognizedSave = tag.copy(); return;
        }
        var savedPending = tag.getCompound("Pending");
        var savedUncertain = tag.getCompound("Uncertain");
        pending = FluidStack.parseOptional(registries, savedPending);
        uncertain = FluidStack.parseOptional(registries, savedUncertain);
        phase = tag.getString("Phase");
        if ((!savedPending.isEmpty() && pending.isEmpty()) || (!savedUncertain.isEmpty() && uncertain.isEmpty())
                || !(phase.isEmpty() || phase.equals("drain") || phase.equals("fill"))
                || phase.isEmpty() != uncertain.isEmpty()
                || phase.equals("drain") && !pending.isEmpty()
                || phase.equals("fill") && !pending.isEmpty() && !FluidStack.isSameFluidSameComponents(pending, uncertain))
            unrecognizedSave = tag.copy();
    }
}
