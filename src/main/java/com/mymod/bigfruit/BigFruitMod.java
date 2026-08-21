package com.mymod.bigfruit;

import com.mymod.bigfruit.item.ModItems;
import com.mymod.bigfruit.registry.ModEntities;
import com.mymod.bigfruit.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigFruitMod implements ModInitializer {
	public static final String MOD_ID = "big-fruit-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		ModSounds.register();
		ModItems.registerItems();
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
