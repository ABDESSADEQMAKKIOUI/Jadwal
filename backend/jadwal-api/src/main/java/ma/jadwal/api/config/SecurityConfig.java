package ma.jadwal.api.config;

import jakarta.servlet.http.HttpServletResponse;
import ma.jadwal.api.securite.JwtAuthFilter;
import ma.jadwal.api.securite.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain chaineDeFiltres(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(formulaire -> formulaire.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(gestion -> gestion
                        .authenticationEntryPoint((requete, reponse, exception) ->
                                ecrireErreur(reponse, 401, "Authentification requise"))
                        .accessDeniedHandler((requete, reponse, exception) ->
                                ecrireErreur(reponse, 403, "Accès refusé")))
                .authorizeHttpRequests(autorisations -> autorisations
                        .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/ecole/**").hasRole("DIRECTEUR")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void ecrireErreur(HttpServletResponse reponse, int statut, String message) throws IOException {
        reponse.setStatus(statut);
        reponse.setContentType("application/json;charset=UTF-8");
        reponse.getWriter().write("{\"statut\":" + statut + ",\"message\":\"" + message + "\"}");
    }

    @Bean
    public BCryptPasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }
}
