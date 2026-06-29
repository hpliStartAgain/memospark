package com.memospark.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserService userService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper,
                                           JwtAuthFilter jwtAuthFilter) throws Exception {
        // CSRF: cookie-based so SPA can read XSRF-TOKEN and send X-XSRF-TOKEN header.
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                        "/api/auth/login", "/api/auth/register",
                        "/api/auth/wx-login", "/api/auth/token",
                        "/api/quick-add/**"   // Bearer-auth, stateless — no CSRF needed
                )
                // Requests carrying a Bearer JWT never need CSRF (custom header = safe)
                .ignoringRequestMatchers(req -> {
                    String auth = req.getHeader("Authorization");
                    return auth != null && auth.startsWith("Bearer ");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints & static SPA assets
                .requestMatchers("/api/auth/**").permitAll()
                // Quick-add: Bearer-key auth handled in controller
                .requestMatchers("/api/quick-add/**").permitAll()
                .requestMatchers(
                    "/", "/index.html", "/login",
                    "/assets/**", "/*.js", "/*.css", "/*.ico", "/*.png", "/*.svg",
                    "/manifest.webmanifest", "/sw.js",
                    "/dashboard/**", "/targets/**",
                    "/decks/**", "/review/**", "/practice/**", "/notebook/**",
                    "/stats/**", "/settings/**", "/admin/**", "/plans/**"
                ).permitAll()
                // Actuator health (unauthenticated)
                .requestMatchers("/actuator/health").permitAll()
                // Admin-only operations
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/practice/problems").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/practice/problems/*").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/practice/problems/*").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/decks/pool").hasRole("ADMIN")
                // Everything else under /api requires authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(response.getOutputStream(),
                            Map.of("error", "Not authenticated"));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(response.getOutputStream(),
                            Map.of("error", "Access denied"));
                })
            )
            .addFilterAfter(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {
                    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                    if (csrfToken == null) {
                        csrfToken = (CsrfToken) request.getAttribute("_csrf");
                    }
                    if (csrfToken != null) {
                        csrfToken.getToken();
                    }
                    filterChain.doFilter(request, response);
                }
            }, CsrfFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpStatus.OK.value());
                    response.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(response.getOutputStream(),
                            Map.of("message", "Logged out"));
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
            );
        return http.build();
    }
}
