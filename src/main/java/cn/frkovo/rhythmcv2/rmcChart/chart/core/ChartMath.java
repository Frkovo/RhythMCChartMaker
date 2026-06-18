package cn.frkovo.rhythmcv2.rmcChart.chart.core;

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
}
