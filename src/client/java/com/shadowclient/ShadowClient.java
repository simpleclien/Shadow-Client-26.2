package com.shadowclient;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
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
    private static KeyMapping freecamKey;
    private static KeyMapping flyKey;
    private static final List<BlockPos> nearbyStorage = new ArrayList<>();
    private static final List<BlockPos> nearbySpawners = new ArrayList<>();
    private static int scanTicks;
    private static final RenderPipeline THROUGH_WALLS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/block_highlight_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );
    private static final StagedVertexBuffer HIGHLIGHT_BUFFER =
            new StagedVertexBuffer(() -> "Shadow Client Highlights", RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    @Override
    public void onInitializeClient() {
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.shadowclient.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));
        freecamKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.shadowclient.freecam",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                CATEGORY
        ));
        flyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.shadowclient.fly",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new ClickGuiScreen());
            }
            while (freecamKey.consumeClick()) {
                FreecamController.toggle(client);
            }
            while (flyKey.consumeClick()) {
                if (client.player == null) continue;
                var abilities = client.player.getAbilities();
                if (!abilities.mayfly) {
                    client.player.sendOverlayMessage(Component.literal("Flight unavailable: this world/server has not granted flight."));
                    continue;
                }
                abilities.flying = !abilities.flying;
                client.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket(abilities));
                client.player.sendOverlayMessage(Component.literal(abilities.flying ? "Flight ON" : "Flight OFF"));
            }

            if (client.player == null || client.level == null) {
                FreecamController.disable(client);
                nearbyStorage.clear();
                nearbySpawners.clear();
                scanTicks = 0;
                return;
            }

            FreecamController.tick(client);

            if (++scanTicks >= 100) {
                scanTicks = 0;
                scanNearbyBlocks(client);
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
                (graphics, deltaTracker) -> renderHud(graphics, Minecraft.getInstance())
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ShadowClient::renderHighlights);

        LOGGER.info("Shadow Client 26.2 initialized. Press Right Shift to open the menu.");
    }

    private static void scanNearbyBlocks(Minecraft client) {
        nearbyStorage.clear();
        nearbySpawners.clear();

        // Client-side scan of blocks currently present in the client world. Scan is throttled to every 5 seconds; vertical range is +/-32 blocks.
        if (client.player == null || client.level == null) return;

        BlockPos origin = client.player.blockPosition();
        for (int x = -100; x <= 100; x++) {
            for (int y = -32; y <= 32; y++) {
                for (int z = -100; z <= 100; z++) {
                    if ((long) x * x + (long) y * y + (long) z * z > 10000L) continue;
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

    private static void renderHighlights(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        // Render highlights using block data currently available to this client.
        if (client.player == null || client.level == null) return;
        boolean showStorage = ModuleState.storageFinder;
        boolean showSpawners = ModuleState.spawnerFinder;
        boolean showPlayerBoxes = ModuleState.playerBoxes;
        if (!showStorage && !showSpawners && !showPlayerBoxes) return;

        RenderPipeline pipeline = THROUGH_WALLS_PIPELINE;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        PrimitiveTopology topology = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = HIGHLIGHT_BUFFER.appendDraw(
                format, topology,
                topology == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null
        );

        var matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer out = HIGHLIGHT_BUFFER.getVertexBuilder(draw);

        if (showPlayerBoxes) {
            for (var other : client.level.players()) {
                if (other == client.player || other.distanceTo(client.player) > 100.0f) continue;
                var bounds = other.getBoundingBox();
                addEntityOutline(matrices.last().pose(), out,
                        (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ,
                        (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ,
                        0.10f, 1.0f, 0.20f);
            }
        }
        if (showStorage) {
            for (BlockPos pos : nearbyStorage) {
                addBlockOutline(matrices.last().pose(), out, pos, 1f, 0.66f, 0.20f);
            }
        }
        if (showSpawners) {
            for (BlockPos pos : nearbySpawners) {
                addBlockOutline(matrices.last().pose(), out, pos, 1f, 0.28f, 0.28f);
            }
        }
        matrices.popPose();

        HIGHLIGHT_BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = HIGHLIGHT_BUFFER.getExecuteInfo(draw);
        if (info != null) drawHighlights(client, info, pipeline);
        HIGHLIGHT_BUFFER.endFrame();
    }

    // Twelve thin rectangular bars form a wireframe cube. The custom pipeline has no depth test,
    // so the bars remain visible when the target block is behind terrain.
    private static void addBlockOutline(Matrix4fc matrix, VertexConsumer out, BlockPos pos,
                                        float red, float green, float blue) {
        float x = pos.getX(), y = pos.getY(), z = pos.getZ();
        float t = 0.035f;
        // Four vertical edges.
        addBox(matrix, out, x, y, z, x + t, y + 1, z + t, red, green, blue);
        addBox(matrix, out, x + 1 - t, y, z, x + 1, y + 1, z + t, red, green, blue);
        addBox(matrix, out, x, y, z + 1 - t, x + t, y + 1, z + 1, red, green, blue);
        addBox(matrix, out, x + 1 - t, y, z + 1 - t, x + 1, y + 1, z + 1, red, green, blue);
        // Four top edges and four bottom edges.
        addBox(matrix, out, x, y + 1 - t, z, x + 1, y + 1, z + t, red, green, blue);
        addBox(matrix, out, x, y + 1 - t, z + 1 - t, x + 1, y + 1, z + 1, red, green, blue);
        addBox(matrix, out, x, y + 1 - t, z, x + t, y + 1, z + 1, red, green, blue);
        addBox(matrix, out, x + 1 - t, y + 1 - t, z, x + 1, y + 1, z + 1, red, green, blue);
        addBox(matrix, out, x, y, z, x + 1, y + t, z + t, red, green, blue);
        addBox(matrix, out, x, y, z + 1 - t, x + 1, y + t, z + 1, red, green, blue);
        addBox(matrix, out, x, y, z, x + t, y + t, z + 1, red, green, blue);
        addBox(matrix, out, x + 1 - t, y, z, x + 1, y + t, z + 1, red, green, blue);
    }

    // Green wireframe around each other player currently known to the client.
    private static void addEntityOutline(Matrix4fc m, VertexConsumer b,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float r, float g, float bl) {
        float t = 0.035f;
        addBox(m, b, x1, y1, z1, x1 + t, y2, z1 + t, r, g, bl);
        addBox(m, b, x2 - t, y1, z1, x2, y2, z1 + t, r, g, bl);
        addBox(m, b, x1, y1, z2 - t, x1 + t, y2, z2, r, g, bl);
        addBox(m, b, x2 - t, y1, z2 - t, x2, y2, z2, r, g, bl);
        addBox(m, b, x1, y2 - t, z1, x2, y2, z1 + t, r, g, bl);
        addBox(m, b, x1, y2 - t, z2 - t, x2, y2, z2, r, g, bl);
        addBox(m, b, x1, y2 - t, z1, x1 + t, y2, z2, r, g, bl);
        addBox(m, b, x2 - t, y2 - t, z1, x2, y2, z2, r, g, bl);
        addBox(m, b, x1, y1, z1, x2, y1 + t, z1 + t, r, g, bl);
        addBox(m, b, x1, y1, z2 - t, x2, y1 + t, z2, r, g, bl);
        addBox(m, b, x1, y1, z1, x1 + t, y1 + t, z2, r, g, bl);
        addBox(m, b, x2 - t, y1, z1, x2, y1 + t, z2, r, g, bl);
    }

    private static void addBox(Matrix4fc m, VertexConsumer b,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float r, float g, float bl) {
        // Front, back, left, right, top, bottom faces.
        vertex(b, m, x1, y1, z2, r, g, bl); vertex(b, m, x2, y1, z2, r, g, bl);
        vertex(b, m, x2, y2, z2, r, g, bl); vertex(b, m, x1, y2, z2, r, g, bl);
        vertex(b, m, x2, y1, z1, r, g, bl); vertex(b, m, x1, y1, z1, r, g, bl);
        vertex(b, m, x1, y2, z1, r, g, bl); vertex(b, m, x2, y2, z1, r, g, bl);
        vertex(b, m, x1, y1, z1, r, g, bl); vertex(b, m, x1, y1, z2, r, g, bl);
        vertex(b, m, x1, y2, z2, r, g, bl); vertex(b, m, x1, y2, z1, r, g, bl);
        vertex(b, m, x2, y1, z2, r, g, bl); vertex(b, m, x2, y1, z1, r, g, bl);
        vertex(b, m, x2, y2, z1, r, g, bl); vertex(b, m, x2, y2, z2, r, g, bl);
        vertex(b, m, x1, y2, z2, r, g, bl); vertex(b, m, x2, y2, z2, r, g, bl);
        vertex(b, m, x2, y2, z1, r, g, bl); vertex(b, m, x1, y2, z1, r, g, bl);
        vertex(b, m, x1, y1, z1, r, g, bl); vertex(b, m, x2, y1, z1, r, g, bl);
        vertex(b, m, x2, y1, z2, r, g, bl); vertex(b, m, x1, y1, z2, r, g, bl);
    }

    private static void vertex(VertexConsumer b, Matrix4fc m, float x, float y, float z,
                               float r, float g, float bl) {
        b.addVertex(m, x, y, z).setColor(r, g, bl, 1f);
    }

    private static void drawHighlights(Minecraft client, StagedVertexBuffer.ExecuteInfo info,
                                       RenderPipeline pipeline) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> MOD_ID + " through-wall highlights", color, Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
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

        if (ModuleState.storageFinder) {
            graphics.text(client.font, "Nearby storage:", x, y, 0xFF55CCFF, true);
            y += 12;
            if (nearbyStorage.isEmpty()) {
                graphics.text(client.font, "None found within 100 blocks", x, y, 0xFFAAAAAA, true);
                y += 12;
            } else {
                for (BlockPos pos : nearbyStorage) {
                    graphics.text(client.font, "Container " + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                            x, y, 0xFFFFFFFF, true);
                    y += 11;
                }
            }
        }

        if (ModuleState.spawnerFinder) {
            graphics.text(client.font, "Nearby spawners:", x, y, 0xFF55CCFF, true);
            y += 12;
            if (nearbySpawners.isEmpty()) {
                graphics.text(client.font, "None found within 100 blocks", x, y, 0xFFAAAAAA, true);
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
