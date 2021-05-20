package de.ovolynets.tickerstats.controller;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class Tick {

    @NotNull
    @NotBlank(message = "Instrument must not be blank")
    private final String instrument;
    @NotNull
    @Min(value = 0, message = "Price must be positive")
    private final Double price;
    @NotNull
    @Min(value = 0, message = "Timestamp cannot be negative")
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Tick{");
        sb.append("instrument='").append(instrument).append('\'');
        sb.append(", price=").append(price);
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}
