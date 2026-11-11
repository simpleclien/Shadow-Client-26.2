package com.shadowclient;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    private static final int ACCENT = 0xFF26FF33;
    private static final int PANEL = 0xF0181D22;
    private static final int MUTED = 0xFF9AA4AE;

    public ClickGuiScreen() {
        super(Component.literal("Shadow Client"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = Math.min(460, this.width - 24);
        int panelLeft = centerX - panelWidth / 2;
        int buttonGap = 8;
        int buttonWidth = (panelWidth - 48) / 2;
        int left = panelLeft + 16;
        int right = left + buttonWidth + buttonGap;
        int top = centerY - 146;

        addToggle(left, top + 78, buttonWidth, "Visible Player List",
                () -> ModuleState.visiblePlayers, () -> ModuleState.visiblePlayers = !ModuleState.visiblePlayers);
        addToggle(right, top + 78, buttonWidth, "Green Player Boxes",
                () -> ModuleState.playerBoxes, () -> ModuleState.playerBoxes = !ModuleState.playerBoxes);
        addToggle(left, top + 105, buttonWidth, "Storage Finder",
                () -> ModuleState.storageFinder, () -> ModuleState.storageFinder = !ModuleState.storageFinder);
        addToggle(right, top + 105, buttonWidth, "Spawner Finder",
                () -> ModuleState.spawnerFinder, () -> ModuleState.spawnerFinder = !ModuleState.spawnerFinder);

        addToggle(left, top + 159, buttonWidth, "Module List HUD",
                () -> ModuleState.moduleList, () -> ModuleState.moduleList = !ModuleState.moduleList);
        addToggle(right, top + 159, buttonWidth, "Coordinates HUD",
                () -> ModuleState.coordinates, () -> ModuleState.coordinates = !ModuleState.coordinates);
        addToggle(left, top + 186, buttonWidth, "FPS Counter",
                () -> ModuleState.fps, () -> ModuleState.fps = !ModuleState.fps);
        addToggle(right, top + 186, buttonWidth, "Armor Status",
                () -> ModuleState.armorStatus, () -> ModuleState.armorStatus = !ModuleState.armorStatus);
        addToggle(left, top + 213, buttonWidth, "Keystrokes HUD",
                () -> ModuleState.keystrokes, () -> ModuleState.keystrokes = !ModuleState.keystrokes);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(centerX - 62, top + 250, 124, 20).build());
    }

    private void addToggle(int x, int y, int width, String name,
                           Supplier<Boolean> enabled, Runnable toggle) {
        this.addRenderableWidget(Button.builder(label(name, enabled.get()), button -> {
            toggle.run();
            button.setMessage(label(name, enabled.get()));
        }).bounds(x, y, width, 22).build());
    }

    private static Component label(String name, boolean enabled) {
        return Component.literal(name + (enabled ? "  [ON]" : "  [OFF]"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = Math.min(460, this.width - 24);
        int left = centerX - panelWidth / 2;
        int top = centerY - 146;
        graphics.fill(left, top, left + panelWidth, top + 292, PANEL);
        graphics.fill(left, top, left + panelWidth, top + 3, ACCENT);
        graphics.fill(left + 16, top + 45, left + panelWidth - 16, top + 46, 0xFF39434D);

        String title = "SHADOW CLIENT";
        graphics.text(this.font, title, centerX - this.font.width(title) / 2,
                top + 15, 0xFFFFFFFF, true);
        String version = "26.2  •  CLIENT MOD MENU";
        graphics.text(this.font, version, centerX - this.font.width(version) / 2,
                top + 30, MUTED, false);

        graphics.text(this.font, "PLAYER & WORLD", left + 16, top + 59, ACCENT, true);
        graphics.text(this.font, "HUD", left + 16, top + 140, ACCENT, true);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String hint = "Right Shift to reopen";
        graphics.text(this.font, hint, centerX - this.font.width(hint) / 2,
                top + 276, MUTED, false);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
