// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.guide;

import com.miophas.singularity_iteration.common.guide.GuideBook;
import guideme.Guide;
import guideme.GuideItemSettings;
import guideme.compiler.PageCompiler;
import guideme.compiler.TagCompiler;
import guideme.document.block.LytBlockContainer;
import guideme.document.interaction.LytWidget;
import guideme.libs.mdast.mdx.model.MdxJsxFlowElement;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GuideME integration (0.1.8.0). Before 0.1.8.0 the manual was a data-driven guide
 * ({@code guideme_guides/manual.json}); a data-driven guide cannot have its own tags. The manual
 * is now built in code with the same id, folder and start page, plus the
 * {@code <ReactorSimulator chambers="N" preset="..."/>} tag.
 *
 * <p>Only {@code Singularity_Iteration_Client} calls {@link #register}, and only when GuideME is
 * loaded, so no GuideME class loads without GuideME.
 */
@OnlyIn(Dist.CLIENT)
public final class GuideMeIntegration {
    private GuideMeIntegration() {}

    public static void register() {
        Guide.builder(GuideBook.GUIDE_ID)
            .itemSettings(new GuideItemSettings(Optional.of(Component.translatable("item.mio_icif.guide")),
                List.of(Component.literal("Singularity Iteration").withStyle(ChatFormatting.DARK_AQUA)), Optional.empty()))
            .extension(TagCompiler.EXTENSION_POINT, new ReactorSimulatorTag())
            .build();
    }

    /** {@code <ReactorSimulator chambers="6" preset="rows/separated/by/slash"/>}: the interactive simulator. */
    static final class ReactorSimulatorTag implements TagCompiler {
        @Override
        public Set<String> getTagNames() {
            return Set.of("ReactorSimulator");
        }

        @Override
        public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
            int chambers;
            try {
                chambers = Integer.parseInt(el.getAttributeString("chambers", "6").trim());
            } catch (NumberFormatException e) {
                parent.appendError(compiler, "chambers must be a number 0..6", el);
                return;
            }
            String preset = el.getAttributeString("preset", "");
            parent.append(new LytWidget(new ReactorSimulatorWidget(3 + Math.max(0, Math.min(6, chambers)), preset)));
        }
    }
}
