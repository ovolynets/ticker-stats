package de.ovolynets.tickerstats.service;

import de.ovolynets.tickerstats.controller.Tick;
import de.ovolynets.tickerstats.controller.TickerStatistics;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

public class TickerServiceTest {

    private static final double PRICE1 = 140;
    private static final double PRICE2 = 142;
    private static final double PRICE3 = 144;

    private static final double PRICE21 = 12;
    private static final double PRICE22 = 16;

    private static final String INSTRUMENT1 = "IBM.N";
    private static final String INSTRUMENT2 = "KO";
    private TickerService tickerService = new TickerServiceImpl();
    private int MILLIS_IN_SECONDS = 1000;

    @Test
    public void addSingleTickSuccessfully() {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis));
        Optional<TickerStatistics> tickerStatisticsOpt = tickerService.getStatistics();
        Assertions.assertThat(tickerStatisticsOpt).isNotEmpty();
        TickerStatistics tickerStatistics = tickerStatisticsOpt.get();
        Assertions.assertThat(tickerStatistics.getAvg()).isEqualTo(PRICE1);
        Assertions.assertThat(tickerStatistics.getCount()).isEqualTo(1);
    }

    @Test
    public void addMultipleTicksOfSameInstrument() {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE2, currentTimeInMillis - 1 * MILLIS_IN_SECONDS));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE3, currentTimeInMillis - 2 * MILLIS_IN_SECONDS));
        Optional<TickerStatistics> tickerStatisticsOpt = tickerService.getStatistics();
        Assertions.assertThat(tickerStatisticsOpt).isNotEmpty();
        TickerStatistics tickerStatistics = tickerStatisticsOpt.get();
        Assertions.assertThat(tickerStatistics.getAvg()).isEqualTo((PRICE1 + PRICE2 + PRICE3)/3.);
        Assertions.assertThat(tickerStatistics.getCount()).isEqualTo(3);
    }

    @Test
    public void addMultipleTicksOfDifferentInstruments() {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE1, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT1, PRICE2, currentTimeInMillis - 1 * MILLIS_IN_SECONDS));
        tickerService.addTick(new Tick(INSTRUMENT2, PRICE21, currentTimeInMillis));
        tickerService.addTick(new Tick(INSTRUMENT2, PRICE22, currentTimeInMillis - 1 * MILLIS_IN_SECONDS));
        Optional<TickerStatistics> tickerStatisticsOpt = tickerService.getStatistics();
        Assertions.assertThat(tickerStatisticsOpt).isNotEmpty();
        TickerStatistics tickerStatistics = tickerStatisticsOpt.get();
        Assertions.assertThat(tickerStatistics.getAvg()).isEqualTo((PRICE1+PRICE2+PRICE21+PRICE22)/4);
        Assertions.assertThat(tickerStatistics.getCount()).isEqualTo(4);

        Optional<TickerStatistics> statistics2Opt = tickerService.getStatistics(INSTRUMENT2);
        Assertions.assertThat(statistics2Opt).isNotEmpty();
        TickerStatistics tickerStatistics2 = statistics2Opt.get();
        Assertions.assertThat(tickerStatistics2.getAvg()).isEqualTo((PRICE21+PRICE22)/2);
        Assertions.assertThat(tickerStatistics2.getCount()).isEqualTo(2);
    }

    @Test
    public void ignoresOldTick() {
        long currentTimeInMillis = ZonedDateTime.now().toInstant().toEpochMilli();
        tickerService.addTick(new Tick("IBM.N", PRICE1, currentTimeInMillis));
        tickerService.addTick(new Tick("IBM.N", PRICE2, currentTimeInMillis - 61 * MILLIS_IN_SECONDS));
        Optional<TickerStatistics> tickerStatisticsOpt = tickerService.getStatistics();
        Assertions.assertThat(tickerStatisticsOpt).isNotEmpty();
        TickerStatistics tickerStatistics = tickerStatisticsOpt.get();
        Assertions.assertThat(tickerStatistics.getAvg()).isEqualTo(PRICE1);
        Assertions.assertThat(tickerStatistics.getCount()).isEqualTo(1);
    }

}
