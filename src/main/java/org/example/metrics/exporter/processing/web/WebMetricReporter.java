package org.example.metrics.exporter.processing.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.metrics.exporter.configuration.DetailedRequirementsReceiver;
import org.example.metrics.exporter.model.MetricResponseModel;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.joining;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebMetricReporter {

    private static final DateTimeFormatter EXCEL_FORMATTER = ofPattern("dd.MM.yyyy");

    private final WebMetricProcessor webMetricProcessor;
    private final DetailedRequirementsReceiver detailedRequirementsReceiver;

    public void formWebReport() {
        try (var fileOutputStream = new FileOutputStream("metrics-report-web.xlsx")) {
            var workbook = new XSSFWorkbook();
            var metricRows = detailedRequirementsReceiver.getWebMetricRows();
            var precalculatedDates = precalcDates(detailedRequirementsReceiver.getReportStartDate(), detailedRequirementsReceiver.getReportEndDate());

            var sheetWebEvents = workbook.createSheet("web events");
            var sheetWebUsers = workbook.createSheet("web users");

            fillExcel(sheetWebEvents, webMetricProcessor.senaAndProcessWebEvent(), metricRows, precalculatedDates);
            fillExcel(sheetWebUsers, webMetricProcessor.senaAndProcessWebUser(), metricRows, precalculatedDates);

            workbook.write(fileOutputStream);
            workbook.close();
        } catch (IOException e) {
            log.warn("Failed to process web", e);
        }
    }

    private void fillExcel(Sheet sheet, List<MetricResponseModel> response, List<String> metricRows, List<LocalDate> dates) {
        int rowPos = 0;
        var headerRow = sheet.createRow(rowPos);
        rowPos++;

        int cellPos = 1;
        for (int i = 0; i < metricRows.size(); i++, cellPos++) {
            headerRow.createCell(cellPos).setCellValue(metricRows.get(cellPos - 1));
        }

        for (int i = 0; i < dates.size(); i++, rowPos++) {
            cellPos = 0;
            var row = sheet.createRow(rowPos);

            var cell = row.createCell(cellPos);
            cell.setCellValue(dates.get(i).format(EXCEL_FORMATTER));
            cellPos++;

            for (int j = 0; j < metricRows.size(); j++, cellPos++) {
                var metricName = metricRows.get(j);
                var value = currCellValue(response, metricName, i);
                cell = row.createCell(cellPos);
                cell.setCellValue(value);
            }
        }
    }

    private String currCellValue(List<MetricResponseModel> response, String metricName, int pos) {
        var value = response.stream()
                .filter(metric -> Arrays.stream(metric.getMetricName().split(" -> ")).collect(joining(",")).equals(metricName))
                .findFirst()
                .get()
                .getValues()
                .get(pos);
        return value.contains(".") ? value.substring(0, value.indexOf(".")) : value;
    }

    private List<LocalDate> precalcDates(LocalDate startDate, LocalDate endDate) {
        var list = new ArrayList<LocalDate>();

        var tmpDate = startDate;
        while (!tmpDate.isAfter(endDate)) {
            list.add(tmpDate);
            tmpDate = tmpDate.plusDays(1);
        }

        return list;
    }
}
