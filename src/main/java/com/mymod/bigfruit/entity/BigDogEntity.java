package com.mymod.bigfruit.entity;

import com.mymod.bigfruit.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BigDogEntity extends PathAwareEntity {

    public static final int CHARGING_TICKS = 122; // 6.11s bigdog.MP3
    public static final int FIRING_TICKS = 80; // 4.00s bark_long.MP3
    public static final int COOLDOWN_TICKS = 20;
    public static final int CHARGE_LOOP_INTERVAL = 122; // >= CHARGING_TICKS, single play (opencode P0)
    public static final double RANGE = 30.0; // +10 per user request
    public static final double HALF_WIDTH = 1.0;
    public static final float DAMAGE = 4.0f;
    public static final int DAMAGE_INTERVAL = 10;

    private static final TrackedData<Integer> STATE = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHARGE_PROGRESS = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public enum State {
        IDLE,
        CHARGING,
        FIRING,
        COOLDOWN
    }

    private int chargingTicksRemaining;
    private int firingTicksRemaining;
    private int cooldownTicksRemaining;
    private int chargeLoopCooldown;
    private int damageIntervalCooldown;
    private LivingEntity revengeTarget;
    private double firingDirX;
    private double firingDirZ;
    private int processedSegments;
    private final Set<BlockPos> brokenThisAttack = new HashSet<>();

    public BigDogEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.2)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new WanderAroundGoal(this, 0.8));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STATE, State.IDLE.ordinal());
        this.dataTracker.startTracking(CHARGE_PROGRESS, 0);
    }

    public State getState() {
        return State.values()[this.dataTracker.get(STATE)];
    }

    private void setState(State state) {
        this.dataTracker.set(STATE, state.ordinal());
    }

    public int getChargeProgress() {
        return this.dataTracker.get(CHARGE_PROGRESS);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);
        if (!this.getWorld().isClient && result && amount > 0) {
            if (this.getState() != State.IDLE) {
                return result;
            }
            if (this.cooldownTicksRemaining > 0) {
                return result;
            }
            var attacker = source.getAttacker();
            if (attacker instanceof LivingEntity living && living != this && living.isAlive()) {
                this.revengeTarget = living;
                this.startCharging();
            } else if (source.getSource() instanceof LivingEntity living && living != this && living.isAlive()) {
                this.revengeTarget = living;
                this.startCharging();
            }
        }
        return result;
    }

    private void startCharging() {
        if (this.getState() != State.IDLE) return;
        this.setState(State.CHARGING);
        this.chargingTicksRemaining = CHARGING_TICKS;
        this.chargeLoopCooldown = 0;
        this.getNavigation().stop();
        this.dataTracker.set(CHARGE_PROGRESS, 0);
    }

    private void startFiring() {
        this.setState(State.FIRING);
        this.firingTicksRemaining = FIRING_TICKS;
        this.damageIntervalCooldown = 0;
        this.processedSegments = 0;
        this.brokenThisAttack.clear();

        Vec3d dir;
        if (this.revengeTarget != null && this.revengeTarget.isAlive()) {
            dir = new Vec3d(
                    this.revengeTarget.getX() - this.getX(),
                    0,
                    this.revengeTarget.getZ() - this.getZ()
            );
        } else {
            dir = this.getRotationVector().multiply(1, 0, 1);
        }
        if (dir.lengthSquared() < 1.0e-6) {
            dir = new Vec3d(0, 0, 1);
        }
        dir = dir.normalize();
        this.firingDirX = dir.x;
        this.firingDirZ = dir.z;

        if (!this.getWorld().isClient) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.BIG_DOG_CALL, SoundCategory.HOSTILE, 2.0f, 1.0f);
        }
    }

    private void enterCooldown() {
        this.setState(State.COOLDOWN);
        this.cooldownTicksRemaining = COOLDOWN_TICKS;
        this.revengeTarget = null;
        this.brokenThisAttack.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }

        switch (this.getState()) {
            case IDLE -> {
                if (this.cooldownTicksRemaining > 0) {
                    this.cooldownTicksRemaining--;
                }
            }
            case CHARGING -> tickCharging();
            case FIRING -> tickFiring();
            case COOLDOWN -> {
                this.cooldownTicksRemaining--;
                if (this.cooldownTicksRemaining <= 0) {
                    this.setState(State.IDLE);
                }
            }
        }
    }

    private void tickCharging() {
        if (this.revengeTarget != null && (!this.revengeTarget.isAlive() || this.revengeTarget.isRemoved())) {
            this.cancelAttack();
            return;
        }

        this.getNavigation().stop();
        if (this.revengeTarget != null) {
            this.getLookControl().lookAt(this.revengeTarget, 30.0f, 30.0f);
        }

        if (this.chargeLoopCooldown <= 0) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.BIG_DOG_CHARGE_LOOP, SoundCategory.HOSTILE, 1.2f, 1.0f);
            this.chargeLoopCooldown = CHARGE_LOOP_INTERVAL;
        } else {
            this.chargeLoopCooldown--;
        }

        this.chargingTicksRemaining--;
        int progress = (int) ((1.0 - (double) this.chargingTicksRemaining / CHARGING_TICKS) * 100);
        this.dataTracker.set(CHARGE_PROGRESS, MathHelper.clamp(progress, 0, 100));

        if (this.chargingTicksRemaining <= 0) {
            this.startFiring();
        }
    }

    private void tickFiring() {
        this.getNavigation().stop();

        if (this.damageIntervalCooldown <= 0) {
            applySonicDamage();
            this.damageIntervalCooldown = DAMAGE_INTERVAL;
        } else {
            this.damageIntervalCooldown--;
        }

        processBlockDestruction();

        this.firingTicksRemaining--;
        if (this.firingTicksRemaining <= 0) {
            this.enterCooldown();
        }
    }

    private void cancelAttack() {
        this.chargingTicksRemaining = 0;
        this.firingTicksRemaining = 0;
        this.chargeLoopCooldown = 0;
        this.damageIntervalCooldown = 0;
        this.brokenThisAttack.clear();
        this.dataTracker.set(CHARGE_PROGRESS, 0);
        this.setState(State.IDLE);
        this.revengeTarget = null;
    }

    private void applySonicDamage() {
        Vec3d origin = this.getPos().add(0, this.getEyeY() - this.getY() - 0.3, 0);
        Vec3d dir = new Vec3d(this.firingDirX, 0, this.firingDirZ).normalize();
        Vec3d end = origin.add(dir.multiply(RANGE));

        double minX = Math.min(origin.x, end.x) - HALF_WIDTH;
        double minY = origin.y - HALF_WIDTH;
        double minZ = Math.min(origin.z, end.z) - HALF_WIDTH;
        double maxX = Math.max(origin.x, end.x) + HALF_WIDTH;
        double maxY = origin.y + HALF_WIDTH;
        double maxZ = Math.max(origin.z, end.z) + HALF_WIDTH;

        Box beamBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        List<LivingEntity> candidates = this.getWorld().getNonSpectatingEntities(LivingEntity.class, beamBox);

        for (LivingEntity target : candidates) {
            if (target == this) continue;
            if (!target.isAlive()) continue;
            if (!isInBeam(target, origin, dir)) continue;

            DamageSource source;
            try {
                source = this.getDamageSources().sonicBoom(this);
            } catch (Throwable t) {
                source = this.getDamageSources().mobAttack(this);
            }
            target.damage(source, DAMAGE);
        }
    }

    private boolean isInBeam(LivingEntity target, Vec3d origin, Vec3d dir) {
        Vec3d toTarget = new Vec3d(
                target.getX() - origin.x,
                target.getY() - origin.y,
                target.getZ() - origin.z
        );
        double proj = toTarget.dotProduct(dir);
        if (proj < 0 || proj > RANGE) return false;
        Vec3d closest = origin.add(dir.multiply(proj));
        double dx = target.getX() - closest.x;
        double dy = (target.getY() + target.getHeight() * 0.5) - closest.y;
        double dz = target.getZ() - closest.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double radius = HALF_WIDTH + target.getWidth() * 0.5;
        return distSq <= radius * radius;
    }

    private void processBlockDestruction() {
        int totalSegments = (int) RANGE;
        double progress = 1.0 - (double) this.firingTicksRemaining / FIRING_TICKS;
        int targetSegments = (int) Math.ceil(progress * totalSegments);
        targetSegments = MathHelper.clamp(targetSegments, 0, totalSegments);

        Vec3d origin = this.getPos().add(0, 0.6, 0);
        Vec3d dir = new Vec3d(this.firingDirX, 0, this.firingDirZ).normalize();

        while (this.processedSegments < targetSegments) {
            double dist = this.processedSegments + 0.5;
            Vec3d pos = origin.add(dir.multiply(dist));
            BlockPos center = BlockPos.ofFloored(pos);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) > 1) continue;
                    BlockPos p = center.add(dx, dy, 0);
                    if (Math.abs(this.firingDirX) > Math.abs(this.firingDirZ)) {
                        p = center.add(0, dy, dx);
                    }
                    if (this.brokenThisAttack.contains(p)) continue;
                    this.brokenThisAttack.add(p);
                    tryBreakBlock(p);
                }
            }
            this.processedSegments++;
        }
    }

    private void tryBreakBlock(BlockPos pos) {
        var world = this.getWorld();
        var state = world.getBlockState(pos);
        if (state.isAir()) return;
        if (state.getHardness(world, pos) < 0) return;
        float blastResistance = state.getBlock().getBlastResistance();
        // TNT strength 4 => max resist ~8.0, matches plan P1
        if (blastResistance > 8.0f) return;
        world.breakBlock(pos, false, this); // no drops, per user + opencode P1
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("BigDogState", this.getState().ordinal());
        nbt.putInt("ChargingTicks", this.chargingTicksRemaining);
        nbt.putInt("FiringTicks", this.firingTicksRemaining);
        nbt.putInt("CooldownTicks", this.cooldownTicksRemaining);
        nbt.putDouble("FiringDirX", this.firingDirX);
        nbt.putDouble("FiringDirZ", this.firingDirZ);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ChargingTicks")) this.chargingTicksRemaining = nbt.getInt("ChargingTicks");
        if (nbt.contains("FiringTicks")) this.firingTicksRemaining = nbt.getInt("FiringTicks");
        if (nbt.contains("CooldownTicks")) this.cooldownTicksRemaining = nbt.getInt("CooldownTicks");
        if (nbt.contains("FiringDirX")) this.firingDirX = nbt.getDouble("FiringDirX");
        if (nbt.contains("FiringDirZ")) this.firingDirZ = nbt.getDouble("FiringDirZ");
        if (nbt.contains("BigDogState")) {
            int ord = nbt.getInt("BigDogState");
            State[] vals = State.values();
            if (ord >= 0 && ord < vals.length) {
                this.setState(vals[ord]);
            }
        }
        if (this.getState() == State.CHARGING || this.getState() == State.FIRING) {
            this.cancelAttack();
        }
    }

    public double getFiringDirX() {
        return firingDirX;
    }

    public double getFiringDirZ() {
        return firingDirZ;
    }
}
