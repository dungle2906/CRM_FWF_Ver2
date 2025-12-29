package com.example.basiccrmfwf.shared.util;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServiceUtils {

    // Set the cursor to method name and Shift + F6 to rename all usage function
    // Or use Ctrl + Shift + R to replace all occurrences in project to rename
    public static String getCellValue(Cell cell) {
        try {
            if (cell == null || cell.getCellType() == CellType.BLANK) return null;

            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        LocalDateTime dt = cell.getLocalDateTimeCellValue();
                        yield dt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
                    }
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> cell.getCellFormula();
                default -> null;
            };
        } catch (Exception e) {
            return "ERR(" + e.getMessage() + ")";
        }
    }

    public static boolean isRowEmpty(Row row) {
        for (int c = 0; c <= 4; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static BigDecimal toBigDecimal(Cell cell) {
        try {
            return new BigDecimal(cell.toString().trim().replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static double calculateGrowth(long previous, long current) {
        if (previous == 0) return 100.0;
        return ((double) (current - previous) / previous) * 100.0;
    }

    public static BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), RoundingMode.HALF_UP);
    }

    public static double pct(long curr, long prev) {
        return prev > 0 ? ((double)(curr - prev) / prev) * 100.0 : 0.0;
    }

    public static int parseExcelInteger(String value) {
        if (value == null || value.isBlank()) return 0;
        return (int) Double.parseDouble(value);
    }

    public static boolean isWholeMonthSelection(LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDate fromD = fromDate.toLocalDate();
        LocalDate toD   = toDate.toLocalDate();

        boolean sameMonthYear = fromD.getYear() == toD.getYear() && fromD.getMonth() == toD.getMonth();

        if (!sameMonthYear) return false;

        LocalDate firstDay = fromD.withDayOfMonth(1);
        LocalDate lastDay  = fromD.with(TemporalAdjusters.lastDayOfMonth());

        return fromD.isEqual(firstDay) && toD.isEqual(lastDay);
    }

    public static Map<String, Object[]> toMap(List<Object[]> raw) {
        return raw.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> new Object[]{ row[1], row[2] }
        ));
    }

    public static Pair<String, Integer> extractQuantityAndCleanName(String s) {
        if (s == null) return org.apache.commons.lang3.tuple.Pair.of(null, 0);

        String input = s.trim().replaceAll("\\s+", " ");
        Matcher m = Pattern.compile("\\((\\d+)\\)\\s*$").matcher(input);

        int quantity = 1;
        if (m.find()) {
            try { quantity = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignore) { quantity = 1; }
        }
        String cleanName = cleanTailNumber(input);
        return org.apache.commons.lang3.tuple.Pair.of(cleanName, quantity);
    }

    public static String cleanTailNumber(String s) {
        return s.replaceAll("\\s*\\(\\d+\\)$", "");
    }

    // Helper method to safely cast to BigDecimal
    public static BigDecimal toSafeBigDecimal(Object value) {
        return value != null ? (BigDecimal) value : BigDecimal.ZERO;
    }

    public static double calculateGrowthBigDecimal(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return 100.0;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public static double calculatePercentChange(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    public static LocalDateTime parseDate(String value) {
        List<String> patterns = List.of("HH:mm dd/MM/yyyy");
        for (String p : patterns) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Invalid date format: " + value);
    }

    public static String normalize(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
