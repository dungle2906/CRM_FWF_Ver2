package com.example.BasicCRM_FWF.Service.SecureTokenService;


import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Token.SecureToken;

public interface ISecureTokenService {
    SecureToken createToken();
    void saveSecureToken(SecureToken secureToken);
    SecureToken findByToken(String token);
    void removeToken(SecureToken token);
    void removeTokenByUser(User user);
}
