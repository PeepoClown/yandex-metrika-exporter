package org.example.metrics.exporter.model.mob;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AppmetricaRequestModel {
    private String id;
    private String date1;
    private String date2;
    private String group;
    private String metrics;
    private String includeUndefined;
    private String accuracy;
    private String proposedAccuracy;
    private String rows;
    private String filters;
    private String alias;
}
