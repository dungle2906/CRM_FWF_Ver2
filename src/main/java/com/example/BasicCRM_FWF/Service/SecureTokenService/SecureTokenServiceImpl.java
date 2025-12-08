package com.example.BasicCRM_FWF.Service.SecureTokenService;

import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Repository.SecureTokenRepository;
import com.example.BasicCRM_FWF.Token.SecureToken;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SecureTokenServiceImpl implements ISecureTokenService {

    private static final BytesKeyGenerator DEFAULT_TOKEN_GENERATOR = KeyGenerators.secureRandom(12);

    @Value("600")
    private int tokenValidityInSeconds;

    private final SecureTokenRepository secureTokenRepository;

    @Override
    public SecureToken createToken() {
        String tokenValue = Base64.encodeBase64URLSafeString(DEFAULT_TOKEN_GENERATOR.generateKey());
        SecureToken secureToken = new SecureToken();
        secureToken.setToken(tokenValue);
        secureToken.setExpiredAt(LocalDateTime.now().plusSeconds(tokenValidityInSeconds));
        this.saveSecureToken(secureToken);
        return secureToken;
    }

    @Override
    public void saveSecureToken(SecureToken secureToken) {
        secureTokenRepository.save(secureToken);
    }

    @Override
    public SecureToken findByToken(String token) {
        return secureTokenRepository.findByToken(token);
    }

    @Override
    public void removeToken(SecureToken token) {
        secureTokenRepository.delete(token);
    }

    @Override
    public void removeTokenByUser(User user) {
        secureTokenRepository.removeSecureTokenByUser(user);
    }
}
