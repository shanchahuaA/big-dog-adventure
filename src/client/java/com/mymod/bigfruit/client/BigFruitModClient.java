package com.mymod.bigfruit.client;

import com.mymod.bigfruit.client.render.BigDogBillboardRenderer;
import com.mymod.bigfruit.client.sound.BigDogEntitySound;
import com.mymod.bigfruit.entity.BigDogEntity;
import com.mymod.bigfruit.registry.ModEntities;
import com.mymod.bigfruit.registry.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BigFruitModClient implements ClientModInitializer {
	private static final Map<Integer, SoundInstance> CHARGE_SOUNDS = new HashMap<>();
	private static final Map<Integer, SoundInstance> CALL_SOUNDS = new HashMap<>();
	private static net.minecraft.client.world.ClientWorld currentWorld = null;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.BIG_DOG, BigDogBillboardRenderer::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Handle world switch: clear old sounds when world instance changes (dimension/server switch, ID reuse)
			if (client.world != currentWorld) {
				for (var s : CHARGE_SOUNDS.values()) MinecraftClient.getInstance().getSoundManager().stop(s);
				for (var s : CALL_SOUNDS.values()) MinecraftClient.getInstance().getSoundManager().stop(s);
				CHARGE_SOUNDS.clear();
				CALL_SOUNDS.clear();
				currentWorld = client.world;
			}
			if (client.world == null) {
				return;
			}
			// Iterate all big dogs
			for (var e : client.world.getEntitiesByClass(BigDogEntity.class, new Box(-1e7, -1e7, -1e7, 1e7, 1e7, 1e7), entity -> true)) {
				BigDogEntity dog = (BigDogEntity) e;
				int id = dog.getId();
				BigDogEntity.State state = dog.getState();
				boolean alive = dog.isAlive() && !dog.isRemoved();

				// Charge sound - single play per CHARGING entry, not looped via isPlaying
				if (state == BigDogEntity.State.CHARGING && alive) {
					if (!CHARGE_SOUNDS.containsKey(id)) {
						SoundInstance sound = new BigDogEntitySound(dog, ModSounds.BIG_DOG_CHARGE_LOOP, SoundCategory.HOSTILE, 1.2f, 1.0f);
						MinecraftClient.getInstance().getSoundManager().play(sound);
						CHARGE_SOUNDS.put(id, sound);
					}
				} else {
					SoundInstance s = CHARGE_SOUNDS.remove(id);
					if (s != null) MinecraftClient.getInstance().getSoundManager().stop(s);
				}
				// Call sound - single play per FIRING entry
				if (state == BigDogEntity.State.FIRING && alive) {
					if (!CALL_SOUNDS.containsKey(id)) {
						SoundInstance sound = new BigDogEntitySound(dog, ModSounds.BIG_DOG_CALL, SoundCategory.HOSTILE, 2.0f, 1.0f);
						MinecraftClient.getInstance().getSoundManager().play(sound);
						CALL_SOUNDS.put(id, sound);
					}
				} else {
					SoundInstance s = CALL_SOUNDS.remove(id);
					if (s != null) MinecraftClient.getInstance().getSoundManager().stop(s);
				}
				// Cleanup if dead
				if (!alive) {
					SoundInstance s1 = CHARGE_SOUNDS.remove(id);
					if (s1 != null) MinecraftClient.getInstance().getSoundManager().stop(s1);
					SoundInstance s2 = CALL_SOUNDS.remove(id);
					if (s2 != null) MinecraftClient.getInstance().getSoundManager().stop(s2);
				}
			}
			// Cleanup removed entities
			Iterator<Map.Entry<Integer, SoundInstance>> it = CHARGE_SOUNDS.entrySet().iterator();
			while (it.hasNext()) {
				var entry = it.next();
				var ent = client.world.getEntityById(entry.getKey());
				if (ent == null || !ent.isAlive() || ent.isRemoved() || !(ent instanceof BigDogEntity) || ((BigDogEntity) ent).getState() != BigDogEntity.State.CHARGING) {
					MinecraftClient.getInstance().getSoundManager().stop(entry.getValue());
					it.remove();
				}
			}
			Iterator<Map.Entry<Integer, SoundInstance>> it2 = CALL_SOUNDS.entrySet().iterator();
			while (it2.hasNext()) {
				var entry = it2.next();
				var ent = client.world.getEntityById(entry.getKey());
				if (ent == null || !ent.isAlive() || ent.isRemoved() || !(ent instanceof BigDogEntity) || ((BigDogEntity) ent).getState() != BigDogEntity.State.FIRING) {
					MinecraftClient.getInstance().getSoundManager().stop(entry.getValue());
					it2.remove();
				}
			}
		});
	}
}