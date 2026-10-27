// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 0.1.10.28 (X4): the rules of the ID card (proposal B4). Pure Java (the offline harness runs it).
 *
 * <p>A card names its issuer and its holder (UUIDs), a level, the last market day it is valid and a serial; the server signs
 * these fields with a secret of the world ({@link #sign}), so a card that was edited (another holder, a higher level, a later
 * day) no longer verifies. A card works only for its holder ("biometric": handing it to someone else gives them nothing).
 *
 * <ul>
 *   <li>Personal card ({@code personal}): issuer = holder; opens the holder's account from anywhere (orders, auction orders,
 *       mailbox) and the list of the delegate cards the holder issued.</li>
 *   <li>Delegate card: written by the owner of Trade Terminals for another player; gives that player the card's level (view,
 *       trade or configure; never owner) at all terminals of the issuer until the expiry day, unless the issuer revokes it
 *       (the issuer's register no longer holds the serial).</li>
 * </ul>
 */
public final class IdCardRules {
    private IdCardRules() {}

    /** Delegate cards an issuer may have open at once. */
    public static final int MAX_DELEGATES = 16;
    /** The longest validity of a delegate card (market days). */
    public static final int MAX_DAYS = 30;
    /** The expiry of a personal card. */
    public static final long NEVER = Long.MAX_VALUE;

    /** The signed fields of a card. */
    public record Fields(UUID issuer, UUID holder, int level, long expiryDay, long serial, boolean personal) { }

    /** Why a card does not work. */
    public enum Check { OK, BAD_SIGNATURE, NOT_HOLDER, EXPIRED, REVOKED, WRONG_ISSUER }

    /** The signature of the fields under the world secret. */
    public static long sign(long secret, Fields f) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer b = ByteBuffer.allocate(8 + 16 + 16 + 4 + 8 + 8 + 1);
            b.putLong(secret);
            b.putLong(f.issuer().getMostSignificantBits()).putLong(f.issuer().getLeastSignificantBits());
            b.putLong(f.holder().getMostSignificantBits()).putLong(f.holder().getLeastSignificantBits());
            b.putInt(f.level()).putLong(f.expiryDay()).putLong(f.serial()).put((byte) (f.personal() ? 1 : 0));
            md.update(b.array());
            md.update("mio_icif:id_card".getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(md.digest()).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 missing", e);
        }
    }

    /**
     * Checks a card for its user: the signature, the holder, the day and (delegate cards) the issuer's register. A personal
     * card is never revoked or expired.
     */
    public static Check check(long secret, Fields f, long signature, UUID user, long today, boolean registered) {
        if (f.issuer() == null || f.holder() == null || sign(secret, f) != signature) return Check.BAD_SIGNATURE;
        if (!f.holder().equals(user)) return Check.NOT_HOLDER;
        if (f.personal()) return f.issuer().equals(f.holder()) ? Check.OK : Check.BAD_SIGNATURE;
        if (today > f.expiryDay()) return Check.EXPIRED;
        if (!registered) return Check.REVOKED;
        return Check.OK;
    }

    /** The level a delegate card gives at a terminal of {@code owner} (TerminalRules levels), or NONE. */
    public static int delegatedLevel(Check check, Fields f, UUID owner) {
        if (check != Check.OK || f.personal() || !f.issuer().equals(owner)) return TerminalRules.NONE;
        return Math.max(TerminalRules.NONE, Math.min(TerminalRules.CONFIG, f.level()));
    }

    /** The expiry day of a delegate card written on {@code today} for {@code days} days (1..{@link #MAX_DAYS}). */
    public static long expiry(long today, int days) {
        return today + Math.max(1, Math.min(MAX_DAYS, days)) - 1;
    }
}
