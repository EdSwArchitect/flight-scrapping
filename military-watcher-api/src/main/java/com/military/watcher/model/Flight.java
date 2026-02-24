package com.military.watcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class Flight {
    private Long id;
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
    private Timestamp updatedAt;

    @JsonProperty("alt_baro")
    public Integer getAltBaro() { return altBaro; }
    @JsonProperty("alt_baro")
    public void setAltBaro(Integer altBaro) { this.altBaro = altBaro; }
}
