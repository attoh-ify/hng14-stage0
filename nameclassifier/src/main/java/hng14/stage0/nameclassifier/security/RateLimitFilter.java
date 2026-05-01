package hng14.stage0.nameclassifier.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng14.stage0.nameclassifier.dto.error.ErrorResponse;
import hng14.stage0.nameclassifier.entities.AppUser;
import hng14.stage0.nameclassifier.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Don't rate-limit OPTIONS preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit;
        String key;

        if (path.startsWith("/auth/")) {
            // Auth endpoints: 10 req/min per IP — matches TRD spec
            // /auth/github is a redirect (no payload), still rate-limited per spec
            limit = 10;
            key = "auth:" + getClientIp(request);
        } else {
            // All other endpoints: 60 req/min per user
            limit = 60;
            key = "user:" + getUserKey(request);
        }

        boolean allowed = rateLimitService.isAllowed(key, limit, 60);

        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse("error", "Too many requests. Please try again later.")
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getUserKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AppUser user) {
            return user.getId();
        }
        return getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}