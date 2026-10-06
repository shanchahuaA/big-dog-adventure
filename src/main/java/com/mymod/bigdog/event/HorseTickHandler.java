package com.mymod.bigdog.event;

import com.mymod.bigdog.ai.ChickenBgmManager;
import com.mymod.bigdog.entity.BigDogEntity;
import com.mymod.bigdog.item.ModItems;
import com.mymod.bigdog.util.HorseColorUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HorseTickHandler {
    private HorseTickHandler() {}

    private static final int CADENCE = 10;
    private static final long PUNISH_COOLDOWN_TICKS = 600L;
    private static final long TALISMAN_SUBLIMED_TICKS = 1800L;
    private static final long SUBLIMED_CORE_TICKS = 12000L;

    private static final Set<Block> SCULK_FAMILY = Set.of(
            Blocks.SCULK, Blocks.SCULK_VEIN, Blocks.SCULK_CATALYST,
            Blocks.SCULK_SHRIEKER, Blocks.SCULK_SENSOR);

    private static final Map<UUID, Long> TALISMAN_TIME = new HashMap<>();
    private static final Map<UUID, Long> PUNISH_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> SUBLIMED_SINCE = new HashMap<>();
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();
    private static final Set<UUID> SEEN_HORSES = new HashSet<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getTime() % CADENCE == 0) {
                    tickWorld(world, world.getTime());
                }
            }
        });
    }

    private static void tickWorld(ServerWorld world, long time) {
        for (Entity e : world.iterateEntities()) {
            if (!(e instanceof HorseEntity horse) || !horse.isAlive() || horse.isRemoved()) {
                continue;
            }
            if (HorseColorUtil.getRaw(horse) == null && !horse.isBaby()) {
                if (world.random.nextFloat() < 0.30f) {
                    HorseColorUtil.setSublimed(horse);
                } else {
                    HorseColorUtil.setRaw(horse, HorseColorUtil.NONE);
                }
            }
            String color = HorseColorUtil.getColor(horse);
            boolean playerRidden = horse.getFirstPassenger() instanceof PlayerEntity;
            HorseColorUtil.updateDemonizedSpeed(horse, HorseColorUtil.DEMONIZED.equals(color) && playerRidden);
            if (color != null) {
                HorseColorUtil.applyMaxHealth(horse, color, false);
            }
            if (color != null && SEEN_HORSES.add(horse.getUuid()) && !horse.hasArmorInSlot()) {
                HorseColorUtil.equipDyedArmor(horse, HorseColorUtil.DEMONIZED.equals(color)
                        ? HorseColorUtil.DEMONIZED_DYE : HorseColorUtil.SUBLIMED_DYE);
            }
        }
        SEEN_HORSES.removeIf(uuid -> world.getEntity(uuid) == null);

        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID uuid = player.getUuid();
            online.add(uuid);
            if (!player.isAlive() || player.isRemoved()) {
                clearPlayerState(uuid);
                continue;
            }
            Entity vehicle = player.getVehicle();
            HorseEntity horse = null;
            if (vehicle instanceof HorseEntity) {
                horse = (HorseEntity) vehicle;
            }
            String color = horse != null ? HorseColorUtil.getColor(horse) : null;
            float health = player.getHealth();

            boolean undeadHit = detectUndeadHit(player, uuid, health);
            if (HorseColorUtil.DEMONIZED.equals(color)) {
                long talismanTicks = undeadHit ? 0L : TALISMAN_TIME.getOrDefault(uuid, 0L);
                if (!undeadHit && BigDogEntity.isWearingTalisman(player)) {
                    talismanTicks += CADENCE;
                } else if (!undeadHit) {
                    talismanTicks = 0L;
                }
                if (talismanTicks >= TALISMAN_SUBLIMED_TICKS && horse != null) {
                    HorseColorUtil.setSublimed(horse);
                    SUBLIMED_SINCE.put(uuid, time);
                    talismanTicks = 0L;
                }
                TALISMAN_TIME.put(uuid, talismanTicks);
                if (time % 100 == 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 110, 0));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 110, 1));
                }
            } else {
                TALISMAN_TIME.put(uuid, 0L);
            }

            if (HorseColorUtil.SUBLIMED.equals(color)) {
                if (undeadHit && horse != null) {
                    HorseColorUtil.setDemonized(horse);
                    SUBLIMED_SINCE.remove(uuid);
                    TALISMAN_TIME.put(uuid, 0L);
                } else if (!SUBLIMED_SINCE.containsKey(uuid)) {
                    SUBLIMED_SINCE.put(uuid, time);
                } else if (time - SUBLIMED_SINCE.get(uuid) >= SUBLIMED_CORE_TICKS) {
                    SUBLIMED_SINCE.put(uuid, time);
                    if (horse != null && !consumeCore(player)) {
                        HorseColorUtil.setDemonized(horse);
                    }
                }
            } else {
                SUBLIMED_SINCE.remove(uuid);
            }

            if (horse != null) {
                long until = PUNISH_UNTIL.getOrDefault(uuid, 0L);
                if (time >= until && (nearWarden(world, player) || onSculk(world, player))) {
                    PUNISH_UNTIL.put(uuid, time + PUNISH_COOLDOWN_TICKS);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 4));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 600, 4));
                    ChickenBgmManager.forceTrigger(world, player);
                }
            }

            LAST_HEALTH.put(uuid, health);
        }
        TALISMAN_TIME.keySet().removeIf(uuid -> !online.contains(uuid));
        PUNISH_UNTIL.keySet().removeIf(uuid -> !online.contains(uuid));
        SUBLIMED_SINCE.keySet().removeIf(uuid -> !online.contains(uuid));
        LAST_HEALTH.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    private static void clearPlayerState(UUID uuid) {
        TALISMAN_TIME.remove(uuid);
        PUNISH_UNTIL.remove(uuid);
        SUBLIMED_SINCE.remove(uuid);
        LAST_HEALTH.remove(uuid);
    }

    private static boolean detectUndeadHit(ServerPlayerEntity player, UUID uuid, float health) {
        Float last = LAST_HEALTH.get(uuid);
        if (last == null || health >= last || player.hurtTime <= 0) {
            return false;
        }
        LivingEntity attacker = player.getAttacker();
        if (attacker == null) {
            return false;
        }
        if (attacker.getGroup() != EntityGroup.UNDEAD) {
            return false;
        }
        return attacker instanceof MobEntity mob && mob.getTarget() == player;
    }

    private static boolean nearWarden(ServerWorld world, PlayerEntity player) {
        return !world.getEntitiesByClass(WardenEntity.class,
                player.getBoundingBox().expand(2.5), WardenEntity::isAlive).isEmpty();
    }

    private static boolean onSculk(ServerWorld world, PlayerEntity player) {
        var feet = player.getBlockPos();
        return SCULK_FAMILY.contains(world.getBlockState(feet).getBlock())
                || SCULK_FAMILY.contains(world.getBlockState(feet.down()).getBlock());
    }

    private static boolean consumeCore(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack.isOf(ModItems.ELEMENT_CORE)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }
}
