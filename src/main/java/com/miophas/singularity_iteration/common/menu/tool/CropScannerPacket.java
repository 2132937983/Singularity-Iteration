// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.crop.CropDiagnosis;
import com.miophas.singularity_iteration.common.item.tools.CropAnalyzerItem;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * <p><b>EN:</b> Payloads of the Advanced Crop Scanner (0.1.8.2).
 * <ul>
 *   <li>{@link SetGene} (C2S): the gene-editing slider of the open scanner menu sets one target.
 *       The server checks the container id and the menu type.</li>
 *   <li>{@link HudRequest} (C2S): "read the crop stick at {@code pos}". The client sends it at most
 *       twice a second while it holds a charged scanner and aims at a crop stick. The server limits
 *       the rate per player (8 ticks), checks the distance (10 blocks) and the loaded chunk, and
 *       takes {@link CropAnalyzerItem#HUD_EU_PER_READING} EU from the held scanner.</li>
 *   <li>{@link HudReply} (S2C): the reading ({@link CropDiagnosis.Snapshot}).</li>
 * </ul>
 * <p><b>ZH:</b> 次世代作物扫描仪的数据包（0.1.8.2）。
 * <ul>
 *   <li>{@link SetGene}（C2S）：打开的扫描仪菜单中的基因编辑滑块设置一个目标。服务器检查容器 ID 与菜单类型。</li>
 *   <li>{@link HudRequest}（C2S）：“读取 {@code pos} 处的作物架”。客户端在手持有电的扫描仪并瞄准作物架时，最多每秒发送两次。
 *       服务器按玩家限速（8 tick），检查距离（10 格）与区块是否加载，并从所持扫描仪扣除 {@link CropAnalyzerItem#HUD_EU_PER_READING} EU。</li>
 *   <li>{@link HudReply}（S2C）：读数（{@link CropDiagnosis.Snapshot}）。</li>
 * </ul>
 * <p><b>JA:</b> 次世代型作物スキャナーのペイロード（0.1.8.2）。
 * <ul>
 *   <li>{@link SetGene}（C2S）：開いているスキャナーメニューの遺伝子改変スライダーが目標値を 1 つ設定する。サーバーはコンテナ ID とメニューの種類を確かめる。</li>
 *   <li>{@link HudRequest}（C2S）：「{@code pos} の作物棒を読む」。クライアントは充電済みのスキャナーを持って作物棒を狙っている間、最大で毎秒 2 回送る。
 *       サーバーはプレイヤーごとに頻度を制限し（8 tick）、距離（10 ブロック）と読み込み済みチャンクを確かめ、手のスキャナーから
 *       {@link CropAnalyzerItem#HUD_EU_PER_READING} EU を取る。</li>
 *   <li>{@link HudReply}（S2C）：読み取り結果（{@link CropDiagnosis.Snapshot}）。</li>
 * </ul>
 */
public final class CropScannerPacket {
    private CropScannerPacket() {}

    public record SetGene(int containerId, int stat, int value) implements CustomPacketPayload {
        public static final Type<SetGene> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "crop_scanner_gene"));
        public static final StreamCodec<FriendlyByteBuf, SetGene> CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.containerId); b.writeByte(p.stat); b.writeByte(p.value); },
            b -> new SetGene(b.readVarInt(), b.readUnsignedByte(), b.readUnsignedByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HudRequest(BlockPos pos) implements CustomPacketPayload {
        public static final Type<HudRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "crop_scanner_hud_request"));
        public static final StreamCodec<FriendlyByteBuf, HudRequest> CODEC = StreamCodec.of((b, p) -> b.writeBlockPos(p.pos), b -> new HudRequest(b.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HudReply(BlockPos pos, long gameTime, CropDiagnosis.Snapshot snapshot) implements CustomPacketPayload {
        public static final Type<HudReply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "crop_scanner_hud_reply"));
        public static final StreamCodec<FriendlyByteBuf, HudReply> CODEC = StreamCodec.of(
            (b, p) -> { b.writeBlockPos(p.pos); b.writeVarLong(Math.max(0, p.gameTime)); p.snapshot.write(b); },
            b -> new HudReply(b.readBlockPos(), b.readVarLong(), CropDiagnosis.Snapshot.read(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static volatile HudReply latest;

    /** EN: Client: last reply (null before the first). ZH: 客户端：最近一次回复（首次之前为 null）。JA: クライアント：最後の返信（最初の受信前は null）。 */
    public static HudReply latest() { return latest; }

    public static void handleSetGene(SetGene packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CropAnalyzerMenu menu
                    && menu.containerId == packet.containerId() && menu.stillValid(player)) {
                menu.setGeneTarget(packet.stat(), packet.value());
            }
        });
    }

    public static void handleHudRequest(HudRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
            long now = level.getGameTime();
            Long last = LAST.get(player.getUUID());
            if (last != null && now - last < 8 && last <= now) return;
            if (LAST.size() > 256) LAST.values().removeIf(t -> now - t > 1200 || t > now);
            LAST.put(player.getUUID(), now);
            BlockPos pos = request.pos();
            if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > 10 * 10) return;
            if (!(level.getBlockEntity(pos) instanceof IPlanter planter)) return;
            ItemStack scanner = heldScanner(player);
            if (scanner.isEmpty() || !((CropAnalyzerItem) scanner.getItem()).consumeEnergy(scanner, CropAnalyzerItem.HUD_EU_PER_READING)) return;
            try {
                if (player.connection != null && player.connection.hasChannel(HudReply.TYPE))
                    PacketDistributor.sendToPlayer(player, new HudReply(pos, now, CropDiagnosis.of(planter)));
            } catch (RuntimeException ignored) { }
        });
    }

    /** EN: The scanner in the main hand, else in the off hand, else empty. ZH: 主手的扫描仪，否则副手的，否则为空。JA: メインハンドのスキャナー、なければオフハンド、なければ空。 */
    public static ItemStack heldScanner(net.minecraft.world.entity.player.Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof CropAnalyzerItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static void handleHudReply(HudReply reply, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> latest = reply);
    }
}
