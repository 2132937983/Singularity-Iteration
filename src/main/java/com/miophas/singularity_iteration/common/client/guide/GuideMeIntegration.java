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
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import guideme.document.LytErrorSink;
import guideme.document.block.LytParagraph;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import guideme.libs.unist.UnistNode;
import guideme.scene.GuidebookScene;
import guideme.scene.element.SceneElementTagCompiler;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
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
 * <p>0.1.10.19: {@code <SiStructure id="ns:path"/>} inside a {@code <GameScene>} places the blocks of a
 * registered {@link StructureBlueprint} (GuideME then turns, zooms and names the blocks), and
 * {@code <SiStructureInfo id="ns:path"/>} lists its materials and rules. Addon guides can use both tags for
 * their own blueprints.
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
            .extension(SceneElementTagCompiler.EXTENSION_POINT, new StructureSceneElement())
            .extension(TagCompiler.EXTENSION_POINT, new StructureInfoTag())
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

    /** The blueprint of attribute {@code id}, or null after an error on the page. */
    static StructureBlueprint blueprint(PageCompiler compiler, LytErrorSink errors, MdxJsxElementFields el) {
        String raw = el.getAttributeString("id", "").trim();
        ResourceLocation id = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
        StructureBlueprint blueprint = id == null ? null : StructureBlueprintRegistry.get(id);
        if (blueprint == null) errors.appendError(compiler, "Unknown structure: " + raw, (UnistNode) el);
        return blueprint;
    }

    /**
     * {@code <SiStructure id="mio_icif:fluid_reactor" x="0" y="0" z="0"/>} in a {@code <GameScene>}: the first
     * permitted block of each position, moved by x/y/z.
     */
    static final class StructureSceneElement implements SceneElementTagCompiler {
        @Override
        public Set<String> getTagNames() {
            return Set.of("SiStructure");
        }

        @Override
        public void compile(GuidebookScene scene, PageCompiler compiler, LytErrorSink errors, MdxJsxElementFields el) {
            StructureBlueprint blueprint = blueprint(compiler, errors, el);
            if (blueprint == null) return;
            BlockPos origin;
            try {
                origin = new BlockPos(Integer.parseInt(el.getAttributeString("x", "0").trim()),
                    Integer.parseInt(el.getAttributeString("y", "0").trim()), Integer.parseInt(el.getAttributeString("z", "0").trim()));
            } catch (NumberFormatException e) {
                errors.appendError(compiler, "x, y and z must be numbers", (UnistNode) el);
                return;
            }
            for (StructureBlueprint.Placement placement : blueprint.placements()) {
                for (StructureBlueprint.BlockRef ref : placement.key().blocks()) {
                    BlockState state = ref.resolve();
                    if (state == null) continue;
                    scene.getLevel().setBlockAndUpdate(origin.offset(placement.pos()), state);
                    break;
                }
            }
        }
    }

    /** {@code <SiStructureInfo id="mio_icif:fluid_reactor"/>}: one line per material (count, blocks, rule), then the notes. */
    static final class StructureInfoTag implements TagCompiler {
        @Override
        public Set<String> getTagNames() {
            return Set.of("SiStructureInfo");
        }

        @Override
        public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
            StructureBlueprint blueprint = blueprint(compiler, parent, el);
            if (blueprint == null) return;
            for (Map.Entry<StructureBlueprint.Key, Integer> entry : blueprint.counts().entrySet()) {
                StringBuilder names = new StringBuilder();
                for (StructureBlueprint.BlockRef ref : entry.getKey().blocks()) {
                    BlockState state = ref.resolve();
                    if (state == null) continue;
                    if (!names.isEmpty()) names.append(" / ");
                    names.append(state.getBlock().getName().getString());
                }
                if (names.isEmpty()) continue;
                String line = entry.getValue() + " × " + names;
                String note = entry.getKey().noteKey();
                if (note != null) line += " — " + Component.translatable(note).getString();
                parent.append(LytParagraph.of(line));
            }
            for (String note : blueprint.noteKeys()) parent.append(LytParagraph.of(Component.translatable(note).getString()));
        }
    }
}
