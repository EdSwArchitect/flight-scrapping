package com.military.aircraft;

import com.military.aircraft.model.V2ResponseAcItem;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KafkaProducerServiceTest {

    private MockProducer<String, String> mockProducer;

    @BeforeEach
    void setUp() {
        mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
    }

    @Test
    void sendAll_success_sendsAllRecords() {
        KafkaProducerService service = new KafkaProducerService(mockProducer, "military_flights");

        V2ResponseAcItem item = new V2ResponseAcItem();
        item.setHex("ae2902");
        item.setFlight("C6018");
        item.setLat(57.57);
        item.setLon(-152.57);
        List<V2ResponseAcItem> items = List.of(item);

        int sent = service.sendAll(items);

        assertEquals(1, sent);
        assertEquals(1, mockProducer.history().size());
        assertEquals("ae2902", mockProducer.history().get(0).key());
        assertTrue(mockProducer.history().get(0).value().contains("ae2902"));
    }

    @Test
    void sendAll_emptyList_returnsZero() {
        KafkaProducerService service = new KafkaProducerService(mockProducer, "military_flights");

        int sent = service.sendAll(Collections.emptyList());

        assertEquals(0, sent);
        assertTrue(mockProducer.history().isEmpty());
    }

    @Test
    void sendAll_unsuccessful_throwsException() {
        mockProducer.sendException = new RuntimeException("Kafka broker unavailable");
        KafkaProducerService service = new KafkaProducerService(mockProducer, "military_flights");

        V2ResponseAcItem item = new V2ResponseAcItem();
        item.setHex("ae2902");
        List<V2ResponseAcItem> items = List.of(item);

        assertThrows(RuntimeException.class, () -> service.sendAll(items));
    }
}
