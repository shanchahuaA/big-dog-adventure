package com.mymod.bigfruit.client.render;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.entity.BigDogEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BigDogBillboardRenderer extends EntityRenderer<BigDogEntity> {

    private static final Identifier TEXTURE_NOBARK = BigFruitMod.id("textures/entity/nobark.png");
    private static final Identifier TEXTURE_BARK = BigFruitMod.id("textures/entity/bark.png");
    private static final RenderLayer LAYER_NOBARK = RenderLayer.getEntityTranslucent(TEXTURE_NOBARK);
    private static final RenderLayer LAYER_BARK = RenderLayer.getEntityTranslucent(TEXTURE_BARK);

    public BigDogBillboardRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTexture(BigDogEntity entity) {
        return entity.getState() == BigDogEntity.State.FIRING ? TEXTURE_BARK : TEXTURE_NOBARK;
    }

    @Override
    public void render(BigDogEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0, 0.8, 0.0);
        matrices.multiply(this.dispatcher.getRotation());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        RenderLayer layer = entity.getState() == BigDogEntity.State.FIRING ? LAYER_BARK : LAYER_NOBARK;
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);

        float halfW = 0.6f;
        float halfH = 0.6f;

        vertex(vertexConsumer, posMatrix, normalMatrix, -halfW, -halfH, 0, 1, light);
        vertex(vertexConsumer, posMatrix, normalMatrix, halfW, -halfH, 1, 1, light);
        vertex(vertexConsumer, posMatrix, normalMatrix, halfW, halfH, 1, 0, light);
        vertex(vertexConsumer, posMatrix, normalMatrix, -halfW, halfH, 0, 0, light);

        matrices.pop();
        // Sonic beam (yellow + rings) when firing
        if (entity.getState() == BigDogEntity.State.FIRING) {
            BigDogSonicBeamRenderer.render(entity, tickDelta, matrices, vertexConsumers, light);
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f posMatrix, Matrix3f normalMatrix,
                               float x, float y, float u, float v, int light) {
        consumer.vertex(posMatrix, x, y, 0.0f)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .next();
    }
}
