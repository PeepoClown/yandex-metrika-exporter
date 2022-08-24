package org.example.metrics.exporter.configuration;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.System.getProperty;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

@Component
public class DetailedRequirementsReceiver {

    public static final DateTimeFormatter FORMATTER = ISO_LOCAL_DATE;

    public List<String> getMobileMetricRows() {
        return ofNullable(getProperty("mobile.rows"))
                .map(property -> asList(property.split(",")))
                .orElseThrow(() -> new IllegalArgumentException("Mobile rows not specified"));
    }

    public List<String> getWebMetricRows() {
        return ofNullable(getProperty("web.rows"))
                .map(property -> asList(property.split(":")))
                .orElseThrow(() -> new IllegalArgumentException("Web rows not specified"));
    }

    public LocalDate getReportStartDate() {
        return ofNullable(getProperty("date.from"))
                .map(date -> LocalDate.parse(date, FORMATTER))
                .orElseThrow(() -> new IllegalArgumentException("Start date not specified"));
    }

    public LocalDate getReportEndDate() {
        return ofNullable(getProperty("date.to"))
                .map(date -> LocalDate.parse(date, FORMATTER))
                .orElseThrow(() -> new IllegalArgumentException("End date not specified"));
    }

    public String getExportMode() {
        return ofNullable(getProperty("export.mode"))
                .orElseThrow(() -> new IllegalArgumentException("Export mode not specified"));
    }
}
