package com.example.rag.auth.config;

import com.example.rag.common.web.RequestContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig
 * 
 * @author gel
 * @date 2026/7/31
 * @description 
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final RequestContextFilter
            requestContextFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    @Value("${rag.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * 定义接口访问规则和认证方式。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors->
                        cors.configurationSource(
                                corsConfigurationSource()
                        ))
                .sessionManagement(
                        /*
                         * JWT API 不使用服务器 Session。
                         * 每次请求都通过 Bearer Token 建立身份。
                         */
                session->session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                )
        )
                /*
                 * 当前认证信息放在 Authorization Header，
                 * 不使用 Cookie，因此关闭 CSRF。
                 */
                .csrf(
                        csrf->csrf.disable()
                )
                // 配置公开接口和受保护接口。
                .authorizeHttpRequests(
                        authorize->authorize.requestMatchers(
                                "/api/auth/login",
                                        "/api/auth/refresh",
                                "/api/health",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                        "/actuator/health",
                                        "/actuator/info",
                                        "/actuator/metrics",
                                        "/actuator/metrics/**"
                        )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                /*
                 * 开启 OAuth2 Resource Server JWT。
                 * Spring 会自动注册 BearerTokenAuthenticationFilter。
                 */
                .oauth2ResourceServer(resourceServer ->
                        resourceServer
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
                                .jwt(
                                        jwt -> jwt.jwtAuthenticationConverter(
                                                jwtAuthenticationConverter()
                        )
                ))
                .exceptionHandling(exception->
                        exception.authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler))
                /*
                 * JWT 验证完成后，再执行 RequestContextFilter。
                 * 此时 SecurityContext 中已经存在 Authentication。
                 */
                .addFilterAfter(
                        requestContextFilter,
                        BearerTokenAuthenticationFilter.class
                );
        return http.build();
    }
    /**
     * 把 JWT 中的 role Claim 转换为 ROLE_xxx 权限。
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // 从自定义 role Claim 读取权限。
        authoritiesConverter.setAuthoritiesClaimName("role");

        // USER 转换成 ROLE_USER，ADMIN 转换成 ROLE_ADMIN。
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }
    /**
     * 禁止 RequestContextFilter 被 Servlet 容器自动注册。
     *
     * <p>否则它可能执行两次，或者在 Spring Security 前执行。</p>
     */
    @Bean
    public FilterRegistrationBean<RequestContextFilter>
    requestContextFilterRegistration(
            RequestContextFilter filter
    ) {
        FilterRegistrationBean<RequestContextFilter>
                registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);
        return registration;
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration =new CorsConfiguration();

        // 必须填写完整 Origin，不能带路径。
        configuration.setAllowedOrigins(
                allowedOrigins
        );

        // 允许前端使用的 HTTP 方法。
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // 允许前端发送的请求头。
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-Request-Id",
                        "Accept",
                        "Cache-Control"
                )
        );

        // 允许前端读取后端返回的请求链路 ID。
        configuration.setExposedHeaders(
                List.of(
                        "X-Request-Id"
                )
        );

        /*
         * 当前认证信息放在 Authorization Header 中，
         * 不使用跨域 Cookie，因此可以设置为 false。
         */
        configuration.setAllowCredentials(false);

        // 浏览器缓存预检请求一小时。
        configuration.setMaxAge(3600L);
        source.registerCorsConfiguration(
                "/**",
                configuration);
        return source;
    }
}