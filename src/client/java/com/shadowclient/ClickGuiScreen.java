package com.shadowclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    public ClickGuiScreen() {
        super(Component.literal("Shadow Client"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 64;

        this.addRenderableWidget(Button.builder(
                playerListLabel(),
                button -> {
                    ModuleState.visiblePlayers = !ModuleState.visiblePlayers;
                    button.setMessage(playerListLabel());
                }
        ).bounds(centerX - 105, top, 210, 20).build());

        this.addRenderableWidget(Button.builder(
                moduleListLabel(),
                button -> {
                    ModuleState.moduleList = !ModuleState.moduleList;
                    button.setMessage(moduleListLabel());
                }
        ).bounds(centerX - 105, top + 27, 210, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose()
        ).bounds(centerX - 105, top + 82, 210, 20).build());
    }

    private static Component playerListLabel() {
        return Component.literal("Visible Player List: " + (ModuleState.visiblePlayers ? "ON" : "OFF"));
    }

    private static Component moduleListLabel() {
        return Component.literal("Module List HUD: " + (ModuleState.moduleList ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        graphics.text(this.font, "SHADOW CLIENT 26.2", this.width / 2 - 67, this.height / 2 - 94, 0xFF55CCFF, true);
        graphics.text(this.font, "CURRENT MODULES", this.width / 2 - 47, this.height / 2 - 24, 0xFF55CCFF, true);
        graphics.text(this.font, "Visible Player List (line of sight only)", this.width / 2 - 104, this.height / 2 + 9, 0xFFFFFFFF, false);
        graphics.text(this.font, "Module List HUD", this.width / 2 - 104, this.height / 2 + 22, 0xFFFFFFFF, false);
        graphics.text(this.font, "Right Shift to reopen", this.width / 2 - 53, this.height / 2 + 54, 0xFFBBBBBB, false);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
