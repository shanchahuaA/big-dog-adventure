package com.mymod.bigfruit.client.render;

import com.mymod.bigfruit.BigFruitMod;
import com.mymod.bigfruit.entity.MagicCircleEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 法阵渲染。
 * <p>普通阵（{@link MagicCircleEntity#isGrand() grand=false}）＝贴地平盘，按 {@link MagicCircleEntity#age}
 * 绽放 + 淡出，与本类旧版逐字一致。
 * <p>豪华阵（坚守者召唤）分三段：平盘 → 1.2s 起内环旋转 → 2.0s 起圆墙升起＋坚守者落地；
 * 收场按后进先出 —— 圆墙最先消失，平盘最后消失（＝实体 discard）。
 * <p>用满亮（{@code 0xF000F0}）让它不受昼夜光照影响，读起来才像魔法光。
 */
public class MagicCircleRenderer extends EntityRenderer<MagicCircleEntity> {
    private static final Identifier TEXTURE =
            BigFruitMod.id("textures/entity/magic_circle.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    // —— 豪华阵时间轴（单位 tick，与 MagicCircleEntity.GRAND_LIFE_TICKS 对齐）——
    /** 平盘绽放用时。 */
    private static final float DISC_GROW_TICKS = 6f;
    /** 内环出现并开始旋转。 */
    private static final float RING_IN_TICK = 24f;
    /** 内环涨满用时（用户说的「旋转 0.8s」＝这一段）。 */
    private static final float RING_GROW_TICKS = 6f;
    /** 圆墙开始升起（＝坚守者落地）。 */
    private static final float WALL_RISE_TICK = 40f;
    /** 圆墙升到满高用时。 */
    private static final float WALL_RISE_TICKS = 12f;
    /** 圆墙满高保持到此刻。 */
    private static final float WALL_HOLD_END_TICK = 60f;
    /** 圆墙淡出结束 —— 最后生成，最先消失。 */
    private static final float WALL_FADE_END_TICK = 68f;
    /** 内环淡出结束。 */
    private static final float RING_FADE_END_TICK = 74f;
    /** 平盘淡出结束 —— 最先生成，最后消失。 */
    private static final float DISC_FADE_END_TICK = 80f;

    private static final float WALL_HEIGHT = 2.2f;
    private static final int WALL_SEGMENTS = 24;
    /** 内环半径相对平盘的比值。 */
    private static final float RING_RADIUS_RATIO = 0.65f;
    /** 内环转速（度 / tick）。 */
    private static final float RING_SPIN_DEG_PER_TICK = 6f;
    /** 圆墙顶边的 alpha 相对底边的比值（上淡下浓）。 */
    private static final float WALL_TOP_ALPHA_RATIO = 0.25f;

    public MagicCircleRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public Identifier getTexture(MagicCircleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(MagicCircleEntity entity, float yaw, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light) {
        float life = entity.getLifeTicks();
        float age = entity.age + tickDelta;
        if (age <= 0f || age >= life) {
            return;
        }
        if (entity.isGrand()) {
            renderGrand(entity, age, matrices, vertexConsumers);
        } else {
            renderNormal(entity, age / life, matrices, vertexConsumers);
        }
    }

    /** 旧版行为逐字保留：贴地平盘，绽放 + 淡出。 */
    private void renderNormal(MagicCircleEntity entity, float t, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers) {
        float grow = MathHelper.clamp(t / 0.25f, 0f, 1f);
        grow = easeOut(grow);                           // ease-out 绽放
        float fade = t <= 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f;
        float alpha = MathHelper.clamp(fade, 0f, 1f) * MathHelper.clamp(t / 0.1f, 0f, 1f);
        if (alpha <= 0.01f) {
            return;
        }

        matrices.push();
        matrices.translate(0.0, 0.02, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));   // 立起来的四边形放平
        MatrixStack.Entry entry = matrices.peek();
        flatQuad(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)),
                entry.getPositionMatrix(), entry.getNormalMatrix(), entity.getRadius() * grow, alpha);
        matrices.pop();
    }

    /** 豪华阵：平盘 → 内环 → 圆墙，三段各自管自己的出现与消失。 */
    private void renderGrand(MagicCircleEntity entity, float age, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers) {
        float radius = entity.getRadius();

        // ① 平盘：绽放 6 tick，74→80 淡出（最后消失）。
        float discGrow = easeOut(MathHelper.clamp(age / DISC_GROW_TICKS, 0f, 1f));
        float discAlpha = age <= RING_FADE_END_TICK ? 1f
                : MathHelper.clamp((DISC_FADE_END_TICK - age) / (DISC_FADE_END_TICK - RING_FADE_END_TICK),
                        0f, 1f);
        if (discAlpha > 0.01f) {
            matrices.push();
            matrices.translate(0.0, 0.02, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
            MatrixStack.Entry entry = matrices.peek();
            flatQuad(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)),
                    entry.getPositionMatrix(), entry.getNormalMatrix(),
                    radius * discGrow, discAlpha);
            matrices.pop();
        }

        // ② 内环：24 tick 起出现并一直转到收场，68→74 淡出。
        if (age >= RING_IN_TICK && age < RING_FADE_END_TICK) {
            float ringGrow = easeOut(MathHelper.clamp((age - RING_IN_TICK) / RING_GROW_TICKS, 0f, 1f));
            float ringAlpha = age <= WALL_FADE_END_TICK ? 1f
                    : MathHelper.clamp(
                            (RING_FADE_END_TICK - age) / (RING_FADE_END_TICK - WALL_FADE_END_TICK), 0f, 1f);
            if (ringAlpha > 0.01f) {
                matrices.push();
                matrices.translate(0.0, 0.02, 0.0);
                matrices.multiply(RotationAxis.POSITIVE_Y
                        .rotationDegrees((age - RING_IN_TICK) * RING_SPIN_DEG_PER_TICK));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
                MatrixStack.Entry entry = matrices.peek();
                flatQuad(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)),
                        entry.getPositionMatrix(), entry.getNormalMatrix(),
                        radius * RING_RADIUS_RATIO * ringGrow, ringAlpha);
                matrices.pop();
            }
        }

        // ③ 圆墙：40 tick 起升起，60→68 淡出（最先消失）。抬高 1cm 躲开与平盘的共面闪烁。
        if (age >= WALL_RISE_TICK && age < WALL_FADE_END_TICK) {
            float rise = MathHelper.clamp((age - WALL_RISE_TICK) / WALL_RISE_TICKS, 0f, 1f);
            float wallAlpha = age <= WALL_HOLD_END_TICK ? rise
                    : MathHelper.clamp(
                            (WALL_FADE_END_TICK - age) / (WALL_FADE_END_TICK - WALL_HOLD_END_TICK), 0f, 1f);
            if (wallAlpha > 0.01f) {
                matrices.push();
                matrices.translate(0.0, 0.03, 0.0);
                MatrixStack.Entry entry = matrices.peek();
                cylinderWall(vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE)),
                        entry.getPositionMatrix(), entry.getNormalMatrix(),
                        radius, WALL_HEIGHT * easeOut(rise), wallAlpha);
                matrices.pop();
            }
        }
    }

    /** 当前坐标系里一个水平四边形（贴图铺满）。调用方已把它放平。 */
    private static void flatQuad(VertexConsumer buffer, Matrix4f posMatrix, Matrix3f normalMatrix,
            float r, float alpha) {
        vertex(buffer, posMatrix, normalMatrix, -r, -r, 0f, 0f, alpha);
        vertex(buffer, posMatrix, normalMatrix, r, -r, 1f, 0f, alpha);
        vertex(buffer, posMatrix, normalMatrix, r, r, 1f, 1f, alpha);
        vertex(buffer, posMatrix, normalMatrix, -r, r, 0f, 1f, alpha);
    }

    /**
     * 绕一圈 {@value #WALL_SEGMENTS} 个竖直四边形拼成圆柱侧面：底边满 alpha、顶边 ×0.25。
     * 图层 culling 已关，所以每个四边形只写一遍，两面都看得见。
     */
    private static void cylinderWall(VertexConsumer buffer, Matrix4f posMatrix, Matrix3f normalMatrix,
            float radius, float height, float alpha) {
        float topAlpha = alpha * WALL_TOP_ALPHA_RATIO;
        for (int i = 0; i < WALL_SEGMENTS; i++) {
            double a0 = (i / (double) WALL_SEGMENTS) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) WALL_SEGMENTS) * Math.PI * 2.0;
            float u0 = i / (float) WALL_SEGMENTS;
            float u1 = (i + 1) / (float) WALL_SEGMENTS;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;

            wallVertex(buffer, posMatrix, normalMatrix, x0, 0f, z0, u0, 1f, alpha);
            wallVertex(buffer, posMatrix, normalMatrix, x1, 0f, z1, u1, 1f, alpha);
            wallVertex(buffer, posMatrix, normalMatrix, x1, height, z1, u1, 0f, topAlpha);
            wallVertex(buffer, posMatrix, normalMatrix, x0, height, z0, u0, 0f, topAlpha);
        }
    }

    private static void wallVertex(VertexConsumer consumer, Matrix4f posMatrix, Matrix3f normalMatrix,
            float x, float y, float z, float u, float v, float alpha) {
        consumer.vertex(posMatrix, x, y, z)
                .color(1.0f, 1.0f, 1.0f, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .next();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f posMatrix, Matrix3f normalMatrix,
            float x, float y, float u, float v, float alpha) {
        consumer.vertex(posMatrix, x, y, 0.0f)
                .color(1.0f, 1.0f, 1.0f, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .next();
    }

    private static float easeOut(float x) {
        return 1f - (1f - x) * (1f - x);
    }
}
