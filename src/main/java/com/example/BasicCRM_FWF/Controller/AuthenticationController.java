package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTO.*;
import com.example.BasicCRM_FWF.Service.AuthenticationService.IAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<String> register(
           @RequestBody RegisterRequest request,
           HttpServletRequest servletRequest,
           HttpServletResponse servletResponse
    ){
        return authenticationService.register(request, servletRequest, servletResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) throws Exception {
        AuthenticationResponse auth = authenticationService.authenticate(request, servletRequest);

        // Set refresh token bằng HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", auth.getRefreshToken())
                .httpOnly(true)
                .secure(true) // bật khi chạy HTTPS
                .path("/api/auth/refresh-token") // chỉ gửi cookie cho endpoint refresh
                .sameSite("Strict") // Set Strict là default hoặc Lax nếu cần
                .maxAge(7 * 24 * 60 * 60) // 7 ngày
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        // Trả lại response nhưng KHÔNG chứa refresh token trong JSON nữa
        return ResponseEntity.ok(
                AuthenticationResponse.builder()
                        .accessToken(auth.getAccessToken())
//                        .refreshToken(auth.getRefreshToken())
                        .role(auth.getRole())
                        .build()
        );
    }

    @PostMapping("/refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        authenticationService.refreshToken(request, response);
    }

    @PostMapping("/forgot-password/{email}")
    public ResponseEntity<String> forgotPassword(
            @PathVariable("email") String email
    ) {
        return authenticationService.forgotPassword(email);
    }

    @PostMapping("/change-forgot-password")
    public ResponseMessageAPI changeForgotPassword(
            @RequestBody ChangeForgotPasswordRequest request,
            Principal connectedUser
    ) throws BadRequestException {
        authenticationService.changeForgotPassword(request);
        return ResponseMessageAPI.builder()
                .message("Change password successfully")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @GetMapping("/register/verify")
    public void verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) throws IOException {
        authenticationService.verifyEmail(token, servletRequest, servletResponse);
    }

    @PostMapping("/register/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam("email") String email) {
        return authenticationService.resendVerification(email);
    }
}
