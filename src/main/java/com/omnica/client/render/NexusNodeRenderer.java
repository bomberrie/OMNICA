package com.omnica.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.omnica.common.blockentity.NexusNodeBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class NexusNodeRenderer implements BlockEntityRenderer<NexusNodeBlockEntity, NexusNodeRenderer.NexusNodeRenderState> {
    public static class NexusNodeRenderState extends BlockEntityRenderState {
        public final List<BlockPos> connections = new ArrayList<>();
    }

    public NexusNodeRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public NexusNodeRenderState createRenderState() {
        return new NexusNodeRenderState();
    }

    @Override
    public void extractRenderState(NexusNodeBlockEntity entity, NexusNodeRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.connections.clear();
        state.connections.addAll(entity.getConnections());
    }

    @Override
    public void submit(NexusNodeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        BlockPos nodePos = state.blockPos;
        if (nodePos == null) return;
        Vec3 start = new Vec3(0.5, 0.5, 0.5);

        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
            for (BlockPos targetPos : state.connections) {
                // Avoid duplicate rendering (only render if nodePos < targetPos)
                if (nodePos.compareTo(targetPos) > 0) {
                    continue;
                }

                Vec3 targetRel = new Vec3(
                        targetPos.getX() - nodePos.getX() + 0.5,
                        targetPos.getY() - nodePos.getY() + 0.5,
                        targetPos.getZ() - nodePos.getZ() + 0.5
                );

                // Render laser beam in vibrant amethyst cyan/purple
                TetherRenderer.renderBeam(pose, consumer, start, targetRel, 0.78f, 0.45f, 0.95f, 0.75f);
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
