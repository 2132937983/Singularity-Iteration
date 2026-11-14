package example.sicore;

import com.miophas.singularity_iteration.core.api.upgrade.tile.BonusOutputs;
import com.miophas.singularity_iteration.core.api.upgrade.tile.CycleDecision;
import com.miophas.singularity_iteration.core.api.upgrade.tile.IWorkCycleUpgrade;
import com.miophas.singularity_iteration.core.api.upgrade.tile.UpgradableProperty;
import com.miophas.singularity_iteration.core.api.upgrade.tile.WorkCycleContext;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Example of the 0.1.11.2 work cycle upgrade API ({@link IWorkCycleUpgrade}). The behaviour comes from the custom
 * data of the stack, so one registered item shows every answer:
 * <ul>
 *   <li>{@code hold: true} - the machine does not start ({@link CycleDecision#hold()});</li>
 *   <li>{@code duration} / {@code energy} (floats) - the cycle factors ({@link CycleDecision#proceed(float, float)});</li>
 *   <li>{@code bonus} (an item id) - one such item as a bonus output at the end of each operation.</li>
 * </ul>
 * Without custom data the upgrade changes nothing. Uses only the published core JAR.
 */
public final class ExampleCycleUpgrade extends Item implements IWorkCycleUpgrade {
    /** The last start context of each machine position (for the GameTests). */
    static final Map<BlockPos, WorkCycleContext> LAST_START = new ConcurrentHashMap<>();
    /** Completed operations seen by this upgrade, per machine position (for the GameTests). */
    static final Map<BlockPos, Integer> COMPLETIONS = new ConcurrentHashMap<>();

    public ExampleCycleUpgrade() { super(new Item.Properties().stacksTo(16)); }

    /** A stack with the given custom data. */
    public static ItemStack configured(CompoundTag data) {
        ItemStack stack = new ItemStack(CoreExampleMod.CYCLE_UPGRADE.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties) {
        return true;
    }

    @Override
    public CycleDecision onCycleStart(ItemStack upgrade, WorkCycleContext ctx) {
        LAST_START.put(ctx.pos().immutable(), ctx);
        CompoundTag data = data(upgrade);
        if (data.getBoolean("hold")) return CycleDecision.hold();
        float duration = data.contains("duration") ? data.getFloat("duration") : 1.0F;
        float energy = data.contains("energy") ? data.getFloat("energy") : 1.0F;
        return CycleDecision.proceed(duration, energy);
    }

    @Override
    public void onCycleComplete(ItemStack upgrade, WorkCycleContext ctx, BonusOutputs out) {
        COMPLETIONS.merge(ctx.pos().immutable(), 1, Integer::sum);
        String bonus = data(upgrade).getString("bonus");
        if (bonus.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(bonus);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) out.add(new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }
}
