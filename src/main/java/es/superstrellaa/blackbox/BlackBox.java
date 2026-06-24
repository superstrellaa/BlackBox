package es.superstrellaa.blackbox;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlackBox implements ModInitializer {
	public static final String MOD_ID = "blackbox";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("BlackBox Common initialized successfully!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
