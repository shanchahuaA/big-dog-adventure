package com.mymod.bigdog.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * 合奏法阵的纯表现实体：瞬时绽放、不持久、不跟随。
 * 服务端只在「就绪边沿」和「召唤点」spawn 一个，{@link #getLifeTicks()} tick 后 discard。
 * 半径由生成方设定 —— 鸡脚下小阵约 0.8 格，召唤点大阵约 3 格。
 * 豪华阵（{@link #isGrand()}，坚守者专用）停留更久，并在前半程持续往上冒光点。
 * 客户端渲染靠 {@link Entity#age}（两端都会自增），除半径外只需同步 {@code GRAND} 这一个标志。
 */
public class MagicCircleEntity extends Entity {
    /** 普通阵（鸡脚下小阵 / 大狗召唤阵）的停留时长。 */
    public static final int NORMAL_LIFE_TICKS = 30;
    /** 豪华阵（坚守者召唤阵）的停留时长：4.0s，配合渲染器的三段演出。 */
    public static final int GRAND_LIFE_TICKS = 80;
    /** 豪华阵到这一刻为止还会冒新粒子，之后进入收场段。 */
    private static final int GRAND_PARTICLE_UNTIL = 74;

    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(MagicCircleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> GRAND =
            DataTracker.registerData(MagicCircleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public MagicCircleEntity(EntityType<? extends MagicCircleEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(RADIUS, 3.0f);
        this.dataTracker.startTracking(GRAND, false);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, radius);
    }

    public boolean isGrand() {
        return this.dataTracker.get(GRAND);
    }

    public void setGrand(boolean grand) {
        this.dataTracker.set(GRAND, grand);
    }

    public int getLifeTicks() {
        return this.isGrand() ? GRAND_LIFE_TICKS : NORMAL_LIFE_TICKS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (this.isGrand() && this.age < GRAND_PARTICLE_UNTIL && this.age % 2 == 0) {
            emitRisingParticles();
        }
        if (this.age >= this.getLifeTicks()) {
            this.discard();
        }
    }

    /** 每波 2 颗、贴着法阵平面随机散布，靠 deltaY 提供的初速往上飘。 */
    private void emitRisingParticles() {
        ServerWorld world = (ServerWorld) this.getWorld();
        for (int i = 0; i < 2; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(this.random.nextDouble()) * this.getRadius() * 0.85;
            world.spawnParticles(ParticleTypes.END_ROD,
                    this.getX() + Math.cos(angle) * dist,
                    this.getY() + 0.15,
                    this.getZ() + Math.sin(angle) * dist,
                    1, 0.0, 0.55, 0.0, 0.02);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }
}
