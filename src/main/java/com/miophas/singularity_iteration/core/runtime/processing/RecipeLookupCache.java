package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Per-machine memo for {@link RecipeManager#getRecipeFor}.
 *
 * <p>{@code getRecipeFor} is a linear scan over every recipe of a type. Machines used to call it
 * from {@code canWork()} (every tick) and again from {@code doWork()}, and an idle machine holding an
 * unprocessable item rescanned forever. This cache makes the steady state O(slots):
 * <ul>
 *   <li>The key is an exact snapshot of the {@link RecipeInput} items (item, components and count),
 *       so count-sensitive recipes stay correct.</li>
 *   <li>A positive hit is revalidated with {@link RecipeManager#byKey} (a hash lookup). After a
 *       datapack reload the holder instance changes, so a stale recipe is never returned.</li>
 *   <li>A negative result is kept for {@link #NEGATIVE_TTL_TICKS} ticks. That bounds how long a newly
 *       reloaded recipe can go unnoticed without scanning every tick.</li>
 *   <li>On a miss the previously matched recipe is tried first, before the full scan.</li>
 * </ul>
 * Not thread-safe; one instance per block entity per lookup purpose, used on that entity's thread.
 */
public final class RecipeLookupCache<I extends RecipeInput, R extends Recipe<I>> {
    public static final int NEGATIVE_TTL_TICKS = 20;
    private static final ItemStack[] NO_KEY = new ItemStack[0];

    private final Supplier<? extends RecipeType<R>> type;
    private ItemStack[] key = NO_KEY;
    private boolean hasEntry;
    private @Nullable RecipeHolder<R> cached;
    private @Nullable RecipeHolder<R> lastMatch;
    private long negativeUntil;

    public RecipeLookupCache(Supplier<? extends RecipeType<R>> type) {
        this.type = type;
    }

    /** For vanilla constants such as {@link RecipeType#SMELTING}. */
    public static <I extends RecipeInput, R extends Recipe<I>> RecipeLookupCache<I, R> of(RecipeType<R> type) {
        return new RecipeLookupCache<>(() -> type);
    }

    public Optional<RecipeHolder<R>> find(@Nullable Level level, I input) {
        if (level == null) return Optional.empty();
        RecipeManager manager = level.getRecipeManager();
        if (hasEntry && sameKey(input)) {
            if (cached == null) {
                if (level.getGameTime() < negativeUntil) return Optional.empty();
            } else if (isCurrent(manager, cached)) {
                return Optional.of(cached);
            }
        }
        return store(level, manager, input);
    }

    /** Convenience accessor returning the recipe value or {@code null}. */
    public @Nullable R findValue(@Nullable Level level, I input) {
        return find(level, input).map(RecipeHolder::value).orElse(null);
    }

    public boolean matches(@Nullable Level level, I input) {
        return find(level, input).isPresent();
    }

    /** Forget the memo, e.g. after an external change the key cannot observe. */
    public void invalidate() {
        hasEntry = false;
        cached = null;
        key = NO_KEY;
    }

    private Optional<RecipeHolder<R>> store(Level level, RecipeManager manager, I input) {
        RecipeHolder<R> found = null;
        if (lastMatch != null && isCurrent(manager, lastMatch) && lastMatch.value().matches(input, level)) {
            found = lastMatch;
        } else {
            found = manager.getRecipeFor(type.get(), input, level).orElse(null);
        }
        key = snapshot(input);
        hasEntry = true;
        cached = found;
        if (found != null) {
            lastMatch = found;
        } else {
            negativeUntil = level.getGameTime() + NEGATIVE_TTL_TICKS;
        }
        return Optional.ofNullable(found);
    }

    private static boolean isCurrent(RecipeManager manager, RecipeHolder<?> holder) {
        return manager.byKey(holder.id()).orElse(null) == holder;
    }

    private boolean sameKey(I input) {
        int size = input.size();
        if (size != key.length) return false;
        for (int i = 0; i < size; i++) {
            ItemStack now = input.getItem(i);
            ItemStack then = key[i];
            if (now.getCount() != then.getCount() || !ItemStack.isSameItemSameComponents(now, then)) return false;
        }
        return true;
    }

    private static ItemStack[] snapshot(RecipeInput input) {
        int size = input.size();
        if (size == 0) return NO_KEY;
        ItemStack[] copy = new ItemStack[size];
        for (int i = 0; i < size; i++) copy[i] = input.getItem(i).copy();
        return copy;
    }
}
