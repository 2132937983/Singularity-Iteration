// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe;

import com.miophas.singularity_iteration.common.recipe.charge_carrying.ChargeCarryingRecipe;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * 0.1.10.38: an electric item or a machine block in a crafting recipe ({@code "type": "mio_icif:electric"} with {@code "item"} or
 * {@code "tag"}). It matches the item whatever its charge or stored data, like a plain item ingredient. What it shows and what
 * autocrafting patterns record (JEI, Refined Storage, AE2) is the stack a crafting grid makes from empty parts: the empty item.
 *
 * <p>36 electric items (super lapotron, charge batteries, power units, scanners, rifles...) are fully charged by default, and a plain
 * ingredient shows that default. Their crafted copies have 0 charge, so a pattern recorded from the recipe never matched what its
 * own sub-pattern made and those chains could not be autocrafted. Machine blocks (D14) record the plain block a recipe makes.
 */
public final class ElectricIngredient implements ICustomIngredient {
    public static final MapCodec<ElectricIngredient> CODEC = com.mojang.serialization.Codec.mapEither(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item"),
            TagKey.codec(Registries.ITEM).fieldOf("tag"))
        .xmap(ElectricIngredient::new, i -> i.target);

    public static final IngredientType<ElectricIngredient> TYPE = new IngredientType<>(CODEC);

    private final Either<Item, TagKey<Item>> target;

    public ElectricIngredient(Either<Item, TagKey<Item>> target) {
        this.target = Objects.requireNonNull(target);
    }

    public static Ingredient of(Item item) {
        return new ElectricIngredient(Either.left(item)).toVanilla();
    }

    /** The empty stack of {@code item}: a battery with 0 charge (as a crafting grid gives it), any other item plain. */
    public static ItemStack canonical(Item item) {
        ItemStack stack = new ItemStack(item);
        ChargeCarryingRecipe.chargeResult(stack, 0);
        return stack;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return target.map(stack::is, stack::is);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return target.map(item -> Stream.of(canonical(item)),
            tag -> BuiltInRegistries.ITEM.getTag(tag).stream().flatMap(set -> set.stream()).map(holder -> canonical(holder.value())));
    }

    /** The test depends on the item only, never on its components. */
    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    public Ingredient toVanilla() {
        return new Ingredient(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ElectricIngredient that && target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }
}
