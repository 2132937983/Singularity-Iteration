// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.tuning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data-driven base values of machines and tools (0.1.7.36, pilot).
 *
 * <p>Files: {@code data/<namespace>/si_tuning/<name>.json}. Each file is one flat JSON object of
 * number values. The file id ({@code <namespace>:<name>}) and the key select one value:
 * <pre>{@code
 * // data/mio_icif/si_tuning/ov_scanner.json
 * { "scan_radius": 12, "y_radius": 6, "energy_per_use": 96 }
 * }</pre>
 *
 * <p>Why: before 0.1.7.36 these values were {@code static final} constants. A pack maker had to
 * change the jar to change a scan range or a cost. Now a datapack can override a file with the
 * same id. Vanilla rules apply: the pack with the highest priority supplies the whole file.
 * A key that the file does not contain uses the default in the code.
 *
 * <p>Contract:
 * <ul>
 *   <li>Each value is a {@link Value} handle with a default, a minimum and a maximum. Code reads
 *       the value with {@link IntValue#get()} etc. at use time, never in a static initializer.</li>
 *   <li>A value out of range is clamped. A value that is not a number uses the default. Both
 *       write one warning per reload.</li>
 *   <li>A key in a file that no handle declares writes a warning (it is usually a typo).</li>
 *   <li>The defaults in the code and the files in {@code data/mio_icif/si_tuning} must agree.
 *       {@code Round36GameTests} checks this.</li>
 * </ul>
 *
 * <p>Threading: the reload runs on the server thread and replaces one immutable map
 * ({@code volatile}). Reads from any thread see one complete map. Before the first reload
 * (for example on a client in a multiplayer world) every read returns the default.
 *
 * <p>Scope: server-side values only. The client does not get the values; the client text that
 * shows a value uses the default. A later step can add a sync payload when a value is shown.
 */
public final class SiTuning extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "si_tuning";
    private static final Gson GSON = new GsonBuilder().create();

    /** All declared handles, in declaration order. Written only during class initialization. */
    private static final List<Value> DECLARED = Collections.synchronizedList(new ArrayList<>());
    /** File id → key → number. Replaced as a whole on reload. */
    private static volatile Map<ResourceLocation, Map<String, Double>> values = Map.of();
    /** Reload counter (diagnostics and tests). */
    private static volatile int generation;

    /** Number of completed reloads. */
    public static int generation() { return generation; }

    public SiTuning() {
        super(GSON, DIRECTORY);
    }

    // ------------------------------------------------------------------ handles

    /** One tuning value: file id, key, default and the allowed range. */
    public abstract static sealed class Value permits IntValue, LongValue, DoubleValue {
        public final ResourceLocation file;
        public final String key;
        final double min, max;

        Value(ResourceLocation file, String key, double def, double min, double max) {
            if (min > max || def < min || def > max)
                throw new IllegalArgumentException("Bad tuning range for " + file + "/" + key);
            this.file = file;
            this.key = key;
            this.min = min;
            this.max = max;
            DECLARED.add(this);
        }

        public abstract double defaultValue();

        /** The current value, clamped into {@code min..max}. */
        final double raw() {
            // Two map reads, no cache: a cache shared between threads would need its own
            // memory ordering, and the callers read a value at most once per use.
            Map<String, Double> file = values.get(this.file);
            Double v = file == null ? null : file.get(key);
            return v == null ? defaultValue() : Math.max(min, Math.min(max, v));
        }
    }

    public static final class IntValue extends Value {
        private final int def;
        IntValue(ResourceLocation file, String key, int def, int min, int max) { super(file, key, def, min, max); this.def = def; }
        @Override public double defaultValue() { return def; }
        public int get() { return (int) Math.round(raw()); }
    }

    public static final class LongValue extends Value {
        private final long def;
        LongValue(ResourceLocation file, String key, long def, long min, long max) { super(file, key, def, min, max); this.def = def; }
        @Override public double defaultValue() { return def; }
        public long get() { return Math.round(raw()); }
    }

    public static final class DoubleValue extends Value {
        private final double def;
        DoubleValue(ResourceLocation file, String key, double def, double min, double max) { super(file, key, def, min, max); this.def = def; }
        @Override public double defaultValue() { return def; }
        public double get() { return raw(); }
    }

    /** Declares an int value in {@code data/mio_icif/si_tuning/<file>.json}. */
    public static IntValue intValue(String file, String key, int def, int min, int max) {
        return new IntValue(id(file), key, def, min, max);
    }

    /** Declares a long value in {@code data/mio_icif/si_tuning/<file>.json}. */
    public static LongValue longValue(String file, String key, long def, long min, long max) {
        return new LongValue(id(file), key, def, min, max);
    }

    /** Declares a double value in {@code data/mio_icif/si_tuning/<file>.json}. */
    public static DoubleValue doubleValue(String file, String key, double def, double min, double max) {
        return new DoubleValue(id(file), key, def, min, max);
    }

    private static ResourceLocation id(String file) {
        return ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, file);
    }

    /** A copy of the declared handles (for tests and documentation tools). */
    public static List<Value> declared() {
        synchronized (DECLARED) { return List.copyOf(DECLARED); }
    }

    /** The loaded number for {@code file}/{@code key}, or null when no file has it. */
    public static Double loaded(ResourceLocation file, String key) {
        Map<String, Double> m = values.get(file);
        return m == null ? null : m.get(key);
    }

    // ------------------------------------------------------------------ reload

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        // Make sure every holder class has declared its handles before the check below.
        SiTuningValues.init();
        Map<ResourceLocation, Map<String, Double>> next = new HashMap<>();
        int count = 0;
        for (var file : new TreeMap<>(files).entrySet()) {
            if (!file.getValue().isJsonObject()) {
                Singularity_Iteration.LOGGER.warn("[Tuning] {} is not a JSON object; skipped", file.getKey());
                continue;
            }
            Map<String, Double> numbers = new HashMap<>();
            for (var e : file.getValue().getAsJsonObject().entrySet()) {
                if (e.getValue() instanceof JsonPrimitive p && p.isNumber()) {
                    numbers.put(e.getKey(), p.getAsDouble());
                    count++;
                } else {
                    Singularity_Iteration.LOGGER.warn("[Tuning] {}/{} is not a number; the default applies", file.getKey(), e.getKey());
                }
            }
            next.put(file.getKey(), Map.copyOf(numbers));
        }
        check(next);
        values = Map.copyOf(next);
        generation++;
        Singularity_Iteration.LOGGER.info("[Tuning] {} values from {} files", count, next.size());
    }

    /** Warns about unknown keys and values out of range. It changes nothing. */
    private static void check(Map<ResourceLocation, Map<String, Double>> next) {
        Map<ResourceLocation, java.util.Set<String>> known = new HashMap<>();
        for (Value v : declared()) {
            known.computeIfAbsent(v.file, k -> new java.util.HashSet<>()).add(v.key);
            Map<String, Double> file = next.get(v.file);
            Double d = file == null ? null : file.get(v.key);
            if (d != null && (d < v.min || d > v.max)) {
                Singularity_Iteration.LOGGER.warn("[Tuning] {}/{} = {} is out of range {}..{}; clamped",
                    v.file, v.key, d, v.min, v.max);
            }
        }
        for (var file : next.entrySet()) {
            var keys = known.get(file.getKey());
            for (String key : file.getValue().keySet()) {
                if (keys == null || !keys.contains(key))
                    Singularity_Iteration.LOGGER.warn("[Tuning] {}/{} is not a known key; ignored", file.getKey(), key);
            }
        }
    }

    @EventBusSubscriber(modid = Singularity_Iteration.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onReloadListeners(AddReloadListenerEvent event) {
            event.addListener(new SiTuning());
        }
    }
}
