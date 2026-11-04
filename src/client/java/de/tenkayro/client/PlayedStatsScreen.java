package de.tenkayro.client;

import de.tenkayro.client.config.PlayedConfig;
import de.tenkayro.client.config.PlayedSettings;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Objects;

/** One hub for statistics, settings, HUD editing and portable local backups. */
public final class PlayedStatsScreen extends Screen {
    private final @Nullable Screen parent;
    private int tab;
    private int page;
    private @NonNull String query = "";
    private EditBox search;
    private Button previous;
    private Button next;
    private List<PlaytimeStorage.@NonNull Entry> entries;
    private @NonNull String status = "";

    public PlayedStatsScreen(@Nullable Screen parent) {
        super(Component.translatable("played.stats.title"));
        this.parent = parent;
    }

    private int left() { return (width - panelWidth()) / 2; }
    private int panelWidth() { return Math.min(width - 24, 560); }
    private int rows() { return Math.max(1, (height - 184) / 22); }

    @Override protected void init() {
        entries = PlaytimeStorage.entries();
        int x = left(), w = panelWidth(), gap = 4, bw = (w - gap * 3) / 4;
        String[] tabs = {"overview", "worlds", "servers", "sessions"};
        for (int i = 0; i < tabs.length; i++) {
            final int selected = i;
            Button button = addRenderableWidget(Button.builder(Component.translatable("played.tab." + tabs[i]), b -> {
                tab = selected; page = 0; rebuildWidgets();
            }).bounds(x + i * (bw + gap), 30, bw, 20).build());
            button.active = tab != i;
        }
        if (tab != 0) {
            search = addRenderableWidget(new EditBox(font, x, 56, w, 18, Component.translatable("played.search")));
            search.setMaxLength(128);
            search.setHint(Component.translatable("played.search"));
            search.setValue(query);
            search.setResponder(value -> { query = Objects.requireNonNull(value); page = 0; });
        }
        addRenderableWidget(Button.builder(Component.translatable("played.export"), b -> export()).bounds(x, height - 52, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.import"), b -> minecraft.gui.setScreen(new PlayedImportScreen(this))).bounds(x + bw + gap, height - 52, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.hud.edit"), b -> minecraft.gui.setScreen(new PlayedHudEditorScreen(this))).bounds(x + (bw + gap) * 2, height - 52, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("played.settings"), b -> minecraft.gui.setScreen(PlayedSettings.createScreen(this))).bounds(x + (bw + gap) * 3, height - 52, bw, 20).build());
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1)).bounds(x, height - 28, 36, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> page++).bounds(x + w - 36, height - 28, 36, 20).build());
        previous.visible = next.visible = tab != 0;
        addRenderableWidget(Button.builder(Component.translatable("played.done"), b -> onClose()).bounds(x + 40, height - 28, w - 80, 20).build());
    }

    private void export() {
        try {
            var file = PlaytimeTransfer.export(FabricLoader.getInstance().getGameDir());
            status = Component.translatable("played.export.done").getString();
            minecraft.keyboardHandler.setClipboard(Objects.requireNonNull(file.toAbsolutePath().toString()));
        } catch (Exception ex) {
            de.tenkayro.Played.LOGGER.warn("Could not export Played data", ex);
            status = Component.translatable("played.export.failed").getString();
        }
    }

    @Override public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, 12, 0xFFF0DDFF);
        if (tab == 0) overview(g, mouseX, mouseY); else list(g, mouseX, mouseY);
        @NonNull String foot = status.isEmpty() ? Component.translatable("played.history.note").getString() : status;
        g.centeredText(font, font.plainSubstrByWidth(foot, panelWidth()), width / 2, height - 67, 0xFFB9ADCB);
    }

    private void overview(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        LocalDate today = LocalDate.now();
        int x = left(), half = panelWidth() / 2;
        metric(g, "total", PlaytimeStorage.getAllTimeMillis(), x, 62);
        metric(g, "today", PlaytimeHistory.day(today), x + half, 62);
        metric(g, "week", PlaytimeHistory.week(today), x, 90);
        metric(g, "longest", PlaytimeHistory.longestSession(), x + half, 90);
        g.text(font, Component.translatable("played.last_week", PlayedConfig.formatTime(PlaytimeHistory.week(today.minusWeeks(1)))), x, 117, 0xFFB9ADCB);
        int top = 138, bottom = height - 89;
        if (bottom <= top) return;
        int step = panelWidth() / 7;
        long max = 1;
        for (int i = 0; i < 7; i++) max = Math.max(max, PlaytimeHistory.day(today.minusDays(6 - i)));
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            long value = PlaytimeHistory.day(day);
            int barX = x + i * step + 4;
            int barTop = bottom - (int) ((bottom - top) * ((double) value / max));
            g.fill(barX, top, barX + step - 8, bottom, 0x402B203C);
            if (value > 0) g.fillGradient(barX, barTop, barX + step - 8, bottom, 0xFFB36AF0, 0xFFE26BB2);
            @NonNull String label = Component.translatable("played.weekday." + day.getDayOfWeek().name().toLowerCase(Locale.ROOT)).getString();
            g.centeredText(font, label, barX + (step - 8) / 2, bottom + 3, 0xFFD9CBE7);
            if (mouseX >= barX && mouseX < barX + step - 8 && mouseY >= top && mouseY <= bottom + 12)
                g.setTooltipForNextFrame(Component.literal(day + " · " + PlayedConfig.formatTime(value)), mouseX, mouseY);
        }
    }

    private void metric(GuiGraphicsExtractor g, String key, long millis, int x, int y) {
        g.text(font, Component.translatable("played.metric." + key), x, y, 0xFFB9ADCB);
        g.text(font, PlayedConfig.formatTime(millis), x, y + 11, 0xFFFFFFFF);
    }

    private void list(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String filter = query.toLowerCase(Locale.ROOT);
        List<String[]> values;
        if (tab == 3) {
            DateTimeFormatter format = SessionDateFormat.create(Component.translatable("played.date.session_pattern").getString());
            values = PlaytimeHistory.snapshot().sessions.stream()
                .filter(s -> (s.name() + " " + s.target()).toLowerCase(Locale.ROOT).contains(filter))
                .map(s -> new String[] {s.name(), format.format(Instant.ofEpochMilli(s.start())) + " · " + s.target(), PlayedConfig.formatTime(s.millis())}).toList();
        } else {
            values = entries.stream().filter(e -> e.kind().equals(tab == 1 ? "world" : "server"))
                .filter(e -> (e.name() + " " + e.key()).toLowerCase(Locale.ROOT).contains(filter))
                .map(e -> new String[] {e.name(), e.key(), PlayedConfig.formatTime(e.millis())}).toList();
        }
        int pages = Math.max(1, (values.size() + rows() - 1) / rows());
        page = Math.min(page, pages - 1);
        previous.active = page > 0;
        next.active = page + 1 < pages;
        if (values.isEmpty()) g.centeredText(font, Component.translatable("played.empty"), width / 2, 95, 0xFFB9ADCB);
        for (int i = page * rows(); i < Math.min(values.size(), (page + 1) * rows()); i++) {
            int y = 81 + (i % rows()) * 22;
            String[] row = values.get(i);
            g.fill(left() - 3, y - 2, left() + panelWidth() + 3, y + 19, 0x80302140);
            int timeWidth = font.width(Objects.requireNonNull(row[2]));
            g.text(font, font.plainSubstrByWidth(Objects.requireNonNull(row[0]), Math.max(20, panelWidth() - timeWidth - 12)), left(), y, 0xFFFFFFFF);
            g.text(font, Objects.requireNonNull(row[2]), left() + panelWidth() - timeWidth, y, 0xFFEAB7F4);
            g.text(font, font.plainSubstrByWidth(Objects.requireNonNull(row[1]), panelWidth()), left(), y + 10, 0xFFB9ADCB);
            if (mouseX >= left() && mouseX < left() + panelWidth() && mouseY >= y && mouseY < y + 20)
                g.setTooltipForNextFrame(List.of(
                    Component.literal(Objects.requireNonNull(row[0])).getVisualOrderText(),
                    Component.literal(Objects.requireNonNull(row[1])).getVisualOrderText()
                ), mouseX, mouseY);
        }
        g.centeredText(font, Component.translatable("played.page", page + 1, pages), width / 2, height - 83, 0xFFB9ADCB);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
