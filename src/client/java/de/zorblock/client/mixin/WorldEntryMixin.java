package de.zorblock.client.mixin;

import de.zorblock.client.ListPlaytimeLayout;
import de.zorblock.client.PlaytimeStorage;
import de.zorblock.client.config.PlayedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldEntryMixin extends WorldSelectionList.Entry {
	private long played$cachedMillis = -1L;
	private int played$cachedScreenWidth = -1;
	private boolean played$cachedHasScrollbar = false;
	private ListPlaytimeLayout.Placement played$cachedPlacement = null;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	LevelSummary summary;

	@Shadow
	@Final
	WorldSelectionList this$0;

	@Inject(method = "extractContent", at = @At("RETURN"))
	private void played$renderWorldPlaytime(
		GuiGraphicsExtractor context,
		int mouseX,
		int mouseY,
		boolean hovered,
		float tickDelta,
		CallbackInfo info
	) {
		if (!PlayedConfig.shouldShowWorldPlaytime()) {
			return;
		}

		long millis = PlaytimeStorage.getWorldMillis(this.minecraft.getLevelSource().getLevelPath(this.summary.getLevelId()));
		long seconds = millis / 1000L;
		int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
		boolean hasScrollbar = this.this$0.maxScrollAmount() > 0;

		if (this.played$cachedMillis != seconds || this.played$cachedScreenWidth != screenWidth || this.played$cachedHasScrollbar != hasScrollbar || this.played$cachedPlacement == null) {
			this.played$cachedMillis = seconds;
			this.played$cachedScreenWidth = screenWidth;
			this.played$cachedHasScrollbar = hasScrollbar;
			this.played$cachedPlacement = ListPlaytimeLayout.nextToScrollbar(
				this.minecraft.font,
				millis,
				this.this$0.getRowRight(),
				this.this$0.scrollbarWidth(),
				hasScrollbar,
				screenWidth
			);
		}

		ListPlaytimeLayout.Placement placement = this.played$cachedPlacement;

		int textWidth = this.minecraft.font.width(java.util.Objects.requireNonNull(placement.text()));
		int bgX1 = placement.x() - 4;
		int bgY1 = this.getContentY() - 1;
		int bgX2 = placement.x() + textWidth + 4;
		int bgY2 = this.getContentY() + 10;
		
		if (de.zorblock.client.config.PlayedConfig.shouldShowListBackground()) {
			context.fill(bgX1, bgY1, bgX2, bgY2, 0x60000000);
		}
		context.text(this.minecraft.font, java.util.Objects.requireNonNull(placement.text()), placement.x(), this.getContentY() + 1, 0xFFDDDDDD, true);
	}
}
