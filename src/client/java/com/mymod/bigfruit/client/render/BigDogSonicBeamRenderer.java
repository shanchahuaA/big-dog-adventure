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
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BigDogSonicBeamRenderer {
    private static final Identifier BEAM_TEX = new Identifier("minecraft", "textures/entity/beacon_beam.png");
    private static final RenderLayer BEAM_LAYER = RenderLayer.getEntityTranslucent(BEAM_TEX);
    private static final RenderLayer RING_LAYER = RenderLayer.getEntityTranslucent(BEAM_TEX);

    private BigDogSonicBeamRenderer() {}

    public static void render(BigDogEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity.getState() != BigDogEntity.State.FIRING) return;

        float progress = entity.getFiringProgress() / 100.0f;
        float dirX = entity.getFiringDirXTracked();
        float dirY = entity.getFiringDirYTracked();
        float dirZ = entity.getFiringDirZTracked();
        if (dirX == 0.0f && dirY == 0.0f && dirZ == 0.0f) {
            dirX = (float) entity.getFiringDirX();
            dirY = (float) entity.getFiringDirY();
            dirZ = (float) entity.getFiringDirZ();
        }
        double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len < 1.0e-6 || !Double.isFinite(len)) return;
        float nx = (float) (dirX / len);
        float ny = (float) (dirY / len);
        float nz = (float) (dirZ / len);
        if (!Float.isFinite(nx) || !Float.isFinite(ny) || !Float.isFinite(nz)) return;

        float beamLen = (float) BigDogEntity.RANGE;
        if (beamLen < 0.1f) return;

        matrices.push();
        matrices.translate(0.0, entity.getEyeY() - entity.getY(), 0.0);
        Quaternionf q = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f(nx, ny, nz));
        matrices.multiply(q);

        float smoothProgress = MathHelper.lerp(tickDelta, progress - 0.0125f, progress);
        renderPrismBeam(matrices, vertexConsumers, beamLen, smoothProgress, light);
        renderCoreBeam(matrices, vertexConsumers, beamLen, smoothProgress, light);
        renderHexRings(matrices, vertexConsumers, beamLen, smoothProgress, light);

        matrices.pop();
    }

    private static void renderPrismBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float length, float progress, int light) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pos = e.getPositionMatrix();
        Matrix3f norm = e.getNormalMatrix();
        VertexConsumer buf = vertexConsumers.getBuffer(BEAM_LAYER);
        int sides = 6;
        float radius = (float) BigDogEntity.HALF_WIDTH;
        for (int i = 0; i < sides; i++) {
            float a0 = (i / (float) sides) * MathHelper.TAU;
            float a1 = ((i + 1) / (float) sides) * MathHelper.TAU;
            float midA = (a0 + a1) * 0.5f;
            float x0 = MathHelper.cos(a0) * radius;
            float y0 = MathHelper.sin(a0) * radius;
            float x1 = MathHelper.cos(a1) * radius;
            float y1 = MathHelper.sin(a1) * radius;
            float nx = MathHelper.cos(midA);
            float ny = MathHelper.sin(midA);
            float phase = progress * MathHelper.TAU * 2.0f + midA;
            float h0 = MathHelper.cos(phase);
            float h1 = MathHelper.cos(phase + (float) Math.PI);
            float helix = Math.max(h0, h1);
            helix = MathHelper.clamp((helix - 0.55f) / 0.45f, 0.0f, 1.0f);
            float r = MathHelper.lerp(helix, 1.0f, 1.0f);
            float g = MathHelper.lerp(helix, 0.62f, 0.85f);
            float b = MathHelper.lerp(helix, 0.05f, 0.35f);
            float a = MathHelper.lerp(helix, 0.32f, 0.55f) * (0.85f + 0.15f * progress);
            float v0 = progress * 2.0f;
            float v1 = v0 + 1.0f;
            quadN(buf, pos, norm,
                    x0, y0, 0f, 0f, v0,
                    x1, y1, 0f, 1f, v0,
                    x1, y1, length, 1f, v1,
                    x0, y0, length, 0f, v1,
                    light, r, g, b, a, nx, ny, 0f);
        }
    }

    private static void renderCoreBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float length, float progress, int light) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pos = e.getPositionMatrix();
        Matrix3f norm = e.getNormalMatrix();
        VertexConsumer buf = vertexConsumers.getBuffer(BEAM_LAYER);
        int sides = 6;
        float radius = 0.9f;
        float innerLen = length * 0.96f;
        for (int i = 0; i < sides; i++) {
            float a0 = (i / (float) sides) * MathHelper.TAU;
            float a1 = ((i + 1) / (float) sides) * MathHelper.TAU;
            float midA = (a0 + a1) * 0.5f;
            float x0 = MathHelper.cos(a0) * radius;
            float y0 = MathHelper.sin(a0) * radius;
            float x1 = MathHelper.cos(a1) * radius;
            float y1 = MathHelper.sin(a1) * radius;
            float nx = MathHelper.cos(midA);
            float ny = MathHelper.sin(midA);
            float pulse = 0.75f + 0.25f * MathHelper.cos(progress * MathHelper.TAU * 1.5f + midA);
            float a = 0.75f * pulse;
            quadN(buf, pos, norm,
                    x0, y0, 0f, 0f, 0f,
                    x1, y1, 0f, 1f, 0f,
                    x1, y1, innerLen, 1f, 1f,
                    x0, y0, innerLen, 0f, 1f,
                    light, 1.0f, 0.92f, 0.18f, a, nx, ny, 0f);
        }
        // double helix ribbons inside core
        int helixSegs = 10;
        float helixR = 0.32f;
        float helixW = 0.10f;
        for (int h = 0; h < 2; h++) {
            float phaseOffset = h * (float) Math.PI;
            for (int s = 0; s < helixSegs; s++) {
                float t0 = (s / (float) helixSegs);
                float t1 = ((s + 1) / (float) helixSegs);
                float z0 = t0 * length * 0.88f;
                float z1 = t1 * length * 0.88f;
                float ang0 = t0 * MathHelper.TAU * 2.2f + progress * MathHelper.TAU * 2.0f + phaseOffset;
                float ang1 = t1 * MathHelper.TAU * 2.2f + progress * MathHelper.TAU * 2.0f + phaseOffset;
                float hx0 = MathHelper.cos(ang0) * helixR;
                float hy0 = MathHelper.sin(ang0) * helixR;
                float hx1 = MathHelper.cos(ang1) * helixR;
                float hy1 = MathHelper.sin(ang1) * helixR;
                float nx = MathHelper.cos((ang0 + ang1) * 0.5f);
                float ny = MathHelper.sin((ang0 + ang1) * 0.5f);
                float alpha = 0.65f * (1.0f - t0 * 0.35f);
                quadN(buf, pos, norm,
                        hx0 - ny * helixW, hy0 + nx * helixW, z0, 0f, 0f,
                        hx0 + ny * helixW, hy0 - nx * helixW, z0, 1f, 0f,
                        hx1 + ny * helixW, hy1 - nx * helixW, z1, 1f, 1f,
                        hx1 - ny * helixW, hy1 + nx * helixW, z1, 0f, 1f,
                        light, 1.0f, 0.96f, 0.35f, alpha, nx, ny, 0f);
            }
        }
    }

    private static void renderHexRings(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float length, float progress, int light) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pos = e.getPositionMatrix();
        Matrix3f norm = e.getNormalMatrix();
        VertexConsumer ringBuf = vertexConsumers.getBuffer(RING_LAYER);
        VertexConsumer arcBuf = vertexConsumers.getBuffer(RING_LAYER);
        float baseRadius = (float) BigDogEntity.HALF_WIDTH;
        float thick = 0.22f;
        int rings = Math.max(6, (int) Math.ceil(length / 5.0f));
        float uvOffset = (progress * 8.0f) % 1.0f;
        for (int i = 0; i < rings; i++) {
            float base = (i * 5.0f + (progress * 6.0f) % 5.0f);
            if (base > length || base < 0.5f) continue;
            float z = base;
            float distFade = 1.0f - base / length;
            float alphaRing = 0.78f * distFade * (0.55f + 0.45f * progress);
            float alphaArc = 0.60f * distFade * (0.6f + 0.4f * MathHelper.cos(progress * MathHelper.TAU * 1.2f + i));
            float pulse = 1.0f + 0.08f * MathHelper.sin(progress * MathHelper.TAU + i * 1.1f);
            float rInner = baseRadius * pulse;
            float rOuter = (baseRadius + thick) * pulse;
            float arcInner = rInner + 0.04f;
            float arcOuter = rOuter + 0.04f;
            for (int s = 0; s < 6; s++) {
                float a0 = (s / 6.0f) * MathHelper.TAU;
                float a1 = ((s + 1) / 6.0f) * MathHelper.TAU;
                float x0 = MathHelper.cos(a0) * rInner;
                float y0 = MathHelper.sin(a0) * rInner;
                float x1 = MathHelper.cos(a1) * rInner;
                float y1 = MathHelper.sin(a1) * rInner;
                float xo0 = MathHelper.cos(a0) * rOuter;
                float yo0 = MathHelper.sin(a0) * rOuter;
                float xo1 = MathHelper.cos(a1) * rOuter;
                float yo1 = MathHelper.sin(a1) * rOuter;
                quadN(ringBuf, pos, norm,
                        x0, y0, z, 0f, 0f,
                        x1, y1, z, 1f, 0f,
                        xo1, yo1, z, 1f, 1f,
                        xo0, yo0, z, 0f, 1f,
                        light, 1.0f, 0.78f, 0.05f, alphaRing, 0f, 0f, 1f);
                // blue-white electric arc overlay, thinner, flickering UV
                float ax0 = MathHelper.cos(a0) * arcInner;
                float ay0 = MathHelper.sin(a0) * arcInner;
                float ax1 = MathHelper.cos(a1) * arcInner;
                float ay1 = MathHelper.sin(a1) * arcInner;
                float axo0 = MathHelper.cos(a0) * arcOuter;
                float ayo0 = MathHelper.sin(a0) * arcOuter;
                float axo1 = MathHelper.cos(a1) * arcOuter;
                float ayo1 = MathHelper.sin(a1) * arcOuter;
                float uOff = uvOffset + s * 0.07f;
                quadN(arcBuf, pos, norm,
                        ax0, ay0, z, uOff, 0f,
                        ax1, ay1, z, uOff + 0.14f, 0f,
                        axo1, ayo1, z, uOff + 0.14f, 1f,
                        axo0, ayo0, z, uOff, 1f,
                        light, 0.35f, 0.60f, 1.0f, alphaArc, 0f, 0f, 1f);
            }
        }
    }

    private static void quadN(VertexConsumer buf, Matrix4f pos, Matrix3f norm,
                              float x0, float y0, float z0, float u0, float v0,
                              float x1, float y1, float z1, float u1, float v1,
                              float x2, float y2, float z2, float u2, float v2,
                              float x3, float y3, float z3, float u3, float v3,
                              int light, float r, float g, float b, float a,
                              float nx, float ny, float nz) {
        buf.vertex(pos, x0, y0, z0).color(r, g, b, a).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, nx, ny, nz).next();
        buf.vertex(pos, x1, y1, z1).color(r, g, b, a).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, nx, ny, nz).next();
        buf.vertex(pos, x2, y2, z2).color(r, g, b, a).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, nx, ny, nz).next();
        buf.vertex(pos, x3, y3, z3).color(r, g, b, a).texture(u3, v3).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(norm, nx, ny, nz).next();
    }

    private static void quad(VertexConsumer buf, Matrix4f pos, Matrix3f norm,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             int light, float r, float g, float b, float a) {
        quadN(buf, pos, norm, x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3, light, r, g, b, a, 0f, 1f, 0f);
    }
}
