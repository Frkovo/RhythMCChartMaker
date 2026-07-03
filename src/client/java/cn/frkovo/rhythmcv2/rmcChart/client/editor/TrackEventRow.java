package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

class TrackEventRow {
    final TextFieldWidget startBeat;
    final TextFieldWidget endBeat;
    final TextFieldWidget startValue;
    final TextFieldWidget endValue;
    final ButtonWidget easingButton;
    EasingType easingType = EasingType.LINEAR;
    final ButtonWidget copyButton;
    final ButtonWidget removeButton;
    boolean visible;
    int y;
    int dragX;
    int rowRight;

    TrackEventRow(TextFieldWidget startBeat, TextFieldWidget endBeat, TextFieldWidget startValue, TextFieldWidget endValue, ButtonWidget easingButton, ButtonWidget copyButton, ButtonWidget removeButton) {
        this.startBeat = startBeat;
        this.endBeat = endBeat;
        this.startValue = startValue;
        this.endValue = endValue;
        this.easingButton = easingButton;
        this.copyButton = copyButton;
        this.removeButton = removeButton;
    }
}
