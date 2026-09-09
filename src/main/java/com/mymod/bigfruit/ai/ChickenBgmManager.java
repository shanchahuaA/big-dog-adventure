package com.mymod.bigfruit.ai;

import com.mymod.bigfruit.entity.BigDogEntity;
import com.mymod.bigfruit.entity.DingdongChicken;
import com.mymod.bigfruit.registry.ModEntities;
import com.mymod.bigfruit.registry.ModSounds;
import com.mymod.bigfruit.util.HorseColorUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChickenBgmManager {
    private ChickenBgmManager() {}

    private static final int BGM_COOLDOWN_TICKS = 100;
    private static final int TRIPLE_THRESHOLD = 3;
    private static final long FOLLOW_WARDEN_TICKS = 1800L;
    private static final long WARDEN_REPEAT_TICKS = 6000L;
    private static final long VILLAGE_CHECK_PERIOD = 1200L;
    private static final double VILLAGE_RADIUS = 32.0;
    private static final int VILLAGE_LOCATE_RADIUS = 100;

    private static final Map<UUID, TargetState> TARGETS = new HashMap<>();
    private static final Map<ServerWorld, List<VillageEntry>> VILLAGES = new HashMap<>();
    private static final Map<UUID, FollowState> FOLLOWS = new HashMap<>();

    private static final class TargetState {
        int count;
        long nextBgmTick;
    }

    private static final class FollowState {
        long firstSeenTick = -1L;
        long lastWardenTick = Long.MIN_VALUE / 2;
    }

    private static final class VillageEntry {
        BlockPos center;
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ChickenBgmManager::onEndTick);
    }

    private static void onEndTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            long time = world.getTime();
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
        Map<UUID, List<DingdongChicken>> byTarget = new HashMap<>();
        for (Entity e : world.iterateEntities()) {
            if (!(e instanceof DingdongChicken chicken) || !chicken.isAlive() || chicken.isRemoved()) {
                continue;
            }
            LivingEntity target = chicken.getBgmTarget();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                continue;
            }
            byTarget.computeIfAbsent(target.getUuid(), u -> new ArrayList<>()).add(chicken);
        }
        for (var entry : byTarget.entrySet()) {
            List<DingdongChicken> group = entry.getValue();
            if (group.size() < TRIPLE_THRESHOLD) {
                continue;
            }
            LivingEntity target = group.get(0).getBgmTarget();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                continue;
            }
            int facing = 0;
            for (DingdongChicken chicken : group) {
                Vec3d toTarget = target.getEyePos().subtract(chicken.getEyePos());
                if (toTarget.lengthSquared() < 1.0e-6) {
                    facing++;
                } else if (chicken.getRotationVector().dotProduct(toTarget.normalize()) > 0.5) {
                    facing++;
                }
            }
            if (facing < TRIPLE_THRESHOLD) {
                continue;
            }
            TargetState state = TARGETS.computeIfAbsent(target.getUuid(), u -> new TargetState());
            if (time < state.nextBgmTick) {
                continue;
            }
            countBgm(world, target, time);
        }
    }

    private static void countBgm(ServerWorld world, LivingEntity target, long time) {
        world.playSound(null, target.getBlockPos(), ModSounds.DINGDONG_BGM,
                SoundCategory.NEUTRAL, 1.0f, 1.0f);
        TargetState state = TARGETS.computeIfAbsent(target.getUuid(), u -> new TargetState());
        state.count++;
        state.nextBgmTick = time + BGM_COOLDOWN_TICKS;
        if (state.count >= TRIPLE_THRESHOLD) {
            state.count = 0;
            triggerTriple(world, target);
        }
    }

    public static void forceTrigger(ServerWorld world, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }
        countBgm(world, target, world.getTime());
    }

    private static void triggerTriple(ServerWorld world, LivingEntity target) {
        BlockPos spot = findSpawnSpot(world, target.getBlockPos(), 8);
        BigDogEntity dog = ModEntities.BIG_DOG.create(world);
        if (dog != null) {
            dog.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                    world.random.nextFloat() * 360.0f, 0.0f);
            dog.setInitialTarget(target);
            world.spawnEntity(dog);
        }
        if (target instanceof ServerPlayerEntity player && !BigDogEntity.isWearingMask(player)) {
            BlockPos horseSpot = findSpawnSpot(world, player.getBlockPos(), 8);
            HorseEntity horse = EntityType.HORSE.create(world);
            if (horse != null) {
                horse.refreshPositionAndAngles(horseSpot.getX() + 0.5, horseSpot.getY(),
                        horseSpot.getZ() + 0.5, world.random.nextFloat() * 360.0f, 0.0f);
                horse.setTame(true);
                horse.setOwnerUuid(player.getUuid());
                horse.saddle(SoundCategory.NEUTRAL);
                HorseColorUtil.setRed(horse);
                world.spawnEntity(horse);
                player.startRiding(horse, true);
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
        Set<UUID> targeted = new HashSet<>();
        for (Entity e : world.iterateEntities()) {
            if (e instanceof DingdongChicken chicken && chicken.isAlive() && !chicken.isRemoved()) {
                LivingEntity target = chicken.getBgmTarget();
                if (target instanceof PlayerEntity player && player.isAlive() && !player.isRemoved()) {
                    targeted.add(player.getUuid());
                }
            }
        }
        for (PlayerEntity player : world.getPlayers()) {
            UUID uuid = player.getUuid();
            if (!player.isAlive() || player.isRemoved()) {
                FOLLOWS.remove(uuid);
                continue;
            }
            if (player.getVehicle() instanceof HorseEntity) {
                FOLLOWS.remove(uuid);
                continue;
            }
            if (!targeted.contains(uuid)) {
                FOLLOWS.remove(uuid);
                continue;
            }
            FollowState follow = FOLLOWS.computeIfAbsent(uuid, u -> new FollowState());
            if (follow.firstSeenTick < 0) {
                follow.firstSeenTick = time;
            }
            if (time - follow.firstSeenTick >= FOLLOW_WARDEN_TICKS
                    && time - follow.lastWardenTick >= WARDEN_REPEAT_TICKS) {
                follow.lastWardenTick = time;
                BlockPos spot = findSpawnSpot(world, player.getBlockPos(), 8);
                var warden = EntityType.WARDEN.create(world);
                if (warden != null) {
                    warden.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                            world.random.nextFloat() * 360.0f, 0.0f);
                    warden.setTarget(player);
                    world.spawnEntity(warden);
                }
            }
        }
        Iterator<UUID> it = FOLLOWS.keySet().iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            if (world.getPlayerByUuid(uuid) == null) {
                it.remove();
            }
        }
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
                if (e instanceof DingdongChicken chicken && chicken.isAlive() && !chicken.isRemoved()
                        && chicken.squaredDistanceTo(
                                entry.center.getX(), entry.center.getY(), entry.center.getZ())
                                <= VILLAGE_RADIUS * VILLAGE_RADIUS) {
                    count++;
                }
            }
            for (int i = count; i < 3; i++) {
                BlockPos spot = findSpawnSpot(world, entry.center, 16);
                DingdongChicken chicken = ModEntities.DINGDONG_CHICKEN.create(world);
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
