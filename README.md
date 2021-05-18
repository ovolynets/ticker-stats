# Ticker statistics
This repository contains a "Ticker Statistics" service which provides a convenient REST API
to submit ticker prices for individual instruments and read statistics per instrument and overall.

## Run tests
`mvn clean test`

## Run and test locally
```
mvn clean spring-boot:run

curl localhost:8080/api/ticks --header 'Content-Type: application/json' -d '{"instrument": "IBM.N", "price": 143.82, "timestamp": 1478192204000}'
curl localhost:8080/api/statistics
```

## API documentation
You can find the OpenAPI 3.0 definition in `api/ticker-api.yaml` file.

## Tradeoffs, nice-to-haves, decisions, TODOs

1. TicksPostResponse to provide a more detailed message about the status of the API call
