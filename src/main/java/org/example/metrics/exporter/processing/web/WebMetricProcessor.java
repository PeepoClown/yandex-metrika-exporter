package org.example.metrics.exporter.processing.web;

import lombok.RequiredArgsConstructor;
import org.example.metrics.exporter.configuration.DetailedRequirementsReceiver;
import org.example.metrics.exporter.model.MetricResponseModel;
import org.example.metrics.exporter.model.mob.AppmetricaRequestModel;
import org.example.metrics.exporter.properties.web.WebReportProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static org.example.metrics.exporter.configuration.DetailedRequirementsReceiver.FORMATTER;

@Service
@RequiredArgsConstructor
public class WebMetricProcessor {

    private static final int MAX_DATE_RANGE = 2;
    private static final int MAX_METRICS_COUNT = 10;

    private final WebStatsSender webStatsSender;
    private final WebReportProperties webReportProperties;
    private final DetailedRequirementsReceiver detailedRequirementsReceiver;

    public List<MetricResponseModel> senaAndProcessWebEvent() {
        return sendAndProcess(true);
    }

    public List<MetricResponseModel> senaAndProcessWebUser() {
        return sendAndProcess(false);
    }

    private List<MetricResponseModel> sendAndProcess(boolean isEvent) {
        var resultedList = new ArrayList<MetricResponseModel>();

        int metricsCounter = 0;
        var metricRows = detailedRequirementsReceiver.getWebMetricRows();
        while (metricsCounter < metricRows.size()) {
            int metricsCounterEnd = Math.min(metricsCounter + MAX_METRICS_COUNT, metricRows.size());

            var sublist = metricRows.subList(metricsCounter, metricsCounterEnd);
            resultedList.addAll(sendAndProcessWithDateCheck(isEvent, sublist));

            metricsCounter = metricsCounterEnd;
        }

        return resultedList;
    }

    private List<MetricResponseModel> sendAndProcessWithDateCheck(boolean isEvent, List<String> rows) {
        var resultedList = new ArrayList<MetricResponseModel>();

        var startDate = detailedRequirementsReceiver.getReportStartDate();
        var endDate = detailedRequirementsReceiver.getReportEndDate();

        while (!startDate.isAfter(endDate)) {
            var currentEndDate = startDate.plusDays(MAX_DATE_RANGE).isBefore(endDate)
                    ? startDate.plusDays(MAX_DATE_RANGE)
                    : endDate;

            var request = buildRequestModel(isEvent, startDate, currentEndDate, rows);
            var apiResult = webStatsSender.sendRequest(request);
            appendDateResults(resultedList, apiResult);

            startDate = currentEndDate.plusDays(1);
        }

        return resultedList;
    }

    private AppmetricaRequestModel buildRequestModel(boolean isEvent, LocalDate dateFrom, LocalDate dateTo, List<String> rows) {
        var formattedRows = "[" + rows.stream()
                .map(s -> Arrays.stream(s.split(","))
                        .map(ss -> "\"" + ss + "\"")
                        .collect(Collectors.joining(",")))
                .map(s -> "[" + s + "]")
                .collect(Collectors.joining(",")) + "]";
        return new AppmetricaRequestModel()
                .setId(webReportProperties.getId())
                .setDate1(FORMATTER.format(dateFrom))
                .setDate2(FORMATTER.format(dateTo))
                .setGroup(webReportProperties.getGroupBy())
                .setMetrics(isEvent ? webReportProperties.getEventMetric() : webReportProperties.getUserMetric())
                .setDimensions(webReportProperties.getDimensions())
                .setIncludeUndefined(String.valueOf(webReportProperties.isIncludeUndefined()))
                .setAccuracy(String.valueOf(webReportProperties.getAccuracy()))
                .setProposedAccuracy(String.valueOf(webReportProperties.isProposedAccuracy()))
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
