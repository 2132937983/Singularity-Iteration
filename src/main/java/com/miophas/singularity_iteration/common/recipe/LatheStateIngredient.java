package com.miophas.singularity_iteration.common.recipe;

import com.miophas.singularity_iteration.common.item.lathe.LatheTurningBlanksItem;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A lathe workpiece turned to one shape: the item and the thickness of its 5 positions. IC2 matched
 * turning blanks by their damage, the numeric state (itemTurningBlanks@344865 = 0x54321 = 5, 4, 3, 2, 1).
 * <p>JSON: {@code {"type": "mio_icif:lathe_state", "item": "mio_icif:normal/item_iron_turning_blanks", "state": [5, 4, 3, 2, 1]}}
 */
@SuppressWarnings("null")
public class LatheStateIngredient implements ICustomIngredient {

    private static final Codec<List<Integer>> STATE_CODEC = Codec.INT.listOf().validate(state -> state.size() == 5
        ? DataResult.success(state) : DataResult.error(() -> "A lathe state has 5 positions: " + state));

    public static final MapCodec<LatheStateIngredient> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(i -> i.item),
            STATE_CODEC.fieldOf("state").forGetter(i -> Arrays.stream(i.state).boxed().toList())
        ).apply(instance, (item, state) -> new LatheStateIngredient(item, state.stream().mapToInt(Integer::intValue).toArray()))
    );

    public static final IngredientType<LatheStateIngredient> TYPE = new IngredientType<>(CODEC);

    private final Item item;
    private final int[] state;

    public LatheStateIngredient(Item item, int[] state) {
        this.item = item;
        this.state = state.clone();
    }

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && stack.is(item) && item instanceof ILatheItem lathe
            && Arrays.equals(lathe.getCurrentState(stack), state);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(LatheTurningBlanksItem.getStackForState(new ItemStack(item), state));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LatheStateIngredient that)) return false;
        return item == that.item && Arrays.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(item) + Arrays.hashCode(state);
    }
}
