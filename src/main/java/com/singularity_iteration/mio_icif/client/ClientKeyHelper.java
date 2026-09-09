package com.singularity_iteration.mio_icif.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class ClientKeyHelper {

    public static boolean isJumpKeyDown() {
        try {
            Minecraft mc = Minecraft.getInstance();
            var keyJump = mc.options.keyJump;
            var field = KeyMapping.class.getDeclaredField("isDown");
            field.setAccessible(true);
            return field.getBoolean(keyJump);
        } catch (Exception e) {
            return false;
        }
    }
}