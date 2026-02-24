package com.military.watcher;

import com.military.watcher.model.Flight;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FlightServiceTest {

    @Mock
    private FlightRepository repository;

    private FlightService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FlightService(repository, new SimpleMeterRegistry());
    }

    @Test
    void listFlights_success_returnsJson() throws Exception {
        when(repository.listFlights(0, 50)).thenReturn(List.of(
                Flight.builder().hex("ae2902").flight("C6018").build()
        ));
        String json = service.listFlights(0, 50);
        assertNotNull(json);
        assertTrue(json.contains("ae2902"));
        assertTrue(json.contains("C6018"));
    }

    @Test
    void getFlightById_success_returnsJson() throws Exception {
        when(repository.getById("ae2902")).thenReturn(
                Flight.builder().hex("ae2902").flight("C6018").build()
        );
        String json = service.getFlightById("ae2902");
        assertNotNull(json);
        assertTrue(json.contains("ae2902"));
    }

    @Test
    void getFlightById_notFound_returnsNull() {
        when(repository.getById("nonexistent")).thenReturn(null);
        String json = service.getFlightById("nonexistent");
        assertNull(json);
    }

    @Test
    void listByGeobox_success_returnsJson() throws Exception {
        when(repository.listByGeobox(30.0, 40.0, -120.0, -100.0))
                .thenReturn(List.of(Flight.builder().hex("ae2902").lat(35.0).lon(-110.0).build()));
        String body = "{\"minLat\":30,\"maxLat\":40,\"minLon\":-120,\"maxLon\":-100}";
        String json = service.listByGeobox(body);
        assertNotNull(json);
        assertTrue(json.contains("ae2902"));
    }

    @Test
    void listByGeobox_invalidRequest_returnsNull() {
        String body = "{\"minLat\":30}";
        String json = service.listByGeobox(body);
        assertNull(json);
    }
}
