package cn.frkovo.rhythmcv2.rmcChart.client.wizard;

public record ImportedSongMetadata(
        String title,
        String composer,
        String description,
        int lengthMillis
) {
    public static ImportedSongMetadata empty() {
        return new ImportedSongMetadata("", "", "", 0);
    }
}
