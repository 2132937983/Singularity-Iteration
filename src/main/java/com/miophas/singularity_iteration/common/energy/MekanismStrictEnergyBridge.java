// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import com.miophas.singularity_iteration.core.runtime.energy.BatteryFeCapability;
import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.FeMachineBridge;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Optional Mekanism strict-energy view. The class has no Mekanism link at
 * compile time: when Mekanism is absent registration is a cheap no-op. When it
 * is present, SI exposes the same whole-EU balance through the official
 * IStrictEnergyHandler capability, without adding a second ledger.
 */
public final class MekanismStrictEnergyBridge {
    private static final String CAPABILITIES = "mekanism.common.capabilities.Capabilities";
    private static final String STRICT_HANDLER = "mekanism.api.energy.IStrictEnergyHandler";
    private static final String CONFIG = "mekanism.common.config.MekanismConfig";
    private static final System.Logger LOGGER = System.getLogger(MekanismStrictEnergyBridge.class.getName());
    private static volatile ConversionAccess conversionAccess;
    private static volatile boolean conversionApiUnavailable;
    private static int diagnosticFlags;
    private static volatile boolean registered;

    private MekanismStrictEnergyBridge() {
    }

    /** Current exact whole-J/EU ratio, or zero while the strict view cannot transfer. */
    public static long joulesPerEu() {
        ConversionAccess access = conversionAccess();
        if (access == null) return 0L;
        try {
            if (!Boolean.TRUE.equals(access.isLoaded().invoke(access.config()))) {
                warnOnce(1, "Mekanism server configuration is not loaded; SI strict-J transfers are unavailable until it loads.");
                return 0L;
            }
            // Cache reflection metadata, never the conversion value: an existing
            // capability must immediately follow configuration reloads, too.
            double feRate = ((Number) access.get().invoke(access.value())).doubleValue();
            double ratio = feRate * BatteryFeCapability.FE_PER_EU;
            if (!Double.isFinite(ratio) || ratio <= 0.0 || ratio >= 0x1.0p63 || ratio != Math.rint(ratio)) {
                warnOnce(2, "Mekanism feConversionRate=" + feRate
                    + " cannot express an exact positive whole-J/EU ratio; SI strict-J transfers are disabled while this value is active. FE remains available.");
                return 0L;
            }
            return (long) ratio;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            warnOnce(4, "Cannot read the active Mekanism energy conversion: " + error.getClass().getSimpleName()
                + "; SI strict-J transfers are disabled rather than using a fallback ratio. FE remains available.");
            return 0L;
        }
    }

    private static ConversionAccess conversionAccess() {
        ConversionAccess access = conversionAccess;
        if (access != null || conversionApiUnavailable) return access;
        synchronized (MekanismStrictEnergyBridge.class) {
            if (conversionAccess != null || conversionApiUnavailable) return conversionAccess;
            try {
                Class<?> type = Class.forName(CONFIG, false, MekanismStrictEnergyBridge.class.getClassLoader());
                Object general = type.getField("general").get(null);
                Object value = general.getClass().getField("forgeConversionRate").get(general);
                conversionAccess = new ConversionAccess(general, general.getClass().getMethod("isLoaded"),
                    value, value.getClass().getMethod("get"));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                conversionApiUnavailable = true;
                warnOnce(8, "Mekanism energy conversion API is unavailable: " + error.getClass().getSimpleName()
                    + "; SI strict-J transfers are disabled. FE remains available.");
            }
            return conversionAccess;
        }
    }

    private record ConversionAccess(Object config, Method isLoaded, Object value, Method get) {
    }

    private static synchronized void warnOnce(int flag, String message) {
        if ((diagnosticFlags & flag) == 0) {
            diagnosticFlags |= flag;
            LOGGER.log(System.Logger.Level.WARNING, message);
        }
    }

    public static void register(RegisterCapabilitiesEvent event) {
        if (registered || !isPresent(CAPABILITIES) || !isPresent(STRICT_HANDLER)) {
            return;
        }
        try {
            Class<?> caps = Class.forName(CAPABILITIES, false, MekanismStrictEnergyBridge.class.getClassLoader());
            Object multi = caps.getField("STRICT_ENERGY").get(null);
            Object block = multi.getClass().getMethod("block").invoke(multi);
            Object item = multi.getClass().getMethod("item").invoke(multi);
            if (!(block instanceof BlockCapability<?, ?> blockCapability)
                    || !(item instanceof ItemCapability<?, ?> itemCapability)) {
                return;
            }
            registerBlocks(event, blockCapability);
            registerItems(event, itemCapability);
            registered = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Optional dependency: FE remains the fallback integration path.
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerBlocks(RegisterCapabilitiesEvent event, BlockCapability<?, ?> capability) {
        for (var type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            var key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (key == null || !"mio_icif".equals(key.getNamespace())) {
                continue;
            }
            event.registerBlockEntity((BlockCapability) capability, type,
                (ICapabilityProvider) (tile, side) -> tile instanceof AbstractEnergyBlockEntity machine
                    ? handler(machine, (Direction) side) : null);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerItems(RegisterCapabilitiesEvent event, ItemCapability<?, ?> capability) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof IBatteryItem)) {
                continue;
            }
            event.registerItem((ItemCapability) capability,
                (ICapabilityProvider) (stack, ignored) -> handler((ItemStack) stack), item);
        }
    }

    private static Object handler(AbstractEnergyBlockEntity machine, Direction side) {
        // Match the FE bridge: side-restricted producers/converters expose no
        // capability on a blocked port instead of returning a proxy that would
        // later dereference a null storage view.
        var port = machine.scexFeCapability(side);
        return port == null ? null : proxy(new BlockView(machine, side, port));
    }

    private static Object handler(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1
                || !(stack.getItem() instanceof IBatteryItem)) return null;
        return proxy(new ItemView(stack));
    }

    /** Resolved once: the capability providers ask for a view on every query. */
    private static volatile Class<?> strictHandler;

    private static Object proxy(View view) {
        try {
            Class<?> api = strictHandler;
            if (api == null) strictHandler = api = Class.forName(STRICT_HANDLER, false, MekanismStrictEnergyBridge.class.getClassLoader());
            return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == (args == null ? null : args[0]);
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SCEX Mekanism strict-energy view";
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                }
                return view.call(method, args);
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static boolean isPresent(String name) {
        try {
            Class.forName(name, false, MekanismStrictEnergyBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private interface View {
        Object call(Method method, Object[] args);
    }

    private abstract static class BaseView implements View {
        protected abstract long eu();
        protected abstract long maxEu();
        protected abstract long insertEu(long amount, boolean simulate);
        protected abstract long extractEu(long amount, boolean simulate);

        @Override
        public Object call(Method method, Object[] args) {
            String name = method.getName();
            long unit = joulesPerEu();
            return switch (name) {
                case "getEnergyContainerCount" -> 1;
                case "getEnergy" -> args != null && args.length > 0 && ((Number) args[0]).intValue() == 0 ? toJ(eu(), unit) : 0L;
                case "getMaxEnergy" -> args != null && args.length > 0 && ((Number) args[0]).intValue() == 0 ? toJ(maxEu(), unit) : 0L;
                case "getNeededEnergy" -> args != null && args.length > 0 && ((Number) args[0]).intValue() == 0 ? toJ(Math.max(0L, maxEu() - eu()), unit) : 0L;
                case "insertEnergy" -> transfer(args, true, unit);
                case "extractEnergy" -> transfer(args, false, unit);
                case "setEnergy" -> throw new UnsupportedOperationException("Use energy transfer methods on an SI energy view");
                default -> defaultValue(method.getReturnType());
            };
        }

        private long transfer(Object[] args, boolean insert, long unit) {
            if (args == null || (args.length != 2 && args.length != 3)) return 0L;
            int offset = args.length == 3 ? 1 : 0;
            long joules = Math.max(0L, ((Number) args[offset]).longValue());
            if (args.length == 3 && ((Number) args[0]).intValue() != 0) return insert ? joules : 0L;
            if (unit <= 0L) return insert ? joules : 0L;
            long eu = joules / unit;
            if (eu == 0) return insert ? joules : 0L;
            boolean simulate = !"EXECUTE".equals(String.valueOf(args[offset + 1]));
            long moved = insert ? insertEu(eu, simulate) : extractEu(eu, simulate);
            if (moved < 0 || moved > eu) throw new IllegalStateException("Invalid SI Joule transfer receipt");
            long movedJ = toJ(moved, unit);
            return insert ? Math.max(0L, joules - movedJ) : movedJ;
        }
    }

    private static final class BlockView extends BaseView {
        private final AbstractEnergyBlockEntity owner;
        private final Direction side;
        private final IEnergyStorage port;
        private BlockView(AbstractEnergyBlockEntity owner, Direction side, IEnergyStorage port) {
            this.owner = owner;
            this.side = side;
            this.port = port;
        }
        private FeMachineBridge bridge() { return owner.scexFeBridge(); }
        private com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage storage() {
            return owner.getEnergyStorageInternal();
        }
        @Override protected long eu() { return storage().getAmount(); }
        @Override protected long maxEu() { return storage().getCapacity(); }
        @Override protected long insertEu(long amount, boolean simulate) {
            return port.canReceive() ? bridge().receiveWholeEu(amount, simulate) : 0L;
        }
        @Override protected long extractEu(long amount, boolean simulate) {
            return port.canExtract() ? bridge().extractWholeEu(amount, simulate) : 0L;
        }
    }

    private static final class ItemView extends BaseView {
        private final ItemStack stack;
        private final IBatteryItem battery;
        private ItemView(ItemStack stack) { this.stack = stack; this.battery = (IBatteryItem) stack.getItem(); }
        @Override protected long eu() { return battery.getEnergy(stack); }
        @Override protected long maxEu() { return battery.getMaxEnergy(stack); }
        @Override protected long insertEu(long amount, boolean simulate) {
            return BatteryTransfer.transfer(stack, battery,
                Math.min(amount, Math.max(0L, battery.getChargeRate(stack))), true, simulate);
        }
        @Override protected long extractEu(long amount, boolean simulate) {
            if (!BatteryTransfer.canProvideExternally(stack)) return 0L;
            return BatteryTransfer.transfer(stack, battery,
                Math.min(amount, Math.max(0L, battery.getChargeRate(stack))), false, simulate);
        }
    }

    private static long toJ(long eu, long unit) {
        if (eu <= 0 || unit <= 0) return 0L;
        return eu > Long.MAX_VALUE / unit ? Long.MAX_VALUE : eu * unit;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return (char) 0;
        return null;
    }
}


