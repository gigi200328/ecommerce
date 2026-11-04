package com.ojt.ecommerce.backofficeinventory.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtUtil.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token received: {}", e.getMessage());
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED", "Your session has expired. Please log in again.");
            return;
        } catch (JwtException e) {
            log.warn("Invalid JWT token received: {}", e.getMessage());
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Invalid authentication token. Please log in again.");
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = this.customUserDetailsService.loadUserByUsername(userEmail);
            } catch (Exception e) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "USER_NOT_FOUND", "Account not found or no longer available.");
                return;
            }

            // Check if user account is active/enabled
            if (!userDetails.isEnabled()) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_DISABLED", "Your account has been deactivated. Please contact an administrator.");
                return;
            }

            if (jwtUtil.validateToken(jwt, userDetails)) {
                CustomUserDetails customDetails = (CustomUserDetails) userDetails;
                Integer tokenVersionInToken = jwtUtil.extractTokenVersion(jwt);

                // Token Version mismatch check (Single active session & revoked token enforcement)
                if (tokenVersionInToken == null || !tokenVersionInToken.equals(customDetails.getTokenVersion())) {
                    sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "SESSION_TERMINATED", "Your session has expired because a new login occurred from another session.");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void sendJsonError(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\":\"%s\",\"message\":\"%s\"}", errorCode, message));
    }
}
