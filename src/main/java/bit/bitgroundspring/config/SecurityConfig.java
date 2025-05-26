package bit.bitgroundspring.config;

import bit.bitgroundspring.security.oauth2.CustomOidcUserService;
import bit.bitgroundspring.security.oauth2.OidcAuthenticationFailureHandler;
import bit.bitgroundspring.security.oauth2.OidcAuthenticationSuccessHandler;
import bit.bitgroundspring.security.token.JwtAuthenticationFilter;
import bit.bitgroundspring.security.token.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOidcUserService customOidcUserService;
    private final OidcAuthenticationSuccessHandler oidcAuthenticationSuccessHandler;
    private final OidcAuthenticationFailureHandler oidcAuthenticationFailureHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${react.host}")
    private String reactHost; // React 앱의 호스트 주소

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 미인증 상태에서 401 응답 반환
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        }))
                // HTTP 요청에 대한 인가 규칙 설정
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/api/auth/refresh", "/oauth2/**", "/error", "/favicon.ico", "/test",
                                "/api/news/**", "/api/coin/**", // /api/coin/** 허용
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // 인증 없이 허용
                        .requestMatchers("/api/public/**").permitAll() // 공개 API 경로
                        .requestMatchers("/api/user/**").hasRole("USER") // USER 역할 요구
                        .anyRequest().authenticated() // 그 외는 인증 필요
                        .requestMatchers("/auth/refresh", "/oauth2/**", "/actuator/*", "/error", "/test").permitAll() // OAuth2 로그인 과정 및 일부 정적 리소스 허용
                        .requestMatchers("/public/**").permitAll() // 공개 API 경로
                        .requestMatchers("/api/user/**").hasRole("USER") // USER 역할이 있는 사용자만 접근 가능 (역할 기반 접근 제어 예시)
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                // OAuth2/OIDC 로그인 설정
                .oauth2Login(oauth2Login -> oauth2Login
                        .userInfoEndpoint(userInfoEndpoint ->
                                userInfoEndpoint.oidcUserService(customOidcUserService)
                        )
                        .successHandler(oidcAuthenticationSuccessHandler)
                        .failureHandler(oidcAuthenticationFailureHandler)
                )
                // JWT 사용 시 세션 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(reactHost, "http://localhost:5173")); // Vite 앱 및 로컬 테스트 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}