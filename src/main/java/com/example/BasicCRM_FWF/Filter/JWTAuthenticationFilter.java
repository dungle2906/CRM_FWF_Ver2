package com.example.BasicCRM_FWF.Filter;

import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Repository.TokenRepository;
import com.example.BasicCRM_FWF.Service.JWTService;
import com.example.BasicCRM_FWF.Token.Token;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;
    private static final Logger log = LoggerFactory.getLogger(CommonsRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String reqId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("reqId", reqId);

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String origin = request.getHeader("Origin");
        String ua = request.getHeader("User-Agent");

        // Chỉ log trạng thái có/không có header Authorization, KHÔNG log token thật
        boolean hasAuth = request.getHeader("Authorization") != null;

        log.info("➡️  {} {} | origin={} | ua={} | hasAuth={}", method, uri, origin, ua, hasAuth);

        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authorizationHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {

            try {
                Jwt decodedJwt = jwtService.jwtDecoder.decode(jwt);
                String email = decodedJwt.getClaim("email");
                processOAuth2User(email, request);
                filterChain.doFilter(request, response);
                return;
            } catch (JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Token has been revoked or expired1");
                response.getWriter().flush();
//                response.sendRedirect("/Authentication/Authenticate");
                return;
            }
        }

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
//        findTokenByUserID(request, userDetails, jwt);
        var tokenOptional = tokenRepository.findTokenByToken(jwt);

        if (tokenOptional.isPresent()) {
            Token token = tokenOptional.get();
            boolean isDatabaseTokenValid = !token.isExpired() && !token.isRevoked();
            if (jwtService.isTokenValid(jwt, userDetails) && isDatabaseTokenValid) {
                System.out.println("Authority: " + userDetails.getAuthorities());
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                System.out.println("Sau khi set Authentication: " + SecurityContextHolder.getContext().getAuthentication());
            } else {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Token has been revoked or expired2");
                response.getWriter().flush();
//                response.sendRedirect("/Authentication/Authenticate");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void processOAuth2User(String email, HttpServletRequest request) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        System.out.println(userDetails.getUsername());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void findTokenByUserID(HttpServletRequest request, UserDetails userDetails, String jwt) {
        User user = (User) userDetails;
        Integer userId = user.getId();
        var tokenOptional = tokenRepository.findById(userId);
        if (tokenOptional.isPresent()) {
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
    }
}

