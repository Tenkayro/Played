package de.tenkayro.client.mixin;

import de.tenkayro.client.PlaytimeStorage;
import de.tenkayro.client.config.PlayedConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin {
	@ModifyVariable(method = "fillLevels", at = @At("HEAD"), argsOnly = true)
	private List<LevelSummary> played$sortWorldLevels(List<LevelSummary> levels) {
		PlayedConfig.SortMode sortMode = PlayedConfig.getSortWorldList();
		if (sortMode == PlayedConfig.SortMode.DISABLED || levels.size() < 2) {
			return levels;
		}

		List<LevelSummary> sortedLevels = new ArrayList<>(levels);
		Comparator<LevelSummary> comparator = Comparator.comparingLong(this::played$getWorldMillis);
		if (sortMode == PlayedConfig.SortMode.UP_DOWN) {
			comparator = comparator.reversed();
		}

		sortedLevels.sort(comparator.thenComparing(Comparator.naturalOrder()));
		return sortedLevels;
	}

	private long played$getWorldMillis(LevelSummary levelSummary) {
		Minecraft minecraft = Minecraft.getInstance();
		return PlaytimeStorage.getWorldMillis(minecraft.getLevelSource().getLevelPath(levelSummary.getLevelId()));
	}
}
