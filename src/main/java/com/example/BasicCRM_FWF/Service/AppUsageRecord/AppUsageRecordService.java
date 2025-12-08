package com.example.BasicCRM_FWF.Service.AppUsageRecord;

import com.example.BasicCRM_FWF.Model.AppUsageRecord;
import com.example.BasicCRM_FWF.Repository.AppUsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.BasicCRM_FWF.Utils.ServiceUtils.isRowEmpty;
import static javax.swing.UIManager.getString;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUsageRecordService implements AppUsageRecordInterface {

    private final AppUsageRecordRepository repository;

    public void importFromExcel(MultipartFile file) {
        int success = 0;
        int failed = 0;

        // Dùng DataFormatter để lấy đúng “giá trị hiển thị” trong Excel (tránh số serial date)
        final DataFormatter formatter = new DataFormatter();
        final ZoneId zone = ZoneId.systemDefault();

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    log.info("Stopped at row {} (blank)", i);
                    break;
                }

                try {
                    // ---- Extract & validate từng cột ----
                    String rawCustomerId = safeGetString(row.getCell(1), formatter); // ví dụ: "#123"
                    if (rawCustomerId == null || rawCustomerId.length() < 2 || !Character.isDigit(rawCustomerId.charAt(1))) {
                        throw new IllegalArgumentException("Invalid customerId: " + rawCustomerId);
                    }
                    int customerId = Integer.parseInt(rawCustomerId.substring(1));

                    String customerName = nullToEmpty(safeGetString(row.getCell(2), formatter));
                    String phoneNumber  = nullToEmpty(safeGetString(row.getCell(3), formatter));

                    // Cột 4: device — file gốc đang parse boolean rồi map sang "IOS"/"Android".
                    // Ở đây mình giữ nguyên logic nhận boolean, nhưng trả ra String deviceType cho an toàn.
                    String deviceType = parseDeviceType(row.getCell(4), formatter); // "IOS" | "Android" | "UNKNOWN"

                    // Cột 5: status — ví dụ "Online"/"Offline"
                    boolean status = parseStatus(row.getCell(5), formatter);

                    // Cột 6: installedAt — hỗ trợ NUMERIC (date) và STRING với nhiều pattern
                    LocalDateTime installedAt = parseInstalledAt(row.getCell(6), formatter, zone);

                    AppUsageRecord record = AppUsageRecord.builder()
                            .customerId(customerId)
                            .customerName(customerName)
                            .phoneNumber(phoneNumber)
                            // !!! Sửa cho đúng kiểu field của bạn:
                            // .device(deviceType) // nếu field là String
                            // hoặc .device(Device.valueOf(deviceType)) nếu là enum
                            .status(status)
                            .installedAt(installedAt)
                            .build();

                    repository.save(record);
                    success++;

                } catch (Exception e) {
                    failed++;

                    // Log chi tiết: giá trị hiển thị & cell type từng ô quan trọng
                    String c1 = safeGetString(row.getCell(1), formatter);
                    String c2 = safeGetString(row.getCell(2), formatter);
                    String c3 = safeGetString(row.getCell(3), formatter);
                    String c4 = safeGetString(row.getCell(4), formatter);
                    String c5 = safeGetString(row.getCell(5), formatter);
                    String c6 = safeGetString(row.getCell(6), formatter);

                    log.warn(
                            "Row {} failed: {} | values => id:'{}' name:'{}' phone:'{}' device:'{}' status:'{}' installedAt:'{}' | types => c4:{} c6:{}",
                            i, e.toString(),
                            trimForLog(c1), trimForLog(c2), trimForLog(c3), trimForLog(c4), trimForLog(c5), trimForLog(c6),
                            cellType(row.getCell(4)), cellType(row.getCell(6))
                    );

                    // In stacktrace ở mức DEBUG để không “ngập” log WARN
                    if (log.isDebugEnabled()) {
                        log.debug("Row {} exception stack:", i, e);
                    }
                }
            }

            log.info("IMPORT APP USAGE: Success = {}, Failed = {}", success, failed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import app usage Excel", e);
        }
    }

// ----------------- Helpers -----------------

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String trimForLog(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private static String cellType(Cell cell) {
        if (cell == null) return "null";
        return cell.getCellType().name();
    }

    private static String safeGetString(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        // DataFormatter sẽ convert mọi loại cell về “hiển thị” giống người dùng nhìn thấy
        String v = formatter.formatCellValue(cell);
        return v != null ? v.trim() : null;
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int cn = row.getFirstCellNum(); cn < row.getLastCellNum(); cn++) {
            Cell c = row.getCell(cn);
            if (c != null && c.getCellType() != CellType.BLANK && !safeIsBlank(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean safeIsBlank(Cell c) {
        if (c == null) return true;
        if (c.getCellType() == CellType.BLANK) return true;
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue();
            return s == null || s.trim().isEmpty();
        }
        return false;
    }

    private static String parseDeviceType(Cell cell, DataFormatter formatter) {
        String v = safeGetString(cell, formatter);
        if (v == null) return "UNKNOWN";
        // Giữ tương thích với code cũ: cột 4 là "true"/"false"
        // true -> IOS, false -> Android
        if ("true".equalsIgnoreCase(v)) return "IOS";
        if ("false".equalsIgnoreCase(v)) return "Android";
        // Nếu cột đã được ghi thẳng "IOS"/"Android"
        if (v.equalsIgnoreCase("IOS") || v.equalsIgnoreCase("iOS")) return "IOS";
        if (v.equalsIgnoreCase("Android")) return "Android";
        return "UNKNOWN";
    }

    private static boolean parseStatus(Cell cell, DataFormatter formatter) {
        String v = safeGetString(cell, formatter);
        if (v == null) return false;
        // theo code gốc: startsWith("Onl")
        return v.trim().toLowerCase(Locale.ROOT).startsWith("onl");
    }

    private static LocalDateTime parseInstalledAt(Cell cell, DataFormatter formatter, ZoneId zone) {
        if (cell == null) throw new IllegalArgumentException("installedAt cell is null");

        // 1) Nếu là numeric & định dạng ngày: lấy trực tiếp
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return LocalDateTime.ofInstant(date.toInstant(), zone);
        }

        // 2) Chuỗi: thử nhiều pattern phổ biến
        String s = safeGetString(cell, formatter);
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("installedAt is blank");
        }

        // Loại bỏ ký tự lạ phổ biến
        s = s.replace('\u00A0', ' ').trim();

        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                // Thêm nếu bạn gặp dạng khác:
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") // ISO không zone
        );

        // Thử LocalDateTime trước
        for (DateTimeFormatter f : fmts) {
            try {
                TemporalAccessor ta = f.parse(s);
                if (ta.isSupported(ChronoField.HOUR_OF_DAY)) {
                    return LocalDateTime.from(ta);
                } else {
                    // Nếu chỉ là LocalDate -> atStartOfDay
                    LocalDate d = LocalDate.from(ta);
                    return d.atStartOfDay();
                }
            } catch (Exception ignore) {}
        }

        // 3) Thử parse ISO với offset (ví dụ: 2025-11-03T11:49:28)
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {}

        throw new IllegalArgumentException("Unparseable installedAt: '" + s + "'");
    }

}
