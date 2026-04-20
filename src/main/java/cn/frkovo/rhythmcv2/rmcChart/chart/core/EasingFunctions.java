package cn.frkovo.rhythmcv2.rmcChart.chart.core;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.EasingType;

import java.util.List;
import java.util.function.Function;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public final class EasingFunctions {
    private static final List<Function<Double, Double>> EASING_FUNCTIONS = List.of(
            x -> x,
            x -> 1 - cos(x * PI) / 2,
            x -> sin(x * PI) / 2,
            x -> -(cos(PI * x) - 1) / 2,
            x -> x * x,
            x -> 1 - (1 - x) * (1 - x),
            x -> x < 0.5 ? 2 * x * x : 1 - pow(-2 * x + 2, 2) / 2,
            x -> x * x * x,
            x -> 1 - pow(1 - x, 3),
            x -> x < 0.5 ? 4 * x * x * x : 1 - pow(-2 * x + 2, 3) / 2,
            x -> x * x * x * x,
            x -> 1 - pow(1 - x, 4),
            x -> x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2,
            x -> x * x * x * x * x,
            x -> 1 - pow(1 - x, 5),
            x -> x < 0.5 ? 16 * x * x * x * x * x : 1 - pow(-2 * x + 2, 5) / 2,
            x -> x == 0 ? 0 : pow(2, 10 * x - 10),
            x -> x == 1 ? 1 : 1 - pow(2, -10 * x),
            x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? pow(2, 20 * x - 10) / 2 : (2 - pow(2, -20 * x + 10)) / 2,
            x -> 1 - sqrt(1 - pow(x, 2)),
            x -> sqrt(1 - pow(x - 1, 2)),
            x -> x < 0.5 ? (1 - sqrt(1 - pow(2 * x, 2))) / 2 : (sqrt(1 - pow(-2 * x + 2, 2)) + 1) / 2,
            x -> 2.70158 * x * x * x - 1.70158 * x * x,
            x -> 1 + 2.70158 * pow(x - 1, 3) + 1.70158 * pow(x - 1, 2),
            x -> x < 0.5 ? (pow(2 * x, 2) * ((1.70158 * 1.525 + 1) * 2 * x - 1.70158 * 1.525)) / 2 : (pow(2 * x - 2, 2) * ((1.70158 * 1.525 + 1) * (x * 2 - 2) + 1.70158 * 1.525) + 2) / 2,
            x -> x == 0 ? 0 : x == 1 ? 1 : -pow(2, 10 * x - 10) * sin((x * 10 - 10.75) * ((2 * PI) / 3)),
            x -> x == 0 ? 0 : x == 1 ? 1 : pow(2, -10 * x) * sin((x * 10 - 0.75) * ((2 * PI) / 3)) + 1,
            x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? -(pow(2, 20 * x - 10) * sin((20 * x - 11.125) * ((2 * PI) / 4.5))) / 2 : (pow(2, -20 * x + 10) * sin((20 * x - 11.125) * ((2 * PI) / 4.5))) / 2 + 1,
            x -> 1 - easeOutBounce(x),
            EasingFunctions::easeOutBounce,
            x -> x < 0.5 ? (1 - easeOutBounce(1 - 2 * x)) / 2 : (1 + easeOutBounce(2 * x - 1)) / 2,
            x -> 0.0,
            x -> 1.0,
            x -> x < 0.5 ? 0.0 : 1.0
    );

    private EasingFunctions() {
    }

    public static double getEase(double t, EasingType easingType) {
        if (t < 0.0) {
            return 0.0;
        }
        if (t > 1.0) {
            return 1.0;
        }
        return EASING_FUNCTIONS.get(easingType.ordinal()).apply(t);
    }

    public static double getEase(double start, double end, double t, EasingType easingType) {
        if (t < 0.0) {
            return start;
        }
        if (t > 1.0) {
            return end;
        }
        return start + (end - start) * getEase(t, easingType);
    }

    public static double getEaseAt(double start, double end, double current, EasingType easingType) {
        if (start == end) {
            return start;
        }
        if (current <= start) {
            return start;
        }
        if (current >= end) {
            return end;
        }
        double t = (current - start) / (end - start);
        return getEase(start, end, t, easingType);
    }

    private static double easeOutBounce(double x) {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (x < 1 / d1) {
            return n1 * x * x;
        }
        if (x < 2 / d1) {
            x -= 1.5 / d1;
            return n1 * x * x + 0.75;
        }
        if (x < 2.5 / d1) {
            x -= 2.25 / d1;
            return n1 * x * x + 0.9375;
        }
        x -= 2.625 / d1;
        return n1 * x * x + 0.984375;
    }
}
