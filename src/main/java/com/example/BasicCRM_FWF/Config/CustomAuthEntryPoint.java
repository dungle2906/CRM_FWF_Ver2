package com.example.BasicCRM_FWF.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.getWriter().write(
            new ObjectMapper().writeValueAsString(Map.of(
                "status", 401,
                "error", "UNAUTHORIZED",
                "message", "Token is invalid or get expired. Please try login again!",
                "path", request.getRequestURI()
            ))
        );
    }
}
