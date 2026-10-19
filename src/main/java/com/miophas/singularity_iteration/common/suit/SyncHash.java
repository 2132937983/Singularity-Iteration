// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.8: the hash of {@code java.util.Objects.hash(...)} without the varargs array and the boxing
 * of every value. {@code add} gives the same values: start with {@link #START}, then add the values in
 * the same order (each primitive hashes as its box does).
 */
public final class SyncHash {
    private SyncHash() { }

    public static final int START = 1;

    public static int add(int hash, boolean value) { return 31 * hash + Boolean.hashCode(value); }

    public static int add(int hash, int value) { return 31 * hash + Integer.hashCode(value); }

    public static int add(int hash, long value) { return 31 * hash + Long.hashCode(value); }

    public static int add(int hash, float value) { return 31 * hash + Float.hashCode(value); }

    public static int add(int hash, double value) { return 31 * hash + Double.hashCode(value); }

    public static int add(int hash, @Nullable Object value) { return 31 * hash + (value == null ? 0 : value.hashCode()); }
}
