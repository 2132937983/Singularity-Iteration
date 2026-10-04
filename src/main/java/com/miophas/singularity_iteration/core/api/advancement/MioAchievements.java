// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 进度的授予入口。
 * <p>
 * 进度背后的世界事件（超压爆炸、核爆、反应堆熔毁与启动、复制机产出）大多发生在没有玩家上下文的
 * 方块实体、多方块管理器或能量网络里，所以统一照搬原版 {@code CrafterBlock} 的做法：把进度授予
 * 触发点附近 {@link #RADIUS} 格内的所有在线玩家。
 * <p>
 * 进度或 criterion 名字不存在时静默失败，因此调用点不需要额外判空。
 * <p>
 * 本类位于 {@code core} 源码集，因此命名空间按该源码集的既有约定（{@code EUApi.MOD_ID}、
 * {@code CoreSounds} 等）内联为字面量，不引用 {@code common} 下的模组主类。
 */
public final class MioAchievements {
    /** 本模组的命名空间。 */
    public static final String MOD_ID = "mio_icif";

    /** 授予半径（格）。 */
    public static final double RADIUS = 16.0D;

    private MioAchievements() {}

    /** 按方块坐标授予。 */
    public static void at(Level level, BlockPos pos, String path, String criterion) {
        if (pos == null) {
            return;
        }
        at(level, Vec3.atCenterOf(pos), path, criterion);
    }

    /** 按世界坐标授予。 */
    public static void at(Level level, Vec3 center, String path, String criterion) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        double limit = RADIUS * RADIUS;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(center) <= limit) {
                grant(player, path, criterion);
            }
        }
    }

    /** 只授予指定玩家。 */
    public static void grant(ServerPlayer player, String path, String criterion) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        AdvancementHolder holder = player.server.getAdvancements().get(id);
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}
