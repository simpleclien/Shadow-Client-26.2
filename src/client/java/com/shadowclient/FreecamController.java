package com.shadowclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Detached camera movement for local singleplayer worlds only. */
public final class FreecamController {
    private static boolean enabled;
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static Vec3 playerStart = Vec3.ZERO;
    private static boolean previousNoGravity;

    private FreecamController() {}

    public static boolean isEnabled() { return enabled; }
    public static Vec3 getCameraPosition() { return cameraPosition; }

    public static void toggle(Minecraft client) {
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null) return;
        if (enabled) {
            disable(client);
            return;
        }
        enabled = true;
        playerStart = client.player.position();
        cameraPosition = client.player.getEyePosition();
        previousNoGravity = client.player.isNoGravity();
        client.player.setNoGravity(true);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Freecam ON"));
    }

    public static void tick(Minecraft client) {
        if (!enabled) return;
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
            disable(client);
            return;
        }

        var player = client.player;
        player.setPos(playerStart.x, playerStart.y, playerStart.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);

        float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 movement = Vec3.ZERO;

        if (client.options.keyUp.isDown()) movement = movement.add(forward);
        if (client.options.keyDown.isDown()) movement = movement.subtract(forward);
        if (client.options.keyRight.isDown()) movement = movement.add(right);
        if (client.options.keyLeft.isDown()) movement = movement.subtract(right);
        if (client.options.keyJump.isDown()) movement = movement.add(0, 1, 0);
        if (client.options.keyShift.isDown()) movement = movement.add(0, -1, 0);

        if (movement.lengthSqr() > 0) cameraPosition = cameraPosition.add(movement.normalize().scale(0.65));
    }

    public static void disable(Minecraft client) {
        if (!enabled) return;
        enabled = false;
        if (client.player != null) {
            client.player.setPos(playerStart.x, playerStart.y, playerStart.z);
            client.player.setDeltaMovement(Vec3.ZERO);
            client.player.setNoGravity(previousNoGravity);
            client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Freecam OFF"));
        }
        cameraPosition = Vec3.ZERO;
        playerStart = Vec3.ZERO;
    }
}
