package com.example.BasicCRM_FWF.Service.ServiceRecord;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Model.*;
import com.example.BasicCRM_FWF.Repository.*;
import com.example.BasicCRM_FWF.Service.CustomerSaleRecord.CustomerSaleRecordService;
import com.example.BasicCRM_FWF.Utils.ServiceUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.BasicCRM_FWF.Service.CustomerSaleRecord.CustomerSaleRecordService.getResult;
import static com.example.BasicCRM_FWF.Utils.ServiceUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRecordService implements ServiceRecordInterface {

    private final ServiceRecordRepository repository;
    private final RegionRepository regionRepository;
    private final AppliedCardRepository appliedCardRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeTempRepository serviceTypeTempRepository;

    @Override
    @Transactional
    public void importFromExcelOrigin(MultipartFile file) {

        final int BATCH_SIZE = 500;

        int success = 0;
        int failed = 0;

        List<ServiceRecord> batch = new ArrayList<>(BATCH_SIZE);

        try (InputStream is = file.getInputStream()) {

            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            // ===== CACHE MASTER DATA =====
            Map<String, Region> regionMap =
                    regionRepository.findAll().stream()
                            .collect(Collectors.toMap(
                                    r -> r.getShop_name().trim().toLowerCase(),
                                    Function.identity(),
                                    (a, b) -> a
                            ));

            Map<String, ServiceTypeTemp> serviceTypeMap =
                    serviceTypeTempRepository.findAll().stream()
                            .collect(Collectors.toMap(
                                    s -> s.getService_name().trim().toLowerCase(),
                                    Function.identity(),
                                    (a, b) -> a
                            ));

            Map<String, AppliedCard> appliedCardMap =
                    appliedCardRepository.findAll().stream()
                            .collect(Collectors.toMap(
                                    c -> c.getCard_name().trim().toLowerCase(),
                                    Function.identity(),
                                    (a, b) -> a
                            ));

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    // ===== DATE =====
                    String dateStr = getCellValue(row.getCell(3));
                    if (dateStr == null || dateStr.isBlank()) {
                        failed++;
                        continue;
                    }

                    LocalDateTime bookingDate;
                    try {
                        bookingDate = LocalDateTime.parse(
                                dateStr,
                                DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                        );
                    } catch (Exception e) {
                        failed++;
                        continue;
                    }

                    // ===== REGION =====
                    String regionKey = ServiceUtils.getCellValue(row.getCell(4));
                    Region facility = regionKey == null
                            ? null
                            : regionMap.get(regionKey.trim().toLowerCase());

                    // ===== SERVICE TYPE (AUTO INSERT) =====
                    String serviceKey = ServiceUtils.getCellValue(row.getCell(8));
                    ServiceTypeTemp serviceType = null;

                    if (serviceKey != null && !serviceKey.isBlank()) {
                        String normalizedServiceKey = serviceKey.trim().toLowerCase();
                        serviceType = serviceTypeMap.get(normalizedServiceKey);

                        if (serviceType == null) {
                            serviceType = ServiceTypeTemp.builder()
                                    .service_name(serviceKey.trim())
                                    .build();

                            serviceType = serviceTypeTempRepository.save(serviceType);
                            serviceTypeMap.put(normalizedServiceKey, serviceType);

                            log.warn("AUTO INSERT ServiceTypeTemp: '{}'", serviceKey);
                        }
                    }

                    // ===== APPLIED CARD (AUTO INSERT) =====
                    String cardKey = ServiceUtils.getCellValue(row.getCell(9));
                    AppliedCard appliedCard = null;

                    if (cardKey != null && !cardKey.isBlank()) {
                        String normalizedCardKey = cardKey.trim().toLowerCase();
                        appliedCard = appliedCardMap.get(normalizedCardKey);

                        if (appliedCard == null) {
                            appliedCard = AppliedCard.builder()
                                    .card_name(cardKey.trim())
                                    .build();

                            appliedCard = appliedCardRepository.save(appliedCard);
                            appliedCardMap.put(normalizedCardKey, appliedCard);

                            log.warn("AUTO INSERT AppliedCard: '{}'", cardKey);
                        }
                    }

                    // ===== SERVICE RECORD =====
                    ServiceRecord record = ServiceRecord.builder()
                            .recordId(ServiceUtils.getCellValue(row.getCell(1)))
                            .orderId(ServiceUtils.getCellValue(row.getCell(2)))
                            .bookingDate(bookingDate)
                            .facility(facility)
                            .customerName(ServiceUtils.getCellValue(row.getCell(5)))
                            .phoneNumber(ServiceUtils.getCellValue(row.getCell(6)))
                            .customerType(ServiceUtils.getCellValue(row.getCell(7)))
                            .baseService(serviceType)
                            .appliedCard(appliedCard)
                            .shiftEmployee(ServiceUtils.getCellValue(row.getCell(10)))
                            .performingEmployee(ServiceUtils.getCellValue(row.getCell(11)))
                            .employeeSalary(toBigDecimal(row.getCell(12)))
                            .build();

                    batch.add(record);
                    success++;

                    // ===== FLUSH BATCH =====
                    if (batch.size() >= BATCH_SIZE) {
                        repository.saveAll(batch);
                        batch.clear();
                        log.info("Imported {} service records...", success);
                    }

                } catch (Exception e) {
                    failed++;
                    log.warn("Row {} failed", i, e);
                }
            }

            // ===== FLUSH REMAIN =====
            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }

            log.info("IMPORT SERVICE RECORD DONE → Success={}, Failed={}", success, failed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import service record Excel", e);
        }
    }

    @Override
    public void importSaleServiceFile(MultipartFile file) {
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    log.info("Stopped at row {} (blank)", i);
                    break;
                }

                try {
                    String service_name = ServiceUtils.getCellValue(row.getCell(2));
                    String service_code = ServiceUtils.getCellValue(row.getCell(3));
                    String price = ServiceUtils.getCellValue(row.getCell(5));
                    String category = ServiceUtils.getCellValue(row.getCell(6));

                    if (service_code == null || service_name == null || price == null || category == null) {
                        log.warn("Row {} skipped: missing required fields", i);
                        failCount++;
                        continue;
                    }

                    // ✅ Chuẩn hoá service_name
                    service_name = service_name.trim().replaceAll("\\s+", " ");

                    // ✅ Kiểm tra tồn tại theo code hoặc name
                    ServiceType byCode = serviceTypeRepository.findByCode(service_code);
                    ServiceType byName = serviceTypeRepository.findByName(service_name);

                    if (byCode != null || byName != null) {
                        log.warn("Row {} skipped: ServiceType đã tồn tại (code='{}', name='{}')",
                                i, service_code, service_name);
                        skippedCount++;
                        continue;
                    }

                    // ✅ Nếu chưa có thì insert
                    ServiceType st = ServiceType.builder()
                            .service_name(service_name)
                            .service_code(service_code)
                            .price(new BigDecimal(price))
                            .category(category)
                            .build();

                    serviceTypeRepository.save(st);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    log.error("Row {} failed: {}", i, e.getMessage(), e);
                }
            }

            log.info("IMPORT SERVICE RECORD: Success = {}, Skipped = {}, Failed = {}",
                    successCount, skippedCount, failCount);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import service record Excel", e);
        }
    }

    @Override
    public List<DailyServiceTypeStatDTO> getServiceTypeBreakdown(CustomerReportRequest request) {
        List<Object[]> raw = repository.countServiceTypesPerDay(request.getFromDate(), request.getToDate());
        return raw.stream()
                .map(obj -> new DailyServiceTypeStatDTO(
                        ((Date) obj[0]).toLocalDate(),
                        (String) obj[1],
                        ((Number) obj[2]).longValue()
                )).collect(Collectors.toList());
    }

    @Override
    public ServiceSummaryDTO getServiceSummary(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);

        long combo = count("combo", r.fromDate(), r.toDate());
        long le = count("dv", r.fromDate(), r.toDate());
        long ct = count("ct", r.fromDate(), r.toDate());
        long gift = count("quà tặng", r.fromDate(), r.toDate());
        long total = combo + le + ct + gift;

        long prevCombo = count("combo", r.prevFrom(), r.prevTo());
        long prevLe = count("dv", r.prevFrom(), r.prevTo());
        long prevCT = count("ct", r.prevFrom(), r.prevTo());
        long prevGift = count("quà tặng", r.prevFrom(), r.prevTo());
        long prevTotal = prevCombo + prevLe + prevCT + prevGift;

        return new ServiceSummaryDTO(
                combo, le, ct, gift, total,
                prevCombo, prevLe, prevCT, prevGift, prevTotal,
                calculateGrowth(prevCombo, combo),
                calculateGrowth(prevLe, le),
                calculateGrowth(prevCT, ct),
                calculateGrowth(prevGift, gift),
                calculateGrowth(prevTotal, total)
        );
    }

    private long count(String prefix, LocalDateTime start, LocalDateTime end) {
        return repository.countByServiceCodePrefix(prefix, start, end);
    }

    @Override
    public List<RegionServiceTypeUsageDTO> getServiceUsageByRegion(CustomerReportRequest request) {
        List<Object[]> result = repository.findRegionServiceTypeCount(
                request.getFromDate(), request.getToDate()
        );

        return result.stream().map(r -> new RegionServiceTypeUsageDTO(
                r[0].toString(),
                r[1].toString(),
                ((Number) r[2]).longValue()
        )).collect(Collectors.toList());
    }

    @Override
    public List<ServiceUsageDTO> getServiceUsageByShop(CustomerReportRequest request) {
        List<Object[]> raw = repository.findServiceUsageByShop(request.getFromDate(), request.getToDate());

        // Convert each result row into a DTO
        return raw.stream().map(obj -> new ServiceUsageDTO(
                obj[0].toString(),  // shop name
                obj[1].toString(),  // service type
                ((Number) obj[2]).intValue() // total count
        )).collect(Collectors.toList());
    }

    @Override
    public List<TopServiceUsage> getTop10ServiceUsage(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        // Map raw query result into DTOs
        return repository.findTop10ServiceNames(start, end).stream()
                .map(row -> new TopServiceUsage(
                        row[0] != null ? row[0].toString() : "Không xác định",
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<TopServiceRevenue> getTop10ServicesByRevenue(CustomerReportRequest request) {
        List<Object[]> rawResults = repository.findTop10ServicesByRevenue(
                request.getFromDate(),
                request.getToDate()
        );

        // Convert raw query result to typed DTOs
        return rawResults.stream()
                .map(obj -> new TopServiceRevenue(
                        obj[0] != null ? obj[0].toString() : "Không xác định", // Service name or fallback
                        obj[1] != null ? (BigDecimal) obj[1] : BigDecimal.ZERO // Convert revenue
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<TopServiceRevenue> getBottom3ServiceRevenue(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        return repository.findTopBottomServicesRevenue(start, end).stream()
                .map(objects -> new TopServiceRevenue(
                        objects[0] != null ? objects[0].toString() : "Không xác định",
                        objects[1] != null ? (BigDecimal) objects[1] : BigDecimal.ZERO
                )).collect(Collectors.toList());
    }

    @Override
    public List<TopServiceUsage> getBottom3ServicesUsage(CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        return repository.findTopBottomServicesUsage(start, end).stream()
                .map(objects -> new TopServiceUsage(
                        objects[0] != null ? objects[0].toString() : "Không xác định",
                        ((Number) objects[1]).longValue()
                )).collect(Collectors.toList());
    }

    @Override
    public List<ServiceStatsDTO> getTopServiceTable(CustomerReportRequest request) {
        CustomerSaleRecordService.Result result = getResult(request);
        Map<String, Object[]> previousData = repository.findTop10ServicesWithPreviousData(result.prevFrom(), result.prevTo())
                .stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> row
                ));

        List<Object[]> currentData = repository.findTop10ServicesWithCurrentData(result.fromDate(), result.toDate());
        long totalUsage = currentData.stream().mapToLong(r -> ((Number) r[2]).longValue()).sum();
        BigDecimal totalRevenue = currentData.stream()
                .map(r -> (BigDecimal) r[3])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return currentData.stream().map(row -> {
            String name = row[0].toString();
            String type = row[1].toString();
            long currentCount = ((Number) row[2]).longValue();
            BigDecimal currentRevenue = (BigDecimal) row[3];

            Object[] prev = previousData.get(name);
            long prevCount = prev != null ? ((Number) prev[1]).longValue() : 0;
            BigDecimal prevRevenue = prev != null && prev[2] != null ? (BigDecimal) prev[2] : BigDecimal.ZERO;

            long deltaCount = currentCount - prevCount;
            double deltaRevenuePct = prevRevenue.compareTo(BigDecimal.ZERO) == 0 ? 100.0 :
                    currentRevenue.subtract(prevRevenue).multiply(BigDecimal.valueOf(100)).divide(prevRevenue, 2, RoundingMode.HALF_UP).doubleValue();

            double usagePct = totalUsage == 0 ? 0.0 : ((double) currentCount / totalUsage) * 100.0;
            double revenuePct = totalRevenue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                    currentRevenue.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue();

            return ServiceStatsDTO.builder()
                    .serviceName(name)
                    .type(type)
                    .usageCount(currentCount)
                    .usageDeltaCount(deltaCount)
                    .usagePercent(usagePct)
                    .totalRevenue(currentRevenue)
                    .revenueDeltaPercent(deltaRevenuePct)
                    .revenuePercent(revenuePct)
                    .build();
        }).collect(Collectors.toList());
    }
}
