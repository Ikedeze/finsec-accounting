package com.finsec.accounting.service;

import com.finsec.accounting.model.PaymentMethod;
import com.finsec.accounting.model.Transaction;
import com.finsec.accounting.repository.TransactionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelReportService {

    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public ExcelReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // REPORT A: Comprehensive Ledger (Now includes ALL 3 Tabs!)
    public byte[] generateTransactionExcel(String orgId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Transaction> allTransactions = transactionRepository.findByOrganizationId(orgId);
        List<Transaction> periodTransactions = filterByDate(allTransactions, startDate, endDate);
        BigDecimal bbf = calculateBalanceBroughtForward(allTransactions, startDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ==========================================
            // TAB 1: COMPREHENSIVE LEDGER (GROUPED BY MONTH)
            // ==========================================
            Sheet sheet1 = workbook.createSheet("Transactions Report");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle monthHeaderStyle = createMonthHeaderStyle(workbook);

            String[] headers = {"Date", "Type", "Category", "Amount", "Payment Method", "Bank Details", "Description"};
            Row headerRow = sheet1.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Group transactions by monthYear
            Map<String, List<Transaction>> byMonth = periodTransactions.stream()
                    .filter(t -> parseMonthYear(t.getMonthYear()) != null)
                    .collect(Collectors.groupingBy(Transaction::getMonthYear));

            // Sort months chronologically
            List<String> sortedMonths = new ArrayList<>(byMonth.keySet());
            sortedMonths.sort((m1, m2) -> parseMonthYear(m1).compareTo(parseMonthYear(m2)));

            int rowIndex = 1;
            for (String monthYearStr : sortedMonths) {
                // Create Month Header Row
                Row monthRow = sheet1.createRow(rowIndex++);
                Cell monthCell = monthRow.createCell(0);
                monthCell.setCellValue("MONTH: " + monthYearStr.toUpperCase());
                monthCell.setCellStyle(monthHeaderStyle);

                // Print transactions for this month
                for (Transaction t : byMonth.get(monthYearStr)) {
                    Row row = sheet1.createRow(rowIndex++);
                    row.createCell(0).setCellValue(t.getTransactionDate() != null ? t.getTransactionDate().toString() : "");
                    row.createCell(1).setCellValue(t.getType() != null ? t.getType() : "");
                    row.createCell(2).setCellValue(t.getCategoryName() != null ? t.getCategoryName() : "");

                    Cell amountCell = row.createCell(3);
                    if (t.getAmount() != null) {
                        amountCell.setCellValue(t.getAmount().doubleValue());
                        amountCell.setCellStyle(currencyStyle);
                    }

                    row.createCell(4).setCellValue(t.getPaymentMethod() != null ? t.getPaymentMethod().name() : "CASH");
                    row.createCell(5).setCellValue(t.getBankDetails() != null ? t.getBankDetails() : "");
                    row.createCell(6).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                }
            }
            autoSizeColumns(sheet1, headers.length);

            // ==========================================
            // TAB 2: MONTHLY SUMMARY
            // ==========================================
            buildMonthlySummarySheet(workbook, bbf, byMonth, sortedMonths, headerStyle, currencyStyle);

            // ==========================================
            // TAB 3: CATEGORY BREAKDOWN
            // ==========================================
            buildCategoryBreakdownSheet(workbook, bbf, periodTransactions, allTransactions, endDate, headerStyle, currencyStyle);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Existing individual methods kept intact so your Spring Controller doesn't break
    public byte[] generateMonthlySummaryExcel(String orgId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Transaction> allTransactions = transactionRepository.findByOrganizationId(orgId);
        BigDecimal bbf = calculateBalanceBroughtForward(allTransactions, startDate);
        List<Transaction> periodTransactions = filterByDate(allTransactions, startDate, endDate);

        Map<String, List<Transaction>> byMonthYear = periodTransactions.stream()
                .filter(t -> parseMonthYear(t.getMonthYear()) != null)
                .collect(Collectors.groupingBy(Transaction::getMonthYear));

        List<String> sortedMonths = new ArrayList<>(byMonthYear.keySet());
        sortedMonths.sort((m1, m2) -> parseMonthYear(m1).compareTo(parseMonthYear(m2)));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildMonthlySummarySheet(workbook, bbf, byMonthYear, sortedMonths, createHeaderStyle(workbook), createCurrencyStyle(workbook));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateCategoryBreakdownExcel(String orgId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Transaction> allTransactions = transactionRepository.findByOrganizationId(orgId);
        BigDecimal bbf = calculateBalanceBroughtForward(allTransactions, startDate);
        List<Transaction> periodTransactions = filterByDate(allTransactions, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildCategoryBreakdownSheet(workbook, bbf, periodTransactions, allTransactions, endDate, createHeaderStyle(workbook), createCurrencyStyle(workbook));
            workbook.write(out);
            return out.toByteArray();
        }
    }


    // SHEET BUILDER HELPERS
    private void buildMonthlySummarySheet(Workbook workbook, BigDecimal bbf, Map<String, List<Transaction>> byMonth, List<String> sortedMonths, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Monthly Summary");
        String[] headers = {"MONTH", "INCOME", "EXPENDITURE", "BALANCE"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        Row bbfRow = sheet.createRow(rowIndex++);
        bbfRow.createCell(0).setCellValue("BALANCE BROUGHT FORWARD (B/F)");
        bbfRow.createCell(1).setCellValue("-");
        bbfRow.createCell(2).setCellValue("-");
        Cell bbfBalCell = bbfRow.createCell(3);
        bbfBalCell.setCellValue(bbf.doubleValue());
        bbfBalCell.setCellStyle(currencyStyle);

        BigDecimal runningBalance = bbf;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (String monthYearStr : sortedMonths) {
            List<Transaction> monthTrans = byMonth.get(monthYearStr);
            BigDecimal monthInc = sumAmount(monthTrans, "INCOME");
            BigDecimal monthExp = sumAmount(monthTrans, "EXPENSE");
            runningBalance = runningBalance.add(monthInc).subtract(monthExp);
            totalIncome = totalIncome.add(monthInc);
            totalExpense = totalExpense.add(monthExp);

            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(monthYearStr.toUpperCase());
            Cell incCell = row.createCell(1); incCell.setCellValue(monthInc.doubleValue()); incCell.setCellStyle(currencyStyle);
            Cell expCell = row.createCell(2); expCell.setCellValue(monthExp.doubleValue()); expCell.setCellStyle(currencyStyle);
            Cell balCell = row.createCell(3); balCell.setCellValue(runningBalance.doubleValue()); balCell.setCellStyle(currencyStyle);
        }

        Row totalRow = sheet.createRow(rowIndex + 1);
        totalRow.createCell(0).setCellValue("TOTAL");
        totalRow.getCell(0).setCellStyle(headerStyle);
        Cell tInc = totalRow.createCell(1); tInc.setCellValue(totalIncome.doubleValue()); tInc.setCellStyle(currencyStyle);
        Cell tExp = totalRow.createCell(2); tExp.setCellValue(totalExpense.doubleValue()); tExp.setCellStyle(currencyStyle);
        autoSizeColumns(sheet, headers.length);
    }

    private void buildCategoryBreakdownSheet(Workbook workbook, BigDecimal bbf, List<Transaction> periodTransactions, List<Transaction> allTransactions, LocalDate endDate, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Category Report");
        Map<String, BigDecimal> incomes = periodTransactions.stream()
                .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> t.getCategoryName() != null ? t.getCategoryName() : "Uncategorized",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Map<String, BigDecimal> expenses = periodTransactions.stream()
                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> t.getCategoryName() != null ? t.getCategoryName() : "Uncategorized",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        int rowIndex = 0;
        Row incTitle = sheet.createRow(rowIndex++);
        incTitle.createCell(0).setCellValue("SOURCE OF INCOME");
        incTitle.getCell(0).setCellStyle(headerStyle);
        incTitle.createCell(1).setCellValue("AMOUNT");
        incTitle.getCell(1).setCellStyle(headerStyle);

        Row bbfRow = sheet.createRow(rowIndex++);
        bbfRow.createCell(0).setCellValue("Balance B/F");
        Cell bbfCell = bbfRow.createCell(1); bbfCell.setCellValue(bbf.doubleValue()); bbfCell.setCellStyle(currencyStyle);

        BigDecimal totalInc = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : incomes.entrySet()) {
            Row r = sheet.createRow(rowIndex++);
            r.createCell(0).setCellValue(entry.getKey());
            Cell c = r.createCell(1); c.setCellValue(entry.getValue().doubleValue()); c.setCellStyle(currencyStyle);
            totalInc = totalInc.add(entry.getValue());
        }

        rowIndex++;
        Row expTitle = sheet.createRow(rowIndex++);
        expTitle.createCell(0).setCellValue("EXPENDITURES");
        expTitle.getCell(0).setCellStyle(headerStyle);
        expTitle.createCell(1).setCellValue("AMOUNT");
        expTitle.getCell(1).setCellStyle(headerStyle);

        BigDecimal totalExp = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : expenses.entrySet()) {
            Row r = sheet.createRow(rowIndex++);
            r.createCell(0).setCellValue(entry.getKey());
            Cell c = r.createCell(1); c.setCellValue(entry.getValue().doubleValue()); c.setCellStyle(currencyStyle);
            totalExp = totalExp.add(entry.getValue());
        }

        rowIndex++;
        Row sumTitle = sheet.createRow(rowIndex++);
        sumTitle.createCell(0).setCellValue("SUMMARY");
        sumTitle.getCell(0).setCellStyle(headerStyle);

        BigDecimal grossIncome = totalInc.add(bbf);
        Row rTotalInc = sheet.createRow(rowIndex++);
        rTotalInc.createCell(0).setCellValue("TOTAL INCOME (Inc. B/F)");
        Cell cTotalInc = rTotalInc.createCell(1); cTotalInc.setCellValue(grossIncome.doubleValue()); cTotalInc.setCellStyle(currencyStyle);

        Row rTotalExp = sheet.createRow(rowIndex++);
        rTotalExp.createCell(0).setCellValue("TOTAL EXPENDITURE");
        Cell cTotalExp = rTotalExp.createCell(1); cTotalExp.setCellValue(totalExp.doubleValue()); cTotalExp.setCellStyle(currencyStyle);

        BigDecimal finalBal = grossIncome.subtract(totalExp);
        Row rBal = sheet.createRow(rowIndex++);
        rBal.createCell(0).setCellValue("BALANCE");
        Cell cBal = rBal.createCell(1); cBal.setCellValue(finalBal.doubleValue()); cBal.setCellStyle(currencyStyle);

        List<Transaction> upToDateList = filterByDate(allTransactions, null, endDate);
        BigDecimal cashAtBank = sumByPaymentMethod(upToDateList, PaymentMethod.BANK);
        BigDecimal cashAtHand = sumByPaymentMethod(upToDateList, PaymentMethod.CASH);

        Row rBank = sheet.createRow(rowIndex++);
        rBank.createCell(0).setCellValue("CASH AT BANK");
        Cell cBank = rBank.createCell(1); cBank.setCellValue(cashAtBank.doubleValue()); cBank.setCellStyle(currencyStyle);

        Row rHand = sheet.createRow(rowIndex++);
        rHand.createCell(0).setCellValue("CASH AT HAND");
        Cell cHand = rHand.createCell(1); cHand.setCellValue(cashAtHand.doubleValue()); cHand.setCellStyle(currencyStyle);

        autoSizeColumns(sheet, 2);
    }


    // DATA FILTERING & UTILITY HELPERS

    // Safely parses "August 2026" into a YearMonth object for logic comparisons
    private YearMonth parseMonthYear(String monthYearStr) {
        if (monthYearStr == null || monthYearStr.trim().isEmpty()) return null;
        try {
            return YearMonth.parse(monthYearStr, MONTH_YEAR_FORMATTER);
        } catch (Exception e) {
            return null; // Ignore invalid formats gracefully
        }
    }

    // Filters entirely based on the monthYear string!
    private List<Transaction> filterByDate(List<Transaction> list, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) return list;

        YearMonth startMonth = startDate != null ? YearMonth.from(startDate) : null;
        YearMonth endMonth = endDate != null ? YearMonth.from(endDate) : null;

        return list.stream().filter(t -> {
            YearMonth txMonth = parseMonthYear(t.getMonthYear());
            if (txMonth == null) return false;
            if (startMonth != null && txMonth.isBefore(startMonth)) return false;
            if (endMonth != null && txMonth.isAfter(endMonth)) return false;
            return true;
        }).collect(Collectors.toList());
    }

    // BBF calculates everything strictly before the startDate's monthYear!
    private BigDecimal calculateBalanceBroughtForward(List<Transaction> list, LocalDate startDate) {
        if (startDate == null) return BigDecimal.ZERO;
        YearMonth startMonth = YearMonth.from(startDate);

        return list.stream().filter(t -> {
                    YearMonth txMonth = parseMonthYear(t.getMonthYear());
                    return txMonth != null && txMonth.isBefore(startMonth);
                })
                .map(t -> "INCOME".equalsIgnoreCase(t.getType()) ? (t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                        : (t.getAmount() != null ? t.getAmount().negate() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmount(List<Transaction> list, String type) {
        return list.stream()
                .filter(t -> type.equalsIgnoreCase(t.getType()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByPaymentMethod(List<Transaction> list, PaymentMethod pm) {
        return list.stream()
                .filter(t -> pm.equals(t.getPaymentMethod()) && t.getAmount() != null)
                .map(t -> "INCOME".equalsIgnoreCase(t.getType()) ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // STYLING HELPERS
    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle(); style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createMonthHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont(); font.setBold(true);
        CellStyle style = workbook.createCellStyle(); style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex()); // Highlights the month separator
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) sheet.autoSizeColumn(i);
    }
}