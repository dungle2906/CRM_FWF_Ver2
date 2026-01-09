package com.example.BasicCRM_FWF.Service.BookingRecord;

import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Model.*;
import com.example.BasicCRM_FWF.Repository.*;
import com.example.BasicCRM_FWF.Service.BookingImportBatchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.BasicCRM_FWF.Utils.ServiceUtils.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRecordService implements BookingRecordInterface {

    @PersistenceContext
    private EntityManager entityManager;

    private final BookingRecordRepository repository;
    private final RegionRepository regionRepository;
    private final BookingStatusRepository bookingStatusRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final BookingRecordRepository bookingRepository;
    private final BookingSalesMapRepository  bookingSalesMapRepository;
    private final BookingImportBatchService  batchService;

    public void importFromExcel(MultipartFile file) {
        final int BATCH_SIZE = 1000;
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        List<BookingRecord> batchBookings = new ArrayList<>(BATCH_SIZE);

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            // ===== Cache Region =====
            Map<String, Region> regionMap = regionRepository.findAll().stream().collect(Collectors.toMap(r -> r.getShop_name().trim().toLowerCase(), Function.identity()));
            // ===== Cache BookingStatus =====
            Map<String, BookingStatus> bookingStatusMap = bookingStatusRepository.findAll().stream().collect(Collectors.toMap(b -> normalize(b.getStatus().trim().toLowerCase()), Function.identity()));


            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;


                try {
                    BookingRecord booking = buildBooking(row, regionMap, bookingStatusMap);
                    if (booking == null) {
                        skippedCount++;
                        continue;
                    }

                    batchBookings.add(booking);

                    if (batchBookings.size() >= BATCH_SIZE) {
                        try {
                            batchService.importBatch(batchBookings);
                            successCount += batchBookings.size();
                            log.info("Imported {} rows...", successCount);
                        } catch (Exception e) {
                            failCount += batchBookings.size();
                            log.error("Batch failed at row {}", i, e);
                        }
                        batchBookings.clear();
                    }

                } catch (Exception e) {
                    failCount++;
                    log.error("Row {} failed", i, e);
                }
            }

            if (!batchBookings.isEmpty()) {
                try {
                    batchService.importBatch(batchBookings);
                    successCount += batchBookings.size();
                } catch (Exception e) {
                    failCount += batchBookings.size();
                    log.error("Final batch failed", e);
                }
            }
            log.info("IMPORT DONE → Success={}, Failed={}, Skipped={}", successCount, failCount, skippedCount);

        } catch (Exception e) {
            throw new RuntimeException("Import Booking Excel failed", e);
        }
    }

    private BookingRecord buildBooking(Row row, Map<String, Region> regionMap, Map<String, BookingStatus> bookingStatusMap) {
        String createdStr = getCellValue(row.getCell(1));
        String bookingStr = getCellValue(row.getCell(2));
        String shopName = getCellValue(row.getCell(3));

        Region facility = regionMap.get(shopName.trim().toLowerCase());
        if (facility == null) return null;

        String cusAmountRaw = getCellValue(row.getCell(11));
        if (cusAmountRaw.endsWith(".")) {
            cusAmountRaw = cusAmountRaw.substring(0, cusAmountRaw.length() - 1);
        }


        String bookingStatusName = getCellValue(row.getCell(7));
        BookingStatus bookingStatus = bookingStatusName == null ? null : bookingStatusMap.get(normalize(bookingStatusName.trim().toLowerCase()));


        return BookingRecord.builder().
                created_date(parseDate(createdStr)).
                booking_date(parseDate(bookingStr)).
                facility(facility).
                customer_name(getCellValue(row.getCell(4))).
                phone_number(getCellValue(row.getCell(5))).
                bookingStatus(bookingStatus).
                price("0".equals(getCellValue(row.getCell(7))) ? null : getCellValue(row.getCell(7))).
                bookingEmployee(getCellValue(row.getCell(8))).
                customerStatus("Khách cũ".equalsIgnoreCase(getCellValue(row.getCell(9)))).
                orderId(getCellValue(row.getCell(10))).
                customer_amount(cusAmountRaw.isBlank() ? null : Integer.parseInt(cusAmountRaw)).
                tag(getCellValue(row.getCell(12))).build();
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
