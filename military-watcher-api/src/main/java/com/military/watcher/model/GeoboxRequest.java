package com.military.watcher.model;

import lombok.Data;

@Data
public class GeoboxRequest {
    private Double minLat;
    private Double maxLat;
    private Double minLon;
    private Double maxLon;

    public boolean isValid() {
        return minLat != null && maxLat != null && minLon != null && maxLon != null
                && minLat <= maxLat && minLon <= maxLon;
    }
}
