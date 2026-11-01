package com.omnica.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.omnica.common.packet.TetherTransferPayload;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
    private static final ItemStackRenderState SHARED_ITEM_RENDER_STATE = new ItemStackRenderState();

    public record InFlightTransfer(
            Vec3 start,
            Vec3 end,
            ItemStack item,
            int totalTicks,
            long startWorldTime
    ) {}

    public static void addTransfer(TetherTransferPayload payload) {
        Vec3 start = Vec3.atCenterOf(payload.from());
        Vec3 end = Vec3.atCenterOf(payload.to());
        Minecraft client = Minecraft.getInstance();
        long currentTime = client.level != null ? client.level.getLevelData().getGameTime() : 0;
        ACTIVE_TRANSFERS.add(new InFlightTransfer(start, end, payload.item(), payload.durationTicks(), currentTime));
    }

    public static void renderWorld(LevelRenderContext context) {
        if (!com.omnica.common.config.OmnicaConfig.getInstance().renderFloatingItems) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        PoseStack matrices = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        Vec3 cameraPos = client.gameRenderer.mainCamera().position();
        float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        long worldTime = client.level.getLevelData().getGameTime();

        // Render In-Flight Items
        if (!ACTIVE_TRANSFERS.isEmpty()) {
            matrices.pushPose();
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
                Vec3 currentPos = transfer.start().lerp(transfer.end(), progress);
                double arcHeight = Math.sin(progress * Math.PI) * 0.5;
                Vec3 renderPos = currentPos.add(0, arcHeight, 0);

                matrices.pushPose();
                matrices.translate(renderPos.x, renderPos.y, renderPos.z);
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.rotateDegrees(Axis.YP, (float) (elapsed * 20.0));

                SHARED_ITEM_RENDER_STATE.clear();
                client.getItemModelResolver().updateForTopItem(
                        SHARED_ITEM_RENDER_STATE,
                        transfer.item(),
                        ItemDisplayContext.FIXED,
                        client.level,
                        null,
                        0
                );
                SHARED_ITEM_RENDER_STATE.submit(matrices, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);

                matrices.popPose();
            }

            matrices.popPose();
        }
    }

    /**
     * Helper to render a luminous laser beam between two points.
     */
    public static void renderBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, float r, float g, float b, float a) {
        Vec3 dir = end.subtract(start);
        float length = (float) dir.length();
        if (length < 0.01f) return;

        Matrix4f mat = new Matrix4f(pose.pose());
        mat.translate((float) start.x, (float) start.y, (float) start.z);

        // Normalize direction and rotate beam matrix
        Vec3 norm = dir.normalize();
        float pitch = (float) Math.asin(-norm.y);
        float yaw = (float) Math.atan2(norm.x, norm.z);

        mat.rotate(Axis.YP.rotation(yaw));
        mat.rotate(Axis.XP.rotation(pitch));

        float radius = 0.04f;

        // Draw quad cross-section
        drawBeamQuad(consumer, mat, -radius, 0, radius, length, r, g, b, a);
        drawBeamQuad(consumer, mat, 0, -radius, length, radius, r, g, b, a);
    }

    private static void drawBeamQuad(VertexConsumer consumer, Matrix4f mat, float x0, float y0, float x1, float length, float r, float g, float b, float a) {
        consumer.addVertex(mat, x0, y0, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
        consumer.addVertex(mat, x1, y0, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
        consumer.addVertex(mat, x1, y0, length).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
        consumer.addVertex(mat, x0, y0, length).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
    }
}
