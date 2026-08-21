package com.mymod.bigfruit.client;

import com.mymod.bigfruit.client.render.BigDogBillboardRenderer;
import com.mymod.bigfruit.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BigFruitModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.BIG_DOG, BigDogBillboardRenderer::new);
	}
}