package ma.jadwal.api.securite;

import io.jsonwebtoken.JwtException;
import ma.jadwal.common.UtilisateurCourant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("cle-de-test-jadwal-0123456789abcdef0123456789abcdef");

    @Test
    void allerRetourDesClaimsPourUnDirecteur() {
        String token = jwtService.genererToken(7L, "directeur@ecole.ma", "DIRECTEUR", 3L, "Salma Bennani");

        UtilisateurCourant courant = jwtService.validerToken(token);

        assertEquals(7L, courant.id());
        assertEquals("directeur@ecole.ma", courant.email());
        assertEquals("DIRECTEUR", courant.role());
        assertEquals(3L, courant.etablissementId());
    }

    @Test
    void etablissementIdNulPourLeSuperAdmin() {
        String token = jwtService.genererToken(1L, "admin@jadwal.ma", "SUPER_ADMIN", null, "Administrateur JADWAL");

        UtilisateurCourant courant = jwtService.validerToken(token);

        assertEquals(1L, courant.id());
        assertEquals("SUPER_ADMIN", courant.role());
        assertNull(courant.etablissementId());
    }

    @Test
    void unJetonIllisibleEstRejete() {
        assertThrows(JwtException.class, () -> jwtService.validerToken("jeton-invalide"));
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRejete() {
        JwtService autreService = new JwtService("une-autre-cle-secrete-0123456789abcdef0123456789");
        String token = autreService.genererToken(2L, "x@y.ma", "DIRECTEUR", 1L, "Autre Utilisateur");

        assertThrows(JwtException.class, () -> jwtService.validerToken(token));
    }
}
