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
        int centerY = this.height / 2;
        int buttonWidth = Math.min(300, this.width - 40);
        int buttonX = centerX - buttonWidth / 2;

        this.addRenderableWidget(Button.builder(
                playerListLabel(),
                button -> {
                    ModuleState.visiblePlayers = !ModuleState.visiblePlayers;
                    button.setMessage(playerListLabel());
                }
        ).bounds(buttonX, centerY - 28, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                moduleListLabel(),
                button -> {
                    ModuleState.moduleList = !ModuleState.moduleList;
                    button.setMessage(moduleListLabel());
                }
        ).bounds(buttonX, centerY, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose()
        ).bounds(buttonX, centerY + 44, buttonWidth, 20).build());
    }

    private static Component playerListLabel() {
        return Component.literal("Visible Player List: " + (ModuleState.visiblePlayers ? "ON" : "OFF"));
    }

    private static Component moduleListLabel() {
        return Component.literal("Module List HUD: " + (ModuleState.moduleList ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "SHADOW CLIENT 26.2";
        graphics.text(this.font, title,
                this.width / 2 - this.font.width(title) / 2,
                this.height / 2 - 66, 0xFF55CCFF, true);

        String hint = "Right Shift to reopen";
        graphics.text(this.font, hint,
                this.width / 2 - this.font.width(hint) / 2,
                this.height / 2 + 78, 0xFFBBBBBB, false);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
