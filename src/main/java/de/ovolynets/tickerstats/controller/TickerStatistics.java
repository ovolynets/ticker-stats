package de.ovolynets.tickerstats.controller;

public class TickerStatistics {
    private final double avg;
    private final double max;
    private final double min;
    private final long count;

    public TickerStatistics(double avg, double max, double min, long count) {
        this.avg = avg;
        this.max = max;
        this.min = min;
        this.count = count;
    }

    public double getAvg() {
        return avg;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public long getCount() {
        return count;
    }
}
