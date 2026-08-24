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
    public static final double RANGE = 50.0;
    public static final double HALF_WIDTH = 3.0;
    public static final float DAMAGE = 6.0f;
    public static final int DAMAGE_INTERVAL = 10;
    public static final int BLOCK_BREAK_DURATION_TICKS = 50; // 50格在50t内跑完（1.0段/tick，RANGE 50）

    private static final TrackedData<Integer> STATE = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHARGE_PROGRESS = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> FIRING_DIR_X = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> FIRING_DIR_Y = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> FIRING_DIR_Z = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> FIRING_PROGRESS = DataTracker.registerData(BigDogEntity.class, TrackedDataHandlerRegistry.INTEGER);

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
    private double firingDirY;
    private double firingDirZ;
    private int processedSegments;
    private final Set<BlockPos> brokenThisAttack = new HashSet<>();

    public BigDogEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.2)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
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
        this.dataTracker.startTracking(FIRING_DIR_X, 0.0f);
        this.dataTracker.startTracking(FIRING_DIR_Y, 0.0f);
        this.dataTracker.startTracking(FIRING_DIR_Z, 1.0f);
        this.dataTracker.startTracking(FIRING_PROGRESS, 0);
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

    public float getFiringDirXTracked() {
        return this.dataTracker.get(FIRING_DIR_X);
    }

    public float getFiringDirYTracked() {
        return this.dataTracker.get(FIRING_DIR_Y);
    }

    public float getFiringDirZTracked() {
        return this.dataTracker.get(FIRING_DIR_Z);
    }

    public int getFiringProgress() {
        return this.dataTracker.get(FIRING_PROGRESS);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);
        if (!this.isAlive()) {
            this.cancelAttack();
        }
        if (!this.getWorld().isClient && result && amount > 0) {
            LivingEntity attackerLiving = null;
            var attacker = source.getAttacker();
            if (attacker instanceof LivingEntity living && living != this && living.isAlive()) {
                attackerLiving = living;
            } else if (source.getSource() instanceof LivingEntity living && living != this && living.isAlive()) {
                attackerLiving = living;
            }
            if (attackerLiving != null) {
                this.revengeTarget = attackerLiving;
                this.setTarget(attackerLiving);
                if (this.getState() == State.IDLE && this.cooldownTicksRemaining <= 0) {
                    this.startCharging();
                }
            }
        }
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        this.cancelAttack();
        super.onDeath(damageSource);
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

        Vec3d origin = this.getEyePos();
        Vec3d dir;
        if (this.revengeTarget != null && this.revengeTarget.isAlive()) {
            dir = this.revengeTarget.getEyePos().subtract(origin);
        } else {
            dir = this.getRotationVector();
        }
        if (dir.lengthSquared() < 1.0e-6) {
            dir = new Vec3d(0, 0, 1);
        }
        dir = dir.normalize();
        this.firingDirX = dir.x;
        this.firingDirY = dir.y;
        this.firingDirZ = dir.z;
        this.dataTracker.set(FIRING_DIR_X, (float) dir.x);
        this.dataTracker.set(FIRING_DIR_Y, (float) dir.y);
        this.dataTracker.set(FIRING_DIR_Z, (float) dir.z);
        this.dataTracker.set(FIRING_PROGRESS, 0);
        // Sound now handled client-side via EntityTrackingSoundInstance (no server world.playSound to avoid double play)
    }

    private void enterCooldown() {
        this.setState(State.COOLDOWN);
        this.cooldownTicksRemaining = COOLDOWN_TICKS;
        this.brokenThisAttack.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (!this.isAlive() || this.isRemoved()) {
            if (this.getState() == State.CHARGING || this.getState() == State.FIRING) {
                this.cancelAttack();
            }
            return;
        }
        if (this.revengeTarget != null && (!this.revengeTarget.isAlive() || this.revengeTarget.isRemoved())) {
            this.revengeTarget = null;
            this.setTarget(null);
        }

        switch (this.getState()) {
            case IDLE -> {
                if (this.cooldownTicksRemaining > 0) {
                    this.cooldownTicksRemaining--;
                }
                if (this.cooldownTicksRemaining <= 0 && this.revengeTarget != null && this.revengeTarget.isAlive() && !this.revengeTarget.isRemoved()) {
                    this.getLookControl().lookAt(this.revengeTarget, 30.0f, 30.0f);
                    double distSq = this.squaredDistanceTo(this.revengeTarget);
                    if (distSq > 4.0) {
                        this.getNavigation().startMovingTo(this.revengeTarget, 1.0);
                    }
                    this.startCharging();
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

        // Sound handled client-side via EntityTrackingSoundInstance (single play, no server world.playSound)
        this.chargingTicksRemaining--;
        int progress = (int) ((1.0 - (double) this.chargingTicksRemaining / CHARGING_TICKS) * 100);
        this.dataTracker.set(CHARGE_PROGRESS, MathHelper.clamp(progress, 0, 100));

        if (this.chargingTicksRemaining <= 0) {
            this.startFiring();
        }
    }

    private void tickFiring() {
        this.getNavigation().stop();

        // Update firing progress for client beam (0..100)
        int firingProgress = (int) ((1.0 - (double) this.firingTicksRemaining / FIRING_TICKS) * 100);
        this.dataTracker.set(FIRING_PROGRESS, MathHelper.clamp(firingProgress, 0, 100));

        if (this.damageIntervalCooldown <= 0) {
            applySonicDamage();
            this.damageIntervalCooldown = DAMAGE_INTERVAL;
        } else {
            this.damageIntervalCooldown--;
        }

        processBlockDestruction();

        this.firingTicksRemaining--;
        if (this.firingTicksRemaining <= 0) {
            this.dataTracker.set(FIRING_PROGRESS, 0);
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
        this.dataTracker.set(FIRING_PROGRESS, 0);
        this.setState(State.IDLE);
        this.revengeTarget = null;
    }

    private void applySonicDamage() {
        Vec3d origin = this.getEyePos();
        Vec3d dir = new Vec3d(this.firingDirX, this.firingDirY, this.firingDirZ).normalize();
        Vec3d end = origin.add(dir.multiply(RANGE));

        double minX = Math.min(origin.x, end.x) - HALF_WIDTH;
        double minY = Math.min(origin.y, end.y) - HALF_WIDTH;
        double minZ = Math.min(origin.z, end.z) - HALF_WIDTH;
        double maxX = Math.max(origin.x, end.x) + HALF_WIDTH;
        double maxY = Math.max(origin.y, end.y) + HALF_WIDTH;
        double maxZ = Math.max(origin.z, end.z) + HALF_WIDTH;

        Box beamBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        List<LivingEntity> candidates = this.getWorld().getNonSpectatingEntities(LivingEntity.class, beamBox);

        for (LivingEntity target : candidates) {
            if (target == this) continue;
            if (!target.isAlive()) continue;
            if (!isInBeam(target, origin, dir)) continue;

            DamageSource source = this.getDamageSources().mobAttack(this);
            if (target.damage(source, DAMAGE)) {
                // mobAttack 无内置击退（区别于 sonicBoom），此处直接 0.8 以保持总击退 0.8；takeKnockback 会取反向量，需传 -dir
                target.takeKnockback(0.8, -dir.x, -dir.z);
                if (target instanceof PlayerEntity) {
                    target.velocityModified = true;
                }
            }
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
        int elapsed = FIRING_TICKS - this.firingTicksRemaining;
        double destructionProgress = MathHelper.clamp((double) elapsed / BLOCK_BREAK_DURATION_TICKS, 0.0, 1.0);
        int targetSegments = (int) Math.ceil(destructionProgress * totalSegments);
        targetSegments = MathHelper.clamp(targetSegments, 0, totalSegments);

        Vec3d origin = this.getEyePos();
        Vec3d dir = new Vec3d(this.firingDirX, this.firingDirY, this.firingDirZ).normalize();
        double halfWidthSq = HALF_WIDTH * HALF_WIDTH;
        int r = (int) Math.floor(HALF_WIDTH);

        while (this.processedSegments < targetSegments) {
            double dist = this.processedSegments + 0.5;
            Vec3d center = origin.add(dir.multiply(dist));

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        double axial = dx * dir.x + dy * dir.y + dz * dir.z;
                        if (Math.abs(axial) > 0.5) continue;
                        double dxd = dx;
                        double dyd = dy;
                        double dzd = dz;
                        double perpSq = dxd * dxd + dyd * dyd + dzd * dzd - axial * axial;
                        if (perpSq > halfWidthSq + 1.0e-6) continue;
                        BlockPos p = BlockPos.ofFloored(center.x + dx, center.y + dy, center.z + dz);
                        if (this.brokenThisAttack.contains(p)) continue;
                        this.brokenThisAttack.add(p);
                        tryBreakBlock(p);
                    }
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
        nbt.putDouble("FiringDirY", this.firingDirY);
        nbt.putDouble("FiringDirZ", this.firingDirZ);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ChargingTicks")) this.chargingTicksRemaining = nbt.getInt("ChargingTicks");
        if (nbt.contains("FiringTicks")) this.firingTicksRemaining = nbt.getInt("FiringTicks");
        if (nbt.contains("CooldownTicks")) this.cooldownTicksRemaining = nbt.getInt("CooldownTicks");
        if (nbt.contains("FiringDirX")) this.firingDirX = nbt.getDouble("FiringDirX");
        if (nbt.contains("FiringDirY")) this.firingDirY = nbt.getDouble("FiringDirY");
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

    public double getFiringDirY() {
        return firingDirY;
    }

    public double getFiringDirZ() {
        return firingDirZ;
    }
}
