package com.mymod.bigfruit.entity;

import com.mymod.bigfruit.registry.ModEntities;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.EnumSet;

public class DingdongChicken extends ChickenEntity {
    private LivingEntity bgmTarget;
    private int scanCooldown;
    private boolean fromSquad;

    public DingdongChicken(EntityType<? extends ChickenEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new FollowBgmTargetGoal(this));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (this.getWorld().isClient) {
            return;
        }
        if (--this.scanCooldown <= 0) {
            this.scanCooldown = 20;
            this.bgmTarget = BigDogEntity.findPreferredTarget(this);
            this.setTarget(this.bgmTarget);
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

    static class FollowBgmTargetGoal extends Goal {
        private final DingdongChicken chicken;

        FollowBgmTargetGoal(DingdongChicken chicken) {
            this.chicken = chicken;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        private PlayerEntity targetPlayer() {
            LivingEntity t = this.chicken.getBgmTarget();
            if (t instanceof PlayerEntity player && player.isAlive() && !player.isRemoved()) {
                return player;
            }
            return null;
        }

        @Override
        public boolean canStart() {
            PlayerEntity player = targetPlayer();
            return player != null && this.chicken.squaredDistanceTo(player) > 36.0;
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public void tick() {
            PlayerEntity player = targetPlayer();
            if (player == null) {
                return;
            }
            this.chicken.getLookControl().lookAt(player, 10.0f, 10.0f);
            if (this.chicken.getWorld().isClient) {
                return;
            }
            double distSq = this.chicken.squaredDistanceTo(player);
            if (distSq > 32.0 * 32.0) {
                BlockPos base = player.getBlockPos();
                for (int i = 0; i < 10; i++) {
                    int x = base.getX() + this.chicken.getRandom().nextInt(7) - 3;
                    int z = base.getZ() + this.chicken.getRandom().nextInt(7) - 3;
                    int y = base.getY() + this.chicken.getRandom().nextInt(3) - 1;
                    BlockPos pos = new BlockPos(x, y, z);
                    var world = this.chicken.getWorld();
                    if (world.isAir(pos) && world.isAir(pos.up())
                            && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        this.chicken.refreshPositionAndAngles(x + 0.5, y, z + 0.5,
                                this.chicken.getYaw(), 0.0f);
                        this.chicken.getNavigation().stop();
                        return;
                    }
                }
            } else if (distSq > 36.0) {
                this.chicken.getNavigation().startMovingTo(player, 1.0);
            } else {
                this.chicken.getNavigation().stop();
            }
        }
    }
}
