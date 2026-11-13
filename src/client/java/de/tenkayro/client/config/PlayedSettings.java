package de.tenkayro.client.config;

import dev.isxander.yacl3.api.ButtonOption;
import net.minecraft.client.Minecraft;
import de.tenkayro.client.PlayedStatsScreen;
import de.tenkayro.client.PlayedHudEditorScreen;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings({"null", "deprecation"})
public final class PlayedSettings {
	private PlayedSettings() {}
	public static Screen createScreen(Screen parent) {
		return YetAnotherConfigLib.createBuilder().title(Component.translatable("played.config.title"))
			.category(tracking()).category(inGame()).category(menusAndLists()).category(management()).category(tools())
			.save(PlayedConfig::save).build().generateScreen(parent);
	}

	private static ConfigCategory tools() {
		return ConfigCategory.createBuilder().name(Component.translatable("played.stats.title"))
			.option(ButtonOption.createBuilder().name(Component.translatable("played.stats.title"))
				.text(Component.translatable("played.open")).action((screen, option) ->
					Minecraft.getInstance().gui.setScreen(new PlayedStatsScreen(screen))).build())
			.option(ButtonOption.createBuilder().name(Component.translatable("played.hud.edit"))
				.description(description("played.config.hud_editor.description"))
				.text(Component.translatable("played.open")).action((screen, option) ->
					Minecraft.getInstance().gui.setScreen(new PlayedHudEditorScreen(screen))).build())
			.build();
	}

	private static ConfigCategory tracking() {
		return ConfigCategory.createBuilder().name(Component.translatable("played.config.category.tracking"))
			.tooltip(Component.translatable("played.config.category.tracking.description"))
			.option(toggle("played.config.option.singleplayer_worlds", "played.config.option.singleplayer_worlds.description", true, PlayedConfig::shouldTrackWorlds, PlayedConfig::setTrackWorlds))
			.option(toggle("played.config.option.multiplayer_servers", "played.config.option.multiplayer_servers.description", true, PlayedConfig::shouldTrackServers, PlayedConfig::setTrackServers))
			.option(toggle("played.config.option.ignore_afk_time", "played.config.option.ignore_afk_time.description", false, PlayedConfig::shouldPauseOnAfk, PlayedConfig::setPauseOnAfk))
			.option(integerField("played.config.option.afk_timeout", "played.config.option.afk_timeout.description", 900, 0, 86400, PlayedConfig::getAfkTimeoutSeconds, PlayedConfig::setAfkTimeoutSeconds))
			.option(integerField("played.config.option.break_reminder", "played.config.option.break_reminder.description", 0, 0, 480, PlayedConfig::getBreakReminderMinutes, PlayedConfig::setBreakReminderMinutes))
			.option(toggle("played.config.option.milestone_notifications", "played.config.option.milestone_notifications.description", false, PlayedConfig::shouldNotifyMilestones, PlayedConfig::setNotifyMilestones))
			.build();
	}

	private static ConfigCategory inGame() {
		return ConfigCategory.createBuilder().name(Component.translatable("played.config.category.in_game"))
			.tooltip(Component.translatable("played.config.category.in_game.description"))
			.option(toggle("played.config.option.corner_overlay", "played.config.option.corner_overlay.description", false, PlayedConfig::shouldShowHudPlaytime, PlayedConfig::setShowHudPlaytime))
			.option(toggle("played.config.option.action_bar", "played.config.option.action_bar.description", false, PlayedConfig::shouldShowActionBarPlaytime, PlayedConfig::setShowActionBarPlaytime))
			.option(toggle("played.config.option.tab_list", "played.config.option.tab_list.description", false, PlayedConfig::shouldShowTabListPlaytime, PlayedConfig::setShowTabListPlaytime))
			.option(toggle("played.config.option.scoreboard_sidebar", "played.config.option.scoreboard_sidebar.description", false, PlayedConfig::shouldShowScoreboardPlaytime, PlayedConfig::setShowScoreboardPlaytime))
			.option(enumOption("played.config.option.corner_position", "played.config.option.corner_position.description", PlayedConfig.HudPosition.TOP_LEFT, PlayedConfig.HudPosition.class, PlayedConfig::getHudPosition, PlayedConfig::setHudPosition))
			.option(toggle("played.config.option.corner_background", "played.config.option.corner_background.description", false, PlayedConfig::shouldShowHudBackground, PlayedConfig::setShowHudBackground))
			.option(integerSlider("played.config.option.corner_size", "played.config.option.corner_size.description", 100, 50, 200, 5, PlayedConfig::getHudScalePercent, PlayedConfig::setHudScalePercent))
			.option(integerField("played.config.option.horizontal_margin", "played.config.option.horizontal_margin.description", 5, 0, 1000, PlayedConfig::getHudHorizontalOffset, PlayedConfig::setHudHorizontalOffset))
			.option(integerField("played.config.option.vertical_margin", "played.config.option.vertical_margin.description", 5, 0, 1000, PlayedConfig::getHudVerticalOffset, PlayedConfig::setHudVerticalOffset))
			.option(enumOption("played.config.option.time_format", "played.config.option.time_format.description", PlayedConfig.TimeFormatMode.VERBOSE, PlayedConfig.TimeFormatMode.class, PlayedConfig::getTimeFormat, PlayedConfig::setTimeFormat))
			.option(text("played.config.option.display_text", "played.config.option.display_text.description", "<gradient:#a855f7:#ec4899>\u23F1 {current}</gradient>", PlayedConfig::getHudTemplate, PlayedConfig::setHudTemplate))
			.build();
	}

	private static ConfigCategory menusAndLists() {
		return ConfigCategory.createBuilder().name(Component.translatable("played.config.category.menus"))
			.tooltip(Component.translatable("played.config.category.menus.description"))
			.option(toggle("played.config.option.singleplayer_list", "played.config.option.singleplayer_list.description", true, PlayedConfig::shouldShowWorldPlaytime, PlayedConfig::setShowWorldPlaytime))
			.option(toggle("played.config.option.multiplayer_list", "played.config.option.multiplayer_list.description", true, PlayedConfig::shouldShowServerPlaytime, PlayedConfig::setShowServerPlaytime))
			.option(toggle("played.config.option.list_background", "played.config.option.list_background.description", false, PlayedConfig::shouldShowListBackground, PlayedConfig::setShowListBackground))
			.option(text("played.config.option.list_text", "played.config.option.list_text.description", "<gradient:#a855f7:#ec4899>\u23F1 {time}</gradient>", PlayedConfig::getListPrefix, PlayedConfig::setListPrefix))
			.option(toggle("played.config.option.main_menu", "played.config.option.main_menu.description", true, PlayedConfig::shouldShowTotalPlaytime, PlayedConfig::setShowTotalPlaytime))
			.option(text("played.config.option.main_menu_text", "played.config.option.main_menu_text.description", "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>", PlayedConfig::getMainMenuPrefix, PlayedConfig::setMainMenuPrefix))
			.option(toggle("played.config.option.pause_menu", "played.config.option.pause_menu.description", false, PlayedConfig::shouldShowIngamePlaytime, PlayedConfig::setShowIngamePlaytime))
			.option(text("played.config.option.pause_menu_text", "played.config.option.pause_menu_text.description", "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>", PlayedConfig::getPauseMenuPrefix, PlayedConfig::setPauseMenuPrefix))
			.build();
	}

	private static ConfigCategory management() {
		return ConfigCategory.createBuilder().name(Component.translatable("played.config.category.management"))
			.tooltip(Component.translatable("played.config.category.management.description"))
			.option(enumOption("played.config.option.sort_singleplayer_worlds", "played.config.option.sort_singleplayer_worlds.description", PlayedConfig.SortMode.DISABLED, PlayedConfig.SortMode.class, PlayedConfig::getSortWorldList, PlayedConfig::setSortWorldList))
			.option(enumOption("played.config.option.sort_multiplayer_servers", "played.config.option.sort_multiplayer_servers.description", PlayedConfig.SortMode.DISABLED, PlayedConfig.SortMode.class, PlayedConfig::getSortServerList, PlayedConfig::setSortServerList))
			.option(toggle("played.config.option.world_reset_button", "played.config.option.world_reset_button.description", true, PlayedConfig::shouldShowEditWorldResetButton, PlayedConfig::setShowEditWorldResetButton))
			.option(toggle("played.config.option.server_reset_button", "played.config.option.server_reset_button.description", true, PlayedConfig::shouldShowEditServerResetButton, PlayedConfig::setShowEditServerResetButton))
			.build();
	}

	private static Option<Boolean> toggle(String name, String help, boolean fallback,
		Supplier<Boolean> get, Consumer<Boolean> set) {
		return Option.<Boolean>createBuilder().name(Component.translatable(name)).description(description(help))
			.binding(fallback, get, set).controller(o -> BooleanControllerBuilder.create(o).coloured(true).valueFormatter(value -> Component.translatable(value ? "played.on" : "played.off"))).build();
	}

	private static Option<String> text(String name, String help, String fallback,
		Supplier<String> get, Consumer<String> set) {
		return Option.<String>createBuilder().name(Component.translatable(name)).description(description(help))
			.binding(fallback, get, set).controller(StringControllerBuilder::create).build();
	}

	private static Option<Integer> integerField(String name, String help, int fallback, int min, int max,
		Supplier<Integer> get, Consumer<Integer> set) {
		return Option.<Integer>createBuilder().name(Component.translatable(name)).description(description(help))
			.binding(fallback, get, set).controller(o -> IntegerFieldControllerBuilder.create(o).range(min, max)).build();
	}

	private static Option<Integer> integerSlider(String name, String help, int fallback, int min, int max, int step,
		Supplier<Integer> get, Consumer<Integer> set) {
		return Option.<Integer>createBuilder().name(Component.translatable(name)).description(description(help))
			.binding(fallback, get, set).controller(o -> IntegerSliderControllerBuilder.create(o).range(min, max).step(step)).build();
	}

	private static <E extends Enum<E>> Option<E> enumOption(String name, String help, E fallback, Class<E> type,
		Supplier<E> get, Consumer<E> set) {
		return Option.<E>createBuilder().name(Component.translatable(name)).description(description(help)).binding(fallback, get, set)
			.controller(o -> EnumControllerBuilder.create(o).enumClass(type).valueFormatter(v -> Component.translatable("played.enum." + type.getSimpleName().toLowerCase(java.util.Locale.ROOT) + "." + v.name().toLowerCase(java.util.Locale.ROOT)))).build();
	}

	private static OptionDescription description(String text) { return OptionDescription.of(Component.translatable(text)); }
}
