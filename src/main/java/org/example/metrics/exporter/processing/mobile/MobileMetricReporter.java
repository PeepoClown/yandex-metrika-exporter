package org.example.metrics.exporter.processing.mobile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.metrics.exporter.configuration.DetailedRequirementsReceiver;
import org.example.metrics.exporter.model.MetricResponseModel;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileMetricReporter {

    private static final DateTimeFormatter EXCEL_FORMATTER = ofPattern("dd.MM.yyyy");

    private final MobileMetricProcessor mobileMetricProcessor;
    private final DetailedRequirementsReceiver detailedRequirementsReceiver;

    public void formMobileReport() {
        try (var fileOutputStream = new FileOutputStream("metrics-report-mob.xlsx")) {
            var workbook = new XSSFWorkbook();

            var androidFilters = detailedRequirementsReceiver.getAndroidFilters();
            var iosFilters = detailedRequirementsReceiver.getIosFilters();

            var precalculatedDates = precalcDates(detailedRequirementsReceiver.getReportStartDate(), detailedRequirementsReceiver.getReportEndDate());

            formAndroidReport(workbook, androidFilters, precalculatedDates);
            formIosReport(workbook, iosFilters, precalculatedDates);

            workbook.write(fileOutputStream);
            workbook.close();
        } catch (IOException e) {
            log.warn("Failed to process mobile metrics", e);
        }
    }

    private void formAndroidReport(Workbook workbook, List<String> androidFilters, List<LocalDate> dates) {
        var sheetAndroidUsers = workbook.createSheet("android users");

        fillExcel(sheetAndroidUsers, mobileMetricProcessor.sendAndProcessAndroidUser(androidFilters), dates);
    }

    private void formIosReport(Workbook workbook, List<String> iosFilters, List<LocalDate> dates) {
        var sheetIosUsers = workbook.createSheet("ios users");

        fillExcel(sheetIosUsers, mobileMetricProcessor.sendAndProcessIosUser(iosFilters), dates);
    }

    private void fillExcel(Sheet sheet, List<MetricResponseModel> response, List<LocalDate> dates) {
        int rowPos = 0;
        var headerRow = sheet.createRow(rowPos);
        rowPos++;

        var aliases = response.stream().map(MetricResponseModel::getAlias).collect(Collectors.toList());

        int cellPos = 1;
        for (int i = 0; i < aliases.size(); i++, cellPos++) {
            headerRow.createCell(cellPos).setCellValue(aliases.get(cellPos - 1));
        }

        for (int i = 0; i < dates.size(); i++, rowPos++) {
            cellPos = 0;
            var row = sheet.createRow(rowPos);

            var cell = row.createCell(cellPos);
            cell.setCellValue(dates.get(i).format(EXCEL_FORMATTER));
            cellPos++;

            for (int j = 0; j < aliases.size(); j++, cellPos++) {
                var metricName = aliases.get(j);
                var value = currCellValue(response, metricName, i);
                cell = row.createCell(cellPos);
                cell.setCellValue(value);
            }
        }
    }

    private String currCellValue(List<MetricResponseModel> response, String metricName, int pos) {
        try {
            var value = response.stream()
                    .filter(metric -> metric.getAlias().equals(metricName))
                    .findFirst()
                    .get()
                    .getValues()
                    .get(pos);
            return value.contains(".") ? value.substring(0, value.indexOf(".")) : value;
        } catch (Exception e) {
            return "0";
        }
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
