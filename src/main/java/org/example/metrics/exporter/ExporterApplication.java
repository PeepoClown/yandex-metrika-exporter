package org.example.metrics.exporter;

import org.example.metrics.exporter.processing.MobileMetricReporter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExporterApplication implements CommandLineRunner {

    private final MobileMetricReporter mobileMetricReporter;

    public ExporterApplication(MobileMetricReporter mobileMetricReporter) {
        this.mobileMetricReporter = mobileMetricReporter;
    }

    public static void main(String[] args) {
        SpringApplication.run(ExporterApplication.class, args);
    }

    @Override
    public void run(String... args) {
        mobileMetricReporter.formMobileReport();
    }
}
