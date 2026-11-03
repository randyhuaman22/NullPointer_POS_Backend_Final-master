package pe.upeu.nullpointerpos.autenticacion.config;

import pe.upeu.nullpointerpos.autenticacion.security.FiltroAutenticacionJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, FiltroAutenticacionJwt filtro) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**", "/error"))
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR, jakarta.servlet.DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/error", "/api/v1/salud", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/autenticacion/iniciar-sesion").permitAll()
                        .requestMatchers("/api/v1/administracion/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((q, r, e) -> {
                            r.setStatus(401); r.setContentType("application/json;charset=UTF-8");
                            r.getWriter().write("{\"estado\":401,\"mensaje\":\"Debes iniciar sesión.\"}");
                        })
                        .accessDeniedHandler((q, r, e) -> {
                            r.setStatus(403); r.setContentType("application/json;charset=UTF-8");
                            r.getWriter().write("{\"estado\":403,\"mensaje\":\"No tienes permiso para esta operación.\"}");
                        }))
                .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    CorsConfigurationSource configuracionCors(
            @Value("${app.cors.origenes-permitidos:*}") String patrones) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(patrones.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource();
        origen.registerCorsConfiguration("/**", config);
        return origen;
    }
}
