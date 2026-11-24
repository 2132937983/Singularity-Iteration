// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 0.1.11.125 (armour and flight overhaul E2): a small player for Bedrock-format bone animations (the
 * {@code animation.json} Blockbench writes, format 1.8.0), so the suit's own clips drive its baked model without GeckoLib.
 *
 * <p>Supported: per bone the channels {@code position}, {@code rotation} and {@code scale}; a channel is one constant
 * {@code [x, y, z]} or keyframes {@code {"time": [x, y, z]}} (also {@code {"time": {"post": [..]}}}), interpolated linearly;
 * a value may be a number or a comparison of the clip time, {@code "query.anim_time >= 0.5"} (also {@code > < <=}),
 * which is 1 or 0. {@code loop}: {@code true} loops, {@code "hold_on_last_frame"} holds, anything else plays once and
 * holds as well. Values stay in the file's units (pixels, degrees, Bedrock axes: +y up); the renderer converts them.
 */
public final class SuitAnimation {
    private SuitAnimation() {}

    public enum Loop { LOOP, HOLD }

    private static final byte OP_ZERO = 0, OP_CONST = 1, OP_GE = 2, OP_LE = 3, OP_GT = 4, OP_LT = 5;

    /** Pre-compiled expression: avoids per-frame String ops and NumberFormatException throws. */
    public record CompiledExpr(byte op, float value) {
        public float eval(float time) {
            return switch (op) {
                case OP_CONST -> value;
                case OP_GE -> time >= value ? 1F : 0F;
                case OP_LE -> time <= value ? 1F : 0F;
                case OP_GT -> time > value ? 1F : 0F;
                case OP_LT -> time < value ? 1F : 0F;
                default -> 0F;
            };
        }
    }

    /** One channel: key times (seconds, ascending) and per key three values, or three pre-compiled expressions. */
    public record Channel(float[] times, float[][] values, CompiledExpr[][] expressions) {
        /** The value at {@code time} (the clip's own time, already looped or held). */
        public float[] at(float time) {
            int n = times.length;
            if (n == 1 || time <= times[0]) return eval(0, time);
            if (time >= times[n - 1]) return eval(n - 1, time);
            int i = 0;
            while (i + 1 < n && times[i + 1] <= time) i++;
            float k = (time - times[i]) / (times[i + 1] - times[i]);
            float[] a = eval(i, time), b = eval(i + 1, time);
            a[0] += (b[0] - a[0]) * k;
            a[1] += (b[1] - a[1]) * k;
            a[2] += (b[2] - a[2]) * k;
            return a;
        }

        private float[] eval(int key, float time) {
            float[] v = values[key].clone();
            CompiledExpr[] e = expressions[key];
            if (e != null) for (int c = 0; c < 3; c++) if (e[c] != null) v[c] = e[c].eval(time);
            return v;
        }
    }

    /** One clip: its length in seconds, how it ends, and its channels by bone and channel name. */
    public record Clip(String name, float length, Loop loop, Map<String, Map<String, Channel>> bones) {
        /** The clip's own time at {@code t} seconds after it started. */
        public float localTime(double t) {
            if (t <= 0) return 0;
            if (loop == Loop.LOOP && length > 0) return (float) (t % length);
            return (float) Math.min(t, length);
        }

        /** The channel's value {@code t} seconds after the clip started, or null when the clip does not move it. */
        public float[] sample(String bone, String channel, double t) {
            Map<String, Channel> b = bones.get(bone);
            Channel c = b == null ? null : b.get(channel);
            return c == null ? null : c.at(localTime(t));
        }

        public boolean moves(String bone) { return bones.containsKey(bone); }
    }

    /** Reads the {@code animations} of an animation file, by clip name. */
    public static Map<String, Clip> parse(JsonObject root) {
        Map<String, Clip> out = new HashMap<>();
        JsonObject anims = root.getAsJsonObject("animations");
        if (anims == null) return out;
        for (var e : anims.entrySet()) {
            JsonObject a = e.getValue().getAsJsonObject();
            float length = a.has("animation_length") ? a.get("animation_length").getAsFloat() : 0F;
            JsonElement l = a.get("loop");
            Loop loop = l != null && l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean() && l.getAsBoolean() ? Loop.LOOP : Loop.HOLD;
            Map<String, Map<String, Channel>> bones = new HashMap<>();
            JsonObject b = a.getAsJsonObject("bones");
            if (b != null) for (var be : b.entrySet()) {
                Map<String, Channel> channels = new HashMap<>();
                for (var ce : be.getValue().getAsJsonObject().entrySet()) channels.put(ce.getKey(), channel(ce.getValue()));
                bones.put(be.getKey(), channels);
            }
            out.put(e.getKey(), new Clip(e.getKey(), length, loop, bones));
        }
        return out;
    }

    private static Channel channel(JsonElement el) {
        TreeMap<Float, JsonArray> keys = new TreeMap<>();
        if (el.isJsonArray()) keys.put(0F, el.getAsJsonArray());
        else for (var k : el.getAsJsonObject().entrySet()) {
            JsonElement v = k.getValue();
            if (v.isJsonObject()) v = v.getAsJsonObject().has("post") ? v.getAsJsonObject().get("post") : v.getAsJsonObject().get("pre");
            keys.put(Float.parseFloat(k.getKey()), v.getAsJsonArray());
        }
        int n = keys.size(), i = 0;
        float[] times = new float[n];
        float[][] values = new float[n][3];
        CompiledExpr[][] expressions = new CompiledExpr[n][];
        for (var k : keys.entrySet()) {
            times[i] = k.getKey();
            JsonArray arr = k.getValue();
            for (int c = 0; c < 3; c++) {
                JsonElement v = arr.get(Math.min(c, arr.size() - 1));
                if (v.getAsJsonPrimitive().isNumber()) values[i][c] = v.getAsFloat();
                else {
                    CompiledExpr compiled = compileExpression(v.getAsString());
                    if (compiled.op() == OP_CONST) values[i][c] = compiled.value();
                    else if (compiled.op() != OP_ZERO) {
                        if (expressions[i] == null) expressions[i] = new CompiledExpr[3];
                        expressions[i][c] = compiled;
                    }
                }
            }
            i++;
        }
        return new Channel(times, values, expressions);
    }

    private static CompiledExpr compileExpression(String e) {
        String s = e.replace(" ", "");
        if (!s.isEmpty() && !s.startsWith("query.")) {
            try { return new CompiledExpr(OP_CONST, Float.parseFloat(s)); } catch (NumberFormatException ignored) { }
        }
        String q = "query.anim_time";
        if (!s.startsWith(q)) return new CompiledExpr(OP_ZERO, 0F);
        String rest = s.substring(q.length());
        byte op;
        int opLen;
        if (rest.startsWith(">=")) { op = OP_GE; opLen = 2; }
        else if (rest.startsWith("<=")) { op = OP_LE; opLen = 2; }
        else if (rest.startsWith(">")) { op = OP_GT; opLen = 1; }
        else if (rest.startsWith("<")) { op = OP_LT; opLen = 1; }
        else return new CompiledExpr(OP_ZERO, 0F);
        try { return new CompiledExpr(op, Float.parseFloat(rest.substring(opLen))); }
        catch (NumberFormatException ex) { return new CompiledExpr(OP_ZERO, 0F); }
    }

    /** A number, or {@code query.anim_time <op> number} as 1 or 0. Anything else is 0. */
    static float expression(String e, float time) {
        return compileExpression(e).eval(time);
    }
}
