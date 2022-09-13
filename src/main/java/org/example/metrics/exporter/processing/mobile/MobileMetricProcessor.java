package org.example.metrics.exporter.processing.mobile;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.example.metrics.exporter.configuration.DetailedRequirementsReceiver.FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileMetricProcessor {

    private static final int MAX_DATE_RANGE = 7;
    private final ExecutorService executor = newFixedThreadPool(20);

    private final MobileStatsSender mobileStatsSender;
    private final IosReportProperties iosReportProperties;
    private final AndroidReportProperties androidReportProperties;
    private final DetailedRequirementsReceiver detailedRequirementsReceiver;

    public List<MetricResponseModel> sendAndProcessAndroidUser(List<String> metrics) {
        return sendAndProcess(androidReportProperties, metrics);
    }

    public List<MetricResponseModel> sendAndProcessIosUser(List<String> metrics) {
        return sendAndProcess(iosReportProperties, metrics);
    }

    @SneakyThrows
    private List<MetricResponseModel> sendAndProcess(AppmetricaReportProperties properties, List<String> metrics) {
        var resultedList = new ArrayList<CompletableFuture<MetricResponseModel>>();

        for (var metric : metrics) {
            var splited = metric.split("::");

            var alias = splited[0];
            var filter = splited[1];

            resultedList.add(CompletableFuture.supplyAsync(() -> sendAndProcessWithDateCheck(properties, filter, alias), executor));
        }

        CompletableFuture<MetricResponseModel>[] futureResultArray = resultedList.toArray(new CompletableFuture[resultedList.size()]);
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureResultArray);
        return combinedFuture
                .thenApply(v -> resultedList.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .get();
    }

    private MetricResponseModel sendAndProcessWithDateCheck(AppmetricaReportProperties properties, String filters, String alias) {
        log.info("Filter [{}] in progress...", filters);
        var result = new MetricResponseModel();

        var startDate = detailedRequirementsReceiver.getReportStartDate();
        var endDate = detailedRequirementsReceiver.getReportEndDate();

        while (!startDate.isAfter(endDate)) {
            var currentEndDate = startDate.plusDays(MAX_DATE_RANGE).isBefore(endDate)
                    ? startDate.plusDays(MAX_DATE_RANGE)
                    : endDate;

            var request = buildRequestModel(properties, startDate, currentEndDate, filters, alias);
            var apiResult = mobileStatsSender.sendRequest(request);
            appendDateResults(result, apiResult);

            startDate = currentEndDate.plusDays(1);
        }

        log.info("Filter [{}] completed", filters);
        return result;
    }

    private AppmetricaRequestModel buildRequestModel(AppmetricaReportProperties properties, LocalDate dateFrom, LocalDate dateTo, String filters, String alias) {
        return new AppmetricaRequestModel()
                .setId(properties.getId())
                .setDate1(FORMATTER.format(dateFrom))
                .setDate2(FORMATTER.format(dateTo))
                .setGroup(properties.getGroupBy())
                .setMetrics(properties.getUserMetric())
                .setIncludeUndefined(String.valueOf(properties.isIncludeUndefined()))
                .setAccuracy(String.valueOf(properties.getAccuracy()))
                .setProposedAccuracy(String.valueOf(properties.isProposedAccuracy()))
                .setFilters(filters)
                .setAlias(alias);
    }

    private void appendDateResults(MetricResponseModel resulted, MetricResponseModel apiResult) {
        resulted.setAlias(apiResult.getAlias());
        if (isEmpty(resulted.getValues())) {
            resulted.setValues(new ArrayList<>());
        }
        resulted.getValues().addAll(apiResult.getValues());
    }
}
