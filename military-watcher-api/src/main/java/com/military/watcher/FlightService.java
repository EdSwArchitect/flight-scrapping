package com.military.watcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.watcher.model.Flight;
import com.military.watcher.model.GeoboxRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FlightService {

    private final FlightRepository repository;
    private final ObjectMapper objectMapper;
    private final Counter successCounter;
    private final Counter failureCounter;

    public FlightService(FlightRepository repository, MeterRegistry registry) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.successCounter = registry.counter("api.endpoint.calls", "status", "success");
        this.failureCounter = registry.counter("api.endpoint.calls", "status", "failure");
    }

    public String listFlights(int page, int size) {
        try {
            List<Flight> flights = repository.listFlights(page, size);
            successCounter.increment();
            return objectMapper.writeValueAsString(flights);
        } catch (Exception e) {
            log.error("Failed to list flights", e);
            failureCounter.increment();
            throw new RuntimeException(e);
        }
    }

    public String getFlightById(String id) {
        try {
            Flight flight = repository.getById(id);
            if (flight == null) {
                failureCounter.increment();
                return null;
            }
            successCounter.increment();
            return objectMapper.writeValueAsString(flight);
        } catch (Exception e) {
            log.error("Failed to get flight {}", id, e);
            failureCounter.increment();
            throw new RuntimeException(e);
        }
    }

    public String listByGeobox(String body) {
        try {
            GeoboxRequest req = objectMapper.readValue(body, GeoboxRequest.class);
            if (!req.isValid()) {
                failureCounter.increment();
                return null;
            }
            List<Flight> flights = repository.listByGeobox(
                    req.getMinLat(), req.getMaxLat(),
                    req.getMinLon(), req.getMaxLon());
            successCounter.increment();
            return objectMapper.writeValueAsString(flights);
        } catch (Exception e) {
            log.error("Failed to list flights by geobox", e);
            failureCounter.increment();
            throw new RuntimeException(e);
        }
    }
}
