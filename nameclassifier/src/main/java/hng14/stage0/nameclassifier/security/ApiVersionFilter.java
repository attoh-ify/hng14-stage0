package hng14.stage0.nameclassifier.security;

import hng14.stage0.nameclassifier.dto.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiVersionFilter extends OncePerRequestFilter {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String REQUIRED_VERSION = "1";

    private final ObjectMapper objectMapper;

    public ApiVersionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Allow any path starting with /auth/ to pass without the version header
        return !path.startsWith("/api/") || path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String version = request.getHeader(API_VERSION_HEADER);

        if (version == null || version.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse("error", "API version header required")
            );
            return;
        }

        if (!"1".equals(version.trim())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse("error", "Invalid API version")
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}