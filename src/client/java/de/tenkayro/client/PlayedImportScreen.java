package de.tenkayro.client;

import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Objects;

public final class PlayedImportScreen extends Screen {
    private final @Nullable Screen parent;
    private EditBox path;
    private @NonNull String selected = Objects.requireNonNull(FabricLoader.getInstance().getGameDir().resolve("Played/import.json").toAbsolutePath().toString());
    private @NonNull Component status = Component.translatable("played.import.hint");

    public PlayedImportScreen(@Nullable Screen parent) { super(Component.translatable("played.import")); this.parent = parent; }

    @Override protected void init() {
        int w = Math.min(480, width - 32), x = (width - w) / 2;
        path = addRenderableWidget(new EditBox(font, x, height / 2 - 12, w, 20, Component.translatable("played.import.path")));
        path.setMaxLength(4096);
        path.setValue(selected);
        path.setResponder(value -> selected = Objects.requireNonNull(value));
        addRenderableWidget(Button.builder(Component.translatable("played.import.review"), b -> review()).bounds(x, height - 52, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.back"), b -> onClose()).bounds(x, height - 28, w, 20).build());
    }

    private void review() {
        try {
            PlaytimeTransfer.Backup backup = PlaytimeTransfer.read(Path.of(selected.trim()));
            long worlds = backup.entries().stream().filter(e -> e.kind().equals("world")).count();
            long servers = backup.entries().size() - worlds;
            minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    try {
                        PlaytimeTracker.flush();
                        PlaytimeTransfer.merge(backup);
                        status = Component.translatable("played.import.done");
                    } catch (RuntimeException ex) {
                        de.tenkayro.Played.LOGGER.warn("Could not merge Played backup", ex);
                        status = Component.translatable("played.import.failed");
                    }
                }
                minecraft.gui.setScreen(this);
            }, Component.translatable("played.import.confirm"), Component.translatable("played.import.summary", worlds, servers,
                backup.history().sessions.size(), de.tenkayro.client.config.PlayedConfig.formatTime(backup.total())),
                Component.translatable("played.import.merge"), Component.translatable("played.cancel")));
        } catch (Exception ex) {
            de.tenkayro.Played.LOGGER.warn("Could not import Played backup", ex);
            status = Component.translatable("played.import.failed");
        }
    }

    @Override public void onFilesDrop(@NonNull List<Path> files) {
        if (files.size() == 1) path.setValue(Objects.requireNonNull(files.getFirst().toAbsolutePath().toString()));
    }

    @Override public void extractRenderState(@NonNull GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, title, width / 2, 22, 0xFFF0DDFF);
        g.textWithWordWrap(font, status, 16, 48, width - 32, 0xFFD9CBE7);
        g.text(font, Component.translatable("played.import.path"), (width - Math.min(480, width - 32)) / 2, height / 2 - 25, 0xFFB9ADCB);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
