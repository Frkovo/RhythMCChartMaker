package cn.frkovo.rhythmcv2.rmcChart.client.net;

final class ChunkUpload {
    final String chunkId;
    final int totalChunks;

    ChunkUpload(String chunkId, int totalChunks) {
        this.chunkId = chunkId;
        this.totalChunks = totalChunks;
    }
}
