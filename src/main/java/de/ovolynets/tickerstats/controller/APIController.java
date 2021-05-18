package de.ovolynets.tickerstats.controller;

import java.util.Optional;

import de.ovolynets.tickerstats.service.TickerResponse;
import de.ovolynets.tickerstats.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
public class APIController {

    private static final String RESPONSE_MESSAGE_SUCCESS = "Success";
    private static final String RESPONSE_MESSAGE_TICK_TOO_OLD = "Tick older than 60 seconds";
    private static final String RESPONSE_MESSAGE_BAD_REQUEST = "Malformed request";

    private final TickerService tickerService;

    @Autowired
    public APIController(final TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @PostMapping(value = "/ticks", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TicksPostResponse> postTicks(Tick tick) {
        if (!tick.isValid()) {
            return ResponseEntity.badRequest().body(new TicksPostResponse(RESPONSE_MESSAGE_BAD_REQUEST));
        }
        if (tickerService.addTick(tick)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(new TicksPostResponse(RESPONSE_MESSAGE_SUCCESS));
        }
    }

    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TickerResponse> getStatistics() {
        Optional<TickerResponse> result = tickerService.getStatistics();
        return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/statistics/{instrumentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TickerResponse> getStatistics(@PathVariable("instrumentId") final String instrumentId) {
        Optional<TickerResponse> result = tickerService.getStatistics(instrumentId);
        return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
