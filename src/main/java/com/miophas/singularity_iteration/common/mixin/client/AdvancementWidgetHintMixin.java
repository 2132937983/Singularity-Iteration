package com.miophas.singularity_iteration.common.mixin.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows a different text before and after an advancement is completed.
 * <p>
 * Vanilla bakes {@code display.description} into a fixed {@code description} field when the widget
 * is created, so the same text is shown whether or not the advancement is done. This swaps the text
 * at draw time:
 * <ul>
 *   <li>not completed yet -> {@code advancements.<namespace>.<path>.hint} (tells you what to do)</li>
 *   <li>completed -> the original {@code display.description}</li>
 * </ul>
 * Advancements without a {@code .hint} translation simply keep showing their original description.
 */
@Mixin(AdvancementWidget.class)
@SuppressWarnings("null")
public abstract class AdvancementWidgetHintMixin {

    @Shadow
    @Final
    @Mutable
    private List<FormattedCharSequence> description;

    @Shadow
    @Final
    private AdvancementNode advancementNode;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private int width;

    @Shadow
    private AdvancementProgress progress;

    /** The description baked in by the constructor, kept so it can be restored after completion. */
    @Unique
    private List<FormattedCharSequence> si$originalDescription;

    /** Lazily built hint lines; falls back to the original description when no hint is translated. */
    @Unique
    private List<FormattedCharSequence> si$hintLines;

    @Inject(method = "drawHover", at = @At("HEAD"))
    private void si$swapHintText(GuiGraphics guiGraphics, int x, int y, float fade, int width, int height, CallbackInfo ci) {
        if (this.si$originalDescription == null) {
            this.si$originalDescription = this.description;
        }

        this.description = this.si$isCompleted()
            ? this.si$originalDescription
            : this.si$hintOr(this.si$originalDescription);
    }

    @Unique
    private boolean si$isCompleted() {
        return this.progress != null && this.progress.isDone();
    }

    @Unique
    private List<FormattedCharSequence> si$hintOr(List<FormattedCharSequence> fallback) {
        if (this.si$hintLines == null) {
            ResourceLocation id = this.advancementNode.holder().id();
            String key = "advancements." + id.getNamespace() + "." + id.getPath() + ".hint";
            Component hint = Component.translatable(key);
            this.si$hintLines = hint.getString().equals(key)
                ? fallback
                : this.minecraft.font.split(
                    hint.copy().withStyle(ChatFormatting.YELLOW),
                    Math.max(60, this.width - 12)
                );
        }

        return this.si$hintLines;
    }
}
