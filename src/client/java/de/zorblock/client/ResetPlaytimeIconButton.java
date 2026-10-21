package de.zorblock.client;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class ResetPlaytimeIconButton extends AbstractButton {
	public static final int SIZE = 20;
	private static final int MARGIN = 4;
	private static final Component MESSAGE = Component.literal("⏱");
	private static final Component TOOLTIP = Component.literal("Reset Playtime");
	private static final int RED = 0xFFFF4444;
	private static final int RED_HOVERED = 0xFFFF6666;
	private final Runnable onPress;

	public ResetPlaytimeIconButton(int x, int y, Runnable onPress) {
		super(x, y, SIZE, SIZE, Objects.requireNonNull(MESSAGE, "message"));
		this.onPress = Objects.requireNonNull(onPress, "onPress");
		this.setTooltip(Tooltip.create(Objects.requireNonNull(TOOLTIP, "tooltip")));
	}

	@Override
	public void onPress(@NonNull InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void extractContents(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		this.anchorTo(context.guiWidth(), context.guiHeight());
		this.extractDefaultSprite(context);
		int color = this.isHoveredOrFocused() ? RED_HOVERED : RED;
		context.centeredText(Minecraft.getInstance().font, Objects.requireNonNull(MESSAGE, "message"), this.getX() + this.width / 2, this.getY() + 6, color);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		this.anchorTo(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
		return super.isMouseOver(mouseX, mouseY);
	}

	@Override
	protected void updateWidgetNarration(@NonNull NarrationElementOutput narration) {
		this.defaultButtonNarrationText(narration);
	}

	private void anchorTo(int screenWidth, int screenHeight) {
		this.setX(screenWidth - SIZE - MARGIN);
		this.setY(screenHeight - SIZE - MARGIN);
	}
}
