package org.example.metrics.exporter.properties.mob;

public interface AppmetricaReportProperties {

    String getId();

    String getGroupBy();

    String getEventMetric();

    String getUserMetric();

    String getDimensions();

    boolean isIncludeUndefined();

    int getAccuracy();

    boolean isProposedAccuracy();
}
