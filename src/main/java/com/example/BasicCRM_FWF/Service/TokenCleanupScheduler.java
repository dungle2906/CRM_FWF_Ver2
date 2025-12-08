package com.example.BasicCRM_FWF.Service;

import com.example.BasicCRM_FWF.Repository.OTPRepository;
import com.example.BasicCRM_FWF.Repository.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenRepository tokenRepository;
    private final OTPRepository otpRepository;
    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    // Chạy mỗi 1 tiếng (cron = giây, phút, giờ, ngày, tháng, thứ)
    @Scheduled(cron = "0 0 2 * * Sun")
    public void cleanExpiredTokens() {
        int deletedCount = tokenRepository.deleteExpiredOrRevokedTokens();
        log.info("===== 🧹 Dọn dẹp token: {} token đã bị xóa =====", deletedCount);
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteAllExpiredSince(LocalDateTime.now());
    }
}
