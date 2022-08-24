package org.example.metrics.exporter.properties.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Data
@Validated
@ConstructorBinding
@ConfigurationProperties("web")
public class WebReportProperties {

    @NotBlank
    private final String id;

    private final String groupBy = "day";

    @NotBlank
    private final String eventMetric;

    @NotBlank
    private final String userMetric;

    @NotBlank
    private final String dimensions;

    private final boolean includeUndefined = true;

    private final int accuracy = 1;

    private final boolean proposedAccuracy = true;
}
