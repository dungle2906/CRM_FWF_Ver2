package com.example.BasicCRM_FWF.Service.AuthenticationService;

import com.example.BasicCRM_FWF.DTO.*;
import com.example.BasicCRM_FWF.Mailing.AccountVerificationEmailContext;
import com.example.BasicCRM_FWF.Mailing.EmailService;
import com.example.BasicCRM_FWF.Model.OTP;
import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Repository.OTPRepository;
import com.example.BasicCRM_FWF.Repository.TokenRepository;
import com.example.BasicCRM_FWF.Repository.UserRepository;
import com.example.BasicCRM_FWF.RoleAndPermission.Role;
import com.example.BasicCRM_FWF.Service.AuthRealTime.AuthService;
import com.example.BasicCRM_FWF.Service.JWTService;
import com.example.BasicCRM_FWF.Service.SecureTokenService.ISecureTokenService;
import com.example.BasicCRM_FWF.Token.SecureToken;
import com.example.BasicCRM_FWF.Token.Token;
import com.example.BasicCRM_FWF.Token.TokenType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements IAuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final ISecureTokenService secureTokenService;
    private final OTPRepository otpRepository;
    private final AuthService authService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Value("${application.frontend-url}")
    private String frontendUrl;
    @Value("${application.backend-url}")
    private String backendUrl;
    @Value("${application.deploy-frontend-url}")
    private String deployFrontendUrl;
    @Value("${application.deploy-backend-url}")
    private String deployBackendUrl;

    @Override
    public ResponseEntity<String> register(RegisterRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

        validateRequestRegister(request);

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmailOrPhoneNumber().contains("@") ? request.getEmailOrPhoneNumber() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .isVerified(false)
                .isActive(true)
                .build();
        var savedUser = userRepository.save(user); // Save user in DB

        // Extract IP address from the request
        String ipAddress = servletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = servletRequest.getRemoteAddr();
        }

        // Create secure token and set TTL 10m in createToken()
        var secureToken = secureTokenService.createToken();
        secureToken.setUser(savedUser);
        secureTokenService.saveSecureToken(secureToken);


        // Prepare and send verification email
        AccountVerificationEmailContext emailContext = new AccountVerificationEmailContext();
        emailContext.init(savedUser);
        emailContext.setToken(secureToken.getToken());

        String baseUrl = backendUrl;
        emailContext.buildVerificationUrl(baseUrl, secureToken.getToken());

        try {
            emailService.sendMail(emailContext);
            return ResponseEntity.ok("We have send to your email a verification, please have a check and complete your registration!");
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private void validateRequestRegister(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (request.getEmailOrPhoneNumber().contains("@")) {
            if (userRepository.findByEmail(request.getEmailOrPhoneNumber()) != null ) {
                throw new IllegalArgumentException("Email already exists");
            }
        } else {
            if (userRepository.findByPhoneNumber(request.getEmailOrPhoneNumber()).isPresent()) {
                throw new IllegalArgumentException("Phone number already exists");
            }
        }

        if (!USERNAME_PATTERN.matcher(request.getUsername()).matches()) {
            throw new IllegalArgumentException("Invalid username. Only alphanumeric characters and underscores are allowed (3-20 characters).");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmailOrPhoneNumber()).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        if (request.getPassword().length() < 8 || request.getPassword().length() > 50) {
            throw new IllegalArgumentException("Password must be between 8 and 50 characters");
        }
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request, HttpServletRequest servletRequest) throws Exception {

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username not exist!"));

        if (!user.getIsActive()) {
            throw new DisabledException("Your account is banned");
        }

        if (!user.getIsVerified()) {
            throw new DisabledException("Your account is not verified");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Username and password not correct!");
        }

        // Extract IP address from the request
        String ipAddress = servletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = servletRequest.getRemoteAddr();
        }

        var jwtToken = jwtService.generateToken(user, ipAddress);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserToken(user);
        saveUserToken(user, jwtToken);

        authService.getToken();

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .build();
    }

    @Override
    public void verifyEmail(String token, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

        SecureToken secureToken = secureTokenService.findByToken(token);

        if (secureToken == null || secureToken.isExpired()) {
            servletResponse.sendRedirect(frontendUrl + "/verified?status=expired");
            return;
        }

        assert secureToken != null;
        User user = secureToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);
        secureTokenService.removeToken(secureToken);

        servletResponse.sendRedirect(frontendUrl + "/verified?status=success");
    }

    @Override
    public void saveUserToken(User savedUser, String jwtToken) {
        var token = Token.builder()
                .user(savedUser)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    protected void revokeAllUserToken(User user) {
        var validUserTokens = tokenRepository.findAllTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    @Override
    public void cleanUpExpiredTokens(User user) {
        tokenRepository.deleteExpiredAndRevokedTokensByUserId(user.getId());
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("refresh_token"))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            new ObjectMapper().writeValue(response.getOutputStream(),
                    Map.of("error", "Missing refresh token"));
            return;
        }

        final String username = jwtService.extractUsername(refreshToken);
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            new ObjectMapper().writeValue(response.getOutputStream(),
                    Map.of("error", "Invalid refresh token"));
            return;
        }

        var userDetails = userRepository.findByUsername(username).orElse(null);
        if (userDetails == null || !jwtService.isTokenValid(refreshToken, userDetails)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            new ObjectMapper().writeValue(response.getOutputStream(),
                    Map.of("error", "Refresh token expired or invalid"));
            return;
        }

        // OK -> Generate new access-token
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) ipAddress = request.getRemoteAddr();

        var accessToken = jwtService.generateToken(userDetails, ipAddress);

        new ObjectMapper().writeValue(response.getOutputStream(),
                Map.of(
                        "success", true,
                        "accessToken", accessToken
                ));
    }

    @Override
    public void registerByGoogle(String jwt, HttpServletRequest servletRequest) {
        Jwt decodedJwt = jwtService.jwtDecoder.decode(jwt);
        String email = decodedJwt.getClaim("email");
        var userEmail = userRepository.findByEmail(email);
        if(userEmail == null) {
            var user = User.builder()
                    .firstname(decodedJwt.getClaim("given_name"))
                    .lastname(decodedJwt.getClaim("family_name"))
                    .username(decodedJwt.getClaim("email"))
                    .email(decodedJwt.getClaim("email"))
                    .avatar(decodedJwt.getClaim("picture"))
                    .provider("GOOGLE")
                    .role(Role.USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy("SYSTEM")
                    .updatedBy("SYSTEM")
                    .isActive(true)
                    .build();
            var savedUser = userRepository.save(user);

            String ipAddress = servletRequest.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = servletRequest.getRemoteAddr();
            }

            saveUserToken(savedUser, jwt);
        } else {
            saveUserToken(userEmail, jwt);
        }
    }

    @Override
    @Transactional
    public void changeForgotPassword(ChangeForgotPasswordRequest request) throws BadRequestException {

        // Validate password
        if (request.getNewPassword().length() < 8 || request.getNewPassword().length() > 50) {
            throw new IllegalArgumentException("Password must be between 8 and 50 characters.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords and Confirm Password do not match.");
        }

        // Find OTP record
        OTP otpRecord = otpRepository.findByEmailAndOtp(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP."));

        if (otpRecord.isExpired()) {
            otpRepository.delete(otpRecord);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Find user
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new BadRequestException("No user found with the provided email.");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xoá OTP sau khi dùng
        otpRepository.delete(otpRecord);
    }

    @Override
    @Transactional
    public ResponseEntity<String> forgotPassword(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Email not found.");
        }

        // Xoá OTP cũ (nếu có)
        otpRepository.deleteByEmail(email);

        // Tạo OTP mới
        int otpValue = 100000 + new Random().nextInt(900000); // 6 digits
        OTP otp = OTP.builder()
                .email(email)
                .otp(String.valueOf(otpValue))
                .createdAt(LocalDateTime.now())
//                .expiredAt(LocalDateTime.now().plusMinutes(10))
                .expiredAt(LocalDateTime.now().minusSeconds(40))
                .build();
        otpRepository.save(otp);

        // Gửi email
        emailService.sendOtp(email, "Your Password Reset Code", otpValue);

        return ResponseEntity.ok("An OTP has been sent to your email. Please check your inbox.");
    }

    @Override
    @Transactional
    public ResponseEntity<String> resendVerification(String email) {
        var user = userRepository.findByEmail(email);
        if  (user == null) {
            throw new IllegalArgumentException("Email not found.");
        }

        if (user.getIsVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account already verified.");
        }

        // Xoá token cũ (nếu còn)
        secureTokenService.removeTokenByUser(user);

        // Tạo token mới (15 phút)
        var newToken = secureTokenService.createToken();
        newToken.setUser(user);
        newToken.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        secureTokenService.saveSecureToken(newToken);

        // Gửi lại email
        AccountVerificationEmailContext emailContext = new AccountVerificationEmailContext();
        emailContext.init(user);
        emailContext.setToken(newToken.getToken());
        emailContext.buildVerificationUrl(backendUrl, newToken.getToken());

        try {
            emailService.sendMail(emailContext);
            return ResponseEntity.ok("A new verification email has been sent.");
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }


//    @Override
//    public ResponseEntity<String> forgotPassword(String email, HttpServletRequest servletRequest, HttpServletResponse servletResponse){
//
//        if (!EMAIL_PATTERN.matcher(email).matches()) {
//            throw new IllegalArgumentException("Invalid email format.");
//        }
//
//        User savedUser = userRepository.findByEmail(email);
//        var secureToken = secureTokenService.createToken();
//        secureToken.setUser(savedUser);
//        secureTokenService.saveSecureToken(secureToken);
//
//        // Prepare and send verification email
//        AccountVerificationEmailContext emailContext = new AccountVerificationEmailContext();
//        emailContext.init(savedUser);
//        emailContext.setToken(secureToken.getToken());
//
////        Cookie emailToken = new Cookie("emailToken", secureToken.getToken());
////        emailToken.setHttpOnly(false);
////        emailToken.setSecure(false);
////        emailToken.setPath("/");
////        emailToken.setMaxAge(3600);
////        emailToken.setAttribute("SameSite", "Lax");
////        response.addCookie(emailToken);
//
////        String cookie = String.format("emailTokenForGG=%s; Max-Age=3600; Path=/; SameSite=Lax", secureToken.getToken());
////        servletResponse.setHeader("Set-Cookie", cookie);
//
////        String baseUrl = "http://localhost:5173";
////        emailContext.buildVerificationUrl(baseUrl, secureToken.getToken());
//
//        try {
//            emailService.sendMail(emailContext);
//            return ResponseEntity.ok("We have send to your email a verification, please check have a check and complete your registration!");
//        } catch (MessagingException e) {
//            throw new RuntimeException("Failed to send verification email", e);
//        }
//    }
}
