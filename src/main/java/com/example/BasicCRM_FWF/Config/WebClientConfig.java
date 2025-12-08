package com.example.BasicCRM_FWF.Config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /***
     - WebClient là một phần của Spring Framework, cụ thể là module Spring WebFlux, được giới thiệu từ Spring 5.
     Nó là một HTTP client không đồng bộ (non-blocking) và phản ứng (reactive), được thiết kế để thay thế cho RestTemplate truyền thống.
     - WebClient được sử dụng để gọi và trao đổi dữ liệu với các dịch vụ web bên ngoài (external web services) hoặc các API khác từ ứng dụng Spring Boot của bạn.

     Ưu điểm,Giải thích
     Không đồng bộ (Non-blocking): "WebClient sử dụng mô hình lập trình phản ứng (Reactive Programming). Khi thực hiện một request, ứng dụng không cần phải đợi (block) cho đến khi nhận được phản hồi. Nó giải phóng luồng (thread) hiện tại, cho phép luồng đó xử lý các request khác, giúp tăng hiệu suất và khả năng mở rộng của ứng dụng."
     Lập trình Phản ứng (Reactive):"Nó sử dụng các kiểu dữ liệu phản ứng như Mono (cho kết quả 0 hoặc 1) và Flux (cho kết quả 0 đến N) từ thư viện Project Reactor, cho phép xử lý dữ liệu theo luồng một cách hiệu quả."
     Thay thế cho RestTemplate,RestTemplate là một HTTP client đồng bộ và sắp bị loại bỏ (deprecated). WebClient là giải pháp hiện đại được khuyến nghị thay thế.
     */

    @Bean
    public WebClient fwfWebClient(WebClient.Builder builder) {

        // Cấu hình kết nối (Connector) để kiểm soát Thread Pool và Timeouts

        // Ví dụ: Cấu hình với Netty để kiểm soát Thread Pool (Worker Threads)
        // Tuy nhiên, việc quản lý Pool này thường được Netty xử lý ngầm và tối ưu
        // Dưới đây là ví dụ cấu hình timeout:
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30)) // Timeout cho phản hồi
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30))
                                .addHandlerLast(new WriteTimeoutHandler(30)));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        // Tạo WebClient dùng riêng cho FWF
        return builder
                .clientConnector(connector)
                .baseUrl("https://app.facewashfox.com") // base URL chung
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // header mặc định
                .build();
    }
}
