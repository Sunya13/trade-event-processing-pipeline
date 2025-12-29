package com.trading.app.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiKeyFilter implements Filter {

    @Value("${app.service.token}")
    private String serviceToken;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 1. Allow CORS Preflight
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Allow Error Pages (Spring Boot handles these)
        if (req.getRequestURI().startsWith("/error")) {
            chain.doFilter(request, response);
            return;
        }

        // 3. CHECK TOKEN
        String clientToken = req.getHeader("X-Service-Token");

        if (serviceToken != null && !serviceToken.equals(clientToken)) {
            // Reject the request if token doesn't match
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Invalid Service Token");
            return;
        }

        // Token is valid, proceed
        chain.doFilter(request, response);
    }
}