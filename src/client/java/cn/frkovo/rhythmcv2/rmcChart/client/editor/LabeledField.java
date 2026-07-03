package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import net.minecraft.client.gui.widget.TextFieldWidget;

class LabeledField {
    final String label;
    String hint = "";
    String section = "";
    int sectionY = -1;
    final TextFieldWidget widget;

    LabeledField(String label, TextFieldWidget widget) {
        this.label = label;
        this.widget = widget;
    }
}
