package cn.frkovo.rhythmcv2.rmcChart.client.editor.model;

public final class SmoothingCircle {
    public final double centerX;
    public final double centerY;
    public final double radius;
    public final double startAngle;
    public final double sweep;

    public SmoothingCircle(double centerX, double centerY, double radius, double startAngle, double sweep) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.startAngle = startAngle;
        this.sweep = sweep;
    }

    public double centerX() { return centerX; }
    public double centerY() { return centerY; }
    public double radius() { return radius; }
    public double startAngle() { return startAngle; }
    public double sweep() { return sweep; }
}
