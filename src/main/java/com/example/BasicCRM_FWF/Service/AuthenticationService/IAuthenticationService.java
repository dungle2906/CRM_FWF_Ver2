package com.example.BasicCRM_FWF.Service.AuthenticationService;

import com.example.BasicCRM_FWF.DTO.AuthenticationRequest;
import com.example.BasicCRM_FWF.DTO.AuthenticationResponse;
import com.example.BasicCRM_FWF.DTO.ChangeForgotPasswordRequest;
import com.example.BasicCRM_FWF.DTO.RegisterRequest;
import com.example.BasicCRM_FWF.Model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface IAuthenticationService {

    ResponseEntity<String> register(RegisterRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse);

    AuthenticationResponse authenticate(AuthenticationRequest request, HttpServletRequest servletRequest) throws Exception;

    void verifyEmail(String token, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException;

    void saveUserToken(User savedUser, String jwtToken);

    void cleanUpExpiredTokens(User user);

    void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;

    void registerByGoogle(String jwt, HttpServletRequest servletRequest);

    void changeForgotPassword(ChangeForgotPasswordRequest request) throws BadRequestException;

    ResponseEntity<String> forgotPassword(String email);

    ResponseEntity<String> resendVerification(String email);
}
