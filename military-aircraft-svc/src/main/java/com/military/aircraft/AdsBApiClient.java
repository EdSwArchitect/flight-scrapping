package com.military.aircraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.aircraft.model.V2Response;
import com.military.aircraft.model.V2ResponseAcItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AdsBApiClient {

    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Counter callsTotal;
    private final Counter errorsTotal;
    private final Counter successTotal;
    private final Timer callDuration;
    private final DistributionSummary itemsCount;
    private final DistributionSummary responseSize;
    private final Timer parseDurationPerItem;

    public AdsBApiClient(String apiUrl, MeterRegistry registry) {
        this(apiUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), registry);
    }

    public AdsBApiClient(String apiUrl, HttpClient httpClient, MeterRegistry registry) {
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.callsTotal = registry.counter("adsb.api.calls.total");
        this.errorsTotal = registry.counter("adsb.api.errors.total");
        this.successTotal = registry.counter("adsb.api.success.total");
        this.callDuration = registry.timer("adsb.api.call.duration");
        this.itemsCount = registry.summary("adsb.api.items.count");
        this.responseSize = registry.summary("adsb.api.response.size");
        this.parseDurationPerItem = registry.timer("adsb.api.parse.duration.per.item");
    }

    public List<V2ResponseAcItem> fetchMilitaryFlights() {
        callsTotal.increment();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            long start = System.nanoTime();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            callDuration.record(Duration.ofNanos(System.nanoTime() - start));

            responseSize.record(response.body().length());

            if (response.statusCode() != 200) {
                log.error("ADS-B API returned status {}", response.statusCode());
                errorsTotal.increment();
                return Collections.emptyList();
            }

            long parseStart = System.nanoTime();
            V2Response v2Response = objectMapper.readValue(response.body(), V2Response.class);
            List<V2ResponseAcItem> items = v2Response.getAc() != null ? v2Response.getAc() : Collections.emptyList();
            int count = items.size();
            if (count > 0) {
                parseDurationPerItem.record(Duration.ofNanos(System.nanoTime() - parseStart).dividedBy(count));
            }
            itemsCount.record(count);

            successTotal.increment();
            log.info("Fetched {} military flights from ADS-B API", count);
            return items;
        } catch (Exception e) {
            log.error("Failed to fetch from ADS-B API", e);
            errorsTotal.increment();
            return Collections.emptyList();
        }
    }
}
