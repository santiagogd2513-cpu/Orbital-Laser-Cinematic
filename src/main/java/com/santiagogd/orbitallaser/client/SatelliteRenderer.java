package com.santiagogd.orbitallaser.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.santiagogd.orbitallaser.entity.SatelliteEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SatelliteRenderer extends EntityRenderer<SatelliteEntity> {

    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public SatelliteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(SatelliteEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {

        pose.pushPose();

        float age = entity.tickCount + partialTicks;
        pose.mulPose(Axis.YP.rotationDegrees(age * 0.8f));

        float charge = entity.getCharge();
        boolean firing = entity.isFiring();
        float glow = 0.55f + charge * 0.45f + (firing ? 0.4f : 0f);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE));

        // Central body
        pose.pushPose();
        pose.scale(2.8f, 1.6f, 2.8f);
        drawBox(pose, consumer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                0.75f * glow, 0.85f * glow, 1.0f * glow, 0.95f);
        pose.popPose();

        // Core
        pose.pushPose();
        pose.scale(1.4f + charge * 0.6f, 1.4f + charge * 0.6f, 1.4f + charge * 0.6f);
        float coreR = firing ? 1.0f : 0.4f + charge * 0.6f;
        float coreG = firing ? 0.3f : 0.7f;
        float coreB = firing ? 0.1f : 1.0f;
        drawBox(pose, consumer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                coreR, coreG, coreB, 0.85f);
        pose.popPose();

        // Solar panels
        pose.pushPose();
        pose.translate(-4.2f, 0, 0);
        pose.scale(5.5f, 0.15f, 2.4f);
        drawBox(pose, consumer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                0.15f, 0.25f, 0.55f, 0.9f);
        pose.popPose();

        pose.pushPose();
        pose.translate(4.2f, 0, 0);
        pose.scale(5.5f, 0.15f, 2.4f);
        drawBox(pose, consumer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                0.15f, 0.25f, 0.55f, 0.9f);
        pose.popPose();

        // Antenna
        pose.pushPose();
        pose.translate(0, 1.6f, 0);
        pose.scale(0.6f, 1.8f, 0.6f);
        drawBox(pose, consumer, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f,
                0.7f, 0.7f, 0.75f, 0.95f);
        pose.popPose();

        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    private void drawBox(PoseStack pose, VertexConsumer consumer,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float r, float g, float b, float a) {

        Matrix4f mat = pose.last().pose();
        int light = 0xF000F0;

        vertex(consumer, mat, x1, y1, z1, r, g, b, a, light);
        vertex(consumer, mat, x2, y1, z1, r, g, b, a, light);
        vertex(consumer, mat, x2, y1, z2, r, g, b, a, light);
        vertex(consumer, mat, x1, y1, z2, r, g, b, a, light);

        vertex(consumer, mat, x1, y2, z1, r, g, b, a, light);
        vertex(consumer, mat, x1, y2, z2, r, g, b, a, light);
        vertex(consumer, mat, x2, y2, z2, r, g, b, a, light);
        vertex(consumer, mat, x2, y2, z1, r, g, b, a, light);

        vertex(consumer, mat, x1, y1, z1, r, g, b, a, light);
        vertex(consumer, mat, x1, y2, z1, r, g, b, a, light);
        vertex(consumer, mat, x2, y2, z1, r, g, b, a, light);
        vertex(consumer, mat, x2, y1, z1, r, g, b, a, light);

        vertex(consumer, mat, x2, y1, z1, r, g, b, a, light);
        vertex(consumer, mat, x2, y2, z1, r, g, b, a, light);
        vertex(consumer, mat, x2, y2, z2, r, g, b, a, light);
        vertex(consumer, mat, x2, y1, z2, r, g, b, a, light);

        vertex(consumer, mat, x2, y1, z2, r, g, b, a, light);
        vertex(consumer, mat, x2, y2, z2, r, g, b, a, light);
        vertex(consumer, mat, x1, y2, z2, r, g, b, a, light);
        vertex(consumer, mat, x1, y1, z2, r, g, b, a, light);

        vertex(consumer, mat, x1, y1, z2, r, g, b, a, light);
        vertex(consumer, mat, x1, y2, z2, r, g, b, a, light);
        vertex(consumer, mat, x1, y2, z1, r, g, b, a, light);
        vertex(consumer, mat, x1, y1, z1, r, g, b, a, light);
    }

    private void vertex(VertexConsumer consumer, Matrix4f mat,
                        float x, float y, float z,
                        float r, float g, float b, float a, int light) {
        consumer.vertex(mat, x, y, z)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteEntity entity) {
        return WHITE;
    }
}
