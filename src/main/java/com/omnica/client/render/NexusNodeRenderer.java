package com.omnica.client.render;

import com.omnica.common.blockentity.NexusNodeBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class NexusNodeRenderer implements BlockEntityRenderer<NexusNodeBlockEntity> {
    public NexusNodeRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(NexusNodeBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockPos nodePos = entity.getPos();
        Vec3d start = new Vec3d(0.5, 0.5, 0.5);

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getLightning());

        for (BlockPos targetPos : entity.getConnections()) {
            // Avoid duplicate rendering (only render if nodePos < targetPos)
            if (nodePos.compareTo(targetPos) > 0) {
                continue;
            }

            Vec3d targetRel = new Vec3d(
                    targetPos.getX() - nodePos.getX() + 0.5,
                    targetPos.getY() - nodePos.getY() + 0.5,
                    targetPos.getZ() - nodePos.getZ() + 0.5
            );

            // Render laser beam in vibrant amethyst cyan/purple
            TetherRenderer.renderBeam(matrices, consumer, start, targetRel, 0.78f, 0.45f, 0.95f, 0.75f);
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(NexusNodeBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 64;
    }
}
