package com.mymod.bigfruit.entity;

import com.mymod.bigfruit.ai.ChickenBgmManager;
import com.mymod.bigfruit.registry.ModEntities;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class DingdongChicken extends ChickenEntity implements GeoEntity {
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.dingdong_chicken.idle");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.dingdong_chicken.walk");
    private static final RawAnimation FLAP =
            RawAnimation.begin().thenLoop("animation.dingdong_chicken.flap");
    private static final RawAnimation ASSEMBLE =
            RawAnimation.begin().thenLoop("animation.dingdong_chicken.assemble");

    /**
     * 合奏列阵中。服务端在 {@code ChickenBgmManager.tickChickens} 里写，客户端只读 ——
     * 就绪判定依赖服务端的静态 SLOTS 表，客户端本来拿不到，所以必须同步过来。
     * 这一个字段同时驱动合奏动画与脚下小法阵。
     */
    private static final TrackedData<Boolean> ASSEMBLING =
            DataTracker.registerData(DingdongChicken.class, TrackedDataHandlerRegistry.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity bgmTarget;
    private int scanCooldown;
    private boolean fromSquad;

    public DingdongChicken(EntityType<? extends ChickenEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ASSEMBLING, false);
    }

    public boolean isAssembling() {
        return this.dataTracker.get(ASSEMBLING);
    }

    public void setAssembling(boolean value) {
        if (this.dataTracker.get(ASSEMBLING) != value) {
            this.dataTracker.set(ASSEMBLING, value);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (state.isMoving()) {
                state.getController().setAnimationSpeed(
                        MathHelper.clamp(0.7 + state.getLimbSwingAmount() * 2.4, 0.7, 2.2));
                return state.setAndContinue(WALK);
            }
            state.getController().setAnimationSpeed(1.0);
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "accent", 4, state -> {
            if (this.isAssembling()) {
                return state.setAndContinue(ASSEMBLE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(FLAP);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.clear(goal -> true);
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FaceAssembleGoal(this));
        this.goalSelector.add(2, new SeekEnemyGoal(this));
        this.goalSelector.add(3, new VillageTetherGoal(this));
        this.goalSelector.add(4, new VillageWanderGoal(this));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (this.getWorld().isClient) {
            return;
        }
        if (--this.scanCooldown <= 0) {
            this.scanCooldown = 20;
            LivingEntity next = BigDogEntity.findPreferredTarget(this, true);
            if (next != this.bgmTarget) {
                this.getNavigation().stop();
            }
            this.bgmTarget = next;
            this.setTarget(this.bgmTarget);
            LivingEntity focus = this.bgmTarget;
            if (focus == null) {
                PlayerEntity player = this.getWorld().getClosestPlayer(this, 64.0);
                if (player != null && player.isAlive() && !player.isRemoved()) {
                    focus = player;
                }
            }
            ChickenBgmManager.updateChaseSpeed(this, focus);
        }
    }

    public LivingEntity getBgmTarget() {
        return this.bgmTarget;
    }

    public void forgetTarget(LivingEntity target) {
        if (target != null && this.bgmTarget == target) {
            this.bgmTarget = null;
            this.setTarget(null);
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier,
            net.minecraft.entity.damage.DamageSource damageSource) {
        return false;
    }

    @Override
    protected Identifier getLootTableId() {
        return new Identifier("minecraft", "entities/chicken");
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
            SpawnReason spawnReason, EntityData entityData, net.minecraft.nbt.NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (spawnReason == SpawnReason.NATURAL && !this.fromSquad && world instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 2; i++) {
                DingdongChicken extra = ModEntities.DINGDONG_CHICKEN.create(serverWorld);
                if (extra == null) {
                    break;
                }
                extra.fromSquad = true;
                double ox = this.getX() + (serverWorld.random.nextDouble() - 0.5) * 4.0;
                double oz = this.getZ() + (serverWorld.random.nextDouble() - 0.5) * 4.0;
                extra.refreshPositionAndAngles(ox, this.getY(), oz,
                        serverWorld.random.nextFloat() * 360.0f, 0.0f);
                extra.initialize(world, difficulty, SpawnReason.NATURAL, null, null);
                serverWorld.spawnEntity(extra);
            }
        }
        return data;
    }

    static void faceTowards(DingdongChicken chicken, LivingEntity target) {
        float want = ChickenBgmManager.angleTo(chicken, target);
        float yaw = chicken.getYaw()
                + MathHelper.clamp(MathHelper.wrapDegrees(want - chicken.getYaw()), -30.0f, 30.0f);
        chicken.setYaw(yaw);
        chicken.setHeadYaw(yaw);
    }

    static class FaceAssembleGoal extends Goal {
        private final DingdongChicken chicken;
        private Vec3d lastSlot;

        FaceAssembleGoal(DingdongChicken chicken) {
            this.chicken = chicken;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        private LivingEntity liveTarget() {
            LivingEntity target = this.chicken.getBgmTarget();
            if (target != null && target.isAlive() && !target.isRemoved()) {
                return target;
            }
            return null;
        }

        @Override
        public boolean canStart() {
            return liveTarget() != null;
        }

        @Override
        public boolean shouldContinue() {
            return liveTarget() != null;
        }

        @Override
        public void stop() {
            this.chicken.getNavigation().stop();
            this.lastSlot = null;
        }

        @Override
        public void tick() {
            LivingEntity target = liveTarget();
            if (target == null) {
                return;
            }
            this.chicken.getLookControl().lookAt(target, 30.0f, 30.0f);
            faceTowards(this.chicken, target);
            if (this.chicken.getWorld().isClient) {
                return;
            }
            ChickenBgmManager.chaseHorseless(this.chicken, target);
            ChickenBgmManager.updateChaseSpeed(this.chicken, target);
            Vec3d slot = ChickenBgmManager.getSlotFor(this.chicken.getUuid());
            if (slot == null) {
                if (this.chicken.squaredDistanceTo(target) > 36.0) {
                    this.chicken.getNavigation().startMovingTo(target, 1.0);
                } else {
                    this.chicken.getNavigation().stop();
                }
                return;
            }
            if (this.chicken.squaredDistanceTo(slot.x, slot.y, slot.z) > 2.25) {
                if (this.lastSlot == null || this.lastSlot.squaredDistanceTo(slot) > 1.0
                        || !this.chicken.getNavigation().isFollowingPath()) {
                    this.chicken.getNavigation().startMovingTo(slot.x, slot.y, slot.z, 1.0);
                    this.lastSlot = slot;
                }
            } else {
                this.chicken.getNavigation().stop();
                this.lastSlot = null;
            }
        }
    }

    static class SeekEnemyGoal extends Goal {
        private final DingdongChicken chicken;

        SeekEnemyGoal(DingdongChicken chicken) {
            this.chicken = chicken;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        private PlayerEntity foe() {
            if (this.chicken.getBgmTarget() != null) {
                return null;
            }
            PlayerEntity player = this.chicken.getWorld().getClosestPlayer(this.chicken, 128.0);
            if (player != null && player.isAlive() && !player.isRemoved()
                    && this.chicken.squaredDistanceTo(player) > 36.0) {
                return player;
            }
            return null;
        }

        @Override
        public boolean canStart() {
            return foe() != null;
        }

        @Override
        public boolean shouldContinue() {
            return foe() != null;
        }

        @Override
        public void stop() {
            this.chicken.getNavigation().stop();
        }

        @Override
        public void tick() {
            PlayerEntity player = foe();
            if (player == null) {
                return;
            }
            this.chicken.getLookControl().lookAt(player, 30.0f, 30.0f);
            faceTowards(this.chicken, player);
            if (this.chicken.getWorld().isClient) {
                return;
            }
            ChickenBgmManager.chaseHorseless(this.chicken, player);
            ChickenBgmManager.updateChaseSpeed(this.chicken, player);
            this.chicken.getNavigation().startMovingTo(player, 1.0);
        }
    }

    static class VillageTetherGoal extends Goal {
        private final DingdongChicken chicken;

        VillageTetherGoal(DingdongChicken chicken) {
            this.chicken = chicken;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        private BlockPos strayedCenter() {
            if (this.chicken.getBgmTarget() != null) {
                return null;
            }
            if (!(this.chicken.getWorld() instanceof ServerWorld serverWorld)) {
                return null;
            }
            BlockPos center = ChickenBgmManager.findVillageCenter(serverWorld, this.chicken.getBlockPos());
            if (center != null && this.chicken.squaredDistanceTo(
                    center.getX(), this.chicken.getY(), center.getZ()) > 1024.0) {
                return center;
            }
            return null;
        }

        @Override
        public boolean canStart() {
            return strayedCenter() != null;
        }

        @Override
        public boolean shouldContinue() {
            return strayedCenter() != null;
        }

        @Override
        public void stop() {
            this.chicken.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos center = strayedCenter();
            if (center == null || this.chicken.getWorld().isClient) {
                return;
            }
            this.chicken.getNavigation().startMovingTo(center.getX(), center.getY(), center.getZ(), 1.0);
        }
    }

    static class VillageWanderGoal extends Goal {
        private final DingdongChicken chicken;
        private int cooldown;

        VillageWanderGoal(DingdongChicken chicken) {
            this.chicken = chicken;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.cooldown = 40;
        }

        @Override
        public boolean canStart() {
            return this.chicken.getBgmTarget() == null && --this.cooldown <= 0;
        }

        @Override
        public boolean shouldContinue() {
            return this.chicken.getBgmTarget() == null && this.chicken.getNavigation().isFollowingPath();
        }

        @Override
        public void start() {
            this.cooldown = 80 + this.chicken.getRandom().nextInt(80);
            double dx = this.chicken.getX() + (this.chicken.getRandom().nextDouble() - 0.5) * 20.0;
            double dz = this.chicken.getZ() + (this.chicken.getRandom().nextDouble() - 0.5) * 20.0;
            if (this.chicken.getWorld() instanceof ServerWorld serverWorld) {
                BlockPos center = ChickenBgmManager.findVillageCenter(serverWorld, this.chicken.getBlockPos());
                if (center != null) {
                    dx = center.getX() + (this.chicken.getRandom().nextDouble() - 0.5) * 32.0;
                    dz = center.getZ() + (this.chicken.getRandom().nextDouble() - 0.5) * 32.0;
                }
            }
            if (!this.chicken.getWorld().isClient) {
                this.chicken.getNavigation().startMovingTo(dx, this.chicken.getY(), dz, 1.0);
            }
        }

        @Override
        public void stop() {
            this.chicken.getNavigation().stop();
        }
    }
}
