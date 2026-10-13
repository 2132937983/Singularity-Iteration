// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Geometry of one frame core attachment (0.1.9.0): the 3D exterior of a core on its suit piece,
 * {@code assets/mio_icif/armor_models/attachments/<core>_<piece>.json}, made by
 * {@code tools/gen_core_attachments.py} from the Blockbench models of the retired items.
 *
 * <p>An attachment is a list of segments. Each segment belongs to one humanoid part (head, body,
 * left or right arm), may be emissive, may have an additive glow pass, and may move: deploy
 * (between a closed and an open angle), spin, or sweep (a slow swing), about an axis through a
 * pivot in part space.
 */
public final class CoreAttachmentModel {
    private static final Map<String, CoreAttachmentModel> CACHE = new HashMap<>();
    private static final CoreAttachmentModel EMPTY = new CoreAttachmentModel(null, List.of());

    public enum Part { HEAD, BODY, LEFT_ARM, RIGHT_ARM }

    public enum Glow { NONE, SOLAR, THRUST, PULSE, IDLE }

    public enum Drive { SOLAR, THRUST, ACTIVE }

    /** Movement of a segment. Angles in degrees; pivot in part space pixels. */
    public record Anim(String type, float ax, float ay, float az, float px, float py, float pz,
                       float open, float closed, float speed, float amplitude, float period, Drive drive) { }

    public record Segment(String name, Part part, boolean emissive, Glow glow, @Nullable Anim anim, float[][] quads) { }

    @Nullable private final ResourceLocation texture;
    private final List<Segment> segments;

    private CoreAttachmentModel(@Nullable ResourceLocation texture, List<Segment> segments) {
        this.texture = texture;
        this.segments = segments;
    }

    @Nullable public ResourceLocation texture() { return texture; }
    public List<Segment> segments() { return segments; }
    public boolean isEmpty() { return texture == null || segments.isEmpty(); }

    /** The attachment of {@code id} ({@code <core>_<piece>}); EMPTY when the file is missing. */
    public static synchronized CoreAttachmentModel get(String id) {
        return CACHE.computeIfAbsent(id, CoreAttachmentModel::load);
    }

    /** Resource reload: read the files again on next use. */
    public static synchronized void invalidate() {
        CACHE.clear();
    }

    private static CoreAttachmentModel load(String id) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "armor_models/attachments/" + id + ".json");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return EMPTY;
        try (Reader reader = resource.get().openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            Singularity_Iteration.LOGGER.error("Cannot read core attachment {}", file, e);
            return EMPTY;
        }
    }

    /** JSON -> model. Public for the GameTests (they check the shipped files). */
    public static CoreAttachmentModel parse(JsonObject root) {
        ResourceLocation texture = ResourceLocation.parse(root.get("texture").getAsString());
        List<Segment> segments = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("segments")) {
            JsonObject s = el.getAsJsonObject();
            Part part = switch (s.get("part").getAsString()) {
                case "head" -> Part.HEAD;
                case "left_arm" -> Part.LEFT_ARM;
                case "right_arm" -> Part.RIGHT_ARM;
                default -> Part.BODY;
            };
            Glow glow = s.has("glow") ? switch (s.get("glow").getAsString()) {
                case "solar" -> Glow.SOLAR;
                case "thrust" -> Glow.THRUST;
                case "pulse" -> Glow.PULSE;
                case "idle" -> Glow.IDLE;
                default -> Glow.NONE;
            } : Glow.NONE;
            Anim anim = null;
            if (s.has("anim")) {
                JsonObject a = s.getAsJsonObject("anim");
                JsonArray axis = a.getAsJsonArray("axis"), pivot = a.getAsJsonArray("pivot");
                Drive drive = switch (a.has("drive") ? a.get("drive").getAsString() : "active") {
                    case "solar" -> Drive.SOLAR;
                    case "thrust" -> Drive.THRUST;
                    default -> Drive.ACTIVE;
                };
                anim = new Anim(a.get("type").getAsString(), axis.get(0).getAsFloat(), axis.get(1).getAsFloat(), axis.get(2).getAsFloat(),
                    pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat(),
                    f(a, "open"), f(a, "closed"), f(a, "speed"), f(a, "amplitude"), Math.max(0.1F, f(a, "period")), drive);
            }
            List<float[]> quads = new ArrayList<>();
            for (JsonElement q : s.getAsJsonArray("quads")) {
                JsonObject o = q.getAsJsonObject();
                JsonArray v = o.getAsJsonArray("v"), n = o.getAsJsonArray("n");
                if (v.size() != 20 || n.size() != 3) continue;
                float[] quad = new float[23];
                for (int i = 0; i < 20; i++) quad[i] = v.get(i).getAsFloat();
                for (int i = 0; i < 3; i++) quad[20 + i] = n.get(i).getAsFloat();
                quads.add(quad);
            }
            segments.add(new Segment(s.get("name").getAsString(), part, s.has("emissive") && s.get("emissive").getAsBoolean(),
                glow, anim, quads.toArray(new float[0][])));
        }
        return new CoreAttachmentModel(texture, List.copyOf(segments));
    }

    private static float f(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsFloat() : 0F;
    }
}
