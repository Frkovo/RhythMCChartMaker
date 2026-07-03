package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import net.minecraft.client.gui.widget.TextFieldWidget;

final class FieldRow {
    final String label;
    final String hint;
    final TextFieldWidget widget;
    final boolean section;

    FieldRow(String label, String hint, TextFieldWidget widget, boolean section) {
        this.label = label;
        this.hint = hint;
        this.widget = widget;
        this.section = section;
    }
}
