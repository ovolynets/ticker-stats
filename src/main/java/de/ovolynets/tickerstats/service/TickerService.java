package de.ovolynets.tickerstats.service;

import de.ovolynets.tickerstats.controller.Tick;

import java.util.Optional;

public interface TickerService {
    boolean addTick(Tick tick);
    Optional<TickerResponse> getStatistics();
    Optional<TickerResponse> getStatistics(String instrumentId);
}
