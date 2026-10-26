// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 0.1.10.21 (futures M3): how many processing steps an item is away from raw materials. Pure.
 *
 * <p>An item that no recipe makes has depth 0 (ores, logs, crops, mob drops). An item that recipes make has depth
 * {@code 1 + the largest depth of the ingredients} of its shallowest recipe, where an ingredient with several
 * choices takes its shallowest choice. Recipe cycles (ingot to block to ingot) do not raise the depth: the value is
 * the least fixed point, found by relaxation from "unknown" with at most {@code maxPasses} passes in total.
 * Items that only recipe cycles make (raw iron and its block) are obtained some other way: once the relaxation
 * settles, the unknown items that need nothing outside their own cycle (the bottom strongly connected components of
 * "needs") count as raw (depth 0), and the relaxation goes on from there. Items still unknown when the passes run out
 * are at least that deep and get {@code maxPasses}.
 *
 * <p>The exchange uses it for the currency of a player listing: below {@code uuDepth} coins, from it UU-matter.
 */
public final class CraftDepth {
    private CraftDepth() {}

    /** One recipe: an output and its ingredient slots, each a set of accepted items. */
    public record Recipe<K>(K output, List<Set<K>> slots) { }

    /** Depth per item; an item missing from the result has depth 0. Work is bounded by passes x recipe slots. */
    public static <K> Map<K, Integer> compute(List<Recipe<K>> recipes, int maxPasses) {
        Map<K, Integer> depth = new HashMap<>();
        Set<K> produced = new HashSet<>();
        for (Recipe<K> r : recipes) produced.add(r.output());
        int passes = 0;
        while (true) {
            boolean settled = false;
            while (passes < maxPasses) {
                passes++;
                if (!relax(recipes, produced, depth)) { settled = true; break; }
            }
            List<K> unknown = new ArrayList<>();
            for (K item : produced) if (!depth.containsKey(item)) unknown.add(item);
            if (unknown.isEmpty()) break;
            if (!settled) {
                for (K item : unknown) depth.put(item, maxPasses);
                break;
            }
            for (K item : cycleOnly(recipes, produced, depth)) depth.put(item, 0);
        }
        return depth;
    }

    private record Frame<K>(K item, Iterator<K> next) { }

    /**
     * The unknown items that only cycles among unknown items make: the bottom strongly connected components of the
     * graph "item needs item" (an unknown item needs the items of a recipe slot whose every choice is unknown).
     * Iterative Tarjan; never empty while unknown items remain.
     */
    static <K> List<K> cycleOnly(List<Recipe<K>> recipes, Set<K> produced, Map<K, Integer> depth) {
        Map<K, Set<K>> needs = new HashMap<>();
        for (Recipe<K> r : recipes) {
            if (depth.containsKey(r.output())) continue;
            Set<K> edges = needs.computeIfAbsent(r.output(), k -> new HashSet<>());
            for (Set<K> slot : r.slots()) {
                boolean blocked = !slot.isEmpty();
                for (K item : slot) if (!produced.contains(item) || depth.containsKey(item)) { blocked = false; break; }
                if (blocked) edges.addAll(slot);
            }
        }
        Map<K, Integer> index = new HashMap<>(), low = new HashMap<>(), component = new HashMap<>();
        Deque<K> stack = new ArrayDeque<>();
        Set<K> onStack = new HashSet<>();
        List<List<K>> components = new ArrayList<>();
        int counter = 0;
        for (K root : needs.keySet()) {
            if (index.containsKey(root)) continue;
            Deque<Frame<K>> work = new ArrayDeque<>();
            index.put(root, counter); low.put(root, counter++); stack.push(root); onStack.add(root);
            work.push(new Frame<>(root, needs.get(root).iterator()));
            while (!work.isEmpty()) {
                Frame<K> frame = work.peek();
                K v = frame.item();
                if (frame.next().hasNext()) {
                    K w = frame.next().next();
                    if (!needs.containsKey(w)) continue;
                    if (!index.containsKey(w)) {
                        index.put(w, counter); low.put(w, counter++); stack.push(w); onStack.add(w);
                        work.push(new Frame<>(w, needs.get(w).iterator()));
                    } else if (onStack.contains(w)) {
                        low.put(v, Math.min(low.get(v), index.get(w)));
                    }
                    continue;
                }
                work.pop();
                if (!work.isEmpty()) {
                    K parent = work.peek().item();
                    low.put(parent, Math.min(low.get(parent), low.get(v)));
                }
                if (low.get(v).equals(index.get(v))) {
                    List<K> members = new ArrayList<>();
                    K w;
                    do {
                        w = stack.pop();
                        onStack.remove(w);
                        component.put(w, components.size());
                        members.add(w);
                    } while (!w.equals(v));
                    components.add(members);
                }
            }
        }
        List<K> out = new ArrayList<>();
        for (int c = 0; c < components.size(); c++) {
            boolean bottom = true;
            for (K v : components.get(c)) {
                for (K w : needs.get(v)) if (!Integer.valueOf(c).equals(component.get(w))) { bottom = false; break; }
                if (!bottom) break;
            }
            if (bottom) out.addAll(components.get(c));
        }
        return out;
    }

    /** One pass over the recipes; true when a depth changed. */
    private static <K> boolean relax(List<Recipe<K>> recipes, Set<K> produced, Map<K, Integer> depth) {
        boolean changed = false;
        for (Recipe<K> r : recipes) {
            int worst = 0;
            boolean known = true;
            for (Set<K> slot : r.slots()) {
                if (slot.isEmpty()) continue;
                int best = Integer.MAX_VALUE;
                for (K item : slot) best = Math.min(best, !produced.contains(item) ? 0 : depth.getOrDefault(item, Integer.MAX_VALUE));
                if (best == Integer.MAX_VALUE) { known = false; break; }
                worst = Math.max(worst, best);
            }
            if (!known) continue;
            int candidate = worst + 1;
            Integer old = depth.get(r.output());
            if (old == null || candidate < old) { depth.put(r.output(), candidate); changed = true; }
        }
        return changed;
    }
}
