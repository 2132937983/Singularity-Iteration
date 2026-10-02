package vazkii.botania.stub;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import vazkii.botania.api.BotaniaNeoForgeCapabilities;
import vazkii.botania.api.mana.ManaItem;

/**
 * Headless test stand-in for Botania (compat GameTest server only, never shipped): just
 * the public mana API, one mana-repaired armour piece and one mana-holding item.
 */
@Mod("botania")
public final class BotaniaStub {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("botania");
    public static final DeferredItem<ArmorItem> MANASTEEL_HELMET = ITEMS.register("manasteel_helmet",
        () -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties().durability(200)));
    public static final DeferredItem<Item> MANA_TABLET = ITEMS.register("mana_tablet", () -> new Item(new Item.Properties().stacksTo(1)));

    public BotaniaStub(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(BotaniaStub::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(BotaniaNeoForgeCapabilities.MANA_ITEM, (stack, ctx) -> new Tablet(stack), MANA_TABLET.get());
    }

    /** 500,000 mana stored in the stack's custom data. */
    record Tablet(ItemStack stack) implements ManaItem {
        @Override public int getMana() { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("mana"); }
        @Override public int getMaxMana() { return 500_000; }
        @Override public void addMana(int mana) {
            int next = Math.max(0, Math.min(getMaxMana(), getMana() + mana));
            CompoundTag tag = new CompoundTag();
            tag.putInt("mana", next);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
