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
            var metricRows = detailedRequirementsReceiver.getMobileMetricRows();
            var precalculatedDates = precalcDates(detailedRequirementsReceiver.getReportStartDate(), detailedRequirementsReceiver.getReportEndDate());

            formAndroidReport(workbook, metricRows, precalculatedDates);
            formIosReport(workbook, metricRows, precalculatedDates);

            workbook.write(fileOutputStream);
            workbook.close();
        } catch (IOException e) {
            log.warn("Failed to process mobile", e);
        }
    }

    private void formAndroidReport(Workbook workbook, List<String> metricRows, List<LocalDate> dates) {
        var sheetAndroidEvents = workbook.createSheet("android events");
        var sheetAndroidUsers = workbook.createSheet("android users");

        fillExcel(sheetAndroidEvents, mobileMetricProcessor.sendAndProcessAndroidEvent(), metricRows, dates);
        fillExcel(sheetAndroidUsers, mobileMetricProcessor.sendAndProcessAndroidUser(), metricRows, dates);
    }

    private void formIosReport(Workbook workbook, List<String> metricRows, List<LocalDate> dates) {
        var sheetIosEvents = workbook.createSheet("ios events");
        var sheetIosUsers = workbook.createSheet("ios users");

        fillExcel(sheetIosEvents, mobileMetricProcessor.sendAndProcessIosEvent(), metricRows, dates);
        fillExcel(sheetIosUsers, mobileMetricProcessor.sendAndProcessIosUser(), metricRows, dates);
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
        try {
            var value = response.stream()
                    .filter(metric -> metric.getMetricName().equals(metricName))
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
