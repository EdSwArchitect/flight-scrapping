package com.military.aircraft;

import com.military.aircraft.model.V2ResponseAcItem;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MilitaryAircraftSvcMain {

    private static final Logger log = LoggerFactory.getLogger(MilitaryAircraftSvcMain.class);

    public static void main(String[] args) {
        var config = ConfigProvider.getConfig();
        String apiUrl = config.getOptionalValue("adsb.api.url", String.class)
                .orElse("https://api.adsb.lol/v2/mil");
        String kafkaBootstrap = config.getOptionalValue("kafka.bootstrap.servers", String.class)
                .orElse("localhost:9092");
        String topic = config.getOptionalValue("kafka.topic", String.class)
                .orElse("military_flights");
        String scheduleCron = config.getOptionalValue("schedule.cron", String.class).orElse(null);
        boolean runOnce = config.getOptionalValue("run.once", Boolean.class).orElse(true);

        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        AdsBApiClient apiClient = new AdsBApiClient(apiUrl, prometheusRegistry);

        Properties kafkaProps = buildKafkaProperties(config, kafkaBootstrap);
        var producer = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(kafkaProps);
        KafkaProducerService kafkaService = new KafkaProducerService(producer, topic);

        MetricsServer metricsServer = new MetricsServer(prometheusRegistry, 8081);
        metricsServer.start();

        Runnable fetchAndSend = () -> {
            try {
                List<V2ResponseAcItem> items = apiClient.fetchMilitaryFlights();
                if (!items.isEmpty()) {
                    int sent = kafkaService.sendAll(items);
                    log.info("Sent {} records to Kafka topic {}", sent, topic);
                }
            } catch (Exception e) {
                log.error("Error in fetch-and-send cycle", e);
            }
        };

        if (runOnce) {
            log.info("Running once (CronJob mode)");
            fetchAndSend.run();
            kafkaService.close();
            metricsServer.stop();
            System.exit(0);
        } else if (scheduleCron != null) {
            log.info("Running on schedule: {}", scheduleCron);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            long initialDelay = 0;
            long period = parseCronToSeconds(scheduleCron);
            scheduler.scheduleAtFixedRate(fetchAndSend, initialDelay, period, TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                scheduler.shutdown();
                kafkaService.close();
                metricsServer.stop();
            }));
        } else {
            log.error("Either run.once=true or schedule.cron must be set");
            System.exit(1);
        }
    }

    private static Properties buildKafkaProperties(org.eclipse.microprofile.config.Config config, String bootstrap) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrap);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "1");

        config.getOptionalValue("kafka.sasl.mechanism", String.class).ifPresent(v ->
                props.put("sasl.mechanism", v));
        config.getOptionalValue("kafka.security.protocol", String.class).ifPresent(v ->
                props.put("security.protocol", v));
        config.getOptionalValue("kafka.sasl.jaas.config", String.class).ifPresent(v ->
                props.put("sasl.jaas.config", v));
        return props;
    }

    private static long parseCronToSeconds(String cron) {
        if (cron == null || cron.isBlank()) return 300;
        String[] parts = cron.trim().split("\\s+");
        if (parts.length >= 1) {
            String minutes = parts[0];
            if (minutes.startsWith("*/")) {
                return Integer.parseInt(minutes.substring(2)) * 60L;
            }
        }
        return 300;
    }
}
