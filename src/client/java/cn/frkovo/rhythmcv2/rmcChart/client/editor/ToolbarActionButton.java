package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;

import net.minecraft.client.gui.widget.ButtonWidget;

final class ToolbarActionButton {
    final ToolbarMenu menu;
    final ButtonWidget button;
    final boolean worldOnly;

    ToolbarActionButton(ToolbarMenu menu, ButtonWidget button, boolean worldOnly) {
        this.menu = menu;
        this.button = button;
        this.worldOnly = worldOnly;
    }

    ToolbarMenu menu() { return menu; }
    ButtonWidget button() { return button; }
    boolean worldOnly() { return worldOnly; }
}
