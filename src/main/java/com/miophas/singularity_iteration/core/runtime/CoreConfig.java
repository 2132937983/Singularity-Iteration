package com.miophas.singularity_iteration.core.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class CoreConfig {

    private static final String RESOURCE_PATH = "/assets/mio_icif/config/core.ini";
    private static final Map<String, Section> sections = new LinkedHashMap<>();
    private static volatile boolean loaded = false;

    private CoreConfig() { }

    public static synchronized void load() {
        if (loaded) return;
        try (InputStream in = CoreConfig.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) throw new IllegalStateException("Core config resource not found: " + RESOURCE_PATH);
            parse(in);
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load core config", e);
        }
    }

    public static boolean getBool(String section, String key, boolean defaultValue) {
        load();
        Section s = sections.get(section);
        if (s == null) return defaultValue;
        String raw = s.entries.get(key);
        if (raw == null) return defaultValue;
        return parseBool(raw.trim(), defaultValue);
    }

    public static int getInt(String section, String key, int defaultValue) {
        load();
        Section s = sections.get(section);
        if (s == null) return defaultValue;
        String raw = s.entries.get(key);
        if (raw == null) return defaultValue;
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    public static double getDouble(String section, String key, double defaultValue) {
        load();
        Section s = sections.get(section);
        if (s == null) return defaultValue;
        String raw = s.entries.get(key);
        if (raw == null) return defaultValue;
        try { return Double.parseDouble(raw.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    public static String getString(String section, String key, String defaultValue) {
        load();
        Section s = sections.get(section);
        if (s == null) return defaultValue;
        return s.entries.getOrDefault(key, defaultValue);
    }

    public static Set<String> sectionNames() {
        load();
        return Collections.unmodifiableSet(sections.keySet());
    }

    public static void overrideBool(String section, String key, boolean value) {
        load();
        sections.computeIfAbsent(section, s -> new Section()).entries.put(key, String.valueOf(value));
    }

    private static void parse(InputStream in) throws IOException {
        sections.clear();
        String currentSection = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();
                    sections.computeIfAbsent(currentSection, s -> new Section());
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                sections.computeIfAbsent(currentSection, s -> new Section()).entries.put(key, value);
            }
        }
    }

    private static boolean parseBool(String raw, boolean defaultValue) {
        if (raw.equalsIgnoreCase("true") || raw.equals("1")) return true;
        if (raw.equalsIgnoreCase("false") || raw.equals("0")) return false;
        return defaultValue;
    }

    private static final class Section {
        final Map<String, String> entries = new LinkedHashMap<>();
    }
}