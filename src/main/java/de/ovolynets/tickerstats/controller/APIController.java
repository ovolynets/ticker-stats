package de.ovolynets.tickerstats.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.ovolynets.tickerstats.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/")
public class APIController {

    private final TickerService tickerService;

    @Autowired
    public APIController(final TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @PostMapping(value = "/ticks", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> postTicks(@Valid @RequestBody Tick tick) {
        if (tickerService.addTick(tick)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).build();
        }
    }

    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TickerStatistics> getStatistics() {
        TickerStatistics result = tickerService.getStatistics();
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/statistics/{instrumentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TickerStatistics> getStatistics(@PathVariable("instrumentId") final String instrumentId) {
        Optional<TickerStatistics> result = tickerService.getStatistics(instrumentId);
        return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
