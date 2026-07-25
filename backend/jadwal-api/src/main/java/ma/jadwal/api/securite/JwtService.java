package ma.jadwal.api.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.jadwal.common.UtilisateurCourant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Génération et validation des jetons JWT (HS256, durée 12 heures).
 */
@Service
public class JwtService {

    private static final long DUREE_VALIDITE_MS = 43_200_000L; // 12 heures

    private final SecretKey cle;

    public JwtService(@Value("${jadwal.jwt.secret}") String secret) {
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String genererToken(Long id, String email, String role, Long etablissementId, String nomComplet) {
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("email", email)
                .claim("role", role)
                .claim("etablissementId", etablissementId)
                .claim("nomComplet", nomComplet)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + DUREE_VALIDITE_MS))
                .signWith(cle, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valide la signature et l'expiration du jeton puis reconstruit l'utilisateur courant.
     * Lève une {@link io.jsonwebtoken.JwtException} si le jeton est invalide.
     */
    public UtilisateurCourant validerToken(String token) {
        Claims claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(token).getPayload();
        Long id = Long.valueOf(claims.getSubject());
        String email = (String) claims.get("email");
        String role = (String) claims.get("role");
        Object brut = claims.get("etablissementId");
        Long etablissementId = brut == null ? null : ((Number) brut).longValue();
        return new UtilisateurCourant(id, email, role, etablissementId);
    }
}
