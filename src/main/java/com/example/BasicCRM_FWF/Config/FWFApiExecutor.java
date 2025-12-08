package com.example.BasicCRM_FWF.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

@Component
public class FWFApiExecutor {

    // Giới hạn số lượng request chạy song song = 5
    private final Semaphore semaphore = new Semaphore(25);

    // ExecutorService điều phối các tác vụ async
    private final ExecutorService executorService = Executors.newFixedThreadPool(25);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FWFApiExecutor(@Qualifier("fwfWebClient") WebClient webClient,
                               ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Hàm execute dùng chung cho tất cả API Facewash.
     * @param path PATH của API (ví dụ: "/api/v3/r23/ban-hang/doanh-so-danh-sach")
     * @param payload JSON body gửi lên
     * @param token token FWF
     * @param mapper Function để chuyển response JSON -> kiểu dữ liệu T mà bạn muốn
     */
    public <T> CompletableFuture<T> execute(String path,
                                            Object payload,
                                            String token,
                                            Function<String, T> mapper) {

//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                String stock = String.valueOf(payload instanceof Map ? ((Map) payload).get("StockID") : "UNKNOWN");
//
//                System.out.println("[START] Thread=" + Thread.currentThread().getName()
//                        + " | Stock=" + stock
//                        + " | Time=" + LocalTime.now());
//
//                semaphore.acquire();
//
//                T result = callApiInternal(path, payload, token, mapper);
//
//                System.out.println("[END]   Thread=" + Thread.currentThread().getName()
//                        + " | Stock=" + stock
//                        + " | Time=" + LocalTime.now());
//
//                return result;
//
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//
//            } finally {
//                semaphore.release();
//            }
//
//        }, executorService);

        return CompletableFuture.supplyAsync(() -> {
            try {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return callApiInternal(path, payload, token, mapper);
            } finally {
                semaphore.release();
            }
        }, executorService);

    }

    /**
     * Gọi API thật bằng WebClient (blocking inside Future)
     */
    private <T> T callApiInternal(String path,
                                  Object payload,
                                  String token,
                                  Function<String, T> mapper) {

        String rawJsonResponse =
                webClient.post()
                        .uri(path)
                        .headers(h -> {
                            h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                            h.set(HttpHeaders.ACCEPT, "*/*");
                        })
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        // Retry khi lỗi Connection reset, network lỗi
                        .retryWhen(
                                Retry.backoff(3, Duration.ofSeconds(1))
                                        .filter(ex -> ex instanceof WebClientRequestException)
                        )
                        // Timeout tổng hợp
                        .timeout(Duration.ofSeconds(30))
                        .block(); // block trong thread riêng, KHÔNG block thread web

        return mapper.apply(rawJsonResponse);
    }
}
