package de.tenkayro.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public class PlayedModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Played Configuration").withStyle(ChatFormatting.LIGHT_PURPLE))
				.setSavingRunnable(PlayedConfig::save);

			builder.setGlobalized(true);
			builder.setGlobalizedExpanded(false);

			ConfigEntryBuilder entryBuilder = builder.entryBuilder();
			this.addTrackingCategory(builder, entryBuilder);
			this.addMenusAndListsCategory(builder, entryBuilder);
			this.addInGameDisplayCategory(builder, entryBuilder);
			this.addSortingCategory(builder, entryBuilder);
			return builder.build();
		};
	}

	private void addTrackingCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory tracking = builder.getOrCreateCategory(Component.literal("Tracking"));
		var targets = entryBuilder.startSubCategory(Component.literal("Playtime Tracking"))
			.setExpanded(true)
			.setTooltip(Component.literal("Choose where Played records playtime"));

		targets.add(entryBuilder.startBooleanToggle(Component.literal("Track Singleplayer Worlds"), PlayedConfig.shouldTrackWorlds())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Track playtime in singleplayer worlds"))
			.setSaveConsumer(PlayedConfig::setTrackWorlds)
			.build());
		targets.add(entryBuilder.startBooleanToggle(Component.literal("Track Multiplayer Servers"), PlayedConfig.shouldTrackServers())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Track playtime on multiplayer servers"))
			.setSaveConsumer(PlayedConfig::setTrackServers)
			.build());
		tracking.addEntry(targets.build());

		var afk = entryBuilder.startSubCategory(Component.literal("AFK Detection"))
			.setExpanded(true)
			.setTooltip(Component.literal("Optionally exclude inactive time"));
		afk.add(entryBuilder.startBooleanToggle(Component.literal("Ignore AFK Time"), PlayedConfig.shouldPauseOnAfk())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Pause playtime tracking when AFK"))
			.setSaveConsumer(PlayedConfig::setPauseOnAfk)
			.build());
		afk.add(entryBuilder.startIntField(Component.literal("AFK Timeout"), PlayedConfig.getAfkTimeoutSeconds())
			.setDefaultValue(900)
			.setMin(0)
			.setTooltip(Component.literal("Seconds of inactivity before pausing playtime tracking"))
			.setSaveConsumer(PlayedConfig::setAfkTimeoutSeconds)
			.build());
		tracking.addEntry(afk.build());
	}

	private void addMenusAndListsCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory display = builder.getOrCreateCategory(Component.literal("Menus & Lists"));

		var lists = entryBuilder.startSubCategory(Component.literal("World & Server Lists"))
			.setExpanded(true)
			.setTooltip(Component.literal("Configure playtime shown next to worlds and servers"));
		lists.add(entryBuilder.startBooleanToggle(Component.literal("Show Singleplayer Playtime"), PlayedConfig.shouldShowWorldPlaytime())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Show playtime in the singleplayer world list"))
			.setSaveConsumer(PlayedConfig::setShowWorldPlaytime)
			.build());
		lists.add(entryBuilder.startBooleanToggle(Component.literal("Show Multiplayer Playtime"), PlayedConfig.shouldShowServerPlaytime())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Show playtime in the multiplayer server list"))
			.setSaveConsumer(PlayedConfig::setShowServerPlaytime)
			.build());
		lists.add(entryBuilder.startBooleanToggle(Component.literal("List Background"), PlayedConfig.shouldShowListBackground())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show a translucent background behind list playtime"))
			.setSaveConsumer(PlayedConfig::setShowListBackground)
			.build());
		lists.add(entryBuilder.startStrField(Component.literal("List Template"), PlayedConfig.getListPrefix())
			.setDefaultValue("<gradient:#a855f7:#ec4899>\u23F1 {time}</gradient>")
			.setTooltip(Component.literal("Supports {time}, {session}, {current}, and {total}"))
			.setSaveConsumer(PlayedConfig::setListPrefix)
			.build());
		display.addEntry(lists.build());

		var menus = entryBuilder.startSubCategory(Component.literal("Menus"))
			.setExpanded(true)
			.setTooltip(Component.literal("Configure title-screen and pause-menu playtime"));
		menus.add(entryBuilder.startBooleanToggle(Component.literal("Show Main Menu Playtime"), PlayedConfig.shouldShowTotalPlaytime())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Show total playtime on the main menu"))
			.setSaveConsumer(PlayedConfig::setShowTotalPlaytime)
			.build());
		menus.add(entryBuilder.startStrField(Component.literal("Main Menu Template"), PlayedConfig.getMainMenuPrefix())
			.setDefaultValue("<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>")
			.setTooltip(Component.literal("Supports {time}, {session}, {current}, and {total}"))
			.setSaveConsumer(PlayedConfig::setMainMenuPrefix)
			.build());
		menus.add(entryBuilder.startBooleanToggle(Component.literal("Show Pause Menu Playtime"), PlayedConfig.shouldShowIngamePlaytime())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show playtime on the pause menu"))
			.setSaveConsumer(PlayedConfig::setShowIngamePlaytime)
			.build());
		menus.add(entryBuilder.startStrField(Component.literal("Pause Menu Template"), PlayedConfig.getPauseMenuPrefix())
			.setDefaultValue("<gradient:#a855f7:#ec4899>Playtime: {time}</gradient>")
			.setTooltip(Component.literal("Supports {time}, {session}, {current}, and {total}"))
			.setSaveConsumer(PlayedConfig::setPauseMenuPrefix)
			.build());
		display.addEntry(menus.build());

	}

	private void addInGameDisplayCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory display = builder.getOrCreateCategory(Component.literal("In-Game Display"));
		var locations = entryBuilder.startSubCategory(Component.literal("Display Locations"))
			.setExpanded(true)
			.setTooltip(Component.literal("Choose one or more places to show your playtime"));
		locations.add(entryBuilder.startBooleanToggle(Component.literal("Corner Overlay"), PlayedConfig.shouldShowHudPlaytime())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show playtime in a freely positioned overlay"))
			.setSaveConsumer(PlayedConfig::setShowHudPlaytime)
			.build());
		locations.add(entryBuilder.startBooleanToggle(Component.literal("Action Bar"), PlayedConfig.shouldShowActionBarPlaytime())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show playtime above the hotbar"))
			.setSaveConsumer(PlayedConfig::setShowActionBarPlaytime)
			.build());
		locations.add(entryBuilder.startBooleanToggle(Component.literal("Tab List"), PlayedConfig.shouldShowTabListPlaytime())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show playtime below the player list while holding Tab"))
			.setSaveConsumer(PlayedConfig::setShowTabListPlaytime)
			.build());
		locations.add(entryBuilder.startBooleanToggle(Component.literal("Scoreboard Sidebar"), PlayedConfig.shouldShowScoreboardPlaytime())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show playtime in a compact sidebar on the right"))
			.setSaveConsumer(PlayedConfig::setShowScoreboardPlaytime)
			.build());
		display.addEntry(locations.build());

		var appearance = entryBuilder.startSubCategory(Component.literal("Corner Overlay Layout"))
			.setExpanded(true)
			.setTooltip(Component.literal("These options only affect the Corner Overlay"));
		appearance.add(entryBuilder.startBooleanToggle(Component.literal("Background"), PlayedConfig.shouldShowHudBackground())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show a translucent background behind the HUD text"))
			.setSaveConsumer(PlayedConfig::setShowHudBackground)
			.build());
		appearance.add(entryBuilder.startEnumSelector(Component.literal("Position"), PlayedConfig.HudPosition.class, PlayedConfig.getHudPosition())
			.setDefaultValue(PlayedConfig.HudPosition.TOP_LEFT)
			.setTooltip(Component.literal("Choose where the playtime display appears"))
			.setSaveConsumer(PlayedConfig::setHudPosition)
			.build());
		appearance.add(entryBuilder.startIntSlider(Component.literal("Size (%)"), PlayedConfig.getHudScalePercent(), 50, 200)
			.setDefaultValue(100)
			.setTooltip(Component.literal("Scale the HUD between 50% and 200%"))
			.setSaveConsumer(PlayedConfig::setHudScalePercent)
			.build());
		appearance.add(entryBuilder.startIntField(Component.literal("Horizontal Margin"), PlayedConfig.getHudHorizontalOffset())
			.setDefaultValue(5)
			.setMin(0)
			.setMax(1000)
			.setTooltip(Component.literal("Horizontal distance from the selected corner"))
			.setSaveConsumer(PlayedConfig::setHudHorizontalOffset)
			.build());
		appearance.add(entryBuilder.startIntField(Component.literal("Vertical Margin"), PlayedConfig.getHudVerticalOffset())
			.setDefaultValue(5)
			.setMin(0)
			.setMax(1000)
			.setTooltip(Component.literal("Vertical distance from the selected corner"))
			.setSaveConsumer(PlayedConfig::setHudVerticalOffset)
			.build());
		display.addEntry(appearance.build());

		var content = entryBuilder.startSubCategory(Component.literal("Content & Format"))
			.setExpanded(true)
			.setTooltip(Component.literal("Shared by all in-game display locations"));
		content.add(entryBuilder.startEnumSelector(Component.literal("Time Format"), PlayedConfig.TimeFormatMode.class, PlayedConfig.getTimeFormat())
			.setDefaultValue(PlayedConfig.TimeFormatMode.VERBOSE)
			.setTooltip(Component.literal("Choose how durations are written"))
			.setSaveConsumer(PlayedConfig::setTimeFormat)
			.build());
		content.add(entryBuilder.startStrField(Component.literal("Text Template"), PlayedConfig.getHudTemplate())
			.setDefaultValue("<gradient:#a855f7:#ec4899>\u23F1 {current}</gradient>")
			.setTooltip(Component.literal("Supports {time}, {session}, {current}, and {total}"))
			.setSaveConsumer(PlayedConfig::setHudTemplate)
			.build());
		display.addEntry(content.build());
	}

	private void addSortingCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory sorting = builder.getOrCreateCategory(Component.literal("List Management"));
		var sortOptions = entryBuilder.startSubCategory(Component.literal("Playtime Sorting"))
			.setExpanded(true)
			.setTooltip(Component.literal("Optionally reorder worlds and servers by playtime"));
		sortOptions.add(entryBuilder.startEnumSelector(Component.literal("Singleplayer Worlds"), PlayedConfig.SortMode.class, PlayedConfig.getSortWorldList())
			.setDefaultValue(PlayedConfig.SortMode.DISABLED)
			.setTooltip(Component.literal("Sort the singleplayer world list by playtime"))
			.setSaveConsumer(PlayedConfig::setSortWorldList)
			.build());
		sortOptions.add(entryBuilder.startEnumSelector(Component.literal("Multiplayer Servers"), PlayedConfig.SortMode.class, PlayedConfig.getSortServerList())
			.setDefaultValue(PlayedConfig.SortMode.DISABLED)
			.setTooltip(Component.literal("Sort the multiplayer server list by playtime"))
			.setSaveConsumer(PlayedConfig::setSortServerList)
			.build());
		sorting.addEntry(sortOptions.build());

		var resetButtons = entryBuilder.startSubCategory(Component.literal("Reset Controls"))
			.setExpanded(true)
			.setTooltip(Component.literal("Configure per-world and per-server reset buttons"));
		resetButtons.add(entryBuilder.startBooleanToggle(Component.literal("Singleplayer Reset Button"), PlayedConfig.shouldShowEditWorldResetButton())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Show the reset button in the edit world screen"))
			.setSaveConsumer(PlayedConfig::setShowEditWorldResetButton)
			.build());
		resetButtons.add(entryBuilder.startBooleanToggle(Component.literal("Multiplayer Reset Button"), PlayedConfig.shouldShowEditServerResetButton())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Show the reset button in the edit server screen"))
			.setSaveConsumer(PlayedConfig::setShowEditServerResetButton)
			.build());
		sorting.addEntry(resetButtons.build());
	}
}
