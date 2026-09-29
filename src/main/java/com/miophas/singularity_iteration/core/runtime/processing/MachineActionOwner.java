// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import com.mojang.authlib.GameProfile;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Versioned identity used for protection-sensitive machine actions.
 *
 * <p>Legacy machines retain their fixed automation profile. Newly placed machines use the
 * placing player's UUID and profile name. Unknown or malformed state is deliberately inert
 * instead of silently becoming a legacy machine.</p>
 */
public final class MachineActionOwner {
    public static final int CURRENT_VERSION = 1;

    public enum Kind {
        LEGACY,
        PLAYER,
        INVALID
    }

    private final Kind kind;
    private final GameProfile profile;

    private MachineActionOwner(Kind kind, @Nullable GameProfile profile) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.profile = profile == null ? null : identityCopy(profile);
    }

    public static MachineActionOwner legacy(GameProfile profile) {
        return valid(Kind.LEGACY, profile);
    }

    public static MachineActionOwner player(GameProfile profile) {
        return valid(Kind.PLAYER, profile);
    }

    public static MachineActionOwner fromPlacer(@Nullable LivingEntity placer) {
        return placer instanceof Player player ? player(player.getGameProfile()) : invalid();
    }

    public static MachineActionOwner invalid() {
        return new MachineActionOwner(Kind.INVALID, null);
    }

    private static MachineActionOwner valid(Kind kind, GameProfile profile) {
        if (kind == Kind.INVALID || !validIdentity(profile)) return invalid();
        return new MachineActionOwner(kind, profile);
    }

    public Kind kind() {
        return kind;
    }

    public boolean canAct() {
        return kind != Kind.INVALID && profile != null;
    }

    public UUID uuid() {
        return canAct() ? profile.getId() : null;
    }

    public String name() {
        return canAct() ? profile.getName() : "";
    }

    public GameProfile actorProfile() {
        if (!canAct()) throw new IllegalStateException("Invalid machine owner has no action profile");
        return identityCopy(profile);
    }

    /** Only the same player or two legacy machines may share an automation handoff. */
    public boolean canShareAutomationWith(@Nullable MachineActionOwner other) {
        if (!canAct() || other == null || !other.canAct()) return false;
        if (kind == Kind.LEGACY || other.kind == Kind.LEGACY) {
            return kind == Kind.LEGACY && other.kind == Kind.LEGACY;
        }
        return profile.getId().equals(other.profile.getId());
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("version", CURRENT_VERSION);
        tag.putString("kind", kind.name());
        if (canAct()) {
            tag.putUUID("uuid", profile.getId());
            tag.putString("name", profile.getName());
        }
        return tag;
    }

    /** Missing state is an old save; present but unsupported state is inert. */
    public static MachineActionOwner load(CompoundTag parent, String key, GameProfile legacyProfile) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(key, "key");
        if (!parent.contains(key)) return legacy(legacyProfile);
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return invalid();
        var tag = parent.getCompound(key);
        if (!tag.contains("version", Tag.TAG_INT) || tag.getInt("version") != CURRENT_VERSION
                || !tag.contains("kind", Tag.TAG_STRING)) return invalid();
        Kind kind;
        try {
            kind = Kind.valueOf(tag.getString("kind"));
        } catch (IllegalArgumentException ignored) {
            return invalid();
        }
        if (kind == Kind.INVALID) return invalid();
        if (!tag.hasUUID("uuid") || !tag.contains("name", Tag.TAG_STRING)) return invalid();
        var decoded = new GameProfile(tag.getUUID("uuid"), tag.getString("name"));
        if (!validIdentity(decoded)) return invalid();
        if (kind == Kind.LEGACY) {
            if (!decoded.getId().equals(legacyProfile.getId()) || !decoded.getName().equals(legacyProfile.getName())) {
                return invalid();
            }
            return legacy(legacyProfile);
        }
        return player(decoded);
    }

    private static boolean validIdentity(@Nullable GameProfile profile) {
        return profile != null && profile.getId() != null && profile.getName() != null
            && !profile.getName().isBlank() && profile.getName().length() <= 64;
    }

    private static GameProfile identityCopy(GameProfile profile) {
        return new GameProfile(profile.getId(), profile.getName());
    }
}
