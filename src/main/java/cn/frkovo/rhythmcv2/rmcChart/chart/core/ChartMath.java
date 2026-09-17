package cn.frkovo.rhythmcv2.rmcChart.chart.core;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.NumEventData;

import java.util.List;

public final class ChartMath {
    private ChartMath() {
    }

    public static double getTransformation(List<NumEventData> events, double beat) {
        double value = 0.0;
        for (NumEventData event : events) {
            if (event.startBeat() > beat) {
                break;
            }
            double startBeat = event.startBeat();
            double endBeat = event.endBeat();
            if (beat >= endBeat) {
                value = event.endValue();
                continue;
            }
            double progress = (beat - startBeat) / (endBeat - startBeat);
            value = EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
            break;
        }
        return value;
    }

    public static double getTransformation(List<NumEventData> events, double beat, double defaultValue) {
        if (events.isEmpty()) {
            return defaultValue;
        }
        return getTransformation(events, beat);
    }

    public static double getDistance(List<NumEventData> speedEvents, double beat) {
        if (!speedEvents.isEmpty() && beat < speedEvents.getFirst().startBeat()) {
            NumEventData firstEvent = speedEvents.getFirst();
            return firstEvent.startValue() * (beat - firstEvent.startBeat());
        }

        double distance = 0.0;
        for (NumEventData event : speedEvents) {
            if (event.startBeat() > beat) {
                break;
            }
            distance += getDistanceFromSingleEvent(event, beat);
        }
        return distance;
    }

    private static double getDistanceFromSingleEvent(NumEventData event, double beat) {
        double startBeat = event.startBeat();
        double endBeat = event.endBeat();
        if (beat < startBeat) {
            return 0.0;
        }
        if (beat > endBeat) {
            return 0.5 * (event.startValue() + event.endValue()) * (endBeat - startBeat);
        }
        double delta = beat - startBeat;
        double progress = delta / (endBeat - startBeat);
        double currentValue = EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
        return 0.5 * (event.startValue() + currentValue) * delta;
    }

    /**
     * 时间域距离（格）：{@code D(beat) = ∫ speed(β) x (60 / BPM(β)) dβ}。
     *
     * <p>speed 单位 = 格/秒（与 BPM 无关）；事件按拍定位，BPM 只决定事件持续多少秒。
     * 与运行时 {@code ChartUtils.getDistanceTimeDomain} 同一模型（预览用梯形近似积分）。
     * 空档 / 空列表不产生位移，首个事件之前的区间按它的起始值恒定外推。</p>
     */
    public static double getDistanceTimeDomain(List<NumEventData> speedEvents, List<BpmPoint> bpms,
                                               double beat) {
        if (speedEvents.isEmpty() || bpms == null || bpms.isEmpty() || !Double.isFinite(beat)) {
            return 0.0;
        }
        double anchor = speedEvents.getFirst().startBeat();
        return beat >= anchor
                ? speedAreaByBpm(speedEvents, bpms, anchor, beat)
                : -speedAreaByBpm(speedEvents, bpms, beat, anchor);
    }

    /** {@code ∫_from^to speed(β) x (60 / BPM(β)) dβ}（from < to；按 BPM 变化点分段）。 */
    private static double speedAreaByBpm(List<NumEventData> speedEvents, List<BpmPoint> bpms,
                                         double from, double to) {
        double total = 0.0;
        double cursor = from;
        int bpmIndex = 0;
        while (cursor < to) {
            while (bpmIndex + 1 < bpms.size() && bpms.get(bpmIndex + 1).beat() <= cursor) {
                bpmIndex++;
            }
            double next = to;
            if (bpmIndex + 1 < bpms.size()) {
                next = Math.min(to, bpms.get(bpmIndex + 1).beat());
            }
            if (next <= cursor) {
                break;
            }
            total += speedArea(speedEvents, cursor, next) * (60.0 / bpms.get(bpmIndex).bpm());
            cursor = next;
        }
        return total;
    }

    /** {@code ∫_from^to speed(β) dβ}（拍域；空档 = 0；首事件前按首值恒定外推）。 */
    private static double speedArea(List<NumEventData> speedEvents, double from, double to) {
        double area = 0.0;
        NumEventData first = speedEvents.getFirst();
        if (from < first.startBeat()) {
            area += first.startValue() * (Math.min(to, first.startBeat()) - from);
        }
        for (NumEventData event : speedEvents) {
            if (event.endBeat() <= from) {
                continue;
            }
            if (event.startBeat() >= to) {
                break;
            }
            double segFrom = Math.max(from, event.startBeat());
            double segTo = Math.min(to, event.endBeat());
            if (segTo > segFrom) {
                area += areaFromStart(event, segTo) - areaFromStart(event, segFrom);
            }
        }
        return area;
    }

    /** {@code ∫_startBeat^x speed(β) dβ}（x 截断到 [startBeat, endBeat]；梯形近似）。 */
    private static double areaFromStart(NumEventData event, double x) {
        double startBeat = event.startBeat();
        double endBeat = event.endBeat();
        if (x <= startBeat || endBeat <= startBeat) {
            return 0.0;
        }
        if (x >= endBeat) {
            return 0.5 * (event.startValue() + event.endValue()) * (endBeat - startBeat);
        }
        double delta = x - startBeat;
        double progress = delta / (endBeat - startBeat);
        double currentValue = EasingFunctions.getEase(event.startValue(), event.endValue(), progress, event.easingType());
        return 0.5 * (event.startValue() + currentValue) * delta;
    }
}
