package com.example.BasicCRM_FWF.Config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Data
public class TokenCache {
    private String token;
    private Instant tokenTime;
    private static final long TOKEN_TTL_SECONDS = 3600;

    public boolean isTokenValid() {
        if (token == null || tokenTime == null) return false;
        return Instant.now().isBefore(tokenTime.plusSeconds(TOKEN_TTL_SECONDS));
    }

    public void setToken(String token) {
        this.token = token;
        this.tokenTime = Instant.now();
        System.out.println("💾 New token is saved to cache at: " + tokenTime);
    }

    public void clear() {
        this.token = null;
        this.tokenTime = null;
    }
}
