package com.shadowclient;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ShadowClient implements ClientModInitializer {
    public static final String MOD_ID = "shadowclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static KeyMapping openMenu;
    private static final List<BlockPos> nearbyStorage = new ArrayList<>();
    private static final List<BlockPos> nearbySpawners = new ArrayList<>();
    private static int scanTicks;

    @Override
    public void onInitializeClient() {
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.shadowclient.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new ClickGuiScreen());
            }

            if (client.player == null || client.level == null) {
                nearbyStorage.clear();
                nearbySpawners.clear();
                scanTicks = 0;
                return;
            }

            if (++scanTicks >= 20) {
                scanTicks = 0;
                scanNearbyBlocks(client);
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
                (graphics, deltaTracker) -> renderHud(graphics, Minecraft.getInstance())
        );

        LOGGER.info("Shadow Client 26.2 initialized. Press Right Shift to open the menu.");
    }

    private static void scanNearbyBlocks(Minecraft client) {
        nearbyStorage.clear();
        nearbySpawners.clear();

        // World-finder modules are deliberately limited to an integrated singleplayer world.
        if (!(client.getSingleplayerServer() != null) || client.player == null || client.level == null) return;

        BlockPos origin = client.player.blockPosition();
        for (int x = -16; x <= 16; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -16; z <= 16; z++) {
                    BlockPos pos = new BlockPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    var blockState = client.level.getBlockState(pos);
                    if (blockState.is(Blocks.SPAWNER) && nearbySpawners.size() < 5) {
                        nearbySpawners.add(pos);
                    }
                    if ((blockState.is(Blocks.CHEST)
                            || blockState.is(Blocks.TRAPPED_CHEST)
                            || blockState.is(Blocks.BARREL)
                            || blockState.getBlock() instanceof ShulkerBoxBlock)
                            && nearbyStorage.size() < 8) {
                        nearbyStorage.add(pos);
                    }
                }
            }
        }

        nearbyStorage.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(client.player.position())));
        nearbySpawners.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(client.player.position())));
    }

    private static void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft client) {
        if (client.player == null || client.level == null) return;

        int x = 8;
        int y = 8;
        if (ModuleState.moduleList) {
            graphics.text(client.font, "SHADOW CLIENT", x, y, 0xFF55CCFF, true);
            y += 12;
        }

        if (ModuleState.visiblePlayers) {
            graphics.text(client.font, "Visible players:", x, y, 0xFF55CCFF, true);
            y += 12;
            int shown = 0;
            for (var other : client.level.players()) {
                if (other == client.player || !client.player.hasLineOfSight(other)) continue;
                double distance = client.player.distanceTo(other);
                if (distance > 48.0) continue;
                graphics.text(client.font, other.getName().getString() + "  " + Math.round(distance) + "m",
                        x, y, 0xFFFFFFFF, true);
                y += 11;
                if (++shown >= 6) break;
            }
            if (shown == 0) {
                graphics.text(client.font, "No visible players nearby", x, y, 0xFFAAAAAA, true);
                y += 12;
            }
        }

        if (ModuleState.coordinates) {
            BlockPos pos = client.player.blockPosition();
            graphics.text(client.font, "XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                    x, y, 0xFFFFFFFF, true);
            y += 12;
        }

        if (ModuleState.fps) {
            graphics.text(client.font, "FPS: " + client.getFps(), x, y, 0xFFFFFFFF, true);
            y += 12;
        }

        if (ModuleState.armorStatus) {
            graphics.text(client.font, "Armor: " + client.player.getArmorValue(), x, y, 0xFFFFFFFF, true);
            y += 12;
        }

        if (ModuleState.keystrokes) {
            String keys = (client.options.keyUp.isDown() ? "W" : "-") + " "
                    + (client.options.keyLeft.isDown() ? "A" : "-") + " "
                    + (client.options.keyDown.isDown() ? "S" : "-") + " "
                    + (client.options.keyRight.isDown() ? "D" : "-");
            graphics.text(client.font, "Keys: " + keys, x, y, 0xFFFFFFFF, true);
            y += 12;
        }

        if ((client.getSingleplayerServer() != null) && ModuleState.storageFinder) {
            graphics.text(client.font, "Nearby storage:", x, y, 0xFF55CCFF, true);
            y += 12;
            if (nearbyStorage.isEmpty()) {
                graphics.text(client.font, "None found within 16 blocks", x, y, 0xFFAAAAAA, true);
                y += 12;
            } else {
                for (BlockPos pos : nearbyStorage) {
                    graphics.text(client.font, "Container " + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                            x, y, 0xFFFFFFFF, true);
                    y += 11;
                }
            }
        }

        if ((client.getSingleplayerServer() != null) && ModuleState.spawnerFinder) {
            graphics.text(client.font, "Nearby spawners:", x, y, 0xFF55CCFF, true);
            y += 12;
            if (nearbySpawners.isEmpty()) {
                graphics.text(client.font, "None found within 16 blocks", x, y, 0xFFAAAAAA, true);
                y += 12;
            } else {
                for (BlockPos pos : nearbySpawners) {
                    graphics.text(client.font, "Spawner " + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                            x, y, 0xFFFFCC55, true);
                    y += 11;
                }
            }
        }
    }
}
