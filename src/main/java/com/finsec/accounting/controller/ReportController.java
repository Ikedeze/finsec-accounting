package com.finsec.accounting.controller;

import com.finsec.accounting.service.ExcelReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ExcelReportService excelReportService;

    public ReportController(ExcelReportService excelReportService) {
        this.excelReportService = excelReportService;
    }

    @GetMapping("/excel/ledger")
    public ResponseEntity<byte[]> exportLedgerExcel(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        byte[] excelData = excelReportService.generateTransactionExcel(orgId, startDate, endDate);
        return buildExcelResponse(excelData, "Ledger_Report", startDate, endDate);
    }

    @GetMapping("/excel/monthly")
    public ResponseEntity<byte[]> exportMonthlySummaryExcel(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        byte[] excelData = excelReportService.generateMonthlySummaryExcel(orgId, startDate, endDate);
        return buildExcelResponse(excelData, "Monthly_Summary", startDate, endDate);
    }

    @GetMapping("/excel/category")
    public ResponseEntity<byte[]> exportCategoryBreakdownExcel(
            @RequestHeader("X-Organization-Id") String orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        byte[] excelData = excelReportService.generateCategoryBreakdownExcel(orgId, startDate, endDate);
        return buildExcelResponse(excelData, "Category_Breakdown", startDate, endDate);
    }

    // Helper method to format the HTTP response for file downloads
    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String reportName, LocalDate startDate, LocalDate endDate) {
        String filename = reportName + "_" + (startDate != null ? startDate : "all") + "_to_" + (endDate != null ? endDate : "present") + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}