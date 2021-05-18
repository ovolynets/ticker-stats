package de.ovolynets.tickerstats.service;

public class TickerResponse {
    private final double avg;
    private final double max;
    private final double min;
    private final long count;


    public TickerResponse(double avg, double max, double min, long count) {
        this.avg = avg;
        this.max = max;
        this.min = min;
        this.count = count;
    }
}
