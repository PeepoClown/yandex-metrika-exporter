package org.example.metrics.exporter.processing;

import lombok.RequiredArgsConstructor;
import org.example.metrics.exporter.configuration.DetailedRequirementsReceiver;
import org.example.metrics.exporter.model.MetricResponseModel;
import org.example.metrics.exporter.model.mob.AppmetricaRequestModel;
import org.example.metrics.exporter.properties.mob.AndroidReportProperties;
import org.example.metrics.exporter.properties.mob.AppmetricaReportProperties;
import org.example.metrics.exporter.properties.mob.IosReportProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.example.metrics.exporter.configuration.DetailedRequirementsReceiver.FORMATTER;

@Service
@RequiredArgsConstructor
public class MobileMetricProcessor {

    private static final int MAX_DATE_RANGE = 5;
    private static final int MAX_METRICS_COUNT = 20;

    private final MobileStatsSender mobileStatsSender;
    private final IosReportProperties iosReportProperties;
    private final AndroidReportProperties androidReportProperties;
    private final DetailedRequirementsReceiver detailedRequirementsReceiver;

    public List<MetricResponseModel> sendAndProcessAndroidEvent() {
        return sendAndProcess(androidReportProperties, true);
    }

    public List<MetricResponseModel> sendAndProcessAndroidUser() {
        return sendAndProcess(androidReportProperties, false);
    }

    public List<MetricResponseModel> sendAndProcessIosEvent() {
        return sendAndProcess(iosReportProperties, true);
    }

    public List<MetricResponseModel> sendAndProcessIosUser() {
        return sendAndProcess(iosReportProperties, false);
    }

    private List<MetricResponseModel> sendAndProcess(AppmetricaReportProperties properties, boolean isEvent) {
        var resultedList = new ArrayList<MetricResponseModel>();

        int metricsCounter = 0;
        var metricRows = detailedRequirementsReceiver.getMobileMetricRows();
        while (metricsCounter < metricRows.size()) {
            int metricsCounterEnd = Math.min(metricsCounter + MAX_METRICS_COUNT, metricRows.size());

            var sublist = metricRows.subList(metricsCounter, metricsCounterEnd);
            resultedList.addAll(sendAndProcessWithDateCheck(properties, isEvent, sublist));

            metricsCounter = metricsCounterEnd;
        }

        return resultedList;
    }

    private List<MetricResponseModel> sendAndProcessWithDateCheck(AppmetricaReportProperties properties, boolean isEvent, List<String> rows) {
        var resultedList = new ArrayList<MetricResponseModel>();

        var startDate = detailedRequirementsReceiver.getReportStartDate();
        var endDate = detailedRequirementsReceiver.getReportEndDate();

        while (startDate.isBefore(endDate)) {
            var currentEndDate = startDate.plusDays(MAX_DATE_RANGE).isBefore(endDate)
                    ? startDate.plusDays(MAX_DATE_RANGE)
                    : endDate;

            var request = buildRequestModel(properties, isEvent, startDate, currentEndDate, rows);
            var apiResult = mobileStatsSender.sendRequest(request);
            appendDateResults(resultedList, apiResult);

            startDate = currentEndDate.plusDays(1);
        }

        return resultedList;
    }

    private AppmetricaRequestModel buildRequestModel(AppmetricaReportProperties properties, boolean isEvent, LocalDate dateFrom,
                                                     LocalDate dateTo, List<String> rows) {
        var formattedRows = "[" + rows.stream().map(s -> "[\"" + s + "\"]").collect(Collectors.joining(",")) + "]";
        return new AppmetricaRequestModel()
                .setId(properties.getId())
                .setDate1(FORMATTER.format(dateFrom))
                .setDate2(FORMATTER.format(dateTo))
                .setGroup(properties.getGroupBy())
                .setMetrics(isEvent ? properties.getEventMetric() : properties.getUserMetric())
                .setDimensions(properties.getDimensions())
                .setIncludeUndefined(String.valueOf(properties.isIncludeUndefined()))
                .setAccuracy(String.valueOf(properties.getAccuracy()))
                .setProposedAccuracy(String.valueOf(properties.isProposedAccuracy()))
                .setRows(formattedRows);
    }

    private void appendDateResults(List<MetricResponseModel> resultedList, List<MetricResponseModel> apiResult) {
        if (resultedList.isEmpty()) {
            resultedList.addAll(apiResult);
            return;
        }

        for (var i = new AtomicInteger(0); i.get() < apiResult.size(); i.incrementAndGet()) {
            var resultedElem = resultedList.stream()
                    .filter(metric -> metric.getMetricName().equals(apiResult.get(i.get()).getMetricName()))
                    .findFirst()
                    .get();
            resultedElem.getValues()
                    .addAll(apiResult.get(i.get()).getValues());
        }
    }
}
