package com.singularity_iteration.mio_icif.network;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包注册类
 * 统一管理所有网络包的注册? */
@SuppressWarnings("null")
public class mio_icif_Network {

    /**
     * 注册所有网络包
     * @param event 注册事件
     */
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // 注册金属成型机模式切换包
        registrar.playToServer(
            MetalFormerModePacket.TYPE,
            MetalFormerModePacket.CODEC,
            MetalFormerModePacket::handle
        );

        // 注册核爆炸动画包
        registrar.playToClient(
            NuclearExplosionAnimationPacket.TYPE,
            NuclearExplosionAnimationPacket.CODEC,
            NuclearExplosionAnimationPacket::handle
        );

        // 注册OD扫描器结果包
        registrar.playToClient(
            ODScannerResultPacket.TYPE,
            ODScannerResultPacket.CODEC,
            ODScannerResultPacket::handle
        );

        // 注册未来交易�
    registrar.playToServer(
            FutureTradePacket.TYPE,
            FutureTradePacket.CODEC,
            FutureTradePacket::handle
        );

        // 注册扫描机按钮点击包
        registrar.playToServer(
            ScannerButtonPacket.TYPE,
            ScannerButtonPacket.STREAM_CODEC,
            ScannerButtonPacket::handleOnServer
        );

        // 注册喷气背包模式切换�
    registrar.playToServer(
            JetpackModeSwitchPacket.TYPE,
            JetpackModeSwitchPacket.CODEC,
            JetpackModeSwitchPacket::handle
        );

        // 注册喷气背包按键状态包（IC2风格，独立按键状态）
        registrar.playToServer(
            JetpackKeyStatePacket.TYPE,
            JetpackKeyStatePacket.CODEC,
            JetpackKeyStatePacket::handle
        );

        // 注册工具开关切换包
        registrar.playToServer(
            ToolTogglePacket.TYPE,
            ToolTogglePacket.CODEC,
            ToolTogglePacket::handle
        );

        // 注册量子靴大跳包（保留兼容）
        registrar.playToServer(
            QuantumJumpPacket.TYPE,
            QuantumJumpPacket.CODEC,
            QuantumJumpPacket::handle
        );

        // 注册按键状态同步包（IC2RIn120原版：KeyboardClient.sendKeyUpdate�
    registrar.playToServer(
            KeyboardStatePacket.TYPE,
            KeyboardStatePacket.CODEC,
            KeyboardStatePacket::handle
        );

        // 注册量子靴突进速度同步包（服务端→客户端）
        registrar.playToClient(
            QuantumBoostPacket.TYPE,
            QuantumBoostPacket.CODEC,
            QuantumBoostPacket::handle
        );

        // 注册流体流量调节机流量设置包
        registrar.playToServer(
            FluidRegulatorFlowRatePacket.TYPE,
            FluidRegulatorFlowRatePacket.CODEC,
            FluidRegulatorFlowRatePacket::handle
        );

        // 注册流体分配机模式切换包
        registrar.playToServer(
            FluidDistributorModePacket.TYPE,
            FluidDistributorModePacket.CODEC,
            FluidDistributorModePacket::handle
        );

        // 注册流体流量调节机模式切换包
        registrar.playToServer(
            FluidRegulatorModePacket.TYPE,
            FluidRegulatorModePacket.CODEC,
            FluidRegulatorModePacket::handle
        );

        // 注册加权分配机优先级同步�
    registrar.playToServer(
            WeightedDistributorPriorityPacket.TYPE,
            WeightedDistributorPriorityPacket.CODEC,
            WeightedDistributorPriorityPacket::handle
        );

        // 注册工业工作台清空按钮
        registrar.playToServer(
            WorkbenchClearPacket.TYPE,
            WorkbenchClearPacket.CODEC,
            WorkbenchClearPacket::handle
        );

        // 注册电动分拣机默认输出方向切换包
        registrar.playToServer(
            SorterDefaultDirPacket.TYPE,
            SorterDefaultDirPacket.CODEC,
            SorterDefaultDirPacket::handle
        );

        registrar.playToServer(
            ChunkLoaderTogglePacket.TYPE,
            ChunkLoaderTogglePacket.CODEC,
            ChunkLoaderTogglePacket::handle
        );

        // 注册装备特性切换包
        registrar.playToServer(
            ArmorFeatureTogglePacket.TYPE,
            ArmorFeatureTogglePacket.CODEC,
            ArmorFeatureTogglePacket::handle
        );

        // 注册方块实体网络事件同步包（服务端→客户端）
        registrar.playToClient(
            TileEntityEventPacket.TYPE,
            TileEntityEventPacket.CODEC,
            TileEntityEventPacket::handle
        );

        // 注册物品网络事件同步包（服务端→客户端）
        registrar.playToClient(
            ItemEventPacket.TYPE,
            ItemEventPacket.CODEC,
            ItemEventPacket::handle
        );

        // 注册方块实体字段更新同步包（服务端→客户端）
        registrar.playToClient(
            TileEntityFieldUpdatePacket.TYPE,
            TileEntityFieldUpdatePacket.CODEC,
            TileEntityFieldUpdatePacket::handle
        );
    }

    /**
     * 发送核爆炸动画包到客户端
 * @param level 服务端世�
 * @param centerX 爆炸中心X
     * @param centerY 爆炸中心Y
     * @param centerZ 爆炸中心Z
     * @param explosionRadius 爆炸半径
     */
    public static void sendNuclearExplosionAnimation(ServerLevel level, double centerX, double centerY, double centerZ, int explosionRadius) {
        PacketDistributor.sendToPlayersNear(
            level,
            null,
            centerX, centerY, centerZ,
            explosionRadius * 2,
            new NuclearExplosionAnimationPacket(centerX, centerY, centerZ, explosionRadius)
        );
    }

    /**
     * 发送喷气背包模式切换包到服务端
     */
    public static void sendJetpackModeSwitch() {
        PacketDistributor.sendToServer(new JetpackModeSwitchPacket());
    }

    /**
     * 发送喷气背包按键状态到服务端
     * @param keyState 按键状态（位掩码）
     */
    public static void sendJetpackKeyState(int keyState) {
        PacketDistributor.sendToServer(new JetpackKeyStatePacket(keyState));
    }

    /**
     * 发送装备特性切换包到服务端
     * @param slot 装备槽位
     * @param featureKey 特性键
     * @param enabled 是否启用
     */
    public static void sendArmorFeatureToggle(net.minecraft.world.entity.EquipmentSlot slot, String featureKey, boolean enabled) {
        PacketDistributor.sendToServer(new ArmorFeatureTogglePacket(slot, featureKey, enabled));
    }

    /**
     * 将网络包注册器注册到事件总线
     * @param eventBus 事件总线
     */
    public static void init(IEventBus eventBus) {
        eventBus.addListener(mio_icif_Network::register);
    }
}