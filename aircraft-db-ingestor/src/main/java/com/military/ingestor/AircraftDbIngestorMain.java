package com.military.ingestor;

import com.military.ingestor.model.FlightRecord;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AircraftDbIngestorMain {

    private static final Logger log = LoggerFactory.getLogger(AircraftDbIngestorMain.class);

    public static void main(String[] args) {
        var config = ConfigProvider.getConfig();
        String kafkaBootstrap = config.getOptionalValue("kafka.bootstrap.servers", String.class)
                .orElse("localhost:9092");
        String topic = config.getOptionalValue("kafka.topic", String.class)
                .orElse("military_flights");
        String dbUrl = config.getOptionalValue("db.write.url", String.class)
                .orElse("jdbc:postgresql://localhost:5432/military_watcher");
        String dbUser = config.getOptionalValue("db.write.username", String.class)
                .orElse("military");
        String dbPassword = config.getOptionalValue("db.write.password", String.class)
                .orElse("military");
        int batchSize = config.getOptionalValue("ingestor.batch.size", Integer.class)
                .orElse(100);

        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        FlightRecordMapper mapper = new FlightRecordMapper();
        BatchInsertService batchInsert = new BatchInsertService(dbUrl, dbUser, dbPassword, prometheusRegistry);

        MetricsServer metricsServer = new MetricsServer(prometheusRegistry, 8082);
        metricsServer.start();

        Properties kafkaProps = buildKafkaProperties(config, kafkaBootstrap);
        var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(kafkaProps);
        consumer.subscribe(List.of(topic));

        KafkaConsumerService kafkaService = new KafkaConsumerService(consumer, mapper, batchInsert, batchSize);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.close();
            metricsServer.stop();
        }));

        kafkaService.run();
    }

    private static Properties buildKafkaProperties(org.eclipse.microprofile.config.Config config, String bootstrap) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrap);
        props.put("group.id", "aircraft-db-ingestor");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false");
        props.put("auto.offset.reset", "earliest");

        config.getOptionalValue("kafka.sasl.mechanism", String.class).ifPresent(v ->
                props.put("sasl.mechanism", v));
        config.getOptionalValue("kafka.security.protocol", String.class).ifPresent(v ->
                props.put("security.protocol", v));
        config.getOptionalValue("kafka.sasl.jaas.config", String.class).ifPresent(v ->
                props.put("sasl.jaas.config", v));
        return props;
    }
}
