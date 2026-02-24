package com.military.ingestor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class KafkaConsumerServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("military_watcher")
            .withUsername("military")
            .withPassword("military")
            .withInitScript("init.sql");

    private static final String TOPIC = "military_flights";

    @Test
    void consumeFromKafka_insertsIntoPostgres() throws Exception {
        // Produce records to Kafka
        String json1 = "{\"hex\":\"ae2902\",\"flight\":\"C6018\",\"lat\":57.57,\"lon\":-152.57}";
        String json2 = "{\"hex\":\"ae2903\",\"flight\":\"C6019\",\"lat\":58.0,\"lon\":-153.0}";

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(new ProducerRecord<>(TOPIC, "ae2902", json1)).get();
            producer.send(new ProducerRecord<>(TOPIC, "ae2903", json2)).get();
        }

        // Build consumer and services
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-ingestor");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of(TOPIC));

        FlightRecordMapper mapper = new FlightRecordMapper();
        BatchInsertService batchInsert = new BatchInsertService(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                new SimpleMeterRegistry());

        // Use batchSize=2 so we flush after 2 records
        KafkaConsumerService kafkaService = new KafkaConsumerService(consumer, mapper, batchInsert, 2);

        AtomicBoolean running = new AtomicBoolean(true);
        Thread consumerThread = new Thread(() -> {
            try {
                kafkaService.run();
            } catch (Exception e) {
                if (running.get()) {
                    throw new RuntimeException(e);
                }
            }
        });
        consumerThread.setDaemon(true);
        consumerThread.start();

        // Wait for records to be consumed and inserted (max 15 seconds)
        for (int i = 0; i < 30; i++) {
            Thread.sleep(500);
            try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM flights")) {
                if (rs.next() && rs.getInt(1) >= 2) {
                    break;
                }
            }
            if (i == 29) {
                running.set(false);
                consumer.wakeup();
                fail("Expected 2 rows in flights table within 15 seconds");
            }
        }

        running.set(false);
        consumer.wakeup();
        consumerThread.join(3000);

        // Verify inserted rows
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT hex, flight, lat, lon FROM flights ORDER BY hex")) {
            assertTrue(rs.next());
            assertEquals("ae2902", rs.getString("hex"));
            assertEquals("C6018", rs.getString("flight"));
            assertEquals(57.57, rs.getDouble("lat"), 0.001);
            assertEquals(-152.57, rs.getDouble("lon"), 0.001);
            assertTrue(rs.next());
            assertEquals("ae2903", rs.getString("hex"));
            assertEquals("C6019", rs.getString("flight"));
            assertEquals(58.0, rs.getDouble("lat"), 0.001);
            assertEquals(-153.0, rs.getDouble("lon"), 0.001);
            assertFalse(rs.next());
        }
    }

    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
}
