package de.ovolynets.tickerstats.service;

import de.ovolynets.tickerstats.controller.Tick;
import de.ovolynets.tickerstats.controller.TickerStatistics;

import java.util.Optional;

public interface TickerService {
    /**
     * Add tick data into the database
     *
     * @param tick tick data containing instrument name, price and timestamp
     * @return {@code false} if data is older than 60 seconds, {@code true} otherwise
     */
    boolean addTick(Tick tick);

    /**
     * Returns overall statistics on all available ticker data
     *
     * @return overall statistics
     */
    TickerStatistics getStatistics();

    /**
     * Returns statistics on ticker data for a given instrument ID. If no data is available
     * for the given instrument, returns {@code Optional.empty()}
     *
     * @return statistics data for the instrument
     */
    Optional<TickerStatistics> getStatistics(String instrumentId);
}
