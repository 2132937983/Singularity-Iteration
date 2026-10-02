package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Botania mana for the Armory, with no compile-time link to Botania.
 *
 * <p>The Armory answers Botania's {@code ManaReceiver} block capability, so Mana Spreaders
 * bound to it fill its mana tank. Stored mana repairs Botania gear (manasteel, elementium,
 * terrasteel... - they normally repair themselves from mana in the player's inventory) and
 * tops up mana items through Botania's {@code ManaItem} item capability.
 *
 * <p>Capabilities are found by their interface name among all registered capabilities, so
 * the bridge does not depend on where a given Botania version declares them.
 */
public final class ArmoryMana {
    private ArmoryMana() {}

    static final String RECEIVER = "vazkii.botania.api.mana.ManaReceiver";
    static final String ITEM = "vazkii.botania.api.mana.ManaItem";
    private static final String[] HOLDERS = {
        "vazkii.botania.api.BotaniaNeoForgeCapabilities", "vazkii.botania.neoforge.BotaniaNeoForgeCapabilities",
        "vazkii.botania.api.BotaniaForgeCapabilities", "vazkii.botania.common.capability.BotaniaCapabilities"};
    /** Mods whose damageable gear repairs from mana instead of EU. */
    private static final Set<String> MANA_GEAR_MODS = Set.of("botania", "extrabotany", "mythicbotany");
    /** Botania charges about 60-70 mana per point of durability for its own gear. */
    public static final int MANA_PER_DURABILITY = 70;

    @Nullable private static ItemCapability<Object, Void> itemCapability;
    private static boolean itemResolved;
    private static Method getMana, getMaxMana, addMana;

    public static boolean isManaGear(ItemStack stack) {
        return !stack.isEmpty() && MANA_GEAR_MODS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    // ------------------------------------------------------------------ block side
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register(RegisterCapabilitiesEvent event, BlockEntityType<mio_icif_armory> type) {
        Class<?> api = load(RECEIVER);
        if (api == null) return;
        BlockCapability<?, ?> capability = findBlock(api, "mana_receiver");
        if (capability == null) return;
        event.registerBlockEntity((BlockCapability) capability, type,
            (ICapabilityProvider) (tile, side) -> receiver(api, (mio_icif_armory) tile));
        Singularity_Iteration.LOGGER.info("[Armory] Botania detected: the Armory accepts mana ({})", capability.name());
    }

    private static Object receiver(Class<?> api, mio_icif_armory armory) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getManaReceiverLevel": return armory.getLevel();
                case "getManaReceiverPos": return armory.getBlockPos();
                case "getCurrentMana": return armory.mana();
                case "isFull": return armory.mana() >= mio_icif_armory.MANA_CAPACITY;
                case "receiveMana": armory.receiveMana(((Number) args[0]).intValue()); return null;
                case "canReceiveManaFromBursts": return true;
                case "equals": return proxy == args[0];
                case "hashCode": return System.identityHashCode(proxy);
                case "toString": return "SI Armory mana receiver at " + armory.getBlockPos();
                default:
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    return zero(method.getReturnType());
            }
        };
        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, handler);
    }

    // ------------------------------------------------------------------ item side
    /** Mana the item can still take (0 when it is no mana item or Botania is absent). */
    public static int itemRoom(ItemStack stack) {
        Object item = manaItem(stack);
        if (item == null) return 0;
        try {
            return Math.max(0, (int) getMaxMana.invoke(item) - (int) getMana.invoke(item));
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    /** Adds up to {@code mana}; returns how much was taken. */
    public static int fillItem(ItemStack stack, int mana) {
        Object item = manaItem(stack);
        if (item == null || mana <= 0) return 0;
        try {
            int take = Math.min(mana, Math.max(0, (int) getMaxMana.invoke(item) - (int) getMana.invoke(item)));
            if (take > 0) addMana.invoke(item, take);
            return take;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Object manaItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!itemResolved) {
            itemResolved = true;
            Class<?> api = load(ITEM);
            if (api != null) {
                try {
                    getMana = api.getMethod("getMana");
                    getMaxMana = api.getMethod("getMaxMana");
                    addMana = api.getMethod("addMana", int.class);
                    for (var cap : ItemCapability.getAll()) if (cap.typeClass() == api) itemCapability = (ItemCapability<Object, Void>) cap;
                } catch (ReflectiveOperationException | ClassCastException e) {
                    itemCapability = null;
                }
            }
        }
        return itemCapability == null ? null : stack.getCapability(itemCapability);
    }

    // ------------------------------------------------------------------ discovery
    @Nullable
    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, ArmoryMana.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError absent) {
            return null;
        }
    }

    @Nullable
    private static BlockCapability<?, ?> findBlock(Class<?> type, String guess) {
        for (String holder : HOLDERS) {
            try {
                Class.forName(holder, true, ArmoryMana.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError ignored) {
                // not this version's holder
            }
        }
        for (var cap : BlockCapability.getAll()) {
            if (cap.typeClass() == type && cap.contextClass() == Direction.class) return cap;
        }
        // Last resort: the conventional name; a capability is interned by name, so this is
        // the very same object Botania uses if the guess is right (and inert otherwise).
        return BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("botania", guess), type);
    }

    private static Object zero(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return (byte) 0;
    }
}
