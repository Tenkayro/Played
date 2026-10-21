package de.tenkayro.client;

import de.tenkayro.client.config.PlayedConfig;
import java.util.Objects;
import net.minecraft.client.gui.Font;

public final class ListPlaytimeLayout {
	private static final int SCREEN_PADDING = 4;
	private static final int ENTRY_GAP = 4;
	private static final int SCROLLBAR_GAP = 8;

	private ListPlaytimeLayout() {
	}

	public static Placement nextToScrollbar(Font font, long millis, int rowRight, int scrollbarWidth, boolean hasScrollbar, int screenWidth) {
		int textX = rowRight + (hasScrollbar ? scrollbarWidth * 2 + SCROLLBAR_GAP : ENTRY_GAP);
		int right = screenWidth - SCREEN_PADDING;
		int availableWidth = Math.max(0, right - textX);

		for (net.minecraft.network.chat.Component text : candidates(millis)) {
			net.minecraft.network.chat.Component candidate = Objects.requireNonNull(text, "text");
			int width = font.width(candidate);
			if (width <= availableWidth) {
				return new Placement(candidate, textX);
			}
		}

		net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal(Objects.requireNonNull(shortestCandidate(millis)));
		net.minecraft.network.chat.Component candidate = Objects.requireNonNull(text, "text");
		int fallbackX = Math.max(SCREEN_PADDING, right - font.width(candidate));
		return new Placement(candidate, fallbackX);
	}

	private static net.minecraft.network.chat.Component[] candidates(long millis) {
		String formatted = PlayedConfig.formatTime(millis);
		String compact = compact(millis, false);
		String shortest = compact(millis, true);

		return new net.minecraft.network.chat.Component[] {
			Objects.requireNonNull(PlayedConfig.formatListPlaytime(formatted), "formatted"),
			Objects.requireNonNull(PlayedConfig.formatListPlaytime(compact), "compact"),
			Objects.requireNonNull(PlayedConfig.formatListPlaytime(shortest), "shortest"),
			net.minecraft.network.chat.Component.literal(Objects.requireNonNull(compact)),
			net.minecraft.network.chat.Component.literal(Objects.requireNonNull(shortest))
		};
	}

	private static String shortestCandidate(long millis) {
		return compact(millis, true);
	}

	private static String compact(long millis, boolean singleUnit) {
		long seconds = Math.max(0L, millis / 1000L);
		long minutes = seconds / 60L;
		long hours = minutes / 60L;
		long days = hours / 24L;

		seconds %= 60L;
		minutes %= 60L;
		hours %= 24L;

		if (days > 0L) {
			return singleUnit ? days + "d" : days + "d" + hours + "h";
		}

		if (hours > 0L) {
			return singleUnit ? hours + "h" : hours + "h" + minutes + "m";
		}

		if (minutes > 0L) {
			return singleUnit ? minutes + "m" : minutes + "m" + seconds + "s";
		}

		return seconds + "s";
	}

	public record Placement(net.minecraft.network.chat.Component text, int x) {
	}
}
