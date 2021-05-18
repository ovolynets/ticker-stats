package de.ovolynets.tickerstats.service;

import de.ovolynets.tickerstats.controller.Tick;
import de.ovolynets.tickerstats.controller.TickerStatistics;

import java.util.Optional;

public interface TickerService {
    boolean addTick(Tick tick);
    Optional<TickerStatistics> getStatistics();
    Optional<TickerStatistics> getStatistics(String instrumentId);
}
