// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.uu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded independent recipe accounting. Reference quotes stay authoritative. */
public final class UuRecipeSolver<K> {
    public record Choice<K>(K consumed, K returned) { }
    public record Rule<K>(String id, K output, int outputCount, List<List<Choice<K>>> slots, double overhead) {
        public Rule {
            if (id == null || output == null || outputCount < 1 || outputCount > 64
                    || slots == null || slots.isEmpty() || slots.size() > 81
                    || !Double.isFinite(overhead) || overhead <= 0) throw new IllegalArgumentException("Recipe bounds");
            slots = slots.stream().map(List::copyOf).toList();
            if (slots.stream().anyMatch(s -> s.isEmpty() || s.size() > 256
                    || s.stream().anyMatch(c -> c.consumed() == null))) throw new IllegalArgumentException("Ingredient bounds");
        }
    }
    public record Solution<K>(Map<K, Double> values, int derived, int work, boolean complete) { }
    private record Value<K>(double amount, Set<K> ancestors) { }
    private final int maxNodes;
    private final int maxWork;

    public UuRecipeSolver(int maxNodes, int maxWork) {
        if (maxNodes < 1 || maxNodes > 16384 || maxWork < 1) throw new IllegalArgumentException("Solver bounds");
        this.maxNodes = maxNodes; this.maxWork = maxWork;
    }

    public Solution<K> solve(Map<K, Double> fixed, Set<K> denied, List<Rule<K>> rules) {
        if (fixed.size() > maxNodes || rules.size() > 32768 || denied.size() > maxNodes)
            throw new IllegalArgumentException("Catalog bounds");
        Map<K, Value<K>> values = new HashMap<>();
        for (var row : fixed.entrySet()) {
            if (row.getKey() == null || !valid(row.getValue()) || denied.contains(row.getKey()))
                throw new IllegalArgumentException("Reference quote");
            values.put(row.getKey(), new Value<>(row.getValue(), Set.of(row.getKey())));
        }
        Map<K, List<Integer>> dependents = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        boolean[] queued = new boolean[rules.size()];
        int work = 0;
        for (int i = 0; i < rules.size(); i++) {
            var rule = rules.get(i);
            if (fixed.containsKey(rule.output()) || denied.contains(rule.output())) continue;
            Set<K> dependencies = new HashSet<>();
            for (var slot : rule.slots()) for (var choice : slot) {
                if (++work > maxWork) return result(values, fixed.size(), work, false);
                dependencies.add(choice.consumed());
                if (choice.returned() != null) dependencies.add(choice.returned());
            }
            for (var key : dependencies) dependents.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i);
            queue.add(i); queued[i] = true;
        }
        while (!queue.isEmpty()) {
            int id = queue.remove(); queued[id] = false;
            var rule = rules.get(id);
            double total = rule.overhead();
            Set<K> ancestry = new HashSet<>();
            boolean possible = true;
            for (var slot : rule.slots()) {
                double best = Double.POSITIVE_INFINITY;
                Set<K> chosen = Set.of();
                for (var choice : slot) {
                    if (++work > maxWork) return result(values, fixed.size(), work, false);
                    var consumed = values.get(choice.consumed());
                    var returned = choice.returned() == null ? null : values.get(choice.returned());
                    if (consumed == null || choice.returned() != null && returned == null
                            || consumed.ancestors().contains(rule.output())
                            || returned != null && returned.ancestors().contains(rule.output())) continue;
                    double amount = consumed.amount() - (returned == null ? 0 : returned.amount());
                    if (!Double.isFinite(amount) || amount < 0 || amount >= best) continue;
                    var dependencies = new HashSet<>(consumed.ancestors());
                    if (returned != null) dependencies.addAll(returned.ancestors());
                    if (dependencies.size() > 256) continue;
                    best = amount; chosen = dependencies;
                }
                if (!Double.isFinite(best)) { possible = false; break; }
                total += best; ancestry.addAll(chosen);
                if (ancestry.size() > 256) { possible = false; break; }
            }
            double price = total / rule.outputCount();
            if (!possible || !valid(price)) continue;
            var old = values.get(rule.output());
            if (old != null && price >= old.amount()) continue;
            if (old == null && values.size() == maxNodes) return result(values, fixed.size(), work, false);
            ancestry.add(rule.output());
            values.put(rule.output(), new Value<>(price, Set.copyOf(ancestry)));
            for (int dependent : dependents.getOrDefault(rule.output(), List.of())) {
                if (!queued[dependent]) { queue.add(dependent); queued[dependent] = true; }
            }
        }
        return result(values, fixed.size(), work, true);
    }

    private static boolean valid(double value) {
        return Double.isFinite(value) && value > 0 && value < Long.MAX_VALUE / 1000.0;
    }
    private Solution<K> result(Map<K, Value<K>> values, int fixed, int work, boolean complete) {
        Map<K, Double> result = new HashMap<>();
        values.forEach((key, value) -> result.put(key, value.amount()));
        return new Solution<>(Map.copyOf(result), values.size() - fixed, work, complete);
    }
}