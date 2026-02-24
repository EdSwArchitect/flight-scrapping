package com.military.ingestor;

import com.military.ingestor.model.FlightRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightRecordMapperTest {

    private final FlightRecordMapper mapper = new FlightRecordMapper();

    @Test
    void fromJson_valid_returnsFlightRecord() {
        String json = """
            {"hex":"ae2902","flight":"C6018","r":"6018","t":"H60","lat":57.57,"lon":-152.57,"alt_baro":3100,"gs":132.2,"track":192.67,"seen":0.2}
            """;
        FlightRecord r = mapper.fromJson(json);
        assertNotNull(r);
        assertEquals("ae2902", r.getHex());
        assertEquals("C6018", r.getFlight());
        assertEquals("6018", r.getR());
        assertEquals("H60", r.getT());
        assertEquals(57.57, r.getLat());
        assertEquals(-152.57, r.getLon());
        assertEquals(3100, r.getAltBaro());
        assertEquals(132.2, r.getGs());
        assertEquals(192.67, r.getTrack());
        assertNotNull(r.getRaw());
    }

    @Test
    void fromJson_missingHex_returnsNull() {
        String json = "{\"flight\":\"C6018\",\"lat\":57.57}";
        FlightRecord r = mapper.fromJson(json);
        assertNull(r);
    }

    @Test
    void fromJson_altBaroGround_handlesGracefully() {
        String json = "{\"hex\":\"ae2902\",\"alt_baro\":\"ground\",\"lat\":57.57,\"lon\":-152.57}";
        FlightRecord r = mapper.fromJson(json);
        assertNotNull(r);
        assertNull(r.getAltBaro());
    }
}
