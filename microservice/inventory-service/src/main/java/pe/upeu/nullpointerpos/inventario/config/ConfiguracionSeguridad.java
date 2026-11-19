package pe.upeu.nullpointerpos.inventario.config;

import pe.upeu.nullpointerpos.inventario.security.FiltroAutenticacionJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, FiltroAutenticacionJwt filtro) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**", "/error"))
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR, jakarta.servlet.DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/error", "/api/v1/salud", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/inventario/insumos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/inventario/insumos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventario/auditoria-insumos").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/v1/inventario/**").hasAnyRole("ALMACENERO", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((q, r, x) -> {
                            r.setStatus(401); r.setContentType("application/json;charset=UTF-8");
                            r.getWriter().write("{\"estado\":401,\"mensaje\":\"Debes iniciar sesión.\"}");
                        })
                        .accessDeniedHandler((q, r, x) -> {
                            r.setStatus(403); r.setContentType("application/json;charset=UTF-8");
                            r.getWriter().write("{\"estado\":403,\"mensaje\":\"No tienes permiso para esta operación.\"}");
                        }));
        return http.build();
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors.origenes-permitidos:*}") String patrones) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(patrones.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource();
        origen.registerCorsConfiguration("/**", config);
        return origen;
    }
}
