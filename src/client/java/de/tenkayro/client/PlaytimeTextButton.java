package de.tenkayro.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

/** Accessible hit target for the title screen's existing animated playtime text. */
public final class PlaytimeTextButton extends AbstractButton {
    private final Runnable action;

    public PlaytimeTextButton(Runnable action) {
        super(0, 0, 1, 9, Component.translatable("played.stats.title"));
        this.action = action;
        this.visible = false;
        this.active = false;
        setTooltip(Tooltip.create(Component.translatable("played.stats.title")));
    }

    @Override public void onPress(@NonNull InputWithModifiers input) { action.run(); }

    @Override protected void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // The title-screen mixin draws the text after resolving other mods' bottom-left text.
    }

    @Override protected void updateWidgetNarration(@NonNull NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
