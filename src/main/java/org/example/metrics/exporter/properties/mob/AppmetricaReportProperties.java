package org.example.metrics.exporter.properties.mob;

public interface AppmetricaReportProperties {

    String getId();

    String getGroupBy();

    String getUserMetric();

    boolean isIncludeUndefined();

    int getAccuracy();

    boolean isProposedAccuracy();
}
