package com.shadowclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ShadowClient implements ClientModInitializer {
    public static final String MOD_ID = "shadowclient";
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.shadowclient.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(new ClickGuiScreen());
                }
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
                (graphics, deltaTracker) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player == null || client.level == null) return;

                    int y = 8;
                    if (ModuleState.moduleList) {
                        graphics.text(client.font, "SHADOW CLIENT", 8, y, 0xFF55CCFF, true);
                        y += 12;
                        if (ModuleState.visiblePlayers) {
                            graphics.text(client.font, "Visible Player List: ON", 8, y, 0xFFFFFFFF, true);
                            y += 12;
                        }
                    }

                    if (ModuleState.visiblePlayers) {
                        graphics.text(client.font, "Visible players:", 8, y, 0xFF55CCFF, true);
                        y += 12;
                        int shown = 0;
                        for (var other : client.level.players()) {
                            if (other == client.player || !client.player.hasLineOfSight(other)) continue;
                            double distance = client.player.distanceTo(other);
                            if (distance > 48.0) continue;
                            graphics.text(client.font,
                                    other.getName().getString() + "  " + Math.round(distance) + "m",
                                    8, y, 0xFFFFFFFF, true);
                            y += 11;
                            if (++shown >= 6) break;
                        }
                        if (shown == 0) {
                            graphics.text(client.font, "No visible players nearby", 8, y, 0xFFAAAAAA, true);
                        }
                    }
                }
        );

        ShadowClient.LOGGER.info("Shadow Client 26.2 initialized. Press Right Shift to open the menu.");
    }
}
