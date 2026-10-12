// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

/**
 * Client bridge to <b>SlashBlade: Resharped</b> (0.1.8.0). Only this fork is supported: mod id
 * {@code slashblade} with the display name "Slash Blade:Resharpened" (2.x for 1.21.1, checked
 * against 2.1.9). Other SlashBlade builds that use the same mod id do not match.
 *
 * <p>Resharped draws nothing for the hand display contexts: a held blade comes from its own
 * player layer. The Armory flight therefore draws the "blade" part of the blade OBJ model
 * directly, with the texture and model of the blade state, as the player layer does. There is
 * no compile dependency: the method handles are found once, on first use. If a lookup fails
 * (an API change in a later Resharped version), the bridge turns off, writes one log line, and
 * the piece flies as a normal item.
 */
@OnlyIn(Dist.CLIENT)
final class SlashBladeResharpedBridge {
    static final String MOD_ID = "slashblade";
    /** Text in the display name of the Resharped fork ("Slash Blade:Resharpened"). */
    private static final String FORK_MARK = "resharp";

    /** Model scale: blade.obj is about 333 units long; this gives a blade of about 1.15 blocks. */
    static final float MODEL_SCALE = 0.0035F;
    /** Grip centre on the model x axis (the tip is at -x, the pommel at +x). */
    static final float GRIP_X = 8.0F;

    private static final int UNKNOWN = 0, READY = 1, OFF = -1;
    private static volatile int state = UNKNOWN;

    private static Class<?> bladeItem;
    private static MethodHandle stateOf, getModel, getTexture, isBroken;
    private static MethodHandle modelManager, getObj, renderSolid, renderLuminous, resetCol;
    private static ResourceLocation defaultModel, defaultTexture;

    private SlashBladeResharpedBridge() {}

    /** True for a blade item of SlashBlade: Resharped (its addons included). */
    static boolean isBlade(ItemStack stack) {
        return !stack.isEmpty() && ready() && bladeItem.isInstance(stack.getItem());
    }

    /**
     * Draws the drawn blade (no sheath) of {@code stack}. The blade lies on the model x axis:
     * the tip at -x. The caller turns and scales the pose. Returns false if nothing was drawn.
     */
    static boolean renderBlade(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ready()) return false;
        try {
            ResourceLocation model = defaultModel, texture = defaultTexture;
            boolean broken = false;
            Object blade = ((Optional<?>) stateOf.invoke(stack)).orElse(null);
            if (blade != null) {
                model = ((Optional<?>) getModel.invoke(blade)).map(ResourceLocation.class::cast).orElse(defaultModel);
                texture = ((Optional<?>) getTexture.invoke(blade)).map(ResourceLocation.class::cast).orElse(defaultTexture);
                broken = (boolean) isBroken.invoke(blade);
            }
            Object obj = getObj.invoke(modelManager.invoke(), model);
            if (obj == null) return false;
            resetCol.invoke();
            renderSolid.invoke(stack, obj, broken ? "blade_damaged" : "blade", texture, pose, buffers, light);
            renderLuminous.invoke(stack, obj, broken ? "blade_damaged_luminous" : "blade_luminous", texture, pose, buffers, light);
            return true;
        } catch (Throwable t) {
            turnOff("render", t);
            return false;
        }
    }

    private static boolean ready() {
        int s = state;
        if (s == UNKNOWN) s = init();
        return s == READY;
    }

    private static synchronized int init() {
        if (state != UNKNOWN) return state;
        boolean resharped = ModList.get().getModContainerById(MOD_ID)
            .map(c -> c.getModInfo().getDisplayName().toLowerCase(java.util.Locale.ROOT).contains(FORK_MARK))
            .orElse(false);
        if (!resharped) return state = OFF;
        try {
            ClassLoader cl = SlashBladeResharpedBridge.class.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> access = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess", false, cl);
            Class<?> bladeState = Class.forName("mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState", false, cl);
            Class<?> manager = Class.forName("mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager", false, cl);
            Class<?> obj = Class.forName("mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject", false, cl);
            Class<?> render = Class.forName("mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState", false, cl);
            Class<?> resources = Class.forName("mods.flammpfeil.slashblade.init.DefaultResources", false, cl);
            bladeItem = Class.forName("mods.flammpfeil.slashblade.item.ItemSlashBlade", false, cl);

            stateOf = lookup.findStatic(access, "of", MethodType.methodType(Optional.class, ItemStack.class));
            getModel = lookup.findVirtual(bladeState, "getModel", MethodType.methodType(Optional.class));
            getTexture = lookup.findVirtual(bladeState, "getTexture", MethodType.methodType(Optional.class));
            isBroken = lookup.findVirtual(bladeState, "isBroken", MethodType.methodType(boolean.class));
            modelManager = lookup.findStatic(manager, "getInstance", MethodType.methodType(manager));
            getObj = lookup.findVirtual(manager, "getModel", MethodType.methodType(obj, ResourceLocation.class));
            MethodType part = MethodType.methodType(void.class, ItemStack.class, obj, String.class, ResourceLocation.class,
                PoseStack.class, MultiBufferSource.class, int.class);
            renderSolid = lookup.findStatic(render, "renderOverrided", part);
            renderLuminous = lookup.findStatic(render, "renderOverridedLuminous", part);
            resetCol = lookup.findStatic(render, "resetCol", MethodType.methodType(void.class));
            defaultModel = (ResourceLocation) lookup.findStaticGetter(resources, "resourceDefaultModel", ResourceLocation.class).invoke();
            defaultTexture = (ResourceLocation) lookup.findStaticGetter(resources, "resourceDefaultTexture", ResourceLocation.class).invoke();
            Singularity_Iteration.LOGGER.info("Armory: SlashBlade: Resharped found, blades fly in drawn");
            return state = READY;
        } catch (Throwable t) {
            turnOff("link", t);
            return OFF;
        }
    }

    private static void turnOff(String step, Throwable t) {
        if (state == OFF) return;
        state = OFF;
        Singularity_Iteration.LOGGER.warn("Armory: SlashBlade: Resharped {} failed, blades fly as items ({})", step, t.toString());
    }
}
