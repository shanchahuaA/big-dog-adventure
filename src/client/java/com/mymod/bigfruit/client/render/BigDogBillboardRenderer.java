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

    private static final Identifier TEXTURE = BigFruitMod.id("textures/entity/big_dog.png");
    private static final RenderLayer LAYER = RenderLayer.getEntityCutoutNoCull(TEXTURE);

    public BigDogBillboardRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTexture(BigDogEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BigDogEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0, 0.8, 0.0);
        matrices.multiply(this.dispatcher.getRotation());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(LAYER);

        float halfW = 0.6f;
        float halfH = 0.6f;

        vertex(vertexConsumer, posMatrix, normalMatrix, -halfW, -halfH, 0, 1, light);
        vertex(vertexConsumer, posMatrix, normalMatrix, halfW, -halfH, 1, 1, light);
        vertex(vertexConsumer, posMatrix, normalMatrix, halfW, halfH, 1, 0, light);
        vertex(vertexConsumer, posMatrix, normalMatrix, -halfW, halfH, 0, 0, light);

        matrices.pop();
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
