package pe.upeu.nullpointerpos.caja.config;
import pe.upeu.nullpointerpos.caja.security.FiltroAutenticacionJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays; import java.util.List;
@Configuration
public class ConfiguracionSeguridad {
    @Bean SecurityFilterChain cadena(HttpSecurity http,FiltroAutenticacionJwt filtro)throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**", "/error")).cors(Customizer.withDefaults()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR,jakarta.servlet.DispatcherType.FORWARD).permitAll()
                .requestMatchers("/error","/api/v1/salud","/actuator/health","/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/clientes/**").hasAnyRole("CAJERO","ADMINISTRADOR")
                .requestMatchers("/api/v1/caja/**","/api/v1/clientes/**","/api/v1/tickets/**","/api/v1/comprobantes/**").hasAnyRole("CAJERO","ADMINISTRADOR")
                .anyRequest().authenticated())
            .exceptionHandling(ex->ex.authenticationEntryPoint((q,r,e)->{r.setStatus(401);r.setContentType("application/json;charset=UTF-8");r.getWriter().write("{\"estado\":401,\"mensaje\":\"Debes iniciar sesión.\"}");})
                .accessDeniedHandler((q,r,e)->{r.setStatus(403);r.setContentType("application/json;charset=UTF-8");r.getWriter().write("{\"estado\":403,\"mensaje\":\"No tienes permiso para esta operación.\"}");}))
            .addFilterBefore(filtro,UsernamePasswordAuthenticationFilter.class); return http.build();
    }
    @Bean CorsConfigurationSource cors(@Value("${app.cors.origenes-permitidos:*}") String origenes){
        CorsConfiguration c=new CorsConfiguration();c.setAllowedOriginPatterns(Arrays.stream(origenes.split(",")).map(String::trim).filter(x->!x.isBlank()).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Idempotency-Key"));c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
    }
}
