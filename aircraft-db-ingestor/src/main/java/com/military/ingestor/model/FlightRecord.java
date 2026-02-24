package com.military.ingestor.model;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class FlightRecord {
    private String hex;
    private String flight;
    private String r;
    private String t;
    private Double lat;
    private Double lon;
    private Integer altBaro;
    private Double gs;
    private Double track;
    private Timestamp seen;
    private String raw;
}
