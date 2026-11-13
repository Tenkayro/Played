package de.tenkayro.client;

import de.tenkayro.Played;
import de.tenkayro.client.config.PlayedConfig;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class PlayedHud {
	private static final int PADDING = 3;
	private static final int TEXT_HEIGHT = 9;
	private static long cachedCurrentSecond = -1L;
	private static long cachedSessionSecond = -1L;
	private static long cachedTotalSecond = -1L;
	private static boolean cachedPaused;
	private static String cachedTemplate;
	private static PlayedConfig.TimeFormatMode cachedTimeFormat;
	private static Component cachedText;
	private static net.minecraft.locale.Language cachedLanguage;
	private static boolean peeking;

	private PlayedHud() {
	}

	public static void setPeeking(boolean peek) { peeking = peek; }
	public static boolean isPeeking() { return peeking; }

	public static void register() {
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(Played.MOD_ID, "playtime"),
			PlayedHud::extractRenderState
		);
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(Played.MOD_ID, "playtime_action_bar"),
			PlayedHud::extractActionBar
		);
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(Played.MOD_ID, "playtime_scoreboard"),
			PlayedHud::extractScoreboard
		);
	}

	private static boolean canRender(Minecraft minecraft) {
		return minecraft.player != null
			&& minecraft.level != null
			&& minecraft.gui != null
			&& !minecraft.gui.hud.isHidden()
			&& minecraft.gui.screen() == null;
	}

	private static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if ((!PlayedConfig.shouldShowHudPlaytime() && !peeking) || !canRender(minecraft)) {
			return;
		}

		long millis = PlaytimeTracker.getActivePlaytimeMillis(minecraft);
		Component text = getText(millis);
		drawCorner(graphics, text, PlayedConfig.getHudPosition(), PlayedConfig.getHudHorizontalOffset(),
			PlayedConfig.getHudVerticalOffset(), PlayedConfig.getHudScalePercent(), PlayedConfig.shouldShowHudBackground());
	}

	public static HudBounds bounds(int screenWidth, int screenHeight, int textWidth,
		PlayedConfig.HudPosition position, int offsetX, int offsetY, int scalePercent, boolean background) {
		int padding = background ? PADDING : 0;
		float scale = scalePercent / 100.0F;
		float width = (textWidth + padding * 2) * scale;
		float height = (TEXT_HEIGHT + padding * 2) * scale;
		boolean right = position == PlayedConfig.HudPosition.TOP_RIGHT || position == PlayedConfig.HudPosition.BOTTOM_RIGHT;
		boolean bottom = position == PlayedConfig.HudPosition.BOTTOM_LEFT || position == PlayedConfig.HudPosition.BOTTOM_RIGHT;
		float x = right ? screenWidth - offsetX - width : offsetX;
		float y = bottom ? screenHeight - offsetY - height : offsetY;
		return new HudBounds(Math.max(0, Math.min(Math.max(0, screenWidth - width), x)),
			Math.max(0, Math.min(Math.max(0, screenHeight - height), y)), width, height);
	}

	public record HudBounds(float x, float y, float width, float height) {}

	public static HudBounds drawCorner(GuiGraphicsExtractor graphics, @NonNull Component text, PlayedConfig.HudPosition position,
		int offsetX, int offsetY, int scalePercent, boolean background) {
		Minecraft minecraft = Minecraft.getInstance();
		int textWidth = minecraft.font.width(text);
		HudBounds box = bounds(graphics.guiWidth(), graphics.guiHeight(), textWidth, position, offsetX, offsetY, scalePercent, background);
		float scale = scalePercent / 100.0F;
		int padding = background ? PADDING : 0;
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(box.x() + padding * scale, box.y() + padding * scale);
			graphics.pose().scale(scale, scale);
			if (background) graphics.fill(-PADDING, -PADDING, textWidth + PADDING, TEXT_HEIGHT + PADDING, 0x80000000);
			graphics.text(minecraft.font, text, 0, 0, 0xFFFFFFFF, true);
		} finally { graphics.pose().popMatrix(); }
		return box;
	}

	private static void extractActionBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!PlayedConfig.shouldShowActionBarPlaytime() || !canRender(minecraft)) {
			return;
		}

		@NonNull Component text = getDisplayText(minecraft);
		int width = minecraft.font.width(text);
		int x = (graphics.guiWidth() - width) / 2;
		int y = Math.max(0, graphics.guiHeight() - 68);
		graphics.text(minecraft.font, text, x, y, 0xFFFFFFFF, true);
	}

	private static void extractScoreboard(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!PlayedConfig.shouldShowScoreboardPlaytime() || !canRender(minecraft)) {
			return;
		}

		@NonNull Component title = Objects.requireNonNull(
			Component.translatable("played.name").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
		);
		@NonNull Component text = getDisplayText(minecraft);
		int width = Math.max(minecraft.font.width(title), minecraft.font.width(text));
		int x = Math.max(2, graphics.guiWidth() - width - 8);
		int y = Math.max(2, graphics.guiHeight() / 2 - 12);
		graphics.fill(x - 4, y - 3, graphics.guiWidth() - 2, y + 22, 0x80000000);
		graphics.text(minecraft.font, title, x, y, 0xFFFFFFFF, true);
		graphics.text(minecraft.font, text, x, y + 11, 0xFFFFFFFF, true);
	}

	public static @NonNull Component getDisplayText(Minecraft minecraft) {
		return getText(PlaytimeTracker.getActivePlaytimeMillis(minecraft));
	}

	private static @NonNull Component getText(long currentMillis) {
		long currentSecond = currentMillis / 1000L;
		long sessionSecond = PlaytimeTracker.getSessionPlaytimeMillis() / 1000L;
		long totalSecond = PlaytimeStorage.getAllTimeMillis() / 1000L;
		boolean paused = PlaytimeTracker.isPaused();
		String template = PlayedConfig.getHudTemplate();
		PlayedConfig.TimeFormatMode timeFormat = PlayedConfig.getTimeFormat();
		if (cachedLanguage != net.minecraft.locale.Language.getInstance()
			|| cachedText == null
			|| currentSecond != cachedCurrentSecond
			|| sessionSecond != cachedSessionSecond
			|| totalSecond != cachedTotalSecond
			|| paused != cachedPaused
			|| !Objects.equals(template, cachedTemplate)
			|| timeFormat != cachedTimeFormat) {
			cachedCurrentSecond = currentSecond;
			cachedSessionSecond = sessionSecond;
			cachedTotalSecond = totalSecond;
			cachedPaused = paused;
			cachedLanguage = net.minecraft.locale.Language.getInstance();
			cachedTemplate = template;
			cachedTimeFormat = timeFormat;
			cachedText = Objects.requireNonNull(PlayedConfig.formatHudPlaytime(currentMillis), "text");
		}

		return Objects.requireNonNull(cachedText, "text");
	}
}
