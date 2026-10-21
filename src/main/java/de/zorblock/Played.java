package de.zorblock;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Played implements ModInitializer {
	public static final String MOD_ID = "played";
	public static final String MOD_NAME = "Played";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("{} loaded.", MOD_NAME);
	}
}
