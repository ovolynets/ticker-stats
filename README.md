# Ticker statistics
This repository contains a "Ticker Statistics" service which provides a convenient REST API
to submit ticker prices for individual instruments and read statistics per instrument and overall.

## Run tests
`mvn clean test`

## Run and test locally
```
mvn clean spring-boot:run

curl localhost:8080/api/ticks -H "Content-Type: application/json" -d '{"instrument": "IBM.N", "price": 143.82, "timestamp": 1478192204000}'
curl localhost:8080/api/statistics
```

## API documentation
You can find the OpenAPI 3.0 definition in `api/ticker-api.yaml` file.


## Design considerations

1. Entry point is APIController that defines the routes of the application starting with a base path of `/api`
2. The core of the application is `TickerService` (interface) and its implementation of `TickerServiceImpl`
3. The application keep track of the price information in a combination of HashMap
(structure: timestamp -> Map (instrument -> price)) and a sorted set of available timestamps to make the deletion of
old entries efficient. Old entries are deleted in regular intervals. Statistics is also updated in regular intervals
(same as deletion of old entries) and is stored in a cached variable that is used to return the result in O(1) time.
Also see comments in TickerServiceImpl class.
4. Cleanup of old entries happens in regular intervals in a separate thread by a spring-scheduled executor service.
For this implementation, this interval is set to 500ms - subject to adjustments based on more detailed specifications.
Relevant data structures are synchronized to not allow for parallel updates - again, subject for adjustments,
see the notes on traffic considerations below.


### Tradeoffs, nice-to-haves, decisions, TODOs

1. Traffic considerations and immediate possible improvements. The implemented solution should work fine with
   traffic up to, say, 1,000 rps on updates and about 10,000 rps on reading the statistics
   (precise calculations were not performed), maybe slightly more. If we expect a significantly
   higher traffic on updates, we might need to implement an asynchronous queue to chunk write requests and execute them
   at once, instead of one-by-one. This will make the response be returned to the client faster (because of potential
   waiting for a synchronized write operation to ConcurrentHashMap). Alternatively, we might execute statistics
   calculation less frequently (e.g. once a second, rather than every 500ms as in this solution)
   if the accuracy of the data has less business priority.
   We might also want to analyze the nature of incoming data. The implemented solution focuses on quick random access
   when the timestamp of the incoming data is likely to be distinct, i.e. written to different entries of the HashMap
   (key is timestamp). If we expect that the data often comes in chunks of the same timestamp, we end up working on
   the same entry concurrently and this might have slight performance implications. This would be potentially a data
   mining problem, or a task to set appropriate monitoring for the data stream.
2. On strategies for rebuilding the statistics index/cache. Currently, the implemented strategy is to frequently update
   the index by a separate schedule job (every 500 ms by default, configurable). It should work fine for general
   average-traffic mode (say, up to 1000 rps for each read and write). There are multiple alternatives, such as:
   - for frequent updates of the data, which would be affected by the block on the relevant HashMap and TreeSet.
     In such case we might need to chunk the updates and insert them in blocks.
   - for the requirement of having as frequently updated statistics as possible. In such case we might need to rebuild
     the index on every update to the data and not wait for the next scheduler execution.
     This will potentially create a large overhead if the updates come too frequent.
   - for non-frequent requests for statistics (e.g. /statistics endpoint called only once a second) we might
     relax the condition for getting statistics in O(1) time in the requirements and calculate it on-the-fly.
     This would reduce CPU usage but of course this will potentially require re-negotiations of the requirements.
3. Testing. There are two tests:
   - basic controller test that is mostly to check that spring boot starts and that expected response types
    are as should be. It doesn't check the business logic, for which there are better and more detailed unit tests.
   - unit test for the core TickerService to check for all possible scenarios. It does check also that the old entries
    older than 60 seconds expire, although sub-optimal using Thread.sleep(). This can be changed by considering slight
    redesign based on traffic consideration (see above).
4. BigDecimal vs double - we might (or even should) use BigDecimal for price calculations. There are no sophisticated
   calculations done with the double type but in general we might need to switch to BigDecimal. I haven't done it here
   to avoid over-engineering of the initial solution.
5. Logging. There aren't much logging due to the potential high traffic, except of when the statistics index
   is being rebuilt. It might be useful to add some general messages which would indicate that the system is
   running and in case there are errors. Here it should be done with caution so not to print too many messages/errors
   which might explode the logfile in case of high traffic.

### Minor comments to the presented solution:
1. I didn't use annotations processors (lombok) for boilerplate code reduction - is a personal preference but can
   be done of course
2. Concurrency is not tested by unit tests, although it would be nice to stress-test it a bit.
3. It might be a good idea to move the sliding window of 60 seconds as a configuration parameter.

### Long-term improvements
1. Performance/stress test - definitely. Concurrency check, possibly stress testing to estimate the
   highest possible traffic.
2. Monitoring - would be good to expose the health of the system such as current traffic, CPU/memory metrics,
   success rates, as well as KPIs, maybe the fraction of old ticker data, as well as usage of the internal data
   structure resources (how big is the HashMap cache etc.)
3. Authentication - would be needed for a real-life application, such as OAuth.
