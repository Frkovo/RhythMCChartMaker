package cn.frkovo.rhythmcv2.rmcChart.client.wizard;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

public final class SongFilePicker {
    private SongFilePicker() {
    }

    public static Path chooseAudioFile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(4);
            filters.put(stack.UTF8("*.ogg"));
            filters.put(stack.UTF8("*.mp3"));
            filters.put(stack.UTF8("*.wav"));
            filters.put(stack.UTF8("*.flac"));
            filters.flip();

            String selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "Choose Song Audio",
                    null,
                    filters,
                    "Audio Files",
                    false
            );
            return selected == null || selected.isBlank() ? null : Path.of(selected);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
