// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 0.1.10.25 (X1): finds ways to earn coins by buying from the market, processing and selling back to the market, and recipe
 * cycles that give back more of an item than they take. Pure (the command {@code /mio_icif market arbitrage} feeds it the
 * recipes of the server; the offline harness feeds it test recipes).
 *
 * <ul>
 *   <li><b>Cost</b>: every market good costs its ask; every other item the cheapest recipe that makes it from market goods
 *       (relaxation, at most {@link #PASSES} passes). The energy (EU) along the cheapest path is carried along.</li>
 *   <li><b>Loop</b>: a market good whose bid (no sell pressure) is above the cost of making it by a recipe. Energy is not
 *       priced; the report gives the EU per coin of profit, so production with a lot of energy can be told apart from a
 *       real gap in the prices.</li>
 *   <li><b>Cycle</b>: with every item no recipe makes costing 1, an item whose cost still falls after many passes lies on
 *       a cycle that multiplies it (for example 1 ingot, 1 plate, 2 casings, 2 cans, 2 dusts, 2 ingots).</li>
 * </ul>
 */
public final class ArbitrageAudit {
    private ArbitrageAudit() {}

    public static final int PASSES = 40;
    /** A cycle cost below this (from 1 per base input) can only come from a cycle that multiplies its items; it stops there. */
    private static final double COLLAPSED = 1e-200;

    /** One recipe: {@code inputs} holds one set of accepted items per consumed item. */
    public record Recipe<T>(String id, T output, int count, List<Set<T>> inputs, double extraCost, double energy) { }

    /** A market loop. {@code inputs}: the cheapest input of each slot of the recipe. */
    public record Loop<T>(T item, double bid, double cost, double energy, String via, List<T> inputs) {
        public double profit() { return bid - cost; }

        /** EU per coin of profit (0 when no energy is needed). */
        public double energyPerCoin() { return energy <= 0 ? 0 : energy / profit(); }
    }

    /** Cheapest cost, energy and recipe of every item. */
    public record Costs<T>(Map<T, Double> cost, Map<T, Double> energy, Map<T, String> via) { }

    public static <T> Costs<T> costs(Map<T, Double> ask, List<Recipe<T>> recipes) {
        Map<T, Double> cost = new HashMap<>(ask);
        Map<T, Double> energy = new HashMap<>();
        Map<T, String> via = new HashMap<>();
        for (int pass = 0; pass < PASSES; pass++) {
            boolean changed = false;
            for (Recipe<T> r : recipes) {
                if (r.count() <= 0 || r.inputs().isEmpty()) continue;
                double total = r.extraCost(), eu = r.energy();
                for (Set<T> slot : r.inputs()) {
                    T pick = cheapest(slot, cost);
                    double c = pick == null ? Double.POSITIVE_INFINITY : cost.get(pick);
                    total += c;
                    if (pick != null) eu += energy.getOrDefault(pick, 0.0);
                    if (Double.isInfinite(total)) break;
                }
                if (Double.isInfinite(total)) continue;
                double each = total / r.count();
                if (each < cost.getOrDefault(r.output(), Double.POSITIVE_INFINITY) - 1e-9) {
                    cost.put(r.output(), each);
                    energy.put(r.output(), eu / r.count());
                    via.put(r.output(), r.id());
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return new Costs<>(cost, energy, via);
    }

    /** Market goods whose bid is above the cost of making them by a recipe; the most profitable first. */
    public static <T> List<Loop<T>> loops(Map<T, Double> ask, Map<T, Double> bid, List<Recipe<T>> recipes) {
        Costs<T> costs = costs(ask, recipes);
        List<Loop<T>> out = new ArrayList<>();
        for (Map.Entry<T, Double> e : bid.entrySet()) {
            T item = e.getKey();
            double best = Double.POSITIVE_INFINITY, eu = 0;
            Recipe<T> bestRecipe = null;
            List<T> picks = List.of();
            for (Recipe<T> r : recipes) {
                if (!r.output().equals(item) || r.count() <= 0) continue;
                double total = r.extraCost(), energy = r.energy();
                List<T> chosen = new ArrayList<>();
                for (Set<T> slot : r.inputs()) {
                    T pick = cheapest(slot, costs.cost());
                    if (pick == null) { total = Double.POSITIVE_INFINITY; break; }
                    total += costs.cost().get(pick);
                    energy += costs.energy().getOrDefault(pick, 0.0);
                    chosen.add(pick);
                }
                if (total / r.count() < best) {
                    best = total / r.count();
                    eu = energy / r.count();
                    bestRecipe = r;
                    picks = chosen;
                }
            }
            if (bestRecipe != null && best < e.getValue() - 1e-9)
                out.add(new Loop<>(item, e.getValue(), best, eu, bestRecipe.id(), List.copyOf(picks)));
        }
        out.sort(Comparator.comparingDouble((Loop<T> l) -> -l.profit()));
        return out;
    }

    /** Items on recipe cycles that multiply them (fuel and energy ignored). */
    public static <T> Set<T> cycles(List<Recipe<T>> recipes) {
        Set<T> made = new HashSet<>();
        for (Recipe<T> r : recipes) made.add(r.output());
        Map<T, Double> cost = new HashMap<>();
        for (Recipe<T> r : recipes) for (Set<T> slot : r.inputs()) for (T i : slot) if (!made.contains(i)) cost.put(i, 1.0);
        Map<T, Double> before = null;
        Set<T> collapsed = new HashSet<>();
        for (int pass = 0; pass < 60; pass++) {
            if (pass == 49) before = new HashMap<>(cost);
            for (Recipe<T> r : recipes) {
                if (r.count() <= 0 || r.inputs().isEmpty()) continue;
                double total = 0;
                for (Set<T> slot : r.inputs()) {
                    T pick = cheapest(slot, cost);
                    total += pick == null ? Double.POSITIVE_INFINITY : cost.get(pick);
                    if (Double.isInfinite(total)) break;
                }
                double each = total / r.count();
                // 0.1.10.42: a relative step. The old absolute one (1e-15) froze the costs of a strong cycle (x10 a pass) after
                // about 15 passes, so pass 49 matched pass 60 and the cycle was not reported
                if (each < cost.getOrDefault(r.output(), Double.POSITIVE_INFINITY) * (1 - 1e-9)) {
                    if (each < COLLAPSED) collapsed.add(r.output());   // far below any real cost: only a multiplying cycle gets here
                    cost.put(r.output(), Math.max(each, COLLAPSED));
                }
            }
        }
        Set<T> falling = new HashSet<>(collapsed);
        for (Map.Entry<T, Double> e : cost.entrySet()) {
            Double then = before.get(e.getKey());
            if (then != null && e.getValue() < then * 0.999) falling.add(e.getKey());
        }
        return falling;
    }

    private static <T> T cheapest(Set<T> slot, Map<T, Double> cost) {
        T best = null;
        double c = Double.POSITIVE_INFINITY;
        for (T i : slot) {
            Double v = cost.get(i);
            if (v != null && v < c) { c = v; best = i; }
        }
        return best;
    }
}
