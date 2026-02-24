package com.military.aircraft;

import com.military.aircraft.model.V2ResponseAcItem;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class KafkaProducerServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    private static final String TOPIC = "military_flights";

    private KafkaProducerService service;
    private KafkaProducer<String, String> producer;

    @BeforeEach
    void setUp() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(producerProps);
        service = new KafkaProducerService(producer, TOPIC);
    }

    @Test
    void sendAll_producesToRealKafka_consumerReceivesMessages() {
        V2ResponseAcItem item1 = new V2ResponseAcItem();
        item1.setHex("ae2902");
        item1.setFlight("C6018");
        item1.setLat(57.57);
        item1.setLon(-152.57);

        V2ResponseAcItem item2 = new V2ResponseAcItem();
        item2.setHex("ae2903");
        item2.setFlight("C6019");
        item2.setLat(58.0);
        item2.setLon(-153.0);

        int sent = service.sendAll(List.of(item1, item2));

        assertEquals(2, sent);
        service.close();

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertEquals(2, records.count());

            List<ConsumerRecord<String, String>> recordList = new ArrayList<>();
            records.records(TOPIC).forEach(recordList::add);
            assertTrue(recordList.stream().anyMatch(r -> "ae2902".equals(r.key()) && r.value().contains("ae2902")));
            assertTrue(recordList.stream().anyMatch(r -> "ae2903".equals(r.key()) && r.value().contains("ae2903")));
        }
    }
}
