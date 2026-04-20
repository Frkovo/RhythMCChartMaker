package cn.frkovo.rhythmcv2.rmcChart.chart.model;

import java.util.ArrayList;
import java.util.List;

public class MetaData {
    private int uid;
    private String initialArena;
    private long offset;
    private double level;
    private final List<String> charters = new ArrayList<>();
    private final List<String> comments = new ArrayList<>();
    private final List<BpmPoint> bpms = new ArrayList<>();

    public MetaData(int uid, String initialArena, long offset, double level) {
        this.uid = uid;
        this.initialArena = initialArena;
        this.offset = offset;
        this.level = level;
    }

    public static MetaData createDefault(int uid) {
        MetaData meta = new MetaData(uid, "default", 0L, 1.0);
        meta.bpms.add(new BpmPoint(0.0, 120.0));
        return meta;
    }

    public int uid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String initialArena() {
        return initialArena;
    }

    public void setInitialArena(String initialArena) {
        this.initialArena = initialArena;
    }

    public long offset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public double level() {
        return level;
    }

    public void setLevel(double level) {
        this.level = level;
    }

    public List<String> charters() {
        return charters;
    }

    public List<String> comments() {
        return comments;
    }

    public List<BpmPoint> bpms() {
        return bpms;
    }
}
