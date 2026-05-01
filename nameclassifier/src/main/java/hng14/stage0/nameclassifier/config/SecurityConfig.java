package hng14.stage0.nameclassifier.config;

import hng14.stage0.nameclassifier.dto.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import hng14.stage0.nameclassifier.security.ApiVersionFilter;
import hng14.stage0.nameclassifier.security.JwtAuthenticationFilter;
import hng14.stage0.nameclassifier.security.RateLimitFilter;
import hng14.stage0.nameclassifier.security.RequestLoggingFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiVersionFilter apiVersionFilter,
            RateLimitFilter rateLimitFilter,
            RequestLoggingFilter requestLoggingFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // /api/users/me — accessible to any authenticated user (admin or analyst)
                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("admin", "analyst")

                        // Profile write operations — admin only
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("admin")

                        // Profile read operations — admin + analyst
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("admin", "analyst")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiVersionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    new ErrorResponse("error", "Unauthorized"));
                        })
                );

        return http.build();
    }
}