package org.example.metrics.exporter.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MetricResponseModel {
    private String metricName;
    private List<String> values;
}
