package com.zenith.screen;

import com.zenith.PearlPlusMod;
import com.zenith.config.Config;
import com.zenith.pearlplus.PearlPlusApi;
import com.zenith.pearlplus.PearlPlusApiException;
import com.zenith.pearlplus.model.PearlLoadResponse;
import com.zenith.pearlplus.model.PearlStatusResponse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PearlPlusScreen extends Screen {
    private static final Component TITLE = Component.literal("PearlPlus");
    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private PearlList pearlList;
    private Button refreshButton;
    private Component statusMessage = Component.literal("");
    private boolean loading;

    public PearlPlusScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listTop = 32;
        int listBottom = this.height - 64;
        this.pearlList = new PearlList(this.minecraft, this.width, listBottom, listTop, 24);
        this.addRenderableWidget(this.pearlList);

        int buttonY = this.height - 28;
        int totalWidth = BUTTON_WIDTH * 3 + 8;
        int startX = this.width / 2 - totalWidth / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).width(BUTTON_WIDTH).pos(startX, buttonY).build());

        this.addRenderableWidget(Button.builder(Component.literal("Settings"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PearlPlusSettingsScreen(this));
            }
        }).width(BUTTON_WIDTH).pos(startX + BUTTON_WIDTH + 4, buttonY).build());

        this.refreshButton = this.addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> requestPearls())
            .width(BUTTON_WIDTH)
            .pos(startX + (BUTTON_WIDTH + 4) * 2, buttonY)
            .build());

        requestPearls();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (this.statusMessage != null && !this.statusMessage.getString().isBlank()) {
            guiGraphics.drawCenteredString(this.font, this.statusMessage, this.width / 2, this.height - 44, 0xA0A0A0);
        }
    }

    private void requestPearls() {
        String playerName = PearlPlusMod.getPlayerName();
        if (playerName.isBlank()) {
            setStatus(false, Component.literal("Player name unavailable."));
            updatePearls(List.of());
            return;
        }
        setStatus(true, Component.literal("Loading pearls..."));
        var config = PearlPlusMod.config;
        config.ensureDefaultEndpoint();
        List<Config.PearlPlusEndpoint> endpoints = new ArrayList<>(config.endpoints);
        List<CompletableFuture<PearlFetchResult>> futures = new ArrayList<>();
        for (var endpoint : endpoints) {
            futures.add(CompletableFuture.supplyAsync(() -> fetchPearlsForEndpoint(playerName, endpoint)));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((ignored, throwable) -> Minecraft.getInstance().execute(() -> {
                List<PearlFetchResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                updateFromResults(results);
            }));
    }

    private PearlFetchResult fetchPearlsForEndpoint(String playerName, Config.PearlPlusEndpoint endpoint) {
        try {
            PearlStatusResponse response = PearlPlusApi.INSTANCE.fetchPearls(playerName, endpoint.apiUrl, endpoint.authToken);
            List<String> pearls = response.pearls() == null ? List.of() : response.pearls();
            return new PearlFetchResult(endpoint, pearls, null, response.botName(), response.serverIp(), response.botConnected());
        } catch (Exception e) {
            String message = e instanceof PearlPlusApiException
                ? e.getMessage()
                : e.getMessage();
            return new PearlFetchResult(endpoint, List.of(), message, null, null, null);
        }
    }

    private void updateFromResults(List<PearlFetchResult> results) {
        int pearlCount = results.stream().mapToInt(result -> result.pearls().size()).sum();
        long errorCount = results.stream().filter(result -> result.error() != null).count();
        if (pearlCount == 0 && errorCount == 0) {
            setStatus(false, Component.literal("No pearls found."));
        } else if (errorCount > 0) {
            setStatus(false, Component.literal("Loaded " + pearlCount + " pearls, " + errorCount + " errors."));
        } else {
            setStatus(false, Component.literal("Loaded " + pearlCount + " pearls."));
        }
        updatePearls(buildEntries(results));
    }

    private List<PearlListEntry> buildEntries(List<PearlFetchResult> results) {
        List<PearlListEntry> entries = new ArrayList<>();
        for (PearlFetchResult result : results) {
            String displayName = resolveEndpointName(result);
            entries.add(new HeaderEntry(displayName, result.error()));
            List<String> sorted = new ArrayList<>(result.pearls());
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            for (String pearlId : sorted) {
                entries.add(new PearlEntry(result.endpoint(), pearlId, displayName, result.botConnected()));
            }
        }
        if (entries.stream().noneMatch(entry -> entry instanceof PearlEntry)) {
            entries.add(new EmptyEntry(Component.literal("No pearls available.")));
        }
        return entries;
    }

    private void updatePearls(List<PearlListEntry> entries) {
        this.pearlList.setEntries(entries);
    }

    private void setStatus(boolean loading, Component message) {
        this.loading = loading;
        this.statusMessage = message;
        if (this.refreshButton != null) {
            this.refreshButton.active = !loading;
        }
        if (this.pearlList != null) {
            this.pearlList.setButtonsActive(!loading);
        }
    }

    private void requestLoad(Config.PearlPlusEndpoint endpoint, String pearlId, String displayName, boolean botConnected) {
        if (!botConnected) {
            setStatus(false, Component.literal("Cannot load pearl. Bot is not connected."));
            return;
        }
        String playerName = PearlPlusMod.getPlayerName();
        if (playerName.isBlank()) {
            setStatus(false, Component.literal("Player name unavailable."));
            return;
        }
        setStatus(true, Component.literal("Loading pearl " + pearlId + " from " + displayName + "..."));
        CompletableFuture.supplyAsync(() -> {
            try {
                return PearlPlusApi.INSTANCE.loadPearl(playerName, pearlId, endpoint.apiUrl, endpoint.authToken);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((response, throwable) -> {
            Minecraft.getInstance().execute(() -> {
                if (throwable != null) {
                    var cause = throwable.getCause();
                    String message = cause instanceof PearlPlusApiException
                        ? cause.getMessage()
                        : throwable.getMessage();
                    setStatus(false, Component.literal("Failed to load pearl: " + message));
                    return;
                }
                updateFromLoad(response, pearlId, displayName);
            });
        });
    }

    private void updateFromLoad(PearlLoadResponse response, String pearlId, String endpointName) {
        String status = response.status() == null ? "Queued" : response.status();
        setStatus(false, Component.literal("Pearl " + pearlId + " " + status + " (" + endpointName + ")."));
    }

    private record PearlFetchResult(
        Config.PearlPlusEndpoint endpoint,
        List<String> pearls,
        String error,
        String botName,
        String serverIp,
        Boolean botConnected
    ) {
    }

    private class PearlList extends ObjectSelectionList<PearlListEntry> {
        private static final int LIST_WIDTH = 300;

        private PearlList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
        }

        private void setEntries(List<PearlListEntry> entries) {
            this.clearEntries();
            entries.forEach(this::addEntry);
        }

        private void setButtonsActive(boolean active) {
            for (PearlListEntry entry : this.children()) {
                entry.setButtonActive(active);
            }
        }

        @Override
        public int getRowWidth() {
            return LIST_WIDTH;
        }

        @Override
        public int getRowLeft() {
            return (PearlPlusScreen.this.width - LIST_WIDTH) / 2;
        }
    }

    private abstract class PearlListEntry extends ObjectSelectionList.Entry<PearlListEntry> {
        public void setButtonActive(boolean active) {
        }
    }

    private class HeaderEntry extends PearlListEntry {
        private final String name;
        private final String error;

        private HeaderEntry(String name, String error) {
            this.name = name == null || name.isBlank() ? "Endpoint" : name;
            this.error = error;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            guiGraphics.drawString(PearlPlusScreen.this.font, this.name, x + 4, y + 4, 0xFFE69A, false);
            if (this.error != null && !this.error.isBlank()) {
                guiGraphics.drawString(PearlPlusScreen.this.font, this.error, x + 4, y + 14, 0xFF7777, false);
            }
        }

        @Override
        public Component getNarration() {
            return Component.literal("Endpoint " + this.name);
        }
    }

    private class EmptyEntry extends PearlListEntry {
        private final Component message;

        private EmptyEntry(Component message) {
            this.message = message;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            guiGraphics.drawString(PearlPlusScreen.this.font, this.message, x + 6, y + 6, 0xA0A0A0, false);
        }

        @Override
        public Component getNarration() {
            return this.message;
        }
    }

    private class PearlEntry extends PearlListEntry {
        private static final int LOAD_BUTTON_WIDTH = 60;
        private final Config.PearlPlusEndpoint endpoint;
        private final String pearlId;
        private final String displayName;
        private final boolean botConnected;
        private final Button loadButton;

        private PearlEntry(Config.PearlPlusEndpoint endpoint, String pearlId, String displayName, Boolean botConnected) {
            this.endpoint = endpoint;
            this.pearlId = pearlId;
            this.displayName = displayName;
            this.botConnected = Boolean.TRUE.equals(botConnected);
            this.loadButton = Button.builder(Component.literal("Load"),
                    button -> requestLoad(this.endpoint, this.pearlId, this.displayName, this.botConnected))
                .width(LOAD_BUTTON_WIDTH)
                .build();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            guiGraphics.drawString(PearlPlusScreen.this.font, this.pearlId, x + 6, y + 6, 0xFFFFFF, false);
            int buttonX = x + entryWidth - LOAD_BUTTON_WIDTH - 6;
            this.loadButton.setX(buttonX);
            this.loadButton.setY(y);
            this.loadButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.loadButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.loadButton.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal("Pearl " + this.pearlId + " from " + this.displayName);
        }

        @Override
        public void setButtonActive(boolean active) {
            this.loadButton.active = active && this.botConnected;
        }
    }

    private String resolveEndpointName(PearlFetchResult result) {
        String botName = result.botName();
        String serverIp = result.serverIp();
        String resolvedName = botName == null || botName.isBlank() ? "Unknown Bot" : botName;
        String resolvedServer = serverIp == null || serverIp.isBlank() ? "Unknown Server" : serverIp;
        return resolvedName + " - " + resolvedServer;
    }
}
