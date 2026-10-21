package de.tenkayro.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
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
public final class PlayedModMenu implements ModMenuApi {
	@Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return PlayedModMenu::createScreen; }

	private static Screen createScreen(Screen parent) {
		return YetAnotherConfigLib.createBuilder().title(Component.literal("Played Settings"))
			.category(tracking()).category(inGame()).category(menusAndLists()).category(management())
			.save(PlayedConfig::save).build().generateScreen(parent);
	}

	private static ConfigCategory tracking() {
		return ConfigCategory.createBuilder().name(Component.literal("Tracking"))
			.tooltip(Component.literal("Choose what counts toward your playtime"))
			.option(toggle("Singleplayer Worlds", "Track time spent in local worlds", true, PlayedConfig::shouldTrackWorlds, PlayedConfig::setTrackWorlds))
			.option(toggle("Multiplayer Servers", "Track time spent on multiplayer servers", true, PlayedConfig::shouldTrackServers, PlayedConfig::setTrackServers))
			.option(toggle("Ignore AFK Time", "Pause tracking after a period without input", false, PlayedConfig::shouldPauseOnAfk, PlayedConfig::setPauseOnAfk))
			.option(integerField("AFK Timeout", "Seconds without input before tracking pauses", 900, 0, 86400, PlayedConfig::getAfkTimeoutSeconds, PlayedConfig::setAfkTimeoutSeconds))
			.build();
	}

	private static ConfigCategory inGame() {
		return ConfigCategory.createBuilder().name(Component.literal("In-Game Display"))
			.tooltip(Component.literal("Choose where and how playtime appears while playing"))
			.option(toggle("Corner Overlay", "Show a freely positioned playtime overlay", false, PlayedConfig::shouldShowHudPlaytime, PlayedConfig::setShowHudPlaytime))
			.option(toggle("Action Bar", "Show playtime above the hotbar", false, PlayedConfig::shouldShowActionBarPlaytime, PlayedConfig::setShowActionBarPlaytime))
			.option(toggle("Tab List", "Show playtime below the player list", false, PlayedConfig::shouldShowTabListPlaytime, PlayedConfig::setShowTabListPlaytime))
			.option(toggle("Scoreboard Sidebar", "Show a compact playtime panel on the right", false, PlayedConfig::shouldShowScoreboardPlaytime, PlayedConfig::setShowScoreboardPlaytime))
			.option(enumOption("Corner Position", "Position of the Corner Overlay", PlayedConfig.HudPosition.TOP_LEFT, PlayedConfig.HudPosition.class, PlayedConfig::getHudPosition, PlayedConfig::setHudPosition))
			.option(toggle("Corner Background", "Draw a background behind the Corner Overlay", false, PlayedConfig::shouldShowHudBackground, PlayedConfig::setShowHudBackground))
			.option(integerSlider("Corner Size", "Scale the Corner Overlay", 100, 50, 200, 5, PlayedConfig::getHudScalePercent, PlayedConfig::setHudScalePercent))
			.option(integerField("Horizontal Margin", "Distance from the selected screen edge", 5, 0, 1000, PlayedConfig::getHudHorizontalOffset, PlayedConfig::setHudHorizontalOffset))
			.option(integerField("Vertical Margin", "Distance from the selected screen edge", 5, 0, 1000, PlayedConfig::getHudVerticalOffset, PlayedConfig::setHudVerticalOffset))
			.option(enumOption("Time Format", "Choose how durations are written", PlayedConfig.TimeFormatMode.VERBOSE, PlayedConfig.TimeFormatMode.class, PlayedConfig::getTimeFormat, PlayedConfig::setTimeFormat))
			.option(text("Display Text", "Supports {time}, {session}, {current}, and {total}", "<gradient:#a855f7:#ec4899>\u23F1 {current}</gradient>", PlayedConfig::getHudTemplate, PlayedConfig::setHudTemplate))
			.build();
	}

	private static ConfigCategory menusAndLists() {
		return ConfigCategory.createBuilder().name(Component.literal("Menus & Lists"))
			.tooltip(Component.literal("Playtime shown outside the active game HUD"))
			.option(toggle("Singleplayer List", "Show playtime next to each world", true, PlayedConfig::shouldShowWorldPlaytime, PlayedConfig::setShowWorldPlaytime))
			.option(toggle("Multiplayer List", "Show playtime next to each server", true, PlayedConfig::shouldShowServerPlaytime, PlayedConfig::setShowServerPlaytime))
			.option(toggle("List Background", "Draw a background behind list playtime", false, PlayedConfig::shouldShowListBackground, PlayedConfig::setShowListBackground))
			.option(text("List Text", "Supports {time}, {session}, {current}, and {total}", "<gradient:#a855f7:#ec4899>\u23F1 {time}</gradient>", PlayedConfig::getListPrefix, PlayedConfig::setListPrefix))
			.option(toggle("Main Menu", "Show total playtime on the title screen", true, PlayedConfig::shouldShowTotalPlaytime, PlayedConfig::setShowTotalPlaytime))
			.option(text("Main Menu Text", "Supports {time}, {session}, {current}, and {total}", "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>", PlayedConfig::getMainMenuPrefix, PlayedConfig::setMainMenuPrefix))
			.option(toggle("Pause Menu", "Show current playtime in the pause menu", false, PlayedConfig::shouldShowIngamePlaytime, PlayedConfig::setShowIngamePlaytime))
			.option(text("Pause Menu Text", "Supports {time}, {session}, {current}, and {total}", "<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>", PlayedConfig::getPauseMenuPrefix, PlayedConfig::setPauseMenuPrefix))
			.build();
	}

	private static ConfigCategory management() {
		return ConfigCategory.createBuilder().name(Component.literal("List Management"))
			.tooltip(Component.literal("Sorting and reset controls"))
			.option(enumOption("Sort Singleplayer Worlds", "Reorder worlds by tracked playtime", PlayedConfig.SortMode.DISABLED, PlayedConfig.SortMode.class, PlayedConfig::getSortWorldList, PlayedConfig::setSortWorldList))
			.option(enumOption("Sort Multiplayer Servers", "Reorder servers by tracked playtime", PlayedConfig.SortMode.DISABLED, PlayedConfig.SortMode.class, PlayedConfig::getSortServerList, PlayedConfig::setSortServerList))
			.option(toggle("World Reset Button", "Show a reset button when editing a world", true, PlayedConfig::shouldShowEditWorldResetButton, PlayedConfig::setShowEditWorldResetButton))
			.option(toggle("Server Reset Button", "Show a reset button when editing a server", true, PlayedConfig::shouldShowEditServerResetButton, PlayedConfig::setShowEditServerResetButton))
			.build();
	}

	private static Option<Boolean> toggle(String name, String help, boolean fallback,
		Supplier<Boolean> get, Consumer<Boolean> set) {
		return Option.<Boolean>createBuilder().name(Component.literal(name)).description(description(help))
			.binding(fallback, get, set).controller(o -> BooleanControllerBuilder.create(o).coloured(true)).build();
	}

	private static Option<String> text(String name, String help, String fallback,
		Supplier<String> get, Consumer<String> set) {
		return Option.<String>createBuilder().name(Component.literal(name)).description(description(help))
			.binding(fallback, get, set).controller(StringControllerBuilder::create).build();
	}

	private static Option<Integer> integerField(String name, String help, int fallback, int min, int max,
		Supplier<Integer> get, Consumer<Integer> set) {
		return Option.<Integer>createBuilder().name(Component.literal(name)).description(description(help))
			.binding(fallback, get, set).controller(o -> IntegerFieldControllerBuilder.create(o).range(min, max)).build();
	}

	private static Option<Integer> integerSlider(String name, String help, int fallback, int min, int max, int step,
		Supplier<Integer> get, Consumer<Integer> set) {
		return Option.<Integer>createBuilder().name(Component.literal(name)).description(description(help))
			.binding(fallback, get, set).controller(o -> IntegerSliderControllerBuilder.create(o).range(min, max).step(step)).build();
	}

	private static <E extends Enum<E>> Option<E> enumOption(String name, String help, E fallback, Class<E> type,
		Supplier<E> get, Consumer<E> set) {
		return Option.<E>createBuilder().name(Component.literal(name)).description(description(help)).binding(fallback, get, set)
			.controller(o -> EnumControllerBuilder.create(o).enumClass(type).valueFormatter(v -> Component.literal(v.toString()))).build();
	}

	private static OptionDescription description(String text) { return OptionDescription.of(Component.literal(text)); }
}
