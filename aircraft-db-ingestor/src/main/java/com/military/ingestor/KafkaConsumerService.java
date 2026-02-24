package com.military.ingestor;

import com.military.ingestor.model.FlightRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class KafkaConsumerService {

    private final KafkaConsumer<String, String> consumer;
    private final FlightRecordMapper mapper;
    private final BatchInsertService batchInsert;
    private final int batchSize;

    public KafkaConsumerService(KafkaConsumer<String, String> consumer,
                                FlightRecordMapper mapper,
                                BatchInsertService batchInsert,
                                int batchSize) {
        this.consumer = consumer;
        this.mapper = mapper;
        this.batchInsert = batchInsert;
        this.batchSize = batchSize;
    }

    public void run() {
        log.info("Starting Kafka consumer for military_flights");
        List<FlightRecord> buffer = new ArrayList<>();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                FlightRecord fr = mapper.fromJson(record.value());
                if (fr != null) {
                    buffer.add(fr);
                }
            }
            if (buffer.size() >= batchSize) {
                flush(buffer);
                buffer.clear();
            }
        }
    }

    private void flush(List<FlightRecord> buffer) {
        if (!buffer.isEmpty()) {
            try {
                batchInsert.insertBatch(buffer);
                consumer.commitSync();
            } catch (Exception e) {
                log.error("Failed to insert batch", e);
            }
        }
    }
}
