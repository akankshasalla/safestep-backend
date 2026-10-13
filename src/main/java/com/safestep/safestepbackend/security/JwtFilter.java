package com.safestep.safestepbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Only these actions need an officer login
    private boolean needsLogin(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean listAll = method.equals("GET") && path.equals("/api/complaints");
        boolean updateStatus = method.equals("PUT")
                && path.startsWith("/api/complaints/") && path.endsWith("/status");
        boolean evidence = method.equals("GET")
                && path.startsWith("/api/complaints/") && path.endsWith("/evidence");
        return listAll || updateStatus || evidence;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (needsLogin(request)) {
            String header = request.getHeader("Authorization");
            boolean ok = header != null && header.startsWith("Bearer ")
                    && jwtUtil.isValid(header.substring(7));
            if (!ok) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.getWriter().write("{\"message\":\"Login required\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}