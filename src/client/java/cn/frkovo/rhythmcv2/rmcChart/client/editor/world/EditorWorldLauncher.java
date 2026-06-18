package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

/**
 * Stub retained only to keep {@code ChartEditorScreen} compiling during the reborn rewrite.
 * All in-world display/playback has been removed; chart preview now runs on a real
 * RhythMC-Preview server via the {@code rhythmc:chart_preview} plugin channel.
 *
 * <p>Every method here is a no-op or returns a safe default. This stub will be removed
 * once {@code ChartEditorScreen} is fully rewritten to drop its in-world code paths.
 */
public final class EditorWorldLauncher {
    public EditorWorldLauncher(Object ignored) {
    }

    public void tick(net.minecraft.client.MinecraftClient client) {
    }

    public boolean isEditorWorldActive(net.minecraft.client.MinecraftClient client) {
        return false;
    }

    public void toggleOverlay() {
    }

    public void toggleWorldPlayback() {
    }

    public void stopWorldPlayback() {
    }

    public void setWorldPlaybackAutoPlay(boolean autoPlay) {
    }

    public void syncDisplays() {
    }

    public boolean pickLookTarget() {
        return false;
    }

    public void dragSelection(double deltaX, double deltaY, int button, boolean shiftDown) {
    }

    public void onEditorClosed() {
    }

    public PlaybackEngineStub playbackEngine() {
        return new PlaybackEngineStub();
    }

    public static final class PlaybackEngineStub {
        public boolean isActive() {
            return false;
        }

        public boolean isAutoPlay() {
            return false;
        }

        public Object judge() {
            return null;
        }
    }
}
