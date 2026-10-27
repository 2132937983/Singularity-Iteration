// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 0.1.10.29: the number field of the exchange panel and the Trade Terminal. The vanilla field it replaces kept the old digits
 * when clicked (the cursor went to the click) and had a few digits of room, so typing into a full field did nothing; the
 * panels also took the focus away on every button click. This field:
 * <ul>
 *   <li>selects its digits when a click gives it the focus, so typing replaces the number;</li>
 *   <li>has room for every digit of its maximum and clips to the maximum as it is typed (the panel shows what it sends);</li>
 *   <li>takes full-width digits (an IME) as digits;</li>
 *   <li>steps with the mouse wheel and the arrow keys (Shift x64, Ctrl x10) and with {@link #step} for the panels' +/- buttons.</li>
 * </ul>
 * {@link #value} reads the number clamped to the range.
 */
public class NumberBox extends EditBox {
    private long min, max;

    public NumberBox(Font font, long min, long max, long value) {
        super(font, 0, 0, 50, 12, Component.empty());
        setRange(min, max);
        setFilter(s -> s.chars().allMatch(c -> c >= '0' && c <= '9'));
        setValue(Long.toString(clamp(value)));
        setBordered(true);
    }

    /** Changes the range (for example coins or percent); the number is clipped to the new maximum. */
    public void setRange(long min, long max) {
        if (Math.max(0, min) == this.min && Math.max(this.min, max) == this.max && getValue() != null && !getValue().isEmpty()) return;
        this.min = Math.max(0, min);
        this.max = Math.max(this.min, max);
        // clip first: a shorter maximum would otherwise cut the digits off (12345 -> "123" -> 100 instead of 100 directly)
        String v = getValue();
        if (v != null && !v.isEmpty() && parse(v, this.min) > this.max) setValue(Long.toString(this.max));
        setMaxLength(Long.toString(this.max).length());
    }

    public long min() { return min; }

    public long max() { return max; }

    /** The number, clamped to the range; {@code min} when the field is empty. */
    public long value() {
        return clamp(parse(getValue(), min));
    }

    /** Sets the number (clamped). */
    public void set(long v) {
        setValue(Long.toString(clamp(v)));
    }

    /** Adds {@code steps} (negative to subtract), times 64 with Shift, 10 with Ctrl. */
    public void step(int steps) {
        long unit = Screen.hasShiftDown() ? 64 : Screen.hasControlDown() ? 10 : 1;
        long base = parse(getValue(), min);
        long next = steps >= 0 ? (base > max - unit * steps ? max : base + unit * steps) : base + unit * steps;
        set(next);
    }

    long clamp(long v) {
        return Math.max(min, Math.min(max, v));
    }

    static long parse(String s, long fallback) {
        if (s == null || s.isEmpty()) return fallback;
        long v = 0;
        for (int i = 0; i < s.length(); i++) {
            int d = Character.digit(s.charAt(i), 10);
            if (d < 0) return fallback;
            if (v > (Long.MAX_VALUE - d) / 10) return Long.MAX_VALUE;
            v = v * 10 + d;
        }
        return v;
    }

    /** ASCII digits of {@code text}: full-width and other Unicode digits become 0-9, anything else is dropped. */
    static String digits(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            int d = Character.digit(text.charAt(i), 10);
            if (d >= 0) out.append((char) ('0' + d));
        }
        return out.toString();
    }

    @Override
    public void insertText(String text) {
        super.insertText(digits(text));
        // clip to the maximum while typing (the minimum only when read: an empty or short number is still being typed)
        String v = getValue();
        if (!v.isEmpty() && parse(v, min) > max) setValue(Long.toString(max));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        boolean fresh = !isFocused();
        super.onClick(mouseX, mouseY);
        if (fresh) selectAll();
    }

    /** Selects the whole number (typing replaces it). */
    public void selectAll() {
        moveCursorToEnd(false);
        setHighlightPos(0);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (canConsumeInput() && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
            step(key == GLFW.GLFW_KEY_UP ? 1 : -1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isVisible() || !isActive() || !isMouseOver(mouseX, mouseY) || scrollY == 0) return false;
        step(scrollY > 0 ? 1 : -1);
        return true;
    }
}
