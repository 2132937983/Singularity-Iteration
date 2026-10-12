// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.armory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * <p><b>EN:</b> Armory events (public API since 0.1.8.0). Singularity Iteration posts them on
 * {@code NeoForge.EVENT_BUS}, on the server thread.
 * {@link Summon}: before a suit flies to a wearer (the player or a companion); cancel it to stop the
 * summon, then no energy is spent and no item moves. {@link PieceDocked}: after a flying piece is
 * equipped. {@link WingmanDeploy}: before a stored suit deploys as a Wingman drone; cancel it to stop
 * the deploy.
 * <p><b>ZH:</b> 军械库事件（0.1.8.0 起的公共 API）。奇点迭代在服务器线程上把它们发布到 {@code NeoForge.EVENT_BUS}。
 * {@link Summon}：套装飞向穿戴者（玩家或同伴）之前；取消即停止召唤，不消耗能量、不移动物品。{@link PieceDocked}：飞行部件装备完成之后。
 * {@link WingmanDeploy}：储存的套装作为僚机出动之前；取消即停止出动。
 * <p><b>JA:</b> 武装庫イベント（0.1.8.0 以降の公開 API）。奇点迭代はサーバースレッドで {@code NeoForge.EVENT_BUS} に投稿する。
 * {@link Summon}：スーツが装着者（プレイヤーまたは随伴者）へ飛ぶ前。キャンセルすると召喚を止め、電力も消費せずアイテムも動かない。
 * {@link PieceDocked}：飛行部位の装着完了後。{@link WingmanDeploy}：保管スーツがウィングマンとして出撃する前。キャンセルで出撃を止める。
 */
public abstract class ArmoryEvent extends Event {
    private final Player player;

    protected ArmoryEvent(Player player) { this.player = player; }

    /** EN: The player who gave the order (owner of the Armory). ZH: 下达命令的玩家（军械库所有者）。JA: 命令したプレイヤー（武装庫の所有者）。 */
    public Player getPlayer() { return player; }

    /**
     * <p><b>EN:</b> Before a summon. The pieces are still in the Armory. Cancelable.
     * <p><b>ZH:</b> 召唤之前。部件仍在军械库中。可取消。
     * <p><b>JA:</b> 召喚の前。部位はまだ武装庫にある。キャンセル可能。
     */
    public static final class Summon extends ArmoryEvent implements ICancellableEvent {
        private final LivingEntity wearer;
        private final BlockPos armory;
        private final int set;
        private final List<EquipmentSlot> slots;

        public Summon(Player player, LivingEntity wearer, BlockPos armory, int set, List<EquipmentSlot> slots) {
            super(player);
            this.wearer = wearer;
            this.armory = armory;
            this.set = set;
            this.slots = List.copyOf(slots);
        }

        /** EN: The entity that gets the suit (the player, or a companion such as a maid). ZH: 获得套装的实体（玩家或女仆等同伴）。JA: スーツを受け取るエンティティ（プレイヤー、またはメイドなどの随伴者）。 */
        public LivingEntity getWearer() { return wearer; }
        /** EN: Position of the Armory. ZH: 军械库的位置。JA: 武装庫の位置。 */
        public BlockPos getArmory() { return armory; }
        /** EN: Suit set index of the Armory (0..5). ZH: 军械库中的套装编号（0..5）。JA: 武装庫内のスーツ番号（0..5）。 */
        public int getSet() { return set; }
        /** EN: Slots that receive a piece. ZH: 将接收部件的槽位。JA: 部位を受け取るスロット。 */
        public List<EquipmentSlot> getSlots() { return slots; }
    }

    /**
     * <p><b>EN:</b> After a piece is equipped. Do not replace {@code stack}; it is the worn item now.
     * <p><b>ZH:</b> 部件装备之后。不要替换 {@code stack}，它现在就是穿戴中的物品。
     * <p><b>JA:</b> 部位の装着後。{@code stack} を置き換えない。これが現在着用中のアイテム。
     */
    public static final class PieceDocked extends ArmoryEvent {
        private final LivingEntity wearer;
        private final EquipmentSlot slot;
        private final ItemStack stack;

        public PieceDocked(Player player, LivingEntity wearer, EquipmentSlot slot, ItemStack stack) {
            super(player);
            this.wearer = wearer;
            this.slot = slot;
            this.stack = stack;
        }

        /** EN: The wearer. ZH: 穿戴者。JA: 装着者。 */
        public LivingEntity getWearer() { return wearer; }
        /** EN: The slot of the piece. ZH: 部件的槽位。JA: 部位のスロット。 */
        public EquipmentSlot getSlot() { return slot; }
        /** EN: The worn piece. ZH: 穿戴中的部件。JA: 着用中の部位。 */
        public ItemStack getStack() { return stack; }
    }

    /**
     * <p><b>EN:</b> Before a Wingman deploy. Cancelable.
     * <p><b>ZH:</b> 僚机出动之前。可取消。
     * <p><b>JA:</b> ウィングマン出撃の前。キャンセル可能。
     */
    public static final class WingmanDeploy extends ArmoryEvent implements ICancellableEvent {
        private final BlockPos armory;
        private final int set;
        private final String mode;

        public WingmanDeploy(Player player, BlockPos armory, int set, String mode) {
            super(player);
            this.armory = armory;
            this.set = set;
            this.mode = mode;
        }

        /** EN: Position of the Armory. ZH: 军械库的位置。JA: 武装庫の位置。 */
        public BlockPos getArmory() { return armory; }
        /** EN: Suit set index (0..5). ZH: 套装编号（0..5）。JA: スーツ番号（0..5）。 */
        public int getSet() { return set; }
        /** EN: Tactical mode: OFFENSIVE, DISPERSAL, ESCORT or FULTON. ZH: 战术模式：OFFENSIVE、DISPERSAL、ESCORT 或 FULTON。JA: 戦術モード：OFFENSIVE、DISPERSAL、ESCORT、FULTON。 */
        public String getMode() { return mode; }
    }
}
