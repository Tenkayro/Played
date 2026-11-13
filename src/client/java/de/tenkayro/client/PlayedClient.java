package de.tenkayro.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.tenkayro.client.config.PlayedSettings;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
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
		Path gameDirectory = FabricLoader.getInstance().getGameDir();
		PlayedConfig.initialize(gameDirectory);
		PlaytimeStorage.initialize(gameDirectory);
		PlaytimeHistory.initialize(gameDirectory);
		PlayedHud.register();
		registerNavigation();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PlaytimeTracker.stop());
		// Connection callbacks may run on Netty's I/O thread. Keep every tracker
		// state transition on the client thread, just like the tick callback.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PlaytimeTracker::stop));
		ClientTickEvents.END_CLIENT_TICK.register(PlaytimeTracker::tick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("played")
				.executes(context -> sendStats(context.getSource()))
				.then(openCommand("stats", () -> new PlayedStatsScreen(null)))
				.then(openCommand("config", () -> PlayedSettings.createScreen(null)))
				.then(openCommand("hud", () -> new PlayedHudEditorScreen(null)))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("today")
					.executes(context -> sendStat(context.getSource(), "played.command.today", PlaytimeHistory.day(LocalDate.now()))))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("week")
					.executes(context -> sendStat(context.getSource(), "played.command.week", PlaytimeHistory.week(LocalDate.now()))))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("session")
					.executes(context -> sendStat(context.getSource(), "played.command.session", PlaytimeTracker.getSessionPlaytimeMillis()))
					.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("reset")
						.executes(context -> {
							PlaytimeTracker.resetSession();
							context.getSource().sendFeedback(Component.translatable("played.command.session_reset").withStyle(net.minecraft.ChatFormatting.GREEN));
							return 1;
						})))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("help")
					.executes(context -> {
						context.getSource().sendFeedback(Component.translatable("played.command.help").withStyle(net.minecraft.ChatFormatting.AQUA));
						return 1;
					}))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("current")
					.executes(context -> sendStat(
						context.getSource(),
						"played.command.current",
						PlaytimeTracker.getCurrentPlaytimeMillis(Minecraft.getInstance())
					)))
				.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("total")
					.executes(context -> sendStat(context.getSource(), "played.command.total", PlaytimeStorage.getAllTimeMillis())))
			);
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> openCommand(String name,
		Supplier<Screen> screen) {
		return LiteralArgumentBuilder.<FabricClientCommandSource>literal(name).executes(context -> {
			var client = context.getSource().getClient();
			// Run after the chat screen has finished closing.
			client.schedule(() -> client.gui.setScreen(screen.get()));
			return 1;
		});
	}

	private static void registerNavigation() {
		var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("played", "main"));
		var stats = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.played.stats", InputConstants.UNKNOWN.getValue(), category));
		var hud = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.played.toggle_hud", InputConstants.UNKNOWN.getValue(), category));
		var editor = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.played.edit_hud", InputConstants.UNKNOWN.getValue(), category));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (stats.consumeClick()) client.gui.setScreen(new PlayedStatsScreen(client.gui.screen()));
			while (hud.consumeClick()) PlayedConfig.setShowHudPlaytime(!PlayedConfig.shouldShowHudPlaytime());
			while (editor.consumeClick()) client.gui.setScreen(new PlayedHudEditorScreen(client.gui.screen()));
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof PauseScreen) {
				var button = Button.builder(Component.literal("⏱"),
					b -> client.gui.setScreen(new PlayedStatsScreen(screen))).bounds(width - 24, 4, 20, 20).build();
				button.setTooltip(Tooltip.create(Component.translatable("played.stats.title")));
				Screens.getWidgets(screen).add(button);
			}
		});
	}

	private static int sendStats(FabricClientCommandSource source) {
		source.sendFeedback(Component.translatable("played.command.title").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.BOLD));
		sendStat(source, "played.command.session", PlaytimeTracker.getSessionPlaytimeMillis());
		sendStat(
			source,
			"played.command.current",
			PlaytimeTracker.getCurrentPlaytimeMillis(Minecraft.getInstance())
		);
		return sendStat(source, "played.command.total", PlaytimeStorage.getAllTimeMillis());
	}

	private static int sendStat(FabricClientCommandSource source, @org.jspecify.annotations.NonNull String label, long millis) {
		source.sendFeedback(Component.translatable("played.command.stat", Component.translatable(label), Component.literal(PlayedConfig.formatTime(millis)).withStyle(net.minecraft.ChatFormatting.WHITE)).withStyle(net.minecraft.ChatFormatting.GRAY));
		return 1;
	}
}
