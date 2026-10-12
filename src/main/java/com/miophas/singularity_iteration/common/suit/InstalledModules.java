// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component value: the upgrade units installed in one suit piece, in install order.
 * Built-in units store their bare id ("ballistic"); addon units (0.1.8.0 public API) store their
 * full id ("addon:unit"). The format is unchanged, so old saves load as before.
 */
public record InstalledModules(List<String> ids) {
    public static final InstalledModules EMPTY = new InstalledModules(List.of());
    public static final Codec<InstalledModules> CODEC = Codec.STRING.listOf().xmap(InstalledModules::new, InstalledModules::ids);
    public static final StreamCodec<ByteBuf, InstalledModules> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)).map(InstalledModules::new, InstalledModules::ids);

    public InstalledModules {
        ids = List.copyOf(ids);
    }

    public boolean has(SuitModuleType type) { return ids.contains(type.id()); }
    public boolean has(String key) { return ids.contains(key); }

    /** Known built-in units only (an id from a removed unit type is kept in the data but ignored). */
    public List<SuitModuleType> types() {
        List<SuitModuleType> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            SuitModuleType type = SuitModuleType.byId(id);
            if (type != null) out.add(type);
        }
        return out;
    }

    /** Known units of both kinds (built-in and registered addon units), in install order. */
    public List<UnitRef> units() {
        List<UnitRef> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            UnitRef unit = UnitRef.byKey(id);
            if (unit != null) out.add(unit);
        }
        return out;
    }

    public InstalledModules with(SuitModuleType type) { return with(type.id()); }
    public InstalledModules without(SuitModuleType type) { return without(type.id()); }

    public InstalledModules with(String key) {
        if (has(key)) return this;
        List<String> next = new ArrayList<>(ids);
        next.add(key);
        return new InstalledModules(next);
    }

    public InstalledModules without(String key) {
        List<String> next = new ArrayList<>(ids);
        next.remove(key);
        return new InstalledModules(next);
    }
}
