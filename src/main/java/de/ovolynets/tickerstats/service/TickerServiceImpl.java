package de.ovolynets.tickerstats.service;

import de.ovolynets.tickerstats.controller.Tick;
import de.ovolynets.tickerstats.controller.TickerStatistics;

import java.util.Optional;

public class TickerServiceImpl implements TickerService {
    @Override
    public boolean addTick(Tick tick) {
        return false;
    }

    @Override
    public Optional<TickerStatistics> getStatistics() {
        return Optional.empty();
    }

    @Override
    public Optional<TickerStatistics> getStatistics(String instrumentId) {
        return Optional.empty();
    }
}
