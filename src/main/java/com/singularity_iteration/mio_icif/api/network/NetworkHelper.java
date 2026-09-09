package com.singularity_iteration.mio_icif.api.network;

import com.singularity_iteration.mio_icif.network.ItemEventPacket;
import com.singularity_iteration.mio_icif.network.TileEntityEventPacket;
import com.singularity_iteration.mio_icif.network.TileEntityFieldUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络辅助工具类。
 * <p>
 * 对应 IC2 1.12.2 的 {@code NetworkHelper}。
 * 提供方块实体和物品的网络同步功能，通过 NeoForge 网络系统
 * 将事件从服务端真正发送到客户端。
 */
public final class NetworkHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHelper.class);
    private static final int DEFAULT_RANGE = 64;

    /** Field 鍙嶅皠缂撳瓨锛歬ey = Class.getName() + "#" + fieldName */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private NetworkHelper() {
    }

    /**
     * 更新方块实体的指定字段并同步到客户端。
     */
    public static void updateTileEntityField(BlockEntity tileEntity, String fieldName) {
        if (tileEntity == null || fieldName == null) return;
        if (tileEntity.getLevel() == null || tileEntity.getLevel().isClientSide) return;

        double value = readFieldValue(tileEntity, fieldName);

        PacketDistributor.sendToPlayersNear(
            (ServerLevel) tileEntity.getLevel(),
            null,
            tileEntity.getBlockPos().getX(),
            tileEntity.getBlockPos().getY(),
            tileEntity.getBlockPos().getZ(),
            DEFAULT_RANGE,
            new TileEntityFieldUpdatePacket(tileEntity.getBlockPos(), fieldName, value)
        );
    }

    /**
     * 触发方块实体网络事件。
     */
    public static void initiateTileEntityEvent(BlockEntity tileEntity, int eventId, boolean limitRange) {
        if (tileEntity == null) return;
        if (tileEntity.getLevel() == null || tileEntity.getLevel().isClientSide) return;

        if (limitRange) {
            PacketDistributor.sendToPlayersNear(
                (ServerLevel) tileEntity.getLevel(),
                null,
                tileEntity.getBlockPos().getX(),
                tileEntity.getBlockPos().getY(),
                tileEntity.getBlockPos().getZ(),
                DEFAULT_RANGE,
                new TileEntityEventPacket(tileEntity.getBlockPos(), eventId, true)
            );
        } else {
            PacketDistributor.sendToPlayersInDimension(
                (ServerLevel) tileEntity.getLevel(),
                new TileEntityEventPacket(tileEntity.getBlockPos(), eventId, false)
            );
        }
    }

    /**
     * 触发物品网络事件。
     */
    public static void initiateItemEvent(Player player, ItemStack stack, int eventId, boolean limitRange) {
        if (player == null || stack == null) return;
        if (player.level().isClientSide) return;

        int slotIndex = findSlotIndex(player, stack);
        if (slotIndex < 0) return;

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                serverPlayer,
                new ItemEventPacket(slotIndex, eventId, limitRange)
            );
        }
    }

    /**
     * 发送方块实体的初始同步数据到客户端。
     */
    public static void sendInitialData(BlockEntity tileEntity) {
        if (tileEntity == null) return;
        if (tileEntity.getLevel() == null || tileEntity.getLevel().isClientSide) return;
        if (tileEntity instanceof INetworkDataProvider provider) {
            for (String field : provider.getNetworkedFields()) {
                updateTileEntityField(tileEntity, field);
            }
        }
    }

    private static int findSlotIndex(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 通过反射从方块实体中读取字段值，转换为 double。
     * <p>
     * 支持 {@code int}、{@code long}、{@code float}、{@code double}、{@code boolean} 类型。
 * Field 对象会被存以避免重复反射查找。
     * 如果字段不存在或不可读，返回 0 并在 debug 级别记录日志。
     *
     * <p><b>注意：</b>{@code long} 值转 {@code double} 可能丢失精度（超过 2^53 的值）。
     *
     * @param tileEntity 方块实体
     * @param fieldName  字段名
     * @return 字段值（double）
     */
    private static double readFieldValue(BlockEntity tileEntity, String fieldName) {
        Class<?> clazz = tileEntity.getClass();
        Field field = resolveField(clazz, fieldName);
        if (field == null) {
            LOGGER.debug("Field '{}' not found in {} or {}", fieldName, clazz.getSimpleName(), "BlockEntity");
            return 0;
        }
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == int.class) return field.getInt(tileEntity);
            if (type == long.class) return field.getLong(tileEntity);
            if (type == float.class) return field.getFloat(tileEntity);
            if (type == double.class) return field.getDouble(tileEntity);
            if (type == boolean.class) return field.getBoolean(tileEntity) ? 1.0 : 0.0;
            return 0;
        } catch (Exception e) {
            LOGGER.debug("Failed to read field '{}' from {}: {}", fieldName, clazz.getSimpleName(), e.getMessage());
            return 0;
        }
    }

    /**
 * 解析字段，带存。先查子类，再查 BlockEntity 父类。
     */
    private static Field resolveField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "#" + fieldName;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) return cached;

        Field field = findField(clazz, fieldName);
        if (field != null) {
            FIELD_CACHE.put(key, field);
        }
        return field;
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
