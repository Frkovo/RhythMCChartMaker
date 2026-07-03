package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;

public final class SelectedEffect {
    public final EffectData effect;

    public SelectedEffect(EffectData effect) {
        this.effect = effect;
    }

    public EffectData effect() { return effect; }
}
