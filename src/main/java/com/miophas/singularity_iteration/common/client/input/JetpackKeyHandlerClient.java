// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.input;

import com.miophas.singularity_iteration.common.client.mio_icif_ClientEvents;
import com.miophas.singularity_iteration.common.network.mio_icif_Network;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client key sampler; sends only changed five-bit snapshots. */
@OnlyIn(Dist.CLIENT)
public class JetpackKeyHandlerClient {
    private static int lastSent = -1;

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            lastSent = -1;
            return;
        }
        int bits = currentBits(minecraft);
        if (bits != lastSent) {
            mio_icif_Network.sendJetpackKeyState(bits);
            lastSent = bits;
        }
    }

    private static int currentBits(Minecraft minecraft) {
        int bits = 0;
        if (minecraft.options.keyJump.isDown()) bits |= 1 << 0;
        if (isBoostKeyDownClient()) bits |= 1 << 1;
        if (minecraft.options.keyUp.isDown()) bits |= 1 << 2;
        if (minecraft.options.keyShift.isDown()) bits |= 1 << 3;
        if (isModeSwitchKeyDownClient()) bits |= 1 << 4;
        return bits;
    }

    public static boolean isJumpKeyDownClient() {
        return Minecraft.getInstance().options.keyJump.isDown();
    }

    public static boolean isForwardKeyDownClient() {
        return Minecraft.getInstance().options.keyUp.isDown();
    }

    public static boolean isSneakKeyDownClient() {
        return Minecraft.getInstance().options.keyShift.isDown();
    }

    public static boolean isModeSwitchKeyDownClient() {
        return mio_icif_ClientEvents.JETPACK_MODE_KEY != null
            && mio_icif_ClientEvents.JETPACK_MODE_KEY.isDown();
    }

    public static boolean isAltKeyDownClient() {
        return mio_icif_ClientEvents.isSafetyKeyDown();
    }

    public static boolean isBoostKeyDownClient() {
        return (mio_icif_ClientEvents.BOOST_KEY != null && mio_icif_ClientEvents.BOOST_KEY.isDown())
            || Minecraft.getInstance().options.keySprint.isDown();
    }
}
