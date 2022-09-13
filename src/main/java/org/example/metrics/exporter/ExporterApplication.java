package org.example.metrics.exporter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.metrics.exporter.processing.mobile.MobileMetricReporter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class ExporterApplication implements CommandLineRunner {

    private final ApplicationContext applicationContext;
    private final MobileMetricReporter mobileMetricReporter;

    public static void main(String[] args) {
        SpringApplication.run(ExporterApplication.class, args);
    }

    @Override
    public void run(String... args) {
        mobileMetricReporter.formMobileReport();
        log.info("All works done");
        SpringApplication.exit(applicationContext, () -> 0);
    }
}
