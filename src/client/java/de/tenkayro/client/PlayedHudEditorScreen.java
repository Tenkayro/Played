package de.tenkayro.client;

import de.tenkayro.client.config.PlayedConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PlayedHudEditorScreen extends Screen {
    private final @Nullable Screen parent;
    private int scale = PlayedConfig.getHudScalePercent();
    private int offsetX = PlayedConfig.getHudHorizontalOffset();
    private int offsetY = PlayedConfig.getHudVerticalOffset();
    private PlayedConfig.HudPosition position = PlayedConfig.getHudPosition();
    private boolean enabled = PlayedConfig.shouldShowHudPlaytime();
    private boolean dragging;
    private double grabX, grabY;
    private PlayedHud.HudBounds box;

    public PlayedHudEditorScreen(@Nullable Screen parent) { super(Component.translatable("played.hud.edit")); this.parent = parent; }

    @Override protected void init() {
        int w = Math.min(300, width - 24), x = (width - w) / 2;
        addRenderableWidget(Button.builder(Component.literal("−"), b -> scale = Math.max(50, scale - 5)).bounds(x, height - 76, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> scale = Math.min(200, scale + 5)).bounds(x + 36, height - 76, 32, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.hud.reset"), b -> {
            scale = 100; offsetX = 5; offsetY = 5; position = PlayedConfig.HudPosition.TOP_LEFT;
        }).bounds(x + 72, height - 76, w - 72, 20).build());
        addRenderableWidget(Button.builder(toggleLabel(), b -> { enabled = !enabled; b.setMessage(toggleLabel()); }).bounds(x, height - 52, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.done"), b -> {
            PlayedConfig.setHudPlacement(position, offsetX, offsetY, scale);
            PlayedConfig.setShowHudPlaytime(enabled);
            onClose();
        }).bounds(x, height - 28, (w - 4) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.cancel"), b -> onClose()).bounds(x + (w + 4) / 2, height - 28, (w - 4) / 2, 20).build());
    }

    private @NonNull Component toggleLabel() { return Component.translatable(enabled ? "played.hud.enabled" : "played.hud.disabled"); }

    @Override public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, 18, 0xFFF0DDFF);
        g.centeredText(font, Component.translatable("played.hud.hint"), width / 2, 32, 0xFFB9ADCB);
        g.centeredText(font, Component.translatable("played.hud.scale", scale), width / 2, height - 90, 0xFFB9ADCB);
        box = PlayedHud.drawCorner(g, PlayedHud.getDisplayText(minecraft), position, offsetX, offsetY, scale, PlayedConfig.shouldShowHudBackground());
        g.outline((int) box.x() - 2, (int) box.y() - 2, (int) Math.ceil(box.width()) + 4, (int) Math.ceil(box.height()) + 4, 0xFFEAB7F4);
    }

    @Override public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && box != null && event.x() >= box.x() - 3 && event.x() <= box.x() + box.width() + 3
            && event.y() >= box.y() - 3 && event.y() <= box.y() + box.height() + 3) {
            dragging = true; grabX = event.x() - box.x(); grabY = event.y() - box.y();
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (!dragging || box == null) return super.mouseDragged(event, dx, dy);
        float x = (float) Math.max(0, Math.min(width - box.width(), event.x() - grabX));
        float y = (float) Math.max(0, Math.min(height - box.height(), event.y() - grabY));
        boolean right = x + box.width() / 2 > width / 2.0;
        boolean bottom = y + box.height() / 2 > height / 2.0;
        position = bottom ? (right ? PlayedConfig.HudPosition.BOTTOM_RIGHT : PlayedConfig.HudPosition.BOTTOM_LEFT)
            : (right ? PlayedConfig.HudPosition.TOP_RIGHT : PlayedConfig.HudPosition.TOP_LEFT);
        offsetX = Math.min(1000, Math.round(right ? width - x - box.width() : x));
        offsetY = Math.min(1000, Math.round(bottom ? height - y - box.height() : y));
        return true;
    }

    @Override public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        boolean handled = dragging; dragging = false;
        return super.mouseReleased(event) || handled;
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
