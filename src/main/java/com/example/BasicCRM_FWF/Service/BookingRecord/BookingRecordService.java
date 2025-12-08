package com.example.BasicCRM_FWF.Service.BookingRecord;

import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Model.BookingRecord;
import com.example.BasicCRM_FWF.Model.BookingStatus;
import com.example.BasicCRM_FWF.Model.Region;
import com.example.BasicCRM_FWF.Repository.BookingRecordRepository;
import com.example.BasicCRM_FWF.Repository.BookingStatusRepository;
import com.example.BasicCRM_FWF.Repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.BasicCRM_FWF.Utils.ServiceUtils.*;
import static javax.swing.UIManager.getString;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRecordService implements BookingRecordInterface {

    private final BookingRecordRepository repository;
    private final RegionRepository regionRepository;
    private final BookingStatusRepository bookingStatusRepository;

    public void importFromExcel(MultipartFile file) {
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            // ✅ Map Region: shop_name (chuẩn hoá) → Region
            Map<String, Region> regionMap = regionRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(
                            r -> r.getShop_name().trim().toLowerCase(),
                            Function.identity()
                    ));

            Map<String, BookingStatus> bookingStatusMap = bookingStatusRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(
                            b -> normalize(b.getStatus().trim().toLowerCase()),
                            Function.identity()
                    ));

            // Bỏ qua 2 dòng đầu (header)
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    log.info("Stopped at row {} (blank)", i);
                    break;
                }

                try {
                    // Lấy dữ liệu gốc từ cell
                    String createdStr = getCellValue(row.getCell(1));
                    String bookingStr = getCellValue(row.getCell(2));
                    String shopName = getCellValue(row.getCell(3));
                    String cus_amount = getCellValue(row.getCell(15));
                    if (cus_amount.endsWith(".")) {
                        cus_amount = cus_amount.substring(0, cus_amount.length() - 1);
                    }

                    // Parse ngày giờ
                    LocalDateTime created_date = parseDate(createdStr);
                    LocalDateTime booking_date = parseDate(bookingStr);

                    Region facilityRecordService = regionMap.get(shopName.trim().toLowerCase());
                    if (facilityRecordService == null) {
                        log.warn("Row {} skipped: Không tìm thấy Region cho tên '{}'", i, shopName);
                        skippedCount++;
                        continue;
                    }

                    String bookingStatusName = getCellValue(row.getCell(7));
                    log.info("DTHIS IS INFO: " +  bookingStatusName);
                    BookingStatus bookingStatus = null;
                    if (bookingStatusName != null) {
                        bookingStatus = bookingStatusMap.get(normalize(bookingStatusName.trim().toLowerCase()));
                    }

                    BookingRecord bookingRecord = BookingRecord.builder()
                            .created_date(created_date)
                            .booking_date(booking_date)
                            .facility(facilityRecordService)
                            .customer_name(getCellValue(row.getCell(4)))
                            .phone_number(getCellValue(row.getCell(5)))
                            .bookingStatus(bookingStatus)
                            .bookingEmployee(getCellValue(row.getCell(11)))
                            .customerStatus("Khách cũ".equalsIgnoreCase(getCellValue(row.getCell(13))))
                            .customer_amount(cus_amount.isBlank()? null : Integer.parseInt(cus_amount))
                            .build();
                    repository.save(bookingRecord);
                    successCount++;
                } catch (Exception e) {
                    log.error("Row {} failed. Data snapshot: created='{}', booking='{}', shop='{}', status='{}'. Error:",
                            i,
                            getCellValue(row.getCell(1)),
                            getCellValue(row.getCell(2)),
                            getCellValue(row.getCell(3)),
                            getCellValue(row.getCell(7)),
                            e
                    );
                    failCount++;
                }
            }

            log.info("IMPORT COMPLETE: Success = {}, Failed = {}, Skipped = {}", successCount, failCount, skippedCount);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel", e);
        }
    }

    public List<HourlyFacilityStatsDTO> getHourlyArrivalStats(CustomerReportRequestVer2 request) {

        List<BookingRecord> records =
                repository.findArrivalsBetween(request.getFromDate(), request.getToDate(), request.getStatus());

        Map<String, HourlyFacilityStatsDTO> facilityStatsMap = new HashMap<>();

        for (BookingRecord br : records) {
            if (br.getBooking_date() == null || br.getFacility() == null) continue;

            String facility = br.getFacility().getShop_name();
            int hour = br.getBooking_date().getHour();

            facilityStatsMap
                    .computeIfAbsent(facility, HourlyFacilityStatsDTO::new)
                    .addCount(hour, 1);
        }

        return facilityStatsMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotal(), a.getTotal()))
                .collect(Collectors.toList());
    }

    public List<BookingStatusStatsDTO> getBookingStatusStats(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = repository.countBookingByStatusBetween(start, end);

        return rows.stream()
                .map(row -> new BookingStatusStatsDTO(
                        row[0] != null ? row[0].toString() : "Không xác định",
                        row[1] != null ? ((Number) row[1]).longValue() : 0L
                ))
                .collect(Collectors.toList());
    }

    public CustomerStatusRatioDTO getCustomerStatusRatio(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = repository.countByCustomerStatus(start, end);

        long newCustomers = 0, returningCustomers = 0;
        for (Object[] row : rows) {
            Boolean isOld = (Boolean) row[0]; // customerStatus = true nếu là khách cũ
            Long count = ((Number) row[1]).longValue();

            if (Boolean.TRUE.equals(isOld)) {
                returningCustomers += count;
            } else {
                newCustomers += count;
            }
        }
        return new CustomerStatusRatioDTO(newCustomers, returningCustomers);
    }

    public List<TopCustomerDTO> getTopCustomers(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = repository.findTopCustomers(start, end);
        return rows.stream()
                .map(row -> new TopCustomerDTO(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<TopEmployeeDTO> getTopBookingEmployee(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = repository.findTopBookingEmployees(start, end);
        return rows.stream()
                .map(row -> new TopEmployeeDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }
}
