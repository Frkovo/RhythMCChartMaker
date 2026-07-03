package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class SerializedPreviewChart {
    public final String manifestJson;
    public final String levelJson;

    public SerializedPreviewChart(String manifestJson, String levelJson) {
        this.manifestJson = manifestJson;
        this.levelJson = levelJson;
    }

    public String manifestJson() { return manifestJson; }
    public String levelJson() { return levelJson; }
}
