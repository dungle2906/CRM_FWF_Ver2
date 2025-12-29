package com.example.BasicCRM_FWF.Service.CustomerSaleRecord;

import com.example.BasicCRM_FWF.DTO.CustomerSource;
import com.example.BasicCRM_FWF.DTO.PhoneExportDTO;
import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Model.*;
import com.example.BasicCRM_FWF.Repository.*;
import com.example.BasicCRM_FWF.Utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.BasicCRM_FWF.Utils.ServiceUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSaleRecordService implements CustomerSaleRecordInterface {

    private final CustomerSaleRecordRepository customerSaleRecordRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final AppUsageRecordRepository appUsageRecordRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final BookingRecordRepository bookingRecordRepository;
    private final RegionRepository regionRepository;

    public void importFromExcel(MultipartFile file) {
        int success = 0;
        int failed = 0;

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            // ✅ Tạo map Region: shop_name (chuẩn hoá) → Region
            Map<String, Region> regionMap = regionRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(
                            r -> r.getShop_name().trim().toLowerCase(),
                            Function.identity()
                    ));

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    log.info("Stopped at row {} (blank)", i);
                    break;
                }

                try {
                    String createdStr = ServiceUtils.getCellValue(row.getCell(1));
                    LocalDateTime createdAt = LocalDateTime.parse(createdStr, DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));

                    // ✅ Tra Region bằng shop name (cột CƠ SỞ trong Excel, cột 11 tính từ 0)
                    String shopName = ServiceUtils.getCellValue(row.getCell(11)).trim().toLowerCase();

                    // Nếu "không có" thì set facility = null
//                    Region facilityRecordService;
//                    if ("không có".equals(shopName)) {
//                        facilityRecordService = null;
//                    } else {
//                        facilityRecordService = regionMap.get(shopName);
//                        if (facilityRecordService == null) {
//                            log.warn("Row {}: Không tìm thấy Region cho tên '{}', sẽ import với facility = null", i, shopName);
//                        }
//                    }

                    // ✅ Tạo record dù facility = null
                    CustomerSaleRecord record = CustomerSaleRecord.builder()
                            .createdAt(createdAt)
                            .customerName(ServiceUtils.getCellValue(row.getCell(2)))
                            .customerId(parseExcelInteger(ServiceUtils.getCellValue(row.getCell(3))))
                            .phoneNumber(ServiceUtils.getCellValue(row.getCell(4)))
                            .email(ServiceUtils.getCellValue(row.getCell(5)).contains("Không có Email") ? null : ServiceUtils.getCellValue(row.getCell(5)))
                            .dob(ServiceUtils.getCellValue(row.getCell(6)).contains("Không có") ? null : ServiceUtils.getCellValue(row.getCell(6)))
                            .gender(ServiceUtils.getCellValue(row.getCell(7)))
                            .address(ServiceUtils.getCellValue(row.getCell(8)).contains("Không có") ? null : ServiceUtils.getCellValue(row.getCell(8)))
                            .district(ServiceUtils.getCellValue(row.getCell(9)).contains("Không có") ? null : ServiceUtils.getCellValue(row.getCell(9)))
                            .province(ServiceUtils.getCellValue(row.getCell(10)).contains("Không có") ? null : ServiceUtils.getCellValue(row.getCell(10)))
//                            .facility(null) // null nếu không tìm thấy hoặc "không có"
                            .customerType(ServiceUtils.getCellValue(row.getCell(12)))
                            .source(ServiceUtils.getCellValue(row.getCell(13)))
                            .cardCode(ServiceUtils.getCellValue(row.getCell(14)).contains("Chưa có") ? null : ServiceUtils.getCellValue(row.getCell(14)))
                            .careStaff(ServiceUtils.getCellValue(row.getCell(15)).contains("Chưa có") ? null : ServiceUtils.getCellValue(row.getCell(15)))
                            .wallet(toBigDecimal(ServiceUtils.getCellValue(row.getCell(16)).isBlank() ? null : row.getCell(16)))
                            .debt(toBigDecimal(ServiceUtils.getCellValue(row.getCell(17)).isBlank() ? null : row.getCell(17)))
                            .prepaidCard(toBigDecimal(ServiceUtils.getCellValue(row.getCell(18)).startsWith("0") ? null : row.getCell(18)))
                            .rewardPoint(toBigDecimal(ServiceUtils.getCellValue(row.getCell(19)).startsWith("0") ? null : row.getCell(19)))
                            .build();

                    System.out.println(record.toString());

                    customerSaleRecordRepository.save(record);
                    success++;

                } catch (Exception e) {
                    failed++;
                    log.warn("Row {} skipped due to error: {}", i, e.getMessage());
                }
            }

            log.info("IMPORT CUSTOMER SALE: Success = {}, Failed = {}", success, failed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import customer sale Excel", e);
        }
    }

    public CustomerReportResponse getNewCustomerReport(CustomerReportRequest request) {
        Result r = getResult(request);
        List<DailyCustomerCount> currentRange = customerSaleRecordRepository.countNewCustomersByDate(r.fromDate, r.toDate);
        List<DailyCustomerCount> previousRange = customerSaleRecordRepository.countNewCustomersByDate(r.prevFrom, r.prevTo);

        return new CustomerReportResponse(currentRange, previousRange);
    }

    public CustomerReportResponse getOldCustomerReport(CustomerReportRequest request) {
        Result r = getResult(request);
        List<DailyCustomerCount> currentRange = customerSaleRecordRepository.countOldCustomersByDate(r.fromDate, r.toDate);
        List<DailyCustomerCount> previousRange = customerSaleRecordRepository.countOldCustomersByDate(r.prevFrom, r.prevTo);

        return new CustomerReportResponse(currentRange, previousRange);
    }

    public GenderRatioResponse getGenderRatio(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        List<Object[]> results = customerSaleRecordRepository.countGenderGroup(start, end);

        long male = 0;
        long female = 0;

        for (Object[] row : results) {
            String gender = row[0] != null ? row[0].toString().trim() : "";
            long count = ((Number) row[1]).longValue();

            if (gender.equalsIgnoreCase("Nam")) {
                male += count;
            } else if (gender.equalsIgnoreCase("Nữ")) {
                female += count;
            }
        }

        return new GenderRatioResponse(male, female);
    }

    public Map<String, List<DailyCountDTO>> getCustomerTypeTrend(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        List<Object[]> rawData = customerSaleRecordRepository.countCustomerByTypeAndDay(start, end);

        Map<String, List<DailyCountDTO>> result = new HashMap<>();

        for (Object[] row : rawData) {
            String type = row[0] != null && !row[0].toString().trim().isEmpty()
                    ? row[0].toString().trim()
                    : "Không xác định";
            LocalDateTime date = ((Date) row[1]).toLocalDate().atStartOfDay();
            long count = ((Number) row[2]).longValue();

            result.computeIfAbsent(type, k -> new ArrayList<>())
                    .add(new DailyCountDTO(date, count));
        }

        return result;
    }

    public Map<String, List<DailyCountDTO>> getCustomerSourceTrend(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        List<Object[]> rawData = customerSaleRecordRepository.countCustomerBySourceAndDay(start, end);

        Map<String, List<DailyCountDTO>> result = new HashMap<>();

        for (Object[] row : rawData) {
            String source = row[0] != null ? row[0].toString().trim() : "null";
            LocalDateTime date = ((Date) row[1]).toLocalDate().atStartOfDay();
            long count = ((Number) row[2]).longValue();

            result.computeIfAbsent(source, k -> new ArrayList<>())
                    .add(new DailyCountDTO(date, count));
        }

        return result;
    }

    public List<AppDownloadStatus> calculateAppDownloadStatus(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        List<SalesTransaction> transactions = salesTransactionRepository.findAll().stream()
                .filter(tx -> !tx.getOrderDate().isBefore(start) && !tx.getOrderDate().isAfter(end))
                .collect(Collectors.toList());

        List<CustomerSaleRecord> customerSaleRecords = customerSaleRecordRepository.findAll();
        List<ServiceRecord> serviceRecords = serviceRecordRepository.findAll();

        Set<String> downloadedPhones = appUsageRecordRepository.findAll().stream()
                .map(AppUsageRecord::getPhoneNumber)
                .collect(Collectors.toSet());

        Set<String> allPhones = new HashSet<>();
        transactions.forEach(tx -> allPhones.add(tx.getPhoneNumber()));
        customerSaleRecords.forEach(c -> allPhones.add(c.getPhoneNumber()));
        serviceRecords.forEach(s -> allPhones.add(s.getPhoneNumber()));

        Map<LocalDateTime, Set<String>> phoneByDate = new HashMap<>();
        for (SalesTransaction tx : transactions) {
            LocalDateTime date = tx.getOrderDate().toLocalDate().atStartOfDay();
            phoneByDate.computeIfAbsent(date, k -> new HashSet<>()).add(tx.getPhoneNumber());
        }

        List<AppDownloadStatus> result = new ArrayList<>();

        for (Map.Entry<LocalDateTime, Set<String>> entry : phoneByDate.entrySet()) {
            LocalDateTime date = entry.getKey();
            Set<String> phonesOnDate = entry.getValue();

            long downloaded = phonesOnDate.stream()
                    .filter(downloadedPhones::contains)
                    .count();

            long notDownloaded = phonesOnDate.size() - downloaded;

            result.add(new AppDownloadStatus(date, downloaded, notDownloaded));
        }

        return result;
    }

    public CustomerOrderSummaryDTO calculateAppDownloadSummary(LocalDateTime start, LocalDateTime end) {
        // 1. Lấy danh sách khách hàng (distinct theo phone_number)
        List<CustomerSaleRecord> customerPhones = customerSaleRecordRepository.findDistinctCustomerByCreatedAt(start, end);

        // 2. Lấy danh sách số điện thoại đã tải app
        List<AppUsageRecord> downloadedRecords = appUsageRecordRepository.findDistinctCustomerByInstalled(start, end);

        // 3. Extract danh sách số điện thoại đã tải app
        Set<String> downloadedPhoneNumbers = downloadedRecords.stream()
                .map(AppUsageRecord::getPhoneNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. So sánh số điện thoại customer với số đã tải app
        long downloaded = customerPhones.stream()
                .map(CustomerSaleRecord::getPhoneNumber)
                .filter(Objects::nonNull)
                .filter(downloadedPhoneNumbers::contains)
                .count();

        long notDownloaded = customerPhones.size() - downloaded;

        return new CustomerOrderSummaryDTO(downloaded, notDownloaded);
    }

    public CustomerSummaryDTO calculateCustomerSummary(CustomerReportRequest request) {
        Result result = getResult(request);

        // ===== Kỳ hiện tại - khách mới =====
        long totalNew = bookingRecordRepository.countByCustomerStatusAndBookingDateBetween(
                0, result.fromDate, result.toDate);
        long actualNew = bookingRecordRepository.countByCustomerStatusAndBookingStatusIdAndBookingDateBetween(
                0, result.fromDate, result.toDate);

        // ===== Kỳ trước - khách mới =====
        long prevTotalNew = bookingRecordRepository.countByCustomerStatusAndBookingDateBetween(
                0, result.prevFrom, result.prevTo);
        long prevActualNew = bookingRecordRepository.countByCustomerStatusAndBookingStatusIdAndBookingDateBetween(
                0, result.prevFrom, result.prevTo);

        double growthTotalNew  = calculateGrowth(prevTotalNew, totalNew);
        double growthActualNew = calculateGrowth(prevActualNew, actualNew);

        // ===== Kỳ hiện tại - khách cũ =====
        long totalOld = bookingRecordRepository.countByCustomerStatusAndBookingDateBetween(
                1, result.fromDate, result.toDate);
        long actualOld = bookingRecordRepository.countByCustomerStatusAndBookingStatusIdAndBookingDateBetween(
                1, result.fromDate, result.toDate);

        // ===== Kỳ trước - khách cũ =====
        long prevTotalOld = bookingRecordRepository.countByCustomerStatusAndBookingDateBetween(
                1, result.prevFrom, result.prevTo);
        long prevActualOld = bookingRecordRepository.countByCustomerStatusAndBookingStatusIdAndBookingDateBetween(
                1, result.prevFrom, result.prevTo);

        double growthTotalOld  = calculateGrowth(prevTotalOld, totalOld);
        double growthActualOld = calculateGrowth(prevActualOld, actualOld);

        return new CustomerSummaryDTO(
                totalNew, actualNew, prevTotalNew, prevActualNew, growthTotalNew, growthActualNew,
                totalOld, actualOld, prevTotalOld, prevActualOld, growthTotalOld, growthActualOld
        );
    }

    public List<DailyCustomerOrderTrendDTO> calculateCustomerOrderTrends(LocalDateTime start, LocalDateTime end) {
        List<SalesTransaction> transactions = salesTransactionRepository.findByOrderDateBetween(start, end);

        Set<String> seenPhones = new HashSet<>();
        Map<LocalDateTime, List<SalesTransaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(tx -> tx.getOrderDate().toLocalDate().atStartOfDay()));

        List<DailyCustomerOrderTrendDTO> result = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<SalesTransaction>> entry : grouped.entrySet()) {
            LocalDateTime date = entry.getKey();
            List<SalesTransaction> dayTx = entry.getValue();

            long newCount = 0;
            long oldCount = 0;

            for (SalesTransaction tx : dayTx) {
                if (tx.getPhoneNumber() != null && seenPhones.add(tx.getPhoneNumber())) {
                    newCount++;
                } else {
                    oldCount++;
                }
            }

            result.add(new DailyCustomerOrderTrendDTO(date, newCount, oldCount));
        }

        return result;
    }

    public CustomerOrderSummaryDTO calculateCustomerOrderSummary(LocalDateTime start, LocalDateTime end) {
        List<SalesTransaction> transactions = salesTransactionRepository.findByOrderDateBetween(start, end);

        Set<String> seenPhones = new HashSet<>();
        long newCustomers = 0;
        long oldCustomers = 0;

        for (SalesTransaction tx : transactions) {
            if (tx.getPhoneNumber() != null && seenPhones.add(tx.getPhoneNumber())) {
                newCustomers++;
            } else {
                oldCustomers++;
            }
        }

        return new CustomerOrderSummaryDTO(newCustomers, oldCustomers);
    }

    public CustomerOrderSummaryDTO calculateGenderSummary(LocalDateTime start, LocalDateTime end) {
        List<CustomerSaleRecord> customers = customerSaleRecordRepository.findAll();
        List<ServiceRecord> services = serviceRecordRepository.findAll().stream()
                .filter(s -> s.getBookingDate() != null && !s.getBookingDate().isBefore(start) && !s.getBookingDate().isAfter(end))
                .collect(Collectors.toList());

        Set<String> servicePhones = services.stream()
                .map(ServiceRecord::getPhoneNumber)
                .collect(Collectors.toSet());

        Map<String, String> phoneToGender = customers.stream()
                .filter(c -> c.getPhoneNumber() != null && c.getGender() != null)
                .collect(Collectors.toMap(CustomerSaleRecord::getPhoneNumber, CustomerSaleRecord::getGender, (g1, g2) -> g1));

        Set<String> counted = new HashSet<>();
        long male = 0;
        long female = 0;

        for (String phone : servicePhones) {
            if (counted.contains(phone)) continue;
            String gender = phoneToGender.get(phone);
            if (gender == null) continue;

            if (gender.equalsIgnoreCase("Nam")) male++;
            else if (gender.equalsIgnoreCase("Nữ")) female++;

            counted.add(phone);
        }

        return new CustomerOrderSummaryDTO(female, male);
    }

    public GenderRevenueDTO calculateGenderRevenue(LocalDateTime start, LocalDateTime end) {
        List<CustomerSaleRecord> customers = customerSaleRecordRepository.findAll();
        Map<String, String> phoneToGender = customers.stream()
                .filter(c -> c.getPhoneNumber() != null && c.getGender() != null)
                .collect(Collectors.toMap(CustomerSaleRecord::getPhoneNumber, CustomerSaleRecord::getGender, (g1, g2) -> g1));

        List<SalesTransaction> sales = salesTransactionRepository.findByOrderDateBetween(start, end);

        Map<String, List<BigDecimal>> revenueMapActualRevenue = new HashMap<>();
        Map<String, List<BigDecimal>> revenueMapFoxieCardRevenue = new HashMap<>();

        for (SalesTransaction tx : sales) {
            String gender = phoneToGender.get(tx.getPhoneNumber()); // dùng phoneToGen để tìm gender
            if (gender != null) {
                revenueMapActualRevenue.computeIfAbsent(gender, k -> new ArrayList<>()).add(tx.getCashTransferCredit());
            }
        }

        for (SalesTransaction tx : sales) {
            String gender = phoneToGender.get(tx.getPhoneNumber()); // dùng phoneToGen để tìm gender
            if (gender != null) {
                revenueMapFoxieCardRevenue.computeIfAbsent(gender, k -> new ArrayList<>()).add(tx.getPrepaidCard());
            }
        }

        BigDecimal avgActualRevenueMale = avg(revenueMapActualRevenue.get("Nam"));
        BigDecimal avgActualRevenueFemale = avg(revenueMapActualRevenue.get("Nữ"));
        BigDecimal avgFoxieRevenueMale = avg(revenueMapFoxieCardRevenue.get("Nam"));
        BigDecimal avgFoxieRevenueFemale = avg(revenueMapFoxieCardRevenue.get("Nữ"));

        return new GenderRevenueDTO(avgActualRevenueMale, avgActualRevenueFemale, avgFoxieRevenueMale, avgFoxieRevenueFemale);
    }

    public PaymentBreakdownDTO calculatePaymentStatus(CustomerReportRequest request, boolean isNew) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        Set<String> knownPhones = customerSaleRecordRepository.findAll().stream()
                .map(CustomerSaleRecord::getPhoneNumber)
                .collect(Collectors.toSet());

        List<SalesTransaction> transactions = salesTransactionRepository.findByOrderDateBetween(start, end);

        if (isNew) {
            transactions = transactions.stream()
                    .filter(tx -> !knownPhones.contains(tx.getPhoneNumber()))
                    .collect(Collectors.toList());
        } else {
            transactions = transactions.stream()
                    .filter(tx -> knownPhones.contains(tx.getPhoneNumber()))
                    .collect(Collectors.toList());
        }

        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalTransfer = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalPrepaid = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;

        for (SalesTransaction tx : transactions) {
            totalCash = totalCash.add(Optional.ofNullable(tx.getCash()).orElse(BigDecimal.ZERO));
            totalTransfer = totalTransfer.add(Optional.ofNullable(tx.getTransfer()).orElse(BigDecimal.ZERO));
            totalCredit = totalCredit.add(Optional.ofNullable(tx.getCreditCard()).orElse(BigDecimal.ZERO));
            totalPrepaid = totalPrepaid.add(Optional.ofNullable(tx.getPrepaidCard()).orElse(BigDecimal.ZERO));
            totalDebt = totalDebt.add(Optional.ofNullable(tx.getDebt()).orElse(BigDecimal.ZERO));
        }

        return new PaymentBreakdownDTO(totalCash, totalTransfer, totalCredit, totalPrepaid, totalDebt);
    }

    public TotalCustomerResponse getCustomerSaleRecord(CustomerReportRequest request) {
        Result result = getResult(request);

        long current  = serviceRecordRepository.findByServiceOrderDateBetween(result.fromDate(), result.toDate());
        long previous = serviceRecordRepository.findByServiceOrderDateBetween(result.prevFrom(), result.prevTo());

        long currentMale    = serviceRecordRepository.countServiceByGenderBetween(result.fromDate(), result.toDate(), "Nam");
        long previousMale   = serviceRecordRepository.countServiceByGenderBetween(result.prevFrom(), result.prevTo(), "Nam");

        long currentFemale  = serviceRecordRepository.countServiceByGenderBetween(result.fromDate(), result.toDate(), "Nữ");
        long previousFemale = serviceRecordRepository.countServiceByGenderBetween(result.prevFrom(), result.prevTo(), "Nữ");

        double changePercentTotal  = pct(current, previous);
        double changePercentMale   = pct(currentMale, previousMale);
        double changePercentFemale = pct(currentFemale, previousFemale);

        return TotalCustomerResponse.builder()
                .currentTotal(current)
                .previousTotal(previous)
                .changePercentTotal(changePercentTotal)
                .currentMale(currentMale)
                .previousMale(previousMale)
                .changePercentMale(changePercentMale)
                .currentFemale(currentFemale)
                .previousFemale(previousFemale)
                .changePercentFemale(changePercentFemale)
                .build();
    }

    public long countUniquePhonesBetweenRange(CustomerReportRequest request) {
        List<String> allPhones = Stream.of(
                        appUsageRecordRepository.findPhonesBetweenInstalledAt(request.getFromDate(), request.getToDate()),
                        bookingRecordRepository.findPhonesBetweenCreatedDate(request.getFromDate(), request.getToDate()),
                        customerSaleRecordRepository.findPhonesBetweenCreatedAt(request.getFromDate(), request.getToDate()),
                        salesTransactionRepository.findPhonesBetweenOrderDate(request.getFromDate(), request.getToDate()),
                        serviceRecordRepository.findPhonesBetweenBookingDate(request.getFromDate(), request.getToDate())
                ).flatMap(Collection::stream) // gộp tất cả list thành một stream duy nhất
                .filter(Objects::nonNull) // filter loại bỏ những phần tử null
                .map(String::trim) // loại bỏ khoảng trắng thừa ở đầu và cuối
                .filter(p -> !p.isEmpty()) // sau khi trim(), nếu chuỗi rỗng " " thì loại bỏ
                .map(p -> p.replaceAll("[^0-9]", "")) // dùng regex xóa hết tất cả ký tự không phải số
                .filter(p -> p.length() >= 8) // filter độ dài tối thiểu 8 kí tự
                .collect(Collectors.toList()); // thu về kết quả hợp lệ match với các case trên vào list

        return allPhones.stream().distinct().count(); // đếm số lượng điện thoại distinct
    }

    public long countUniquePhones() {
        List<String> allPhones = Stream.of(
                        appUsageRecordRepository.findPhones(),
                        bookingRecordRepository.findPhones(),
                        customerSaleRecordRepository.findPhones(),
                        salesTransactionRepository.findPhones(),
                        serviceRecordRepository.findPhones()
                ).flatMap(Collection::stream) // gộp tất cả list thành một stream duy nhất
                .filter(Objects::nonNull) // filter loại bỏ những phần tử null
                .map(String::trim) // loại bỏ khoảng trắng thừa ở đầu và cuối
                .filter(p -> !p.isEmpty()) // sau khi trim(), nếu chuỗi rỗng " " thì loại bỏ
                .map(p -> p.replaceAll("[^0-9]", "")) // dùng regex xóa hết tất cả ký tự không phải số
                .filter(p -> p.length() >= 8) // filter độ dài tối thiểu 8 kí tự
                .collect(Collectors.toList()); // thu về kết quả hợp lệ match với các case trên vào list

        return allPhones.stream().distinct().count(); // đếm số lượng điện thoại distinct
    }

    public List<PhoneExportDTO> exportFullPhoneNumbers() {
        return Stream.of(
                        appUsageRecordRepository
                                .findPhones()
                                .stream()
                                .map(p -> new PhoneExportDTO(p, CustomerSource.APP)),

                        bookingRecordRepository
                                .findPhones()
                                .stream()
                                .map(p -> new PhoneExportDTO(p, CustomerSource.BOOKING)),

                        customerSaleRecordRepository
                                .findPhones()
                                .stream()
                                .map(p -> new PhoneExportDTO(p, CustomerSource.CUSTOMER)),

                        salesTransactionRepository
                                .findPhones()
                                .stream()
                                .map(p -> new PhoneExportDTO(p, CustomerSource.SALES)),

                        serviceRecordRepository
                                .findPhones()
                                .stream()
                                .map(p -> new PhoneExportDTO(p, CustomerSource.SERVICE))
                )
                .flatMap(s -> s)
                .map(dto -> normalize(dto))        // chuẩn hoá phone
                .filter(Objects::nonNull)
                // DISTINCT THEO PHONE → ưu tiên source xuất hiện trước
                .collect(Collectors.toMap(
                        PhoneExportDTO::getPhone,
                        dto -> dto,
                        (oldVal, newVal) -> oldVal
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(PhoneExportDTO::getPhone))
                .collect(Collectors.toList());
    }

    private PhoneExportDTO normalize(PhoneExportDTO dto) {

        if (dto.getPhone() == null) {
            return null;
        }

        String phone = dto.getPhone().trim()
                .replaceAll("[^0-9]", "");

        if (phone.length() < 8) {
            return null;
        }

        return new PhoneExportDTO(phone, dto.getSource());
    }



    public static Result getResult(CustomerReportRequest request) {
        // Chuẩn hóa mốc thời gian trong ngày
        LocalDateTime fromDate = request.getFromDate().with(LocalTime.MIN); // 00:00:00
        LocalDateTime toDate   = request.getToDate().with(LocalTime.MAX);   // 23:59:59.999999999

        LocalDateTime prevFrom;
        LocalDateTime prevTo;

        if (isWholeMonthSelection(fromDate, toDate)) {
            // Lấy tháng trước của "fromDate"
            LocalDate firstOfCurrentMonth = fromDate.toLocalDate().withDayOfMonth(1);
            LocalDate firstOfPrevMonth = firstOfCurrentMonth.minusMonths(1);
            LocalDate lastOfPrevMonth  = firstOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());

            prevFrom = firstOfPrevMonth.atTime(LocalTime.MIN);
            prevTo   = lastOfPrevMonth.atTime(LocalTime.MAX);
        } else {
            // Giữ logic cũ: dịch lùi theo số ngày (tính cả 2 đầu)
            long daysBetween = ChronoUnit.DAYS.between(fromDate.toLocalDate(), toDate.toLocalDate()) + 1;
            prevFrom = fromDate.minusDays(daysBetween);
            prevTo   = toDate.minusDays(daysBetween);
        }
        Result result = new Result(fromDate, toDate, prevFrom, prevTo);
        return result;
    }

    public record Result(LocalDateTime fromDate, LocalDateTime toDate, LocalDateTime prevFrom, LocalDateTime prevTo) {
    }

    public List<HourlyFacilityStatsDTO> getHourlyStats(LocalDateTime start, LocalDateTime end) {
        List<ServiceRecord> records = serviceRecordRepository.findByBookingDateBetween(start, end);

        Map<String, HourlyFacilityStatsDTO> facilityStatsMap = new HashMap<>();

        for (ServiceRecord record : records) {
            if (record.getBookingDate() == null || record.getFacility() == null) continue;

            String facility = record.getFacility().getShop_name();
            int hour = record.getBookingDate().getHour();

            facilityStatsMap
                    .computeIfAbsent(facility, HourlyFacilityStatsDTO::new)
                    .addCount(hour, 1);
        }

        return facilityStatsMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotal(), a.getTotal())) // sort descending
                .collect(Collectors.toList());
    }

}
