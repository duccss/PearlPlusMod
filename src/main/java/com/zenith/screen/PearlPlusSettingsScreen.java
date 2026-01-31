package com.zenith.screen;

import com.zenith.PearlPlusMod;
import com.zenith.config.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PearlPlusSettingsScreen extends Screen {
    private static final Component TITLE = Component.literal("PearlPlus Settings");
    private static final int FIELD_WIDTH = 240;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 98;
    private static final int SMALL_BUTTON_WIDTH = 60;

    private final Screen parent;
    private EditBox apiUrlField;
    private EditBox tokenField;

    public PearlPlusSettingsScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        var config = PearlPlusMod.config;
        config.ensureDefaultEndpoint();

        int centerX = this.width / 2;
        int startY = 44;

        this.apiUrlField = new EditBox(this.font, centerX - FIELD_WIDTH / 2, startY, FIELD_WIDTH, FIELD_HEIGHT,
            Component.literal("API URL"));
        this.apiUrlField.setMaxLength(256);
        this.addRenderableWidget(this.apiUrlField);

        this.tokenField = new EditBox(this.font, centerX - FIELD_WIDTH / 2, startY + 32, FIELD_WIDTH, FIELD_HEIGHT,
            Component.literal("Auth Token"));
        this.tokenField.setMaxLength(256);
        this.addRenderableWidget(this.tokenField);

        loadEndpointFields();

        int controlsY = startY + 64;
        int startX = centerX - (SMALL_BUTTON_WIDTH * 4 + 18) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Prev"), button -> switchEndpoint(-1))
            .width(SMALL_BUTTON_WIDTH)
            .pos(startX, controlsY)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Next"), button -> switchEndpoint(1))
            .width(SMALL_BUTTON_WIDTH)
            .pos(startX + SMALL_BUTTON_WIDTH + 6, controlsY)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Add"), button -> addEndpoint())
            .width(SMALL_BUTTON_WIDTH)
            .pos(startX + (SMALL_BUTTON_WIDTH + 6) * 2, controlsY)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Remove"), button -> removeEndpoint())
            .width(SMALL_BUTTON_WIDTH)
            .pos(startX + (SMALL_BUTTON_WIDTH + 6) * 3, controlsY)
            .build());

        int buttonY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).width(BUTTON_WIDTH).pos(centerX - BUTTON_WIDTH - 4, buttonY).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
            .width(BUTTON_WIDTH)
            .pos(centerX + 4, buttonY)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.literal("API URL"), this.width / 2 - FIELD_WIDTH / 2, 32, 0xA0A0A0, false);
        guiGraphics.drawString(this.font, Component.literal("Auth Token"), this.width / 2 - FIELD_WIDTH / 2, 64, 0xA0A0A0, false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void loadEndpointFields() {
        Config.PearlPlusEndpoint endpoint = currentEndpoint();
        this.apiUrlField.setValue(endpoint.apiUrl == null ? "" : endpoint.apiUrl);
        this.tokenField.setValue(endpoint.authToken == null ? "" : endpoint.authToken);
    }

    private void persistFieldsToCurrent() {
        Config.PearlPlusEndpoint endpoint = currentEndpoint();
        endpoint.apiUrl = this.apiUrlField.getValue().trim();
        endpoint.authToken = this.tokenField.getValue().trim();
    }

    private void switchEndpoint(int delta) {
        var config = PearlPlusMod.config;
        persistFieldsToCurrent();
        int nextIndex = config.activeEndpointIndex + delta;
        if (nextIndex < 0) {
            nextIndex = config.endpoints.size() - 1;
        } else if (nextIndex >= config.endpoints.size()) {
            nextIndex = 0;
        }
        config.activeEndpointIndex = nextIndex;
        loadEndpointFields();
    }

    private void addEndpoint() {
        var config = PearlPlusMod.config;
        persistFieldsToCurrent();
        Config.PearlPlusEndpoint endpoint = new Config.PearlPlusEndpoint();
        config.endpoints.add(endpoint);
        config.activeEndpointIndex = config.endpoints.size() - 1;
        loadEndpointFields();
    }

    private void removeEndpoint() {
        var config = PearlPlusMod.config;
        if (config.endpoints.size() <= 1) {
            return;
        }
        config.endpoints.remove(config.activeEndpointIndex);
        if (config.activeEndpointIndex >= config.endpoints.size()) {
            config.activeEndpointIndex = config.endpoints.size() - 1;
        }
        loadEndpointFields();
    }

    private void saveAndClose() {
        persistFieldsToCurrent();
        var config = PearlPlusMod.config;
        config.ensureDefaultEndpoint();
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private Config.PearlPlusEndpoint currentEndpoint() {
        var config = PearlPlusMod.config;
        config.ensureDefaultEndpoint();
        int index = Math.min(Math.max(config.activeEndpointIndex, 0), config.endpoints.size() - 1);
        return config.endpoints.get(index);
    }
}
