package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        log.info("Creating DAO authentication provider bean");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        log.info("Creating authentication manager bean");
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
        log.info("Creating JWT authentication filter bean");
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS settings - allowing all origins");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // API Security Configuration (JWT-based)
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            DaoAuthenticationProvider authenticationProvider) throws Exception {
        log.info("Configuring API Security filter chain");

        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/web/**", "/api/data/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("API Security configuration completed");
        return http.build();
    }

//    @Bean
//    @Order(2)
//    public SecurityFilterChain webFilterChain(
//            HttpSecurity http,
//            DaoAuthenticationProvider authenticationProvider) throws Exception {
//        log.info("Configuring Web Security filter chain");
//
//        http.securityMatcher("/**")
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        // Allow public pages
//                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**","/api/web/**","/attendance/**").permitAll()
//                        // Require authentication for everything else
//                        .anyRequest().authenticated()
//                )
//                .authenticationProvider(authenticationProvider)
//                .formLogin(form -> form
//                        .loginPage("/login")           // your custom login page
//                        .permitAll()
//                )
//                .logout(logout -> logout.permitAll());
//
//
//        log.info("Web Security configuration completed");
//        return http.build();
//    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http,
                                              DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                // If your UI and API are same-origin, keep CORS default; otherwise configure a CorsConfigurationSource bean
                .cors(Customizer.withDefaults())

                // SockJS uses POSTs to /ws/**; ignore CSRF there (or disable globally if you prefer)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))

                // If you keep SockJS iframe fallback enabled, allow same-origin frames
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))

                // formLogin requires a session; STATELESS causes 302/HTML for XHR transports
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> auth
                        // static resources
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // your public endpoints
                        .requestMatchers("/login", "/api/web/**", "/attendance/**").permitAll()

                        // **critical**: allow STOMP/SockJS handshake + info/XHR/websocket endpoints
                        .requestMatchers("/ws/**").permitAll()

                        // dev hot-reload (optional)
                        .requestMatchers("/sockjs-node/**").permitAll()

                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider)

                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout.permitAll())

                // Optional: make APIs return 401 instead of redirecting HTML to /login
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")
                ));

        return http.build();
    }

}