package com.military.aircraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.aircraft.model.V2ResponseAcItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.concurrent.Future;

@Slf4j
public class KafkaProducerService {

    private final Producer<String, String> producer;
    private final String topic;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(Producer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
        this.objectMapper = new ObjectMapper();
    }

    public int sendAll(List<V2ResponseAcItem> items) {
        int sent = 0;
        for (V2ResponseAcItem item : items) {
            try {
                String key = item.getHex() != null ? item.getHex() : String.valueOf(System.nanoTime());
                String value = objectMapper.writeValueAsString(item);
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
                Future<RecordMetadata> future = producer.send(record);
                future.get();
                sent++;
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize flight item: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed to send record to Kafka", e);
                throw new RuntimeException("Kafka send failed", e);
            }
        }
        return sent;
    }

    public void close() {
        producer.flush();
        producer.close();
    }
}
