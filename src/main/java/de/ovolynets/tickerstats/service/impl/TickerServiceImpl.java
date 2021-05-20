package de.ovolynets.tickerstats.service.impl;

import de.ovolynets.tickerstats.controller.Tick;
import de.ovolynets.tickerstats.controller.TickerStatistics;
import de.ovolynets.tickerstats.service.TickerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TickerServiceImpl implements TickerService {

    private final Logger logger = LoggerFactory.getLogger(TickerService.class);

    private static final int SLIDING_WINDOW_MS = 60 * 1_000;

    // Database of input data, each millisecond corresponds to one entry.
    // Initial capacity is set large enough as twice the expected "useful" data of the past 60 seconds
    // to avoid unnecessary but expected expansion of the underlying data structure
    // "Useless" data older than 60 seconds would be wiped out by the scheduled job of index rebuild
    private final Map<Long, Map<String, Double>> priceByDateTime = new ConcurrentHashMap<>(2 * SLIDING_WINDOW_MS);

    // Sorted set of available datapoints for easy removal of data older than 60 seconds
    // while rebuilding the statistics index
    private final Set<Long> dateTimeRecordsQueue = Collections.synchronizedSortedSet(new TreeSet<>());

    // Frequently updated cache for the general statistics
    private TickerStatistics totalStatistics = new TickerStatistics();
    // Frequently updated cache for the statistics per instrument
    private final Map<String, TickerStatistics> statisticsIndexByInstrument = new ConcurrentHashMap<>();

    public TickerServiceImpl(@Value("${index-update-period-ms}:500") int indexUpdatePeriodMillis) {
        // Schedule statistics updates in regular intervals
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::rebuildStatisticsIndex, 0, indexUpdatePeriodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean addTick(Tick tick) {
        long now = ZonedDateTime.now().toInstant().toEpochMilli();
        if (tick.getTimestamp() < now - SLIDING_WINDOW_MS) {
            return false;
        }
        Map<String, Double> pricesAtDateTime = priceByDateTime.computeIfAbsent(tick.getTimestamp(), (ignored) -> new HashMap<>());
        pricesAtDateTime.put(tick.getInstrument(), tick.getPrice());
        dateTimeRecordsQueue.add(tick.getTimestamp());

        // A possible design change: remove elder elements and rebuild statistics on every update
        // Might create a large overhead but might be necessary if we need an updated statistics
        // as frequent as possible
//        removeEldestElements();
//        rebuildStatisticsIndex(now);
        return true;
    }

    @Override
    public TickerStatistics getStatistics() {
        return totalStatistics;
    }

    @Override
    public Optional<TickerStatistics> getStatistics(String instrumentId) {
        return Optional.ofNullable(statisticsIndexByInstrument.get(instrumentId));
    }

    private void removeEldestElements() {
        long now = ZonedDateTime.now().toInstant().toEpochMilli();
        synchronized (dateTimeRecordsQueue) {
            Iterator<Long> iterator = dateTimeRecordsQueue.iterator();
            while (iterator.hasNext()) {
                Long next = iterator.next();
                if (next > now - SLIDING_WINDOW_MS) {
                    break;
                }
                priceByDateTime.remove(next);
                iterator.remove();
            }
        }
    }

    private void rebuildStatisticsIndex() {
        logger.info("Statistics index will be rebuilt");
        removeEldestElements();
        Map<String, TickerAccumulator> indexOfPricesByInstrument = new HashMap<>();
        TickerAccumulator totalIndexAccumulator = new TickerAccumulator();

        synchronized (priceByDateTime) {
            for (Map<String, Double> entry: priceByDateTime.values()) {
                for (String instrument : entry.keySet()) {
                    double price = entry.get(instrument);
                    TickerAccumulator accumulator = indexOfPricesByInstrument.computeIfAbsent(instrument, (ignored) -> new TickerAccumulator());
                    accumulator.sum += price;
                    accumulator.max = Math.max(price, accumulator.max);
                    accumulator.min = Math.min(price, accumulator.min);
                    accumulator.count += 1;

                    totalIndexAccumulator.sum += price;
                    totalIndexAccumulator.max = Math.max(price, totalIndexAccumulator.max);
                    totalIndexAccumulator.min = Math.max(price, totalIndexAccumulator.min);
                    totalIndexAccumulator.count += 1;
                }
            }
        }
        totalStatistics = totalIndexAccumulator.toTickerStatistics();

        statisticsIndexByInstrument.clear();
        for (Map.Entry<String, TickerAccumulator> entry: indexOfPricesByInstrument.entrySet()) {
            TickerAccumulator accumulator = entry.getValue();
            TickerStatistics tickerStatistics = accumulator.toTickerStatistics();
            statisticsIndexByInstrument.put(entry.getKey(), tickerStatistics);
        }
        logger.info("Statistics index rebuild finished");
    }

    // Private helper accumulator to ease statistics calculations
    private static class TickerAccumulator {
        private double sum;
        private double max;
        private double min = Double.MAX_VALUE;
        private long count;

        private TickerStatistics toTickerStatistics() {
            return new TickerStatistics(
                    count > 0 ? sum / count : 0,
                    max,
                    min,
                    count
            );
        }
    }
}
