package vazkii.botania.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaReceiver;

/** Test-only stand-in for Botania's capability holder. */
public final class BotaniaNeoForgeCapabilities {
    private BotaniaNeoForgeCapabilities() {}

    public static final BlockCapability<ManaReceiver, Direction> MANA_RECEIVER =
        BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("botania", "mana_receiver"), ManaReceiver.class);
    public static final ItemCapability<ManaItem, Void> MANA_ITEM =
        ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("botania", "mana_item"), ManaItem.class);
}
