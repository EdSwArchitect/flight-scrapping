package com.military.aircraft.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class V2ResponseAcItem {
    private String hex;
    private String type;
    private String flight;
    private String r;
    private String t;
    private Integer dbFlags;
    private Object altBaro;
    private Double altGeom;
    private Double gs;
    private Double track;
    private Double baroRate;
    private Double geomRate;
    private String squawk;
    private String emergency;
    private String category;
    private Double lat;
    private Double lon;
    private Integer nic;
    private Integer rc;
    private Double seenPos;
    private Integer version;
    private Integer nicBaro;
    private Integer nacP;
    private Integer nacV;
    private Integer sil;
    private String silType;
    private Integer gva;
    private Integer sda;
    private Integer alert;
    private Integer spi;
    private List<String> mlat;
    private List<String> tisb;
    private Integer messages;
    private Double seen;
    private Double rssi;

    @JsonProperty("alt_baro")
    public void setAltBaro(Object altBaro) {
        this.altBaro = altBaro;
    }

    @JsonProperty("alt_geom")
    public void setAltGeom(Double altGeom) {
        this.altGeom = altGeom;
    }

    @JsonProperty("baro_rate")
    public void setBaroRate(Double baroRate) {
        this.baroRate = baroRate;
    }

    @JsonProperty("geom_rate")
    public void setGeomRate(Double geomRate) {
        this.geomRate = geomRate;
    }

    @JsonProperty("seen_pos")
    public void setSeenPos(Double seenPos) {
        this.seenPos = seenPos;
    }

    @JsonProperty("nic_baro")
    public void setNicBaro(Integer nicBaro) {
        this.nicBaro = nicBaro;
    }

    @JsonProperty("nac_p")
    public void setNacP(Integer nacP) {
        this.nacP = nacP;
    }

    @JsonProperty("nac_v")
    public void setNacV(Integer nacV) {
        this.nacV = nacV;
    }

    @JsonProperty("sil_type")
    public void setSilType(String silType) {
        this.silType = silType;
    }
}
