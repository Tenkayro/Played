package de.zorblock.client.config;

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
			this.addDisplayCategory(builder, entryBuilder);
			this.addSortingCategory(builder, entryBuilder);
			return builder.build();
		};
	}

	private void addTrackingCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory tracking = builder.getOrCreateCategory(Component.literal("Tracking"));
		var targets = entryBuilder.startSubCategory(Component.literal("Playtime Tracking"))
			.setExpanded(true)
			.setTooltip(Component.literal("Choose where Played records playtime"));

		targets.add(entryBuilder.startBooleanToggle(Component.literal("Singleplayer"), PlayedConfig.shouldTrackWorlds())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Track playtime in singleplayer worlds"))
			.setSaveConsumer(PlayedConfig::setTrackWorlds)
			.build());
		targets.add(entryBuilder.startBooleanToggle(Component.literal("Multiplayer"), PlayedConfig.shouldTrackServers())
			.setDefaultValue(true)
			.setTooltip(Component.literal("Track playtime on multiplayer servers"))
			.setSaveConsumer(PlayedConfig::setTrackServers)
			.build());
		tracking.addEntry(targets.build());

		var afk = entryBuilder.startSubCategory(Component.literal("AFK Detection"))
			.setExpanded(true)
			.setTooltip(Component.literal("Optionally exclude inactive time"));
		afk.add(entryBuilder.startBooleanToggle(Component.literal("Pause AFK Time"), PlayedConfig.shouldPauseOnAfk())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Pause playtime tracking when AFK"))
			.setSaveConsumer(PlayedConfig::setPauseOnAfk)
			.build());
		afk.add(entryBuilder.startIntField(Component.literal("AFK Timeout (Seconds)"), PlayedConfig.getAfkTimeoutSeconds())
			.setDefaultValue(900)
			.setMin(0)
			.setTooltip(Component.literal("Seconds of inactivity before pausing playtime tracking"))
			.setSaveConsumer(PlayedConfig::setAfkTimeoutSeconds)
			.build());
		tracking.addEntry(afk.build());
	}

	private void addDisplayCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory display = builder.getOrCreateCategory(Component.literal("Display"));

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

		var hud = entryBuilder.startSubCategory(Component.literal("HUD"))
			.setExpanded(true)
			.setTooltip(Component.literal("Configure the optional in-game playtime HUD"));
		hud.add(entryBuilder.startBooleanToggle(Component.literal("Show Playtime HUD"), PlayedConfig.shouldShowHudPlaytime())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show a small playtime display while playing"))
			.setSaveConsumer(PlayedConfig::setShowHudPlaytime)
			.build());
		hud.add(entryBuilder.startBooleanToggle(Component.literal("HUD Background"), PlayedConfig.shouldShowHudBackground())
			.setDefaultValue(false)
			.setTooltip(Component.literal("Show a translucent background behind the HUD text"))
			.setSaveConsumer(PlayedConfig::setShowHudBackground)
			.build());
		hud.add(entryBuilder.startEnumSelector(Component.literal("HUD Position"), PlayedConfig.HudPosition.class, PlayedConfig.getHudPosition())
			.setDefaultValue(PlayedConfig.HudPosition.TOP_LEFT)
			.setTooltip(Component.literal("Choose the corner used as the HUD anchor"))
			.setSaveConsumer(PlayedConfig::setHudPosition)
			.build());
		hud.add(entryBuilder.startIntSlider(Component.literal("HUD Size (%)"), PlayedConfig.getHudScalePercent(), 50, 200)
			.setDefaultValue(100)
			.setTooltip(Component.literal("Scale the HUD between 50% and 200%"))
			.setSaveConsumer(PlayedConfig::setHudScalePercent)
			.build());
		hud.add(entryBuilder.startIntField(Component.literal("HUD Horizontal Offset"), PlayedConfig.getHudHorizontalOffset())
			.setDefaultValue(5)
			.setMin(0)
			.setMax(1000)
			.setTooltip(Component.literal("Horizontal distance from the selected corner"))
			.setSaveConsumer(PlayedConfig::setHudHorizontalOffset)
			.build());
		hud.add(entryBuilder.startIntField(Component.literal("HUD Vertical Offset"), PlayedConfig.getHudVerticalOffset())
			.setDefaultValue(5)
			.setMin(0)
			.setMax(1000)
			.setTooltip(Component.literal("Vertical distance from the selected corner"))
			.setSaveConsumer(PlayedConfig::setHudVerticalOffset)
			.build());
		hud.add(entryBuilder.startStrField(Component.literal("HUD Template"), PlayedConfig.getHudTemplate())
			.setDefaultValue("<gradient:#a855f7:#ec4899>\u23F1 {current}</gradient>")
			.setTooltip(Component.literal("Supports {time}, {session}, {current}, and {total}"))
			.setSaveConsumer(PlayedConfig::setHudTemplate)
			.build());
		display.addEntry(hud.build());

		var resetButtons = entryBuilder.startSubCategory(Component.literal("Reset Buttons"))
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
		display.addEntry(resetButtons.build());
	}

	private void addSortingCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
		ConfigCategory sorting = builder.getOrCreateCategory(Component.literal("Sorting & Formatting"));
		sorting.addEntry(entryBuilder.startEnumSelector(Component.literal("Sort Singleplayer List"), PlayedConfig.SortMode.class, PlayedConfig.getSortWorldList())
			.setDefaultValue(PlayedConfig.SortMode.DISABLED)
			.setTooltip(Component.literal("Sort the singleplayer world list by playtime"))
			.setSaveConsumer(PlayedConfig::setSortWorldList)
			.build());
		sorting.addEntry(entryBuilder.startEnumSelector(Component.literal("Sort Multiplayer List"), PlayedConfig.SortMode.class, PlayedConfig.getSortServerList())
			.setDefaultValue(PlayedConfig.SortMode.DISABLED)
			.setTooltip(Component.literal("Sort the multiplayer server list by playtime"))
			.setSaveConsumer(PlayedConfig::setSortServerList)
			.build());
		sorting.addEntry(entryBuilder.startEnumSelector(Component.literal("Time Format"), PlayedConfig.TimeFormatMode.class, PlayedConfig.getTimeFormat())
			.setDefaultValue(PlayedConfig.TimeFormatMode.VERBOSE)
			.setTooltip(Component.literal("Format used to display playtime"))
			.setSaveConsumer(PlayedConfig::setTimeFormat)
			.build());
	}
}
