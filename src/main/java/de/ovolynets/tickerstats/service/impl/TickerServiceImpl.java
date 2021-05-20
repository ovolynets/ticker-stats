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

    public TickerServiceImpl(@Value("${index-update-period-ms:500}") int indexUpdatePeriodMillis) {
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
        Map<String, Double> pricesAtDateTime = priceByDateTime.computeIfAbsent(
                tick.getTimestamp(),
                (ignored) -> new HashMap<>());
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
        // Synchronize on the treeset to avoid concurrent modifications
        // of the data while we traverse it with an iterator
        synchronized (dateTimeRecordsQueue) {
            Iterator<Long> iterator = dateTimeRecordsQueue.iterator();
            while (iterator.hasNext()) {
                Long next = iterator.next();
                if (next > now - SLIDING_WINDOW_MS) {
                    break;
                }
                Map<String, Double> removed = priceByDateTime.remove(next);
                iterator.remove();
                logger.trace("Removing entries with timestamp {}: {}", next, removed);
            }
        }
    }

    private void rebuildStatisticsIndex() {
        // Update the index/cache of the statistics. First remove irrelevant entries
        // that are older than 60 seconds. After that, calculate the statistics
        // while locking the data HashMap for concurrent modifications.
        // Based on the expected traffic, this solution might not scale when the updates
        // come too frequently. In such case, we might need to chunk updates and execute
        // multiple updates at once instead of one-by-one, e.g. by implementing an async
        // queue solution.

        logger.info("Statistics index will be rebuilt");
        removeEldestElements();

        Map<String, TickerAccumulator> indexOfPricesByInstrument = new HashMap<>();
        TickerAccumulator totalIndexAccumulator = new TickerAccumulator();

        // We need to synchronize on the map once again to avoid concurrent modifications
        // while we calculate the statistics
        synchronized (priceByDateTime) {
            for (Map<String, Double> entry: priceByDateTime.values()) {
                for (String instrument : entry.keySet()) {
                    double price = entry.get(instrument);
                    TickerAccumulator accumulator = indexOfPricesByInstrument.computeIfAbsent(instrument, (ignored) -> new TickerAccumulator());
                    accumulator.sum += price;
                    accumulator.max = Math.max(price, accumulator.max);
                    accumulator.min = accumulator.min > 0
                            ? Math.min(price, accumulator.min)
                            : price;
                    accumulator.count += 1;

                    totalIndexAccumulator.sum += price;
                    totalIndexAccumulator.max = Math.max(price, totalIndexAccumulator.max);
                    totalIndexAccumulator.min = totalIndexAccumulator.min > 0
                            ? Math.min(price, totalIndexAccumulator.min)
                            : price;
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
        private double min;
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
