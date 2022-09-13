package org.example.metrics.exporter.configuration;

import org.example.metrics.exporter.properties.mob.AndroidReportProperties;
import org.example.metrics.exporter.properties.mob.AppmetricaProperties;
import org.example.metrics.exporter.properties.mob.IosReportProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppmetricaProperties.class,
        AndroidReportProperties.class,
        IosReportProperties.class
})
public class ExporterConfigurationProperties {
}
