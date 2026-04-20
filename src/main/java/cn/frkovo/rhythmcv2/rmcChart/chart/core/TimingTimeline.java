package cn.frkovo.rhythmcv2.rmcChart.chart.core;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.LevelData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimingTimeline {
    private final List<Entry> bpmList;
    private final long offsetMillis;

    public TimingTimeline(LevelData level) {
        this(level.meta().bpms(), level.meta().offset());
    }

    public TimingTimeline(List<BpmPoint> bpms, long offsetMillis) {
        this.offsetMillis = offsetMillis;
        this.bpmList = new ArrayList<>();
        for (BpmPoint bpm : bpms) {
            this.bpmList.add(new Entry(bpm.beat(), bpm.bpm(), 0L));
        }
        this.bpmList.sort(Comparator.comparingDouble(Entry::beat));
        if (this.bpmList.isEmpty()) {
            this.bpmList.add(new Entry(0.0, 120.0, 0L));
        }
        initBpmList();
    }

    public long beatToMillis(double beat) {
        Entry target;
        if (!bpmList.isEmpty() && beat < bpmList.getFirst().beat()) {
            target = bpmList.getFirst();
        } else {
            target = findBpmByBeat(beat);
        }
        if (target == null) {
            return offsetMillis;
        }
        return target.timeMillis() + Math.round(60000.0 / target.bpm() * (beat - target.beat())) + offsetMillis;
    }

    public double calcBeat(long millis) {
        long time = millis - offsetMillis;
        Entry target;
        if (time < 0 && !bpmList.isEmpty()) {
            target = bpmList.getFirst();
        } else {
            target = findBpmByTimeMillis(time);
        }
        if (target == null) {
            return 0.0;
        }
        return target.beat() + (time - target.timeMillis()) * target.bpm() / 60000.0;
    }

    private Entry findBpmByBeat(double beat) {
        int low = 0;
        int high = bpmList.size() - 1;
        Entry result = null;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            Entry entry = bpmList.get(mid);
            if (entry.beat() <= beat) {
                result = entry;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    private Entry findBpmByTimeMillis(long timeMillis) {
        int low = 0;
        int high = bpmList.size() - 1;
        Entry result = null;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            Entry entry = bpmList.get(mid);
            if (entry.timeMillis() <= timeMillis) {
                result = entry;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    private void initBpmList() {
        Entry first = bpmList.getFirst();
        first.setTimeMillis(0L);
        for (int index = 1; index < bpmList.size(); index++) {
            Entry previous = bpmList.get(index - 1);
            Entry current = bpmList.get(index);
            long timeMillis = previous.timeMillis() + Math.round(60000.0 / previous.bpm() * (current.beat() - previous.beat()));
            current.setTimeMillis(timeMillis);
        }
    }

    private static final class Entry {
        private final double beat;
        private final double bpm;
        private long timeMillis;

        private Entry(double beat, double bpm, long timeMillis) {
            this.beat = beat;
            this.bpm = bpm;
            this.timeMillis = timeMillis;
        }

        public double beat() {
            return beat;
        }

        public double bpm() {
            return bpm;
        }

        public long timeMillis() {
            return timeMillis;
        }

        public void setTimeMillis(long timeMillis) {
            this.timeMillis = timeMillis;
        }
    }
}
