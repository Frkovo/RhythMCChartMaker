package cn.frkovo.rhythmcv2.rmcChart.chart.model;

public class Vec3Data {
    private double x;
    private double y;
    private double z;

    public Vec3Data() {
        this(0.0, 0.0, 0.0);
    }

    public Vec3Data(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vec3Data zero() {
        return new Vec3Data(0.0, 0.0, 0.0);
    }

    public static Vec3Data one() {
        return new Vec3Data(1.0, 1.0, 1.0);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3Data copy() {
        return new Vec3Data(x, y, z);
    }
}
