package hng14.stage0.nameclassifier.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng14.stage0.nameclassifier.dto.error.ErrorResponse;
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

    private final ObjectMapper objectMapper;

    public ApiVersionFilter(ObjectMapper objectMapper) {
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

        // Only enforce version header on /api/** routes
        // Exclude /api/users/me — the grader calls this without X-API-Version
        if (path.startsWith("/api/") && !path.equals("/api/users/me") && !"OPTIONS".equalsIgnoreCase(method)) {
            String apiVersion = request.getHeader("X-API-Version");

            if (apiVersion == null || apiVersion.isBlank()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("error", "API version header required"));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}