package de.ovolynets.tickerstats.service;

import de.ovolynets.tickerstats.controller.Tick;
import de.ovolynets.tickerstats.controller.TickerStatistics;
import de.ovolynets.tickerstats.service.impl.TickerServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TickerServiceTest {

    private static final double PRICE1 = 140;
    private static final double PRICE2 = 142;
    private static final double PRICE3 = 144;

    private static final double PRICE21 = 12;
    private static final double PRICE22 = 16;

    private static final int SECOND_TO_MILLIS = 1000;

    private static final String INSTRUMENT1 = "IBM.N";
    private static final String INSTRUMENT2 = "KO";

    private static final int INDEX_UPDATE_PERIOD_MS = 10;
    private TickerService tickerService;

    @Before
    public void setUp() {
        tickerService = new TickerServiceImpl(INDEX_UPDATE_PERIOD_MS);
    }

    @Test
    public void addMultipleTicksSuccessfully() throws InterruptedException {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis - 1 * SECOND_TO_MILLIS));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis - 2 * SECOND_TO_MILLIS));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis - 3 * SECOND_TO_MILLIS));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis - 64 * SECOND_TO_MILLIS));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis - 65 * SECOND_TO_MILLIS));

        Thread.sleep(100);
        TickerStatistics tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo(PRICE1);
        assertThat(tickerStatistics.getCount()).isEqualTo(3);
    }

    @Test
    public void requestEmptyStatistics() {
        TickerStatistics tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo(0);
        assertThat(tickerStatistics.getMax()).isEqualTo(0);
        assertThat(tickerStatistics.getMin()).isEqualTo(0);
        assertThat(tickerStatistics.getCount()).isEqualTo(0);
    }

    @Test
    public void addSoonExpiringTicks() throws InterruptedException {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis - 1 * SECOND_TO_MILLIS));
        // This tick is aboud to expire while statistics calculation
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE2, currentTimeInMillis - 60 * SECOND_TO_MILLIS + 150));

        // First, both ticks contribute to the statistics
        Thread.sleep(100);
        TickerStatistics tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo((PRICE1+PRICE2)/2);
        assertThat(tickerStatistics.getCount()).isEqualTo(2);

        // After waiting for another 100ms another round of statistics rebuild and cleanup of old values has been run
        Thread.sleep(100);
        tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo(PRICE1);
        assertThat(tickerStatistics.getCount()).isEqualTo(1);
    }

    @Test
    public void addMultipleTicksOfSameInstrument() throws InterruptedException {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE2, currentTimeInMillis - 1 * SECOND_TO_MILLIS));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE3, currentTimeInMillis - 2 * SECOND_TO_MILLIS));

        Thread.sleep(100);
        TickerStatistics tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo((PRICE1 + PRICE2 + PRICE3)/3.);
        assertThat(tickerStatistics.getMax()).isEqualTo(PRICE3);
        assertThat(tickerStatistics.getMin()).isEqualTo(PRICE1);
        assertThat(tickerStatistics.getCount()).isEqualTo(3);
    }

    @Test
    public void addMultipleTicksOfDifferentInstruments() throws InterruptedException {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE2, currentTimeInMillis - SECOND_TO_MILLIS));
        tickerService.addTick(new Tick(INSTRUMENT2, PRICE21, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT2, PRICE22, currentTimeInMillis - SECOND_TO_MILLIS));

        Thread.sleep(100);
        TickerStatistics tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo((PRICE1+PRICE2+PRICE21+PRICE22)/4);
        assertThat(tickerStatistics.getCount()).isEqualTo(4);

        Optional<TickerStatistics> statistics2Opt = tickerService.getStatistics(INSTRUMENT2);
        assertThat(statistics2Opt).isNotEmpty();
        TickerStatistics tickerStatistics2 = statistics2Opt.get();
        assertThat(tickerStatistics2.getAvg()).isEqualTo((PRICE21+PRICE22)/2);
        assertThat(tickerStatistics2.getMax()).isEqualTo(PRICE22);
        assertThat(tickerStatistics2.getMin()).isEqualTo(PRICE21);
        assertThat(tickerStatistics2.getCount()).isEqualTo(2);
    }

    @Test
    public void ignoresOldTick() throws InterruptedException {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE2, currentTimeInMillis - 61 * SECOND_TO_MILLIS));

        Thread.sleep(100);
        TickerStatistics tickerStatistics = tickerService.getStatistics();
        assertThat(tickerStatistics.getAvg()).isEqualTo(PRICE1);
        assertThat(tickerStatistics.getCount()).isEqualTo(1);
    }
}
