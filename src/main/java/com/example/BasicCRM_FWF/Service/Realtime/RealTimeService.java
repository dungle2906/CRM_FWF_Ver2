package com.example.BasicCRM_FWF.Service.Realtime;

import com.example.BasicCRM_FWF.Config.FWFApiExecutor;
import com.example.BasicCRM_FWF.DTORealTime.*;
import com.example.BasicCRM_FWF.Model.Shift;
import com.example.BasicCRM_FWF.Repository.ShiftRepository;
import com.example.BasicCRM_FWF.Service.AuthRealTime.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RealTimeService implements RealTimeInterface {

    private static final String FWF_PATH = "/api/v3/r23/ban-hang/doanh-so-danh-sach";

    private final RestTemplate restTemplate;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final WebClient fwfWebClient;
    private final ShiftRepository shiftRepository;
    private final FWFApiExecutor executor;

    @Value("${application.stock.id}")
    private String stockListId;

    public RealTimeService(RestTemplate restTemplate,
                           AuthService authService,
                           ObjectMapper objectMapper,
                           @Qualifier("fwfWebClient") WebClient fwfWebClient,
                           ShiftRepository shiftRepository,
                           FWFApiExecutor executor) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.fwfWebClient = fwfWebClient;
        this.shiftRepository = shiftRepository;
        this.executor = executor;
    }

    @Override
    public SalesSummaryDTO getSales(String dateStart, String dateEnd, String stockId) throws Exception {
        // 1 Lấy token realtime (service chuyên handle token hết hạn)
        String token = authService.getToken();

        // 2. Chuẩn hóa list stock cần gọi
        List<String> stockIds = resolveStockIds(stockId);

        if (stockIds.isEmpty()) {
            // Nếu không có stock nào thì trả về DTO rỗng để tránh lỗi
            return createEmptySummary();
        }

        // 3. Tạo 1 list các CompletableFuture, mỗi future là 1 API call cho 1 stock
        List<CompletableFuture<SalesSummaryDTO>> futures = stockIds.stream()
                .map(s -> callSalesApiAsync(dateStart, dateEnd, s, token)).toList();

        // 4. Gộp tất cả future lại bằng allOf (đợi tất cả xong)
        CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // 5. Khi tất cả future hoàn thành, join từng cái để lấy danh sách DTO kết quả
        CompletableFuture<List<SalesSummaryDTO>> allResultsFuture = allOfFuture.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join) // join từng future (ở đây đã hoàn thành nên không block lâu)
                        .toList());

        // 6. get() để chờ kết quả tổng hợp (service vẫn là synchronous nhưng đã tận dụng chạy song song bên trong)
        List<SalesSummaryDTO> partialResults = allResultsFuture.get();

        // 7. Merge tất cả DTO con lại thành 1 DTO tổng
        return mergeSalesSummaries(partialResults);
    }

    /**
     * Gộp (cộng dồn) danh sách nhiều SalesSummaryDTO lại thành 1 DTO tổng.
     */
    private SalesSummaryDTO mergeSalesSummaries(List<SalesSummaryDTO> summaries) {
        // Dùng BigDecimal cho chính xác
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal toPay = BigDecimal.ZERO;
        BigDecimal actualRevenue = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal wallet = BigDecimal.ZERO;
        BigDecimal foxie = BigDecimal.ZERO;
        BigDecimal debt = BigDecimal.ZERO;

        for (SalesSummaryDTO dto : summaries) {
            totalRevenue = totalRevenue.add(parseOrZero(dto.getTotalRevenue()));
            toPay = toPay.add(parseOrZero(dto.getToPay()));
            actualRevenue = actualRevenue.add(parseOrZero(dto.getActualRevenue()));
            cash = cash.add(parseOrZero(dto.getCash()));
            transfer = transfer.add(parseOrZero(dto.getTransfer()));
            card = card.add(parseOrZero(dto.getCard()));
            wallet = wallet.add(parseOrZero(dto.getWalletUsageRevenue()));
            foxie = foxie.add(parseOrZero(dto.getFoxieUsageRevenue()));
            debt = debt.add(parseOrZero(dto.getDebt()));
        }

        SalesSummaryDTO totalDto = new SalesSummaryDTO();
        totalDto.setTotalRevenue(totalRevenue.toPlainString());
        totalDto.setToPay(toPay.toPlainString());
        totalDto.setActualRevenue(actualRevenue.toPlainString());
        totalDto.setCash(cash.toPlainString());
        totalDto.setTransfer(transfer.toPlainString());
        totalDto.setCard(card.toPlainString());
        totalDto.setWalletUsageRevenue(wallet.toPlainString());
        totalDto.setFoxieUsageRevenue(foxie.toPlainString());
        totalDto.setDebt(debt.toPlainString());

        return totalDto;
    }

    /**
     * Parse String -> BigDecimal, nếu null/rỗng thì trả về 0.
     */
    private BigDecimal parseOrZero(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    /**
     * Chuẩn hóa danh sách stock dựa trên tham số truyền vào.
     * - Nếu client truyền stockId (danh sách dạng "1,2,3") thì dùng cái đó.
     * - Nếu không, dùng stockListId mặc định của hệ thống.
     */
    private List<String> resolveStockIds(String stockIdParam) {
        String source = (stockIdParam != null && !stockIdParam.isBlank()) ? stockIdParam : stockListId;
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Tạo một DTO rỗng (tất cả = 0).
     */
    private SalesSummaryDTO createEmptySummary() {
        SalesSummaryDTO dto = new SalesSummaryDTO();
        dto.setTotalRevenue("0");
        dto.setToPay("0");
        dto.setActualRevenue("0");
        dto.setCash("0");
        dto.setTransfer("0");
        dto.setCard("0");
        dto.setWalletUsageRevenue("0");
        dto.setFoxieUsageRevenue("0");
        dto.setDebt("0");
        return dto;
    }

    /**
     * Gọi API doanh số cho 1 stock, chạy theo kiểu async (CompletableFuture)
     */
    private CompletableFuture<SalesSummaryDTO> callSalesApiAsync(
            String dateStart,
            String dateEnd,
            String stockId,
            String token) {

        Map<String, Object> payload = buildPayload(dateStart, dateEnd, stockId);

        return executor.execute(
                "/api/v3/r23/ban-hang/doanh-so-danh-sach",
                payload,
                token,
                jsonString -> {
                    try {
                        JsonNode root = objectMapper.readTree(jsonString);
                        return mapToSalesSummaryDTO(root.path("result"));
                    } catch (Exception e) {
                        throw new RuntimeException("JSON parse error", e);
                    }
                }
        );
    }

    /**
     * Tạo payload cho API theo đúng format yêu cầu.
     */
    private Map<String, Object> buildPayload(String dateStart, String dateEnd, String stockId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("Voucher", "");
        payload.put("Payment", "");
        payload.put("IsMember", "");
        payload.put("MemberID", "");
        payload.put("SourceName", "");
        payload.put("ShipCode", "");
        payload.put("ShowsX", "2");
        payload.put("DebtFrom", null);
        payload.put("DebtTo", null);
        payload.put("no", "");
        return payload;
    }

    /**
     * Map JsonNode (result của 1 lần gọi API) -> SalesSummaryDTO cho 1 stock.
     */
    private SalesSummaryDTO mapToSalesSummaryDTO(JsonNode result) {
        SalesSummaryDTO dto = new SalesSummaryDTO();

        dto.setTotalRevenue(safeDecimal(result, "TotalValue"));
        dto.setToPay(safeDecimal(result, "ToPay"));
        dto.setActualRevenue(safeDecimal(result, "DaThToan"));
        dto.setCash(safeDecimal(result, "DaThToan_TM"));
        dto.setTransfer(safeDecimal(result, "DaThToan_CK"));
        dto.setCard(safeDecimal(result, "DaThToan_QT"));
        dto.setWalletUsageRevenue(safeDecimal(result, "DaThToan_Vi"));
        dto.setFoxieUsageRevenue(safeDecimal(result, "DaThToan_ThTien"));
        dto.setDebt(safeDecimal(result, "ConNo"));

        return dto;
    }

    /**
     * Lấy field decimal từ JsonNode, nếu null thì trả "0" để tránh NPE.
     */
    private String safeDecimal(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return "0";
        }
        // decimalValue() -> BigDecimal -> toPlainString() để tránh notation khoa học
        return valueNode.decimalValue().toPlainString();
    }

    // Hàm gọi API doanh số
//    @Override
//    public SalesSummaryDTO getSales(String dateStart, String dateEnd, String stockId) throws Exception {
//
//        String token = authService.getToken(); // login -> lấy token real-time, chỉ login lại khi token hết hạn
//        String url = "https://app.facewashfox.com/api/v3/r23/ban-hang/doanh-so-danh-sach";
//
//        String[] stockArray;
//        if (stockId != null && !stockId.isEmpty()) {
//            stockArray = stockId.split(",");
//            System.out.println(Arrays.toString(stockArray));
//        } else {
//            stockArray = stockListId.split(",");
//        }
//
//        // DTO tổng để cộng dồn
//        SalesSummaryDTO dto = new SalesSummaryDTO();
//        dto.setTotalRevenue("0");
//        dto.setToPay("0");
//        dto.setActualRevenue("0");
//        dto.setCash("0");
//        dto.setTransfer("0");
//        dto.setCard("0");
//        dto.setWalletUsageRevenue("0");
//        dto.setFoxieUsageRevenue("0");
//        dto.setDebt("0");
//
//        for (String stock : stockArray){
//            // Body request
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("StockID", stock);
//            payload.put("DateStart", dateStart);
//            payload.put("DateEnd", dateEnd);
//            payload.put("Pi", 1);
//            payload.put("Ps", 1000);
//            payload.put("Voucher", "");
//            payload.put("Payment", "");
//            payload.put("IsMember", "");
//            payload.put("MemberID", "");
//            payload.put("SourceName", "");
//            payload.put("ShipCode", "");
//            payload.put("ShowsX", "2");
//            payload.put("DebtFrom", null);
//            payload.put("DebtTo", null);
//            payload.put("no", "");
//
//            // Headers API
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("Accept", "application/json, text/plain, */*");
//            headers.set("Authorization", "Bearer " + token);
//            headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
//            headers.set("Referer", "https://app.facewashfox.com/ban-hang/doanh-so");
//
//            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
//
//            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//            if (response.getStatusCode() == HttpStatus.OK) {
//                JsonNode result = objectMapper.readTree(response.getBody()).path("result");
//
//                // Lấy từng giá trị
//                BigDecimal totalRevenue = result.path("TotalValue").decimalValue();
//                BigDecimal toPay = result.path("ToPay").decimalValue();
//                BigDecimal actualRevenue = result.path("DaThToan").decimalValue();
//                BigDecimal cash = result.path("DaThToan_TM").decimalValue();
//                BigDecimal transfer = result.path("DaThToan_CK").decimalValue();
//                BigDecimal card = result.path("DaThToan_QT").decimalValue();
//                BigDecimal wallet = result.path("DaThToan_Vi").decimalValue();
//                BigDecimal foxie = result.path("DaThToan_ThTien").decimalValue();
//                BigDecimal debt = result.path("ConNo").decimalValue();
//
//                // Cộng dồn vào DTO tổng
//                dto.setTotalRevenue(new BigDecimal(dto.getTotalRevenue()).add(totalRevenue).toPlainString());
//                dto.setToPay(new BigDecimal(dto.getToPay()).add(toPay).toPlainString());
//                dto.setActualRevenue(new BigDecimal(dto.getActualRevenue()).add(actualRevenue).toPlainString());
//                dto.setCash(new BigDecimal(dto.getCash()).add(cash).toPlainString());
//                dto.setTransfer(new BigDecimal(dto.getTransfer()).add(transfer).toPlainString());
//                dto.setCard(new BigDecimal(dto.getCard()).add(card).toPlainString());
//                dto.setWalletUsageRevenue(new BigDecimal(dto.getWalletUsageRevenue()).add(wallet).toPlainString());
//                dto.setFoxieUsageRevenue(new BigDecimal(dto.getFoxieUsageRevenue()).add(foxie).toPlainString());
//                dto.setDebt(new BigDecimal(dto.getDebt()).add(debt).toPlainString());
//
//            } else {
//                throw new RuntimeException("API error: " + response.getStatusCode());
//            }
//        }
//
//        return dto;
//    }

    public List<StockSalesByDateDTO> getSalesByStockPerDay(List<String> stockIds, List<String> dateList) throws Exception {
        List<StockSalesByDateDTO> result = new ArrayList<>();

        for (String stockId : stockIds) {
            List<DailySalesDTO> dailyList = new ArrayList<>();
            for (String date : dateList) {
                SalesSummaryDTO dto = getSalesCopied(date, date, stockId);
                DailySalesDTO dayDto = new DailySalesDTO();
                dayDto.setDate(date);
                dayDto.setTotalRevenue(dto.getTotalRevenue());
                dayDto.setCash(dto.getCash());
                dayDto.setTransfer(dto.getTransfer());
                dayDto.setCard(dto.getCard());
                dayDto.setFoxieUsageRevenue(dto.getFoxieUsageRevenue());
                dayDto.setWalletUsageRevenue(dto.getWalletUsageRevenue());
                dailyList.add(dayDto);
            }
            result.add(new StockSalesByDateDTO(stockId, dailyList));
        }

        return result;
    }

    public List<DailySalesDTO> getAggregatedSales(List<String> stockIds, List<String> dateList) throws Exception {
        Map<String, DailySalesDTO> aggregationMap = new LinkedHashMap<>();

        for (String date : dateList) {
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal cash = BigDecimal.ZERO;
            BigDecimal transfer = BigDecimal.ZERO;
            BigDecimal card = BigDecimal.ZERO;
            BigDecimal foxie = BigDecimal.ZERO;
            BigDecimal wallet = BigDecimal.ZERO;

            for (String stockId : stockIds) {
                SalesSummaryDTO dto = getSalesCopied(date, date, stockId);
                total = total.add(new BigDecimal(dto.getTotalRevenue()));
                cash = cash.add(new BigDecimal(dto.getCash()));
                transfer = transfer.add(new BigDecimal(dto.getTransfer()));
                card = card.add(new BigDecimal(dto.getCard()));
                foxie = foxie.add(new BigDecimal(dto.getFoxieUsageRevenue()));
                wallet = wallet.add(new BigDecimal(dto.getWalletUsageRevenue()));
            }

            DailySalesDTO agg = new DailySalesDTO();
            agg.setDate(date);
            agg.setTotalRevenue(total.toPlainString());
            agg.setCash(cash.toPlainString());
            agg.setTransfer(transfer.toPlainString());
            agg.setCard(card.toPlainString());
            agg.setFoxieUsageRevenue(foxie.toPlainString());
            agg.setWalletUsageRevenue(wallet.toPlainString());

            aggregationMap.put(date, agg);
        }

        return new ArrayList<>(aggregationMap.values());
    }

    @Override
    public SalesSummaryDTO getSalesCopied(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken(); // login -> lấy token real-time, chỉ login lại khi token hết hạn
        String url = "https://app.facewashfox.com/api/v3/r23/ban-hang/doanh-so-danh-sach";

        // Body request
        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("Voucher", "");
        payload.put("Payment", "");
        payload.put("IsMember", "");
        payload.put("MemberID", "");
        payload.put("SourceName", "");
        payload.put("ShipCode", "");
        payload.put("ShowsX", "2");
        payload.put("DebtFrom", null);
        payload.put("DebtTo", null);
        payload.put("no", "");

        // Headers API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/ban-hang/doanh-so");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody()).path("result");

            SalesSummaryDTO dto = new SalesSummaryDTO();
            dto.setTotalRevenue(result.path("TotalValue").decimalValue().toPlainString());
            dto.setToPay(result.path("ToPay").decimalValue().toPlainString());
            dto.setActualRevenue(result.path("DaThToan").decimalValue().toPlainString());
            dto.setCash(result.path("DaThToan_TM").decimalValue().toPlainString());
            dto.setTransfer(result.path("DaThToan_CK").decimalValue().toPlainString());
            dto.setCard(result.path("DaThToan_QT").decimalValue().toPlainString());
            dto.setWalletUsageRevenue(result.path("DaThToan_Vi").decimalValue().toPlainString());
            dto.setFoxieUsageRevenue(result.path("DaThToan_ThTien").decimalValue().toPlainString());
            dto.setDebt(result.path("ConNo").decimalValue().toPlainString());

            return dto;
        } else {
            throw new RuntimeException("API error: " + response.getStatusCode());
        }
    }

    @Override
    public String getActualRevenue(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/ban-hang/doanh-so-danh-sach";

        // Body request
        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("Voucher", "");
        payload.put("Payment", "");
        payload.put("IsMember", "");
        payload.put("MemberID", "");
        payload.put("SourceName", "");
        payload.put("ShipCode", "");
        payload.put("ShowsX", "2");
        payload.put("DebtFrom", null);
        payload.put("DebtTo", null);
        payload.put("no", "");

        // Headers API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/ban-hang/doanh-so");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody()).path("result");
            return result.path("DaThToan").decimalValue().toPlainString();
        } else {
            throw new RuntimeException("API error: " + response.getStatusCode());
        }
    }

    @Override
    public ServiceSummaryDTO getServiceSummary(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/tong-quan";

        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/dich-vu/tong-quan");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody()).path("result");

            ServiceSummaryDTO dto = new ServiceSummaryDTO();
            dto.setTotalServices(String.valueOf(result.path("TotalCasesInDay")));
            dto.setTotalServicesServing(String.valueOf(result.path("DoingCases")));
            dto.setTotalServiceDone(String.valueOf(result.path("DoneCases")));

            List<ServiceItems> items = new ArrayList<>();
            for (JsonNode itemNode : result.path("Items")){
                ServiceItems item = new ServiceItems();
                item.setServiceName(itemNode.path("ProServiceName").asText());
                item.setServiceUsageAmount(String.valueOf(itemNode.path("CasesNum").asInt()));
                item.setServiceUsagePercentage(String.valueOf(itemNode.path("CasesPercent").asDouble()));

                items.add(item);
            }
            dto.setItems(items);

            return dto;
        } else {
            throw new RuntimeException("API Error: " + response.getStatusCode());
        }
    }

    @Override
    public List<ServiceItems> getTop10Service(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/tong-quan";

        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/dich-vu/tong-quan");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody()).path("result");
            JsonNode itemsNode = result.path("Items");

            List<ServiceItems> items = new ArrayList<>();

            // Duyệt qua danh sách items, giới hạn lấy 10 phần tử đầu tiên
            int count = 0;
            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    if (count >= 10) break; // Dừng lại khi đã lấy đủ 10 dịch vụ

                    String serviceName = itemNode.path("ProServiceName").asText();

                    // Logic lọc: Bỏ qua các dịch vụ bắt đầu bằng "QUÀ TẶNG" hoặc "CT"
                    if (serviceName.startsWith("QUÀ TẶNG") || serviceName.startsWith("CT")) {
                        continue;
                    }

                    ServiceItems item = new ServiceItems();
                    item.setServiceName(itemNode.path("ProServiceName").asText());
                    item.setServiceUsageAmount(String.valueOf(itemNode.path("CasesNum").asInt()));
                    item.setServiceUsagePercentage(String.valueOf(itemNode.path("CasesPercent").asDouble()));

                    items.add(item);
                    count++;
                }
            }

            return items;
        } else {
            throw new RuntimeException("API Error: " + response.getStatusCode());
        }
    }

    @Override
    public List<SalesDetailDTO> getSalesDetail(String dateStart, String dateEnd) throws Exception {
        String token = authService.getToken();

        String url = "https://app.facewashfox.com/api/v3/r23/ban-hang/doanh-so-chi-tiet";

        Map<String, Object> payload = new HashMap<>();
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("BrandIds", "");
        payload.put("CategoriesIds", "");
        payload.put("ProductIds", "");
        payload.put("TimeToReal", 1);
        payload.put("ShowsType", "1");
        payload.put("StockRoles", stockListId);
        payload.put("Pi", 1);
        payload.put("Voucher", "");
        payload.put("Payment", "");
        payload.put("IsMember", "");

        // Headers API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/ban-hang/doanh-so");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody()).path("result");

            List<SalesDetailDTO> details = new ArrayList<>();

            if (result.isArray()) {
                for (JsonNode item : result) {
                    SalesDetailDTO dto = new SalesDetailDTO();
                    dto.setProductName(item.path("ProdTitle").asText());
                    dto.setProductPrice(item.path("SumTopay").decimalValue().toPlainString());
                    dto.setProductQuantity(item.path("SumQTy").decimalValue().toPlainString());
                    dto.setProductDiscount(item.path("Giamgia").decimalValue().toPlainString());
                    dto.setProductCode(item.path("DynamicID").asText());
                    dto.setProductUnit(item.path("StockUnit").asText());
                    dto.setFormatTable(item.path("Format").asText());

                    // doanh thu theo phương thức thanh toán
                    dto.setCash(item.path("TM").decimalValue().toPlainString());
                    dto.setTransfer(item.path("CK").decimalValue().toPlainString());
                    dto.setCard(item.path("QT").decimalValue().toPlainString());
                    dto.setWallet(item.path("Vi").decimalValue().toPlainString());
                    dto.setFoxie(item.path("TT").decimalValue().toPlainString());

                    details.add(dto);
                }
            }

            return details;
        } else {
            throw new RuntimeException("API lỗi: " + response.getStatusCode());
        }
    }

    @Override
    public BookingDTO getBookings(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/bao-cao-dat-lich";

        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("StatusMember", "");
        payload.put("StatusBook", "");
        payload.put("StatusAtHome", "");
        payload.put("MemberID", "");
        payload.put("UserID", "");
        payload.put("UserServiceIDs", "");
        payload.put("include", "IsNewMember,OrderInDate");
        payload.put("StocksRoles", stockListId);
        payload.put("Status", "XAC_NHAN,XAC_NHAN_TU_DONG,CHUA_XAC_NHAN,KHACH_KHONG_DEN,KHACH_DEN,TU_CHOI");

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/ban-hang/doanh-so");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody()).path("result");
            JsonNode sumNode = root.path("Sum");

            BookingDTO dto = new BookingDTO();
            dto.setNotConfirmed(sumNode.path("CHUA_XAC_NHAN").asText("0"));
            dto.setConfirmed(sumNode.path("XAC_NHAN").asText("0"));
            dto.setDenied(sumNode.path("TU_CHOI").asText("0"));
            dto.setCustomerCome(sumNode.path("KHACH_DEN").asText("0"));
            dto.setCustomerNotCome(sumNode.path("KHACH_KHONG_DEN").asText("0"));
            dto.setCancel(sumNode.path("KHACH_HUY").asText("0"));
            dto.setAutoConfirmed(sumNode.path("XAC_NHAN_TU_DONG").asText("0"));

            return dto;
        }

        throw new RuntimeException("API call failed with status: " + response.getStatusCode());
    }

    @Override
    public List<CustomerDTO> getNewCustomers(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/bao-cao-dat-lich";

        // Payload dùng format khách mới
        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("StatusMember", "KHACH_MOI");
        payload.put("StatusBook", "");
        payload.put("StatusAtHome", "");
        payload.put("MemberID", "");
        payload.put("UserID", "");
        payload.put("UserServiceIDs", "");
        payload.put("include", "IsNewMember,OrderInDate");
        payload.put("StocksRoles", stockListId); // Biến stockListId cần được define trong class
        payload.put("Status", "KHACH_DEN");

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Referer", "https://app.facewashfox.com/");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API call failed: " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody()).path("result");
        JsonNode items = root.path("Items");

        Map<String, Integer> sourceCountMap = new HashMap<>();

        if (items.isArray()) {
            for (JsonNode item : items) {
                // 1. Ưu tiên lấy Source từ Member
                String source = item.path("Member").path("Source").asText("").trim();

                // 2. Nếu Source null/rỗng hoặc là "app" thì parse từ Desc (Tags)
                if (source.isEmpty() || "app".equalsIgnoreCase(source)) {
                    String desc = item.path("Desc").asText("");
                    if (desc.contains("Tags:")) {
                        // Tách chuỗi để lấy phần sau "Tags:"
                        String[] parts = desc.split("Tags:");
                        if (parts.length > 1) {
                            // Lấy phần tử thứ 2, trim khoảng trắng
                            String tagPart = parts[1].trim();
                            // Nếu có xuống dòng (Ghi chú: ...) thì cắt lấy dòng đầu tiên
                            if (tagPart.contains("\n")) {
                                tagPart = tagPart.split("\n")[0].trim();
                            }

                            // Chỉ cập nhật source nếu tag lấy được không rỗng
                            if (!tagPart.isEmpty()) {
                                source = tagPart;
                            }
                        }
                    }
                }

                // 3. Nếu vẫn rỗng thì gán mặc định
                if (source.isEmpty()) {
                    source = "Không xác định";
                }

                // Cộng dồn vào Map
                sourceCountMap.put(source, sourceCountMap.getOrDefault(source, 0) + 1);
            }
        }

        // Convert Map sang List DTO
        List<CustomerDTO> results = new ArrayList<>();
        for (Map.Entry<String, Integer> e : sourceCountMap.entrySet()) {
            CustomerDTO dto = new CustomerDTO();
            dto.setType(e.getKey());
            dto.setCount(e.getValue());
            results.add(dto);
        }
        return results;
    }

    public String getNewCustomersRaw(String dateStart, String dateEnd, String stockId) throws Exception {

        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/bao-cao-dat-lich";

        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("StatusMember", "KHACH_MOI");
        payload.put("StatusBook", "");
        payload.put("StatusAtHome", "");
        payload.put("MemberID", "");
        payload.put("UserID", "");
        payload.put("UserServiceIDs", "");
        payload.put("include", "IsNewMember,OrderInDate");
        payload.put("StocksRoles", stockListId);
        payload.put("Status", "KHACH_DEN");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Referer", "https://app.facewashfox.com/");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API call failed: " + response.getStatusCode());
        }

        // 👉 Trả về JSON nguyên bản từ API
        return response.getBody();
    }

    @Override
    public List<CustomerDTO> getOldCustomers(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/bao-cao-dat-lich";

        // Payload cho khách cũ
        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("StatusMember", "KHACH_CU");
        payload.put("StatusBook", "");
        payload.put("StatusAtHome", "");
        payload.put("MemberID", "");
        payload.put("UserID", "");
        payload.put("UserServiceIDs", "");
        payload.put("include", "IsNewMember,OrderInDate");
        payload.put("StocksRoles", stockListId);
        payload.put("Status", "KHACH_DEN");

        // Headers API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/dich-vu/bao-cao-dat-lich");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody()).path("result");
            JsonNode items = root.path("Items");

            // Map đếm số lượng theo nhóm
            Map<String, Integer> tagCountMap = new HashMap<>();

            if (items.isArray()) {

                for (JsonNode item : items) {
                    // 1. Lấy tag gốc
                    String rawTag = extractTag(item);

                    // 2. Chuẩn hóa và gom nhóm tag
                    String groupedTag = normalizeTag(rawTag);

                    // 3. Gộp nhóm và tăng counter
                    tagCountMap.put(groupedTag, tagCountMap.getOrDefault(groupedTag, 0) + 1);
                }
            }

            // Chuyển sang DTO
            List<CustomerDTO> results = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : tagCountMap.entrySet()) {
                CustomerDTO dto = new CustomerDTO();
                dto.setType(entry.getKey());
                dto.setCount(entry.getValue());
                results.add(dto);
            }

            return results;
        } else {
            throw new RuntimeException("API call failed with status: " + response.getStatusCode());
        }
    }

    // HÀM 1: Lấy source/tag từ JSON (ưu tiên Source → fallback Desc)
    private String extractTag(JsonNode item) {
        String tag = item.path("Member").path("Source").asText();

        if (tag == null || tag.trim().isEmpty()) {
            String desc = item.path("Desc").asText();
            if (desc != null && desc.contains("Tags:")) {
                tag = desc.split("Tags:")[1].trim();
                if (tag.contains("\n")) tag = tag.split("\n")[0].trim();
            } else {
                tag = "app"; // fallback cuối cùng
            }
        }

        return tag.trim();
    }

    // HÀM 2: Gom nhóm & Chuẩn hóa Source theo cùng ý nghĩa
    private String normalizeTag(String input) {
        if (input == null) return "Khác";
        String tag = input.toLowerCase().trim();

        // --- Nhóm: Khách quay lại / giới thiệu ---
        if (tag.contains("bạn bè")
                || tag.contains("vãng lai")
                || tag.contains("giới thiệu")
                || tag.contains("truyền miệng")) {
            return "Khách quay lại / Giới thiệu";
        }

        // --- Nhóm: Quảng cáo Facebook ---
        if (tag.contains("facebook") || tag.contains("fb") || tag.contains("qc fb")) {
            return "Quảng cáo Facebook";
        }

        // --- Nhóm: Quảng cáo TikTok ---
        if (tag.contains("tiktok") || tag.contains("tiktok shop")) {
            return "Quảng cáo TikTok";
        }

        // --- Nhóm: Quảng cáo Zalo ---
        if (tag.contains("zalo")) {
            return "Quảng cáo Zalo";
        }

        // --- Nhóm: Nguồn tự nhiên (Organic) ---
        if (tag.contains("fanpage") || tag.contains("web") || tag.contains("app")) {
            return "Nguồn tự nhiên";
        }

        // --- Nhóm: Khách nước ngoài ---
        if (tag.contains("nước ngoài") || tag.contains("nuoc ngoai")) {
            return "Khách nước ngoài";
        }

        // --- Nhóm: CSKH / không xác định ---
        if (tag.contains("cskh") || tag.contains("không áp dụng") ||
                tag.contains("không thể") || tag.contains("không xác định") ||
                tag.contains("ghi chú")) {

            return "Nguồn khác / Không xác định";
        }

        // Tag không khớp nhóm nào → gom về nhóm chung
        return "Khác";
    }

    @Override
    public List<CustomerDTO> getAllBookingByHour(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/dich-vu/bao-cao-dat-lich";

        // Payload: bỏ lọc StatusMember để lấy tất cả khách
        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("StatusMember", ""); // lấy cả khách mới + cũ
        payload.put("StatusBook", "");
        payload.put("StatusAtHome", "");
        payload.put("MemberID", "");
        payload.put("UserID", "");
        payload.put("UserServiceIDs", "");
        payload.put("include", "IsNewMember,OrderInDate");
        payload.put("StocksRoles", stockListId);
        payload.put("Status", "KHACH_DEN"); // để trống = tất cả trạng thái

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/dich-vu/bao-cao-dat-lich");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API call failed with status: " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody()).path("result");
        JsonNode items = root.path("Items");

        // Nhóm theo giờ trong BookDate
        Map<String, Integer> hourCountMap = new TreeMap<>(); // sắp xếp theo giờ

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH:00");

        if (items.isArray()) {
            for (JsonNode item : items) {
                String bookDateStr = item.path("BookDate").asText();
                if (bookDateStr == null || bookDateStr.isEmpty()) continue;

                try {
                    LocalDateTime bookDate = LocalDateTime.parse(bookDateStr, inputFormatter);
                    String hourLabel = bookDate.format(hourFormatter); // ví dụ: "09:00", "14:00"
                    hourCountMap.put(hourLabel, hourCountMap.getOrDefault(hourLabel, 0) + 1);
                } catch (Exception e) {
                    // Bỏ qua lỗi parse nếu có
                }
            }
        }

        // Convert sang DTO
        List<CustomerDTO> results = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : hourCountMap.entrySet()) {
            CustomerDTO dto = new CustomerDTO();
            dto.setType(entry.getKey());   // dùng 'type' để lưu giờ
            dto.setCount(entry.getValue());
            results.add(dto);
        }

        return results;
    }

    @Override
    public List<Map<String, Object>> getSalesByHours(String dateStart, String dateEnd, String stockId) throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/r23/ban-hang/doanh-so-danh-sach";

        Map<String, Object> payload = new HashMap<>();
        payload.put("StockID", stockId);
        payload.put("DateStart", dateStart);
        payload.put("DateEnd", dateEnd);
        payload.put("Pi", 1);
        payload.put("Ps", 1000);
        payload.put("Voucher", "");
        payload.put("Payment", "");
        payload.put("IsMember", "");
        payload.put("MemberID", "");
        payload.put("SourceName", "");
        payload.put("ShipCode", "");
        payload.put("ShowsX", "2");
        payload.put("DebtFrom", null);
        payload.put("DebtTo", null);
        payload.put("no", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
        headers.set("Referer", "https://app.facewashfox.com/ban-hang/doanh-so");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API call failed: " + response.getStatusCode());
        }

        JsonNode items = objectMapper.readTree(response.getBody())
                .path("result")
                .path("Items");

        Map<String, Map<String, Integer>> groupedData = new TreeMap<>();

        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (JsonNode item : items) {
            String createDateStr = item.path("CreateDate").asText(null);
            if (createDateStr == null || createDateStr.isEmpty()) continue;

            LocalDateTime createDate = LocalDateTime.parse(createDateStr, dateTimeFmt);
            int hour = createDate.getHour();
            if (hour < 9 || hour > 22) continue; // ❌ Skip ngoài khung 9:00 - 23:59

            String day = createDate.format(dayFmt);
            String timeRange = getTimeRange(createDate);

            groupedData
                    .computeIfAbsent(day, k -> new TreeMap<>())
                    .merge(timeRange, 1, Integer::sum);
        }

        List<Map<String, Object>> responseList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> dayEntry : groupedData.entrySet()) {
            String day = dayEntry.getKey();
            for (Map.Entry<String, Integer> timeEntry : dayEntry.getValue().entrySet()) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("date", day);
                obj.put("timeRange", timeEntry.getKey());
                obj.put("totalSales", timeEntry.getValue());
                responseList.add(obj);
            }
        }

        return responseList;
    }

    /**
     * ✅ Trả về 14 khung giờ từ 09:00 - 23:59, mỗi khung 1 tiếng
     */
    private static String getTimeRange(LocalDateTime createDate) {
        int hour = createDate.getHour();

        switch (hour) {
            case 9:  return "09:00 - 09:59";
            case 10: return "10:00 - 10:59";
            case 11: return "11:00 - 11:59";
            case 12: return "12:00 - 12:59";
            case 13: return "13:00 - 13:59";
            case 14: return "14:00 - 14:59";
            case 15: return "15:00 - 15:59";
            case 16: return "16:00 - 16:59";
            case 17: return "17:00 - 17:59";
            case 18: return "18:00 - 18:59";
            case 19: return "19:00 - 19:59";
            case 20: return "20:00 - 20:59";
            case 21: return "21:00 - 21:59";
            case 22: return "22:00 - 22:59";
            default: return null;
        }
    }

    @Override
    @Scheduled(cron = "0 0 1 * * *")
    public void autoSaveWorkTrack() throws Exception {
        String token = authService.getToken();
        String url = "https://app.facewashfox.com/api/v3/userwork23@workList";

        LocalDateTime pastDay = LocalDateTime.now().minusDays(1);
        String dateStart = pastDay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String endDate = dateStart;

        Map<String, Object> payload = new HashMap<>();
        payload.put("From", dateStart);
        payload.put("To", endDate);
        payload.put("key", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API call failed: " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode items = root.path("list");
        if (items.isArray()) {
            for (JsonNode item : items) {
                String userId = item.path("UserID").asText();  // ✅ đúng key
                String fullname = item.path("FullName").asText();
                String username = item.path("UserName").asText();
                String stockId = item.path("StockID").asText();
                String stockTitle = item.path("StockTitle").asText();

                JsonNode dates = item.path("Dates");
                if (dates.isArray()) {
                    for (JsonNode dateNode : dates) {
                        String date = dateNode.path("Date").asText();
                        JsonNode workTrack = dateNode.path("WorkTrack");

                        // ✅ Kiểm tra null
                        if (workTrack.isMissingNode() || workTrack.isNull()) continue;

                        String checkIn = workTrack.path("CheckIn").asText();
                        String checkOut = workTrack.path("CheckOut").asText();

                        JsonNode info = workTrack.path("Info");
                        double diSom = 0, diMuon = 0, veSom = 0, veMuon = 0;

                        if (info.has("DI_MUON")) {
                            diMuon = 0 - Math.abs(info.path("DI_MUON").path("Value").asDouble(0)); // luôn âm
                        }
                        if (info.has("VE_SOM")) {
                            veSom = 0 - Math.abs(info.path("VE_SOM").path("Value").asDouble(0)); // luôn âm
                        }
                        if (info.has("CheckOut") && info.path("CheckOut").has("VE_MUON")) {
                            veMuon = Math.abs(info.path("CheckOut").path("VE_MUON").path("Value").asDouble(0)); // luôn dương
                        }

                        String typeCheckIn = info.path("Type").asText();
                        String desTypeCheckIn = info.path("Desc").asText();

                        JsonNode workToday = info.path("WorkToday");
                        JsonNode checkOutNode = info.path("CheckOut");
                        String title = workToday.path("Title").asText();
                        String timeFrom = workToday.path("TimeFrom").asText();
                        String timeTo = workToday.path("TimeTo").asText();
                        String mandays = workToday.path("Value").asText();
                        String typeCheckOut = checkOutNode.path("Type").asText();
                        String desTypeCheckOut = checkOutNode.path("Desc").asText();

                        Shift dto = Shift.builder()
                                .fullname(fullname)
                                .username(username)
                                .stockId(stockId)
                                .stockTitle(stockTitle)
                                .date(date)
                                .checkIn(checkIn)
                                .checkOut(checkOut)
                                .title(title)
                                .timeFrom(timeFrom)
                                .timeTo(timeTo)
                                .mandays(mandays)
                                .typeCheckIn(typeCheckIn)
                                .desTypeCheckIn(desTypeCheckIn)
                                .typeCheckOut(typeCheckOut)
                                .desTypeCheckOut(desTypeCheckOut)
                                .diSom(diSom)
                                .diMuon(diMuon)
                                .veSom(veSom)
                                .veMuon(veMuon)
                                .build();

                        shiftRepository.save(dto);
                    }
                }
            }
        }
        log.info("==== AUTO SAVE WORK TRACK ====");
    }

}
