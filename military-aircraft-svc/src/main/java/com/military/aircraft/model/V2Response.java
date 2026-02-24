package com.military.aircraft.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class V2Response {
    private List<V2ResponseAcItem> ac;
}
