//_+package com.example.BasicCRM_FWF.Service.Realtime;
//
//@Service
//public class SalesServiceImpl implements SalesService {
//
//    private static final String SALES_PATH = "/api/v3/r23/ban-hang/doanh-so-danh-sach";
//
//    private final AuthService authService;
//    private final WebClient facewashWebClient;
//    private final ObjectMapper objectMapper;
//
//    // Danh sách stock default nếu không truyền từ client
//    private final String stockListId;
//
//    public SalesServiceImpl(AuthService authService,
//                            @Qualifier("facewashWebClient") WebClient facewashWebClient,
//                            ObjectMapper objectMapper,
//                            @Value("${app.stock-list-id}") String stockListId) {
//        this.authService = authService;                      // inject service lấy token
//        this.facewashWebClient = facewashWebClient;          // inject WebClient đã config sẵn
//        this.objectMapper = objectMapper;                    // inject ObjectMapper
//        this.stockListId = stockListId;                      // inject chuỗi stock default
//    }
//
//    @Override
//    public SalesSummaryDTO getSales(String dateStart, String dateEnd, String stockId) throws Exception {
//        // 1. Lấy token realtime (service của bạn đã handle chuyện hết hạn)
//        String token = authService.getToken();
//
//        // 2. Chuẩn hóa danh sách stock cần gọi (từ tham số hoặc từ stockListId default)
//        List<String> stockIds = resolveStockIds(stockId);
//
//        if (stockIds.isEmpty()) {
//            // Nếu không có stock nào thì trả về DTO rỗng để tránh lỗi
//            return createEmptySummary();
//        }
//
//        // 3. Tạo 1 list các CompletableFuture, mỗi future là 1 API call cho 1 stock
//        List<CompletableFuture<SalesSummaryDTO>> futures = stockIds.stream()
//                .map(stock -> callSalesApiAsync(dateStart, dateEnd, stock, token))
//                .toList();
//
//        // 4. Gộp tất cả future lại bằng allOf (đợi tất cả xong)
//        CompletableFuture<Void> allOfFuture =
//                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//
//        // 5. Khi tất cả future hoàn thành, join từng cái để lấy danh sách DTO kết quả
//        CompletableFuture<List<SalesSummaryDTO>> allResultsFuture = allOfFuture.thenApply(v ->
//                futures.stream()
//                        .map(CompletableFuture::join) // join từng future (ở đây đã hoàn thành nên không block lâu)
//                        .toList()
//        );
//
//        // 6. get() để chờ kết quả tổng hợp (service vẫn là synchronous nhưng đã tận dụng chạy song song bên trong)
//        List<SalesSummaryDTO> partialResults = allResultsFuture.get();
//
//        // 7. Merge tất cả DTO con lại thành 1 DTO tổng
//        return mergeSalesSummaries(partialResults);
//    }
//
//    /**
//     * Chuẩn hóa danh sách stock dựa trên tham số truyền vào.
//     * - Nếu client truyền stockId (danh sách dạng "1,2,3") thì dùng cái đó.
//     * - Nếu không, dùng stockListId mặc định của hệ thống.
//     */
//    private List<String> resolveStockIds(String stockIdParam) {
//        String source = (stockIdParam != null && !stockIdParam.isBlank())
//                ? stockIdParam
//                : stockListId;
//
//        return Arrays.stream(source.split(","))
//                .map(String::trim)               // bỏ whitespace dư
//                .filter(s -> !s.isEmpty())       // bỏ stock rỗng
//                .distinct()                      // tránh gọi trùng
//                .toList();
//    }
//
//    /**
//     * Gọi API doanh số cho 1 stock, chạy theo kiểu async (CompletableFuture)
//     */
//    private CompletableFuture<SalesSummaryDTO> callSalesApiAsync(String dateStart,
//                                                                 String dateEnd,
//                                                                 String stockId,
//                                                                 String token) {
//        // 1. Build request body
//        Map<String, Object> payload = buildPayload(dateStart, dateEnd, stockId);
//
//        // 2. Gọi WebClient, trả về CompletableFuture
//        return facewashWebClient
//                .post()
//                .uri(SALES_PATH)
//                .headers(headers -> {
//                    headers.set(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
//                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
//                    headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6_2 like Mac OS X)");
//                    headers.set(HttpHeaders.REFERER, "https://app.facewashfox.com/ban-hang/doanh-so");
//                })
//                .bodyValue(payload)
//                .retrieve()
//                // handle lỗi HTTP tại đây nếu cần .onStatus(...)
//                .bodyToMono(JsonNode.class)          // parse body về JsonNode
//                .map(json -> json.path("result"))   // đi vào field "result"
//                .map(this::mapToSalesSummaryDTO)    // convert JsonNode -> SalesSummaryDTO (1 stock)
//                .toFuture();                         // chuyển sang CompletableFuture để dùng chung pattern
//    }
//
//    /**
//     * Tạo payload cho API theo đúng format yêu cầu.
//     */
//    private Map<String, Object> buildPayload(String dateStart, String dateEnd, String stockId) {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("StockID", stockId);
//        payload.put("DateStart", dateStart);
//        payload.put("DateEnd", dateEnd);
//        payload.put("Pi", 1);
//        payload.put("Ps", 1000);
//        payload.put("Voucher", "");
//        payload.put("Payment", "");
//        payload.put("IsMember", "");
//        payload.put("MemberID", "");
//        payload.put("SourceName", "");
//        payload.put("ShipCode", "");
//        payload.put("ShowsX", "2");
//        payload.put("DebtFrom", null);
//        payload.put("DebtTo", null);
//        payload.put("no", "");
//        return payload;
//    }
//
//    /**
//     * Map JsonNode (result của 1 lần gọi API) -> SalesSummaryDTO cho 1 stock.
//     */
//    private SalesSummaryDTO mapToSalesSummaryDTO(JsonNode result) {
//        SalesSummaryDTO dto = new SalesSummaryDTO();
//
//        dto.setTotalRevenue(safeDecimal(result, "TotalValue"));
//        dto.setToPay(safeDecimal(result, "ToPay"));
//        dto.setActualRevenue(safeDecimal(result, "DaThToan"));
//        dto.setCash(safeDecimal(result, "DaThToan_TM"));
//        dto.setTransfer(safeDecimal(result, "DaThToan_CK"));
//        dto.setCard(safeDecimal(result, "DaThToan_QT"));
//        dto.setWalletUsageRevenue(safeDecimal(result, "DaThToan_Vi"));
//        dto.setFoxieUsageRevenue(safeDecimal(result, "DaThToan_ThTien"));
//        dto.setDebt(safeDecimal(result, "ConNo"));
//
//        return dto;
//    }
//
//    /**
//     * Lấy field decimal từ JsonNode, nếu null thì trả "0" để tránh NPE.
//     */
//    private String safeDecimal(JsonNode node, String fieldName) {
//        JsonNode valueNode = node.path(fieldName);
//        if (valueNode.isMissingNode() || valueNode.isNull()) {
//            return "0";
//        }
//        // decimalValue() -> BigDecimal -> toPlainString() để tránh notation khoa học
//        return valueNode.decimalValue().toPlainString();
//    }
//
//    /**
//     * Tạo một DTO rỗng (tất cả = 0).
//     */
//    private SalesSummaryDTO createEmptySummary() {
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
//        return dto;
//    }
//
//    /**
//     * Gộp (cộng dồn) danh sách nhiều SalesSummaryDTO lại thành 1 DTO tổng.
//     */
//    private SalesSummaryDTO mergeSalesSummaries(List<SalesSummaryDTO> summaries) {
//        // Dùng BigDecimal cho chính xác
//        BigDecimal totalRevenue = BigDecimal.ZERO;
//        BigDecimal toPay = BigDecimal.ZERO;
//        BigDecimal actualRevenue = BigDecimal.ZERO;
//        BigDecimal cash = BigDecimal.ZERO;
//        BigDecimal transfer = BigDecimal.ZERO;
//        BigDecimal card = BigDecimal.ZERO;
//        BigDecimal wallet = BigDecimal.ZERO;
//        BigDecimal foxie = BigDecimal.ZERO;
//        BigDecimal debt = BigDecimal.ZERO;
//
//        for (SalesSummaryDTO dto : summaries) {
//            totalRevenue = totalRevenue.add(parseOrZero(dto.getTotalRevenue()));
//            toPay = toPay.add(parseOrZero(dto.getToPay()));
//            actualRevenue = actualRevenue.add(parseOrZero(dto.getActualRevenue()));
//            cash = cash.add(parseOrZero(dto.getCash()));
//            transfer = transfer.add(parseOrZero(dto.getTransfer()));
//            card = card.add(parseOrZero(dto.getCard()));
//            wallet = wallet.add(parseOrZero(dto.getWalletUsageRevenue()));
//            foxie = foxie.add(parseOrZero(dto.getFoxieUsageRevenue()));
//            debt = debt.add(parseOrZero(dto.getDebt()));
//        }
//
//        SalesSummaryDTO totalDto = new SalesSummaryDTO();
//        totalDto.setTotalRevenue(totalRevenue.toPlainString());
//        totalDto.setToPay(toPay.toPlainString());
//        totalDto.setActualRevenue(actualRevenue.toPlainString());
//        totalDto.setCash(cash.toPlainString());
//        totalDto.setTransfer(transfer.toPlainString());
//        totalDto.setCard(card.toPlainString());
//        totalDto.setWalletUsageRevenue(wallet.toPlainString());
//        totalDto.setFoxieUsageRevenue(foxie.toPlainString());
//        totalDto.setDebt(debt.toPlainString());
//
//        return totalDto;
//    }
//
//    /**
//     * Parse String -> BigDecimal, nếu null/rỗng thì trả về 0.
//     */
//    private BigDecimal parseOrZero(String value) {
//        if (value == null || value.isBlank()) {
//            return BigDecimal.ZERO;
//        }
//        return new BigDecimal(value);
//    }
//}
