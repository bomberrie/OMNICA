package com.omnica.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import com.omnica.common.packet.TetherTransferPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles client-side laser tether rendering and floating item animations.
 * Provides the visual feedback of item transport without creating actual server entities.
 */
public class TetherRenderer {
    private static final List<InFlightTransfer> ACTIVE_TRANSFERS = new CopyOnWriteArrayList<>();

    public record InFlightTransfer(
            Vec3d start,
            Vec3d end,
            ItemStack item,
            int totalTicks,
            long startWorldTime
    ) {}

    public static void addTransfer(TetherTransferPayload payload) {
        Vec3d start = payload.from().toCenterPos();
        Vec3d end = payload.to().toCenterPos();
        long currentTime = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getTime() : 0;
        ACTIVE_TRANSFERS.add(new InFlightTransfer(start, end, payload.item(), payload.durationTicks(), currentTime));
    }

    public static void renderWorld(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);
        long worldTime = client.world.getTime();

        // 1. Render In-Flight Items
        if (!ACTIVE_TRANSFERS.isEmpty()) {
            matrices.push();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            Iterator<InFlightTransfer> it = ACTIVE_TRANSFERS.iterator();
            while (it.hasNext()) {
                InFlightTransfer transfer = it.next();
                double elapsed = (worldTime - transfer.startWorldTime()) + tickDelta;
                double progress = elapsed / Math.max(1, transfer.totalTicks());

                if (progress >= 1.0) {
                    ACTIVE_TRANSFERS.remove(transfer);
                    continue;
                }

                // Smooth interpolation with slight arc
                Vec3d currentPos = transfer.start().lerp(transfer.end(), progress);
                double arcHeight = Math.sin(progress * Math.PI) * 0.5;
                Vec3d renderPos = currentPos.add(0, arcHeight, 0);

                matrices.push();
                matrices.translate(renderPos.x, renderPos.y, renderPos.z);
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (elapsed * 20.0)));

                VertexConsumerProvider consumers = context.consumers();
                if (consumers != null) {
                    client.getItemRenderer().renderItem(
                            transfer.item(),
                            ModelTransformationMode.FIXED,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE,
                            OverlayTexture.DEFAULT_UV,
                            matrices,
                            consumers,
                            client.world,
                            0
                    );
                }
                matrices.pop();
            }

            matrices.pop();
        }
    }

    /**
     * Helper to render a luminous laser beam between two points.
     */
    public static void renderBeam(MatrixStack matrices, VertexConsumer consumer, Vec3d start, Vec3d end, float r, float g, float b, float a) {
        Vec3d dir = end.subtract(start);
        float length = (float) dir.length();
        if (length < 0.01f) return;

        matrices.push();
        matrices.translate(start.x, start.y, start.z);

        // Normalize direction and rotate beam matrix
        Vec3d norm = dir.normalize();
        float pitch = (float) Math.asin(-norm.y);
        float yaw = (float) Math.atan2(norm.x, norm.z);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(pitch));

        Matrix4f mat = matrices.peek().getPositionMatrix();
        float radius = 0.04f;

        // Draw quad cross-section
        drawBeamQuad(consumer, mat, -radius, 0, radius, length, r, g, b, a);
        drawBeamQuad(consumer, mat, 0, -radius, length, radius, r, g, b, a);

        matrices.pop();
    }

    private static void drawBeamQuad(VertexConsumer consumer, Matrix4f mat, float x0, float y0, float x1, float length, float r, float g, float b, float a) {
        consumer.vertex(mat, x0, y0, 0).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
        consumer.vertex(mat, x1, y0, 0).color(r, g, b, a).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
        consumer.vertex(mat, x1, y0, length).color(r, g, b, a).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
        consumer.vertex(mat, x0, y0, length).color(r, g, b, a).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
    }
}
