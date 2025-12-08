package com.example.BasicCRM_FWF.Filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class DomainBlockFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(DomainBlockFilter.class);

    // ✅ Danh sách domain hoặc host bạn muốn chặn
    private static final List<String> BLOCKED_DOMAINS = List.of(
            "nhatuvan.vn"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String host = request.getRemoteHost();
        String ip = request.getRemoteAddr();

        // ✅ Kiểm tra có chứa domain hoặc IP bị chặn không
        boolean blocked = BLOCKED_DOMAINS.stream().anyMatch(domain ->
                (origin != null && origin.contains(domain)) ||
                (referer != null && referer.contains(domain)) ||
                (host != null && host.contains(domain)) ||
                ip.equals(domain)
        );

        if (blocked) {
            log.warn("⛔ Blocked request from spam domain/IP: origin={} referer={} ip={}", origin, referer, ip);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access denied.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
