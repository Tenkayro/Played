package de.tenkayro.client;

import de.tenkayro.client.config.PlayedConfig;
import net.fabricmc.api.ClientModInitializer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class PlayedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		java.nio.file.Path gameDirectory = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
		PlayedConfig.initialize(gameDirectory);
		PlaytimeStorage.initialize(gameDirectory);
		PlayedHud.register();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PlaytimeTracker.stop());
		// Connection callbacks may run on Netty's I/O thread. Keep every tracker
		// state transition on the client thread, just like the tick callback.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PlaytimeTracker::stop));
		ClientTickEvents.END_CLIENT_TICK.register(PlaytimeTracker::tick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("played")
				.executes(context -> sendStats(context.getSource()))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("session")
					.executes(context -> sendStat(context.getSource(), "Session", PlaytimeTracker.getSessionPlaytimeMillis())))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("current")
					.executes(context -> sendStat(
						context.getSource(),
						"Current Server/World",
						PlaytimeTracker.getCurrentPlaytimeMillis(net.minecraft.client.Minecraft.getInstance())
					)))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("total")
					.executes(context -> sendStat(context.getSource(), "Total (All-Time)", PlaytimeStorage.getAllTimeMillis())))
			);
		});
	}

	private static int sendStats(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("§d§lPlaytime Stats:"));
		sendStat(source, "Session", PlaytimeTracker.getSessionPlaytimeMillis());
		sendStat(
			source,
			"Current Server/World",
			PlaytimeTracker.getCurrentPlaytimeMillis(net.minecraft.client.Minecraft.getInstance())
		);
		return sendStat(source, "Total (All-Time)", PlaytimeStorage.getAllTimeMillis());
	}

	private static int sendStat(FabricClientCommandSource source, String label, long millis) {
		source.sendFeedback(Component.literal("§7" + label + ": §f" + PlayedConfig.formatTime(millis)));
		return 1;
	}
}
