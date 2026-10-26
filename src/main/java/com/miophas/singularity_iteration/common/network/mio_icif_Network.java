package com.miophas.singularity_iteration.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all custom payloads of the mod in one place ({@link #register}).
 *
 * <h2>Rules for all payloads</h2>
 * <ul>
 *   <li>Channel version: the mod version ({@link #protocolVersion()}). NeoForge refuses a
 *       client and a server with different versions at login.</li>
 *   <li>No payload is optional. NeoForge refuses a vanilla client at login. A fake player or a
 *       test player can still exist without the channel. A server-to-client sender must check
 *       {@code connection.hasChannel(type)} or catch the send error.</li>
 *   <li>Handler thread: the registrar uses the NeoForge default, the main thread (the server
 *       thread, or the client main thread). On the main thread,
 *       {@code IPayloadContext.enqueueWork} runs its task at once.</li>
 *   <li>Trust boundary: a client-to-server handler must treat each field as hostile. It must
 *       check ranges, ownership, distance and the open menu on the server.</li>
 * </ul>
 *
 * <h2>Payloads</h2>
 * C2S = client to server. S2C = server to client.
 * <table>
 *   <caption>Registered payloads</caption>
 *   <tr><th>Payload</th><th>Direction</th><th>Purpose</th></tr>
 *   <tr><td>{@code SuitPackets.ManeuverInput}</td><td>C2S</td><td>throttle and stick of the local Special Maneuver Mode flyer</td></tr>
 *   <tr><td>{@code SuitPackets.ManeuverState}</td><td>S2C</td><td>throttle and stick of a different flyer, for visuals</td></tr>
 *   <tr><td>{@code SuitPackets.Threats}</td><td>S2C</td><td>threat sensor list: entities that watch or target the wearer</td></tr>
 *   <tr><td>{@code SuitPackets.ShieldHit}</td><td>S2C</td><td>one hit that the suit deflector stopped</td></tr>
 *   <tr><td>{@code SuitPackets.ChunkGrid}</td><td>S2C</td><td>EU grid summary of the player's chunk</td></tr>
 *   <tr><td>{@code SuitPackets.ChunkGridRequest}</td><td>C2S</td><td>asks for {@code ChunkGrid}</td></tr>
 *   <tr><td>{@code MetalFormerModePacket}</td><td>C2S</td><td>sets the mode of a Metal Former</td></tr>
 *   <tr><td>{@code FutureCommoditySync}</td><td>S2C</td><td>datapack futures commodity list and the commodities that the player has not unlocked</td></tr>
 *   <tr><td>{@code NuclearExplosionAnimationPacket}</td><td>S2C</td><td>starts the mushroom cloud animation of a nuclear explosion</td></tr>
 *   <tr><td>{@code LaserBlastFxPacket}</td><td>S2C</td><td>plasma blast effect of the mining laser's Ultra-High Energy Blast (0.1.10.11)</td></tr>
 *   <tr><td>{@code ReactorAuditPacket}</td><td>S2C</td><td>one reactor incident for the accident audit report (0.1.10.12)</td></tr>
 *   <tr><td>{@code ODScannerResultPacket}</td><td>S2C</td><td>result of an ore dictionary (OD) scan</td></tr>
 *   <tr><td>{@code FutureTradePacket}</td><td>C2S</td><td>a futures trade request at a market block</td></tr>
 *   <tr><td>{@code ScannerButtonPacket}</td><td>C2S</td><td>delete or save button of the Scanner</td></tr>
 *   <tr><td>{@code JetpackModeSwitchPacket}</td><td>C2S</td><td>toggles the hover mode of the worn jetpack</td></tr>
 *   <tr><td>{@code JetpackKeyStatePacket}</td><td>C2S</td><td>jetpack key state (bit mask)</td></tr>
 *   <tr><td>{@code ToolTogglePacket}</td><td>C2S</td><td>toggles the tool in the main hand (G key)</td></tr>
 *   <tr><td>{@code QuantumJumpPacket}</td><td>C2S</td><td>legacy quantum boots jump. Kept for compatibility. It has no effect.</td></tr>
 *   <tr><td>{@code KeyboardStatePacket}</td><td>C2S</td><td>key state of the player, sent on change (IC2 key update)</td></tr>
 *   <tr><td>{@code QuantumBoostPacket}</td><td>S2C</td><td>dash velocity of the quantum boots</td></tr>
 *   <tr><td>{@code FluidRegulatorFlowRatePacket}</td><td>C2S</td><td>sets the flow rate of a Fluid Regulator</td></tr>
 *   <tr><td>{@code FluidDistributorModePacket}</td><td>C2S</td><td>changes the mode of a Fluid Distributor</td></tr>
 *   <tr><td>{@code FluidRegulatorModePacket}</td><td>C2S</td><td>changes the mode of a Fluid Regulator</td></tr>
 *   <tr><td>{@code WeightedDistributorPriorityPacket}</td><td>C2S</td><td>sets the priority order of a Weighted Distributor</td></tr>
 *   <tr><td>{@code WorkbenchClearPacket}</td><td>C2S</td><td>clear button of the Industrial Workbench</td></tr>
 *   <tr><td>{@code SorterDefaultDirPacket}</td><td>C2S</td><td>sets the default output side of the Electric Sorter</td></tr>
 *   <tr><td>{@code ChunkLoaderTogglePacket}</td><td>C2S</td><td>toggles one chunk of a Chunk Loader</td></tr>
 *   <tr><td>{@code ArmorFeatureTogglePacket}</td><td>C2S</td><td>switches an on/off feature of a worn item on or off</td></tr>
 *   <tr><td>{@code ArmorFeatureCyclePacket}</td><td>C2S</td><td>selects the next mode of a mode feature of a worn item</td></tr>
 *   <tr><td>{@code TileEntityEventPacket}</td><td>S2C</td><td>a block entity network event</td></tr>
 *   <tr><td>{@code ItemEventPacket}</td><td>S2C</td><td>an item network event for the player that holds the item</td></tr>
 *   <tr><td>{@code TileEntityFieldUpdatePacket}</td><td>S2C</td><td>new value of one block entity field</td></tr>
 *   <tr><td>{@code ArmoryActionPacket}</td><td>C2S</td><td>action in the open Armory menu (summon, rename, unbind, automation)</td></tr>
 *   <tr><td>{@code ArmoryToastPacket}</td><td>S2C</td><td>toast for the owner: the Armory repaired and charged the stored suits</td></tr>
 *   <tr><td>{@code ArmoryConsoleSyncPacket}</td><td>S2C</td><td>contents of the Armory section of the Equipment Console</td></tr>
 *   <tr><td>{@code ArmoryConsolePacket}</td><td>C2S</td><td>action in the Armory section of the Equipment Console (refresh, summon, showcase, return)</td></tr>
 *   <tr><td>{@code MeterHudPacket.Request}</td><td>C2S</td><td>voltage detector HUD: asks what is on the network at a position</td></tr>
 *   <tr><td>{@code MeterHudPacket.Reply}</td><td>S2C</td><td>answer to {@code MeterHudPacket.Request}</td></tr>
 *   <tr><td>{@code MeterNetworkPacket}</td><td>S2C</td><td>network data and samples for the open EU meter menu</td></tr>
 *   <tr><td>{@code MachineStatusPacket}</td><td>S2C</td><td>status lamp states of machines in one chunk</td></tr>
 *   <tr><td>{@code AreaPreviewPacket}</td><td>S2C</td><td>work areas of machines for the Area Preview</td></tr>
 *   <tr><td>{@code AreaPreviewRequestPacket}</td><td>C2S</td><td>asks for the work area of the machine of the open menu</td></tr>
 *   <tr><td>{@code LaserTowerBeamPacket}</td><td>S2C</td><td>laser tower state change or volley, for visuals</td></tr>
 *   <tr><td>{@code EquipmentConsolePacket}</td><td>C2S</td><td>Equipment Console edit of one item (priority, mode, tuning)</td></tr>
 *   <tr><td>{@code EnergyTerminalSyncPacket}</td><td>S2C</td><td>one second of telemetry for the open Energy Terminal menu</td></tr>
 *   <tr><td>{@code EnergyTerminalTogglePacket}</td><td>C2S</td><td>switches one device of the terminal network on or off</td></tr>
 *   <tr><td>{@code LaserTowerSyncPacket}</td><td>S2C</td><td>statistics and target list for the open laser tower menu</td></tr>
 *   <tr><td>{@code LaserTowerConfigPacket}</td><td>C2S</td><td>edits the target list of the open laser tower menu</td></tr>
 *   <tr><td>{@code CreativeGeneratorPacket}</td><td>C2S</td><td>output typed into the open Creative Generator screen (0.1.10.11)</td></tr>
 *   <tr><td>{@code MinerFilterPacket}</td><td>C2S</td><td>sets a ghost filter slot of the Advanced Miner (JEI drag)</td></tr>
 *   <tr><td>{@code CropScannerPacket.SetGene}</td><td>C2S</td><td>gene-editing slider of the open Advanced Crop Scanner menu (0.1.8.2)</td></tr>
 *   <tr><td>{@code CropScannerPacket.HudRequest}</td><td>C2S</td><td>crop HUD: asks for the reading of a crop stick (0.1.8.2)</td></tr>
 *   <tr><td>{@code CropScannerPacket.HudReply}</td><td>S2C</td><td>answer to {@code CropScannerPacket.HudRequest} (0.1.8.2)</td></tr>
 * </table>
 */
@SuppressWarnings("null")
public class mio_icif_Network {

    /**
     * Returns the channel version for all payloads of the mod. The value is the mod version from
     * the mod metadata, or "1" when the mod container is not available.
     */
    public static String protocolVersion() {
        return net.neoforged.fml.ModList.get().getModContainerById(com.miophas.singularity_iteration.common.Singularity_Iteration.MOD_ID)
            .map(c -> c.getModInfo().getVersion().toString()).orElse("1");
    }

    /**
     * Registers each payload with its codec and its handler. {@link #init} adds this method as
     * a listener on the mod event bus. The class Javadoc lists each payload, its direction and
     * its purpose.
     *
     * <p>{@code playToServer} registers a client-to-server payload. {@code playToClient}
     * registers a server-to-client payload. All payloads use the play protocol. The registrar
     * does not call {@code optional()} or {@code executesOn(...)}. Thus each payload is
     * required, and each handler runs on the main thread.
     *
     * @param event the NeoForge payload registration event
     */
    public static void register(final RegisterPayloadHandlersEvent event) {
        // The channel version is the mod version: a client and a server on different SI builds
        // are refused at login with NeoForge's channel-mismatch screen. With the fixed "1" they
        // connected and then crashed on the first menu whose slot layout had changed (the GESU
        // module / core menu gained its charge and armor slots in 0.1.7.29: an older client
        // built 36 slots, the newer server synced 42 -> IndexOutOfBoundsException at slot 36).
        final PayloadRegistrar registrar = event.registrar(protocolVersion());
        // Suit sensors and Special Maneuver Mode: six payloads, see SuitPackets.
        com.miophas.singularity_iteration.common.suit.SuitPackets.register(registrar);

        // 0.1.10.21: the exchange panel of the futures machine (request / action C2S, view S2C).
        com.miophas.singularity_iteration.common.exchange.ExchangePackets.register(registrar);
        GridPackets.register(registrar);   // 0.1.10.22

        // C2S: Metal Former mode change.
        registrar.playToServer(
            MetalFormerModePacket.TYPE,
            MetalFormerModePacket.CODEC,
            MetalFormerModePacket::handle
        );

        // C2S (0.1.10.6): overclock slider of a machine GUI.
        registrar.playToServer(
            MachineOverclockPacket.TYPE,
            MachineOverclockPacket.CODEC,
            MachineOverclockPacket::handle
        );

        // S2C: datapack futures commodities, and the commodities that the player has not unlocked.
        registrar.playToClient(
            com.miophas.singularity_iteration.common.future.FutureCommoditySync.TYPE,
            com.miophas.singularity_iteration.common.future.FutureCommoditySync.CODEC,
            com.miophas.singularity_iteration.common.future.FutureCommoditySync::handle
        );

        // S2C (0.1.8.1): jetpack thrust state of a tracked player, for the thruster sound.
        registrar.playToClient(
            com.miophas.singularity_iteration.common.flight.JetpackThrustPacket.TYPE,
            com.miophas.singularity_iteration.common.flight.JetpackThrustPacket.CODEC,
            com.miophas.singularity_iteration.common.flight.JetpackThrustPacket::handle
        );

        // S2C: nuclear explosion mushroom cloud animation.
        registrar.playToClient(
            NuclearExplosionAnimationPacket.TYPE,
            NuclearExplosionAnimationPacket.CODEC,
            NuclearExplosionAnimationPacket::handle
        );
        // 0.1.10.11, S2C: plasma blast effect of the mining laser's Ultra-High Energy Blast.
        registrar.playToClient(
            LaserBlastFxPacket.TYPE,
            LaserBlastFxPacket.CODEC,
            LaserBlastFxPacket::handle
        );
        // 0.1.10.12, S2C: one reactor incident for the accident audit report.
        registrar.playToClient(
            ReactorAuditPacket.TYPE,
            ReactorAuditPacket.CODEC,
            ReactorAuditPacket::handle
        );

        // S2C: OD scanner result.
        registrar.playToClient(
            ODScannerResultPacket.TYPE,
            ODScannerResultPacket.CODEC,
            ODScannerResultPacket::handle
        );

        // C2S: futures trade request.
    registrar.playToServer(
            FutureTradePacket.TYPE,
            FutureTradePacket.CODEC,
            FutureTradePacket::handle
        );

        // C2S: Scanner button click (delete or save).
        registrar.playToServer(
            ScannerButtonPacket.TYPE,
            ScannerButtonPacket.STREAM_CODEC,
            ScannerButtonPacket::handleOnServer
        );

        // C2S: jetpack hover mode toggle.
    registrar.playToServer(
            JetpackModeSwitchPacket.TYPE,
            JetpackModeSwitchPacket.CODEC,
            JetpackModeSwitchPacket::handle
        );

        // C2S: jetpack key state (IC2 style, separate from the general key state).
        registrar.playToServer(
            JetpackKeyStatePacket.TYPE,
            JetpackKeyStatePacket.CODEC,
            JetpackKeyStatePacket::handle
        );

        // C2S: tool on/off toggle.
        registrar.playToServer(
            ToolTogglePacket.TYPE,
            ToolTogglePacket.CODEC,
            ToolTogglePacket::handle
        );

        // C2S: quantum boots jump. Kept for compatibility only.
        registrar.playToServer(
            QuantumJumpPacket.TYPE,
            QuantumJumpPacket.CODEC,
            QuantumJumpPacket::handle
        );

        // C2S: key state sync (from IC2RIn120: KeyboardClient.sendKeyUpdate).
    registrar.playToServer(
            KeyboardStatePacket.TYPE,
            KeyboardStatePacket.CODEC,
            KeyboardStatePacket::handle
        );

        // S2C: quantum boots dash velocity.
        registrar.playToClient(
            QuantumBoostPacket.TYPE,
            QuantumBoostPacket.CODEC,
            QuantumBoostPacket::handle
        );

        // C2S: Fluid Regulator flow rate.
        registrar.playToServer(
            FluidRegulatorFlowRatePacket.TYPE,
            FluidRegulatorFlowRatePacket.CODEC,
            FluidRegulatorFlowRatePacket::handle
        );

        // C2S: Fluid Distributor mode change.
        registrar.playToServer(
            FluidDistributorModePacket.TYPE,
            FluidDistributorModePacket.CODEC,
            FluidDistributorModePacket::handle
        );

        // C2S: Fluid Regulator mode change.
        registrar.playToServer(
            FluidRegulatorModePacket.TYPE,
            FluidRegulatorModePacket.CODEC,
            FluidRegulatorModePacket::handle
        );

        // C2S: Weighted Distributor priority order.
    registrar.playToServer(
            WeightedDistributorPriorityPacket.TYPE,
            WeightedDistributorPriorityPacket.CODEC,
            WeightedDistributorPriorityPacket::handle
        );

        // C2S: Industrial Workbench clear button.
        registrar.playToServer(
            WorkbenchClearPacket.TYPE,
            WorkbenchClearPacket.CODEC,
            WorkbenchClearPacket::handle
        );

        // C2S: Electric Sorter default output side.
        registrar.playToServer(
            SorterDefaultDirPacket.TYPE,
            SorterDefaultDirPacket.CODEC,
            SorterDefaultDirPacket::handle
        );

        // C2S: Chunk Loader chunk toggle.
        registrar.playToServer(
            ChunkLoaderTogglePacket.TYPE,
            ChunkLoaderTogglePacket.CODEC,
            ChunkLoaderTogglePacket::handle
        );

        // C2S: armor feature on/off toggle.
        registrar.playToServer(
            ArmorFeatureTogglePacket.TYPE,
            ArmorFeatureTogglePacket.CODEC,
            ArmorFeatureTogglePacket::handle
        );

        // C2S: armor feature mode cycle.
        registrar.playToServer(
            ArmorFeatureCyclePacket.TYPE,
            ArmorFeatureCyclePacket.CODEC,
            ArmorFeatureCyclePacket::handle
        );

        // S2C: block entity network event.
        registrar.playToClient(
            TileEntityEventPacket.TYPE,
            TileEntityEventPacket.CODEC,
            TileEntityEventPacket::handle
        );

        // S2C: item network event.
        registrar.playToClient(
            ItemEventPacket.TYPE,
            ItemEventPacket.CODEC,
            ItemEventPacket::handle
        );

        // S2C: block entity field update.
        registrar.playToClient(
            TileEntityFieldUpdatePacket.TYPE,
            TileEntityFieldUpdatePacket.CODEC,
            TileEntityFieldUpdatePacket::handle
        );

        // Armory: C2S menu action, S2C maintenance toast, S2C console contents, C2S console action.
        registrar.playToServer(
            com.miophas.singularity_iteration.common.armory.ArmoryActionPacket.TYPE,
            com.miophas.singularity_iteration.common.armory.ArmoryActionPacket.CODEC,
            com.miophas.singularity_iteration.common.armory.ArmoryActionPacket::handle
        );
        registrar.playToClient(
            com.miophas.singularity_iteration.common.armory.ArmoryToastPacket.TYPE,
            com.miophas.singularity_iteration.common.armory.ArmoryToastPacket.CODEC,
            com.miophas.singularity_iteration.common.armory.ArmoryToastPacket::handle
        );
        registrar.playToClient(
            com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket.TYPE,
            com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket.CODEC,
            com.miophas.singularity_iteration.common.armory.ArmoryConsoleSyncPacket::handle
        );
        registrar.playToServer(
            com.miophas.singularity_iteration.common.armory.ArmoryConsolePacket.TYPE,
            com.miophas.singularity_iteration.common.armory.ArmoryConsolePacket.CODEC,
            com.miophas.singularity_iteration.common.armory.ArmoryConsolePacket::handle
        );
        // 0.1.8.0 Blueprint Builder: C2S button of the open builder menu; S2C RTS Building drone assist.
        registrar.playToServer(
            com.miophas.singularity_iteration.common.builder.BuilderActionPacket.TYPE,
            com.miophas.singularity_iteration.common.builder.BuilderActionPacket.CODEC,
            com.miophas.singularity_iteration.common.builder.BuilderActionPacket::handle
        );
        // 0.1.8.0 energy weapon charge / compression effect (one packet per shot, to the shooter and its trackers)
        registrar.playToClient(
            com.miophas.singularity_iteration.common.item.tools.WeaponChargeFx.Shot.TYPE,
            com.miophas.singularity_iteration.common.item.tools.WeaponChargeFx.Shot.CODEC,
            com.miophas.singularity_iteration.common.item.tools.WeaponChargeFx.Shot::handle
        );
        registrar.playToClient(
            com.miophas.singularity_iteration.common.builder.RtsDroneAssist.Sites.TYPE,
            com.miophas.singularity_iteration.common.builder.RtsDroneAssist.Sites.CODEC,
            com.miophas.singularity_iteration.common.builder.RtsDroneAssist.Sites::handle
        );
        // EU meter and voltage detector: C2S HUD request, S2C HUD reply, S2C open-menu data.
        registrar.playToServer(
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket.Request.TYPE,
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket.Request.CODEC,
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket::handleRequest
        );
        registrar.playToClient(
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket.Reply.TYPE,
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket.Reply.CODEC,
            com.miophas.singularity_iteration.common.menu.tool.MeterHudPacket::handleReply
        );
        registrar.playToClient(
            com.miophas.singularity_iteration.common.menu.tool.MeterNetworkPacket.TYPE,
            com.miophas.singularity_iteration.common.menu.tool.MeterNetworkPacket.CODEC,
            com.miophas.singularity_iteration.common.menu.tool.MeterNetworkPacket::handle
        );
        // S2C: status lamp changes of one chunk (MachineStatusTracker).
        registrar.playToClient(
            com.miophas.singularity_iteration.common.machine.MachineStatusPacket.TYPE,
            com.miophas.singularity_iteration.common.machine.MachineStatusPacket.CODEC,
            com.miophas.singularity_iteration.common.machine.MachineStatusPacket::handle
        );
        // Area Preview: S2C work areas, C2S request for the machine of the open menu.
        registrar.playToClient(
            AreaPreviewPacket.TYPE,
            AreaPreviewPacket.CODEC,
            AreaPreviewPacket::handle
        );
        registrar.playToServer(
            AreaPreviewRequestPacket.TYPE,
            AreaPreviewRequestPacket.CODEC,
            AreaPreviewRequestPacket::handle
        );
        // S2C: laser tower state change or volley, for visuals. One packet per change, never per tick.
        registrar.playToClient(
            LaserTowerBeamPacket.TYPE,
            LaserTowerBeamPacket.CODEC,
            LaserTowerBeamPacket::handle
        );
        // C2S: Equipment Console edit of one item.
        registrar.playToServer(
            EquipmentConsolePacket.TYPE,
            EquipmentConsolePacket.CODEC,
            EquipmentConsolePacket::handle
        );
        // C2S (0.1.8.0): suit palette of one worn piece.
        registrar.playToServer(
            SuitPalettePacket.TYPE,
            SuitPalettePacket.CODEC,
            SuitPalettePacket::handle
        );
        // Energy Terminal: S2C telemetry, C2S device toggle.
        registrar.playToClient(
            EnergyTerminalSyncPacket.TYPE,
            EnergyTerminalSyncPacket.CODEC,
            EnergyTerminalSyncPacket::handle
        );
        registrar.playToServer(
            EnergyTerminalTogglePacket.TYPE,
            EnergyTerminalTogglePacket.CODEC,
            EnergyTerminalTogglePacket::handle
        );
        // Laser tower menu: S2C statistics and target list, C2S target list edit.
        registrar.playToClient(
            LaserTowerSyncPacket.TYPE,
            LaserTowerSyncPacket.CODEC,
            LaserTowerSyncPacket::handle
        );
        registrar.playToServer(
            LaserTowerConfigPacket.TYPE,
            LaserTowerConfigPacket.CODEC,
            LaserTowerConfigPacket::handle
        );
        // 0.1.10.11, C2S: output typed into the Creative Generator screen.
        registrar.playToServer(
            CreativeGeneratorPacket.TYPE,
            CreativeGeneratorPacket.CODEC,
            CreativeGeneratorPacket::handle
        );

        // C2S: Advanced Miner filter slot (JEI ghost drag).
        registrar.playToServer(
            MinerFilterPacket.TYPE,
            MinerFilterPacket.CODEC,
            MinerFilterPacket::handle
        );

        // EN: 0.1.8.2 Advanced Crop Scanner: C2S gene slider, C2S HUD request, S2C HUD reply.
        // ZH: 0.1.8.2 次世代作物扫描仪：C2S 基因滑块、C2S HUD 请求、S2C HUD 回复。
        // JA: 0.1.8.2 次世代型作物スキャナー：C2S 遺伝子スライダー、C2S HUD 要求、S2C HUD 返信。
        registrar.playToServer(
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket.SetGene.TYPE,
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket.SetGene.CODEC,
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket::handleSetGene
        );
        registrar.playToServer(
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket.HudRequest.TYPE,
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket.HudRequest.CODEC,
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket::handleHudRequest
        );
        registrar.playToClient(
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket.HudReply.TYPE,
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket.HudReply.CODEC,
            com.miophas.singularity_iteration.common.menu.tool.CropScannerPacket::handleHudReply
        );
    }

    /**
     * 发送高级采矿机过滤槽设置包到服务端（JEI 幽灵拖拽使用）
     * @param pos 采矿机方块坐标
     * @param filterIndex 过滤槽索引（0..FILTER_COUNT-1）
     * @param stack 要设置的过滤物品
     */
    public static void sendMinerFilter(BlockPos pos, int filterIndex, ItemStack stack) {
        PacketDistributor.sendToServer(new MinerFilterPacket(pos, filterIndex, stack));
    }

    /**
     * Sends the nuclear explosion animation packet to clients.
     * @param level the server level
     * @param centerX the explosion center X
     * @param centerY the explosion center Y
     * @param centerZ the explosion center Z
     * @param explosionRadius the explosion radius
     */
    public static void sendNuclearExplosionAnimation(ServerLevel level, double centerX, double centerY, double centerZ, int explosionRadius) {
        double distance = Math.max(512, Math.min(4096, explosionRadius * 3));
        var packet = new NuclearExplosionAnimationPacket(centerX, centerY, centerZ, explosionRadius);
        for (var player : level.players()) {
            if (player.connection != null && player.connection.hasChannel(NuclearExplosionAnimationPacket.TYPE)
                    && player.distanceToSqr(centerX, centerY, centerZ) <= distance * distance)
                PacketDistributor.sendToPlayer(player, packet);
        }
    }

    /** 0.1.10.11: the plasma blast effect to the players within {@link LaserBlastFxPacket#RANGE} blocks. */
    public static void sendLaserBlast(ServerLevel level, double x, double y, double z, float strength) {
        var packet = new LaserBlastFxPacket(x, y, z, strength);
        double range = LaserBlastFxPacket.RANGE;
        for (var player : level.players()) {
            if (player.connection != null && player.connection.hasChannel(LaserBlastFxPacket.TYPE)
                    && player.distanceToSqr(x, y, z) <= range * range)
                PacketDistributor.sendToPlayer(player, packet);
        }
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
     * @param slotId 槽位标识（head/chest/legs/feet/back）
     * @param featureKey 特性键
     * @param enabled 是否启用
     */
    public static void sendArmorFeatureToggle(String slotId, String featureKey, boolean enabled) {
        PacketDistributor.sendToServer(new ArmorFeatureTogglePacket(slotId, featureKey, enabled));
    }

    /**
     * 发送装备特性模式循环包到服务端
     * @param slotId 槽位标识（head/chest/legs/feet/back）
     * @param featureKey 特性键
     */
    public static void sendArmorFeatureCycle(String slotId, String featureKey) {
        PacketDistributor.sendToServer(new ArmorFeatureCyclePacket(slotId, featureKey));
    }

    /**
     * 将网络包注册器注册到事件总线
     * @param eventBus 事件总线
     */
    public static void init(IEventBus eventBus) {
        eventBus.addListener(mio_icif_Network::register);
    }
}