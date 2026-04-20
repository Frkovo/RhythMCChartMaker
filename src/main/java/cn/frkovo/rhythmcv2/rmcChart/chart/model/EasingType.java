package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public enum EasingType {
    LINEAR(0),
    IN_SINE(1),
    OUT_SINE(2),
    IN_OUT_SINE(3),
    IN_QUAD(4),
    OUT_QUAD(5),
    IN_OUT_QUAD(6),
    IN_CUBIC(7),
    OUT_CUBIC(8),
    IN_OUT_CUBIC(9),
    IN_QUART(10),
    OUT_QUART(11),
    IN_OUT_QUART(12),
    IN_QUINT(13),
    OUT_QUINT(14),
    IN_OUT_QUINT(15),
    IN_EXPO(16),
    OUT_EXPO(17),
    IN_OUT_EXPO(18),
    IN_CIRC(19),
    OUT_CIRC(20),
    IN_OUT_CIRC(21),
    IN_BACK(22),
    OUT_BACK(23),
    IN_OUT_BACK(24),
    IN_ELASTIC(25),
    OUT_ELASTIC(26),
    IN_OUT_ELASTIC(27),
    IN_BOUNCE(28),
    OUT_BOUNCE(29),
    IN_OUT_BOUNCE(30),
    IN_SQUARE(31),
    OUT_SQUARE(32),
    IN_OUT_SQUARE(33);

    private final int id;

    EasingType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static EasingType fromId(int id) {
        for (EasingType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return LINEAR;
    }
}
