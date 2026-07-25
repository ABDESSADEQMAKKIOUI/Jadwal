package ma.jadwal.api.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.jadwal.common.UtilisateurCourant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit le header Authorization Bearer, valide le JWT et pose l'authentification
 * dont le principal est un {@link UtilisateurCourant}.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String entete = requete.getHeader("Authorization");
        if (entete != null && entete.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = entete.substring(7);
            try {
                UtilisateurCourant courant = jwtService.validerToken(token);
                List<SimpleGrantedAuthority> autorites =
                        List.of(new SimpleGrantedAuthority("ROLE_" + courant.role()));
                UsernamePasswordAuthenticationToken authentification =
                        new UsernamePasswordAuthenticationToken(courant, null, autorites);
                authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(requete));
                SecurityContextHolder.getContext().setAuthentication(authentification);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chaine.doFilter(requete, reponse);
    }
}
