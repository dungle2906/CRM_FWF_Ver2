package com.example.BasicCRM_FWF.Service.AuthRealTime;

import com.example.BasicCRM_FWF.Config.TokenCache;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final TokenCache tokenCache;

    @Value("${app.facewashfox.username}")
    private String username;

    @Value("${app.facewashfox.password}")
    private String password;

    public String getToken() throws Exception {

        if (tokenCache.isTokenValid()) {
            System.out.println("✅ Token still available. Reutilize old token");
            return tokenCache.getToken();
        }
        System.out.println("🔄 Token is expired or invalid...");

        String loginUrl = "https://app.facewashfox.com/admin/Login.aspx";

        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .build();
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

        ResponseEntity<String> getResp = restTemplate.getForEntity(loginUrl, String.class);
        List<String> cookies = getResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        String body = getResp.getBody();

        Document doc = Jsoup.parse(body);
        String viewstate = doc.select("input[name=__VIEWSTATE]").attr("value");
        String viewstategenerator = doc.select("input[name=__VIEWSTATEGENERATOR]").attr("value");
        String eventvalidation = doc.select("input[name=__EVENTVALIDATION]").attr("value");

        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("__EVENTTARGET", "lbtLogin");
        payload.add("__EVENTARGUMENT", "");
        payload.add("__VIEWSTATE", viewstate);
        payload.add("__VIEWSTATEGENERATOR", viewstategenerator);
        payload.add("__EVENTVALIDATION", eventvalidation);
        payload.add("txtUserName", username);
        payload.add("txtPassword", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Referer", loginUrl);

        if (cookies != null) {
            String cookieHeader = cookies.stream()
                    .map(c -> c.split(";", 2)[0])
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("");
            headers.set(HttpHeaders.COOKIE, cookieHeader);
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, entity, String.class);

        List<String> respCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (respCookies != null) {
            for (String cookie : respCookies) {
                if (cookie.contains("token=")) {
                    String token = cookie.split("token=")[1].split(";", 2)[0];
                    tokenCache.setToken(token);
                    return token;
                }
            }
        }
        throw new RuntimeException("Login failed! Unable to achieve token!");
    }
}
