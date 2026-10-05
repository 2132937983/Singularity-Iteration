// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.item.armor.ArmorFeatureSlots;
import com.miophas.singularity_iteration.common.network.EquipmentConsolePacket;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side energy flow statistics of everything the player wears and holds (armor, back
 * slot, Curios accessories, both hands), sampled every tick from the synced item energy.
 *
 * <p>Per second it books, for each item, the EU it gained and the EU it lost. Energy that only
 * moved between the player's own items (a battery pack topping up the chestplate) is netted
 * out, so <i>Income</i> is what came from outside (chargers, solar helmets, generators) and
 * <i>Drain</i> is what features actually consumed. The console shows the rolling 3-second
 * rates, a one-minute history, the per-item drain ranking and the time until every store
 * is empty.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class EquipmentEnergyMonitor {
    private EquipmentEnergyMonitor() {}

    public static final int HISTORY = 60;          // seconds of history
    private static final int WINDOW = 3;           // seconds averaged for the live readouts

    /** One item's contribution over the averaging window. */
    public record Source(String id, ItemStack stack, double drainPerSecond, double incomePerSecond, long stored, long capacity) { }

    private record Sample(net.minecraft.world.item.Item item, long energy) { }

    private static final Map<String, Sample> last = new HashMap<>();
    private static final Map<String, double[]> secondFlow = new LinkedHashMap<>();   // id -> {gain, loss} this second
    private static final Map<String, ItemStack> stacks = new LinkedHashMap<>();
    private static final List<Map<String, double[]>> seconds = new ArrayList<>();    // last WINDOW seconds per item
    private static final float[] incomeHistory = new float[HISTORY], drainHistory = new float[HISTORY], storedHistory = new float[HISTORY];
    private static int head, filled, tick;
    private static long stored, capacity;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }
        // sample every SAMPLE_EVERY ticks: the deltas add up the same, at a quarter of the cost
        if (++clientTick % SAMPLE_EVERY != 0) return;
        sample(player);
    }

    private static final int SAMPLE_EVERY = 4;
    private static int clientTick;

    private static void reset() {
        if (last.isEmpty() && filled == 0) return;
        last.clear(); secondFlow.clear(); stacks.clear(); seconds.clear();
        java.util.Arrays.fill(incomeHistory, 0); java.util.Arrays.fill(drainHistory, 0); java.util.Arrays.fill(storedHistory, 0);
        head = filled = tick = 0;
        rankingTick = -1;
    }

    /** Every electric item the player carries in a worn / held position, keyed by its slot id. */
    public static List<ArmorFeatureSlots.EquippedSlot> electricSlots(Player player) {
        List<ArmorFeatureSlots.EquippedSlot> out = new ArrayList<>();
        for (var slot : ArmorFeatureSlots.equipped(player)) if (isElectric(slot.stack())) out.add(slot);
        if (isElectric(player.getMainHandItem())) out.add(new ArmorFeatureSlots.EquippedSlot(EquipmentConsolePacket.MAINHAND, null, player.getMainHandItem()));
        if (isElectric(player.getOffhandItem())) out.add(new ArmorFeatureSlots.EquippedSlot(EquipmentConsolePacket.OFFHAND, null, player.getOffhandItem()));
        return out;
    }

    public static boolean isElectric(ItemStack s) {
        if (s.isEmpty()) return false;
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        return api.isElectricArmor(s) || api.isElectricTool(s) || api.isBattery(s);
    }

    public static long[] energy(ItemStack s) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        if (api.isElectricArmor(s)) return new long[]{api.getElectricArmorStored(s), api.getElectricArmorMaxEnergy(s)};
        if (api.isElectricTool(s)) return new long[]{api.getElectricToolStored(s), api.getElectricToolMaxEnergy(s)};
        if (api.isBattery(s)) return new long[]{api.getBatteryStored(s), api.getBatteryCapacity(s)};
        return new long[]{0, 0};
    }

    private static void sample(Player player) {
        IItemAPI api = MioIcifAPI.instance().getItemAPI();
        long st = 0, cap = 0;
        double gainItems = 0, lossBatteries = 0;
        Map<String, Sample> seen = new HashMap<>();
        for (var slot : electricSlots(player)) {
            ItemStack stack = slot.stack();
            long[] en = energy(stack);
            st += en[0];
            cap += en[1];
            Sample prev = last.get(slot.id());
            seen.put(slot.id(), new Sample(stack.getItem(), en[0]));
            stacks.put(slot.id(), stack);
            if (prev == null || prev.item() != stack.getItem()) continue;             // new item in this slot: baseline only
            long delta = en[0] - prev.energy();
            if (delta == 0) continue;
            double[] flow = secondFlow.computeIfAbsent(slot.id(), k -> new double[2]);
            if (delta > 0) {
                flow[0] += delta;
                if (!api.isBattery(stack)) gainItems += delta;
            } else {
                flow[1] -= delta;
                if (api.isBattery(stack)) lossBatteries -= delta;
            }
        }
        // energy that only moved battery -> equipment is internal: book it on neither side
        double internal = Math.min(gainItems, lossBatteries);
        if (internal > 0) {
            double[] t = secondFlow.computeIfAbsent("#internal", k -> new double[2]);
            t[0] += internal;
        }
        last.keySet().retainAll(seen.keySet());
        last.putAll(seen);
        stacks.keySet().retainAll(seen.keySet());
        stored = st;
        capacity = cap;
        tick += SAMPLE_EVERY;
        if (tick % 20 != 0) return;
        // close this second
        double internalSecond = secondFlow.containsKey("#internal") ? secondFlow.remove("#internal")[0] : 0;
        double gain = 0, loss = 0;
        for (double[] f : secondFlow.values()) { gain += f[0]; loss += f[1]; }
        incomeHistory[head] = (float) Math.max(0, gain - internalSecond);
        drainHistory[head] = (float) Math.max(0, loss - internalSecond);
        storedHistory[head] = st;
        head = (head + 1) % HISTORY;
        filled = Math.min(HISTORY, filled + 1);
        Map<String, double[]> copy = new LinkedHashMap<>();
        secondFlow.forEach((k, v) -> copy.put(k, v.clone()));
        if (internalSecond > 0) copy.put("#internal", new double[]{internalSecond, 0});
        seconds.add(copy);
        while (seconds.size() > WINDOW) seconds.removeFirst();
        secondFlow.clear();
    }

    // ------------------------------------------------------------------ queries for the console
    public static boolean hasData() { return filled > 0; }
    public static long stored() { return stored; }
    public static long capacity() { return capacity; }

    private static double windowSum(int index) {
        double s = 0, internal = 0;
        for (Map<String, double[]> sec : seconds) {
            for (var e : sec.entrySet()) {
                if (e.getKey().equals("#internal")) internal += e.getValue()[0];
                else s += e.getValue()[index];
            }
        }
        return Math.max(0, s - internal) / Math.max(1, seconds.size());
    }

    /** External charge into the gear, EU/s (3 s average). */
    public static double income() { return windowSum(0); }

    /** Consumption by the gear, EU/s (3 s average). */
    public static double drain() { return windowSum(1); }

    public static double net() { return income() - drain(); }

    /** Seconds until every store is empty at the current net drain; +inf when not draining. */
    public static double depletionSeconds() {
        double net = net();
        return net < -1e-6 ? stored / -net : Double.POSITIVE_INFINITY;
    }

    /** Seconds until everything is full at the current net gain; +inf when not charging. */
    public static double fullSeconds() {
        double net = net();
        return net > 1e-6 ? (capacity - stored) / net : Double.POSITIVE_INFINITY;
    }

    /** Items ranked by their drain (EU/s, 3 s average), heaviest first; idle items last. */
    private static List<Source> rankingCache = List.of();
    private static long rankingTick = -1;

    /** Cached per client tick: the console asks several times per frame. */
    public static List<Source> ranking() {
        if (rankingTick == tick) return rankingCache;
        rankingTick = tick;
        return rankingCache = computeRanking();
    }

    private static List<Source> computeRanking() {
        Map<String, double[]> sum = new LinkedHashMap<>();
        for (Map<String, double[]> sec : seconds) {
            for (var e : sec.entrySet()) {
                if (e.getKey().startsWith("#")) continue;
                double[] t = sum.computeIfAbsent(e.getKey(), k -> new double[2]);
                t[0] += e.getValue()[0];
                t[1] += e.getValue()[1];
            }
        }
        int n = Math.max(1, seconds.size());
        List<Source> out = new ArrayList<>();
        for (var e : stacks.entrySet()) {
            double[] t = sum.getOrDefault(e.getKey(), new double[2]);
            long[] en = energy(e.getValue());
            out.add(new Source(e.getKey(), e.getValue(), t[1] / n, t[0] / n, en[0], en[1]));
        }
        out.sort((a, b) -> Double.compare(b.drainPerSecond(), a.drainPerSecond()));
        return out;
    }

    /** Oldest-first history (length = seconds recorded so far, at most {@link #HISTORY}). */
    public static float[] incomeHistory() { return ordered(incomeHistory); }
    public static float[] drainHistory() { return ordered(drainHistory); }
    public static float[] storedHistory() { return ordered(storedHistory); }

    private static float[] ordered(float[] ring) {
        float[] out = new float[filled];
        for (int i = 0; i < filled; i++) out[i] = ring[Math.floorMod(head - filled + i, HISTORY)];
        return out;
    }
}
