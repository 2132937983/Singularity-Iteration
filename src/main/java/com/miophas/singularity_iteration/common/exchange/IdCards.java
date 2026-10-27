// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.miophas.singularity_iteration.common.exchange.ExchangeService.Result;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.registry.mio_icif_data_components;
import com.mojang.authlib.GameProfile;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.28 (X4): ID cards in the game (rules in {@link IdCardRules}). Blank cards are written in two ways: a player uses a
 * blank card and gets a personal card; the owner of Trade Terminals writes a blank card for another player in the Access
 * page of a terminal (a delegate card). The world secret that signs the cards and the register of the delegate cards live in
 * the exchange data ({@code Cards}). A delegate card that is not in its issuer's register (revoked, expired and pruned) gives
 * nothing.
 */
public final class IdCards {
    private IdCards() {}

    /** The world secret; made on first use. */
    static long secret(ExchangeData d) {
        if (d.cardSecret == 0) {
            long s = 0;
            SecureRandom r = new SecureRandom();
            while (s == 0) s = r.nextLong();
            d.cardSecret = s;
            d.setDirty();
        }
        return d.cardSecret;
    }

    public static boolean isCard(ItemStack stack) { return stack.is(mio_icif_normal.ID_CARD.get()); }

    public static IdCardData data(ItemStack stack) { return isCard(stack) ? stack.get(mio_icif_data_components.ID_CARD.get()) : null; }

    static IdCardData write(ExchangeData d, UUID issuer, String issuerName, UUID holder, String holderName, int level, long expiry, boolean personal) {
        long serial = d.cardSerial++;
        IdCardRules.Fields f = new IdCardRules.Fields(issuer, holder, level, expiry, serial, personal);
        d.setDirty();
        return new IdCardData(issuer, issuerName, holder, holderName, level, expiry, serial, personal, IdCardRules.sign(secret(d), f));
    }

    /** A blank card used by {@code player}: one card becomes the player's personal card (returned; the caller places it). */
    public static ItemStack writePersonal(ServerPlayer player) {
        ExchangeData d = ExchangeData.get(player.server);
        String name = player.getGameProfile().getName();
        ItemStack card = new ItemStack(mio_icif_normal.ID_CARD.get());
        card.set(mio_icif_data_components.ID_CARD.get(),
            write(d, player.getUUID(), name, player.getUUID(), name, TerminalRules.OWNER, IdCardRules.NEVER, true));
        return card;
    }

    /** True when the issuer's register holds the card (delegate cards only). */
    static boolean registered(ExchangeData d, IdCardData c) {
        List<ExchangeData.CardRecord> list = d.cards.get(c.issuer());
        if (list == null) return false;
        for (ExchangeData.CardRecord r : list) if (r.serial == c.serial() && r.holder.equals(c.holder())) return true;
        return false;
    }

    /** The check of a card for its user now. */
    public static IdCardRules.Check check(MinecraftServer server, IdCardData c, UUID user) {
        ExchangeData d = ExchangeData.get(server);
        return IdCardRules.check(secret(d), c.fields(), c.signature(), user, ExchangeService.today(server), !c.personal() && registered(d, c));
    }

    /** The highest level that valid delegate cards of {@code owner} in the player's inventory give (TerminalRules levels). */
    public static int delegatedLevel(ServerPlayer player, UUID owner) {
        if (owner == null) return TerminalRules.NONE;
        int level = TerminalRules.NONE;
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size() + 1; i++) {
            ItemStack s = i < inv.items.size() ? inv.items.get(i) : inv.offhand.get(0);
            IdCardData c = data(s);
            if (c == null || c.personal() || !owner.equals(c.issuer()) || !player.getUUID().equals(c.holder())) continue;
            level = Math.max(level, IdCardRules.delegatedLevel(check(player.server, c, player.getUUID()), c.fields(), owner));
        }
        return level;
    }

    /** The valid personal card of the player in a hand, or null. */
    public static IdCardData personalCard(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            IdCardData c = data(player.getItemInHand(hand));
            if (c != null && c.personal() && check(player.server, c, player.getUUID()) == IdCardRules.Check.OK) return c;
        }
        return null;
    }

    /**
     * The owner writes a delegate card for {@code holder}: a blank card of the owner's inventory becomes the card (level VIEW ..
     * CONFIG, {@code days} market days from today), registered under the owner.
     */
    public static Result issue(ServerPlayer owner, GameProfile holder, int level, int days) {
        if (holder.getId().equals(owner.getUUID())) return Result.fail("card.self");
        ExchangeData d = ExchangeData.get(owner.server);
        List<ExchangeData.CardRecord> list = d.cards.computeIfAbsent(owner.getUUID(), k -> new ArrayList<>());
        if (list.size() >= IdCardRules.MAX_DELEGATES) return Result.fail("card.full", IdCardRules.MAX_DELEGATES);
        var inv = owner.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.items.size() && slot < 0; i++) if (isCard(inv.items.get(i)) && data(inv.items.get(i)) == null) slot = i;
        if (slot < 0) return Result.fail("card.no_blank");
        int lv = Math.max(TerminalRules.VIEW, Math.min(TerminalRules.CONFIG, level));
        long expiry = IdCardRules.expiry(ExchangeService.today(owner.server), days);
        IdCardData c = write(d, owner.getUUID(), owner.getGameProfile().getName(), holder.getId(), holder.getName(), lv, expiry, false);
        ExchangeData.CardRecord r = new ExchangeData.CardRecord();
        r.serial = c.serial(); r.holder = holder.getId(); r.holderName = holder.getName(); r.level = lv; r.expiry = expiry;
        list.add(r);
        ItemStack blank = inv.items.get(slot);
        ItemStack card = new ItemStack(mio_icif_normal.ID_CARD.get());
        card.set(mio_icif_data_components.ID_CARD.get(), c);
        blank.shrink(1);
        if (blank.isEmpty()) inv.items.set(slot, ItemStack.EMPTY);
        if (!inv.add(card)) owner.drop(card, false);
        inv.setChanged();
        d.setDirty();
        return Result.done("card.issued", holder.getName(), expiry - ExchangeService.today(owner.server) + 1);
    }

    /** The issuer revokes a delegate card (by serial) or all of them (serial -1). Returns the cards revoked. */
    public static int revoke(MinecraftServer server, UUID issuer, long serial) {
        ExchangeData d = ExchangeData.get(server);
        List<ExchangeData.CardRecord> list = d.cards.get(issuer);
        if (list == null) return 0;
        int before = list.size();
        if (serial < 0) list.clear(); else list.removeIf(r -> r.serial == serial);
        int n = before - list.size();
        if (list.isEmpty()) d.cards.remove(issuer);
        if (n > 0) d.setDirty();
        return n;
    }

    /** The delegate cards an issuer has open. */
    public static List<ExchangeData.CardRecord> issued(MinecraftServer server, UUID issuer) {
        List<ExchangeData.CardRecord> list = ExchangeData.get(server).cards.get(issuer);
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Expired delegate cards leave the registers (called at the change of the market day). */
    static void prune(ExchangeData d, long day) {
        d.cards.values().forEach(list -> list.removeIf(r -> r.expiry < day));
        d.cards.values().removeIf(List::isEmpty);
    }
}
