package de.ovolynets.tickerstats.controller;

import org.apache.logging.log4j.util.Strings;

public class Tick {
    private final String instrument;
    private final Double price;
    private final Long timestamp;

    public Tick(String instrument, Double price, Long timestamp) {
        this.instrument = instrument;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getInstrument() {
        return instrument;
    }

    public Double getPrice() {
        return price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public boolean isValid() {
        return !(Strings.isEmpty(instrument) ||
                price == null ||
                price < 0 ||
                timestamp == null ||
                timestamp < 0);
    }
}
