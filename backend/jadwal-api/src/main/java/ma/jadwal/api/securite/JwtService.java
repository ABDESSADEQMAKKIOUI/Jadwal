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
import java.util.Locale;
import java.util.Set;

/**
 * Génération et validation des jetons JWT (HS256, durée 12 heures).
 */
@Service
public class JwtService {

    private static final long DUREE_VALIDITE_MS = 43_200_000L; // 12 heures

    /** Longueur minimale d'un secret HS256 (32 octets = 256 bits). */
    private static final int LONGUEUR_MINIMALE = 32;

    /**
     * Secrets ayant circulé publiquement (dépôt public, documentation, exemples).
     * Toute valeur de cette liste doit être considérée comme compromise : un jeton
     * signé avec elle est forgeable par n'importe qui.
     */
    private static final Set<String> SECRETS_COMPROMIS = Set.of(
            "jadwal-dev-secret-change-me-0123456789abcdef",
            "changeme",
            "secret");

    private final SecretKey cle;

    public JwtService(@Value("${jadwal.jwt.secret}") String secret) {
        verifierSecret(secret);
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Refuse le démarrage plutôt que de signer avec un secret faible ou public.
     * Toute l'isolation entre établissements repose sur cette clé : un secret
     * devinable permet de forger un jeton DIRECTEUR de n'importe quelle école,
     * ou SUPER_ADMIN de la plateforme.
     */
    static void verifierSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JADWAL_JWT_SECRET n'est pas défini. Générez-en un : openssl rand -base64 48");
        }
        if (SECRETS_COMPROMIS.contains(secret.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "JADWAL_JWT_SECRET utilise une valeur publiée, donc compromise. "
                            + "Générez-en un nouveau : openssl rand -base64 48");
        }
        int octets = secret.getBytes(StandardCharsets.UTF_8).length;
        if (octets < LONGUEUR_MINIMALE) {
            throw new IllegalStateException("JADWAL_JWT_SECRET fait " + octets
                    + " octets ; il en faut au moins " + LONGUEUR_MINIMALE
                    + ". Générez-en un : openssl rand -base64 48");
        }
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
