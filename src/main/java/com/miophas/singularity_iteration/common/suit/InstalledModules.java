// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Data component value: the upgrade units installed in one suit piece, in install order. */
public record InstalledModules(List<String> ids) {
    public static final InstalledModules EMPTY = new InstalledModules(List.of());
    public static final Codec<InstalledModules> CODEC = Codec.STRING.listOf().xmap(InstalledModules::new, InstalledModules::ids);
    public static final StreamCodec<ByteBuf, InstalledModules> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)).map(InstalledModules::new, InstalledModules::ids);

    public InstalledModules {
        ids = List.copyOf(ids);
    }

    public boolean has(SuitModuleType type) { return ids.contains(type.id()); }

    /** Known units only (an id from a removed unit type is kept in the data but ignored). */
    public List<SuitModuleType> types() {
        List<SuitModuleType> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            SuitModuleType type = SuitModuleType.byId(id);
            if (type != null) out.add(type);
        }
        return out;
    }

    public InstalledModules with(SuitModuleType type) {
        if (has(type)) return this;
        List<String> next = new ArrayList<>(ids);
        next.add(type.id());
        return new InstalledModules(next);
    }

    public InstalledModules without(SuitModuleType type) {
        List<String> next = new ArrayList<>(ids);
        next.remove(type.id());
        return new InstalledModules(next);
    }
}
