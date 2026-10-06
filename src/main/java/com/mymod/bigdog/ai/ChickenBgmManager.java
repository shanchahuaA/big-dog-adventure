package com.mymod.bigdog.ai;

import com.mymod.bigdog.BigDogMod;
import com.mymod.bigdog.entity.BigDogEntity;
import com.mymod.bigdog.entity.WizardChicken;
import com.mymod.bigdog.entity.MagicCircleEntity;
import com.mymod.bigdog.registry.ModEntities;
import com.mymod.bigdog.registry.ModEffects;
import com.mymod.bigdog.registry.ModSounds;
import com.mymod.bigdog.util.HorseColorUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.HorseEntity;import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChickenBgmManager {
    private ChickenBgmManager() {}

    public static final String SUMMONED_WARDEN_TAG = "bigdog_summoned";

    private static final int BGM_COOLDOWN_TICKS = 100;
    private static final int TRIPLE_THRESHOLD = 3;
    private static final long FOLLOW_WARDEN_TICKS = 1800L;
    private static final long WARDEN_REPEAT_TICKS = 6000L;
    private static final long WARDEN_REPEAT_EMPTY_TICKS = 2400L;
    private static final long FIRST_REDUCE_TICKS = 200L;
    private static final long REPEAT_REDUCE_TICKS = 400L;
    private static final long REPEAT_EMPTY_REDUCE_TICKS = 200L;
    private static final long SUMMON_MIN_TICKS = 600L;
    private static final long WARDEN_GRACE_TICKS = 600L;
    private static final double CHASE_BASE_SPEED = 0.45;
    private static final double CHASE_MAX_SPEED = 1.0;
    private static final double CHASE_HOT_DIST_SQ = 12.0 * 12.0;
    private static final double CHASE_TELEPORT_DIST_SQ = 32.0 * 32.0;
    private static final long CHASE_TELEPORT_COOLDOWN_TICKS = 40L;
    private static final long VILLAGE_CHECK_PERIOD = 1200L;
    private static final double VILLAGE_RADIUS = 32.0;
    private static final int VILLAGE_LOCATE_RADIUS = 100;
    private static final double SLOT_READY_DIST_SQ = 9.0;
    private static final float SLOT_READY_FACING_DEG = 45.0f;
    private static final long RETREAT_DELAY_TICKS = 200L;

    /** 法阵半径：鸡脚下小阵 / 召唤点大阵 / 坚守者豪华阵。 */
    private static final float SMALL_CIRCLE_RADIUS = 0.8f;
    private static final float BIG_CIRCLE_RADIUS = 3.0f;
    private static final float WARDEN_CIRCLE_RADIUS = 3.8f;
    /** 豪华阵演出到「圆墙升起」那一刻才让坚守者真正落地。 */
    private static final long WARDEN_CIRCLE_DELAY_TICKS = 40L;

    private static final Map<UUID, TargetState> TARGETS = new HashMap<>();
    private static final Map<UUID, Vec3d> SLOTS = new HashMap<>();
    /** 已经放过小法阵的鸡。只在「就绪」上升沿放一次（法阵是瞬时绽放，不是持续状态）。 */
    private static final Set<UUID> CIRCLED = new HashSet<>();
    private static final Map<ServerWorld, List<VillageEntry>> VILLAGES = new HashMap<>();
    private static final Map<UUID, FollowState> FOLLOWS = new HashMap<>();
    private static final Map<UUID, Long> LAST_CHASE_TELEPORT = new HashMap<>();
    private static final Map<UUID, UUID> WARDEN_OWNERS = new HashMap<>();
    private static final Map<UUID, Integer> ALIVE_WARDENS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, Long> WARDEN_RETREAT_ARMED_AT = new HashMap<>();
    /** 豪华阵已放、只在等演出走到「圆墙升起」那一刻的坚守者。 */
    private static final List<PendingWarden> PENDING_WARDENS = new ArrayList<>();

    private static final class TargetState {
        int count;
        long nextBgmTick;
    }

    private static final class FollowState {
        long firstSeenTick = -1L;
        long lastWardenTick = Long.MIN_VALUE / 2;
        long lastSpotTick = -1L;
        long pauseStartTick = -1L;
    }

    private static final class VillageEntry {
        BlockPos center;
    }

    /** 一次待落地的坚守者召唤。落点与归属在触发那一刻就定死，只在 dueTick 兑现。 */
    private static final class PendingWarden {
        final UUID owner;
        final RegistryKey<World> dim;
        final BlockPos spot;
        final long dueTick;

        PendingWarden(UUID owner, RegistryKey<World> dim, BlockPos spot, long dueTick) {
            this.owner = owner;
            this.dim = dim;
            this.spot = spot;
            this.dueTick = dueTick;
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ChickenBgmManager::onEndTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof HorseEntity horse && HorseColorUtil.isColored(horse)
                    && source.getAttacker() instanceof LivingEntity attacker
                    && isSummonedWarden(attacker)) {
                BigDogMod.LOGGER.info("[WardenGuard] damage on colored horse denied"); // TEMP-PROBE
                return false;
            }
            return true;
        });
    }

    private static void onEndTick(MinecraftServer server) {
        tickPendingWardens(server);
        if (server.getOverworld().getTime() % 10 == 0) {
            refreshWardenOwners(server);
        }
        for (ServerWorld world : server.getWorlds()) {
            long time = world.getTime();
            if (time % 10 == 0) {
                tickWardens(world, time);
            }
            if (time % 20 == 0) {
                tickChickens(world, time);
                tickFollows(world, time);
            }
            if (world.getRegistryKey() == World.OVERWORLD && time % VILLAGE_CHECK_PERIOD == 0) {
                tickVillages(world, time);
            }
        }
    }

    private static void tickChickens(ServerWorld world, long time) {
        Map<UUID, List<WizardChicken>> byTarget = new HashMap<>();
        List<WizardChicken> all = new ArrayList<>();
        int totalChickens = 0; // TEMP-PROBE
        int targetedChickens = 0; // TEMP-PROBE
        for (Entity e : world.iterateEntities()) {
            if (!(e instanceof WizardChicken chicken) || !chicken.isAlive() || chicken.isRemoved()) {
                continue;
            }
            totalChickens++; // TEMP-PROBE
            all.add(chicken);
            LivingEntity target = chicken.getBgmTarget();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                continue;
            }
            targetedChickens++; // TEMP-PROBE
            byTarget.computeIfAbsent(target.getUuid(), u -> new ArrayList<>()).add(chicken);
        }
        if (time % 100 == 0) { // TEMP-PROBE
            BigDogMod.LOGGER.info("[ChorusBgm] chickens={} targeted={} groups={}",
                    totalChickens, targetedChickens, byTarget.size());
        }
        Set<UUID> seen = new HashSet<>();
        Set<UUID> readyIds = new HashSet<>();
        for (var entry : byTarget.entrySet()) {
            List<WizardChicken> group = entry.getValue();
            LivingEntity target = group.get(0).getBgmTarget();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                continue;
            }
            assignSlots(world, target, group, seen);
            List<WizardChicken> ready = new ArrayList<>();
            for (WizardChicken chicken : group) {
                Vec3d slot = SLOTS.get(chicken.getUuid());
                if (slot != null
                        && isFacing(chicken, target, SLOT_READY_FACING_DEG)
                        && chicken.squaredDistanceTo(slot.x, slot.y, slot.z) <= SLOT_READY_DIST_SQ) {
                    ready.add(chicken);
                    readyIds.add(chicken.getUuid());
                }
            }
            if (time % 100 == 0) { // TEMP-PROBE
                int withSlot = 0; // TEMP-PROBE
                int nearSlot = 0; // TEMP-PROBE
                int facingOk = 0; // TEMP-PROBE
                double minD2 = Double.MAX_VALUE; // TEMP-PROBE
                Vec3d minChPos = null; // TEMP-PROBE
                Vec3d minSlotPos = null; // TEMP-PROBE
                for (WizardChicken c : group) { // TEMP-PROBE
                    Vec3d s = SLOTS.get(c.getUuid()); // TEMP-PROBE
                    if (s == null) { // TEMP-PROBE
                        continue; // TEMP-PROBE
                    } // TEMP-PROBE
                    withSlot++; // TEMP-PROBE
                    double d2 = c.squaredDistanceTo(s.x, s.y, s.z); // TEMP-PROBE
                    if (d2 < minD2) { // TEMP-PROBE
                        minD2 = d2; // TEMP-PROBE
                        minChPos = c.getPos(); // TEMP-PROBE
                        minSlotPos = s; // TEMP-PROBE
                    } // TEMP-PROBE
                    if (d2 <= SLOT_READY_DIST_SQ) { // TEMP-PROBE
                        nearSlot++; // TEMP-PROBE
                    } // TEMP-PROBE
                    if (isFacing(c, target, SLOT_READY_FACING_DEG)) { // TEMP-PROBE
                        facingOk++; // TEMP-PROBE
                    } // TEMP-PROBE
                } // TEMP-PROBE
                BigDogMod.LOGGER.info(
                        "[ChorusBgm] group={} ready={} slot={} near={} facing={} minD2={} ch={} sl={} target={}", // TEMP-PROBE
                        group.size(), ready.size(), withSlot, nearSlot, facingOk,
                        minD2 == Double.MAX_VALUE ? -1 : String.format("%.1f", minD2),
                        minChPos == null ? "-" : String.format("%.1f,%.1f,%.1f",
                                minChPos.x, minChPos.y, minChPos.z),
                        minSlotPos == null ? "-" : String.format("%.1f,%.1f,%.1f",
                                minSlotPos.x, minSlotPos.y, minSlotPos.z),
                        target.getName().getString()); // TEMP-PROBE
            }
            if (ready.isEmpty()) {
                continue;
            }
            TargetState state = TARGETS.computeIfAbsent(target.getUuid(), u -> new TargetState());
            if (time < state.nextBgmTick) {
                continue;
            }
            state.nextBgmTick = time + BGM_COOLDOWN_TICKS;
            for (WizardChicken chicken : ready) {
                world.playSound(null, chicken.getBlockPos(), ModSounds.WIZARD_CHICKEN_BGM,
                        SoundCategory.NEUTRAL, 1.0f, 1.0f);
                state.count++;
                if (state.count >= TRIPLE_THRESHOLD) {
                    state.count = 0;
                    triggerTriple(world, target);
                    break;
                }
            }
        }
        // 「就绪」同步给客户端（合奏动画 + 脚下小阵都靠它），并在上升沿放一次小法阵。
        for (WizardChicken chicken : all) {
            boolean ready = readyIds.contains(chicken.getUuid());
            chicken.setAssembling(ready);
            if (!ready) {
                CIRCLED.remove(chicken.getUuid());
            } else if (CIRCLED.add(chicken.getUuid())) {
                bloomCircle(world, chicken.getX(), chicken.getY(), chicken.getZ(), SMALL_CIRCLE_RADIUS,
                        false);
            }
        }
        CIRCLED.removeIf(uuid -> world.getEntity(uuid) == null);
        SLOTS.keySet().retainAll(seen);
        TARGETS.keySet().removeIf(uuid -> world.getEntity(uuid) == null
                && world.getServer().getPlayerManager().getPlayer(uuid) == null);
    }

    public static float angleTo(Entity from, Entity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) (MathHelper.atan2(-dx, dz) * 180.0 / Math.PI);
    }

    public static boolean isFacing(Entity from, Entity to, float toleranceDeg) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        if (dx * dx + dz * dz < 1.0e-6) {
            return true;
        }
        return Math.abs(MathHelper.wrapDegrees(angleTo(from, to) - from.getYaw())) < toleranceDeg;
    }

    public static Vec3d getSlotFor(UUID chickenUuid) {
        return SLOTS.get(chickenUuid);
    }

    public static BlockPos findVillageCenter(ServerWorld world, BlockPos pos) {
        List<VillageEntry> list = VILLAGES.get(world);
        if (list == null) {
            return null;
        }
        VillageEntry best = null;
        double bestSq = 128.0 * 128.0;
        for (VillageEntry entry : list) {
            double d = entry.center.getSquaredDistance(pos);
            if (d < bestSq) {
                bestSq = d;
                best = entry;
            }
        }
        return best == null ? null : best.center;
    }

    private static void assignSlots(ServerWorld world, LivingEntity target, List<WizardChicken> group,
            Set<UUID> seen) {
        List<WizardChicken> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparing(WizardChicken::getUuid));
        int level = target.getBlockY();
        for (int i = 0; i < sorted.size(); i++) {
            WizardChicken chicken = sorted.get(i);
            double rad = (i % 8) * Math.PI / 4.0;
            double sx = target.getX() - Math.sin(rad) * 6.0;
            double sz = target.getZ() + Math.cos(rad) * 6.0;
            SLOTS.put(chicken.getUuid(), findSlotSpot(world, sx, sz, level));
            seen.add(chicken.getUuid());
        }
    }

    private static Vec3d findSlotSpot(ServerWorld world, double sx, double sz, int level) {
        int bx = (int) Math.floor(sx);
        int bz = (int) Math.floor(sz);
        Vec3d best = null;
        int bestDy = Integer.MAX_VALUE;
        for (int r = 0; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            new BlockPos(bx + dx, 0, bz + dz));
                    int dy = Math.abs(top.getY() - level);
                    if (dy <= 1) {
                        return new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
                    }
                    if (dy < bestDy) {
                        bestDy = dy;
                        best = new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
                    }
                }
            }
        }
        return best != null ? best : new Vec3d(sx, level, sz);
    }

    private static void countBgm(ServerWorld world, LivingEntity target, long time) {
        world.playSound(null, target.getBlockPos(), ModSounds.WIZARD_CHICKEN_BGM,
                SoundCategory.NEUTRAL, 1.0f, 1.0f);
        TargetState state = TARGETS.computeIfAbsent(target.getUuid(), u -> new TargetState());
        state.count++;
        state.nextBgmTick = time + BGM_COOLDOWN_TICKS;
        if (state.count >= TRIPLE_THRESHOLD) {
            state.count = 0;
            triggerTriple(world, target);
        }
    }

    /**
     * 在落点放一个法阵（纯表现、不持久，{@link MagicCircleEntity#getLifeTicks()} tick 后自灭）。
     * {@code grand} 只给坚守者召唤阵用 —— 更长的演出 + 上升粒子。
     */
    private static void bloomCircle(ServerWorld world, double x, double y, double z, float radius,
            boolean grand) {
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.create(world);
        if (circle == null) {
            return;
        }
        circle.refreshPositionAndAngles(x, y, z, 0.0f, 0.0f);
        circle.setRadius(radius);
        circle.setGrand(grand);
        world.spawnEntity(circle);
    }

    public static void forceTrigger(ServerWorld world, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }
        countBgm(world, target, world.getTime());
    }

    private static void triggerTriple(ServerWorld world, LivingEntity target) {
        if (target instanceof ServerPlayerEntity talismanPlayer
                && BigDogEntity.isWearingTalisman(talismanPlayer)) {
            return;
        }
        BlockPos spot = findSpawnSpot(world, target.getBlockPos(), 8);
        bloomCircle(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, BIG_CIRCLE_RADIUS, false);
        BigDogEntity dog = ModEntities.BIG_DOG.create(world);
        if (dog != null) {
            dog.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                    world.random.nextFloat() * 360.0f, 0.0f);
            dog.setInitialTarget(target);
            world.spawnEntity(dog);
            BigDogMod.LOGGER.info("[ChorusBgm] dog spawned for {}", target.getName().getString()); // TEMP-PROBE
        }
        if (target instanceof ServerPlayerEntity probePlayer) { // TEMP-PROBE
            BigDogMod.LOGGER.info("[ChorusBgm] horse gate: talisman={} coloredRiding={} vehicle={}", // TEMP-PROBE
                    BigDogEntity.isWearingTalisman(probePlayer), // TEMP-PROBE
                    HorseColorUtil.isRidingColored(probePlayer), // TEMP-PROBE
                    probePlayer.getVehicle()); // TEMP-PROBE
        } // TEMP-PROBE
        if (target instanceof ServerPlayerEntity player && !BigDogEntity.isWearingTalisman(player)) {
            if (HorseColorUtil.isRidingColored(player)) {
                return;
            }
            BlockPos horseSpot = findSpawnSpot(world, player.getBlockPos(), 8);
            HorseEntity horse = EntityType.HORSE.create(world);
            if (horse != null) {
                horse.refreshPositionAndAngles(horseSpot.getX() + 0.5, horseSpot.getY(),
                        horseSpot.getZ() + 0.5, world.random.nextFloat() * 360.0f, 0.0f);
                horse.setTame(true);
                horse.setOwnerUuid(player.getUuid());
                horse.saddle(SoundCategory.NEUTRAL);
                HorseColorUtil.setDemonized(horse);
                world.spawnEntity(horse);
                player.startRiding(horse, true);
                BigDogMod.LOGGER.info("[ChorusBgm] DEMONIZED HORSE spawned + forced mount {}", // TEMP-PROBE
                        player.getName().getString()); // TEMP-PROBE
            }
        }
    }

    public static boolean chaseHorseless(WizardChicken chicken, LivingEntity target) {
        if (!(target instanceof PlayerEntity player) || chicken.getWorld().isClient) {
            return false;
        }
        if (!(chicken.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (!player.isAlive() || player.isRemoved()) {
            return false;
        }
        if (HorseColorUtil.isRidingColored(player)) {
            return false;
        }
        if (chicken.squaredDistanceTo(player) <= CHASE_TELEPORT_DIST_SQ) {
            return false;
        }
        long now = world.getTime();
        if (now - LAST_CHASE_TELEPORT.getOrDefault(chicken.getUuid(), Long.MIN_VALUE / 2)
                < CHASE_TELEPORT_COOLDOWN_TICKS) {
            return false;
        }
        LAST_CHASE_TELEPORT.put(chicken.getUuid(), now);
        BlockPos spot = findSpawnSpot(world, player.getBlockPos(), 8);
        chicken.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                chicken.getYaw(), 0.0f);
        chicken.getNavigation().stop();
        return true;
    }

    public static void updateChaseSpeed(WizardChicken chicken, LivingEntity target) {
        boolean atMaxSpeed = target instanceof PlayerEntity player && player.isAlive() && !player.isRemoved()
                && !HorseColorUtil.isRidingColored(player)
                && chicken.squaredDistanceTo(player) > CHASE_HOT_DIST_SQ;
        EntityAttributeInstance speed =
                chicken.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        double want = atMaxSpeed ? CHASE_MAX_SPEED : CHASE_BASE_SPEED;
        if (speed.getBaseValue() != want) {
            speed.setBaseValue(want);
        }
    }

    public static boolean isSummonedWarden(LivingEntity entity) {
        return entity instanceof WardenEntity
                && entity.getCommandTags().contains(SUMMONED_WARDEN_TAG);
    }

    private static boolean isMountedPlayer(Entity entity) {
        return entity instanceof PlayerEntity player && HorseColorUtil.isRidingColored(player);
    }

    private static boolean wardenMustNotTarget(Entity entity) {
        return entity instanceof BigDogEntity
                || entity instanceof WizardChicken
                || isMountedPlayer(entity)
                || (entity instanceof HorseEntity horse && HorseColorUtil.isColored(horse));
    }

    private static void tickWardens(ServerWorld world, long time) {
        for (Entity e : world.iterateEntities()) {
            if (!(e instanceof WardenEntity warden) || !warden.isAlive() || warden.isRemoved()) {
                continue;
            }
            if (!warden.getCommandTags().contains(SUMMONED_WARDEN_TAG)) {
                continue;
            }
            var prime = warden.getAngerManager().getPrimeSuspect();
            if (prime.isPresent() && wardenMustNotTarget(prime.get())) {
                warden.getAngerManager().removeSuspect(prime.get());
            }
            LivingEntity target = warden.getTarget();
            if (target != null && wardenMustNotTarget(target)) {
                warden.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
                warden.getAngerManager().removeSuspect(target);
                if (target instanceof PlayerEntity mountedPlayer) { // TEMP-PROBE
                    BigDogMod.LOGGER.info("[WardenTimer] warden DISENGAGE from mounted {}", // TEMP-PROBE
                            mountedPlayer.getName().getString()); // TEMP-PROBE
                } else if (target instanceof HorseEntity) { // TEMP-PROBE
                    BigDogMod.LOGGER.info("[WardenTimer] warden dropped colored-horse target"); // TEMP-PROBE
                }
            }
            UUID ownerUuid = WARDEN_OWNERS.get(warden.getUuid());
            PlayerEntity owner = ownerUuid == null ? null
                    : world.getServer().getPlayerManager().getPlayer(ownerUuid);
            if (owner == null) {
                WARDEN_RETREAT_ARMED_AT.remove(warden.getUuid());
                if (warden.getBrain().hasMemoryModule(MemoryModuleType.DIG_COOLDOWN)) {
                    warden.getBrain().forget(MemoryModuleType.DIG_COOLDOWN);
                    BigDogMod.LOGGER.info("[WardenTimer] warden RETREAT (orphan) {}", // TEMP-PROBE
                            warden.getUuid()); // TEMP-PROBE
                }
            } else if (!owner.isAlive() || owner.isRemoved()) {
                WARDEN_RETREAT_ARMED_AT.remove(warden.getUuid());
            } else if (owner.getWorld() == world && HorseColorUtil.isRidingColored(owner)) {
                Long armedAt = WARDEN_RETREAT_ARMED_AT.get(warden.getUuid());
                if (armedAt == null) {
                    WARDEN_RETREAT_ARMED_AT.put(warden.getUuid(), time);
                    BigDogMod.LOGGER.info("[WardenTimer] warden RETREAT armed {}", // TEMP-PROBE
                            owner.getName().getString()); // TEMP-PROBE
                } else if (time - armedAt >= RETREAT_DELAY_TICKS) {
                    WARDEN_RETREAT_ARMED_AT.remove(warden.getUuid());
                    warden.getBrain().forget(MemoryModuleType.DIG_COOLDOWN);
                    BigDogMod.LOGGER.info("[WardenTimer] warden RETREAT firing {}", // TEMP-PROBE
                            owner.getName().getString()); // TEMP-PROBE
                }
            } else {
                WARDEN_RETREAT_ARMED_AT.remove(warden.getUuid());
                if (warden.getAngerManager().getAngerFor(owner) < 150) {
                    warden.increaseAngerAt(owner, 150, false);
                }
                warden.updateAttackTarget(owner);
            }
        }
    }

    private static void refreshWardenOwners(MinecraftServer server) {
        Set<UUID> aliveWardens = new HashSet<>();
        for (ServerWorld world : server.getWorlds()) {
            for (Entity e : world.iterateEntities()) {
                if (e instanceof WardenEntity warden && warden.isAlive() && !warden.isRemoved()
                        && warden.getCommandTags().contains(SUMMONED_WARDEN_TAG)) {
                    aliveWardens.add(warden.getUuid());
                }
            }
        }
        var playerManager = server.getPlayerManager();
        WARDEN_OWNERS.keySet().removeIf(wardenUuid -> {
            if (!aliveWardens.contains(wardenUuid)) {
                return true;
            }
            UUID owner = WARDEN_OWNERS.get(wardenUuid);
            return owner == null || playerManager.getPlayer(owner) == null;
        });
        ALIVE_WARDENS_BY_OWNER.clear();
        for (UUID wardenUuid : aliveWardens) {
            UUID owner = WARDEN_OWNERS.get(wardenUuid);
            if (owner != null) {
                ALIVE_WARDENS_BY_OWNER.merge(owner, 1, Integer::sum);
            }
        }
    }

    private static BlockPos findSpawnSpot(ServerWorld world, BlockPos near, int maxRadius) {
        for (int r = 2; r <= maxRadius; r += 2) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                int x = near.getX() + (int) Math.round(Math.cos(a) * r);
                int z = near.getZ() + (int) Math.round(Math.sin(a) * r);
                BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(x, 0, z));
                if (Math.abs(top.getY() - near.getY()) <= 8) {
                    return top;
                }
            }
        }
        return near.up();
    }

    private static void tickFollows(ServerWorld world, long time) {
        Map<UUID, Integer> spotters = new HashMap<>();
        for (Entity e : world.iterateEntities()) {
            if (!(e instanceof WizardChicken chicken) || !chicken.isAlive() || chicken.isRemoved()) {
                continue;
            }
            for (PlayerEntity player : world.getPlayers()) {
                if (!player.isAlive() || player.isRemoved()) {
                    continue;
                }
                double distSq = chicken.squaredDistanceTo(player);
                if (distSq > 64.0 * 64.0) {
                    continue;
                }
                if (distSq > 100.0 && !chicken.canSee(player)) {
                    continue;
                }
                spotters.merge(player.getUuid(), 1, Integer::sum);
            }
        }
        for (PlayerEntity player : world.getPlayers()) {
            UUID uuid = player.getUuid();
            if (!player.isAlive() || player.isRemoved()
                    || HorseColorUtil.isRidingColored(player)) {
                FOLLOWS.remove(uuid);
                continue;
            }
            int count = spotters.getOrDefault(uuid, 0);
            FollowState follow = FOLLOWS.get(uuid);
            if (count <= 0) {
                if (follow == null) {
                    continue;
                }
                if (time - follow.lastSpotTick > WARDEN_GRACE_TICKS) {
                    FOLLOWS.remove(uuid);
                    BigDogMod.LOGGER.info("[WardenTimer] p={} grace EXPIRED -> reset (off {} ticks)",
                            player.getName().getString(), time - follow.lastSpotTick); // TEMP-PROBE
                } else if (follow.pauseStartTick < 0) {
                    follow.pauseStartTick = time;
                    BigDogMod.LOGGER.info("[WardenTimer] p={} spotters=0 -> grace started",
                            player.getName().getString()); // TEMP-PROBE
                }
                continue;
            }
            if (follow == null) {
                follow = new FollowState();
                FOLLOWS.put(uuid, follow);
            }
            if (follow.pauseStartTick >= 0) {
                long paused = time - follow.pauseStartTick;
                if (follow.firstSeenTick >= 0) {
                    follow.firstSeenTick += paused;
                }
                follow.lastWardenTick += paused;
                follow.pauseStartTick = -1L;
                BigDogMod.LOGGER.info("[WardenTimer] p={} grace resumed -> compensated {} ticks",
                        player.getName().getString(), paused); // TEMP-PROBE
            }
            follow.lastSpotTick = time;
            if (follow.firstSeenTick < 0) {
                follow.firstSeenTick = time;
            }
            long firstThreshold = Math.max(SUMMON_MIN_TICKS,
                    FOLLOW_WARDEN_TICKS - FIRST_REDUCE_TICKS * (count - 1));
            int alive = ALIVE_WARDENS_BY_OWNER.getOrDefault(uuid, 0);
            long repeatBase = alive > 0 ? WARDEN_REPEAT_TICKS : WARDEN_REPEAT_EMPTY_TICKS;
            long repeatReduce = alive > 0 ? REPEAT_REDUCE_TICKS : REPEAT_EMPTY_REDUCE_TICKS;
            long repeatThreshold = Math.max(SUMMON_MIN_TICKS,
                    repeatBase - repeatReduce * (count - 1));
            if (time % 100 == 0) { // TEMP-PROBE
                BigDogMod.LOGGER.info(
                        "[WardenTimer] p={} spotters={} elapsed={} thr1={} sinceWarden={} thr2={} alive={}",
                        player.getName().getString(), count,
                        follow.firstSeenTick < 0 ? -1L : time - follow.firstSeenTick,
                        firstThreshold, time - follow.lastWardenTick, repeatThreshold, alive);
            }
            if (time - follow.firstSeenTick >= firstThreshold
                    && time - follow.lastWardenTick >= repeatThreshold) {
                follow.firstSeenTick = -1L;
                follow.lastWardenTick = time;
                BlockPos spot = findSpawnSpot(world, player.getBlockPos(), 8);
                bloomCircle(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                        WARDEN_CIRCLE_RADIUS, true);
                PENDING_WARDENS.add(new PendingWarden(player.getUuid(), world.getRegistryKey(), spot,
                        time + WARDEN_CIRCLE_DELAY_TICKS));
                BigDogMod.LOGGER.info("[WardenTimer] warden queued for {} (spotters={}, due +{}t)",
                        player.getName().getString(), count, WARDEN_CIRCLE_DELAY_TICKS); // TEMP-PROBE
            }
        }
        MinecraftServer server = world.getServer();
        Iterator<UUID> it = FOLLOWS.keySet().iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            if (server.getPlayerManager().getPlayer(uuid) == null) {
                it.remove();
            }
        }
    }

    /**
     * 兑现到点的待落地坚守者：世界没了就丢弃，主人没了/死了就不落地
     * （重复计时已在触发那一刻推进，所以不会连着重来）。
     */
    private static void tickPendingWardens(MinecraftServer server) {
        if (PENDING_WARDENS.isEmpty()) {
            return;
        }
        Iterator<PendingWarden> it = PENDING_WARDENS.iterator();
        while (it.hasNext()) {
            PendingWarden pending = it.next();
            ServerWorld world = server.getWorld(pending.dim);
            if (world == null) {
                it.remove();
                continue;
            }
            if (world.getTime() < pending.dueTick) {
                continue;
            }
            it.remove();
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(pending.owner);
            if (owner == null || !owner.isAlive() || owner.isRemoved()) {
                BigDogMod.LOGGER.info("[WardenTimer] pending warden dropped (owner gone)"); // TEMP-PROBE
                continue;
            }
            spawnSummonedWarden(world, owner, pending.spot);
        }
    }

    /** 豪华阵走到「圆墙升起」那一刻真正落地的坚守者。 */
    private static void spawnSummonedWarden(ServerWorld world, ServerPlayerEntity player, BlockPos spot) {
        WardenEntity warden = EntityType.WARDEN.create(world,
                null, null, spot, SpawnReason.COMMAND, true, false);
        if (warden == null) {
            return;
        }
        warden.addCommandTag(SUMMONED_WARDEN_TAG);
        WARDEN_OWNERS.put(warden.getUuid(), player.getUuid());
        warden.increaseAngerAt(player, 150, false);
        warden.updateAttackTarget(player);
        warden.setPersistent();
        warden.setTarget(player);
        warden.getBrain().remember(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        warden.getBrain().remember(MemoryModuleType.SONIC_BOOM_COOLDOWN, Unit.INSTANCE, 200L);
        world.spawnEntity(warden);
        BigDogMod.LOGGER.info("[WardenTimer] SUMMONED warden for {} (after {}t circle)",
                player.getName().getString(), WARDEN_CIRCLE_DELAY_TICKS); // TEMP-PROBE
    }

    private static void tickVillages(ServerWorld world, long time) {
        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isAlive() || player.isRemoved()) {
                continue;
            }
            BlockPos found = world.locateStructure(StructureTags.VILLAGE,
                    player.getBlockPos(), VILLAGE_LOCATE_RADIUS, false);
            if (found == null) {
                continue;
            }
            List<VillageEntry> list = VILLAGES.computeIfAbsent(world, w -> new ArrayList<>());
            boolean known = false;
            for (VillageEntry entry : list) {
                double dx = entry.center.getX() - found.getX();
                double dz = entry.center.getZ() - found.getZ();
                if (dx * dx + dz * dz < 128.0 * 128.0) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                VillageEntry entry = new VillageEntry();
                entry.center = found;
                list.add(entry);
            }
        }
        List<VillageEntry> list = VILLAGES.get(world);
        if (list == null) {
            return;
        }
        Iterator<VillageEntry> it = list.iterator();
        while (it.hasNext()) {
            VillageEntry entry = it.next();
            boolean nearAny = false;
            for (PlayerEntity player : world.getPlayers()) {
                if (player.isAlive() && !player.isRemoved()
                        && player.squaredDistanceTo(
                                entry.center.getX(), entry.center.getY(), entry.center.getZ())
                                < 1500.0 * 1500.0) {
                    nearAny = true;
                    break;
                }
            }
            if (!nearAny) {
                it.remove();
                continue;
            }
            int count = 0;
            for (Entity e : world.iterateEntities()) {
                if (e instanceof WizardChicken chicken && chicken.isAlive() && !chicken.isRemoved()
                        && chicken.squaredDistanceTo(
                                entry.center.getX(), entry.center.getY(), entry.center.getZ())
                                <= VILLAGE_RADIUS * VILLAGE_RADIUS) {
                    count++;
                }
            }
            for (int i = count; i < 3; i++) {
                BlockPos spot = findSpawnSpot(world, entry.center, 16);
                WizardChicken chicken = ModEntities.WIZARD_CHICKEN.create(world);
                if (chicken == null) {
                    break;
                }
                chicken.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                        world.random.nextFloat() * 360.0f, 0.0f);
                chicken.initialize(world, world.getLocalDifficulty(spot),
                        net.minecraft.entity.SpawnReason.STRUCTURE, null, null);
                world.spawnEntity(chicken);
            }
        }
    }
}
