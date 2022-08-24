package org.example.metrics.exporter.configuration;

import org.example.metrics.exporter.properties.mob.AndroidReportProperties;
import org.example.metrics.exporter.properties.mob.AppmetricaProperties;
import org.example.metrics.exporter.properties.mob.IosReportProperties;
import org.example.metrics.exporter.properties.web.WebReportProperties;
import org.example.metrics.exporter.properties.web.YandexMetrikaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppmetricaProperties.class,
        AndroidReportProperties.class,
        IosReportProperties.class,
        YandexMetrikaProperties.class,
        WebReportProperties.class
})
public class ExporterConfigurationProperties {
}
