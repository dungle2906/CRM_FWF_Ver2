package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Token.SecureToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecureTokenRepository extends JpaRepository<SecureToken, Long> {
    SecureToken findByToken(String token);
    void deleteByToken(String token);

    void removeSecureTokenByUser(User user);
}
