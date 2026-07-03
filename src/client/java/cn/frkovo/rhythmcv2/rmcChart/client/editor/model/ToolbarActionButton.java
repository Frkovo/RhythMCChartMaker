package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import net.minecraft.client.gui.widget.ButtonWidget;

public final class ToolbarActionButton {
    public final ToolbarMenu menu;
    public final ButtonWidget button;
    public final boolean worldOnly;

    public ToolbarActionButton(ToolbarMenu menu, ButtonWidget button, boolean worldOnly) {
        this.menu = menu;
        this.button = button;
        this.worldOnly = worldOnly;
    }

    public ToolbarMenu menu() { return menu; }
    public ButtonWidget button() { return button; }
    public boolean worldOnly() { return worldOnly; }
}
