package com.military.ingestor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.ingestor.model.FlightRecord;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.Instant;

@Slf4j
public class FlightRecordMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlightRecord fromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String hex = getText(node, "hex");
            if (hex == null || hex.isBlank()) {
                return null;
            }

            Integer altBaro = null;
            JsonNode altBaroNode = node.get("alt_baro");
            if (altBaroNode != null && !altBaroNode.isNull()) {
                if (altBaroNode.isNumber()) {
                    altBaro = altBaroNode.asInt();
                } else if (altBaroNode.isTextual() && !"ground".equalsIgnoreCase(altBaroNode.asText())) {
                    try {
                        altBaro = Integer.parseInt(altBaroNode.asText());
                    } catch (NumberFormatException ignored) {}
                }
            }

            Timestamp seenTs = null;
            JsonNode seenNode = node.get("seen");
            if (seenNode != null && seenNode.isNumber()) {
                double seen = seenNode.asDouble();
                seenTs = Timestamp.from(Instant.now().minusSeconds((long) seen));
            }

            return FlightRecord.builder()
                    .hex(hex)
                    .flight(getText(node, "flight"))
                    .r(getText(node, "r"))
                    .t(getText(node, "t"))
                    .lat(getDouble(node, "lat"))
                    .lon(getDouble(node, "lon"))
                    .altBaro(altBaro)
                    .gs(getDouble(node, "gs"))
                    .track(getDouble(node, "track"))
                    .seen(seenTs)
                    .raw(json)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse flight record: {}", e.getMessage());
            return null;
        }
    }

    private String getText(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n != null && !n.isNull() ? n.asText() : null;
    }

    private Double getDouble(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n != null && n.isNumber() ? n.asDouble() : null;
    }
}
