package com.military.watcher;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.common.QueryParams;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MilitaryWatcherApiMain {

    private static final Logger log = LoggerFactory.getLogger(MilitaryWatcherApiMain.class);

    public static void main(String[] args) {
        var config = ConfigProvider.getConfig();
        String dbUrl = config.getOptionalValue("db.read.url", String.class)
                .orElse("jdbc:postgresql://localhost:5432/military_watcher");
        String dbUser = config.getOptionalValue("db.read.username", String.class)
                .orElse("military");
        String dbPassword = config.getOptionalValue("db.read.password", String.class)
                .orElse("military");
        int port = config.getOptionalValue("server.port", Integer.class)
                .orElse(8080);

        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        FlightRepository repository = new FlightRepository(dbUrl, dbUser, dbPassword);
        FlightService flightService = new FlightService(repository, prometheusRegistry);

        ServerBuilder sb = Server.builder();
        sb.http(port);

        sb.service("/list-flights", (ctx, req) -> {
            QueryParams params = QueryParams.fromQueryString(req.uri().getQuery() != null ? req.uri().getQuery() : "");
            int page = params.get("page") != null ? Integer.parseInt(params.get("page")) : 0;
            int size = params.get("size") != null ? Math.min(100, Integer.parseInt(params.get("size"))) : 50;
            try {
                String json = flightService.listFlights(page, size);
                return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, json);
            } catch (Exception e) {
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
            }
        });

        sb.service("/list-flights/{id}", (ctx, req) -> {
            String id = ctx.pathParam("id");
            try {
                String json = flightService.getFlightById(id);
                if (json == null) {
                    return HttpResponse.of(HttpStatus.NOT_FOUND);
                }
                return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, json);
            } catch (Exception e) {
                return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
            }
        });

        sb.service("/geobox-list-flight", (ctx, req) -> {
            if (!"POST".equals(req.method().name())) {
                return HttpResponse.of(HttpStatus.METHOD_NOT_ALLOWED);
            }
            return HttpResponse.from(req.aggregate().thenApply(agg -> {
                String body = agg.contentUtf8();
                try {
                    String json = flightService.listByGeobox(body);
                    if (json == null) {
                        return HttpResponse.of(HttpStatus.BAD_REQUEST, MediaType.PLAIN_TEXT_UTF_8, "Invalid geobox request");
                    }
                    return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, json);
                } catch (Exception e) {
                    return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, e.getMessage());
                }
            }));
        });

        sb.service("/metrics", (ctx, req) ->
                HttpResponse.of(HttpStatus.OK, MediaType.parse("text/plain"), prometheusRegistry.scrape()));

        Server server = sb.build();
        server.start().join();
        log.info("Military Watcher API started on port {}", port);
    }
}
