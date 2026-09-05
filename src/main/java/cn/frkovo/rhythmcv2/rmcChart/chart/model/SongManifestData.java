package cn.frkovo.rhythmcv2.rmcChart.chart.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SongManifestData {
    private String name = "Unknown Title";
    private String composer = "Unknown Composer";
    private String icon = "NOTE_BLOCK";
    private String alias = "";
    private int length = 0;
    private String respackSha1 = "";
    private String key = "";
    private String description = "";
    private int songId = 0;
    private String version = "1.0";
    private final List<String> comments = new ArrayList<>();
    private final List<String> playerAlias = new ArrayList<>();
    private final List<String> tags = new ArrayList<>();
    private final List<Map<String, Object>> unlockSong = new ArrayList<>();
    private final List<Map<String, Object>> unlockWorld = new ArrayList<>();
    private final List<Map<String, Object>> unlockNether = new ArrayList<>();
    private final List<Map<String, Object>> unlockVoid = new ArrayList<>();
    private int beatsPerBar = 4;

    public static SongManifestData createDefault() {
        return new SongManifestData();
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String composer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String icon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String alias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int length() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String respackSha1() {
        return respackSha1;
    }

    public void setRespackSha1(String respackSha1) {
        this.respackSha1 = respackSha1;
    }

    public String key() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int songId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String version() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> comments() {
        return comments;
    }

    public List<String> playerAlias() {
        return playerAlias;
    }

    public List<String> tags() {
        return tags;
    }

    public List<Map<String, Object>> unlockSong() {
        return unlockSong;
    }

    public List<Map<String, Object>> unlockWorld() {
        return unlockWorld;
    }

    public List<Map<String, Object>> unlockNether() {
        return unlockNether;
    }

    public List<Map<String, Object>> unlockVoid() {
        return unlockVoid;
    }

    /**
     * Editor-only beats per bar for the Preview tunnel shell (bar rings + labels).
     * Gameplay ignores it. Persisted under the {@code editor:} block.
     */
    public int beatsPerBar() {
        return beatsPerBar;
    }

    public void setBeatsPerBar(int beatsPerBar) {
        if (beatsPerBar <= 0) {
            this.beatsPerBar = 4;
            return;
        }
        this.beatsPerBar = Math.max(1, Math.min(32, beatsPerBar));
    }

    public Map<String, Object> toOrderedMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("composer", composer);
        map.put("icon", icon);
        map.put("alias", alias);
        map.put("length", length);
        map.put("respack_sha1", respackSha1);
        if (!key.isBlank()) {
            map.put("key", key);
        }
        map.put("description", description);
        map.put("song_id", songId);
        map.put("version", version);
        map.put("comments", comments);
        map.put("player-alias", playerAlias);
        map.put("tags", tags);
        map.put("unlockSong", unlockSong);
        map.put("unlockWorld", unlockWorld);
        map.put("unlockNether", unlockNether);
        map.put("unlockVoid", unlockVoid);
        Map<String, Object> editor = new LinkedHashMap<>();
        editor.put("beatsPerBar", beatsPerBar);
        map.put("editor", editor);
        return map;
    }
}
