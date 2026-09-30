package example.sicore;

import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** Addon seed with its own storage layout, consumed by the public analyzer contract. */
public final class ForeignCropSeed extends Item implements ICropSeedItem {
    public ForeignCropSeed() { super(new Properties().stacksTo(1)); }
    @Override public String cropModId(ItemStack stack) { return "mio_icif"; }
    @Override public String cropId(ItemStack stack) { return "wheat"; }
    @Override public int cropScanLevel(ItemStack stack) { return cropData(stack).getInt("AddonScan"); }
    @Override public void setCropScanLevel(ItemStack stack, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("AddonScan", level));
    }
}
