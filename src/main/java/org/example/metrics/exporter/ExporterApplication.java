package org.example.metrics.exporter;

import lombok.RequiredArgsConstructor;
import org.example.metrics.exporter.configuration.DetailedRequirementsReceiver;
import org.example.metrics.exporter.processing.mobile.MobileMetricReporter;
import org.example.metrics.exporter.processing.web.WebMetricReporter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class ExporterApplication implements CommandLineRunner {

    private final MobileMetricReporter mobileMetricReporter;
    private final WebMetricReporter webMetricReporter;
    private final DetailedRequirementsReceiver detailedRequirementsReceiver;

    public static void main(String[] args) {
        SpringApplication.run(ExporterApplication.class, args);
    }

    @Override
    public void run(String... args) {
        switch (detailedRequirementsReceiver.getExportMode()) {
            case "all": {
                webMetricReporter.formWebReport();
                mobileMetricReporter.formMobileReport();
                break;
            }
            case "web": {
                webMetricReporter.formWebReport();
                break;
            }
            case "mobile": {
                mobileMetricReporter.formMobileReport();
                break;
            }
            default:
                throw new RuntimeException("Invalid param export.mode");
        }
    }
}
