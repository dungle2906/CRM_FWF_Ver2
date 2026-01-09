package com.example.BasicCRM_FWF.Service.SalesTransaction;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Model.Region;
import com.example.BasicCRM_FWF.Model.SaleServiceItem;
import com.example.BasicCRM_FWF.Model.SalesTransaction;
import com.example.BasicCRM_FWF.Model.ServiceType;
import com.example.BasicCRM_FWF.Repository.RegionRepository;
import com.example.BasicCRM_FWF.Repository.SaleServiceItemRepository;
import com.example.BasicCRM_FWF.Repository.SalesTransactionRepository;
import com.example.BasicCRM_FWF.Repository.ServiceTypeRepository;
import com.example.BasicCRM_FWF.Service.CustomerSaleRecord.CustomerSaleRecordService;
import com.example.BasicCRM_FWF.Utils.ServiceUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.BasicCRM_FWF.Service.CustomerSaleRecord.CustomerSaleRecordService.getResult;
import static com.example.BasicCRM_FWF.Utils.ServiceUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesTransactionService implements SalesTransactionInterface {

    private final SalesTransactionRepository repository;
    private final RegionRepository regionRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SaleServiceItemRepository saleServiceItemRepository;

    @Transactional
    public void importFromExcel(MultipartFile file) {

        final int BATCH_SIZE = 500;

        int successCount = 0;
        int failCount = 0;

        List<SalesTransaction> batchST = new ArrayList<>();
        List<SaleServiceItem> batchItems = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {

            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            // ===== Cache Region =====
            Map<String, Region> regionMap = regionRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(
                            r -> r.getShop_name().trim().toLowerCase(),
                            Function.identity(),
                            (a, b) -> a
                    ));

            // ===== Cache ServiceType =====
            List<ServiceType> allServiceTypes = serviceTypeRepository.findAll();

            Map<String, ServiceType> serviceTypeByName =
                    allServiceTypes.stream()
                            .filter(st -> st.getService_name() != null)
                            .collect(Collectors.toMap(
                                    st -> normalizeServiceName(st.getService_name()),
                                    Function.identity(),
                                    (a, b) -> a
                            ));

            Map<String, ServiceType> serviceTypeByCode =
                    allServiceTypes.stream()
                            .filter(st -> st.getService_code() != null)
                            .collect(Collectors.toMap(
                                    st -> st.getService_code().trim(),
                                    Function.identity(),
                                    (a, b) -> a
                            ));

            // ===== Loop rows =====
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String orderCode = ServiceUtils.getCellValue(row.getCell(1));
                    String rawShop = ServiceUtils.getCellValue(row.getCell(2));
                    String dateTimeStr = ServiceUtils.getCellValue(row.getCell(3));

                    if (orderCode.startsWith("#")) {
                        orderCode = orderCode.substring(1);
                    }

                    if (orderCode == null || rawShop == null || dateTimeStr == null) {
                        failCount++;
                        continue;
                    }

                    LocalDateTime orderDate;
                    try {
                        orderDate = LocalDateTime.parse(
                                dateTimeStr,
                                DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                        );
                    } catch (Exception e) {
                        log.error("Row {} invalid datetime '{}'", i, dateTimeStr);
                        failCount++;
                        continue;
                    }

                    Region region = regionMap.get(rawShop.trim().toLowerCase());
                    if (region == null) {
                        log.warn("Row {}: Region not found '{}'", i, rawShop);
                        failCount++;
                        continue;
                    }

                    // ===== Parse services =====
                    Map<ServiceType, Integer> serviceTypeMap =
                            ServiceUtils.parseServicesPerBill(
                                    ServiceUtils.getCellValue(row.getCell(13)),
                                    serviceTypeByName,
                                    serviceTypeByCode,
                                    i
                            );

                    SalesTransaction st = SalesTransaction.builder()
                            .orderCode(orderCode)
                            .facility(region)
                            .orderDate(orderDate)
                            .customerCode(ServiceUtils.getCellValue(row.getCell(4)))
                            .customerName(ServiceUtils.getCellValue(row.getCell(5)))
                            .phoneNumber(ServiceUtils.getCellValue(row.getCell(6)))
                            .cash(toBigDecimal(row.getCell(8)))
                            .transfer(toBigDecimal(row.getCell(9)))
                            .creditCard(toBigDecimal(row.getCell(10)))
                            .wallet(toBigDecimal(row.getCell(11)))
                            .prepaidCard(toBigDecimal(row.getCell(12)))
                            .build();

                    batchST.add(st);

                    serviceTypeMap.forEach((serviceType, qty) ->
                            batchItems.add(SaleServiceItem.builder()
                                    .salesTransaction(st)
                                    .serviceType(serviceType)
                                    .quantity(qty)
                                    .build())
                    );

                    successCount++;

                    if (batchST.size() >= BATCH_SIZE) {
                        repository.saveAll(batchST);
                        saleServiceItemRepository.saveAll(batchItems);
                        batchST.clear();
                        batchItems.clear();
                        log.info("Imported {} rows...", successCount);
                    }

                } catch (Exception e) {
                    log.error("Row {} failed", i, e);
                    failCount++;
                }
            }

            if (!batchST.isEmpty()) {
                repository.saveAll(batchST);
                saleServiceItemRepository.saveAll(batchItems);
            }

            log.info("IMPORT DONE → Success={}, Failed={}", successCount, failCount);

        } catch (Exception e) {
            throw new RuntimeException("Import Excel failed", e);
        }
    }

    /* public void importFromExcel(MultipartFile file) {
        int successCount = 0;
        int failCount = 0;
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
                    String orderCodeStr = ServiceUtils.getCellValue(row.getCell(1));
                    String dateTimeStr = ServiceUtils.getCellValue(row.getCell(3));

                    if (orderCodeStr == null || dateTimeStr == null) {
                        log.warn("Row {} skipped: missing required fields", i);
                        failCount++;
                        continue;
                    }

                    LocalDateTime orderDate = LocalDateTime.parse(
                            dateTimeStr,
                            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                    );

                    // ✅ Tra Region bằng shop name (cột CƠ SỞ trong Excel, cột 11 tính từ 0)
                    String shopName = ServiceUtils.getCellValue(row.getCell(2)).trim().toLowerCase();
                    Region facilityRecordService = regionMap.get(shopName);

                    if (facilityRecordService == null) {
                        log.warn("Row {} skipped: Không tìm thấy Region cho tên '{}'", i, shopName);
                        failed++;
                        continue;
                    }

                    Map<ServiceType, Integer> serviceTypeMap = new HashMap<>();
                    String allComboString = ServiceUtils.getCellValue(row.getCell(26)).trim().replaceAll("\\s+", " ");
                    int start = 0;
                    while (true) {
                        int idx = allComboString.indexOf(";", start);
                        String originalString = (idx == -1)
                                ? allComboString.substring(start).trim()
                                : allComboString.substring(start, idx).trim();

//                        Set<String> printedPerfectStrings = new HashSet<>();

                        if (!originalString.isEmpty()) {
                            Pair<String, Integer> parsed = extractQuantityAndCleanName(originalString);
                            String perfectString = parsed.getLeft();
                            int quantity = parsed.getRight();

                            if (perfectString.endsWith("))")) {
                                perfectString = perfectString.substring(0, perfectString.length() - 1);
                            } else if (perfectString.endsWith(" )")) {
                                int open = perfectString.lastIndexOf("(");
                                int close = perfectString.lastIndexOf(")");
                                if (open != -1 && close != -1 && close > open) {
                                    String tag = perfectString.substring(open + 1, close).trim();
                                }
                            }

                            // 🔥 PRINT PERFECT STRING **if not duplicated**
//                            if (!printedPerfectStrings.contains(perfectString)) {
//                                printedPerfectStrings.add(perfectString);
//                            }

                            // Tiếp tục logic cũ
                            ServiceType serviceType = getServiceType(perfectString);
                            if (serviceType != null && quantity > 0) {
                                serviceTypeMap.merge(serviceType, quantity, Integer::sum);
                            } else {
                                if (serviceType != null) {
                                    serviceTypeMap.merge(serviceType, 1, Integer::sum);
                                } else {
                                    log.warn("Row {}: Không tìm thấy ServiceType hoặc quantity=0 cho '{}'", i, perfectString);
                                }
                            }
                        }

                        if (idx == -1) break;
                        start = idx + 1;
                    }

                    SalesTransaction st = SalesTransaction.builder()
                            .facility(facilityRecordService)
                            .orderDate(orderDate)
                            .customerName(ServiceUtils.getCellValue(row.getCell(5)))
                            .phoneNumber(ServiceUtils.getCellValue(row.getCell(6)))
                            .cashTransferCredit(toBigDecimal(ServiceUtils.getCellValue(row.getCell(17)).isBlank() ? null : row.getCell(17)))
                            .cash(toBigDecimal(ServiceUtils.getCellValue(row.getCell(18)).isBlank() ? null : row.getCell(18)))
                            .transfer(toBigDecimal(ServiceUtils.getCellValue(row.getCell(19)).isBlank() ? null : row.getCell(19)))
                            .creditCard(toBigDecimal(ServiceUtils.getCellValue(row.getCell(20)).isBlank() ? null : row.getCell(20)))
                            .wallet(toBigDecimal(ServiceUtils.getCellValue(row.getCell(21)).startsWith("0") ? null : row.getCell(21)))
                            .prepaidCard(toBigDecimal(ServiceUtils.getCellValue(row.getCell(22)).startsWith("0") ? null : row.getCell(22)))
                            .build();
                    repository.save(st);

                    List<SaleServiceItem> items = serviceTypeMap.entrySet().stream()
                            .map(entry -> SaleServiceItem.builder()
                                    .salesTransaction(st)
                                    .serviceType(entry.getKey())
                                    .quantity(entry.getValue())
                                    .build())
                            .collect(Collectors.toList());
                    saleServiceItemRepository.saveAll(items);

                    successCount++;
//                    log.info("Successfully processed {} SalesTransaction records", successCount);

                } catch (Exception e) {
                    log.error("Row {} failed: {}", i, e.getMessage());
                    failCount++;
                }
            }

            log.info("IMPORT COMPLETE: Success = {}, Failed = {}", successCount, failCount);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel", e);
        }
    } */

    public List<RegionRevenueDTO> getRevenueByRegion(CustomerReportRequest request) {
        List<Object[]> rawData = repository.fetchRevenueByRegionAndDate(request.getFromDate(), request.getToDate());

        return rawData.stream()
                .map(row -> new RegionRevenueDTO(
                        (String) row[0],
                        ((Date) row[1]).toLocalDate(), // ✅ sửa ở đây
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());
    }

    public List<ShopTypeRevenueDTO> getRevenueByShopType(CustomerReportRequest request) {
        List<Object[]> rawData = repository.fetchRevenueByShopTypeAndDate(request.getFromDate(), request.getToDate());

        return rawData.stream()
                .map(row -> new ShopTypeRevenueDTO(
                        (String) row[0],
                        ((Date) row[1]).toLocalDate().atStartOfDay(),
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());
    }

    public RevenueSummaryDTO getRevenueSummary(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);
        BigDecimal total = repository.fetchRevenueSummary(r.fromDate(), r.toDate( ));
        BigDecimal actual = repository.fetchActualRevenueSummary(r.fromDate(), r.toDate());

        BigDecimal prevTotal = repository.fetchRevenueSummary(r.prevFrom(), r.prevTo());
        BigDecimal prevActual = repository.fetchActualRevenueSummary(r.prevFrom(), r.prevTo());

        double growthTotal = calculateGrowthBigDecimal(prevTotal, total);
        double growthActual = calculateGrowthBigDecimal(prevActual, actual);

        return new RevenueSummaryDTO(total, actual, growthTotal, growthActual);
    }

    public List<RegionRevenueStatDTO> getStatus(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);

        // Kỳ hiện tại
        Map<String, Object[]> current = toMap(repository.fetchOrderAndRevenueByRegion(r.fromDate(), r.toDate()));

        // Kỳ trước
        Map<String, Object[]> previous = toMap(repository.fetchOrderAndRevenueByRegion(r.prevFrom(), r.prevTo()));

        List<RegionRevenueStatDTO> result = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (String region : current.keySet()) {
            Number currOrderNum = (Number) current.get(region)[0];
            long currOrders = currOrderNum.longValue();
            BigDecimal currRevenue = (BigDecimal) current.get(region)[1];

            long prevOrders = 0;
            if (previous.containsKey(region)) {
                Number prevOrderNum = (Number) previous.get(region)[0];
                prevOrders = prevOrderNum.longValue();
            }

            long delta = currOrders - prevOrders;
            double growth = prevOrders == 0 ? 100.0 : ((double) delta / prevOrders) * 100.0;

            result.add(new RegionRevenueStatDTO(
                    region,
                    currOrders,
                    delta,
                    currRevenue,
                    growth,
                    0.0 // placeholder, sẽ tính sau
            ));

            totalRevenue = totalRevenue.add(currRevenue);
        }

        // Tính phần trăm đóng góp doanh thu
        for (RegionRevenueStatDTO dto : result) {
            dto.setRevenuePercent(
                    totalRevenue.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                            dto.getRevenue().multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue()
            );
        }

        return result;
    }

    public List<RegionRevenuePieDTO> getActualRevenuePie(CustomerReportRequest request) {
        List<Object[]> raw = repository.fetchActualRevenueByRegion(request.getFromDate(), request.getToDate());

        BigDecimal total = raw.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream()
                .map(row -> {
                    String region = (String) row[0];
                    BigDecimal revenue = (BigDecimal) row[1];
                    double percent = total.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                            revenue.multiply(BigDecimal.valueOf(100))
                                    .divide(total, 2, RoundingMode.HALF_UP).doubleValue();
                    return new RegionRevenuePieDTO(region, revenue, percent);
                })
                .toList();
    }

    public List<DailyShopTypeRevenueDTO> getDailyRevenueByShopType(CustomerReportRequest request) {
        List<Object[]> raw = repository.getDailyRevenueByShopType(request.getFromDate(), request.getToDate());

        return raw.stream()
                .map(obj -> new DailyShopTypeRevenueDTO(
                        obj[0] instanceof Date date ? date.toLocalDate().atStartOfDay() : null,
                        (String) obj[1],
                        (BigDecimal) obj[2]
                ))
                .collect(Collectors.toList());
    }

    public List<DailyCustomerTypeRevenueDTO> getRevenueByCustomerTypePerDay(CustomerReportRequest request) {
        List<Object[]> rawData = repository
                .findRevenueByCustomerTypeAndDate(request.getFromDate(), request.getToDate());

        return rawData.stream().map(obj -> new DailyCustomerTypeRevenueDTO(
                ((Date) obj[1]).toLocalDate(),
                obj[0] == null || obj[0].toString().isBlank() ? "Không xác định" : obj[0].toString(),
                (BigDecimal) obj[2]
        )).collect(Collectors.toList());
    }

    public List<TopStoreRevenueDTO> getTopStoreRevenue(CustomerReportRequest request) {
        List<Object[]> rawData = repository.findTop10StoreRevenue(request.getFromDate(), request.getToDate());
        return rawData.stream().map(row -> new TopStoreRevenueDTO(
                (String) row[0],
                (BigDecimal) row[1],
                (BigDecimal) row[2]
        )).collect(Collectors.toList());
    }

    public List<StoreRevenueStatDTO> getFullStoreRevenueStats(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);
        List<Object[]> current = repository.findStoreRevenueStatsBetween(r.fromDate(), r.toDate());
        List<Object[]> previous = repository.findStoreRevenueStatsBetween(r.prevFrom(), r.prevTo());

        Map<String, Object[]> prevMap = previous.stream()
                .collect(Collectors.toMap(row -> row[0].toString(), Function.identity()));

        List<StoreRevenueStatDTO> result = new ArrayList<>();

        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalPrepaid = BigDecimal.ZERO;
        long totalOrders = 0;

        for (Object[] row : current) {
            String name = row[0].toString();
            long orders = ((Number) row[1]).longValue();
            BigDecimal cashTransfer = (BigDecimal) row[2];
            BigDecimal prepaidCard = (BigDecimal) row[3];

            Object[] prevRow = prevMap.getOrDefault(name, new Object[]{name, 0L, BigDecimal.ZERO, BigDecimal.ZERO});
            long prevOrders = ((Number) prevRow[1]).longValue();
            BigDecimal prevCash = (BigDecimal) prevRow[2];

            long delta = orders - prevOrders;
            double growth = prevCash.compareTo(BigDecimal.ZERO) == 0 ? 100.0 :
                    cashTransfer.subtract(prevCash)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(prevCash, 2, RoundingMode.HALF_UP)
                            .doubleValue();

            result.add(new StoreRevenueStatDTO(name, orders, delta, cashTransfer, prepaidCard, growth, 0.0, 0.0, 0.0));

            totalCash = totalCash.add(cashTransfer);
            totalPrepaid = totalPrepaid.add(prepaidCard);
            totalOrders += orders;
        }

        for (StoreRevenueStatDTO dto : result) {
            dto.setCashPercent(
                    totalCash.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                            dto.getCashTransfer().multiply(BigDecimal.valueOf(100)).divide(totalCash, 2, RoundingMode.HALF_UP).doubleValue()
            );
            dto.setPrepaidPercent(
                    totalPrepaid.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                            dto.getPrepaidCard().multiply(BigDecimal.valueOf(100)).divide(totalPrepaid, 2, RoundingMode.HALF_UP).doubleValue()
            );
            dto.setOrderPercent(
                    totalOrders == 0 ? 0.0 :
                            ((double) dto.getCurrentOrders() * 100) / totalOrders
            );
        }

        return result;
    }

    public List<DailyShopOrderStatDTO> getDailyOrderStats(CustomerReportRequest request) {
        List<Object[]> rawData = repository.findDailyOrderAndShopStats(request.getFromDate(), request.getToDate());

        return rawData.stream()
                .map(obj -> new DailyShopOrderStatDTO(
                        ((Date) obj[0]).toLocalDate(),
                        ((Number) obj[1]).longValue(),
                        ((Number) obj[2]).intValue()
                ))
                .collect(Collectors.toList());
    }

    public List<DailyRegionRevenueDTO> getDailyRevenue(CustomerReportRequest request) {
        List<Object[]> raw = repository.fetchDailyRevenueByRegion(request.getFromDate(), request.getToDate());
        List<DailyRegionRevenueDTO> result = new ArrayList<>();

        for (Object[] row : raw) {
            Date sqlDate = (Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            String region = (String) row[1];
            BigDecimal revenue = (BigDecimal) row[2];

            result.add(new DailyRegionRevenueDTO(date, region, revenue));
        }

        return result;
    }

    public List<RegionPaymentDTO> getPaymentByRegion(CustomerReportRequest request) {
        List<Object[]> result = repository.findPaymentByRegion(
                request.getFromDate(),
                request.getToDate()
        );

        // Map raw Object[] into typed DTO
        return result.stream().map(row -> new RegionPaymentDTO(
                row[0] != null ? row[0].toString() : "Không xác định",
                (BigDecimal) row[1],
                (BigDecimal) row[2],
                (BigDecimal) row[3]
        )).collect(Collectors.toList());
    }

    public List<RegionOrderBreakdownDTO> getRegionOrderBreakdown(CustomerReportRequest request) {
        List<Object[]> raw = repository.fetchRegionOrderBreakdown(
                request.getFromDate(), request.getToDate());
        List<RegionOrderBreakdownDTO> result = new ArrayList<>();

        for (Object[] row : raw) {
            String region = (String) row[0];
            Long total = ((Number) row[1]).longValue();
            Long service = ((Number) row[2]).longValue();
            Long foxie = ((Number) row[3]).longValue();
            Long product = ((Number) row[4]).longValue();
            Long card = ((Number) row[5]).longValue();

            result.add(new RegionOrderBreakdownDTO(region, total, service, foxie, product, card));
        }
        return result;
    }

    public List<RegionOrderBreakdownTableDTO> getRegionOrderBreakdownTable(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);
        List<Object[]> current = repository.fetchRegionOrderBreakdown(r.fromDate(), r.toDate());
        List<Object[]> previous = repository.fetchRegionOrderBreakdown(r.prevFrom(), r.prevTo());

        Map<String, Object[]> prevMap = previous.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> row));

        List<RegionOrderBreakdownTableDTO> result = new ArrayList<>();

        for (Object[] row : current) {
            String shopName   = (String) row[0];
            long total        = ((Number) row[1]).longValue();
            long service      = ((Number) row[2]).longValue(); // DV%
            long cashTCC      = ((Number) row[3]).longValue(); // prepaid > 0
            long combo        = ((Number) row[4]).longValue(); // CB%
            long cardPurchase = ((Number) row[5]).longValue(); // mua thẻ

            Object[] prev = prevMap.get(shopName);
            long prevTotal        = prev != null ? ((Number) prev[1]).longValue() : 0L;
            long prevService      = prev != null ? ((Number) prev[2]).longValue() : 0L;
            long prevCashTCC      = prev != null ? ((Number) prev[3]).longValue() : 0L;
            long prevCombo        = prev != null ? ((Number) prev[4]).longValue() : 0L;
            long prevCardPurchase = prev != null ? ((Number) prev[5]).longValue() : 0L;

            RegionOrderBreakdownTableDTO dto = new RegionOrderBreakdownTableDTO();
            dto.setShopName(shopName);
            dto.setTotalOrders(total);
            dto.setServiceOrders(service);
            dto.setPrepaidCard(cashTCC);
            dto.setComboOrders(combo);
            dto.setCardPurchaseOrders(cardPurchase);

            dto.setDeltaTotalOrders(total - prevTotal);
            dto.setDeltaServiceOrders(service - prevService);
            dto.setDeltaPrepaidCard(cashTCC - prevCashTCC);
            dto.setDeltaComboOrders(combo - prevCombo);
            dto.setDeltaCardPurchaseOrders(cardPurchase - prevCardPurchase);

            result.add(dto);
        }
        return result;
    }

    public OverallOrderSummaryDTO getOverallOrderSummary(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);
        List<Object[]> currentList = repository.fetchOverallOrderSummary(r.fromDate(), r.toDate());
        List<Object[]> previousList = repository.fetchOverallOrderSummary(r.prevFrom(), r.prevTo());

        Object[] current = currentList.get(0);
        Object[] previous = previousList.get(0);

        OverallOrderSummaryDTO dto = new OverallOrderSummaryDTO();

        Long total = ((Number) current[0]).longValue();
        Long service = ((Number) current[1]).longValue();
        Long foxie = ((Number) current[2]).longValue();
        Long product = ((Number) current[3]).longValue();
        Long card = ((Number) current[4]).longValue();

        Long prevTotal = ((Number) previous[0]).longValue();
        Long prevService = ((Number) previous[1]).longValue();
        Long prevFoxie = ((Number) previous[2]).longValue();
        Long prevProduct = ((Number) previous[3]).longValue();
        Long prevCard = ((Number) previous[4]).longValue();

        dto.setTotalOrders(total);
        dto.setServiceOrders(service);
        dto.setFoxieCardOrders(foxie);
        dto.setProductOrders(product);
        dto.setCardPurchaseOrders(card);

        dto.setDeltaTotalOrders(total - prevTotal);
        dto.setDeltaServiceOrders(service - prevService);
        dto.setDeltaFoxieCardOrders(foxie - prevFoxie);
        dto.setDeltaProductOrders(product - prevProduct);
        dto.setDeltaCardPurchaseOrders(card - prevCard);

        return dto;
    }

    public OverallSummaryDTO getOverallSummary(CustomerReportRequest request) {
        CustomerSaleRecordService.Result r = getResult(request);
        List<Object[]> currentList = repository.fetchOverallRevenueSummary(r.fromDate(), r.toDate());
        List<Object[]> previousList = repository.fetchOverallRevenueSummary(r.prevFrom(), r.prevTo());

        Object[] current = currentList.get(0);
        Object[] previous = previousList.get(0);

        OverallSummaryDTO dto = new OverallSummaryDTO();

        BigDecimal total = toSafeBigDecimal(current[0]);
        BigDecimal service = toSafeBigDecimal(current[1]);
        BigDecimal foxie = toSafeBigDecimal(current[4]);
        BigDecimal product = toSafeBigDecimal(current[3]);
        BigDecimal card = toSafeBigDecimal(current[2]);
        BigDecimal actualRevenue = toSafeBigDecimal(current[5]);

        long days = ChronoUnit.DAYS.between(r.fromDate().toLocalDate(), r.toDate().toLocalDate()) + 1;
        if (days <= 0) days = 1;

        BigDecimal avgDaily = actualRevenue.divide(BigDecimal.valueOf(days), RoundingMode.HALF_UP);

        BigDecimal prevTotal = toSafeBigDecimal(previous[0]);
        BigDecimal prevService = toSafeBigDecimal(previous[1]);
        BigDecimal prevFoxie = toSafeBigDecimal(previous[4]);
        BigDecimal prevProduct = toSafeBigDecimal(previous[3]);
        BigDecimal prevCard = toSafeBigDecimal(previous[2]);
        BigDecimal preActualRev = toSafeBigDecimal(previous[5]);

        long preDays = ChronoUnit.DAYS.between(r.prevFrom().toLocalDate(), r.prevTo().toLocalDate()) + 1;
        if (preDays <= 0) preDays = 1;

        BigDecimal preAvgDaily = preActualRev.divide(BigDecimal.valueOf(preDays), RoundingMode.HALF_UP);

        dto.setTotalRevenue(total);
        dto.setServiceRevenue(service);
        dto.setFoxieCardRevenue(foxie);
        dto.setProductRevenue(product);
        dto.setCardPurchaseRevenue(card);
        dto.setAvgActualRevenueDaily(avgDaily);

        dto.setDeltaTotalRevenue(total.subtract(prevTotal));
        dto.setDeltaServiceRevenue(service.subtract(prevService));
        dto.setDeltaFoxieCardRevenue(foxie.subtract(prevFoxie));
        dto.setDeltaProductRevenue(product.subtract(prevProduct));
        dto.setDeltaCardPurchaseRevenue(card.subtract(prevCard));
        dto.setDeltaAvgActualRevenue(avgDaily.subtract(preAvgDaily));

        dto.setPercentTotalRevenue(calculatePercentChange(prevTotal, total));
        dto.setPercentServiceRevenue(calculatePercentChange(prevService, service));
        dto.setPercentFoxieCardRevenue(calculatePercentChange(prevFoxie, foxie));
        dto.setPercentProductRevenue(calculatePercentChange(prevProduct, product));
        dto.setPercentCardPurchaseRevenue(calculatePercentChange(prevCard, card));
        dto.setPercentAvgActualRevenue(calculatePercentChange(preAvgDaily, avgDaily));

        return dto;
    }

    public List<TopCustomerSpendingDTO> getTopCustomersBySpending(CustomerReportRequest request) {
        List<Object[]> rawData = repository.fetchTopCustomersBySpending(request.getFromDate(), request.getToDate());

        return rawData.stream()
                .map(row -> new TopCustomerSpendingDTO(
                        (String) row[0], // phone number
                        (String) row[1], // customer name
                        (BigDecimal) row[2] // total spending
                ))
                .toList();
    }
}