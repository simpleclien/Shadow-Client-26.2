package com.shadowclient;

import java.util.function.Supplier;
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
        int buttonWidth = Math.min(340, this.width - 32);
        int buttonX = centerX - buttonWidth / 2;
        int y = centerY - 104;

        addToggle(buttonX, y, buttonWidth, () -> label("Visible Player List", ModuleState.visiblePlayers),
                () -> ModuleState.visiblePlayers = !ModuleState.visiblePlayers);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Green Player Boxes", ModuleState.playerBoxes),
                () -> ModuleState.playerBoxes = !ModuleState.playerBoxes);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Module List HUD", ModuleState.moduleList),
                () -> ModuleState.moduleList = !ModuleState.moduleList);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Storage Finder", ModuleState.storageFinder),
                () -> ModuleState.storageFinder = !ModuleState.storageFinder);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Spawner Finder", ModuleState.spawnerFinder),
                () -> ModuleState.spawnerFinder = !ModuleState.spawnerFinder);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Coordinates HUD", ModuleState.coordinates),
                () -> ModuleState.coordinates = !ModuleState.coordinates);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("FPS Counter", ModuleState.fps),
                () -> ModuleState.fps = !ModuleState.fps);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Armor Status", ModuleState.armorStatus),
                () -> ModuleState.armorStatus = !ModuleState.armorStatus);
        y += 24;
        addToggle(buttonX, y, buttonWidth, () -> label("Keystrokes HUD", ModuleState.keystrokes),
                () -> ModuleState.keystrokes = !ModuleState.keystrokes);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(buttonX, centerY + 112, buttonWidth, 20).build());
    }

    private void addToggle(int x, int y, int width, Supplier<Component> label, Runnable toggle) {
        this.addRenderableWidget(Button.builder(label.get(), button -> {
            toggle.run();
            button.setMessage(label.get());
        }).bounds(x, y, width, 20).build());
    }

    private static Component label(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "SHADOW CLIENT 26.2";
        graphics.text(this.font, title, this.width / 2 - this.font.width(title) / 2,
                this.height / 2 - 132, 0xFF55CCFF, true);

        String hint = "Right Shift to reopen";
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2,
                this.height / 2 + 128, 0xFFBBBBBB, false);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
}
