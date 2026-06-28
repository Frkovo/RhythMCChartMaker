package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EffectData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Editor-only scratch data that is not serialized to the compiled chart JSON.
 * This includes user-created groups for effects and notes used purely for
 * organizing the timeline while drafting.
 */
public class EditorDraft {
    private final Map<String, List<EffectData>> effectGroups = new LinkedHashMap<>();
    private final Map<String, List<NoteGroupEntry>> noteGroups = new LinkedHashMap<>();

    public Map<String, List<EffectData>> effectGroups() {
        return effectGroups;
    }

    public Map<String, List<NoteGroupEntry>> noteGroups() {
        return noteGroups;
    }

    public void clear() {
        effectGroups.clear();
        noteGroups.clear();
    }

    public void createEffectGroup(String name, List<EffectData> effects) {
        effectGroups.put(name, new ArrayList<>(effects));
    }

    public void createNoteGroup(String name, List<NoteGroupEntry> notes) {
        noteGroups.put(name, new ArrayList<>(notes));
    }

    public void removeEffectGroup(String name) {
        effectGroups.remove(name);
    }

    public void removeNoteGroup(String name) {
        noteGroups.remove(name);
    }

    public boolean isEffectInGroup(EffectData effect, String groupName) {
        List<EffectData> group = effectGroups.get(groupName);
        return group != null && group.contains(effect);
    }

    public boolean isNoteInGroup(NoteGroupEntry note, String groupName) {
        List<NoteGroupEntry> group = noteGroups.get(groupName);
        return group != null && group.contains(note);
    }

    public Set<String> effectGroupNamesContaining(EffectData effect) {
        Set<String> names = new LinkedHashSet<>();
        for (Map.Entry<String, List<EffectData>> entry : effectGroups.entrySet()) {
            if (entry.getValue().contains(effect)) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    public Set<String> noteGroupNamesContaining(NoteGroupEntry note) {
        Set<String> names = new LinkedHashSet<>();
        for (Map.Entry<String, List<NoteGroupEntry>> entry : noteGroups.entrySet()) {
            if (entry.getValue().contains(note)) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    public record NoteGroupEntry(int trackId, double beat, cn.frkovo.rhythmcv2.rmcChart.chart.model.NoteType noteType) {
    }
}
