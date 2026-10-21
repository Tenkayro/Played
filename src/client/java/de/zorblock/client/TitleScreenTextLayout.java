package de.zorblock.client;

import java.util.ArrayList;
import java.util.List;

public final class TitleScreenTextLayout {
	private static final int LEFT_EDGE_MAX_X = 8;
	private static final int BOTTOM_AREA_HEIGHT = 96;
	private static final int LINE_HEIGHT = 11;
	private static final int TEXT_HEIGHT = 9;
	private static final int BOTTOM_PADDING = 2;
	private static final List<Integer> OCCUPIED_ROWS = new ArrayList<>();

	private static boolean active;
	private static int screenHeight;

	private TitleScreenTextLayout() {
	}

	public static void beginFrame(int height) {
		screenHeight = height;
		OCCUPIED_ROWS.clear();
		active = true;
	}

	public static void endFrame() {
		active = false;
	}

	public static void recordText(int x, int y, int color) {
		if (!active || x > LEFT_EDGE_MAX_X || y < screenHeight - BOTTOM_AREA_HEIGHT || ((color >>> 24) & 0xFF) == 0) {
			return;
		}

		OCCUPIED_ROWS.add(y);
	}

	public static int nextBottomLeftY() {
		int y = screenHeight - TEXT_HEIGHT - BOTTOM_PADDING;
		while (isOccupied(y)) {
			y -= LINE_HEIGHT;
		}

		return Math.max(2, y);
	}

	private static boolean isOccupied(int y) {
		for (int occupiedY : OCCUPIED_ROWS) {
			if (Math.abs(occupiedY - y) < TEXT_HEIGHT) {
				return true;
			}
		}

		return false;
	}
}
