package com.mymod.bigfruit.client.render;

import com.mymod.bigfruit.entity.BigDogEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class BigDogSonicBeamRenderer {
    private static final Identifier BEAM_TEX = new Identifier("minecraft", "textures/entity/beacon_beam.png");
    private static final RenderLayer BEAM_LAYER = RenderLayer.getBeaconBeam(BEAM_TEX, true);

    private BigDogSonicBeamRenderer() {}

    public static void render(BigDogEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity.getState() != BigDogEntity.State.FIRING) return;

        float progress = entity.getFiringProgress() / 100.0f;
        float dirX = entity.getFiringDirXTracked();
        float dirZ = entity.getFiringDirZTracked();
        if (Math.abs(dirX) < 0.001f && Math.abs(dirZ) < 0.001f) {
            dirX = (float) entity.getFiringDirX();
            dirZ = (float) entity.getFiringDirZ();
        }
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 0.001) return;
        dirX /= len;
        dirZ /= len;

        float yaw = (float) Math.toDegrees(Math.atan2(dirZ, dirX)) - 90.0f;
        float beamLen = 30.0f; // full length throughout FIRING (not progress*30)
        if (beamLen < 0.1f) return;

        matrices.push();
        matrices.translate(0.0, 0.7, 0.0); // match server eyeY -0.3 (~0.7)
        matrices.multiply(new org.joml.Quaternionf().rotateY((float) Math.toRadians(-yaw)));

        renderBeam(matrices, vertexConsumers, beamLen, light);
        // Use tickDelta for smooth ring progress
        float smoothProgress = MathHelper.lerp(tickDelta, progress - 0.0125f, progress);
        renderRings(matrices, vertexConsumers, beamLen, smoothProgress, light);

        matrices.pop();
    }

    private static void renderBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float length, int light) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pos = e.getPositionMatrix();
        Matrix3f norm = e.getNormalMatrix();
        VertexConsumer buf = vertexConsumers.getBuffer(BEAM_LAYER);
        float hw = 0.32f;
        float hh = 0.32f;
        // Quad 1: horizontal plane y=0, extends along Z
        quad(buf, pos, norm,
                -hw, 0f, 0f, 0f, 0f,
                hw, 0f, 0f, 1f, 0f,
                hw, 0f, length, 1f, 1f,
                -hw, 0f, length, 0f, 1f,
                light, 0.98f, 0.88f, 0.18f, 0.52f);
        // Quad 2: vertical plane x=0, extends along Z, crossed
        quad(buf, pos, norm,
                0f, -hh, 0f, 0f, 0f,
                0f, hh, 0f, 1f, 0f,
                0f, hh, length, 1f, 1f,
                0f, -hh, length, 0f, 1f,
                light, 0.98f, 0.88f, 0.18f, 0.42f);
    }

    private static void renderRings(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float length, float progress, int light) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pos = e.getPositionMatrix();
        Matrix3f norm = e.getNormalMatrix();
        VertexConsumer buf = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BEAM_TEX));
        float radius = 1.5f; // match HALF_WIDTH
        float thick = 0.18f;
        int rings = 6;
        for (int i = 0; i < rings; i++) {
            float base = (i * 5.0f + (progress * 6.0f) % 5.0f);
            if (base > length || base < 0.5f) continue;
            float z = base;
            float alpha = 0.72f * (1.0f - base / length) * (0.55f + 0.45f * progress);
            for (int s = 0; s < 16; s++) {
                float a0 = (s / 16.0f) * MathHelper.TAU;
                float a1 = ((s + 1) / 16.0f) * MathHelper.TAU;
                float x0 = MathHelper.cos(a0) * radius;
                float y0 = MathHelper.sin(a0) * radius;
                float x1 = MathHelper.cos(a1) * radius;
                float y1 = MathHelper.sin(a1) * radius;
                float xo0 = MathHelper.cos(a0) * (radius + thick);
                float yo0 = MathHelper.sin(a0) * (radius + thick);
                float xo1 = MathHelper.cos(a1) * (radius + thick);
                float yo1 = MathHelper.sin(a1) * (radius + thick);
                quad(buf, pos, norm,
                        x0, y0, z, 0f, 0f,
                        x1, y1, z, 1f, 0f,
                        xo1, yo1, z, 1f, 1f,
                        xo0, yo0, z, 0f, 1f,
                        light, 1.0f, 0.92f, 0.22f, alpha);
            }
        }
    }

    private static void quad(VertexConsumer buf, Matrix4f pos, Matrix3f norm,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             int light, float r, float g, float b, float a) {
        buf.vertex(pos, x0, y0, z0).color(r, g, b, a).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, 0f, 1f, 0f).next();
        buf.vertex(pos, x1, y1, z1).color(r, g, b, a).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, 0f, 1f, 0f).next();
        buf.vertex(pos, x2, y2, z2).color(r, g, b, a).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, 0f, 1f, 0f).next();
        buf.vertex(pos, x3, y3, z3).color(r, g, b, a).texture(u3, v3).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, 0f, 1f, 0f).next();
    }
}
