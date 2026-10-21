package de.zorblock.client;

import de.zorblock.Played;
import de.zorblock.client.config.PlayedConfig;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PlayedHud {
	private static final int PADDING = 3;
	private static final int TEXT_HEIGHT = 9;
	private static long cachedCurrentSecond = -1L;
	private static long cachedSessionSecond = -1L;
	private static long cachedTotalSecond = -1L;
	private static String cachedTemplate;
	private static PlayedConfig.TimeFormatMode cachedTimeFormat;
	private static Component cachedText;

	private PlayedHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(Played.MOD_ID, "playtime"),
			PlayedHud::extractRenderState
		);
	}

	private static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!PlayedConfig.shouldShowHudPlaytime()
			|| minecraft.player == null
			|| minecraft.level == null
			|| minecraft.gui.screen() != null) {
			return;
		}

		long millis = PlaytimeTracker.getActivePlaytimeMillis(minecraft);
		Component text = getText(millis);
		int textWidth = minecraft.font.width(Objects.requireNonNull(text, "text"));
		boolean right = PlayedConfig.getHudPosition() == PlayedConfig.HudPosition.TOP_RIGHT
			|| PlayedConfig.getHudPosition() == PlayedConfig.HudPosition.BOTTOM_RIGHT;
		boolean bottom = PlayedConfig.getHudPosition() == PlayedConfig.HudPosition.BOTTOM_LEFT
			|| PlayedConfig.getHudPosition() == PlayedConfig.HudPosition.BOTTOM_RIGHT;
		boolean background = PlayedConfig.shouldShowHudBackground();
		int padding = background ? PADDING : 0;
		float scale = PlayedConfig.getHudScalePercent() / 100.0F;
		float scaledWidth = (textWidth + padding * 2) * scale;
		float scaledHeight = (TEXT_HEIGHT + padding * 2) * scale;
		float maxOuterX = Math.max(0.0F, graphics.guiWidth() - scaledWidth);
		float maxOuterY = Math.max(0.0F, graphics.guiHeight() - scaledHeight);
		float outerX = right
			? graphics.guiWidth() - PlayedConfig.getHudHorizontalOffset() - scaledWidth
			: PlayedConfig.getHudHorizontalOffset();
		float outerY = bottom
			? graphics.guiHeight() - PlayedConfig.getHudVerticalOffset() - scaledHeight
			: PlayedConfig.getHudVerticalOffset();
		outerX = Math.max(0.0F, Math.min(maxOuterX, outerX));
		outerY = Math.max(0.0F, Math.min(maxOuterY, outerY));
		float textX = outerX + padding * scale;
		float textY = outerY + padding * scale;

		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(textX, textY);
			graphics.pose().scale(scale, scale);
			if (background) {
				graphics.fill(-PADDING, -PADDING, textWidth + PADDING, TEXT_HEIGHT + PADDING, 0x80000000);
			}

			graphics.text(minecraft.font, Objects.requireNonNull(text, "text"), 0, 0, 0xFFFFFFFF, true);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private static Component getText(long currentMillis) {
		long currentSecond = currentMillis / 1000L;
		long sessionSecond = PlaytimeTracker.getSessionPlaytimeMillis() / 1000L;
		long totalSecond = PlaytimeStorage.getAllTimeMillis() / 1000L;
		String template = PlayedConfig.getHudTemplate();
		PlayedConfig.TimeFormatMode timeFormat = PlayedConfig.getTimeFormat();
		if (cachedText == null
			|| currentSecond != cachedCurrentSecond
			|| sessionSecond != cachedSessionSecond
			|| totalSecond != cachedTotalSecond
			|| !Objects.equals(template, cachedTemplate)
			|| timeFormat != cachedTimeFormat) {
			cachedCurrentSecond = currentSecond;
			cachedSessionSecond = sessionSecond;
			cachedTotalSecond = totalSecond;
			cachedTemplate = template;
			cachedTimeFormat = timeFormat;
			cachedText = Objects.requireNonNull(PlayedConfig.formatHudPlaytime(currentMillis), "text");
		}

		return cachedText;
	}
}
