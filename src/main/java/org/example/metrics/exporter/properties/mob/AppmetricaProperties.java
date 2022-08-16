package org.example.metrics.exporter.properties.mob;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Data
@Validated
@ConstructorBinding
@ConfigurationProperties("appmetrica")
public class AppmetricaProperties {

    @NotBlank
    private final String host;

    @NotBlank
    private final String statsPath;

    @NotBlank
    private final String oAuthToken;
}
