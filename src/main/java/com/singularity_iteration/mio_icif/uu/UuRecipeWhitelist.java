package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UuRecipeWhitelist {
    private static Set<ResourceLocation> allowedTypes = Collections.emptySet();
    private static boolean loaded = false;

    public static void load() {
        Set<ResourceLocation> types = new HashSet<>();

        try (InputStream is = Singularity_Iteration.class.getResourceAsStream("/assets/mio_icif/config/uu_recipe_resolvers.ini")) {
            if (is == null) {
                Singularity_Iteration.LOGGER.warn("[UU] uu_recipe_resolvers.ini not found, no recipe types will be allowed!");
                allowedTypes = Collections.emptySet();
                loaded = true;
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";")) continue;
                try {
                    ResourceLocation rl = ResourceLocation.parse(line);
                    types.add(rl);
                } catch (Exception e) {
                    Singularity_Iteration.LOGGER.warn("[UU] Invalid recipe type in whitelist: '{}'", line);
                }
            }
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("[UU] Failed to load uu_recipe_resolvers.ini", e);
        }

        allowedTypes = Collections.unmodifiableSet(types);
        loaded = true;
        Singularity_Iteration.LOGGER.info("[UU] Loaded recipe type whitelist: {} entries - {}", types.size(), types);
    }

    public static boolean isAllowed(RecipeType<?> recipeType) {
        if (!loaded) load();

        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(recipeType);
        if (key == null) return false;
        return allowedTypes.contains(key);
    }

    public static boolean isAllowed(ResourceLocation recipeTypeKey) {
        if (!loaded) load();
        return allowedTypes.contains(recipeTypeKey);
    }

    public static Set<ResourceLocation> getAllowedTypes() {
        if (!loaded) load();
        return allowedTypes;
    }

    public static void reset() {
        loaded = false;
        allowedTypes = Collections.emptySet();
    }
}