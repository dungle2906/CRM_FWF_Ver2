package com.example.BasicCRM_FWF.Utils;

import com.example.BasicCRM_FWF.Model.ServiceType;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ServiceUtils {

    // Set the cursor to method name and Shift + F6 to rename all usage function
    // Or use Ctrl + Shift + R to replace all occurrences in project to rename
    public static String getCellValue(Cell cell) {
        try {
            if (cell == null || cell.getCellType() == CellType.BLANK) return "";

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
                default -> "";
            };
        } catch (Exception e) {
            return "";
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

    public static String normalizeServiceName(String s) {
        if (s == null) return null;
        String x = s.trim().replaceAll("\\s+", " ").toLowerCase();

        // remove duplicated closing parens at end: "abc))" -> "abc"
        x = x.replaceAll("\\)+\\s*$", ")").replaceAll("\\(\\s*\\)\\s*$", ""); // () at end

        // if ends with extra ")" without "(", remove it
        while (x.endsWith(")") && x.lastIndexOf("(") < x.lastIndexOf(")")) {
            int open = x.lastIndexOf("(");
            int close = x.lastIndexOf(")");
            if (open == -1 || close < open) {
                x = x.substring(0, x.length() - 1).trim();
            } else {
                break;
            }
        }

        // remove quantity tail "(digits)"
        x = x.replaceAll("\\s*\\(\\d+\\)\\s*$", "");

        // remove trailing tags in parentheses if you want exact-name match:
        // NOTE: If DB stores names without tags, remove them; if DB includes tags, keep them.
        x = x.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();

        return x;
    }

    public static Map<ServiceType, Integer> parseServicesPerBill(
            String cellValue,
            Map<String, ServiceType> byName,
            Map<String, ServiceType> byCode,
            int rowIndex
    ) {
        Map<ServiceType, Integer> result = new HashMap<>();
        if (cellValue == null || cellValue.isBlank()) return result;

        for (String part : cellValue.split(";")) {
            if (part.isBlank()) continue;

            Pair<String, Integer> parsed = extractQuantityAndCleanName(part);
            String rawName = parsed.getLeft();
            int qty = parsed.getRight();

            ServiceType st = resolveServiceType(rawName, byName, byCode);
            if (st == null) {
                log.warn("Row {}: ServiceType not found '{}'", rowIndex, rawName);
                continue;
            }
            if (qty <= 0) continue;

            result.merge(st, qty, Integer::sum);
        }
        return result;
    }

    private static ServiceType resolveServiceType(
            String rawName,
            Map<String, ServiceType> byName,
            Map<String, ServiceType> byCode
    ) {
        if (rawName == null || rawName.isBlank()) return null;

        String original = rawName.trim().replaceAll("\\s+", " ");

        // 1️⃣ exact normalized name
        ServiceType st = byName.get(normalizeServiceName(original));
        if (st != null) return st;

        // 2️⃣ rule → service_code
        String code = mapToServiceCodeByRule(original);
        if (code != null) {
            st = byCode.get(code);
            if (st != null) return st;
        }

        return null;
    }

    private static String mapToServiceCodeByRule(String s) {
        if (s == null) return null;

        String x = s.trim().replaceAll("\\s+", " ");

        // ===== COMMON BOOLEAN FLAGS =====
        boolean isBuoiLe =
                x.endsWith("(buổi lẻ)") ||
                x.endsWith("( buổi lẻ )") ||
                x.endsWith("(buồi lẻ)");

        boolean isGiaTheFoxie =
                x.endsWith("(Giá thẻ Foxie Member Card)") ||
                x.endsWith("(giá thẻ Foxie Member Card)") ||
                x.endsWith("(gía Foxie Member Card)") ||
                x.endsWith("(Giá Foxie Member Card)") ||
                x.endsWith("(giá Foxie Member Card)") ||
                x.endsWith("Giá Foxie Member Card") ||
                x.endsWith("(thẻ Foxie Member Card)");

        boolean containsGia = x.toLowerCase().contains("giá");

        // =========================================================
        // 1️⃣ QUICK / SPECIAL CASES
        // =========================================================
        if (x.toUpperCase().startsWith("QT KÈM THẺ TIỀN FOXIE")) return "QT 1.1";
        if (x.equals("Foxie Card 0")) return "TT1";

        // =========================================================
        // 2️⃣ TRẢI NGHIỆM / KHUYẾN MÃI
        // =========================================================
        if (x.contains("TRẢI NGHIỆM LẦN ĐẦU") && x.contains("COMBO 9")) return "CB 9.3";
        if (x.contains("KHUYẾN MÃI TRẢI NGHIỆM LẦN ĐẦU") && x.contains("COMBO 4")) return "CB 4.3";

        // =========================================================
        // 3️⃣ DỊCH VỤ (DV)
        // =========================================================
        if (x.startsWith("DV 1: AQUA PEEL CLEANSE"))
            return isBuoiLe ? "DV 1.1" : (containsGia ? "DV 1.3" : null);

        if (x.startsWith("DV 2: DEEP CLEANSE")) {
            if (isBuoiLe) return "DV 2.1";
            if (containsGia) return "DV 2.2";
        }

        if (x.startsWith("DV 3: CRYO CLEANSE") && containsGia) return "DV 3.3";

        if (x.startsWith("DV 4:")) {
            if (isBuoiLe) return "DV 4.1";
            if (isGiaTheFoxie || x.endsWith("-Giá Foxie Member Card")) return "DV 4.2";
        }

        if (x.startsWith("DV 5: GYMMING CLEANSE")) {
            if (isBuoiLe) return "DV 5.1";
            if (containsGia) return "DV 5.2";
        }

        if (x.startsWith("DV 6: EYE-REVIVE CLEANSE")) {
            if (isBuoiLe) return "DV 6.1";
            if (containsGia) return "DV 6.2";
        }

        // =========================================================
        // 4️⃣ CỘNG THÊM (CT)
        // =========================================================
        if (x.startsWith("CT 1:")) {
            if (isBuoiLe) return "CT 1.1";
            if (isGiaTheFoxie || x.endsWith("- Giá Foxie Member Card")) return "CT 1.2";
        }

        if (x.startsWith("CT 2: ADDED LUMIGLOW"))
            return containsGia ? "CT 2.2" : "CT 2.1";

        if (x.startsWith("CT 3: ADDED GYMMING")) return "CT 3.1";
        if (x.startsWith("CT 4: ADDED EYE-REVIVE")) return "CT 4.1";

        if (x.startsWith("CT 5:")) {
            if (isBuoiLe) return "CT 5.1";
            if (isGiaTheFoxie || x.endsWith("- Giá Foxie Member Card")) return "CT 5.2";
        }

        if (x.startsWith("CT 6: ADDED GOODBYE ACNE")) return "CT 6.1";

        // =========================================================
        // 5️⃣ COMBO THƯỜNG (CB)
        // =========================================================
        if (x.startsWith("COMBO 1")) {
            if (isBuoiLe) return "CB 1.1";
            if (containsGia) return "CB 1.2";
        }

        if (x.startsWith("COMBO 2")) {
            if (isBuoiLe) return "CB 2.1";
            if (isGiaTheFoxie) return "CB 2.2";
        }

        if (x.startsWith("COMBO 3")) {
            if (isBuoiLe) return "CB 3.1";
            if (isGiaTheFoxie) return "CB 3.2";
        }

        if (x.startsWith("COMBO 4")) {
            if (isBuoiLe) return "CB 4.1";
            if (isGiaTheFoxie) return "CB 4.2";
        }

        if (x.startsWith("COMBO 5")) {
            if (isBuoiLe) return "CB 5.1";
            if (isGiaTheFoxie) return "CB 5.2";
        }

        if (x.startsWith("COMBO 6")) {
            if (isBuoiLe) return "CB 6.1";
            if (isGiaTheFoxie || containsGia) return "CB 6.2";
        }

        if (x.startsWith("COMBO 7")) {
            if (isBuoiLe) return "CB 7.1";
            if (isGiaTheFoxie) return "CB 7.2";
        }

        if (x.startsWith("COMBO 8")) {
            if (isBuoiLe) return "CB 8.1";
            if (isGiaTheFoxie) return "CB 8.2";
        }

        if (x.startsWith("COMBO 9")) {
            if (isBuoiLe) return "CB 9.1";
            if (isGiaTheFoxie) return "CB 9.2";
        }

        if (x.startsWith("COMBO 10")) {
            if (isBuoiLe) return "CB 10.1";
            if (isGiaTheFoxie) return "CB 10.2";
        }

        if (x.startsWith("COMBO 11")) {
            if (isBuoiLe) return "CB 11.1";
            if (isGiaTheFoxie) return "CB 11.2";
        }

        if (x.startsWith("COMBO 12")) {
            if (isBuoiLe) return "CB 12.1";
            if (isGiaTheFoxie) return "CB 12.2";
        }

        // =========================================================
        // 6️⃣ COMBO CHĂM SÓC (CBCS)
        // =========================================================
        if (x.startsWith("COMBO CS 1")) {
            if (isBuoiLe) return "CBCS 1.1";
            if (containsGia) return "CBCS 1.2";
        }

        if (x.startsWith("COMBO CS 3")) {
            if (isBuoiLe) return "CBCS 3.1";
            if (isGiaTheFoxie) return "CBCS 3.2";
        }

        if (x.startsWith("COMBO CS 4")) {
            if (isBuoiLe) return "CBCS 4.1";
            if (isGiaTheFoxie) return "CBCS 4.2";
        }

        if (x.startsWith("COMBO CS 5")) {
            if (isBuoiLe) return "CBCS 5.1";
            if (isGiaTheFoxie) return "CBCS 5.2";
        }

        if (x.startsWith("COMBO CS 7")) {
            if (isBuoiLe) return "CBCS 7.1";
            if (isGiaTheFoxie) return "CBCS 7.2";
        }

        if (x.startsWith("COMBO CS 8")) {
            if (isBuoiLe) return "CBCS 8.1";
            if (isGiaTheFoxie) return "CBCS 8.2";
        }

        if (x.startsWith("COMBO CS 9")) return "CBCS 9.1";
        if (x.contains("COMBO CS 11: BURNT SKIN SOS")) return "CBCS 11.2";

        // =========================================================
        // 7️⃣ SẢN PHẨM / HÀNG HOÁ
        // =========================================================
        if (x.equals("BỊCH BÔNG TẨY TRANG")) return "MD000007";
        if (x.equals("Bông tẩy trang quà tặng")) return "NL52";
        if (x.contains("SỮA RỬA MẶT LÀM DỊU ELRAVIE")) return "MP000037";
        if (x.contains("BỘ SẢN PHẨM DƯỠNG DA FULL SIZE ELRAVIE")) return "MP000024";
        if (x.contains("KEM DƯỠNG MẮT ELRAVIE")) return "MP000014";
        if (x.equals("KEM DƯỠNG LÀM DỊU DA DÀNH CHO DA NHẠY CẢM ELRAVIE ATOFERON CALM CREAM 50ML")) return "MP000013";
        if (x.equals("KEM DƯỠNG LÀM DỊU DA DÀNH CHO DA NHẠY CẢM ELRAVIE ATOFERON CALM CREAM 120ML")) return "MP000012";
        if (x.equals("DẦU ARGAN DƯỠNG DA VÀ TÓC PURA D’OR 100% ORGANIC ARGAN OIL 118ML")) return "MP000043";
        if (x.equals("Gối Cổ Bơm Hơi Hình Chữ U")) return "CS000100";
        if (x.equals("TONER PAD LÀM DỊU DA ELRAVIE HYCICAMUE (80 MIẾNG/HỘP)")) return "MP000038";
        if (x.equals("TINH CHẤT NGĂN NGỪA RỤNG TÓC PURA D'OR THERAPY ENERGIZING SCALP 120ML")) return "MP000048";
        if (x.equals("SET 28 LỌ TINH CHẤT PHỤC HỒI DA PDRN 28DAYS TURNOVER AMPOULE")) return "MP000009";
        if (x.equals("KEM DƯỠNG ẨM DẠNG GEL GIÚP LÀM DỊU DA ELRAVIE HYCIAMUE CREAM 50ML")) return "MP000040";
        if (x.equals("TINH CHẤT CHỐNG LÃO HÓA, CẤP ẨM ELRAVIE PRO ULTIMATE AMPOULE 50ML")) return "MP000026";
        if (x.equals("SERUM VITAMIN C CHỐNG LÃO HÓA PURA D’OR 20% VITAMIN C SERUM 118ML")) return "MP000044";
        if (x.equals("DẦU HẠT TẦM XUÂN HỮU CƠ PURA D'OR ROSEHIP OIL DƯỠN DA GIÚP MỜ THÂM SÁNG DA, CHỐNG LÃO HÓA 118ML")) return "MP000047";

        return null;
    }


    public static Map<ServiceType, Integer> parseServices(
            String comboCell,
            Map<String, ServiceType> serviceTypeCache,
            int rowIndex
    ) {
        Map<ServiceType, Integer> result = new HashMap<>();

        if (comboCell == null || comboCell.isBlank()) return result;

        String normalized = comboCell.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(";");

        for (String part : parts) {
            if (part.isBlank()) continue;

            Pair<String, Integer> parsed = extractQuantityAndCleanName(part);
            String name = parsed.getLeft();
            int qty = parsed.getRight();

            ServiceType st = serviceTypeCache.get(name);
            if (st == null) {
                log.warn("Row {}: ServiceType not found '{}'", rowIndex, name);
                continue;
            }

            result.merge(st, qty > 0 ? qty : 1, Integer::sum);
        }

        return result;
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
