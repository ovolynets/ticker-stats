package de.ovolynets.tickerstats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;

@ComponentScan("de.ovolynets.tickerstats")
@SpringBootApplication
public class TickerServiceApp {
    public static void main(final String... args) {
        SpringApplication.run(TickerServiceApp.class, args);
    }
}
